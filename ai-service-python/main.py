import os
import json
import google.generativeai as genai
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List, Optional
from dotenv import load_dotenv

# Forțăm încărcarea .env din directorul curent
load_dotenv()

GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")

print(f"--- DEBUG ATHLETICA AI ---")
if not GEMINI_API_KEY:
    print("CRITICAL: Cheia API NU a fost găsită în .env!")
else:
    print(f"Cheia API a fost încărcată: {GEMINI_API_KEY[:5]}...{GEMINI_API_KEY[-5:]}")

if GEMINI_API_KEY:
    genai.configure(api_key=GEMINI_API_KEY)
    # Folosim Flash pentru stabilitate mai mare pe free tier
    model = genai.GenerativeModel('gemini-1.5-flash')

app = FastAPI(title="Athletica AI - LLM Powered")

class UserProfile(BaseModel):
    username: str
    fitnessGoal: str
    sportsType: str
    trainingFrequency: int
    currentWeight: float
    targetWeight: float
    age: int
    gender: str

class WorkoutLog(BaseModel):
    workout_type: str
    duration: int
    intensity: str
    notes: Optional[str] = ""

class PredictionRequest(BaseModel):
    profile: UserProfile
    recent_workouts: List[WorkoutLog]

@app.post("/predict/daily-advice")
async def get_daily_advice(request: PredictionRequest):
    profile = request.profile
    workouts = request.recent_workouts
    
    workout_history = "\n".join([f"- {w.workout_type}, {w.duration} min, intensitate {w.intensity}" for w in workouts])
    
    prompt = f"""
    Ești un antrenor de fitness expert AI pentru aplicația 'Athletica AI'.
    Utilizator: {profile.username}, {profile.age} ani, {profile.gender}.
    Obiectiv: {profile.fitnessGoal}. Sport principal: {profile.sportsType}.
    Greutate actuală: {profile.currentWeight}kg, Țintă: {profile.targetWeight}kg.
    
    Istoric antrenamente recente:
    {workout_history if workouts else "Niciun antrenament înregistrat încă."}
    
    Analizează starea utilizatorului și oferă un sfat personalizat pentru AZI sub formă de JSON cu următoarele chei:
    - "summary": O scurtă analiză a progresului/oboselii (maxim 2 propoziții).
    - "recommendation": Sfat specific pentru antrenamentul de azi (maxim 2 propoziții).
    - "estimated_vo2_max": Un număr realist (ex: 45.2).
    - "body_battery": Un număr între 0-100 bazat pe efortul recent.
    
    Răspunde DOAR cu JSON-ul valid, fără text suplimentar.
    """

    if not GEMINI_API_KEY or len(GEMINI_API_KEY) < 10:
        return {
            "summary": "Mod Demo: Nu s-a detectat o cheie API validă în .env.",
            "recommendation": "Te rugăm să configurezi GEMINI_API_KEY.",
            "estimated_vo2_max": 40.0,
            "body_battery": 95
        }

    try:
        response = model.generate_content(prompt)
        text_response = response.text
        
        start_idx = text_response.find('{')
        end_idx = text_response.rfind('}') + 1
        
        if start_idx == -1 or end_idx == 0:
             print(f"DEBUG: Răspuns AI non-JSON: {text_response}")
             raise ValueError("AI nu a returnat un JSON valid")
        
        json_str = text_response[start_idx:end_idx]
        return json.loads(json_str)
    except Exception as e:
        print(f"--- EROARE AI DETALIAȚĂ ---")
        print(e)
        return {
            "summary": "AI-ul a întâmpinat o problemă de conexiune sau de regiune.",
            "recommendation": "Verifică consola Python pentru eroarea detaliată.",
            "estimated_vo2_max": 0,
            "body_battery": 0
        }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
