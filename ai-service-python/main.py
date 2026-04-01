import os
import json
import time
import re
from google import genai
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List, Optional
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

app = FastAPI(title="Athletica AI Engine")

# --- MODELE DE DATE (PYDANTIC) ---
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

class DailyMetricsLog(BaseModel):
    date: Optional[str] = ""
    sleepHours: Optional[float] = 0.0
    hrv: Optional[int] = 0
    stressLevel: Optional[int] = 0

class PredictionRequest(BaseModel):
    profile: Optional[UserProfile] = None
    recent_workouts: Optional[List[WorkoutLog]] = []
    daily_metrics: Optional[List[DailyMetricsLog]] = []

class MealAnalysisRequest(BaseModel):
    meal_description: str
    profile: Optional[UserProfile] = None

class RecoveryRequest(BaseModel):
    sore_parts: str
    pain_level: int
    recent_workouts: Optional[List[WorkoutLog]] = []
    profile: Optional[UserProfile] = None # BUG REPARAT AICI

class PRRequest(BaseModel):
    workouts: List[WorkoutLog]


# --- FUNCTII DE GENERARE SI FALLBACK ---
def get_demo_response(type_key, user="Utilizator"):
    if "nutri" in type_key:
        return {"calories": 450, "protein": 30, "carbs": 50, "fats": 12, "feedback": f"Mod Demo: Masa lui {user} pare echilibrată și bogată în proteine."}
    if "recovery" in type_key:
        return {"protocol": f"Mod Demo: Recomandăm odihnă activă, hidratare și stretching ușor pentru {user}.", "estimated_recovery": "24-48 ore"}
    if "prs" in type_key:
        return {"records": [{"exercise": "Mod Demo: Împins", "value": "100kg", "icon": "dumbbell"}]}
    return {
        "summary": f"Salut {user}! Analiza demo este gata.",
        "recommendation": "Continuă antrenamentele și asigură-te că dormi suficient.",
        "estimated_vo2_max": 44.0,
        "body_battery": 80
    }

async def call_gemini(prompt, type_key, user="Utilizator"):
    if not client:
        return get_demo_response(type_key, user)

    models_to_try = [
        'gemini-flash-lite-latest',
        'gemini-2.0-flash-lite-001',
        'gemini-3-flash-preview',
        'gemini-2.0-flash'
    ]

    for model_name in models_to_try:
        try:
            print(f"--- Incercam modelul: {model_name} ---")
            response = client.models.generate_content(
                model=model_name,
                contents=prompt,
                config={
                    'system_instruction': f"Ești antrenorul personal al lui {user}. Vorbești doar în ROMÂNĂ. Răspunzi DOAR cu JSON valid."
                }
            )

            if response and response.text:
                text = response.text
                match = re.search(r'\{.*\}', text, re.DOTALL)
                if match:
                    res = json.loads(match.group(0))
                    print(f"[OK] Succes cu modelul: {model_name}")
                    return res

        except Exception as e:
            error_msg = str(e)
            print(f"[ERR] {model_name} a esuat: {error_msg[:100]}...")

            if "429" in error_msg or "RESOURCE_EXHAUSTED" in error_msg:
                print("[WARN] Cota atinsă pentru acest model. Încercăm următorul...")
                time.sleep(1)
                continue

            if "404" in error_msg:
                continue

    print("[FINAL] Toate modelele au eșuat sau cota e plină. Trimitem date Demo.")
    return get_demo_response(type_key, user)


# --- ENDPOINT-URI API ---

@app.get("/health")
async def health_check():
    """Endpoint de monitorizare a stării microserviciului."""