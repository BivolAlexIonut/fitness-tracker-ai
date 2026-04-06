package com.fitnesstracker.demo.repository;

import com.fitnesstracker.demo.model.FitnessLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface FitnessLevelRepository extends JpaRepository<FitnessLevel, Long> {
    Optional<FitnessLevel> findByUserId(Long userId);
}


