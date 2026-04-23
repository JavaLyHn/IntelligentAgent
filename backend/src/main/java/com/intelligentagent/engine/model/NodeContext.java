package com.intelligentagent.engine.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NodeContext {

    private String nodeId;
    private String nodeType;
    private String label;
    private Map<String, Object> inputData;
    private Map<String, Object> outputData;
    private long executionTimeMs;
    private String status;
    private String errorMessage;
}
