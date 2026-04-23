package com.intelligentagent.engine.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowEdge {

    private String id;
    private String source;
    private String target;
    private String sourceHandle;
    private String targetHandle;
}
