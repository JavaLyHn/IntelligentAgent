package com.intelligentagent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intelligentagent.dto.ExecuteRequestDTO;
import com.intelligentagent.engine.DAGWorkflowEngine;
import com.intelligentagent.engine.model.ExecutionEvent;
import com.intelligentagent.engine.model.WorkflowDefinition;
import com.intelligentagent.engine.model.WorkflowEdge;
import com.intelligentagent.engine.model.WorkflowNode;
import com.intelligentagent.dto.WorkflowDTO;
import com.intelligentagent.entity.ExecutionRecord;
import com.intelligentagent.entity.ExecutionRecord.ExecutionStatus;
import com.intelligentagent.entity.WorkflowConfig;
import com.intelligentagent.mapper.WorkflowConfigMapper;
import com.intelligentagent.service.ExecutionRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class WorkflowExecuteController {

    private final DAGWorkflowEngine dagEngine;
    private final ExecutionRecordService executionRecordService;
    private final WorkflowConfigMapper workflowConfigMapper;
    private final ObjectMapper objectMapper;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    @PostMapping(value = "/workflow/execute/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter executeWorkflowStream(@RequestBody ExecuteRequestDTO request) {
        SseEmitter emitter = new SseEmitter(300_000L);

        emitter.onCompletion(() -> log.debug("SSE connection completed"));
        emitter.onTimeout(() -> {
            log.warn("SSE connection timed out");
            emitter.complete();
        });
        emitter.onError(e -> {
            log.warn("SSE connection error: {}", e.getMessage());
            emitter.complete();
        });

        executor.submit(() -> {
            try {
                String input = request.getInput() != null ? request.getInput() : "";
                String workflowId = request.getWorkflowId();

                if (workflowId != null) {
                    WorkflowConfig config = workflowConfigMapper.selectById(workflowId);
                    if (config != null) {
                        config.setLastExecutedAt(LocalDateTime.now());
                        workflowConfigMapper.updateById(config);
                    }
                }

                List<WorkflowNode> nodes = request.getNodes() != null
                        ? request.getNodes().stream().map(this::toNode).collect(Collectors.toList())
                        : List.of();
                List<WorkflowEdge> edges = request.getEdges() != null
                        ? request.getEdges().stream().map(this::toEdge).collect(Collectors.toList())
                        : List.of();

                WorkflowDefinition definition = WorkflowDefinition.builder()
                        .nodes(nodes)
                        .edges(edges)
                        .build();

                ExecutionRecord record = ExecutionRecord.builder()
                        .workflowId(workflowId != null ? workflowId : "anonymous")
                        .status(ExecutionStatus.RUNNING)
                        .inputText(input)
                        .build();
                executionRecordService.save(record);

                long startTime = System.currentTimeMillis();

                dagEngine.execute(definition, input, event -> {
                    try {
                        String jsonData = objectMapper.writeValueAsString(event);
                        emitter.send(SseEmitter.event()
                                .name(event.getType())
                                .data(jsonData, MediaType.APPLICATION_JSON));
                    } catch (Exception e) {
                        log.warn("Failed to send SSE event: {}", e.getMessage());
                    }
                });

                long duration = System.currentTimeMillis() - startTime;
                record.setDurationMs(duration);
                record.setCompletedAt(LocalDateTime.now());

                record.setStatus(ExecutionStatus.SUCCESS);
                executionRecordService.updateById(record);

                emitter.complete();
            } catch (Exception e) {
                log.error("Workflow execution failed in SSE: {}", e.getMessage());
                try {
                    ExecutionEvent failEvent = ExecutionEvent.builder()
                            .type(ExecutionEvent.WORKFLOW_FAILED)
                            .status("FAILED")
                            .message("工作流执行失败: " + e.getMessage())
                            .build();
                    String jsonData = objectMapper.writeValueAsString(failEvent);
                    emitter.send(SseEmitter.event()
                            .name(ExecutionEvent.WORKFLOW_FAILED)
                            .data(jsonData, MediaType.APPLICATION_JSON));
                } catch (Exception ignored) {}
                emitter.complete();
            }
        });

        return emitter;
    }

    private WorkflowNode toNode(WorkflowDTO.NodeDTO dto) {
        return WorkflowNode.builder()
                .id(dto.getId())
                .type(dto.getType())
                .position(WorkflowNode.Position.builder()
                        .x(dto.getPosition().getX())
                        .y(dto.getPosition().getY())
                        .build())
                .data(WorkflowNode.NodeData.builder()
                        .label(dto.getData().getLabel())
                        .type(dto.getData().getType())
                        .category(dto.getData().getCategory())
                        .config(dto.getData().getConfig())
                        .build())
                .build();
    }

    private WorkflowEdge toEdge(WorkflowDTO.EdgeDTO dto) {
        return WorkflowEdge.builder()
                .id(dto.getId())
                .source(dto.getSource())
                .target(dto.getTarget())
                .sourceHandle(dto.getSourceHandle())
                .targetHandle(dto.getTargetHandle())
                .build();
    }
}
