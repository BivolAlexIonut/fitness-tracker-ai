package com.fitnesstracker.demo.controller;

import com.fitnesstracker.demo.model.DailyMetrics;
import com.fitnesstracker.demo.model.User;
import com.fitnesstracker.demo.repository.DailyMetricsRepository;
import com.fitnesstracker.demo.repository.UserRepository;
import com.fitnesstracker.demo.service.AIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/metrics")
@CrossOrigin(origins = "*")
public class DailyMetricsController {

    @Autowired
    private DailyMetricsRepository dailyMetricsRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AIService aiService;

    @PostMapping("/add/{userId}")
    public ResponseEntity<?> addMetrics(@PathVariable Long userId, @RequestBody DailyMetrics metrics) {
        return userRepository.findById(userId).map(user -> {
            metrics.setUser(user);
            DailyMetrics saved = dailyMetricsRepository.save(metrics);

            // Trigger fitness level update asincron
            triggerFitnessSummaryUpdate(user);

            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public List<DailyMetrics> getUserMetrics(@PathVariable Long userId) {
        return dailyMetricsRepository.findByUserIdOrderByDateDesc(userId);
    }

    private void triggerFitnessSummaryUpdate(User user) {
        try {
            // Apelăm AI pentru a obține rezumatul de fitness
            Map<String, Object> aiResponse = aiService.getFitnessSummary(user);

            // Actualizăm baza de date cu noile date (dacă AI spune că trebuie update)
            aiService.updateFitnessLevel(user, aiResponse);
        } catch (Exception e) {
            System.out.println("[DailyMetricsController] Eroare la update fitness level: " + e.getMessage());
        }
    }
}