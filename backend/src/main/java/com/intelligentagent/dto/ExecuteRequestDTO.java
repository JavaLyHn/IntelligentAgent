package com.intelligentagent.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecuteRequestDTO {

    private String workflowId;
    private String input;
    private List<WorkflowDTO.NodeDTO> nodes;
    private List<WorkflowDTO.EdgeDTO> edges;
}
