@echo off
echo ==========================================
echo    PORNIRE ATHLETICA AI SERVICES
echo ==========================================

:: 1. Pornire Serviciu AI (Python)
echo [+] Pornire AI Service (FastAPI)...
start "AI Service - Python" cmd /k "cd ai-service-python && pip install -r requirements.txt && python main.py"

:: 2. Pornire Backend (Java)
echo [+] Pornire Backend (Spring Boot)...
echo [!] Asigura-te ca ai Maven instalat (mvnw).
start "Backend - Java" cmd /k "cd backend-java/app && ./mvnw spring-boot:run"

echo ==========================================
echo  Serviciile pornesc in ferestre separate.
echo  Poti accesa interfata in browser (auth.html).
echo ==========================================
pause
