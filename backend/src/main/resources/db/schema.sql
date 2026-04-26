CREATE DATABASE IF NOT EXISTS intelligent_agent
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE intelligent_agent;

DROP TABLE IF EXISTS execution_records;
DROP TABLE IF EXISTS workflow_configs;

CREATE TABLE workflow_configs (
    id              VARCHAR(36)     NOT NULL COMMENT '主键ID',
    name            VARCHAR(255)   NOT NULL COMMENT '工作流名称',
    description     VARCHAR(1000)  NULL    COMMENT '工作流描述',
    nodes_json      LONGTEXT       NOT NULL COMMENT '节点配置JSON',
    edges_json      LONGTEXT       NOT NULL COMMENT '边配置JSON',
    active          TINYINT(1)     NOT NULL DEFAULT 1 COMMENT '是否激活(1:是 0:否-逻辑删除)',
    version         INT            NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    last_executed_at DATETIME      NULL    COMMENT '最后执行时间',
    created_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME       NULL    ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_active (active),
    INDEX idx_name (name),
    INDEX idx_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作流配置表';

CREATE TABLE execution_records (
    id              VARCHAR(36)     NOT NULL COMMENT '主键ID',
    workflow_id     VARCHAR(36)     NOT NULL COMMENT '关联工作流ID',
    status          VARCHAR(20)     NOT NULL COMMENT '执行状态: RUNNING/SUCCESS/FAILED',
    input_text      LONGTEXT        NULL    COMMENT '输入文本',
    output_text     LONGTEXT        NULL    COMMENT '输出文本',
    audio_url       VARCHAR(2000)   NULL    COMMENT '音频URL',
    execution_log   LONGTEXT        NULL    COMMENT '执行日志',
    node_results_json LONGTEXT      NULL    COMMENT '节点执行结果JSON',
    duration_ms     BIGINT          NULL    COMMENT '执行耗时(毫秒)',
    error_message   VARCHAR(500)    NULL    COMMENT '错误信息',
    completed_at    DATETIME        NULL    COMMENT '完成时间',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME        NULL    ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_workflow_id (workflow_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作流执行记录表';
