package com.fitnesstracker.demo;

import com.fitnesstracker.demo.controller.WorkoutController;
import com.fitnesstracker.demo.model.User;
import com.fitnesstracker.demo.model.Workout;
import com.fitnesstracker.demo.repository.UserRepository;
import com.fitnesstracker.demo.repository.WorkoutRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class WorkoutControllerTest {

    @Mock
    private WorkoutRepository workoutRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WorkoutController workoutController;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetWorkoutHistory() {
        // 1. Pregătim datele fictive (Mock data)
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        
        List<Workout> workouts = new ArrayList<>();
        Workout w1 = new Workout();
        w1.setType("Gym");
        w1.setDuration(60);
        workouts.add(w1);

        // 2. Configurăm comportamentul Mock-urilor
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(workoutRepository.findByUserIdOrderByDateDesc(userId)).thenReturn(workouts);

        // 3. Executăm metoda din Controller
        ResponseEntity<List<Workout>> response = workoutController.getWorkoutHistory(userId);

        // 4. Verificăm rezultatele
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("Gym", response.getBody().get(0).getType());
        
        // Verificăm dacă repository-ul a fost apelat exact o dată
        verify(workoutRepository, times(1)).findByUserIdOrderByDateDesc(userId);
    }

    @Test
    public void testAddWorkoutNegativeDuration() {
        // Test profesional: Validarea datelor (Sanity Check)
        // Nu ar trebui să putem salva un antrenament cu durată negativă
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        
        Workout workout = new Workout();
        workout.setDuration(-10); // Durată imposibilă
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        ResponseEntity<?> response = workoutController.addWorkout(userId, workout);

        // Dacă controller-ul are logică de validare, ar trebui să returneze Bad Request
        // În acest proiect, verificăm dacă sistemul este pregătit pentru această logică.
        // Chiar dacă acum salvăm, testul va semnala că avem nevoie de validare la nivel de business.
        if (workout.getDuration() < 0) {
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }
    }
}
