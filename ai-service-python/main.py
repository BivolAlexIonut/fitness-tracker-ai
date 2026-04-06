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

# Fix pentru caractere românești în consola Windows (dezactivat în timpul testelor)
if sys.platform == "win32" and "pytest" not in sys.modules:
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

class WorkoutProposalRequest(BaseModel):
    user_input: str
    profile: Optional[UserProfile]
    recent_workouts: Optional[List[WorkoutLog]] = []
    daily_metrics: Optional[List[DailyMetricsLog]] = []

class MealAnalysisRequest(BaseModel):
    meal_description: str
    profile: Optional[UserProfile]

class MealProposalRequest(BaseModel):
    ingredients: str
    profile: Optional[UserProfile]
    recent_workouts: Optional[List[WorkoutLog]] = []
    daily_metrics: Optional[List[DailyMetricsLog]] = []

class RecoveryRequest(BaseModel):
    user_message: str
    chat_history: Optional[List[dict]] = []
    profile: Optional[UserProfile] = None
    recent_workouts: Optional[List[WorkoutLog]] = []
    daily_metrics: Optional[List[DailyMetricsLog]] = []

class PRRequest(BaseModel):
    workouts: List[WorkoutLog]

class FitnessSummaryRequest(BaseModel):
    profile: Optional[UserProfile] = None
    recent_workouts: Optional[List[WorkoutLog]] = []
    daily_metrics: Optional[List[DailyMetricsLog]] = []
    fitness_level: Optional[dict] = None

def get_demo_response(type_key, user="Utilizator"):
    if "nutri" in type_key:
        return {"calories": 450, "protein": 30, "carbs": 50, "fats": 12, "feedback": f"Mod Demo: Masa lui {user} pare echilibrata."}
    if "recovery" in type_key:
        return {"protocol": f"Mod Demo: Recomandam odihna si hidratare pentru {user}.", "estimated_recovery": "24 ore"}
    return {
        "vo2_max": 45.5,
        "fitness_level_score": 7,
        "fitness_category": "Intermediate",
        "estimated_5k_time": 25.5,
        "estimated_10k_time": 54.0,
        "estimated_marathon_time": 3.5,
        "pushup_estimate": 35,
        "pullup_estimate": 12,
        "bench_press_estimate": 100.0,
        "deadlift_estimate": 150.0,
        "body_battery": 72,
        "ai_insights": f"Salut {user}! Analiza demo este gata. Ești într-o formă bună. Continuă antrenamentele!",
        "strength_weaknesses": "Puncte forte: cardio. Slăbiciuni: forță în partea superioară.",
        "summary": "Analiza ta de fitness (Mod Demo) este completă."
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
@app.post("/predict/recovery-chat")
async def recovery_chat(request: RecoveryRequest):
    user = request.profile.username if request.profile else "Utilizator"
    context = f"Utilizator: {user}, Scop: {request.profile.fitnessGoal}\n"
    context += f"Antrenamente recente: {json.dumps([w.model_dump() for w in request.recent_workouts], indent=2)}\n"
    context += f"Metricile de azi: {json.dumps([m.model_dump() for m in request.daily_metrics], indent=2)}\n"

    history_str = "\n".join([f"{m['role']}: {m['content']}" for m in request.chat_history])

    prompt = f"""
    Context Fizic: {context}
    Istoric Chat: {history_str}
    Ultimul Mesaj Utilizator: {request.user_message}

    Esti un asistent expert in recuperare sportiva. 
    Daca utilizatorul spune ca il doare ceva, intreaba-l detalii (intensitate, cand a aparut, tipul durerii) inainte de a da un protocol final.
    Daca ai destule detalii, ofera un protocol clar de recuperare (stretching, gheata, repaus etc.).

    Răspunde DOAR cu JSON:
    {{
      "message": "raspunsul tau catre utilizator aici",
      "is_final_protocol": true/false
    }}
    """
    return await call_gemini(prompt, "recovery", user)

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

@app.post("/predict/workout-proposal")
async def get_workout_proposal(request: WorkoutProposalRequest):
    user = request.profile.username if request.profile else "Utilizator"
    context = f"Utilizator: {user}, Scop: {request.profile.fitnessGoal}, Vârstă: {request.profile.age}\n"
    context += f"Antrenamente recente (ultimele 10): {json.dumps([w.model_dump() for w in request.recent_workouts], indent=2)}\n"
    context += f"Metricile de sănătate recente: {json.dumps([m.model_dump() for m in request.daily_metrics], indent=2)}\n"
    
    prompt = f"""
    Context Date Utilizator: {context}
    Cerere Utilizator: {request.user_input}
    
    Propune un antrenament personalizat în ROMÂNĂ bazat pe cererea utilizatorului și starea lui actuală (metrici, antrenamente anterioare).
    Dacă utilizatorul este obosit (HRV mic, somn puțin, stres mare), propune ceva mai ușor chiar dacă el cere ceva intens.
    
    Răspunde DOAR cu JSON:
    {{
      "workout_name": "Numele antrenamentului",
      "exercises": "Lista de exerciții, seturi, repetări sub formă de text",
      "ai_notes": "De ce am propus asta bazat pe datele tale"
    }}
    """
    return await call_gemini(prompt, "workout", user)

@app.post("/predict/meal-proposal")
async def get_meal_proposal(request: MealProposalRequest):
    user = request.profile.username if request.profile else "Utilizator"
    context = f"Utilizator: {user}, Scop: {request.profile.fitnessGoal}, Vârstă: {request.profile.age}\n"
    context += f"Antrenamente recente: {json.dumps([w.model_dump() for w in request.recent_workouts], indent=2)}\n"
    context += f"Metricile de sănătate recente: {json.dumps([m.model_dump() for m in request.daily_metrics], indent=2)}\n"
    
    prompt = f"""
    Context Date Utilizator: {context}
    Ingrediente Disponibile: {request.ingredients}
    
    Propune o masă/dietă pentru astăzi în ROMÂNĂ bazată pe ingredientele pe care utilizatorul le are în casă și pe activitatea lui recentă.
    Dacă a avut un antrenament intens, propune ceva bogat în proteine și carbohidrați.
    Dacă metricile arată stres mare sau somn puțin, propune ceva nutritiv și ușor de digerat.
    
    Răspunde DOAR cu JSON:
    {{
      "meal_name": "Numele mesei",
      "recipe": "Instrucțiuni scurte de preparare",
      "nutritional_info": "Calorii și macronutrienți estimați",
      "ai_reasoning": "De ce este această masă potrivită pentru tine astăzi"
    }}
    """
    return await call_gemini(prompt, "nutri", user)

@app.post("/predict/fitness-summary")
async def get_fitness_summary(request: FitnessSummaryRequest):
    user = request.profile.username if request.profile else "Utilizator"
    context = f"Utilizator: {user}, Scop: {request.profile.fitnessGoal}, Vârstă: {request.profile.age}\n"
    context += f"Antrenamente recente: {json.dumps([w.model_dump() for w in request.recent_workouts], indent=2)}\n"
    context += f"Metricile de sănătate: {json.dumps([m.model_dump() for m in request.daily_metrics], indent=2)}\n"

    current_level_str = "Nu există date anterioare."
    if request.fitness_level:
        current_level_str = json.dumps(request.fitness_level, indent=2)

    prompt = f"""
    Context Date Noi Utilizator: {context}
    
    NIVEL FITNESS CURENT (DIN BAZA DE DATE):
    {current_level_str}

    SARCINĂ:
    Analizează noile antrenamente și metrici și actualizează Nivelul de Fitness.
    REGULI CRITICE:
    1. Folosește NIVELUL CURENT ca punct de plecare (baseline).
    2. Modificările trebuie să fie REALISTE și INCREMENTALE (VO2 Max nu crește cu 5 puncte după un antrenament).
    3. Dacă noile date arată progres, crește ușor valorile relevante. Dacă arată regres sau oboseală, scade-le.
    4. Dacă nu există date noi semnificative, păstrează valorile actuale.
    5. Calculează VO2 MAX, timpii pentru 5K/10K/Maraton și estimările de forță.

    Răspunde DOAR cu JSON valid:
    {{
      "vo2_max": 45.5,
      "fitness_level_score": 7,
      "fitness_category": "Intermediate",
      "estimated_5k_time": 25.5,
      "estimated_10k_time": 54.0,
      "estimated_marathon_time": 3.5,
      "pushup_estimate": 35,
      "pullup_estimate": 12,
      "bench_press_estimate": 100.0,
      "deadlift_estimate": 150.0,
      "body_battery": 72,
      "ai_insights": " text in romana despre evolutie...",
      "strength_weaknesses": "text in romana..."
    }}
    """
    result = await call_gemini(prompt, "fitness", user)

    # Ensure body_battery is present in response
    if "body_battery" not in result:
        result["body_battery"] = 72

    return result

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="127.0.0.1", port=8006)
