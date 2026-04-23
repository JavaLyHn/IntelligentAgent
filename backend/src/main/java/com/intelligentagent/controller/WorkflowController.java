package com.intelligentagent.controller;

import com.intelligentagent.dto.*;
import com.intelligentagent.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    @PostMapping("/workflow/execute")
    public ApiResponse<ExecuteResponseDTO> executeWorkflow(@RequestBody ExecuteRequestDTO request) {
        ExecuteResponseDTO result = workflowService.executeWorkflow(request);
        return ApiResponse.ok(result);
    }

    @PostMapping("/workflows")
    public ApiResponse<WorkflowDTO> createWorkflow(@RequestBody WorkflowDTO dto) {
        WorkflowDTO created = workflowService.createWorkflow(dto);
        return ApiResponse.ok(created, "工作流创建成功");
    }

    @PutMapping("/workflows/{id}")
    public ApiResponse<WorkflowDTO> updateWorkflow(@PathVariable String id, @RequestBody WorkflowDTO dto) {
        WorkflowDTO updated = workflowService.updateWorkflow(id, dto);
        return ApiResponse.ok(updated, "工作流更新成功");
    }

    @GetMapping("/workflows/{id}")
    public ApiResponse<WorkflowDTO> getWorkflow(@PathVariable String id) {
        WorkflowDTO workflow = workflowService.getWorkflow(id);
        return ApiResponse.ok(workflow);
    }

    @GetMapping("/workflows")
    public ApiResponse<List<WorkflowDTO>> listWorkflows() {
        List<WorkflowDTO> workflows = workflowService.listWorkflows();
        return ApiResponse.ok(workflows);
    }

    @DeleteMapping("/workflows/{id}")
    public ApiResponse<Void> deleteWorkflow(@PathVariable String id) {
        workflowService.deleteWorkflow(id);
        return ApiResponse.ok(null, "工作流删除成功");
    }

    @GetMapping("/workflows/{workflowId}/executions")
    public ApiResponse<List<ExecutionRecordDTO>> getExecutionRecords(@PathVariable String workflowId) {
        List<ExecutionRecordDTO> records = workflowService.getExecutionRecords(workflowId);
        return ApiResponse.ok(records);
    }

    @GetMapping("/executions/{id}")
    public ApiResponse<ExecutionRecordDTO> getExecutionRecord(@PathVariable String id) {
        ExecutionRecordDTO record = workflowService.getExecutionRecord(id);
        return ApiResponse.ok(record);
    }

    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.ok("Intelligent Agent Backend is running");
    }
}
