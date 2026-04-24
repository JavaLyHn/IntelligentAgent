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
public class ExecutionEvent {

    public static final String WORKFLOW_STARTED = "workflow_started";
    public static final String NODE_STARTED = "node_started";
    public static final String NODE_COMPLETED = "node_completed";
    public static final String NODE_FAILED = "node_failed";
    public static final String WORKFLOW_COMPLETED = "workflow_completed";
    public static final String WORKFLOW_FAILED = "workflow_failed";
    public static final String LOG = "log";

    private String type;
    private String nodeId;
    private String nodeType;
    private String label;
    private String status;
    private String message;
    private Long durationMs;
    private Map<String, Object> data;

    public static ExecutionEvent workflowStarted(int nodeCount, int edgeCount) {
        return ExecutionEvent.builder()
                .type(WORKFLOW_STARTED)
                .message("工作流开始执行")
                .data(Map.of("nodeCount", nodeCount, "edgeCount", edgeCount))
                .build();
    }

    public static ExecutionEvent nodeStarted(String nodeId, String nodeType, String label) {
        return ExecutionEvent.builder()
                .type(NODE_STARTED)
                .nodeId(nodeId)
                .nodeType(nodeType)
                .label(label)
                .status("RUNNING")
                .message("节点开始执行: " + label)
                .build();
    }

    public static ExecutionEvent nodeCompleted(String nodeId, String nodeType, String label,
                                                long durationMs, Map<String, Object> outputData) {
        return ExecutionEvent.builder()
                .type(NODE_COMPLETED)
                .nodeId(nodeId)
                .nodeType(nodeType)
                .label(label)
                .status("SUCCESS")
                .message("节点执行完成: " + label)
                .durationMs(durationMs)
                .data(outputData)
                .build();
    }

    public static ExecutionEvent nodeFailed(String nodeId, String nodeType, String label,
                                             long durationMs, String errorMessage) {
        return ExecutionEvent.builder()
                .type(NODE_FAILED)
                .nodeId(nodeId)
                .nodeType(nodeType)
                .label(label)
                .status("FAILED")
                .message("节点执行失败: " + label)
                .durationMs(durationMs)
                .data(Map.of("error", errorMessage))
                .build();
    }

    public static ExecutionEvent workflowCompleted(boolean success, String outputText,
                                                    String audioUrl, String ttsStatus,
                                                    String ttsError, long totalDurationMs) {
        return ExecutionEvent.builder()
                .type(success ? WORKFLOW_COMPLETED : WORKFLOW_FAILED)
                .status(success ? "SUCCESS" : "FAILED")
                .message(success ? "工作流执行完成" : "工作流执行失败")
                .durationMs(totalDurationMs)
                .data(buildResultData(success, outputText, audioUrl, ttsStatus, ttsError))
                .build();
    }

    public static ExecutionEvent log(String message) {
        return ExecutionEvent.builder()
                .type(LOG)
                .message(message)
                .build();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> buildResultData(boolean success, String outputText,
                                                        String audioUrl, String ttsStatus, String ttsError) {
        Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("success", success);
        if (outputText != null) data.put("outputText", outputText);
        if (audioUrl != null) data.put("audioUrl", audioUrl);
        if (ttsStatus != null) data.put("ttsStatus", ttsStatus);
        if (ttsError != null) data.put("ttsError", ttsError);
        return data;
    }
}
