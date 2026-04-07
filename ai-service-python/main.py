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

# Fix for Romanian characters in Windows console (disabled during tests)
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
        print("Google GenAI client initialized.")
    except Exception as e:
        print(f"[ERR] Gemini initialization failed: {str(e)}")

app = FastAPI()

# Enable CORS for frontend communication
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

class UserProfile(BaseModel):
    username: Optional[str] = "User"
    fitnessGoal: Optional[str] = "Maintenance"
    age: Optional[int] = 25
    gender: Optional[str] = "Unspecified"

class WorkoutLog(BaseModel):
    type: Optional[str] = "Workout"
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
    personal_records: Optional[List[dict]] = []
    fitness_level: Optional[dict] = None

def get_demo_response(type_key, user="User"):
    """Provides fallback static data when the AI service is unavailable."""
    if "nutri" in type_key:
        return {"calories": 450, "protein": 30, "carbs": 50, "fats": 12, "feedback": f"Demo Mode: {user}'s meal seems balanced."}
    if "recovery" in type_key:
        return {"protocol": f"Demo Mode: Recommendation for {user}: rest and hydration.", "estimated_recovery": "24 hours"}
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
        "ai_insights": f"Hello {user}! Your demo analysis is ready. You are in good shape. Keep training!",
        "strength_weaknesses": "Strengths: cardio. Weaknesses: upper body strength.",
        "summary": "Your fitness analysis (Demo Mode) is complete."
    }

async def call_gemini(prompt, type_key, user="User"):
    """Orchestrates calls to multiple Gemini models with fallback and retry logic."""
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
            print(f"--- Attempting model: {model_name} ---")
            response = client.models.generate_content(
                model=model_name,
                contents=prompt,
                config={
                    'system_instruction': f"You are {user}'s personal trainer. Speak only in ROMANIAN. Respond ONLY with valid JSON."
                }
            )
            
            if response and response.text:
                text = response.text
                match = re.search(r'\{.*\}', text, re.DOTALL)
                if match:
                    res = json.loads(match.group(0))
                    print(f"[OK] Success with model: {model_name}")
                    return res
                    
        except Exception as e:
            error_msg = str(e)
            print(f"[ERR] {model_name} failed: {error_msg[:100]}...")
            if "429" in error_msg or "RESOURCE_EXHAUSTED" in error_msg:
                time.sleep(1)
                continue
            if "404" in error_msg:
                continue
 
    print("[FINAL] All models failed. Sending Demo data.")
    return get_demo_response(type_key, user)

@app.post("/predict/daily-advice")
async def get_daily_advice(request: PredictionRequest):
    """Generates personalized daily training and recovery advice."""
    user = request.profile.username if request.profile else "User"
    context = f"User: {user}, Goal: {request.profile.fitnessGoal}, Age: {request.profile.age}\n"
    context += f"Recent workouts: {json.dumps([w.model_dump() for w in request.recent_workouts], indent=2)}\n"
    context += f"Daily metrics: {json.dumps([m.model_dump() for m in request.daily_metrics], indent=2)}\n"
    
    prompt = f"""
    Context Data: {context}
    Analyze the data and provide a detailed report in ROMANIAN.
    Respond ONLY with JSON:
    {{
      "summary": "long text here",
      "recommendation": "short advice",
      "estimated_vo2_max": 45.0,
      "body_battery": 85
    }}
    """
    return await call_gemini(prompt, "advice", user)

@app.post("/predict/meal-analysis")
async def analyze_meal(request: MealAnalysisRequest):
    """Analyzes meal descriptions for nutritional content."""
    user = request.profile.username if request.profile else "User"
    prompt = f"Analyze meal: {request.meal_description}. Respond ONLY with JSON: {{'calories': number, 'protein': number, 'carbs': number, 'fats': number, 'feedback': 'text'}}"
    return await call_gemini(prompt, "nutri", user)

@app.post("/predict/recovery-chat")
async def recovery_chat(request: RecoveryRequest):
    """Handles conversational recovery guidance based on recent physical strain."""
    user = request.profile.username if request.profile else "User"
    context = f"User: {user}, Goal: {request.profile.fitnessGoal}\n"
    context += f"Recent workouts: {json.dumps([w.model_dump() for w in request.recent_workouts], indent=2)}\n"
    context += f"Daily metrics: {json.dumps([m.model_dump() for m in request.daily_metrics], indent=2)}\n"

    history_str = "\n".join([f"{m['role']}: {m['content']}" for m in request.chat_history])

    prompt = f"""
    Physical Context: {context}
    Chat History: {history_str}
    Last Message: {request.user_message}

    You are an expert recovery assistant. 
    If the user mentions pain, ask for details (intensity, onset, type) before providing a protocol.
    Provide a clear protocol (stretching, icing, rest) when sufficient data is available.

    Respond ONLY with JSON:
    {{
      "message": "your response here",
      "is_final_protocol": true/false
    }}
    """
    return await call_gemini(prompt, "recovery", user)

@app.post("/analyze-pr-trend")
async def analyze_pr_trend(data: list = Body(...)):
    """Analyzes strength progress and predicts 1RM trends using polynomial regression."""
    if not data: return {"error": "Missing data"}
    
    weights = [float(d.get('weight', 0)) for d in data]
    reps = [int(d.get('reps', 1)) for d in data]
    
    # 1RM Calculation using Epley formula
    last_weight = weights[-1]
    last_reps = reps[-1]
    one_rm = round(last_weight * (1 + 0.0333 * last_reps), 2) if last_reps > 1 else last_weight
    
    if len(weights) < 2:
        return {
            "trend": [last_weight],
            "one_rm": one_rm,
            "next_prediction": last_weight
        }
        
    x = np.arange(len(weights))
    # Use degree 2 polynomial fit for 3+ points, otherwise linear
    degree = 2 if len(weights) >= 3 else 1
    model = np.poly1d(np.polyfit(x, weights, degree))
    
    return {
        "trend": [round(float(p), 2) for p in model(x)],
        "one_rm": one_rm,
        "next_prediction": round(float(model(len(weights))), 2)
    }

@app.post("/predict/workout-proposal")
async def get_workout_proposal(request: WorkoutProposalRequest):
    """Generates a customized workout plan based on user request and health status."""
    user = request.profile.username if request.profile else "User"
    context = f"User: {user}, Goal: {request.profile.fitnessGoal}, Age: {request.profile.age}\n"
    context += f"Recent workouts: {json.dumps([w.model_dump() for w in request.recent_workouts], indent=2)}\n"
    context += f"Recent health metrics: {json.dumps([m.model_dump() for m in request.daily_metrics], indent=2)}\n"
    
    prompt = f"""
    Context Data: {context}
    User Request: {request.user_input}
    
    Propose a personalized workout in ROMANIAN. 
    Adjust intensity based on health metrics (low HRV/sleep should lead to lighter proposals).
    
    Respond ONLY with JSON:
    {{
      "workout_name": "Workout Name",
      "exercises": "List of exercises, sets, reps as text",
      "ai_notes": "Rationale based on health data"
    }}
    """
    return await call_gemini(prompt, "workout", user)

@app.post("/predict/meal-proposal")
async def get_meal_proposal(request: MealProposalRequest):
    """Generates meal recommendations based on available ingredients and recent activity."""
    user = request.profile.username if request.profile else "User"
    context = f"User: {user}, Goal: {request.profile.fitnessGoal}, Age: {request.profile.age}\n"
    context += f"Recent workouts: {json.dumps([w.model_dump() for w in request.recent_workouts], indent=2)}\n"
    context += f"Recent health metrics: {json.dumps([m.model_dump() for m in request.daily_metrics], indent=2)}\n"
    
    prompt = f"""
    Context Data: {context}
    Available Ingredients: {request.ingredients}
    
    Propose a meal/diet for today in ROMANIAN.
    Ensure protein/carbs match recent training intensity.
    
    Respond ONLY with JSON:
    {{
      "meal_name": "Meal Name",
      "recipe": "Short instructions",
      "nutritional_info": "Estimated calories and macros",
      "ai_reasoning": "Rationale for this choice"
    }}
    """
    return await call_gemini(prompt, "nutri", user)

@app.post("/predict/fitness-summary")
async def get_fitness_summary(request: FitnessSummaryRequest):
    """Calculates comprehensive fitness levels, including VO2Max and strength estimates."""
    user = request.profile.username if request.profile else "User"
    context = f"User: {user}, Goal: {request.profile.fitnessGoal}, Age: {request.profile.age}\n"
    context += f"Recent workouts: {json.dumps([w.model_dump() for w in request.recent_workouts], indent=2)}\n"
    context += f"Health metrics: {json.dumps([m.model_dump() for m in request.daily_metrics], indent=2)}\n"
    
    if request.personal_records:
        context += f"Personal Records (PRs): {json.dumps(request.personal_records, indent=2)}\n"

    current_level_str = "No prior data."
    if request.fitness_level:
        current_level_str = json.dumps(request.fitness_level, indent=2)

    prompt = f"""
    New Context: {context}
    CURRENT FITNESS LEVEL (BASELINE): {current_level_str}

    TASK: Update fitness level based on new data.
    CRITICAL RULES:
    1. Use baseline for realistic, incremental updates.
    2. Force update strength estimates if new PRs are present.
    3. Calculate VO2 MAX, race time predictions (5K/10K/Marathon), and strength metrics.

    Respond ONLY with valid JSON:
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
      "ai_insights": "insights in romanian...",
      "strength_weaknesses": "text in romanian..."
    }}
    """
    result = await call_gemini(prompt, "fitness", user)

    if "body_battery" not in result:
        result["body_battery"] = 72

    return result

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="127.0.0.1", port=8006)
