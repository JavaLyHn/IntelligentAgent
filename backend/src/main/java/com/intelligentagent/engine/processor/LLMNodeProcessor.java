package com.intelligentagent.engine.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intelligentagent.engine.llm.LLMRequest;
import com.intelligentagent.engine.llm.LLMResponse;
import com.intelligentagent.engine.llm.OpenAICompatibleProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class LLMNodeProcessor implements NodeProcessor {

    private final OpenAICompatibleProvider llmProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getType() {
        return "llmNode";
    }

    @Override
    public Map<String, Object> process(Map<String, Object> inputData, Map<String, Object> config) {
        String provider = getStr(config, "provider", "deepseek");
        String model = getStr(config, "model", null);
        String baseUrl = getStr(config, "baseUrl", null);
        String apiKey = getStr(config, "apiKey", null);
        String systemPrompt = getStr(config, "systemPrompt", "你是一个有帮助的AI助手，请用简洁明了的方式回答问题。");
        String userPromptTemplate = getStr(config, "userPrompt", "{{input}}");
        double temperature = getDouble(config, "temperature", 0.7);
        int maxTokens = getInt(config, "maxTokens", 2000);

        Map<String, Object> resolvedParams = resolveInputParams(config.get("inputParams"), inputData);

        Map<String, Object> templateData = new HashMap<>(inputData);
        templateData.putAll(resolvedParams);

        String userPrompt = resolveTemplate(userPromptTemplate, templateData);

        log.info("LLM Node: provider={}, model={}, inputParams={}, resolvedParams={}, hasCustomBaseUrl={}, hasApiKey={}",
                provider, model, resolvedParams.keySet(), resolvedParams.values(),
                baseUrl != null && !baseUrl.isEmpty(), apiKey != null && !apiKey.isEmpty());

        LLMRequest request = LLMRequest.builder()
                .provider(provider)
                .model(model)
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .systemPrompt(systemPrompt)
                .userPrompt(userPrompt)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .build();

        LLMResponse response = llmProvider.chat(request);

        Map<String, Object> output = new HashMap<>();
        output.put("text", response.getText());
        output.put("model", response.getModel());
        output.put("provider", response.getProvider());
        output.put("isDemo", response.isDemo());
        if (response.getPromptTokens() > 0) {
            output.put("promptTokens", response.getPromptTokens());
            output.put("completionTokens", response.getCompletionTokens());
        }

        List<Map<String, Object>> outputParamConfigs = parseParamList(config.get("outputParamConfigs"));
        if (!outputParamConfigs.isEmpty()) {
            for (Map<String, Object> param : outputParamConfigs) {
                String name = param.get("name") != null ? param.get("name").toString() : "";
                if (!name.isEmpty() && !name.equals("text") && !output.containsKey(name)) {
                    output.put(name, response.getText());
                }
            }
        }

        return output;
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

    private double getDouble(Map<String, Object> map, String key, double defaultVal) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).doubleValue();
        if (val instanceof String) {
            try { return Double.parseDouble((String) val); } catch (NumberFormatException ignored) {}
        }
        return defaultVal;
    }

    private int getInt(Map<String, Object> map, String key, int defaultVal) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        if (val instanceof String) {
            try { return Integer.parseInt((String) val); } catch (NumberFormatException ignored) {}
        }
        return defaultVal;
    }
}
