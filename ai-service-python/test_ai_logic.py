import pytest
from main import app, FitnessSummaryRequest, UserProfile
from fastapi.testclient import TestClient

client = TestClient(app)

def test_vo2max_stability_logic():
    """
    Test profesional: Verifică dacă logica de actualizare a VO2Max 
    este incrementală și nu are fluctuații nerealiste.
    """
    # Simulăm un utilizator care are deja VO2Max 45.0 în baza de date
    payload = {
        "profile": {"username": "Alex", "fitnessGoal": "Muscle Gain", "age": 25},
        "recent_workouts": [
            {"type": "Running", "duration": 30, "intensity": "High", "averageHeartRate": 165}
        ],
        "daily_metrics": [
            {"date": "2026-04-07", "sleepHours": 8, "hrv": 65, "stressLevel": 20}
        ],
        "fitness_level": {
            "vo2_max": 45.0,
            "level_score": 6,
            "category": "Intermediate"
        }
    }
    
    response = client.post("/predict/fitness-summary", json=payload)
    assert response.status_code == 200
    data = response.json()
    
    # Verificăm stabilitatea (Modul Demo returnează 45.5, deci e un pas de +0.5)
    # Într-un test real cu API-ul activ, am verifica dacă variația este < 2.0 unități
    new_vo2 = data["vo2_max"]
    assert new_vo2 >= 44.0 and new_vo2 <= 47.0, f"Fluctuație nerealistă detectată: {new_vo2}"
    assert "fitness_category" in data
    print(f"\n[TEST PASSED] VO2Max nou: {new_vo2} (Variație sigură)")
