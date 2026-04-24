package com.intelligentagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intelligentagent.dto.WorkflowDTO;
import com.intelligentagent.entity.WorkflowConfig;
import com.intelligentagent.mapper.WorkflowConfigMapper;
import com.intelligentagent.service.WorkflowConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowConfigServiceImpl extends ServiceImpl<WorkflowConfigMapper, WorkflowConfig>
        implements WorkflowConfigService {

    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowDTO createWorkflow(WorkflowDTO dto) {
        WorkflowConfig config = WorkflowConfig.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .nodesJson(serializeNodes(dto.getNodes()))
                .edgesJson(serializeEdges(dto.getEdges()))
                .active(true)
                .version(1)
                .build();
        save(config);
        log.info("Created workflow: id={}, name={}", config.getId(), config.getName());
        return toDTO(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowDTO updateWorkflow(String id, WorkflowDTO dto) {
        WorkflowConfig config = getById(id);
        if (config == null) {
            throw new RuntimeException("工作流不存在: " + id);
        }
        config.setName(dto.getName());
        config.setDescription(dto.getDescription());
        config.setNodesJson(serializeNodes(dto.getNodes()));
        config.setEdgesJson(serializeEdges(dto.getEdges()));
        updateById(config);
        log.info("Updated workflow: id={}, name={}", id, config.getName());
        return toDTO(config);
    }

    @Override
    public WorkflowDTO getWorkflow(String id) {
        WorkflowConfig config = getById(id);
        if (config == null) {
            throw new RuntimeException("工作流不存在: " + id);
        }
        return toDTO(config);
    }

    @Override
    public List<WorkflowDTO> listWorkflows() {
        LambdaQueryWrapper<WorkflowConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(WorkflowConfig::getUpdatedAt);
        List<WorkflowConfig> configs = list(wrapper);
        log.info("Listed workflows: count={}", configs.size());
        return configs.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public IPage<WorkflowDTO> pageWorkflows(long pageNum, long pageSize, String name) {
        Page<WorkflowConfig> page = new Page<>(pageNum, pageSize);
        IPage<WorkflowConfig> result = baseMapper.selectPageActive(page, name);
        return result.convert(this::toDTO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWorkflow(String id) {
        WorkflowConfig config = getById(id);
        if (config == null) {
            log.warn("Delete workflow failed: not found, id={}", id);
            throw new RuntimeException("工作流不存在: " + id);
        }
        String name = config.getName();
        boolean removed = removeById(id);
        if (removed) {
            log.info("Deleted workflow: id={}, name={}", id, name);
        } else {
            log.error("Delete workflow failed: removeById returned false, id={}", id);
            throw new RuntimeException("工作流删除失败: " + id);
        }
    }

    private WorkflowDTO toDTO(WorkflowConfig config) {
        return WorkflowDTO.builder()
                .id(config.getId())
                .name(config.getName())
                .description(config.getDescription())
                .nodes(deserializeNodes(config.getNodesJson()))
                .edges(deserializeEdges(config.getEdgesJson()))
                .version(config.getVersion())
                .createdAt(config.getCreatedAt() != null ? config.getCreatedAt().format(FMT) : null)
                .updatedAt(config.getUpdatedAt() != null ? config.getUpdatedAt().format(FMT) : null)
                .build();
    }

    private String serializeNodes(List<WorkflowDTO.NodeDTO> nodes) {
        try {
            return objectMapper.writeValueAsString(nodes);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化节点数据失败", e);
        }
    }

    private String serializeEdges(List<WorkflowDTO.EdgeDTO> edges) {
        try {
            return objectMapper.writeValueAsString(edges);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化边数据失败", e);
        }
    }

    private List<WorkflowDTO.NodeDTO> deserializeNodes(String json) {
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, WorkflowDTO.NodeDTO.class));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("反序列化节点数据失败", e);
        }
    }

    private List<WorkflowDTO.EdgeDTO> deserializeEdges(String json) {
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, WorkflowDTO.EdgeDTO.class));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("反序列化边数据失败", e);
        }
    }
}
