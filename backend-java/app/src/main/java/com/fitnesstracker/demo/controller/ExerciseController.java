package com.fitnesstracker.demo.controller;

import com.fitnesstracker.demo.model.Exercise;
import com.fitnesstracker.demo.repository.ExerciseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/exercises")
@CrossOrigin(origins = "*")
public class ExerciseController {

    @Autowired
    private ExerciseRepository repository;

    @GetMapping
    public List<Exercise> getAllExercises() {
        return repository.findAll();
    }
}
