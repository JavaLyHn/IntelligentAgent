package com.intelligentagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intelligentagent.dto.*;
import com.intelligentagent.engine.DAGWorkflowEngine;
import com.intelligentagent.engine.model.*;
import com.intelligentagent.entity.ExecutionRecord;
import com.intelligentagent.entity.ExecutionRecord.ExecutionStatus;
import com.intelligentagent.entity.WorkflowConfig;
import com.intelligentagent.mapper.ExecutionRecordMapper;
import com.intelligentagent.mapper.WorkflowConfigMapper;
import com.intelligentagent.service.ExecutionRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionRecordServiceImpl extends ServiceImpl<ExecutionRecordMapper, ExecutionRecord>
        implements ExecutionRecordService {

    private final WorkflowConfigMapper workflowConfigMapper;
    private final DAGWorkflowEngine dagEngine;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExecuteResponseDTO executeWorkflow(ExecuteRequestDTO request) {
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
        save(record);

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

        updateById(record);

        return ExecuteResponseDTO.builder()
                .success(result.isSuccess())
                .outputText(result.getOutputText())
                .audioUrl(result.getAudioUrl())
                .ttsStatus(result.getTtsStatus())
                .ttsError(result.getTtsError())
                .logs(result.getLogs())
                .durationMs(duration)
                .errorMessage(result.getErrorMessage())
                .build();
    }

    @Override
    public List<ExecutionRecordDTO> getExecutionRecords(String workflowId) {
        LambdaQueryWrapper<ExecutionRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExecutionRecord::getWorkflowId, workflowId)
                .orderByDesc(ExecutionRecord::getCreatedAt);
        return list(wrapper).stream().map(this::toRecordDTO).collect(Collectors.toList());
    }

    @Override
    public IPage<ExecutionRecordDTO> pageExecutionRecords(String workflowId, long pageNum, long pageSize) {
        Page<ExecutionRecord> page = new Page<>(pageNum, pageSize);
        IPage<ExecutionRecord> result = baseMapper.selectPageByWorkflowId(page, workflowId);
        return result.convert(this::toRecordDTO);
    }

    @Override
    public ExecutionRecordDTO getExecutionRecord(String id) {
        ExecutionRecord record = getById(id);
        if (record == null) {
            throw new RuntimeException("执行记录不存在: " + id);
        }
        return toRecordDTO(record);
    }

    private ExecutionRecordDTO toRecordDTO(ExecutionRecord record) {
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
                .createdAt(record.getCreatedAt() != null ? record.getCreatedAt().format(FMT) : null)
                .completedAt(record.getCompletedAt() != null ? record.getCompletedAt().format(FMT) : null)
                .build();
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
