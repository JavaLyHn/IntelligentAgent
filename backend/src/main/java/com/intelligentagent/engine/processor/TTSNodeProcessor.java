package com.intelligentagent.engine.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intelligentagent.engine.llm.BailianTTSProvider;
import com.intelligentagent.engine.util.AudioMerger;
import com.intelligentagent.engine.util.TextSegmenter;
import com.intelligentagent.service.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class TTSNodeProcessor implements NodeProcessor {

    private final BailianTTSProvider ttsProvider;
    private final MinioService minioService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getType() {
        return "toolNode";
    }

    @Override
    public Map<String, Object> process(Map<String, Object> inputData, Map<String, Object> config) {
        Map<String, Object> resolvedParams = resolveInputParams(config.get("inputParams"), inputData);

        String text = getResolvedStr(resolvedParams, "text", getStr(inputData, "text", getStr(inputData, "input", "")));
        String voice = getResolvedStr(resolvedParams, "voice", "Cherry");
        String languageType = getResolvedStr(resolvedParams, "language_type", "Auto");
        String apiKey = getResolvedStr(resolvedParams, "api_key", getStr(config, "apiKey", null));
        String model = getResolvedStr(resolvedParams, "model", getStr(config, "model", "qwen3-tts-flash"));

        boolean hasApiKey = apiKey != null && !apiKey.isBlank();
        log.info("TTS Node: model={}, voice={}, languageType={}, textLength={}, hasApiKey={}",
                model, voice, languageType, text.length(), hasApiKey);

        String voiceUrl = "";
        boolean isDemo = true;
        String ttsStatus = "skipped";
        String ttsError = "";

        if (!hasApiKey) {
            ttsStatus = "no_api_key";
            ttsError = "未配置API密钥，无法调用百炼TTS服务";
            log.warn("TTS Node: no API key configured");
        } else if (text == null || text.isBlank()) {
            ttsStatus = "no_text";
            ttsError = "输入文本为空，跳过TTS调用";
            log.warn("TTS Node: no text input");
        } else {
            try {
                List<String> segments = TextSegmenter.segment(text);
                log.info("Text segmented into {} parts (original length: {})", segments.size(), text.length());

                List<String> audioUrls = new ArrayList<>();
                for (int i = 0; i < segments.size(); i++) {
                    String segment = segments.get(i);
                    log.info("Calling TTS for segment {}/{}: length={}", i + 1, segments.size(), segment.length());

                    BailianTTSProvider.TTSRequest ttsRequest = BailianTTSProvider.TTSRequest.builder()
                            .text(segment)
                            .voice(voice)
                            .languageType(mapLanguageType(languageType))
                            .model(model)
                            .apiKey(apiKey)
                            .build();

                    BailianTTSProvider.TTSResult ttsResult = ttsProvider.synthesize(ttsRequest);
                    if (ttsResult.isDemo() || ttsResult.getVoiceUrl() == null || ttsResult.getVoiceUrl().isEmpty()) {
                        throw new RuntimeException("TTS第" + (i + 1) + "段未返回音频URL");
                    }
                    audioUrls.add(ttsResult.getVoiceUrl());
                    log.info("Segment {}/{} TTS success", i + 1, segments.size());
                }

                List<byte[]> audioDataList = new ArrayList<>();
                for (int i = 0; i < audioUrls.size(); i++) {
                    log.info("Downloading audio segment {}/{}...", i + 1, audioUrls.size());
                    byte[] audioData = minioService.downloadBytes(audioUrls.get(i));
                    audioDataList.add(audioData);
                    log.info("Downloaded segment {}/{}: {} bytes", i + 1, audioUrls.size(), audioData.length);
                }

                byte[] mergedAudio = AudioMerger.mergeAudioFiles(audioDataList);
                log.info("Merged audio: {} bytes", mergedAudio.length);

                String contentType = AudioMerger.detectContentType(mergedAudio);
                String minioUrl = minioService.uploadBytes(mergedAudio, "tts-audio", contentType);
                voiceUrl = minioUrl;
                isDemo = false;
                ttsStatus = "success";
                log.info("TTS synthesis completed: merged {} segments, uploaded to MinIO: {}", segments.size(), minioUrl);
            } catch (Exception e) {
                ttsStatus = "error";
                ttsError = e.getMessage() != null ? e.getMessage() : "未知错误";
                log.error("TTS synthesis failed: {}", ttsError);
            }
        }

        Map<String, Object> output = new HashMap<>();
        output.put("text", text);
        output.put("voice_url", voiceUrl);
        output.put("voice", voice);
        output.put("languageType", languageType);
        output.put("model", model);
        output.put("isDemo", isDemo);
        output.put("ttsStatus", ttsStatus);
        output.put("ttsError", ttsError);

        return output;
    }

    private String mapLanguageType(String languageType) {
        if (languageType == null || languageType.isBlank() || "Auto".equalsIgnoreCase(languageType)) {
            return "Chinese";
        }
        return languageType;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveInputParams(Object inputParamsObj, Map<String, Object> inputData) {
        Map<String, Object> resolved = new LinkedHashMap<>();
        List<Map<String, Object>> inputParams = parseParamList(inputParamsObj);

        for (Map<String, Object> param : inputParams) {
            String name = param.get("name") != null ? param.get("name").toString() : "";
            String paramType = param.get("paramType") != null ? param.get("paramType").toString() : "reference";
            String value = param.get("value") != null ? param.get("value").toString() : "";

            if (name.isEmpty()) continue;

            if ("reference".equals(paramType) && !value.isEmpty()) {
                Object resolvedValue = resolveReference(value, inputData);
                resolved.put(name, resolvedValue);
            } else if ("input".equals(paramType)) {
                resolved.put(name, value);
            } else {
                if (inputData.containsKey(name)) {
                    resolved.put(name, inputData.get(name));
                } else if (!value.isEmpty()) {
                    resolved.put(name, value);
                }
            }
        }

        return resolved;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseParamList(Object paramsObj) {
        if (paramsObj instanceof List) {
            return (List<Map<String, Object>>) paramsObj;
        }
        if (paramsObj instanceof String) {
            try {
                return objectMapper.readValue((String) paramsObj, List.class);
            } catch (Exception e) {
                log.warn("Failed to parse inputParams: {}", e.getMessage());
            }
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private Object resolveReference(String reference, Map<String, Object> inputData) {
        if (reference.contains(".")) {
            String[] parts = reference.split("\\.", 2);
            String nodeId = parts[0];
            String field = parts[1];

            Object nodeOutput = inputData.get("node_" + nodeId);
            if (nodeOutput instanceof Map) {
                Object val = ((Map<String, Object>) nodeOutput).get(field);
                if (val != null) return val;
            }
        }

        if (inputData.containsKey(reference)) {
            return inputData.get(reference);
        }

        Object directText = inputData.get("text");
        if (directText != null && !directText.toString().isEmpty()) {
            return directText;
        }

        return "";
    }

    private String getStr(Map<String, Object> map, String key, String defaultVal) {
        Object val = map.get(key);
        return val != null ? val.toString() : defaultVal;
    }

    private String getResolvedStr(Map<String, Object> resolved, String key, String defaultVal) {
        Object val = resolved.get(key);
        return val != null ? val.toString() : defaultVal;
    }
}
