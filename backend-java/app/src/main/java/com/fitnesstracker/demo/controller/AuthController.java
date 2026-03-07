package com.fitnesstracker.demo.controller;

import com.fitnesstracker.demo.dto.RegistrationRequest;
import com.fitnesstracker.demo.model.HealthProfile;
import com.fitnesstracker.demo.model.User;
import com.fitnesstracker.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegistrationRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email already in use");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());

        HealthProfile profile = new HealthProfile();
        profile.setHeight(request.getHeight());
        profile.setCurrentWeight(request.getCurrentWeight());
        profile.setTargetWeight(request.getTargetWeight());
        profile.setAge(request.getAge());
        profile.setGender(request.getGender());
        profile.setRestingHeartRate(request.getRestingHeartRate());
        profile.setMaxHeartRate(request.getMaxHeartRate());
        profile.setActivityLevel(request.getActivityLevel());

        user.setHealthProfile(profile);
        profile.setUser(user);

        userRepository.save(user);

        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String email = credentials.get("email");
        String password = credentials.get("password");

        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isPresent() && userOpt.get().getPassword().equals(password)) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Login successful");
            response.put("username", userOpt.get().getUsername());
            response.put("userId", userOpt.get().getId());
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.status(401).body("Invalid email or password");
    }
}
