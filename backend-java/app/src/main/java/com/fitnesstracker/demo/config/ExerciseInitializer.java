package com.fitnesstracker.demo.config;

import com.fitnesstracker.demo.model.Exercise;
import com.fitnesstracker.demo.repository.ExerciseRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class ExerciseInitializer {

    @Bean
    CommandLineRunner initExercises(ExerciseRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                List<Exercise> exercises = Arrays.asList(
                    new Exercise("bench-press", "Bench Press (Împins din culcat)"),
                    new Exercise("squat", "Squat (Genuflexiuni)"),
                    new Exercise("deadlift", "Deadlift (Îndreptări)"),
                    new Exercise("overhead-press", "Overhead Press (Presă deasupra capului)"),
                    new Exercise("snatch", "Snatch (Smuls) - Haltere"),
                    new Exercise("clean-and-jerk", "Clean and Jerk (Aruncat) - Haltere"),
                    new Exercise("front-squat", "Front Squat (Genuflexiuni față)"),
                    new Exercise("incline-bench", "Incline Bench Press (Împins înclinat)"),
                    new Exercise("barbell-row", "Barbell Row (Ramat cu haltera)"),
                    new Exercise("pull-ups", "Pull-ups (Tracțiuni)"),
                    new Exercise("dips", "Dips (Flotări la paralele)"),
                    new Exercise("leg-press", "Leg Press (Presă picioare)"),
                    new Exercise("romanian-deadlift", "Romanian Deadlift (Îndreptări românești)"),
                    new Exercise("power-clean", "Power Clean"),
                    new Exercise("push-press", "Push Press")
                );
                repository.saveAll(exercises);
                System.out.println("[DB-SEED] 15 exerciții adăugate cu succes.");
            }
        };
    }
}
