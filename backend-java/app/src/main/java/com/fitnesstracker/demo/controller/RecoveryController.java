package com.fitnesstracker.demo.controller;

import com.fitnesstracker.demo.model.RecoveryLog;
import com.fitnesstracker.demo.model.User;
import com.fitnesstracker.demo.model.Workout;
import com.fitnesstracker.demo.repository.RecoveryRepository;
import com.fitnesstracker.demo.repository.UserRepository;
import com.fitnesstracker.demo.repository.WorkoutRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/recovery")
@CrossOrigin(origins = "*")
public class RecoveryController {

    @Autowired
    private RecoveryRepository recoveryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkoutRepository workoutRepository;

    @org.springframework.beans.factory.annotation.Value("${ai.service.url}")
    private String aiServiceBaseUrl;

    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeRecovery(@RequestParam Long userId, @RequestBody Map<String, Object> request) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) return ResponseEntity.notFound().build();

        String url = aiServiceBaseUrl + "/recovery-protocol";

        // Extragem datele din request-ul primit de la Frontend
        String soreParts = request.get("soreParts") != null ? request.get("soreParts").toString() : "Nespecificat";

        // Convertim painLevel în mod sigur
        Object painLevelObj = request.get("painLevel");
        Integer painLevel = (painLevelObj != null) ? Integer.parseInt(painLevelObj.toString()) : 0;

        // Luăm ultimele 3 antrenamente pentru context
        List<Workout> recent = workoutRepository.findByUserIdOrderByDateDesc(userId)
                .stream()
                .limit(3)
                .collect(Collectors.toList());

        // Pregătim obiectul pentru Python
        Map<String, Object> aiRequest = new HashMap<>();
        aiRequest.put("sore_parts", soreParts);
        aiRequest.put("pain_level", painLevel);
        aiRequest.put("recent_workouts", recent);

        try {
            RestTemplate restTemplate = new RestTemplate();
            // Specificăm Map.class pentru a primi JSON-ul ca un dicționar Java
            Map<String, Object> aiResponse = restTemplate.postForObject(url, aiRequest, Map.class);

            if (aiResponse != null && aiResponse.containsKey("protocol")) {
                RecoveryLog log = new RecoveryLog();
                log.setUser(userOpt.get());
                log.setSoreParts(soreParts);
                log.setPainLevel(painLevel);

                // --- FIX AICI: Folosim String.valueOf() in loc de (String) ---
                // Aceasta previne ClassCastException daca AI-ul trimite un obiect in loc de string
                String protocolContent = String.valueOf(aiResponse.get("protocol"));
                log.setProtocol(protocolContent);

                log.setDate(LocalDateTime.now());
                recoveryRepository.save(log);

                // Returnăm răspunsul AI-ului plus data salvată
                aiResponse.put("date", log.getDate());
                return ResponseEntity.ok(aiResponse);
            }
        } catch (Exception e) {
            System.err.println("Eroare la comunicarea cu AI-ul: " + e.getMessage());
            e.printStackTrace();
        }

        return ResponseEntity.badRequest().body("Eroare la analiza recuperarii. Verifica daca serviciul AI este pornit.");
    }
}