package com.fitnesstracker.demo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "health_profiles")
public class HealthProfile {
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public double getTargetWeight() {
        return targetWeight;
    }

    public void setTargetWeight(double targetWeight) {
        this.targetWeight = targetWeight;
    }

    public int getMaxHeartRate() {
        return maxHeartRate;
    }

    public void setMaxHeartRate(int maxHeartRate) {
        this.maxHeartRate = maxHeartRate;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double height; // înălțime în cm
    private double targetWeight; // greutate țintă în kg
    private int maxHeartRate; // ritm cardiac maxim

    @OneToOne(mappedBy = "healthProfile")
    private User user;
}