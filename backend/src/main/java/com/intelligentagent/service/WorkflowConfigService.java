package com.intelligentagent.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.intelligentagent.dto.*;
import com.intelligentagent.entity.WorkflowConfig;

import java.util.List;

public interface WorkflowConfigService extends IService<WorkflowConfig> {

    WorkflowDTO createWorkflow(WorkflowDTO dto);

    WorkflowDTO updateWorkflow(String id, WorkflowDTO dto);

    WorkflowDTO getWorkflow(String id);

    List<WorkflowDTO> listWorkflows();

    IPage<WorkflowDTO> pageWorkflows(long pageNum, long pageSize, String name);

    void deleteWorkflow(String id);
}
