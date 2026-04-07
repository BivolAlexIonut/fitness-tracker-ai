package com.fitnesstracker.demo.repository;

import com.fitnesstracker.demo.model.PersonalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PRRepository extends JpaRepository<PersonalRecord, Long> {
    List<PersonalRecord> findByUserIdAndExerciseNameOrderByDateAsc(String userId, String exerciseName);
    List<PersonalRecord> findByUserId(String userId);
}