package com.fitnesstracker.demo.controller;

import com.fitnesstracker.demo.model.HealthProfile;
import com.fitnesstracker.demo.model.User;
import com.fitnesstracker.demo.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    private final UserRepository userRepository;

    public TestController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/creeaza-test")
    public String creeazaUserTest() {
        // 1. Creăm un profil de sănătate
        HealthProfile profil = new HealthProfile();
        profil.setHeight(180.5);
        profil.setTargetWeight(75.0);
        profil.setMaxHeartRate(190);

        // 2. Creăm utilizatorul
        User user = new User();
        user.setUsername("Alex_AI");
        user.setEmail("alex@fitness.ro");
        user.setPassword("parolasecreta123");

        // 3. Le legăm între ele (One-to-One)
        user.setHealthProfile(profil);

        // 4. Salvăm în baza de date MySQL din Docker!
        userRepository.save(user);

        return "Succes! Utilizatorul Alex_AI și profilul lui de sănătate au fost salvate în baza de date!";
    }
}