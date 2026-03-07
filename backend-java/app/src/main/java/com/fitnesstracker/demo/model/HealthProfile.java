package com.fitnesstracker.demo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "health_profiles")
public class HealthProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double height; // înălțime în cm
    private double currentWeight; // greutate curentă în kg
    private double targetWeight; // greutate țintă în kg
    private int age;
    private String gender;
    private int restingHeartRate; // ritm cardiac în repaus
    private int maxHeartRate; // ritm cardiac maxim
    private String activityLevel; // Sedentary, Lightly Active, etc.
    private String fitnessGoal; // Weight Loss, Muscle Gain, Endurance, Maintenance
    private String sportsType; // CrossFit, Running, Swimming, etc.
    private int trainingFrequency; // Days per week
    @Column(length = 1000)
    private String additionalNotes;

    @OneToOne(mappedBy = "healthProfile")
    private User user;

    public String getFitnessGoal() {
        return fitnessGoal;
    }

    public void setFitnessGoal(String fitnessGoal) {
        this.fitnessGoal = fitnessGoal;
    }

    public String getSportsType() {
        return sportsType;
    }

    public void setSportsType(String sportsType) {
        this.sportsType = sportsType;
    }

    public int getTrainingFrequency() {
        return trainingFrequency;
    }

    public void setTrainingFrequency(int trainingFrequency) {
        this.trainingFrequency = trainingFrequency;
    }

    public String getAdditionalNotes() {
        return additionalNotes;
    }

    public void setAdditionalNotes(String additionalNotes) {
        this.additionalNotes = additionalNotes;
    }

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

    public double getCurrentWeight() {
        return currentWeight;
    }

    public void setCurrentWeight(double currentWeight) {
        this.currentWeight = currentWeight;
    }

    public double getTargetWeight() {
        return targetWeight;
    }

    public void setTargetWeight(double targetWeight) {
        this.targetWeight = targetWeight;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public int getRestingHeartRate() {
        return restingHeartRate;
    }

    public void setRestingHeartRate(int restingHeartRate) {
        this.restingHeartRate = restingHeartRate;
    }

    public int getMaxHeartRate() {
        return maxHeartRate;
    }

    public void setMaxHeartRate(int maxHeartRate) {
        this.maxHeartRate = maxHeartRate;
    }

    public String getActivityLevel() {
        return activityLevel;
    }

    public void setActivityLevel(String activityLevel) {
        this.activityLevel = activityLevel;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}