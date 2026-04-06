package com.fitnesstracker.demo.service;

import com.fitnesstracker.demo.model.User;
import com.fitnesstracker.demo.model.Workout;
import com.fitnesstracker.demo.model.FitnessLevel;
import com.fitnesstracker.demo.repository.WorkoutRepository;
import com.fitnesstracker.demo.repository.FitnessLevelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AIService {

    @Autowired
    private WorkoutRepository workoutRepository;

    @Autowired
    private com.fitnesstracker.demo.repository.DailyMetricsRepository dailyMetricsRepository;

    @Autowired
    private FitnessLevelRepository fitnessLevelRepository;

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

    public Map<String, Object> getFitnessSummary(User user) {
        try {
            String url = aiServiceBaseUrl + "/fitness-summary";
            RestTemplate restTemplate = new RestTemplate();

            List<Workout> recentWorkouts = workoutRepository.findByUserIdOrderByDateDesc(user.getId());
            List<com.fitnesstracker.demo.model.DailyMetrics> metrics = dailyMetricsRepository.findByUserIdOrderByDateDesc(user.getId())
                    .stream().limit(7).collect(Collectors.toList());

            Map<String, Object> request = new HashMap<>();
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
                map.put("duration", w.getDuration());
                map.put("intensity", w.getIntensity());
                return map;
            }).collect(Collectors.toList());

            List<Map<String, Object>> dailyMetricsLogs = metrics.stream().map(m -> {
                Map<String, Object> map = new HashMap<>();
                map.put("date", m.getDate().toString());
                map.put("sleepHours", m.getSleepHours());
                map.put("hrv", m.getHrv());
                return map;
            }).collect(Collectors.toList());

            request.put("profile", profile);
            request.put("recent_workouts", workoutLogs);
            request.put("daily_metrics", dailyMetricsLogs);

            // Adăugăm nivelul curent de fitness pentru context (preventing fluctuations)
            Optional<FitnessLevel> currentLevelOpt = fitnessLevelRepository.findByUserId(user.getId());
            if (currentLevelOpt.isPresent()) {
                FitnessLevel cl = currentLevelOpt.get();
                Map<String, Object> fitnessCtx = new HashMap<>();
                fitnessCtx.put("vo2_max", cl.getEstimatedVO2Max());
                fitnessCtx.put("level_score", cl.getFitnessLevelScore());
                fitnessCtx.put("category", cl.getFitnessCategory());
                fitnessCtx.put("body_battery", cl.getBodyBattery());
                fitnessCtx.put("pushup_estimate", cl.getPushupEstimate());
                fitnessCtx.put("pullup_estimate", cl.getPullupEstimate());
                fitnessCtx.put("bench_press_estimate", cl.getBenchPressEstimate());
                fitnessCtx.put("deadlift_estimate", cl.getDeadliftEstimate());
                request.put("fitness_level", fitnessCtx);
            }

            Map<String, Object> aiResponse = restTemplate.postForObject(url, request, Map.class);
            if (aiResponse == null) {
                return getDemoFitnessData();
            }
            return aiResponse;
        } catch (Exception e) {
            System.err.println("[AIService] Eroare fitness summary: " + e.getMessage());
            return getDemoFitnessData();
        }
    }

    private Map<String, Object> getDemoFitnessData() {
        Map<String, Object> demo = new HashMap<>();
        demo.put("vo2_max", 45.5);
        demo.put("fitness_level_score", 7);
        demo.put("fitness_category", "Intermediate");
        demo.put("estimated_5k_time", 25.5);
        demo.put("estimated_10k_time", 54.0);
        demo.put("estimated_marathon_time", 3.5);
        demo.put("pushup_estimate", 35);
        demo.put("pullup_estimate", 12);
        demo.put("bench_press_estimate", 100.0);
        demo.put("deadlift_estimate", 150.0);
        demo.put("body_battery", 72);
        demo.put("ai_insights", "Demo: Ești într-o formă bună!");
        demo.put("strength_weaknesses", "Demo: Puncte forte: cardio. Slăbiciuni: forță.");
        return demo;
    }

    public void updateFitnessLevel(User user, Map<String, Object> aiResponse) {
        try {
            Optional<FitnessLevel> existingOpt = fitnessLevelRepository.findByUserId(user.getId());
            FitnessLevel fitnessLevel = existingOpt.orElse(new FitnessLevel(user));

            if (aiResponse.containsKey("vo2_max")) {
                fitnessLevel.setEstimatedVO2Max(parseDouble(aiResponse.get("vo2_max")));
            }
            if (aiResponse.containsKey("fitness_level_score")) {
                fitnessLevel.setFitnessLevelScore(parseInteger(aiResponse.get("fitness_level_score")));
            }
            if (aiResponse.containsKey("fitness_category")) {
                fitnessLevel.setFitnessCategory(aiResponse.get("fitness_category").toString());
            }
            if (aiResponse.containsKey("body_battery")) {
                fitnessLevel.setBodyBattery(parseInteger(aiResponse.get("body_battery")));
            }
            
            // Predicții timp alergare
            if (aiResponse.containsKey("estimated_5k_time")) {
                fitnessLevel.setEstimated5kTime(parseDouble(aiResponse.get("estimated_5k_time")));
            }
            if (aiResponse.containsKey("estimated_10k_time")) {
                fitnessLevel.setEstimated10kTime(parseDouble(aiResponse.get("estimated_10k_time")));
            }
            if (aiResponse.containsKey("estimated_marathon_time")) {
                fitnessLevel.setEstimatedMarathonTime(parseDouble(aiResponse.get("estimated_marathon_time")));
            }

            // Estimări forță
            if (aiResponse.containsKey("pushup_estimate")) {
                fitnessLevel.setPushupEstimate(parseInteger(aiResponse.get("pushup_estimate")));
            }
            if (aiResponse.containsKey("pullup_estimate")) {
                fitnessLevel.setPullupEstimate(parseInteger(aiResponse.get("pullup_estimate")));
            }
            if (aiResponse.containsKey("bench_press_estimate")) {
                fitnessLevel.setBenchPressEstimate(parseDouble(aiResponse.get("bench_press_estimate")));
            }
            if (aiResponse.containsKey("deadlift_estimate")) {
                fitnessLevel.setDeadliftEstimate(parseDouble(aiResponse.get("deadlift_estimate")));
            }

            // Insights AI
            if (aiResponse.containsKey("ai_insights")) {
                fitnessLevel.setAiInsights(aiResponse.get("ai_insights").toString());
            }
            if (aiResponse.containsKey("strength_weaknesses")) {
                fitnessLevel.setStrengthWeaknesses(aiResponse.get("strength_weaknesses").toString());
            }

            fitnessLevel.setLastUpdated(LocalDateTime.now());
            fitnessLevelRepository.save(fitnessLevel);
            System.out.println("[AIService] Nivel fitness actualizat pentru: " + user.getUsername());
        } catch (Exception e) {
            System.err.println("[AIService] Eroare updateFitnessLevel: " + e.getMessage());
        }
    }

    private Double parseDouble(Object obj) {
        if (obj == null) return 0.0;
        if (obj instanceof Double) return (Double) obj;
        if (obj instanceof Integer) return ((Integer) obj).doubleValue();
        if (obj instanceof Float) return ((Float) obj).doubleValue();
        try {
            return Double.parseDouble(obj.toString());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private Integer parseInteger(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Integer) return (Integer) obj;
        if (obj instanceof Double) return ((Double) obj).intValue();
        if (obj instanceof Float) return ((Float) obj).intValue();
        try {
            // Folosim Double.parseDouble și apoi intValue pentru a gestiona "72.0"
            return (int) Double.parseDouble(obj.toString());
        } catch (Exception e) {
            return 0;
        }
    }
}
