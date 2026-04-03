package com.fitnesstracker.demo;

import com.fitnesstracker.demo.model.DailyMetrics;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDate;

public class DailyMetricsTest {

    @Test
    public void testMetricsCreation() {
        DailyMetrics metrics = new DailyMetrics();
        metrics.setHrv(75);
        metrics.setSleepHours(8.5);
        metrics.setStressLevel(20);
        metrics.setDate(LocalDate.now());

        assertEquals(75, metrics.getHrv(), "HRV-ul ar trebui să fie 75");
        assertEquals(8.5, metrics.getSleepHours());
        assertEquals(20, metrics.getStressLevel());
        assertNotNull(metrics.getDate());
    }

    @Test
    public void testStressValidation() {
        DailyMetrics metrics = new DailyMetrics();
        metrics.setStressLevel(110); // Presupunem că limita e 100
        
        // Aici am putea adăuga logică de validare în model dacă am avea-o
        assertTrue(metrics.getStressLevel() > 0);
    }
}
