package com.fitnesstracker.demo.controller;

import com.fitnesstracker.demo.model.PersonalRecord;
import com.fitnesstracker.demo.repository.PRRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Controller for managing Personal Records (PRs).
 */
@RestController
@RequestMapping("/api/pr")
@CrossOrigin(origins = "*")
public class PRController {

    @Autowired
    private PRRepository repository;

    @Autowired
    private com.fitnesstracker.demo.service.AIService aiService;

    @Autowired
    private com.fitnesstracker.demo.repository.UserRepository userRepository;

    /**
     * Records a new PR and triggers an asynchronous fitness level update.
     */
    @PostMapping("/add")
    public PersonalRecord addPR(@RequestBody PersonalRecord pr) {
        if (pr.getDate() == null) {
            pr.setDate(LocalDate.now());
        }
        PersonalRecord savedPr = repository.save(pr);
        
        try {
            Long userId = Long.parseLong(pr.getUserId());
            userRepository.findById(userId).ifPresent(user -> {
                Map<String, Object> aiResponse = aiService.getFitnessSummary(user);
                aiService.updateFitnessLevel(user, aiResponse);
            });
        } catch (Exception e) {
            System.err.println("[PRController] Fitness update trigger failed: " + e.getMessage());
        }
        
        return savedPr;
    }

    /**
     * Retrieves historical PR data for a specific exercise.
     */
    @GetMapping("/{userId}/{exercise}")
    public List<PersonalRecord> getHistory(@PathVariable String userId, @PathVariable String exercise) {
        return repository.findByUserIdAndExerciseNameOrderByDateAsc(userId, exercise);
    }
}
