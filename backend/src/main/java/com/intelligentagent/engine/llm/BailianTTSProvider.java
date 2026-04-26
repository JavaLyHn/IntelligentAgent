package com.intelligentagent.engine.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class BailianTTSProvider {

    private static final String BASE_URL = "https://dashscope.aliyuncs.com/api/v1";
    private static final String TTS_PATH = "/services/aigc/multimodal-generation/generation";
    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");
    private static final int MAX_RETRIES = 2;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    @Value("${llm.providers.tongyi.api-key:}")
    private String defaultApiKey;

    public TTSResult synthesize(TTSRequest request) {
        String apiKey = resolveApiKey(request);

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("No API key for Bailian TTS, returning demo result");
            return TTSResult.builder()
                    .voiceUrl("")
                    .text(request.getText())
                    .isDemo(true)
                    .build();
        }

        Exception lastException = null;
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                if (attempt > 0) {
                    long delay = (long) Math.pow(2, attempt) * 1000;
                    log.info("Retrying Bailian TTS API (attempt {}/{}), waiting {}ms", attempt + 1, MAX_RETRIES + 1, delay);
                    Thread.sleep(delay);
                }

                return doSynthesize(request, apiKey);
            } catch (RuntimeException e) {
                lastException = e;
                if (isRetryable(e)) {
                    log.warn("Bailian TTS API call failed (attempt {}/{}): {}", attempt + 1, MAX_RETRIES + 1, e.getMessage());
                } else {
                    throw e;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("百炼TTS API调用被中断", e);
            }
        }

        throw new RuntimeException("百炼TTS API调用失败，已重试" + MAX_RETRIES + "次: " + (lastException != null ? lastException.getMessage() : "未知错误"), lastException);
    }

    private String resolveApiKey(TTSRequest request) {
        if (request.getApiKey() != null && !request.getApiKey().isBlank()) {
            return request.getApiKey();
        }
        return defaultApiKey;
    }

    private boolean isRetryable(Exception e) {
        if (e instanceof RuntimeException) {
            String msg = e.getMessage();
            return msg != null && (msg.contains("HTTP 429") || msg.contains("HTTP 500") || msg.contains("HTTP 502") || msg.contains("HTTP 503"));
        }
        return false;
    }

    private TTSResult doSynthesize(TTSRequest request, String apiKey) {
        try {
            String requestBody = buildRequestBody(request);
            log.info("Calling Bailian TTS API: model={}, voice={}, languageType={}, textLength={}",
                    request.getModel(), request.getVoice(), request.getLanguageType(),
                    request.getText() != null ? request.getText().length() : 0);

            Request httpRequest = new Request.Builder()
                    .url(BASE_URL + TTS_PATH)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, JSON_TYPE))
                    .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    String maskedError = maskSensitiveInfo(errorBody);
                    log.error("Bailian TTS API error: {} - {}", response.code(), maskedError);
                    throw new RuntimeException("百炼TTS API调用失败 (HTTP " + response.code() + "): " + maskedError);
                }

                String responseBody = response.body().string();
                return parseResponse(responseBody, request.getText());
            }
        } catch (IOException e) {
            log.error("Bailian TTS API call failed: {}", e.getMessage());
            throw new RuntimeException("百炼TTS API调用异常: " + e.getMessage(), e);
        }
    }

    private String buildRequestBody(TTSRequest request) throws IOException {
        String model = request.getModel() != null ? request.getModel() : "qwen3-tts-flash";
        String voice = request.getVoice() != null ? request.getVoice() : "Cherry";
        String languageType = request.getLanguageType() != null ? request.getLanguageType() : "Auto";
        String text = request.getText() != null ? request.getText() : "";

        return objectMapper.writeValueAsString(Map.of(
                "model", model,
                "input", Map.of(
                        "text", text,
                        "voice", voice,
                        "language_type", languageType
                )
        ));
    }

    private TTSResult parseResponse(String responseBody, String originalText) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);

        String requestId = root.path("request_id").asText("");
        JsonNode output = root.path("output");
        String finishReason = output.path("finish_reason").asText("");

        JsonNode audioNode = output.path("audio");
        String audioUrl = audioNode.path("url").asText("");

        if (audioUrl.isEmpty()) {
            JsonNode errorCode = root.path("error").path("code");
            JsonNode errorMessage = root.path("error").path("message");
            if (!errorCode.isMissingNode()) {
                log.error("Bailian TTS API error: code={}, message={}", errorCode.asText(), errorMessage.asText());
                throw new RuntimeException("百炼TTS API错误: " + errorCode.asText() + " - " + errorMessage.asText());
            }
            log.error("Bailian TTS API returned no audio URL. Response: {}", maskSensitiveInfo(responseBody));
            throw new RuntimeException("百炼TTS API未返回音频URL");
        }

        log.info("Bailian TTS success: requestId={}, finishReason={}, audioUrlLength={}",
                requestId, finishReason, audioUrl.length());

        return TTSResult.builder()
                .voiceUrl(audioUrl)
                .text(originalText)
                .isDemo(false)
                .requestId(requestId)
                .build();
    }

    private String maskSensitiveInfo(String text) {
        if (text == null) return null;
        return text.replaceAll("(sk-|api[_-]?key[\"']?\\s*[:=]\\s*[\"']?)[\\w-]{8,}", "$1****");
    }

    @Data
    @Builder
    public static class TTSRequest {
        private String text;
        private String voice;
        private String languageType;
        private String model;
        private String apiKey;
    }

    @Data
    @Builder
    public static class TTSResult {
        private String voiceUrl;
        private String text;
        private boolean isDemo;
        private String requestId;
    }
}
