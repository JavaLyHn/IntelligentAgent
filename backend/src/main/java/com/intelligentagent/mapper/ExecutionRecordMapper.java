package com.intelligentagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.intelligentagent.entity.ExecutionRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ExecutionRecordMapper extends BaseMapper<ExecutionRecord> {

    IPage<ExecutionRecord> selectPageByWorkflowId(Page<ExecutionRecord> page, @Param("workflowId") String workflowId);

    Long countByWorkflowId(@Param("workflowId") String workflowId);
}
