import main
from fastapi.testclient import TestClient

client = TestClient(main.app)

def test_ai_summary_new_user_no_history(monkeypatch):
    """
    Verifies that the AI can handle a 'Cold Start' scenario for new users
    with no training or health metrics history.
    """
    async def fake_call_gemini(*args, **kwargs):
        return {
            "vo2_max": 42.0,
            "ai_insights": "Stable demo plan for new user, with introductory recommendations and gradual progress."
        }

    monkeypatch.setattr(main, "call_gemini", fake_call_gemini)

    payload = {
        "profile": {"username": "NewUser", "fitnessGoal": "Weight Loss", "age": 30},
        "recent_workouts": [],
        "daily_metrics": [],
        "fitness_level": None
    }
    
    response = client.post("/predict/fitness-summary", json=payload)
    assert response.status_code == 200
    data = response.json()
    
    # Ensure reasonable defaults are provided and no crashes occur
    assert "vo2_max" in data
    assert "ai_insights" in data
    assert "Stable demo plan" in data["ai_insights"]
