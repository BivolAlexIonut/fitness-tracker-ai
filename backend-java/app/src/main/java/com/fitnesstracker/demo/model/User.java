package com.fitnesstracker.demo.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public HealthProfile getHealthProfile() {
        return healthProfile;
    }

    public void setHealthProfile(HealthProfile healthProfile) {
        this.healthProfile = healthProfile;
    }

    public List<DailyMetrics> getDailyMetrics() {
        return dailyMetrics;
    }

    public void setDailyMetrics(List<DailyMetrics> dailyMetrics) {
        this.dailyMetrics = dailyMetrics;
    }

    private String email;
    private String password;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "health_profile_id", referencedColumnName = "id")
    private HealthProfile healthProfile;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<DailyMetrics> dailyMetrics;
}