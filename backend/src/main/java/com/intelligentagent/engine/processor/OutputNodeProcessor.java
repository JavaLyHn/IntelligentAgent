package com.intelligentagent.engine.processor;

import com.intelligentagent.engine.model.NodeContext;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

@Component
public class OutputNodeProcessor implements NodeProcessor {

    @Override
    public String getType() {
        return "outputNode";
    }

    @Override
    public Map<String, Object> process(Map<String, Object> inputData, Map<String, Object> config) {
        Map<String, Object> output = new HashMap<>();
        output.put("text", inputData.getOrDefault("text", inputData.getOrDefault("input", "")));
        output.put("audioUrl", inputData.get("audioUrl"));
        return output;
    }
}
