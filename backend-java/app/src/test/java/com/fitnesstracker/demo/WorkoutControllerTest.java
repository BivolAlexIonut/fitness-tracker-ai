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

/**
 * Unit tests for WorkoutController.
 */
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

    /**
     * Verifies that workout history is correctly retrieved and mapped for a given user.
     */
    @Test
    public void testGetWorkoutHistory() {
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        
        List<Workout> workouts = new ArrayList<>();
        Workout w1 = new Workout();
        w1.setType("Gym");
        w1.setDuration(60);
        workouts.add(w1);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(workoutRepository.findByUserIdOrderByDateDesc(userId)).thenReturn(workouts);

        ResponseEntity<List<Workout>> response = workoutController.getWorkoutHistory(userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("Gym", response.getBody().get(0).getType());
        
        verify(workoutRepository, times(1)).findByUserIdOrderByDateDesc(userId);
    }

    /**
     * Ensures that adding a workout with a negative duration returns a Bad Request status.
     */
    @Test
    public void testAddWorkoutNegativeDuration() {
        Long userId = 1L;
        Workout workout = new Workout();
        workout.setDuration(-10);

        ResponseEntity<?> response = workoutController.addWorkout(userId, workout);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Workout duration cannot be negative.", response.getBody());
        verify(workoutRepository, never()).save(any(Workout.class));
    }

    /**
     * Validates that attempting to log a workout for a non-existent user returns a Not Found status.
     */
    @Test
    public void testAddWorkoutUserNotFound() {
        Long userId = 999L;
        Workout workout = new Workout();
        workout.setType("Running");
        workout.setDuration(30);

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        ResponseEntity<?> response = workoutController.addWorkout(userId, workout);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(workoutRepository, never()).save(any(Workout.class));
    }
}
