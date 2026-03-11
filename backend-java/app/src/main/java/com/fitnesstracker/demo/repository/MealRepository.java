package com.fitnesstracker.demo.repository;

import com.fitnesstracker.demo.model.MealLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MealRepository extends JpaRepository<MealLog, Long> {
    List<MealLog> findByUserIdOrderByDateDesc(Long userId);
}
