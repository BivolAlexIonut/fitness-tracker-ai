package com.fitnesstracker.demo.controller;

import com.fitnesstracker.demo.model.HealthProfile;
import com.fitnesstracker.demo.model.User;
import com.fitnesstracker.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/preferences")
    public ResponseEntity<?> savePreferences(@RequestParam Long userId, @RequestBody Map<String, Object> preferences) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            HealthProfile profile = user.getHealthProfile();
            
            if (profile == null) {
                profile = new HealthProfile();
                profile.setUser(user);
                user.setHealthProfile(profile);
            }

            profile.setFitnessGoal((String) preferences.get("fitnessGoal"));
            profile.setSportsType((String) preferences.get("sportsType"));
            profile.setTrainingFrequency((Integer) preferences.get("trainingFrequency"));
            profile.setAdditionalNotes((String) preferences.get("additionalNotes"));

            userRepository.save(user);
            return ResponseEntity.ok("Preferences saved successfully");
        }
        return ResponseEntity.notFound().build();
    }
}
