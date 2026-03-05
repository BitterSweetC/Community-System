package com.cloud.community.core.repository;

import com.cloud.community.core.entity.Resource;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface ResourceRepository extends JpaRepository<Resource, Long> {
    List<Resource> findByStatus(String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Resource r WHERE r.id = :id")
    Optional<Resource> findByIdForUpdate(@Param("id") Long id);
}
