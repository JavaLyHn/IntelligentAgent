package com.intelligentagent.engine.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class InputNodeProcessor implements NodeProcessor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getType() {
        return "inputNode";
    }

    @Override
    public Map<String, Object> process(Map<String, Object> inputData, Map<String, Object> config) {
        Map<String, Object> output = new HashMap<>(inputData);
        output.put("text", inputData.getOrDefault("input", ""));

        List<Map<String, Object>> outputParamConfigs = parseOutputParamConfigs(config.get("outputParamConfigs"));
        if (!outputParamConfigs.isEmpty()) {
            String primaryOutputName = null;
            for (Map<String, Object> param : outputParamConfigs) {
                String name = param.get("name") != null ? param.get("name").toString() : "";
                if (!name.isEmpty() && primaryOutputName == null) {
                    primaryOutputName = name;
                }
            }
            if (primaryOutputName != null && !primaryOutputName.equals("text")) {
                output.put(primaryOutputName, inputData.getOrDefault("input", ""));
            }
        }

        return output;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseOutputParamConfigs(Object paramsObj) {
        if (paramsObj instanceof List) {
            return (List<Map<String, Object>>) paramsObj;
        }
        if (paramsObj instanceof String) {
            try {
                return objectMapper.readValue((String) paramsObj, List.class);
            } catch (Exception e) {
                log.warn("Failed to parse outputParamConfigs: {}", e.getMessage());
            }
        }
        return Collections.emptyList();
    }
}
