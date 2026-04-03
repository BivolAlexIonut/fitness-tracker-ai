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
        try {
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
                map.put("details", w.getDetails());
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

            return restTemplate.postForObject(url, request, Map.class);
        } catch (Exception e) {
            System.err.println("[AIService] Eroare critică: " + e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("summary", "Serviciul AI este momentan indisponibil (Verifică conexiunea DB sau Serviciul Python).");
            error.put("recommendation", "Încearcă să reîncarci pagina peste câteva secunde.");
            return error;
        }
    }

    public Map<String, Object> getWorkoutProposal(User user, String userInput) {
        try {
            String url = aiServiceBaseUrl + "/workout-proposal";
            RestTemplate restTemplate = new RestTemplate();

            // Colectăm contextul (la fel ca la daily advice)
            List<Workout> recentWorkouts = workoutRepository.findByUserIdOrderByDateDesc(user.getId());
            List<com.fitnesstracker.demo.model.DailyMetrics> metrics = dailyMetricsRepository.findByUserIdOrderByDateDesc(user.getId())
                    .stream().limit(7).collect(Collectors.toList());

            Map<String, Object> request = new HashMap<>();
            request.put("user_input", userInput);

            Map<String, Object> profile = new HashMap<>();
            if (user.getHealthProfile() != null) {
                profile.put("username", user.getUsername());
                profile.put("fitnessGoal", user.getHealthProfile().getFitnessGoal());
                profile.put("age", user.getHealthProfile().getAge());
            } else {
                profile.put("username", user.getUsername());
            }

            List<Map<String, Object>> workoutLogs = recentWorkouts.stream().limit(10).map(w -> {
                Map<String, Object> map = new HashMap<>();
                map.put("type", w.getType());
                map.put("duration", w.getDuration());
                map.put("intensity", w.getIntensity());
                map.put("details", w.getDetails());
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

            return restTemplate.postForObject(url, request, Map.class);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("workout_name", "Eroare Generare");
            error.put("exercises", "Nu am putut genera antrenamentul momentan.");
            error.put("ai_notes", "Verifică dacă serviciul AI este pornit.");
            return error;
        }
    }

    public Map<String, Object> getMealProposal(User user, String ingredients) {
        try {
            String url = aiServiceBaseUrl + "/meal-proposal";
            RestTemplate restTemplate = new RestTemplate();

            // Colectăm contextul
            List<Workout> recentWorkouts = workoutRepository.findByUserIdOrderByDateDesc(user.getId());
            List<com.fitnesstracker.demo.model.DailyMetrics> metrics = dailyMetricsRepository.findByUserIdOrderByDateDesc(user.getId())
                    .stream().limit(7).collect(Collectors.toList());

            Map<String, Object> request = new HashMap<>();
            request.put("ingredients", ingredients);

            Map<String, Object> profile = new HashMap<>();
            if (user.getHealthProfile() != null) {
                profile.put("username", user.getUsername());
                profile.put("fitnessGoal", user.getHealthProfile().getFitnessGoal());
                profile.put("age", user.getHealthProfile().getAge());
            } else {
                profile.put("username", user.getUsername());
            }

            List<Map<String, Object>> workoutLogs = recentWorkouts.stream().limit(5).map(w -> {
                Map<String, Object> map = new HashMap<>();
                map.put("type", w.getType());
                map.put("intensity", w.getIntensity());
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

            return restTemplate.postForObject(url, request, Map.class);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("meal_name", "Eroare Generare");
            error.put("recipe", "Nu am putut genera planul alimentar.");
            error.put("ai_reasoning", "Verifică conexiunea cu serviciul AI.");
            return error;
        }
    }

    public Map<String, Object> getRecoveryChat(User user, Map<String, Object> chatRequest) {
        try {
            String url = aiServiceBaseUrl + "/recovery-chat";
            RestTemplate restTemplate = new RestTemplate();

            // Adăugăm contextul fizic în request-ul de chat
            List<Workout> recentWorkouts = workoutRepository.findByUserIdOrderByDateDesc(user.getId());
            List<com.fitnesstracker.demo.model.DailyMetrics> metrics = dailyMetricsRepository.findByUserIdOrderByDateDesc(user.getId())
                    .stream().limit(7).collect(Collectors.toList());

            Map<String, Object> profile = new HashMap<>();
            if (user.getHealthProfile() != null) {
                profile.put("username", user.getUsername());
                profile.put("fitnessGoal", user.getHealthProfile().getFitnessGoal());
            } else {
                profile.put("username", user.getUsername());
            }

            List<Map<String, Object>> workoutLogs = recentWorkouts.stream().limit(5).map(w -> {
                Map<String, Object> map = new HashMap<>();
                map.put("type", w.getType());
                map.put("details", w.getDetails());
                return map;
            }).collect(Collectors.toList());

            List<Map<String, Object>> dailyMetricsLogs = metrics.stream().map(m -> {
                Map<String, Object> map = new HashMap<>();
                map.put("date", m.getDate().toString());
                map.put("sleepHours", m.getSleepHours());
                map.put("hrv", m.getHrv());
                return map;
            }).collect(Collectors.toList());

            chatRequest.put("profile", profile);
            chatRequest.put("recent_workouts", workoutLogs);
            chatRequest.put("daily_metrics", dailyMetricsLogs);

            return restTemplate.postForObject(url, chatRequest, Map.class);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Eroare de conexiune cu asistentul de recuperare.");
            error.put("is_final_protocol", false);
            return error;
        }
    }
}
