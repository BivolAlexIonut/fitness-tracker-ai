package com.fitnesstracker.demo.repository;

import com.fitnesstracker.demo.model.DailyMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DailyMetricsRepository extends JpaRepository<DailyMetrics, Long> {
    List<DailyMetrics> findByUserIdOrderByDateDesc(Long userId);
}
