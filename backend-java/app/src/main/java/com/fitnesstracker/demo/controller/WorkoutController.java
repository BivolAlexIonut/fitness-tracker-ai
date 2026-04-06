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

    @PostMapping("/add")
    public ResponseEntity<?> addWorkout(@RequestParam Long userId, @RequestBody Workout workout) {
        if (workout.getDuration() < 0) {
            return ResponseEntity.badRequest().body("Durata antrenamentului nu poate fi negativă.");
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            workout.setUser(userOpt.get());
            if (workout.getDate() == null) workout.setDate(LocalDateTime.now());
            workoutRepository.save(workout);

            // Trigger fitness level update asincron
            triggerFitnessSummaryUpdate(userOpt.get());

            return ResponseEntity.ok("Workout logged successfully");
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/history")
    public ResponseEntity<List<Workout>> getWorkoutHistory(@RequestParam Long userId) {
        List<Workout> workouts = workoutRepository.findByUserIdOrderByDateDesc(userId);
        return ResponseEntity.ok(workouts);
    }

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
            return ResponseEntity.badRequest().body("Eroare la extragerea PR-urilor.");
        }
    }

    private void triggerFitnessSummaryUpdate(User user) {
        try {
            // Apelăm AI pentru a obține rezumatul de fitness
            Map<String, Object> aiResponse = aiService.getFitnessSummary(user);

            // Actualizăm baza de date cu noile date (dacă AI spune că trebuie update)
            aiService.updateFitnessLevel(user, aiResponse);
        } catch (Exception e) {
            System.out.println("[WorkoutController] Eroare la update fitness level: " + e.getMessage());
        }
    }
}
