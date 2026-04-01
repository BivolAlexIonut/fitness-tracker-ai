# 🏋️‍♂️ Athletica AI - Your Intelligent Fitness Companion

Athletica AI este o aplicație full-stack modernă concepută pentru monitorizarea performanței fizice, a stării de sănătate și oferirea de recomandări personalizate prin intermediul Inteligenței Artificiale (Google Gemini). Proiectul a fost dezvoltat ca parte a disciplinei **Metode de Dezvoltare Software**.

---

## 🌟 Caracteristici Principale

### 📊 Analiză avansată a Recordurilor Personale (PR)
*   **Monitorizare Evoluție:** Urmărirea progresului pentru peste 15 exerciții fundamentale și olimpice (Snatch, Clean and Jerk, Squat, Deadlift, etc.).
*   **AI 1RM Estimation:** Calcularea automată a "One Rep Max" folosind formula Epley, optimizată chiar și pentru recorduri de o singură repetiție.
*   **Predicții Trend:** Analiză bazată pe regresie polinomială pentru a estima performanțele viitoare și a seta obiective realiste.

### 🧠 Inteligență Artificială Integrată (Gemini)
*   **Daily AI Coach:** Analizează metricile de sănătate și antrenamentele recente pentru a oferi un sfat personalizat în fiecare dimineață.
*   **Nutri-Coach AI:** Introduci ce ai mâncat în format text, iar AI-ul estimează caloriile, macronutrienții și oferă feedback nutrițional.
*   **Protocol de Recuperare:** Raportezi zonele cu febra musculară sau dureri, iar sistemul generează un plan de stretching și recuperare personalizat.

### 📈 Monitorizare Sănătate și Activitate
*   **Metrici Zilnice:** Înregistrarea HRV (Heart Rate Variability), orelor de somn, nivelului de stres și pulsului de repaus.
*   **Istoric Antrenamente:** Jurnal detaliat al activităților fizice (tip, durată, intensitate, calorii arse).

---

## 🛠️ Arhitectură Tehnică

Proiectul este împărțit în trei componente principale care comunică între ele:

1.  **Frontend (Web):** Interfață modernă construită cu **HTML5, CSS3 și JavaScript (Vanilla)**. Folosește Chart.js pentru vizualizarea graficelor de progres.
2.  **Backend (Java):** Construit cu **Spring Boot**, gestionează logica de business, autentificarea și stocarea datelor în **MySQL**.
3.  **AI Service (Python):** Motorul de inteligență artificială bazat pe **FastAPI** și **Google Gemini API**. Se ocupă de calculele matematice complexe și procesarea limbajului natural.

---

## 🚀 Instalare și Configurare

### 1. Baza de Date (MySQL)
*   Creați o bază de date numită `fitness_app_db`.
*   Configurați conexiunea în `backend-java/app/src/main/resources/application.properties`.

### 2. AI Service (Python)
```bash
cd ai-service-python
pip install -r requirements.txt
# Adăugați cheia GEMINI_API_KEY în fișierul .env
python main.py
```

### 3. Backend (Java)
```bash
cd backend-java/app
mvn clean install
mvn spring-boot:run
```

### 4. Frontend
*   Deschideți `frontend-web/auth.html` într-un browser (recomandat folosind Live Server).

---

## 👥 Echipa și Context
Proiect realizat pentru laboratorul de **Metode de Dezvoltare Software**. 
Aplicația pune accent pe integrarea serviciilor distribuite și utilizarea algoritmilor de AI pentru îmbunătățirea experienței utilizatorului în domeniul fitness-ului.
