package com.intelligentagent.engine.processor;

import com.intelligentagent.engine.model.NodeContext;
import java.util.Map;

public interface NodeProcessor {

    String getType();

    Map<String, Object> process(Map<String, Object> inputData, Map<String, Object> config);
}
