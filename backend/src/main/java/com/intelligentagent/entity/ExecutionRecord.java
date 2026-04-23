package com.intelligentagent.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "execution_records")
public class ExecutionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String workflowId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ExecutionStatus status;

    @Column(columnDefinition = "TEXT")
    private String inputText;

    @Column(columnDefinition = "TEXT")
    private String outputText;

    @Column(length = 2000)
    private String audioUrl;

    @Column(columnDefinition = "TEXT")
    private String executionLog;

    @Column(columnDefinition = "TEXT")
    private String nodeResultsJson;

    private Long durationMs;

    @Column(length = 500)
    private String errorMessage;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime completedAt;

    public enum ExecutionStatus {
        RUNNING, SUCCESS, FAILED
    }
}
