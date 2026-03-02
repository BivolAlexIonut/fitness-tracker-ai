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
    private int hrv;
    private int stressLevel;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}