package com.fitnesstracker.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.File;

@SpringBootApplication
public class FitnessTrackerApplication {

	public static void main(String[] args) {
		SpringApplication.run(FitnessTrackerApplication.class, args);
	}

    @PostConstruct
    public void startPythonAI() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            try {
                // Încercăm mai multe căi posibile pentru a găsi folderul AI
                String[] pathsToTry = {
                    "../../ai-service-python", // Din backend-java/app/
                    "../ai-service-python",    // Din backend-java/
                    "./ai-service-python"      // Din rădăcină
                };

                File aiDir = null;
                for (String path : pathsToTry) {
                    File tempDir = new File(path);
                    if (tempDir.exists() && tempDir.isDirectory()) {
                        aiDir = tempDir;
                        break;
                    }
                }
                
                if (aiDir != null) {
                    System.out.println("[INFO] S-a găsit serviciul AI la: " + aiDir.getAbsolutePath());
                    System.out.println("[INFO] Se pornește serviciul AI Python...");
                    
                    ProcessBuilder builder = new ProcessBuilder(
                        "cmd.exe", "/c", 
                        "start \"Athletica AI Engine\" cmd /k \"cd /d " + aiDir.getAbsolutePath() + " && pip install -r requirements.txt && python main.py\""
                    );
                    builder.start();
                } else {
                    System.err.println("[EROARE] Nu s-a găsit folderul ai-service-python în nicio locație relativă.");
                }
            } catch (IOException e) {
                System.err.println("[EROARE] Nu s-a putut porni serviciul Python: " + e.getMessage());
            }
        }
    }
}