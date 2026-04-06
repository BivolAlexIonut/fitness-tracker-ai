package com.fitnesstracker.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "fitness_levels")
public class FitnessLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private Double estimatedVO2Max;       // VO2 MAX estimat
    private Integer fitnessLevelScore;    // 1-10 sau 1-100
    private String fitnessCategory;       // "Beginner", "Intermediate", "Advanced", "Elite"

    private Double estimated5kTime;       // Timp estimat 5K (minute)
    private Double estimated10kTime;      // Timp estimat 10K (minute)
    private Double estimatedMarathonTime; // Timp estimat maraton (ore)

    private Integer bodyBattery;           // Body battery score (0-100) - ușor de schimbat

    private Integer pushupEstimate;       // Estimare max push-ups consecutive
    private Integer pullupEstimate;       // Estimare max pull-ups consecutive
    private Double benchPressEstimate;    // Estimare max bench press (kg)
    private Double deadliftEstimate;      // Estimare max deadlift (kg)

    private String aiInsights;            // Note/recomandări din AI
    @Column(length = 2000)
    private String strengthWeaknesses;    // Puncte forte și slabe

    private LocalDateTime lastUpdated;    // Când au fost actualizate datele
    private LocalDateTime createdAt;      // Când a fost creat recordul

    // Constructori
    public FitnessLevel() {}

    public FitnessLevel(User user) {
        this.user = user;
        this.createdAt = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Double getEstimatedVO2Max() { return estimatedVO2Max; }
    public void setEstimatedVO2Max(Double estimatedVO2Max) { this.estimatedVO2Max = estimatedVO2Max; }

    public Integer getFitnessLevelScore() { return fitnessLevelScore; }
    public void setFitnessLevelScore(Integer fitnessLevelScore) { this.fitnessLevelScore = fitnessLevelScore; }

    public String getFitnessCategory() { return fitnessCategory; }
    public void setFitnessCategory(String fitnessCategory) { this.fitnessCategory = fitnessCategory; }

    public Double getEstimated5kTime() { return estimated5kTime; }
    public void setEstimated5kTime(Double estimated5kTime) { this.estimated5kTime = estimated5kTime; }

    public Double getEstimated10kTime() { return estimated10kTime; }
    public void setEstimated10kTime(Double estimated10kTime) { this.estimated10kTime = estimated10kTime; }

    public Double getEstimatedMarathonTime() { return estimatedMarathonTime; }
    public void setEstimatedMarathonTime(Double estimatedMarathonTime) { this.estimatedMarathonTime = estimatedMarathonTime; }

    public Integer getBodyBattery() { return bodyBattery; }
    public void setBodyBattery(Integer bodyBattery) { this.bodyBattery = bodyBattery; }

    public Integer getPushupEstimate() { return pushupEstimate; }
    public void setPushupEstimate(Integer pushupEstimate) { this.pushupEstimate = pushupEstimate; }

    public Integer getPullupEstimate() { return pullupEstimate; }
    public void setPullupEstimate(Integer pullupEstimate) { this.pullupEstimate = pullupEstimate; }

    public Double getBenchPressEstimate() { return benchPressEstimate; }
    public void setBenchPressEstimate(Double benchPressEstimate) { this.benchPressEstimate = benchPressEstimate; }

    public Double getDeadliftEstimate() { return deadliftEstimate; }
    public void setDeadliftEstimate(Double deadliftEstimate) { this.deadliftEstimate = deadliftEstimate; }

    public String getAiInsights() { return aiInsights; }
    public void setAiInsights(String aiInsights) { this.aiInsights = aiInsights; }

    public String getStrengthWeaknesses() { return strengthWeaknesses; }
    public void setStrengthWeaknesses(String strengthWeaknesses) { this.strengthWeaknesses = strengthWeaknesses; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

