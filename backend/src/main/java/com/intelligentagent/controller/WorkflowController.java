package com.intelligentagent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.intelligentagent.dto.*;
import com.intelligentagent.service.ExecutionRecordService;
import com.intelligentagent.service.WorkflowConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowConfigService workflowConfigService;
    private final ExecutionRecordService executionRecordService;

    @PostMapping("/workflow/execute")
    public ApiResponse<ExecuteResponseDTO> executeWorkflow(@RequestBody ExecuteRequestDTO request) {
        ExecuteResponseDTO result = executionRecordService.executeWorkflow(request);
        return ApiResponse.ok(result);
    }

    @PostMapping("/workflows")
    public ApiResponse<WorkflowDTO> createWorkflow(@RequestBody WorkflowDTO dto) {
        WorkflowDTO created = workflowConfigService.createWorkflow(dto);
        return ApiResponse.ok(created, "工作流创建成功");
    }

    @PutMapping("/workflows/{id}")
    public ApiResponse<WorkflowDTO> updateWorkflow(@PathVariable String id, @RequestBody WorkflowDTO dto) {
        WorkflowDTO updated = workflowConfigService.updateWorkflow(id, dto);
        return ApiResponse.ok(updated, "工作流更新成功");
    }

    @GetMapping("/workflows/{id}")
    public ApiResponse<WorkflowDTO> getWorkflow(@PathVariable String id) {
        WorkflowDTO workflow = workflowConfigService.getWorkflow(id);
        return ApiResponse.ok(workflow);
    }

    @GetMapping("/workflows")
    public ApiResponse<List<WorkflowDTO>> listWorkflows(
            @RequestParam(required = false) String name) {
        List<WorkflowDTO> workflows = workflowConfigService.listWorkflows();
        return ApiResponse.ok(workflows);
    }

    @GetMapping("/workflows/page")
    public ApiResponse<IPage<WorkflowDTO>> pageWorkflows(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String name) {
        IPage<WorkflowDTO> page = workflowConfigService.pageWorkflows(pageNum, pageSize, name);
        return ApiResponse.ok(page);
    }

    @DeleteMapping("/workflows/{id}")
    public ApiResponse<Void> deleteWorkflow(@PathVariable String id) {
        workflowConfigService.deleteWorkflow(id);
        return ApiResponse.ok(null, "工作流删除成功");
    }

    @GetMapping("/workflows/{workflowId}/executions")
    public ApiResponse<List<ExecutionRecordDTO>> getExecutionRecords(@PathVariable String workflowId) {
        List<ExecutionRecordDTO> records = executionRecordService.getExecutionRecords(workflowId);
        return ApiResponse.ok(records);
    }

    @GetMapping("/workflows/{workflowId}/executions/page")
    public ApiResponse<IPage<ExecutionRecordDTO>> pageExecutionRecords(
            @PathVariable String workflowId,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        IPage<ExecutionRecordDTO> page = executionRecordService.pageExecutionRecords(workflowId, pageNum, pageSize);
        return ApiResponse.ok(page);
    }

    @GetMapping("/executions/{id}")
    public ApiResponse<ExecutionRecordDTO> getExecutionRecord(@PathVariable String id) {
        ExecutionRecordDTO record = executionRecordService.getExecutionRecord(id);
        return ApiResponse.ok(record);
    }

    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.ok("Intelligent Agent Backend is running");
    }
}
