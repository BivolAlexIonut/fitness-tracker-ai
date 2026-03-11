# 🏋️‍♂️ Fitness Tracker AI (Athletica AI) - Documentație Completă

Acesta este un proiect complex de monitorizare a fitness-ului și sănătății, care utilizează Inteligența Artificială pentru a oferi utilizatorilor recomandări personalizate, analize nutriționale și planuri de recuperare.

---

## 🎯 Scopul Proiectului
Scopul acestei aplicații este de a transforma datele brute despre antrenamente și nutriție în informații acționabile. Prin integrarea modelelor de limbaj mari (Google Gemini), aplicația acționează ca un antrenor personal digital care:
*   Analizează progresul în timp real.
*   Oferă feedback nutrițional detaliat bazat pe descrieri textuale.
*   Generează protocoale de recuperare în funcție de durerile musculare raportate.
*   Extrage recorduri personale (PR) din istoricul de antrenamente.

---

## 🏗️ Arhitectura Sistemului

Proiectul este împărțit în trei mari componente care comunică între ele:

1.  **Backend (Java Spring Boot):** 
    *   Gestionează logica de business, autentificarea și securitatea.
    *   Administrează baza de date MySQL prin Hibernate ORM.
    *   Expune API-uri REST pentru frontend și comunică cu serviciul de AI.
2.  **AI Service (Python FastAPI):**
    *   Interfața cu modelele Google Gemini (Flash 2.0 / Pro).
    *   Procesează datele de fitness și returnează răspunsuri structurate JSON.
    *   Include un mod "Demo" (fallback) în cazul în care API Key-ul lipsește.
3.  **Frontend (Web Static):**
    *   Interfață curată construită cu HTML5, CSS3 și JavaScript (Vanilla).
    *   Comunică direct cu backend-ul pentru afișarea datelor.
4.  **Baza de Date (MySQL):**
    *   Stochează profilul utilizatorului, jurnalele de masă, antrenamentele și metricile zilnice.

---

## 🛠️ Instrucțiuni de Instalare (Setup)

Urmați acești pași pentru a configura proiectul pe un calculator nou după `git clone`.

### 1. Cerințe Prealabile
Asigurați-vă că aveți instalate următoarele:
*   **Docker Desktop** (pentru baza de date).
*   **Java JDK 17** sau mai nou.
*   **Python 3.10+**.
*   **Maven** (sau folosiți wrapper-ul `mvnw`).
*   **Un API Key Google Gemini** (opțional, dar recomandat).

### 2. Configurarea Bazei de Date (Docker)
Porniți containerul de MySQL folosind fișierul `docker-compose.yml` aflat în rădăcina proiectului:
```bash
docker-compose up -d
```
*Acest lucru va porni o bază de date MySQL pe portul 3306 cu numele `fitness_app_db`.*

### 3. Configurarea Serviciului de AI (Python)
1. Intrați în folderul dedicat:
   ```bash
   cd ai-service-python
   ```
2. Creați un mediu virtual și instalați dependențele:
   ```bash
   python -m venv venv
   source venv/bin/activate  # Pe Windows: venv\Scripts\activate
   pip install -r requirements.txt
   ```
3. (Opțional) Creați un fișier `.env` în acest folder și adăugați cheia API:
   ```env
   GEMINI_API_KEY=cheia_ta_aici
   ```
4. Porniți serviciul:
   ```bash
   python main.py
   ```
   *Serviciul va rula pe http://localhost:8005.*

### 4. Configurarea Backend-ului (Java)
1. Intrați în folderul backend:
   ```bash
   cd backend-java/app
   ```
2. Verificați setările bazei de date în `src/main/resources/application.properties` (implicit sunt setate pentru Docker).
3. Construiți și porniți aplicația:
   ```bash
   ./mvnw spring-boot:run
   ```
   *Backend-ul va rula pe http://localhost:8080.*

### 5. Accesarea Interfeței (Frontend)
Nu este nevoie de un server de build (ca la React). 
1. Navigați în folderul `frontend-web/`.
2. Deschideți fișierul `auth.html` sau `index.html` direct în browser (sau folosiți extensia "Live Server" din VS Code).

---

## 🚀 Utilizare Rapidă
*   **Înregistrare:** Creați un cont în pagina de Auth.
*   **Onboarding:** Completați profilul fizic (vârstă, greutate, scop).
*   **Analiză AI:** Adăugați un antrenament sau o masă, apoi mergeți la "AI Insights" pentru a primi sfaturi personalizate.

---

## 📝 Note Tehnice
*   **Porturi:** MySQL (3306), Java (8080), Python (8005).
*   **Fallback AI:** Dacă nu aveți o cheie Gemini, serviciul Python va returna date "Demo" predefinite pentru a permite testarea interfeței.
