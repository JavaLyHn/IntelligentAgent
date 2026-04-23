package com.intelligentagent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.experimental.SuperBuilder;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@TableName(value = "workflow_configs")
public class WorkflowConfig extends BaseEntity {

    @TableField(value = "name", condition = SqlCondition.LIKE)
    private String name;

    private String description;

    private String nodesJson;

    private String edgesJson;

    @Builder.Default
    @TableLogic
    private Boolean active = true;

    @Builder.Default
    @Version
    private Integer version = 1;

    private LocalDateTime lastExecutedAt;
}
