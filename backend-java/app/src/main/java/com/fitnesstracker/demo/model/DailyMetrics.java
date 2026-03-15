package com.fitnesstracker.demo.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "daily_metrics")
public class DailyMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate date;

    private double sleepHours;
    private int sleepScore; // Scorul de somn 1-100
    private int hrv; // Heart Rate Variability (ms)
    private int restingHeartRate; // Pulsul de repaus (bpm)
    private int stressLevel; // Scorul de stres 1-100 (bazat pe HRV)

    private int steps; // Numărul de pași
    private int activeCalories; // Calorii arse prin mișcare
    private int waterIntakeMl; // Hidratare (ml)

    private String mood; // Starea de spirit (Ex: "Energic", "Obosit")
    private int energyLevel; // 1-10 (Cât de pregătit se simte utilizatorul)

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User user;

    @PrePersist
    protected void onCreate() {
        if (this.date == null) {
            this.date = LocalDate.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public double getSleepHours() {
        return sleepHours;
    }

    public void setSleepHours(double sleepHours) {
        this.sleepHours = sleepHours;
    }

    public int getHrv() {
        return hrv;
    }

    public void setHrv(int hrv) {
        this.hrv = hrv;
    }

    public int getStressLevel() {
        return stressLevel;
    }

    public void setStressLevel(int stressLevel) {
        this.stressLevel = stressLevel;
    }

    public int getActiveCalories() {
        return activeCalories;
    }

    public void setActiveCalories(int activeCalories) {
        this.activeCalories = activeCalories;
    }

    public int getEnergyLevel() {
        return energyLevel;
    }

    public void setEnergyLevel(int energyLevel) {
        this.energyLevel = energyLevel;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public int getSleepScore() { return sleepScore; }
    public void setSleepScore(int sleepScore) { this.sleepScore = sleepScore; }

    public int getRestingHeartRate() { return restingHeartRate; }
    public void setRestingHeartRate(int restingHeartRate) { this.restingHeartRate = restingHeartRate; }

    public int getSteps() { return steps; }
    public void setSteps(int steps) { this.steps = steps; }

    public int getWaterIntakeMl() { return waterIntakeMl; }
    public void setWaterIntakeMl(int waterIntakeMl) { this.waterIntakeMl = waterIntakeMl; }

    public String getMood() { return mood; }
    public void setMood(String mood) { this.mood = mood; }
}