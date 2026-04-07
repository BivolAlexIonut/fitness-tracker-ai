package com.fitnesstracker.demo.controller;

import com.fitnesstracker.demo.model.User;
import com.fitnesstracker.demo.model.Workout;
import com.fitnesstracker.demo.repository.UserRepository;
import com.fitnesstracker.demo.repository.WorkoutRepository;
import com.fitnesstracker.demo.service.AIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST controller for workout logging and analytics.
 */
@RestController
@RequestMapping("/api/workouts")
@CrossOrigin(origins = "*")
public class WorkoutController {

    @Autowired
    private WorkoutRepository workoutRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AIService aiService;

    @org.springframework.beans.factory.annotation.Value("${ai.service.url}")
    private String aiServiceBaseUrl;

    /**
     * Persists a new workout and triggers a background fitness profile update.
     */
    @PostMapping("/add")
    public ResponseEntity<?> addWorkout(@RequestParam Long userId, @RequestBody Workout workout) {
        if (workout.getDuration() < 0) {
            return ResponseEntity.badRequest().body("Workout duration cannot be negative.");
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            workout.setUser(userOpt.get());
            if (workout.getDate() == null) workout.setDate(LocalDateTime.now());
            workoutRepository.save(workout);

            // Update fitness baseline asynchronously based on new training data
            triggerFitnessSummaryUpdate(userOpt.get());

            return ResponseEntity.ok("Workout logged successfully");
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Retrieves workout history for a specific user, ordered by date descending.
     */
    @GetMapping("/history")
    public ResponseEntity<List<Workout>> getWorkoutHistory(@RequestParam Long userId) {
        List<Workout> workouts = workoutRepository.findByUserIdOrderByDateDesc(userId);
        return ResponseEntity.ok(workouts);
    }

    /**
     * Interfaces with the AI service to extract and predict personal records from training logs.
     */
    @GetMapping("/prs")
    public ResponseEntity<?> getPersonalRecords(@RequestParam Long userId) {
        List<Workout> workouts = workoutRepository.findByUserIdOrderByDateDesc(userId);
        
        String url = aiServiceBaseUrl + "/extract-prs";
        Map<String, Object> aiRequest = new HashMap<>();
        aiRequest.put("workouts", workouts);

        try {
            RestTemplate restTemplate = new RestTemplate();
            return ResponseEntity.ok(restTemplate.postForObject(url, aiRequest, Map.class));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to extract PRs from workout history.");
        }
    }

    private void triggerFitnessSummaryUpdate(User user) {
        try {
            Map<String, Object> aiResponse = aiService.getFitnessSummary(user);
            aiService.updateFitnessLevel(user, aiResponse);
        } catch (Exception e) {
            System.err.println("[WorkoutController] Background fitness update failed: " + e.getMessage());
        }
    }
}
