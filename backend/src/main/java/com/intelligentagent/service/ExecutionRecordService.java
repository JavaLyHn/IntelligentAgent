package com.intelligentagent.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.intelligentagent.dto.ExecuteRequestDTO;
import com.intelligentagent.dto.ExecuteResponseDTO;
import com.intelligentagent.dto.ExecutionRecordDTO;
import com.intelligentagent.entity.ExecutionRecord;

import java.util.List;

public interface ExecutionRecordService extends IService<ExecutionRecord> {

    ExecuteResponseDTO executeWorkflow(ExecuteRequestDTO request);

    List<ExecutionRecordDTO> getExecutionRecords(String workflowId);

    IPage<ExecutionRecordDTO> pageExecutionRecords(String workflowId, long pageNum, long pageSize);

    ExecutionRecordDTO getExecutionRecord(String id);
}
