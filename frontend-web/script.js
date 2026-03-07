document.addEventListener('DOMContentLoaded', () => {
    
    const userStr = localStorage.getItem('user');
    if (!userStr) return;
    const user = JSON.parse(userStr);
    
    const displayUser = document.getElementById('display-username');
    if (displayUser) displayUser.innerText = user.username;

    // Logout
    document.getElementById('btn-logout').addEventListener('click', () => {
        localStorage.removeItem('user');
        window.location.href = 'auth.html';
    });

    // Modal Antrenament
    const modal = document.getElementById('workout-modal');
    const btnShow = document.getElementById('btn-show-workout');
    const btnClose = document.getElementById('btn-close-workout');
    const workoutForm = document.getElementById('workout-form');

    btnShow.onclick = () => modal.style.display = "flex";
    btnClose.onclick = () => modal.style.display = "none";

    workoutForm.onsubmit = async (e) => {
        e.preventDefault();
        const workoutData = {
            type: document.getElementById('w-type').value,
            duration: parseInt(document.getElementById('w-duration').value),
            intensity: document.getElementById('w-intensity').value
        };

        try {
            const res = await fetch(`http://localhost:8080/api/workouts/add?userId=${user.userId}`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(workoutData)
            });
            if (res.ok) {
                alert('Antrenament salvat!');
                modal.style.display = "none";
                workoutForm.reset();
            }
        } catch (e) { alert('Eroare la salvare.'); }
    };

    // Predicție AI
    document.getElementById('btn-predictie').addEventListener('click', async () => {
        const summary = document.getElementById('ai-summary');
        const recommendation = document.getElementById('ai-recommendation');
        
        summary.innerText = "Analizăm istoricul antrenamentelor...";
        recommendation.innerText = "";

        try {
            const res = await fetch(`http://localhost:8080/api/ai/prediction?userId=${user.userId}`);
            if (res.ok) {
                const data = await res.json();
                summary.innerText = data.summary;
                recommendation.innerText = data.recommendation;
                document.getElementById('vo2-val').innerText = data.estimated_vo2_max;
                document.getElementById('bb-val').innerText = data.body_battery;
            } else {
                summary.innerText = "Eroare la obținerea predicției.";
            }
        } catch (e) {
            summary.innerText = "Verifică dacă serverul Python (main.py) este pornit.";
        }
    });
});
