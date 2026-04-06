package com.fitnesstracker.demo.controller;

import com.fitnesstracker.demo.model.User;
import com.fitnesstracker.demo.model.FitnessLevel;
import com.fitnesstracker.demo.repository.UserRepository;
import com.fitnesstracker.demo.repository.FitnessLevelRepository;
import com.fitnesstracker.demo.service.AIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/fitness-level")
@CrossOrigin(origins = "*")
public class FitnessLevelController {

    @Autowired
    private FitnessLevelRepository fitnessLevelRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AIService aiService;

    @GetMapping("/summary/{userId}")
    public ResponseEntity<?> getFitnessSummary(@PathVariable Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) return ResponseEntity.notFound().build();

        User user = userOpt.get();

        try {
            // Preluăm datele de fitness summary din AI
            Map<String, Object> aiSummary = aiService.getFitnessSummary(user);

            // Actualizează și în BD
            aiService.updateFitnessLevel(user, aiSummary);

            return ResponseEntity.ok(aiSummary);
        } catch (Exception e) {
            System.err.println("[FitnessLevelController] Eroare getFitnessSummary: " + e.getMessage());

            // Fallback - return demo data dacă ceva e greșit
            Map<String, Object> demoData = new java.util.HashMap<>();
            demoData.put("vo2_max", 45.5);
            demoData.put("fitness_level_score", 7);
            demoData.put("fitness_category", "Intermediate");
            demoData.put("estimated_5k_time", 25.5);
            demoData.put("estimated_10k_time", 54.0);
            demoData.put("estimated_marathon_time", 3.5);
            demoData.put("pushup_estimate", 35);
            demoData.put("pullup_estimate", 12);
            demoData.put("bench_press_estimate", 100.0);
            demoData.put("deadlift_estimate", 150.0);
            demoData.put("body_battery", 72);
            demoData.put("ai_insights", "Demo mode: Ești într-o formă bună!");
            demoData.put("strength_weaknesses", "Demo: Puncte forte: cardio. Slăbiciuni: forță.");

            return ResponseEntity.ok(demoData);
        }
    }

    @GetMapping("/current/{userId}")
    public ResponseEntity<?> getCurrentFitnessLevel(@PathVariable Long userId) {
        Optional<FitnessLevel> fitnessLevel = fitnessLevelRepository.findByUserId(userId);
        if (fitnessLevel.isEmpty()) {
            return ResponseEntity.ok(new java.util.HashMap<String, Object>() {{
                put("message", "Nu avem date de fitness level încă. Adaugă antrenamente pentru a genera.");
            }});
        }
        return ResponseEntity.ok(fitnessLevel.get());
    }

    @PostMapping("/refresh/{userId}")
    public ResponseEntity<?> refreshFitnessLevel(@PathVariable Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) return ResponseEntity.notFound().build();

        User user = userOpt.get();

        // Apelează AI pentru a obține datele actualizate
        Map<String, Object> aiResponse = aiService.getFitnessSummary(user);

        // Actualizează nivelul de fitness
        aiService.updateFitnessLevel(user, aiResponse);

        return ResponseEntity.ok(aiResponse);
    }

    @GetMapping("/test")
    public ResponseEntity<?> testEndpoint() {
        Map<String, String> response = new java.util.HashMap<>();
        response.put("status", "OK");
        response.put("message", "Fitness API endpoint is working");
        return ResponseEntity.ok(response);
    }
}

