import pytest
from main import app
from fastapi.testclient import TestClient

client = TestClient(app)

def test_ai_summary_new_user_no_history():
    """
    Test profesional: Verifică dacă AI-ul poate gestiona un utilizator nou
    care nu are încă istoric de antrenamente (Cold Start).
    """
    payload = {
        "profile": {"username": "NewUser", "fitnessGoal": "Weight Loss", "age": 30},
        "recent_workouts": [],  # ISTORIC GOL
        "daily_metrics": [],    # METRICI GOALE
        "fitness_level": None   # NIVEL INITIAL LIPSĂ
    }
    
    response = client.post("/predict/fitness-summary", json=payload)
    assert response.status_code == 200
    data = response.json()
    
    # Verificăm că AI-ul oferă valori default rezonabile și nu crapă
    assert "vo2_max" in data
    assert "ai_insights" in data
    assert len(data["ai_insights"]) > 10
    print(f"\n[TEST PASSED] AI-ul a generat recomandări pentru un cont nou: {data['ai_insights'][:50]}...")
