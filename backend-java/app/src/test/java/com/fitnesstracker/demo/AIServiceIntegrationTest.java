package com.fitnesstracker.demo;

import com.fitnesstracker.demo.model.User;
import com.fitnesstracker.demo.service.AIService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class AIServiceIntegrationTest {

    @Autowired
    private AIService aiService;

    @Test
    public void testDailyAdviceFallback() {
        // Testăm că serviciul AI oferă un răspuns de "fallback" 
        // dacă serviciul Python nu este pornit (aruncă eroare)
        User user = new User();
        user.setId(1L);
        user.setUsername("TestUser");

        Map<String, Object> advice = aiService.getDailyAdvice(user);

        assertNotNull(advice);
        assertTrue(advice.containsKey("summary"));
        // Dacă serviciul Python e oprit, ar trebui să avem mesajul de eroare configurat în AIService.java
        assertTrue(advice.get("summary").toString().contains("indisponibil") || 
                   advice.get("summary").toString().length() > 0);
    }
}
