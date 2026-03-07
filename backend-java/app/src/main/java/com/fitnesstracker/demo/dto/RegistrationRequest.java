package com.fitnesstracker.demo.dto;

public class RegistrationRequest {
    private String username;
    private String email;
    private String password;
    private double height;
    private double currentWeight;
    private double targetWeight;
    private int age;
    private String gender;
    private int restingHeartRate;
    private int maxHeartRate;
    private String activityLevel;

    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public double getHeight() { return height; }
    public void setHeight(double height) { this.height = height; }
    public double getCurrentWeight() { return currentWeight; }
    public void setCurrentWeight(double currentWeight) { this.currentWeight = currentWeight; }
    public double getTargetWeight() { return targetWeight; }
    public void setTargetWeight(double targetWeight) { this.targetWeight = targetWeight; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public int getRestingHeartRate() { return restingHeartRate; }
    public void setRestingHeartRate(int restingHeartRate) { this.restingHeartRate = restingHeartRate; }
    public int getMaxHeartRate() { return maxHeartRate; }
    public void setMaxHeartRate(int maxHeartRate) { this.maxHeartRate = maxHeartRate; }
    public String getActivityLevel() { return activityLevel; }
    public void setActivityLevel(String activityLevel) { this.activityLevel = activityLevel; }
}
