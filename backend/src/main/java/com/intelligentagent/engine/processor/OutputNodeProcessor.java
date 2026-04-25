package com.intelligentagent.engine.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class OutputNodeProcessor implements NodeProcessor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getType() {
        return "outputNode";
    }

    @Override
    public Map<String, Object> process(Map<String, Object> inputData, Map<String, Object> config) {
        Map<String, Object> effectiveInput = applyInputSource(inputData, config);

        Map<String, Object> resolvedParams = new LinkedHashMap<>();
        List<Map<String, Object>> outputParams = resolveOutputParams(config.get("outputParams"));

        for (Map<String, Object> param : outputParams) {
            String name = param.get("name") != null ? param.get("name").toString() : "";
            String paramType = param.get("paramType") != null ? param.get("paramType").toString() : "input";
            String value = param.get("value") != null ? param.get("value").toString() : "";

            if (name.isEmpty()) continue;

            if ("reference".equals(paramType) && !value.isEmpty()) {
                Object resolved = resolveReference(value, effectiveInput);
                resolvedParams.put(name, resolved);
            } else {
                resolvedParams.put(name, value);
            }
        }

        String answerTemplate = config.get("answerTemplate") != null ? config.get("answerTemplate").toString() : "";
        String outputText;

        if (answerTemplate != null && !answerTemplate.isEmpty()) {
            outputText = resolveTemplate(answerTemplate, resolvedParams);
            if (outputText.equals(answerTemplate) && effectiveInput.containsKey("text")) {
                outputText = getStr(effectiveInput, "text", "");
            }
        } else {
            outputText = getStr(effectiveInput, "text", getStr(effectiveInput, "input", ""));
        }

        String voiceUrl = extractVoiceUrl(effectiveInput);

        Map<String, Object> output = new HashMap<>();
        output.put("text", outputText);
        output.put("voice_url", voiceUrl);
        output.put("resolvedParams", resolvedParams);

        List<Map<String, Object>> outputParamConfigs = parseParamList(config.get("outputParamConfigs"));
        if (!outputParamConfigs.isEmpty()) {
            for (Map<String, Object> param : outputParamConfigs) {
                String name = param.get("name") != null ? param.get("name").toString() : "";
                if (!name.isEmpty() && !output.containsKey(name)) {
                    output.put(name, outputText);
                }
            }
        }

        return output;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> applyInputSource(Map<String, Object> inputData, Map<String, Object> config) {
        String inputSource = config.get("inputSource") != null ? config.get("inputSource").toString() : "";

        if (inputSource.isEmpty()) {
            return inputData;
        }

        Object nodeOutput = inputData.get("node_" + inputSource);
        if (nodeOutput instanceof Map) {
            Map<String, Object> filtered = new HashMap<>(inputData);
            Map<String, Object> sourceOutput = (Map<String, Object>) nodeOutput;
            for (Map.Entry<String, Object> entry : sourceOutput.entrySet()) {
                filtered.put(entry.getKey(), entry.getValue());
            }
            log.info("Output node using inputSource: {}, fields: {}", inputSource, sourceOutput.keySet());
            return filtered;
        }

        log.warn("Output node inputSource '{}' not found in inputData, using all inputs", inputSource);
        return inputData;
    }

    @SuppressWarnings("unchecked")
    private String extractVoiceUrl(Map<String, Object> inputData) {
        if (inputData.containsKey("voice_url") && inputData.get("voice_url") != null) {
            return inputData.get("voice_url").toString();
        }
        if (inputData.containsKey("audioUrl") && inputData.get("audioUrl") != null) {
            return inputData.get("audioUrl").toString();
        }
        for (Map.Entry<String, Object> entry : inputData.entrySet()) {
            if (entry.getKey().startsWith("node_") && entry.getValue() instanceof Map) {
                Map<String, Object> nodeOutput = (Map<String, Object>) entry.getValue();
                if (nodeOutput.containsKey("voice_url") && nodeOutput.get("voice_url") != null) {
                    return nodeOutput.get("voice_url").toString();
                }
            }
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> resolveOutputParams(Object paramsObj) {
        if (paramsObj instanceof List) {
            return (List<Map<String, Object>>) paramsObj;
        }
        if (paramsObj instanceof String) {
            try {
                return objectMapper.readValue((String) paramsObj, List.class);
            } catch (Exception e) {
                log.warn("Failed to parse outputParams: {}", e.getMessage());
            }
        }
        return Collections.emptyList();
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
                log.warn("Failed to parse paramList: {}", e.getMessage());
            }
        }
        return Collections.emptyList();
    }

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

            if (inputData.containsKey(reference)) {
                return inputData.get(reference);
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

    private String resolveTemplate(String template, Map<String, Object> data) {
        String result = template;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (entry.getValue() != null) {
                result = result.replace("{{" + entry.getKey() + "}}", String.valueOf(entry.getValue()));
            }
        }
        return result;
    }

    private String getStr(Map<String, Object> map, String key, String defaultVal) {
        Object val = map.get(key);
        return val != null ? val.toString() : defaultVal;
    }
}
