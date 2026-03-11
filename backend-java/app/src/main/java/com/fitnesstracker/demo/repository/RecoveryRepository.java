package com.fitnesstracker.demo.repository;

import com.fitnesstracker.demo.model.RecoveryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RecoveryRepository extends JpaRepository<RecoveryLog, Long> {
    List<RecoveryLog> findByUserIdOrderByDateDesc(Long userId);
}
