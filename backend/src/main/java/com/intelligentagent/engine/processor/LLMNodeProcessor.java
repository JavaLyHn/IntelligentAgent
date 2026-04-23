package com.intelligentagent.engine.processor;

import com.intelligentagent.engine.llm.LLMRequest;
import com.intelligentagent.engine.llm.LLMResponse;
import com.intelligentagent.engine.llm.OpenAICompatibleProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class LLMNodeProcessor implements NodeProcessor {

    private final OpenAICompatibleProvider llmProvider;

    @Override
    public String getType() {
        return "llmNode";
    }

    @Override
    public Map<String, Object> process(Map<String, Object> inputData, Map<String, Object> config) {
        String provider = getStr(config, "provider", "deepseek");
        String model = getStr(config, "model", null);
        String systemPrompt = getStr(config, "systemPrompt", "你是一个有帮助的AI助手，请用简洁明了的方式回答问题。");
        String userPromptTemplate = getStr(config, "userPrompt", "{{input}}");
        double temperature = getDouble(config, "temperature", 0.7);
        int maxTokens = getInt(config, "maxTokens", 2000);

        String inputText = getStr(inputData, "text", getStr(inputData, "input", ""));
        String userPrompt = resolveTemplate(userPromptTemplate, inputData);

        log.info("LLM Node: provider={}, model={}, inputLength={}", provider, model, inputText.length());

        LLMRequest request = LLMRequest.builder()
                .provider(provider)
                .model(model)
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

        return output;
    }

    private String resolveTemplate(String template, Map<String, Object> data) {
        String result = template;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", String.valueOf(entry.getValue()));
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
