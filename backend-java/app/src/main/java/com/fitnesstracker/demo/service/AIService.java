package com.fitnesstracker.demo.service;

import com.fitnesstracker.demo.model.User;
import com.fitnesstracker.demo.model.Workout;
import com.fitnesstracker.demo.repository.WorkoutRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AIService {

    @Autowired
    private WorkoutRepository workoutRepository;

    private final String PYTHON_AI_URL = "http://localhost:8000/predict/daily-advice";

    public Map<String, Object> getDailyAdvice(User user) {
        RestTemplate restTemplate = new RestTemplate();
        
        // Colectăm antrenamentele recente
        List<Workout> recentWorkouts = workoutRepository.findByUserIdOrderByDateDesc(user.getId());
        
        // Pregătim datele pentru Python
        Map<String, Object> request = new HashMap<>();
        
        Map<String, Object> profile = new HashMap<>();
        profile.put("username", user.getUsername());
        profile.put("fitnessGoal", user.getHealthProfile().getFitnessGoal());
        profile.put("sportsType", user.getHealthProfile().getSportsType());
        profile.put("trainingFrequency", user.getHealthProfile().getTrainingFrequency());
        profile.put("currentWeight", user.getHealthProfile().getCurrentWeight());
        profile.put("targetWeight", user.getHealthProfile().getTargetWeight());
        profile.put("age", user.getHealthProfile().getAge());
        profile.put("gender", user.getHealthProfile().getGender());
        
        List<Map<String, Object>> workoutLogs = recentWorkouts.stream().map(w -> {
            Map<String, Object> map = new HashMap<>();
            map.put("workout_type", w.getType());
            map.put("duration", w.getDuration());
            map.put("intensity", w.getIntensity());
            map.put("notes", w.getNotes());
            return map;
        }).collect(Collectors.toList());

        request.put("profile", profile);
        request.put("recent_workouts", workoutLogs);

        try {
            return restTemplate.postForObject(PYTHON_AI_URL, request, Map.class);
        } catch (Exception e) {
            e.printStackTrace(); // Vezi eroarea reală în consola IntelliJ
            Map<String, Object> error = new HashMap<>();
            error.put("summary", "Serviciul AI Python nu este pornit.");
            error.put("recommendation", "Te rugăm să pornești main.py din folderul ai-service-python.");
            return error;
        }
    }
}
