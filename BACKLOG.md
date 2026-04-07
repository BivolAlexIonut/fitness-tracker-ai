# 📋 Product Backlog - Athletica AI

Acest document conține lista de funcționalități planificate și starea implementării lor, urmând metodologia Agile/Scrum.

## 🟢 Finalizate (Implementate)

### 🎨 UI/UX Design (Monochrome)
- **Sarcini:**
  - [x] Tema Monochrome (Black & White) - *Design profesional*
  - [x] Efecte de încărcare (Loading spinners)
  - [x] Formatare avansată a răspunsurilor AI (Liste, Monospace)
  - [x] Design nou pentru secțiunea "Nivel Fitness" (Metrics Grid)

### 👤 Autentificare & Onboarding
- **User Story:** Ca utilizator, vreau să-mi creez un cont și să-mi definesc profilul fizic pentru a primi sfaturi personalizate.
- **Sarcini:** 
  - [x] Ecran de Login/Register (Frontend)
  - [x] API de înregistrare (Backend Java)
  - [x] Profil de sănătate (Vârstă, Obiectiv, Gen)

### 📊 Monitorizare Activitate & Sănătate
- **User Story:** Ca utilizator, vreau să înregistrez antrenamentele și metricile zilnice (HRV, somn) pentru a vedea progresul.
- **Sarcini:**
  - [x] Salvare antrenamente (Java + MySQL)
  - [x] Check-in metrici de sănătate (HRV, Sleep, Stress)
  - [x] Istoric tabelar pentru metrici și antrenamente

### 🤖 Inteligență Artificială (Core)
- **User Story:** Ca utilizator, vreau ca AI-ul să analizeze datele mele pentru a-mi oferi recomandări zilnice.
- **Sarcini:**
  - [x] Integrare Google Gemini API via Python
  - [x] Sfatul zilei bazat pe contextul utilizatorului
  - [x] Analiză personală a recordurilor (1RM Prediction)

### 🧪 Testare & Calitate (Cerință Proiect)
- **Sarcini:**
  - [x] Implementare Teste Unitare în Java (JUnit) - *Validare Controller & Business Logic*
  - [x] Implementare Teste Unitare în Python (Pytest) - *Validare Algoritmi AI & Edge Cases*
  - [x] Teste de Integritate AI (Stabilitate VO2Max)
  - [x] Documentație completă de Debugging & Testare (AUTOMATION_TESTS.md)

### 💬 Asistenți Specializați
- **User Story:** Ca utilizator, vreau să pot vorbi cu asistenți specializați pe nutriție și recuperare.
- **Sarcini:**
  - [x] Generator de mese bazat pe ingredientele din casă
  - [x] Chatbot pentru recuperare (conversativ)
  - [x] Generator de antrenamente personalizate (ex: CrossFit)

---

## 🟡 În Lucru (In Progress)

### 📈 Vizualizare Avansată
- **User Story:** Ca utilizator, vreau să văd grafice evolutive pentru a înțelege trendurile pe termen lung.
- **Sarcini:**
  - [/] Grafic evoluție PR-uri (Trendline AI)
  - [ ] Grafic comparativ HRV vs Stress pe 30 de zile

---

## 🔴 De Implementat (Future / To Do)

### 🔔 Notificări & Gamification
- **Sarcini:**
  - [ ] Sistem de "Streaks" pentru utilizatorii care fac check-in zilnic
  - [ ] Badge-uri AI pentru atingerea obiectivelor de PR
