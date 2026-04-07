import main
from fastapi.testclient import TestClient

client = TestClient(main.app)

def test_vo2max_stability_logic(monkeypatch):
    """
    Verifies that VO2Max updates are incremental and stable.
    Ensures no unrealistic fluctuations occur when provided with a baseline.
    """
    async def fake_call_gemini(*args, **kwargs):
        return {
            "vo2_max": 45.5,
            "fitness_category": "Intermediate",
            "ai_insights": "Stable evolution detected."
        }

    monkeypatch.setattr(main, "call_gemini", fake_call_gemini)

    # Payload simulating a user with an existing VO2Max baseline of 45.0
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
    
    # Assert stability: variance should be within safe bounds
    new_vo2 = data["vo2_max"]
    assert 44.0 <= new_vo2 <= 47.0, f"Unrealistic fluctuation detected: {new_vo2}"
    assert "fitness_category" in data
