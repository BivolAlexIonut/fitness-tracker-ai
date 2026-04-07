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

/**
 * Service for orchestrating AI interactions and managing fitness level derivations.
 */
@Service
public class AIService {

    @Autowired
    private WorkoutRepository workoutRepository;

    @Autowired
    private com.fitnesstracker.demo.repository.DailyMetricsRepository dailyMetricsRepository;

    @Autowired
    private com.fitnesstracker.demo.repository.PRRepository prRepository;

    @Autowired
    private FitnessLevelRepository fitnessLevelRepository;

    @org.springframework.beans.factory.annotation.Value("${ai.service.url}")
    private String aiServiceBaseUrl;

    /**
     * Fetches daily lifestyle and training advice based on recent health metrics.
     */
    public Map<String, Object> getDailyAdvice(User user) {
        try {
            String url = aiServiceBaseUrl + "/daily-advice";
            RestTemplate restTemplate = new RestTemplate();
            
            List<Workout> recentWorkouts = workoutRepository.findByUserIdOrderByDateDesc(user.getId());
            List<com.fitnesstracker.demo.model.DailyMetrics> metrics = dailyMetricsRepository.findByUserIdOrderByDateDesc(user.getId())
                    .stream().limit(7).collect(Collectors.toList());
            
            Map<String, Object> request = new HashMap<>();
            Map<String, Object> profile = mapUserProfile(user);
            
            List<Map<String, Object>> workoutLogs = recentWorkouts.stream().limit(10).map(this::mapWorkoutToContext).collect(Collectors.toList());
            List<Map<String, Object>> dailyMetricsLogs = metrics.stream().map(this::mapMetricsToContext).collect(Collectors.toList());

            request.put("profile", profile);
            request.put("recent_workouts", workoutLogs);
            request.put("daily_metrics", dailyMetricsLogs);

            return restTemplate.postForObject(url, request, Map.class);
        } catch (Exception e) {
            System.err.println("[AIService] Daily advice fetch failed: " + e.getMessage());
            return fallbackAdvice();
        }
    }

    /**
     * Generates a tailored workout plan based on a natural language user query.
     */
    public Map<String, Object> getWorkoutProposal(User user, String userInput) {
        try {
            String url = aiServiceBaseUrl + "/workout-proposal";
            RestTemplate restTemplate = new RestTemplate();

            List<Workout> recentWorkouts = workoutRepository.findByUserIdOrderByDateDesc(user.getId());
            List<com.fitnesstracker.demo.model.DailyMetrics> metrics = dailyMetricsRepository.findByUserIdOrderByDateDesc(user.getId())
                    .stream().limit(7).collect(Collectors.toList());

            Map<String, Object> request = new HashMap<>();
            request.put("user_input", userInput);
            request.put("profile", mapUserProfile(user));
            request.put("recent_workouts", recentWorkouts.stream().limit(10).map(this::mapWorkoutToContext).collect(Collectors.toList()));
            request.put("daily_metrics", metrics.stream().map(this::mapMetricsToContext).collect(Collectors.toList()));

            return restTemplate.postForObject(url, request, Map.class);
        } catch (Exception e) {
            return fallbackWorkout();
        }
    }

    /**
     * Generates meal recommendations based on available ingredients and recent activity.
     */
    public Map<String, Object> getMealProposal(User user, String ingredients) {
        try {
            String url = aiServiceBaseUrl + "/meal-proposal";
            RestTemplate restTemplate = new RestTemplate();

            List<Workout> recentWorkouts = workoutRepository.findByUserIdOrderByDateDesc(user.getId());
            List<com.fitnesstracker.demo.model.DailyMetrics> metrics = dailyMetricsRepository.findByUserIdOrderByDateDesc(user.getId())
                    .stream().limit(7).collect(Collectors.toList());

            Map<String, Object> request = new HashMap<>();
            request.put("ingredients", ingredients);
            request.put("profile", mapUserProfile(user));
            request.put("recent_workouts", recentWorkouts.stream().limit(5).map(this::mapWorkoutToContext).collect(Collectors.toList()));
            request.put("daily_metrics", metrics.stream().map(this::mapMetricsToContext).collect(Collectors.toList()));

            return restTemplate.postForObject(url, request, Map.class);
        } catch (Exception e) {
            return fallbackMeal();
        }
    }

    /**
     * Handles conversational recovery guidance based on recent physical strain.
     */
    public Map<String, Object> getRecoveryChat(User user, Map<String, Object> chatRequest) {
        try {
            String url = aiServiceBaseUrl + "/recovery-chat";
            RestTemplate restTemplate = new RestTemplate();

            List<Workout> recentWorkouts = workoutRepository.findByUserIdOrderByDateDesc(user.getId());
            List<com.fitnesstracker.demo.model.DailyMetrics> metrics = dailyMetricsRepository.findByUserIdOrderByDateDesc(user.getId())
                    .stream().limit(7).collect(Collectors.toList());

            chatRequest.put("profile", mapUserProfile(user));
            chatRequest.put("recent_workouts", recentWorkouts.stream().limit(5).map(this::mapWorkoutToContext).collect(Collectors.toList()));
            chatRequest.put("daily_metrics", metrics.stream().map(this::mapMetricsToContext).collect(Collectors.toList()));

            return restTemplate.postForObject(url, chatRequest, Map.class);
        } catch (Exception e) {
            return fallbackRecovery();
        }
    }

    /**
     * Calculates an updated fitness summary, leveraging existing baselines to prevent unrealistic fluctuations.
     */
    public Map<String, Object> getFitnessSummary(User user) {
        try {
            String url = aiServiceBaseUrl + "/fitness-summary";
            RestTemplate restTemplate = new RestTemplate();

            List<Workout> recentWorkouts = workoutRepository.findByUserIdOrderByDateDesc(user.getId());
            List<com.fitnesstracker.demo.model.DailyMetrics> metrics = dailyMetricsRepository.findByUserIdOrderByDateDesc(user.getId())
                    .stream().limit(7).collect(Collectors.toList());
            List<com.fitnesstracker.demo.model.PersonalRecord> prs = prRepository.findByUserId(user.getId().toString());

            Map<String, Object> request = new HashMap<>();
            request.put("profile", mapUserProfile(user));
            request.put("recent_workouts", recentWorkouts.stream().limit(5).map(this::mapWorkoutToContext).collect(Collectors.toList()));
            request.put("daily_metrics", metrics.stream().map(this::mapMetricsToContext).collect(Collectors.toList()));
            request.put("personal_records", prs.stream().map(this::mapPRToContext).collect(Collectors.toList()));

            // Include current baseline to ensure incremental updates
            fitnessLevelRepository.findByUserId(user.getId()).ifPresent(cl -> {
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
            });

            Map<String, Object> aiResponse = restTemplate.postForObject(url, request, Map.class);
            return aiResponse != null ? aiResponse : getDemoFitnessData();
        } catch (Exception e) {
            System.err.println("[AIService] Fitness summary fetch failed: " + e.getMessage());
            return getDemoFitnessData();
        }
    }

    /**
     * Persists the AI-calculated fitness metrics into the database.
     */
    public void updateFitnessLevel(User user, Map<String, Object> aiResponse) {
        try {
            Optional<FitnessLevel> existingOpt = fitnessLevelRepository.findByUserId(user.getId());
            FitnessLevel fitnessLevel = existingOpt.orElse(new FitnessLevel(user));

            fitnessLevel.setEstimatedVO2Max(parseDouble(aiResponse.get("vo2_max")));
            fitnessLevel.setFitnessLevelScore(parseInteger(aiResponse.get("fitness_level_score")));
            fitnessLevel.setFitnessCategory(safeString(aiResponse.get("fitness_category")));
            fitnessLevel.setBodyBattery(parseInteger(aiResponse.get("body_battery")));
            fitnessLevel.setEstimated5kTime(parseDouble(aiResponse.get("estimated_5k_time")));
            fitnessLevel.setEstimated10kTime(parseDouble(aiResponse.get("estimated_10k_time")));
            fitnessLevel.setEstimatedMarathonTime(parseDouble(aiResponse.get("estimated_marathon_time")));
            fitnessLevel.setPushupEstimate(parseInteger(aiResponse.get("pushup_estimate")));
            fitnessLevel.setPullupEstimate(parseInteger(aiResponse.get("pullup_estimate")));
            fitnessLevel.setBenchPressEstimate(parseDouble(aiResponse.get("bench_press_estimate")));
            fitnessLevel.setDeadliftEstimate(parseDouble(aiResponse.get("deadlift_estimate")));
            fitnessLevel.setAiInsights(safeString(aiResponse.get("ai_insights")));
            fitnessLevel.setStrengthWeaknesses(safeString(aiResponse.get("strength_weaknesses")));

            fitnessLevel.setLastUpdated(LocalDateTime.now());
            fitnessLevelRepository.save(fitnessLevel);
            System.out.println("[AIService] Fitness level updated for user: " + user.getUsername());
        } catch (Exception e) {
            System.err.println("[AIService] updateFitnessLevel persistence failed: " + e.getMessage());
        }
    }

    // Helper mapping methods
    private Map<String, Object> mapUserProfile(User user) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("username", user.getUsername());
        if (user.getHealthProfile() != null) {
            profile.put("fitnessGoal", user.getHealthProfile().getFitnessGoal());
            profile.put("age", user.getHealthProfile().getAge());
            profile.put("gender", user.getHealthProfile().getGender());
        }
        return profile;
    }

    private Map<String, Object> mapWorkoutToContext(Workout w) {
        Map<String, Object> map = new HashMap<>();
        map.put("type", w.getType());
        map.put("duration", w.getDuration());
        map.put("intensity", w.getIntensity());
        map.put("details", w.getDetails());
        map.put("averageHeartRate", w.getAverageHeartRate());
        return map;
    }

    private Map<String, Object> mapMetricsToContext(com.fitnesstracker.demo.model.DailyMetrics m) {
        Map<String, Object> map = new HashMap<>();
        map.put("date", m.getDate().toString());
        map.put("sleepHours", m.getSleepHours());
        map.put("hrv", m.getHrv());
        map.put("stressLevel", m.getStressLevel());
        return map;
    }

    private Map<String, Object> mapPRToContext(com.fitnesstracker.demo.model.PersonalRecord pr) {
        Map<String, Object> map = new HashMap<>();
        map.put("exercise", pr.getExerciseName());
        map.put("weight", pr.getWeight());
        map.put("reps", pr.getReps());
        map.put("date", pr.getDate().toString());
        return map;
    }

    private Map<String, Object> fallbackAdvice() {
        Map<String, Object> error = new HashMap<>();
        error.put("summary", "AI service is currently unavailable. Please check connectivity.");
        error.put("recommendation", "Try again in a few moments.");
        return error;
    }

    private Map<String, Object> fallbackWorkout() {
        Map<String, Object> error = new HashMap<>();
        error.put("workout_name", "Generation Error");
        error.put("exercises", "Unable to generate workout proposal at this time.");
        return error;
    }

    private Map<String, Object> fallbackMeal() {
        Map<String, Object> error = new HashMap<>();
        error.put("meal_name", "Generation Error");
        error.put("recipe", "Unable to generate meal proposal at this time.");
        return error;
    }

    private Map<String, Object> fallbackRecovery() {
        Map<String, Object> error = new HashMap<>();
        error.put("message", "Recovery assistant is currently unavailable.");
        error.put("is_final_protocol", false);
        return error;
    }

    private Map<String, Object> getDemoFitnessData() {
        Map<String, Object> demo = new HashMap<>();
        demo.put("vo2_max", 45.5);
        demo.put("fitness_level_score", 7);
        demo.put("fitness_category", "Intermediate");
        demo.put("ai_insights", "Demo: You are performing well!");
        return demo;
    }

    private String safeString(Object obj) { return obj != null ? obj.toString() : ""; }

    private Double parseDouble(Object obj) {
        if (obj == null) return 0.0;
        try { return Double.parseDouble(obj.toString()); } catch (Exception e) { return 0.0; }
    }

    private Integer parseInteger(Object obj) {
        if (obj == null) return 0;
        try { return (int) Double.parseDouble(obj.toString()); } catch (Exception e) { return 0; }
    }
}
