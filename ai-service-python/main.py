import os
import json
import time
import re
import sys
import numpy as np
from google import genai
from fastapi import FastAPI, HTTPException, Body
from pydantic import BaseModel
from typing import List, Optional
from fastapi.middleware.cors import CORSMiddleware
from dotenv import load_dotenv

# Fix pentru caractere românești în consola Windows
if sys.platform == "win32":
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8')

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

# Permitem comunicarea cu Frontend-ul
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
    weight: Optional[float] = 0.0
    reps: Optional[int] = 0

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
    profile: Optional[UserProfile] = None
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
                    'system_instruction': f"Esti antrenorul personal al lui {user}. Vorbesti doar in ROMANA. Raspunzi DOAR cu JSON valid."
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
                time.sleep(1)
                continue
            if "404" in error_msg:
                continue
 
    print("[FINAL] Toate modelele au esuat. Trimitem date Demo.")
    return get_demo_response(type_key, user)

@app.post("/predict/daily-advice")
async def get_daily_advice(request: PredictionRequest):
    user = request.profile.username if request.profile else "Utilizator"
    context = f"Utilizator: {user}, Scop: {request.profile.fitnessGoal}, Vârstă: {request.profile.age}\n"
    context += f"Antrenamente recente: {json.dumps([w.model_dump() for w in request.recent_workouts], indent=2)}\n"
    context += f"Metricile de azi: {json.dumps([m.model_dump() for m in request.daily_metrics], indent=2)}\n"
    
    prompt = f"""
    Context Date Utilizator: {context}
    Analizează datele și oferă un raport detaliat în ROMÂNĂ.
    Răspunde DOAR cu JSON:
    {{
      "summary": "text lung aici",
      "recommendation": "sfat scurt",
      "estimated_vo2_max": 45.0,
      "body_battery": 85
    }}
    """
    return await call_gemini(prompt, "advice", user)

@app.post("/predict/meal-analysis")
async def analyze_meal(request: MealAnalysisRequest):
    user = request.profile.username if request.profile else "Utilizator"
    prompt = f"Analizează masa: {request.meal_description}. Răspunde DOAR cu JSON: {{'calories': număr, 'protein': număr, 'carbs': număr, 'fats': număr, 'feedback': 'text'}}"
    return await call_gemini(prompt, "nutri", user)

@app.post("/predict/recovery-protocol")
async def recovery_protocol(request: RecoveryRequest):
    prompt = f"Ofera protocol pentru: {request.sore_parts}. Raspunde JSON: {{'protocol': 'text', 'estimated_recovery': 'text'}}"
    return await call_gemini(prompt, "recovery")

@app.post("/analyze-pr-trend")
async def analyze_pr_trend(data: list = Body(...)):
    if not data: return {"error": "Lipsesc datele"}
    
    weights = [float(d.get('weight', 0)) for d in data]
    reps = [int(d.get('reps', 1)) for d in data]
    
    # Calcul 1RM (Epley formula) pentru ultimul record
    last_weight = weights[-1]
    last_reps = reps[-1]
    one_rm = round(last_weight * (1 + 0.0333 * last_reps), 2) if last_reps > 1 else last_weight
    
    if len(weights) < 2:
        return {
            "trend": [last_weight],
            "one_rm": one_rm,
            "next_prediction": last_weight # Fără istoric, predicția e greutatea actuală
        }
        
    x = np.arange(len(weights))
    # Folosim grad 2 doar dacă avem cel puțin 3 puncte, altfel grad 1 (linie dreaptă)
    degree = 2 if len(weights) >= 3 else 1
    model = np.poly1d(np.polyfit(x, weights, degree))
    
    return {
        "trend": [round(float(p), 2) for p in model(x)],
        "one_rm": one_rm,
        "next_prediction": round(float(model(len(weights))), 2)
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="127.0.0.1", port=8006)
