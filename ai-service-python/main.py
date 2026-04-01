import os
import json
import time
import re
import numpy as np  # Necesar pentru regresia de grad 2 solicitată
from google import genai
from fastapi import FastAPI, HTTPException, Body
from pydantic import BaseModel
from typing import List, Optional
from fastapi.middleware.cors import CORSMiddleware
from dotenv import load_dotenv

load_dotenv()
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")

client = None
if GEMINI_API_KEY:
    try:
        client = genai.Client(api_key=GEMINI_API_KEY)
        print("--- DEBUG ATHLETICA AI ---")
        print("Clientul Google GenAI a fost initializat.")
    except Exception as e:
        print(f"[ERR] Initializare Gemini: {str(e)}")

app = FastAPI()

# Permitem comunicarea cu Frontend-ul pentru a evita ERR_CONNECTION_REFUSED
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

class UserProfile(BaseModel):
    username: Optional[str] = "Utilizator"
    fitnessGoal: Optional[str] = "Mentinere"
    age: Optional[int] = 25
    gender: Optional[str] = "Nespecificat"

class WorkoutLog(BaseModel):
    type: Optional[str] = "Antrenament"
    duration: Optional[int] = 0
    intensity: Optional[str] = "Medium"
    details: Optional[str] = ""
    averageHeartRate: Optional[int] = None
    weight: Optional[float] = 0.0  # Necesar pentru PR
    reps: Optional[int] = 0       # Necesar pentru PR

class DailyMetricsLog(BaseModel):
    date: Optional[str] = ""
    sleepHours: Optional[float] = 0.0
    hrv: Optional[int] = 0
    stressLevel: Optional[int] = 0

class PredictionRequest(BaseModel):
    profile: Optional[UserProfile]
    recent_workouts: Optional[List[WorkoutLog]] = []
    daily_metrics: Optional[List[DailyMetricsLog]] = []

class MealAnalysisRequest(BaseModel):
    meal_description: str
    profile: Optional[UserProfile]

class RecoveryRequest(BaseModel):
    sore_parts: str
    pain_level: int
    profile: Optional[UserProfile] = None # Corecție pentru eroarea AttributeError semnalată anterior
    recent_workouts: Optional[List[WorkoutLog]] = []

class PRRequest(BaseModel):
    workouts: List[WorkoutLog]

def get_demo_response(type_key, user="Utilizator"):
    if "nutri" in type_key:
        return {"calories": 450, "protein": 30, "carbs": 50, "fats": 12, "feedback": f"Mod Demo: Masa lui {user} pare echilibrata."}
    if "recovery" in type_key:
        return {"protocol": f"Mod Demo: Recomandam odihna si hidratare pentru {user}.", "estimated_recovery": "24 ore"}
    return {
        "summary": f"Salut {user}! Analiza demo este gata.",
        "recommendation": "Continua antrenamentele si asigura-te ca dormi suficient.",
        "estimated_vo2_max": 44.0,
        "body_battery": 80
    }

async def call_gemini(prompt, type_key, user="Utilizator"):
    if not client:
        return get_demo_response(type_key, user)

    models_to_try = [
        'gemini-2.0-flash-lite-001',
        'gemini-1.5-flash',
        'gemini-2.0-flash'
    ]

    for model_name in models_to_try:
        try:
            response = client.models.generate_content(
                model=model_name,
                contents=prompt,
                config={
                    'system_instruction': f"Esti antrenorul personal al lui {user}. Vorbesti doar in ROMANA. Raspunzi DOAR cu JSON valid."
                }
            )

            if response and response.text:
                text = response.text
                match = re.search(r'\{.*\}', text, re.DOTALL)
                if match:
                    return json.loads(match.group(0))

        except Exception as e:
            continue

    return get_demo_response(type_key, user)

# --- RUTE EXISTENTE ---

@app.post("/predict/daily-advice")
async def get_daily_advice(request: PredictionRequest):
    user = request.profile.username if request.profile else "Utilizator"
    context = f"Utilizator: {user}, Antrenamente: {len(request.recent_workouts)}"
    prompt = f"Analizeaza datele de fitness pentru {user} si ofera sfaturi JSON."
    return await call_gemini(prompt, "advice", user)

@app.post("/predict/meal-analysis")
async def analyze_meal(request: MealAnalysisRequest):
    user = request.profile.username if request.profile else "Utilizator"
    prompt = f"Analizeaza masa: {request.meal_description}. Raspunde JSON."
    return await call_gemini(prompt, "nutri", user)

@app.post("/predict/recovery-protocol")
async def recovery_protocol(request: RecoveryRequest):
    # Folosim .model_dump() pentru compatibilitate Pydantic v2 [cite: 26]
    profile_data = request.profile.model_dump() if request.profile else "Standard"
    prompt = f"Ofera protocol pentru: {request.sore_parts}. Profil: {profile_data}. Raspunde JSON."
    return await call_gemini(prompt, "recovery")

# --- NOU: LOGICA PENTRU PR (CALCUL MATEMATIC) ---

@app.post("/analyze-pr-trend")
async def analyze_pr_trend(data: list = Body(...)):
    if len(data) < 2:
        return {"error": "Date insuficiente", "one_rm": 0, "trend": []}

    weights = [float(d.get('weight', 0)) for d in data]
    reps = [int(d.get('reps', 0)) for d in data]
    x = np.arange(len(weights))

    # Regresie de grad 2 (y = ax^2 + bx + c)
    degree = 2 if len(weights) >= 3 else 1
    model = np.poly1d(np.polyfit(x, weights, degree))

    # Formula Brzycki pentru PR relativ: weight / (1.0278 - 0.0278 * reps)
    last_w, last_r = weights[-1], reps[-1]
    one_rm = last_w / (1.0278 - (0.0278 * last_r)) if last_r > 0 else last_w

    return {
        "trend": [round(float(p), 2) for p in model(x)],
        "one_rm": round(one_rm, 2),
        "next_prediction": round(float(model(len(weights))), 2)
    }

@app.post("/predict/extract-prs")
async def extract_prs(request: PRRequest):
    prompt = "Extrage PR-urile din antrenamente. JSON: records [exercise, value]."
    return await call_gemini(prompt, "prs")

if __name__ == "__main__":
    import uvicorn
    # Pornim pe portul 8006 pentru a repara eroarea de conexiune din Java [cite: 26, 31]
    uvicorn.run(app, host="127.0.0.1", port=8006)