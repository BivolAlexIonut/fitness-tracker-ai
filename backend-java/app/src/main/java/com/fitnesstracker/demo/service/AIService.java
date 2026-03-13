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

    @Autowired
    private com.fitnesstracker.demo.repository.DailyMetricsRepository dailyMetricsRepository;

    @org.springframework.beans.factory.annotation.Value("${ai.service.url}")
    private String aiServiceBaseUrl;

    public Map<String, Object> getDailyAdvice(User user) {
        String url = aiServiceBaseUrl + "/daily-advice";
        RestTemplate restTemplate = new RestTemplate();
        
        // Colectăm antrenamentele recente
        List<Workout> recentWorkouts = workoutRepository.findByUserIdOrderByDateDesc(user.getId());

        // Colectăm metricile zilnice recente (ultimele 7 zile)
        List<com.fitnesstracker.demo.model.DailyMetrics> metrics = dailyMetricsRepository.findByUserIdOrderByDateDesc(user.getId())
                .stream().limit(7).collect(Collectors.toList());
        
        // Pregătim datele pentru Python
        Map<String, Object> request = new HashMap<>();
        
        Map<String, Object> profile = new HashMap<>();
        if (user.getHealthProfile() != null) {
            profile.put("username", user.getUsername());
            profile.put("fitnessGoal", user.getHealthProfile().getFitnessGoal());
            profile.put("age", user.getHealthProfile().getAge());
            profile.put("gender", user.getHealthProfile().getGender());
        } else {
            profile.put("username", user.getUsername());
        }
        
        List<Map<String, Object>> workoutLogs = recentWorkouts.stream().limit(10).map(w -> {
            Map<String, Object> map = new HashMap<>();
            map.put("type", w.getType());
            map.put("duration", w.getDuration());
            map.put("intensity", w.getIntensity());
            map.put("details", w.getDetails() != null ? w.getDetails() : w.getNotes());
            map.put("averageHeartRate", w.getAverageHeartRate());
            return map;
        }).collect(Collectors.toList());

        List<Map<String, Object>> dailyMetricsLogs = metrics.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("date", m.getDate().toString());
            map.put("sleepHours", m.getSleepHours());
            map.put("hrv", m.getHrv());
            map.put("stressLevel", m.getStressLevel());
            return map;
        }).collect(Collectors.toList());

        request.put("profile", profile);
        request.put("recent_workouts", workoutLogs);
        request.put("daily_metrics", dailyMetricsLogs);

        try {
            return restTemplate.postForObject(url, request, Map.class);
        } catch (Exception e) {
            System.err.println("[AIService] Eroare la comunicarea cu AI: " + e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("summary", "Serviciul AI Python nu este accesibil.");
            error.put("recommendation", "Te rugăm să verifici dacă serviciul AI este pornit pe portul configurat.");
            return error;
        }
    }
}
