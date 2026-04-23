package com.intelligentagent.repository;

import com.intelligentagent.entity.ExecutionRecord;
import com.intelligentagent.entity.ExecutionRecord.ExecutionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ExecutionRecordRepository extends JpaRepository<ExecutionRecord, String> {

    List<ExecutionRecord> findByWorkflowIdOrderByCreatedAtDesc(String workflowId);

    List<ExecutionRecord> findByStatusOrderByCreatedAtDesc(ExecutionStatus status);

    long countByWorkflowId(String workflowId);
}
