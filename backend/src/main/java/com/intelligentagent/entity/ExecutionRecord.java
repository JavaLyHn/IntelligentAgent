package com.intelligentagent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.experimental.SuperBuilder;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@TableName(value = "execution_records")
public class ExecutionRecord extends BaseEntity {

    private String workflowId;

    @TableField(value = "status")
    private ExecutionStatus status;

    private String inputText;

    private String outputText;

    private String audioUrl;

    private String executionLog;

    private String nodeResultsJson;

    private Long durationMs;

    private String errorMessage;

    private LocalDateTime completedAt;

    public enum ExecutionStatus {
        RUNNING, SUCCESS, FAILED
    }
}
