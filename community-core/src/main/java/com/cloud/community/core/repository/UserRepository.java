package com.cloud.community.core.repository;

import com.cloud.community.core.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    @org.springframework.data.jpa.repository.Query(value = "SELECT DATE_FORMAT(created_at, '%m-%d') as dateStr, COUNT(*) FROM t_user WHERE created_at >= :since GROUP BY dateStr", nativeQuery = true)
    List<Object[]> countUserGrowth(@Param("since") java.time.LocalDateTime since);
}
