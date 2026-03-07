package com.fitnesstracker.demo.controller;

import com.fitnesstracker.demo.model.User;
import com.fitnesstracker.demo.repository.UserRepository;
import com.fitnesstracker.demo.service.AIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AIController {

    @Autowired
    private AIService aiService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/prediction")
    public ResponseEntity<?> getPrediction(@RequestParam Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            Map<String, Object> advice = aiService.getDailyAdvice(userOpt.get());
            return ResponseEntity.ok(advice);
        }
        return ResponseEntity.notFound().build();
    }
}
