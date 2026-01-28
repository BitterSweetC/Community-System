package com.cloud.community.core.repository;

import com.cloud.community.core.entity.RecruitFormField;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecruitFormFieldRepository extends JpaRepository<RecruitFormField, Long> {
    List<RecruitFormField> findByBatchIdOrderBySortOrderAsc(Long batchId);
}
