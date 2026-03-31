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

app = FastAPI()

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
    profile: Optional[UserProfile]
    recent_workouts: Optional[List[WorkoutLog]] = []
    daily_metrics: Optional[List[DailyMetricsLog]] = []

class MealAnalysisRequest(BaseModel):
    meal_description: str
    profile: Optional[UserProfile]

class RecoveryRequest(BaseModel):
    sore_parts: str
    pain_level: int
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
                print("[WARN] Cota atinsa pentru acest model. Incercam urmatorul...")
                time.sleep(1)
                continue
            
            if "404" in error_msg:
                continue
 
    print("[FINAL] Toate modelele au esuat sau cota e plina. Trimitem date Demo.")
    return get_demo_response(type_key, user)


@app.post("/predict/daily-advice")
async def get_daily_advice(request: PredictionRequest):
    user = request.profile.username if request.profile else "Utilizator"
    
    # Context bogat pentru AI bazat pe datele primite
    context = f"Utilizator: {user}, Scop: {request.profile.fitnessGoal}, Vârstă: {request.profile.age}\n"
    context += f"Antrenamente recente: {json.dumps([w.model_dump() for w in request.recent_workouts], indent=2)}\n"
    context += f"Metrici sănătate: {json.dumps([m.model_dump() for m in request.daily_metrics], indent=2)}\n"
    
    prompt = f"""
    Context Date Utilizator:
    {context}

    Sarcina: Ești un expert în fiziologie sportivă. Analizează datele și oferă un raport detaliat în ROMÂNĂ.
    1. Compară efortul din antrenamente cu calitatea somnului și HRV-ul.
    2. Oferă un rezumat (summary) de minim 40-60 cuvinte, foarte specific (ex: 'Deoarece ai avut un antrenament intens de tip {request.recent_workouts[0].type if request.recent_workouts else "cardio"} ieri...').
    3. Oferă o recomandare clară pentru antrenamentul de azi.

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
    prompt = f"""
    Masa descrisă: {request.meal_description}
    Profil utilizator: {request.profile.dict() if request.profile else "Standard"}
    
    Analizează această masă în contextul fitness-ului.
    Răspunde DOAR cu JSON:
    {{
      "calories": număr,
      "protein": număr,
      "carbs": număr,
      "fats": număr,
      "feedback": "explicație detaliată în română despre cum ajută această masă la scopul de {request.profile.fitnessGoal if request.profile else "sănătate"}"
    }}
    """
    return await call_gemini(prompt, "nutri", user)

@app.post("/predict/recovery-protocol")
async def recovery_protocol(request: RecoveryRequest):
    prompt = f"""
    Durere raportată: {request.sore_parts} (Nivel: {request.pain_level}/10)
    Profil utilizator: {request.profile.model_dump() if request.profile else "Standard"}
    
    Oferă un protocol de recuperare sportivă profesional în ROMÂNĂ.
    Răspunde DOAR cu JSON:
    {{
      "protocol": "listă detaliată de pași, stretching, nutriție, odihnă",
      "estimated_recovery": "ex: 36 ore"
    }}
    """
    return await call_gemini(prompt, "recovery")

@app.post("/predict/extract-prs")
async def extract_prs(request: PRRequest):
    prompt = "Extrage PR-urile din antrenamentele furnizate. JSON: records [exercise, value, icon]."
    return await call_gemini(prompt, "prs")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8006)