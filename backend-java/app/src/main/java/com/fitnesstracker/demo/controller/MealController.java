package com.fitnesstracker.demo.controller;

import com.fitnesstracker.demo.model.MealLog;
import com.fitnesstracker.demo.model.User;
import com.fitnesstracker.demo.repository.MealRepository;
import com.fitnesstracker.demo.repository.UserRepository;
import com.fitnesstracker.demo.service.AIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/meals")
@CrossOrigin(origins = "*")
public class MealController {

    @Autowired
    private MealRepository mealRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AIService aiService;

    @org.springframework.beans.factory.annotation.Value("${ai.service.url}")
    private String aiServiceBaseUrl;

    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeAndSaveMeal(@RequestParam Long userId, @RequestBody Map<String, String> request) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) return ResponseEntity.notFound().build();
        User user = userOpt.get();

        String mealDescription = request.get("description");
        String url = aiServiceBaseUrl + "/meal-analysis";

        Map<String, Object> aiRequest = new HashMap<>();
        aiRequest.put("meal_description", mealDescription);
        
        Map<String, Object> profile = new HashMap<>();
        profile.put("age", user.getHealthProfile().getAge());
        profile.put("gender", user.getHealthProfile().getGender());
        profile.put("fitnessGoal", user.getHealthProfile().getFitnessGoal());
        aiRequest.put("profile", profile);

        try {
            RestTemplate restTemplate = new RestTemplate();
            Map<String, Object> aiResponse = restTemplate.postForObject(url, aiRequest, Map.class);

            // Verificare defensivă: AI-ul trebuie să returneze un răspuns valid și fără cheia 'error'
            if (aiResponse != null && !aiResponse.containsKey("error") && aiResponse.containsKey("calories")) {
                MealLog meal = new MealLog();
                meal.setUser(user);
                meal.setDescription(mealDescription);
                
                // Procesare sigură a numerelor (AI-ul poate returna Double sau Integer)
                meal.setCalories(parseNumber(aiResponse.get("calories")).intValue());
                meal.setProtein(parseNumber(aiResponse.get("protein")).doubleValue());
                meal.setCarbs(parseNumber(aiResponse.get("carbs")).doubleValue());
                meal.setFats(parseNumber(aiResponse.get("fats")).doubleValue());
                
                meal.setDate(LocalDateTime.now());
                mealRepository.save(meal);

                // Trigger fitness level update asincron
                triggerFitnessSummaryUpdate(user);

                aiResponse.put("id", meal.getId());
                return ResponseEntity.ok(aiResponse);
            } else {
                String errorMsg = aiResponse != null && aiResponse.containsKey("error") 
                    ? (String) aiResponse.get("error") 
                    : "AI-ul nu a putut procesa masa. Încearcă o descriere mai clară.";
                return ResponseEntity.badRequest().body(errorMsg);
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Eroare de conexiune cu serviciul AI.");
        }
    }

    private Number parseNumber(Object obj) {
        if (obj instanceof Number) return (Number) obj;
        try {
            return Double.parseDouble(obj.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    @GetMapping("/history")
    public List<MealLog> getMealHistory(@RequestParam Long userId) {
        return mealRepository.findByUserIdOrderByDateDesc(userId);
    }

    private void triggerFitnessSummaryUpdate(User user) {
        try {
            // Apelăm AI pentru a obține rezumatul de fitness
            Map<String, Object> aiResponse = aiService.getFitnessSummary(user);

            // Actualizăm baza de date cu noile date (dacă AI spune că trebuie update)
            aiService.updateFitnessLevel(user, aiResponse);
        } catch (Exception e) {
            System.out.println("[MealController] Eroare la update fitness level: " + e.getMessage());
        }
    }
}

