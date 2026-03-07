package com.fitnesstracker.demo.controller;

import com.fitnesstracker.demo.model.User;
import com.fitnesstracker.demo.model.Workout;
import com.fitnesstracker.demo.repository.UserRepository;
import com.fitnesstracker.demo.repository.WorkoutRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/workouts")
@CrossOrigin(origins = "*")
public class WorkoutController {

    @Autowired
    private WorkoutRepository workoutRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/add")
    public ResponseEntity<?> addWorkout(@RequestParam Long userId, @RequestBody Workout workout) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            workout.setUser(userOpt.get());
            workout.setDate(LocalDateTime.now());
            workoutRepository.save(workout);
            return ResponseEntity.ok("Workout logged successfully");
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/history")
    public List<Workout> getHistory(@RequestParam Long userId) {
        return workoutRepository.findByUserIdOrderByDateDesc(userId);
    }
}
