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

            if (preferences.containsKey("fitnessGoal")) profile.setFitnessGoal((String) preferences.get("fitnessGoal"));
            if (preferences.containsKey("sportsType")) profile.setSportsType((String) preferences.get("sportsType"));
            if (preferences.containsKey("trainingFrequency")) profile.setTrainingFrequency(parseNumber(preferences.get("trainingFrequency")).intValue());
            if (preferences.containsKey("additionalNotes")) profile.setAdditionalNotes((String) preferences.get("additionalNotes"));
            
            if (preferences.containsKey("height")) profile.setHeight(parseNumber(preferences.get("height")).doubleValue());
            if (preferences.containsKey("currentWeight")) profile.setCurrentWeight(parseNumber(preferences.get("currentWeight")).doubleValue());
            if (preferences.containsKey("targetWeight")) profile.setTargetWeight(parseNumber(preferences.get("targetWeight")).doubleValue());
            if (preferences.containsKey("age")) profile.setAge(parseNumber(preferences.get("age")).intValue());
            if (preferences.containsKey("gender")) profile.setGender((String) preferences.get("gender"));
            if (preferences.containsKey("restingHeartRate")) profile.setRestingHeartRate(parseNumber(preferences.get("restingHeartRate")).intValue());
            if (preferences.containsKey("maxHeartRate")) profile.setMaxHeartRate(parseNumber(preferences.get("maxHeartRate")).intValue());
            if (preferences.containsKey("activityLevel")) profile.setActivityLevel((String) preferences.get("activityLevel"));

            userRepository.save(user);
            return ResponseEntity.ok("Preferences saved successfully");
        }
        return ResponseEntity.notFound().build();
    }

    private Number parseNumber(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Number) return (Number) obj;
        try {
            return Double.parseDouble(obj.toString());
        } catch (Exception e) {
            return 0;
        }
    }
}
