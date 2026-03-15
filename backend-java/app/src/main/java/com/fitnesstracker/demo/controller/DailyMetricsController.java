package com.fitnesstracker.demo.controller;

import com.fitnesstracker.demo.model.DailyMetrics;
import com.fitnesstracker.demo.model.User;
import com.fitnesstracker.demo.repository.DailyMetricsRepository;
import com.fitnesstracker.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/metrics")
@CrossOrigin(origins = "*")
public class DailyMetricsController {

    @Autowired
    private DailyMetricsRepository dailyMetricsRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/add/{userId}")
    public ResponseEntity<?> addMetrics(@PathVariable Long userId, @RequestBody DailyMetrics metrics) {
        return userRepository.findById(userId).map(user -> {
            metrics.setUser(user);
            DailyMetrics saved = dailyMetricsRepository.save(metrics);
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public List<DailyMetrics> getUserMetrics(@PathVariable Long userId) {
        return dailyMetricsRepository.findByUserIdOrderByDateDesc(userId);
    }
}