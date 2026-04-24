package com.intelligentagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.intelligentagent.entity.WorkflowConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface WorkflowConfigMapper extends BaseMapper<WorkflowConfig> {

    List<WorkflowConfig> selectActiveOrderByUpdatedAtDesc();

    IPage<WorkflowConfig> selectPageActive(Page<WorkflowConfig> page, @Param("name") String name);
}
