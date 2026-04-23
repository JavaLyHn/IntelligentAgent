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
public class WorkflowDTO {

    private String id;
    private String name;
    private String description;
    private List<NodeDTO> nodes;
    private List<EdgeDTO> edges;
    private Integer version;
    private String createdAt;
    private String updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NodeDTO {
        private String id;
        private String type;
        private PositionDTO position;
        private NodeDataDTO data;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PositionDTO {
        private double x;
        private double y;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NodeDataDTO {
        private String label;
        private String type;
        private String category;
        private Map<String, Object> config;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EdgeDTO {
        private String id;
        private String source;
        private String target;
        private String sourceHandle;
        private String targetHandle;
    }
}
