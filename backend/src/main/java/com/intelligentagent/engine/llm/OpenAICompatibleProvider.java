package com.intelligentagent.engine.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class OpenAICompatibleProvider implements LLMProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAICompatibleProvider.class);
    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
            .build();

    @Value("${llm.providers.openai.base-url:https://api.openai.com/v1}")
    private String openaiBaseUrl;

    @Value("${llm.providers.openai.api-key:}")
    private String openaiApiKey;

    @Value("${llm.providers.openai.default-model:gpt-4o}")
    private String openaiDefaultModel;

    @Value("${llm.providers.deepseek.base-url:https://api.deepseek.com/v1}")
    private String deepseekBaseUrl;

    @Value("${llm.providers.deepseek.api-key:}")
    private String deepseekApiKey;

    @Value("${llm.providers.deepseek.default-model:deepseek-chat}")
    private String deepseekDefaultModel;

    @Value("${llm.providers.tongyi.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String tongyiBaseUrl;

    @Value("${llm.providers.tongyi.api-key:}")
    private String tongyiApiKey;

    @Value("${llm.providers.tongyi.default-model:qwen-plus}")
    private String tongyiDefaultModel;

    @Override
    public String getName() {
        return "openai-compatible";
    }

    @Override
    public LLMResponse chat(LLMRequest request) {
        String provider = request.getProvider() != null ? request.getProvider() : "openai";
        ProviderConfig config = getConfig(provider, request.getModel());

        if (config.apiKey == null || config.apiKey.isBlank()) {
            log.warn("No API key configured for provider: {}, using demo mode", provider);
            return generateDemoResponse(request);
        }

        try {
            String requestBody = buildRequestBody(request, config);
            log.info("Calling LLM API: {}/chat/completions, model={}", config.baseUrl, config.model);

            Request httpRequest = new Request.Builder()
                    .url(config.baseUrl + "/chat/completions")
                    .addHeader("Authorization", "Bearer " + config.apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, JSON_TYPE))
                    .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    log.error("LLM API error: {} - {}", response.code(), errorBody);
                    return generateDemoResponse(request);
                }

                String responseBody = response.body().string();
                return parseResponse(responseBody, provider, config.model);
            }
        } catch (IOException e) {
            log.error("LLM API call failed: {}", e.getMessage());
            return generateDemoResponse(request);
        }
    }

    private ProviderConfig getConfig(String provider, String requestModel) {
        return switch (provider.toLowerCase()) {
            case "deepseek" -> new ProviderConfig(
                    deepseekBaseUrl,
                    deepseekApiKey,
                    requestModel != null ? requestModel : deepseekDefaultModel
            );
            case "tongyi" -> new ProviderConfig(
                    tongyiBaseUrl,
                    tongyiApiKey,
                    requestModel != null ? requestModel : tongyiDefaultModel
            );
            default -> new ProviderConfig(
                    openaiBaseUrl,
                    openaiApiKey,
                    requestModel != null ? requestModel : openaiDefaultModel
            );
        };
    }

    private String buildRequestBody(LLMRequest request, ProviderConfig config) throws IOException {
        Map<String, Object> body = Map.of(
                "model", config.model,
                "messages", List.of(
                        Map.of("role", "system", "content",
                                request.getSystemPrompt() != null ? request.getSystemPrompt() : "你是一个有帮助的AI助手。"),
                        Map.of("role", "user", "content", request.getUserPrompt())
                ),
                "temperature", request.getTemperature(),
                "max_tokens", request.getMaxTokens()
        );
        return objectMapper.writeValueAsString(body);
    }

    private LLMResponse parseResponse(String responseBody, String provider, String model) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode choices = root.path("choices");

        if (choices.isArray() && !choices.isEmpty()) {
            String text = choices.get(0).path("message").path("content").asText("");
            JsonNode usage = root.path("usage");

            return LLMResponse.builder()
                    .text(text)
                    .model(model)
                    .provider(provider)
                    .promptTokens(usage.path("prompt_tokens").asInt(0))
                    .completionTokens(usage.path("completion_tokens").asInt(0))
                    .demo(false)
                    .build();
        }

        return generateDemoResponse(LLMRequest.builder().provider(provider).model(model).build());
    }

    private LLMResponse generateDemoResponse(LLMRequest request) {
        String prompt = request.getUserPrompt() != null ? request.getUserPrompt() : "";
        String demoText = String.format(
                "[演示模式] 您输入的内容: \"%s\"\n\n" +
                "这是一个演示模式下的模拟回复。请配置正确的API密钥以获取真实的大模型响应。\n\n" +
                "关于您提到的内容，我可以从以下几个方面进行分析：\n" +
                "1. 背景与概述\n2. 核心要点分析\n3. 应用场景与建议\n4. 总结与展望",
                prompt.length() > 100 ? prompt.substring(0, 100) + "..." : prompt
        );

        return LLMResponse.builder()
                .text(demoText)
                .model(request.getModel() != null ? request.getModel() : "demo")
                .provider(request.getProvider() != null ? request.getProvider() : "demo")
                .demo(true)
                .build();
    }

    private record ProviderConfig(String baseUrl, String apiKey, String model) {}
}
