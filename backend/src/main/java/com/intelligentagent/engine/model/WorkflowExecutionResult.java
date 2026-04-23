package com.intelligentagent.engine.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowExecutionResult {

    private boolean success;
    private String outputText;
    private String audioUrl;
    private List<String> logs;
    private Map<String, NodeContext> nodeContexts;
    private long totalDurationMs;
    private String errorMessage;
}
