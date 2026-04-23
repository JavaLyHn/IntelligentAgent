package com.intelligentagent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intelligentagent.dto.*;
import com.intelligentagent.entity.ExecutionRecord;
import com.intelligentagent.entity.ExecutionRecord.ExecutionStatus;
import com.intelligentagent.entity.WorkflowConfig;
import com.intelligentagent.engine.DAGWorkflowEngine;
import com.intelligentagent.engine.model.*;
import com.intelligentagent.repository.ExecutionRecordRepository;
import com.intelligentagent.repository.WorkflowConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final WorkflowConfigRepository workflowConfigRepository;
    private final ExecutionRecordRepository executionRecordRepository;
    private final DAGWorkflowEngine dagEngine;
    private final ObjectMapper objectMapper;

    public WorkflowDTO createWorkflow(WorkflowDTO dto) {
        try {
            WorkflowConfig config = WorkflowConfig.builder()
                    .name(dto.getName())
                    .description(dto.getDescription())
                    .nodesJson(objectMapper.writeValueAsString(dto.getNodes()))
                    .edgesJson(objectMapper.writeValueAsString(dto.getEdges()))
                    .active(true)
                    .version(1)
                    .createdAt(LocalDateTime.now())
                    .build();

            config = workflowConfigRepository.save(config);
            return toDTO(config);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化失败", e);
        }
    }

    public WorkflowDTO updateWorkflow(String id, WorkflowDTO dto) {
        WorkflowConfig config = workflowConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("工作流不存在: " + id));

        try {
            config.setName(dto.getName());
            config.setDescription(dto.getDescription());
            config.setNodesJson(objectMapper.writeValueAsString(dto.getNodes()));
            config.setEdgesJson(objectMapper.writeValueAsString(dto.getEdges()));
            config.setVersion(config.getVersion() + 1);
            config.setUpdatedAt(LocalDateTime.now());

            config = workflowConfigRepository.save(config);
            return toDTO(config);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化失败", e);
        }
    }

    public WorkflowDTO getWorkflow(String id) {
        WorkflowConfig config = workflowConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("工作流不存在: " + id));
        return toDTO(config);
    }

    public List<WorkflowDTO> listWorkflows() {
        return workflowConfigRepository.findByActiveTrueOrderByUpdatedAtDesc()
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public void deleteWorkflow(String id) {
        WorkflowConfig config = workflowConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("工作流不存在: " + id));
        config.setActive(false);
        config.setUpdatedAt(LocalDateTime.now());
        workflowConfigRepository.save(config);
    }

    public ExecuteResponseDTO executeWorkflow(ExecuteRequestDTO request) {
        String input = request.getInput() != null ? request.getInput() : "";
        String workflowId = request.getWorkflowId();

        if (workflowId != null) {
            WorkflowConfig config = workflowConfigRepository.findById(workflowId).orElse(null);
            if (config != null) {
                config.setLastExecutedAt(LocalDateTime.now());
                workflowConfigRepository.save(config);
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
                .createdAt(LocalDateTime.now())
                .build();
        record = executionRecordRepository.save(record);

        long startTime = System.currentTimeMillis();
        WorkflowExecutionResult result = dagEngine.execute(definition, input);
        long duration = System.currentTimeMillis() - startTime;

        record.setStatus(result.isSuccess() ? ExecutionStatus.SUCCESS : ExecutionStatus.FAILED);
        record.setOutputText(result.getOutputText());
        record.setAudioUrl(result.getAudioUrl());
        record.setDurationMs(duration);
        record.setErrorMessage(result.getErrorMessage());
        record.setCompletedAt(LocalDateTime.now());

        if (result.getLogs() != null) {
            record.setExecutionLog(String.join("\n", result.getLogs()));
        }

        executionRecordRepository.save(record);

        return ExecuteResponseDTO.builder()
                .success(result.isSuccess())
                .outputText(result.getOutputText())
                .audioUrl(result.getAudioUrl())
                .logs(result.getLogs())
                .durationMs(duration)
                .errorMessage(result.getErrorMessage())
                .build();
    }

    public List<ExecutionRecordDTO> getExecutionRecords(String workflowId) {
        return executionRecordRepository.findByWorkflowIdOrderByCreatedAtDesc(workflowId)
                .stream().map(this::toRecordDTO).collect(Collectors.toList());
    }

    public ExecutionRecordDTO getExecutionRecord(String id) {
        ExecutionRecord record = executionRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("执行记录不存在: " + id));
        return toRecordDTO(record);
    }

    private WorkflowDTO toDTO(WorkflowConfig config) {
        try {
            List<WorkflowDTO.NodeDTO> nodes = objectMapper.readValue(
                    config.getNodesJson(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, WorkflowDTO.NodeDTO.class)
            );
            List<WorkflowDTO.EdgeDTO> edges = objectMapper.readValue(
                    config.getEdgesJson(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, WorkflowDTO.EdgeDTO.class)
            );

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            return WorkflowDTO.builder()
                    .id(config.getId())
                    .name(config.getName())
                    .description(config.getDescription())
                    .nodes(nodes)
                    .edges(edges)
                    .version(config.getVersion())
                    .createdAt(config.getCreatedAt() != null ? config.getCreatedAt().format(fmt) : null)
                    .updatedAt(config.getUpdatedAt() != null ? config.getUpdatedAt().format(fmt) : null)
                    .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("反序列化失败", e);
        }
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

    private ExecutionRecordDTO toRecordDTO(ExecutionRecord record) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return ExecutionRecordDTO.builder()
                .id(record.getId())
                .workflowId(record.getWorkflowId())
                .status(record.getStatus().name())
                .inputText(record.getInputText())
                .outputText(record.getOutputText())
                .audioUrl(record.getAudioUrl())
                .executionLog(record.getExecutionLog())
                .durationMs(record.getDurationMs())
                .errorMessage(record.getErrorMessage())
                .createdAt(record.getCreatedAt() != null ? record.getCreatedAt().format(fmt) : null)
                .completedAt(record.getCompletedAt() != null ? record.getCompletedAt().format(fmt) : null)
                .build();
    }
}
