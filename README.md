# 🏋️‍♂️ Fitness Tracker AI - Team Project

Aceasta este o aplicație avansată de monitorizare a fitness-ului dezvoltată pe parcursul a 5 luni. Proiectul integrează **Java** pentru logica de business, **Python** pentru Inteligență Artificială și **Docker** pentru o infrastructură stabilă.

---

## 🏗️ Arhitectura Sistemului

Proiectul este structurat în trei module principale (microservicii):

* **`backend-java/`**: Inima aplicației (Spring Boot). Gestionează baza de date prin **Hibernate ORM** (Relații One-to-One).
* **`ai-service-python/`**: Creierul aplicației (FastAPI). Rulează modelele de AI pentru predicția progresului.
* **`frontend-web/`**: Interfața utilizatorului (HTML/CSS/JS).

---

## 🤖 Modele de Inteligență Artificială

Vom implementa două modele specifice în Python:
1.  **Analiza Progresului:** Predicția evoluției greutății pe baza activității din ultima lună.
2.  **Antrenamente Inteligente:** Recomandări de exerciții bazate pe profilul fizic al utilizatorului.

---

## 🛠️ Ghid de Instalare pentru Echipă

Pentru a lucra pe acest proiect, asigurați-vă că aveți instalat **Docker Desktop**.

### 1. Clonarea Proiectului
```bash
git clone [https://github.com/BivolAlexIonut/fitness-tracker-ai.git](https://github.com/BivolAlexIonut/fitness-tracker-ai.git)
cd fitness-tracker-ai
