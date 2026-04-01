package com.fitnesstracker.demo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@SpringBootApplication
public class FitnessTrackerApplication {

    public static void main(String[] args) {
        startAIService();
        SpringApplication.run(FitnessTrackerApplication.class, args);
    }

    @Bean
    CommandLineRunner fixDatabase(JdbcTemplate jdbcTemplate) {
        return args -> {
            new Thread(() -> {
                try {
                    // Așteptăm puțin să se stabilizeze Hibernate
                    Thread.sleep(5000);
                    System.out.println("[DB-FIX] Optimizare tabele pentru AI...");
                    jdbcTemplate.execute("ALTER TABLE recovery_logs MODIFY COLUMN protocol LONGTEXT");
                    jdbcTemplate.execute("ALTER TABLE workouts MODIFY COLUMN details LONGTEXT");
                } catch (Exception e) {
                    System.out.println("[DB-FIX] Tabelele sunt deja configurate corect.");
                }
            }).start();
        };
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // Permite toate rutele
                        .allowedOrigins("http://localhost:3000", "http://localhost:5173") // Porturile standard de React/Vite
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
            }
        };
    }

    private static void startAIService() {
        Thread thread = new Thread(() -> {
            try {
                System.out.println("[JAVA] Inițializare Serviciu AI...");

                if (System.getProperty("os.name").toLowerCase().contains("win")) {
                    Runtime.getRuntime().exec("cmd /c taskkill /F /IM python.exe /T");
                    Thread.sleep(1000);
                }

                String currentDir = System.getProperty("user.dir");
                File pythonDir = findPythonDirectory(currentDir);
                if (pythonDir == null) {
                    System.err.println("[JAVA] EROARE: Directorul AI nu a fost găsit!");
                    return;
                }

                ProcessBuilder pb = new ProcessBuilder("python", "main.py");
                pb.directory(pythonDir);
                pb.inheritIO();
                Process process = pb.start();

                System.out.println("[JAVA] Serviciul AI a pornit pe portul 8006.");

                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    process.descendants().forEach(ProcessHandle::destroyForcibly);
                    process.destroyForcibly();
                }));
            } catch (Exception e) {
                System.err.println("[JAVA] Eroare critică la pornirea AI: " + e.getMessage());
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private static File findPythonDirectory(String startPath) {
        String[] paths = {"ai-service-python", "../ai-service-python", "../../ai-service-python"};
        for (String p : paths) {
            File f = new File(startPath, p);
            if (f.exists() && f.isDirectory()) return f;
        }
        return null;
    }
}