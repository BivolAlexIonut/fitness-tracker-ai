from fastapi.testclient import TestClient
from main import app

client = TestClient(app)

def test_pr_trend_analysis():
    # Simulăm date de antrenament pentru un exercițiu
    test_data = [
        {"weight": 100, "reps": 5},
        {"weight": 105, "reps": 5},
        {"weight": 110, "reps": 3}
    ]
    
    response = client.post("/analyze-pr-trend", json=test_data)
    
    assert response.status_code == 200
    data = response.json()
    
    # Verificăm dacă avem cheile necesare în răspuns
    assert "one_rm" in data
    assert "trend" in data
    assert "next_prediction" in data
    
    # Verificăm logica de calcul (110kg x 3 reps -> 1RM ~ 121kg)
    assert data["one_rm"] > 110
    print(f"Test succes! 1RM calculat: {data['one_rm']}")

def test_empty_data():
    response = client.post("/analyze-pr-trend", json=[])
    assert response.status_code == 200
    assert "error" in response.json()
