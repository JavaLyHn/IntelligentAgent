package com.intelligentagent.engine.processor;

import com.intelligentagent.engine.model.NodeContext;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

@Component
public class InputNodeProcessor implements NodeProcessor {

    @Override
    public String getType() {
        return "inputNode";
    }

    @Override
    public Map<String, Object> process(Map<String, Object> inputData, Map<String, Object> config) {
        Map<String, Object> output = new HashMap<>(inputData);
        output.put("text", inputData.getOrDefault("input", ""));
        return output;
    }
}
