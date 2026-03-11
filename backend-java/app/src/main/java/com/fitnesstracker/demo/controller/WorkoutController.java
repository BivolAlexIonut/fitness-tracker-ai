package com.fitnesstracker.demo.controller;

import com.fitnesstracker.demo.model.User;
import com.fitnesstracker.demo.model.Workout;
import com.fitnesstracker.demo.repository.UserRepository;
import com.fitnesstracker.demo.repository.WorkoutRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
            if (workout.getDate() == null) workout.setDate(LocalDateTime.now());
            workoutRepository.save(workout);
            return ResponseEntity.ok("Workout logged successfully");
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/history")
    public List<Workout> getHistory(@RequestParam Long userId) {
        return workoutRepository.findByUserIdOrderByDateDesc(userId);
    }

    @GetMapping("/prs")
    public ResponseEntity<?> getPersonalRecords(@RequestParam Long userId) {
        List<Workout> workouts = workoutRepository.findByUserIdOrderByDateDesc(userId);
        
        String PYTHON_AI_URL = "http://localhost:8005/predict/extract-prs";
        Map<String, Object> aiRequest = new HashMap<>();
        aiRequest.put("workouts", workouts);

        try {
            RestTemplate restTemplate = new RestTemplate();
            return ResponseEntity.ok(restTemplate.postForObject(PYTHON_AI_URL, aiRequest, Map.class));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Eroare la extragerea PR-urilor.");
        }
    }
}
