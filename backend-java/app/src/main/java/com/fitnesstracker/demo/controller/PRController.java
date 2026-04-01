package com.fitnesstracker.demo.controller;

import com.fitnesstracker.demo.model.PersonalRecord;
import com.fitnesstracker.demo.repository.PRRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/pr")
@CrossOrigin(origins = "*")
public class PRController {

    @Autowired
    private PRRepository repository;

    @PostMapping("/add")
    public PersonalRecord addPR(@RequestBody PersonalRecord pr) {
        if (pr.getDate() == null) {
            pr.setDate(LocalDate.now());
        }
        return repository.save(pr);
    }

    @GetMapping("/{userId}/{exercise}")
    public List<PersonalRecord> getHistory(@PathVariable String userId, @PathVariable String exercise) {
        return repository.findByUserIdAndExerciseNameOrderByDateAsc(userId, exercise);
    }
}