package com.intelligentagent.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutionRecordDTO {

    private String id;
    private String workflowId;
    private String status;
    private String inputText;
    private String outputText;
    private String audioUrl;
    private String executionLog;
    private Long durationMs;
    private String errorMessage;
    private String createdAt;
    private String completedAt;
}
