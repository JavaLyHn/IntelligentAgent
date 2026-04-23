package com.intelligentagent.repository;

import com.intelligentagent.entity.WorkflowConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WorkflowConfigRepository extends JpaRepository<WorkflowConfig, String> {

    List<WorkflowConfig> findByActiveTrueOrderByUpdatedAtDesc();

    List<WorkflowConfig> findByNameContainingIgnoreCase(String name);
}
