document.addEventListener('DOMContentLoaded', () => {
    console.log("--- ATHLETICA AI STARTUP ---");

    // --- 1. CONFIGURARE INIȚIALĂ ȘI SESIUNE ---
    const userStr = localStorage.getItem('user');
    if (!userStr) {
        console.warn("Utilizator nelogat, redirecționare...");
        window.location.href = 'auth.html';
        return;
    }

    const userRaw = JSON.parse(userStr);
    const user = {
        ...userRaw,
        id: userRaw.id || userRaw.userId,
        userId: userRaw.userId || userRaw.id
    };
    console.log("Sesiune activă pentru:", user.username, "ID:", user.id);

    const API_BASE = "http://127.0.0.1:8080/api";
    const AI_PR_API = "http://127.0.0.1:8006/analyze-pr-trend";

    const displayUsername = document.getElementById('display-username');
    if (displayUsername) displayUsername.innerText = user.username;

    // --- 2. NAVIGARE ȘI UI ---
    const tabBtns = document.querySelectorAll('.tab-btn');
    const tabContents = document.querySelectorAll('.tab-content');

    // Funcție pentru încărcarea exercițiilor din baza de date
    async function loadExerciseOptions() {
        const prSelect = document.getElementById('pr-exercise');
        if (!prSelect) return;

        try {
            const res = await fetch(`${API_BASE}/exercises`);
            if (res.ok) {
                const exercises = await res.json();
                if (exercises.length > 0) {
                    prSelect.innerHTML = ''; // Curățăm opțiunile statice
                    exercises.forEach(ex => {
                        const opt = document.createElement('option');
                        opt.value = ex.name;
                        opt.innerText = ex.displayName;
                        prSelect.appendChild(opt);
                    });
                    // Reîncărcăm analiza pentru primul exercițiu din listă
                    loadPRAnalytics(prSelect.value);
                }
            }
        } catch (e) {
            console.error("Eroare încărcare exerciții:", e);
        }
    }

    loadExerciseOptions();

    tabBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            const targetId = btn.getAttribute('data-target');
            console.log("Schimbare tab către:", targetId);

            tabContents.forEach(content => content.style.display = 'none');
            tabBtns.forEach(b => b.classList.remove('active'));

            const targetContent = document.getElementById(targetId);
            if (targetContent) {
                targetContent.style.display = 'block';
                btn.classList.add('active');
            }

            // Încărcare automată analiză dacă intrăm pe secțiunile relevante
            if (targetId === 'pr-section') {
                const prSelect = document.getElementById('pr-exercise');
                if (prSelect) loadPRAnalytics(prSelect.value);
            } else if (targetId === 'health-section') {
                loadHealthMetrics();
            }
        });
    });

    // Logout [cite: 35]
    const btnLogout = document.getElementById('btn-logout');
    if (btnLogout) {
        btnLogout.onclick = () => {
            localStorage.clear();
            window.location.href = 'auth.html';
        };
    }

    // Modal Antrenament
    const modalWorkout = document.getElementById('workout-modal');
    const btnShowWorkout = document.getElementById('btn-show-workout');
    if (btnShowWorkout) {
        btnShowWorkout.onclick = () => {
            modalWorkout.style.display = 'flex';
        };
    }

    // --- 3. ISTORIC ANTRENAMENTE ---
    async function loadWorkoutHistory() {
        const historyBody = document.getElementById('workout-history-body');
        if (!historyBody) return;

        try {
            const res = await fetch(`${API_BASE}/workouts/history?userId=${user.id}`);
            if (res.ok) {
                const workouts = await res.json();
                if (workouts.length === 0) {
                    historyBody.innerHTML = '<tr><td colspan="5" style="text-align:center;">Niciun antrenament găsit.</td></tr>';
                    return;
                }
                historyBody.innerHTML = workouts.map(w => `
                    <tr>
                        <td><strong>${new Date(w.date).toLocaleDateString('ro-RO')}</strong></td>
                        <td><span class="badge">${w.type}</span></td>
                        <td>${w.details || '-'}</td>
                        <td>${w.duration} min</td>
                        <td>${w.averageHeartRate || '--'} BPM</td>
                    </tr>
                `).join('');
            }
        } catch (e) { console.error("Eroare istoric:", e); }
    }
    loadWorkoutHistory();

    // --- 4. SALVARE ANTRENAMENT [cite: 37, 38] ---
    const workoutForm = document.getElementById('workout-form');
    if (workoutForm) {
        workoutForm.onsubmit = async (e) => {
            e.preventDefault();
            const btn = workoutForm.querySelector('.btn-save');
            btn.classList.add('btn-loading');

            const workoutData = {
                type: document.getElementById('w-type').value,
                duration: parseInt(document.getElementById('w-duration').value),
                averageHeartRate: parseInt(document.getElementById('w-avg-hr').value) || null,
                intensity: document.getElementById('w-intensity').value,
                details: document.getElementById('w-details').value,
                date: new Date().toISOString()
            };

            try {
                const res = await fetch(`${API_BASE}/workouts/add?userId=${user.id}`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(workoutData)
                });
                if (res.ok) {
                    modalWorkout.style.display = "none";
                    workoutForm.reset();
                    loadWorkoutHistory();
                }
            } catch (error) { alert("Eroare de conexiune la salvare."); }
            finally { btn.classList.remove('btn-loading'); }
        };
    }

    // --- 5. PERSONAL RECORDS (high-contrast PRs) ---
    const btnSavePR = document.getElementById('btn-save-pr');
    let prChartInstance = null;

    if (btnSavePR) {
        btnSavePR.onclick = async () => {
            const exercise = document.getElementById('pr-exercise').value;
            const weight = document.getElementById('pr-weight').value;
            const reps = document.getElementById('pr-reps').value;

            if (!weight || !reps) {
                alert("Introdu greutatea și repetările!");
                return;
            }

            btnSavePR.classList.add('btn-loading');

            const prData = {
                exerciseName: exercise,
                weight: parseFloat(weight),
                reps: parseInt(reps),
                userId: user.id.toString()
            };

            try {
                const res = await fetch(`${API_BASE}/pr/add`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(prData)
                });
                if (res.ok) {
                    alert("PR salvat cu succes! 🎉");
                    document.getElementById('pr-weight').value = '';
                    document.getElementById('pr-reps').value = '';
                    loadPRAnalytics(exercise);
                }
            } catch (e) { console.error("Eroare salvare PR:", e); }
            finally { btnSavePR.classList.remove('btn-loading'); }
        };
    }

    async function loadPRAnalytics(exercise) {
        try {
            // Preluare istoric din MySQL via Java (8080)
            const historyRes = await fetch(`${API_BASE}/pr/${user.id}/${exercise}`);
            const history = await historyRes.json();

            // Populare istoric tabel PR
            const prHistoryBody = document.getElementById('pr-history-body');
            const prHistoryTitle = document.getElementById('pr-history-title');
            if (prHistoryTitle) prHistoryTitle.innerText = exercise;
            
            if (prHistoryBody) {
                if (history.length === 0) {
                    prHistoryBody.innerHTML = '<tr><td colspan="3" style="text-align:center;">Niciun record găsit.</td></tr>';
                } else {
                    prHistoryBody.innerHTML = history.map(h => `
                        <tr>
                            <td>${new Date(h.date).toLocaleDateString('ro-RO')}</td>
                            <td><strong>${h.weight} kg</strong></td>
                            <td>${h.reps}</td>
                        </tr>
                    `).join('');
                }
            }

            if (history.length === 0) {
                document.getElementById('display-1rm').innerText = "-- kg";
                document.getElementById('display-next').innerText = "-- kg";
                if (prChartInstance) prChartInstance.destroy();
                return;
            }

            // Analiză AI via Python (8006)
            const aiRes = await fetch(AI_PR_API, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(history)
            });

            if (aiRes.ok) {
                const aiData = await aiRes.json();
                document.getElementById('display-1rm').innerText = aiData.one_rm + " kg";
                document.getElementById('display-next').innerText = aiData.next_prediction + " kg";
                renderPRChart(history, aiData.trend);
            }
        } catch (e) { console.error("Eroare comunicare AI PR (8006):", e); }
    }

    function renderPRChart(history, trendData) {
        const canvas = document.getElementById('pr-chart');
        if (!canvas) return;
        const ctx = canvas.getContext('2d');
        if (prChartInstance) prChartInstance.destroy();

        prChartInstance = new Chart(ctx, {
            type: 'line',
            data: {
                labels: history.map(h => new Date(h.date).toLocaleDateString()),
                datasets: [
                    { label: 'Real (kg)', data: history.map(h => h.weight), borderColor: '#ffffff', tension: 0.1, backgroundColor: 'rgba(255,255,255,0.1)', fill: true },
                    { label: 'AI Trend', data: trendData, borderColor: '#888888', borderDash: [5, 5], fill: false }
                ]
            },
            options: { 
                responsive: true, 
                plugins: { legend: { labels: { color: '#fff' } } },
                scales: {
                    x: { ticks: { color: '#888' }, grid: { color: '#222' } },
                    y: { ticks: { color: '#888' }, grid: { color: '#222' } }
                }
            }
        });
    }

    // Actualizare grafic la schimbarea exercițiului
    const prSelect = document.getElementById('pr-exercise');
    if (prSelect) {
        prSelect.onchange = () => loadPRAnalytics(prSelect.value);
    }

    // --- 6. DAILY METRICS (HEALTH) ---
    const btnSaveHealth = document.getElementById('btn-save-health');
    let healthChartInstance = null;

    if (btnSaveHealth) {
        btnSaveHealth.onclick = async () => {
            const hrv = document.getElementById('h-hrv').value;
            const sleep = document.getElementById('h-sleep').value;
            const stress = document.getElementById('h-stress').value;
            const rhr = document.getElementById('h-rhr').value;

            if (!hrv || !sleep || !stress || !rhr) {
                alert("Te rugăm să completezi toate câmpurile!");
                return;
            }

            btnSaveHealth.classList.add('btn-loading');

            const metricsData = {
                hrv: parseInt(hrv),
                sleepHours: parseFloat(sleep),
                stressLevel: parseInt(stress),
                restingHeartRate: parseInt(rhr),
                date: new Date().toISOString().split('T')[0] // Doar data YYYY-MM-DD
            };

            try {
                const res = await fetch(`${API_BASE}/metrics/add/${user.id}`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(metricsData)
                });
                if (res.ok) {
                    alert("Metrici salvate cu succes! ❤️");
                    loadHealthMetrics();
                }
            } catch (e) { console.error("Eroare salvare metrici:", e); }
            finally { btnSaveHealth.classList.remove('btn-loading'); }
        };
    }

    async function loadHealthMetrics() {
        const historyBody = document.getElementById('health-history-body');
        if (!historyBody) return;

        try {
            const res = await fetch(`${API_BASE}/metrics/user/${user.id}`);
            if (res.ok) {
                const metrics = await res.json();
                if (metrics.length === 0) {
                    historyBody.innerHTML = '<tr><td colspan="5" style="text-align:center;">Nicio metrică găsită.</td></tr>';
                    return;
                }
                historyBody.innerHTML = metrics.map(m => `
                    <tr>
                        <td><strong>${new Date(m.date).toLocaleDateString('ro-RO')}</strong></td>
                        <td>${m.hrv} ms</td>
                        <td>${m.sleepHours} h</td>
                        <td>${m.stressLevel}/100</td>
                        <td>${m.restingHeartRate} BPM</td>
                    </tr>
                `).join('');

                renderHealthChart(metrics);
            }
        } catch (e) { console.error("Eroare încărcare metrici:", e); }
    }

    function renderHealthChart(metrics) {
        const canvas = document.getElementById('health-chart');
        if (!canvas) return;
        const ctx = canvas.getContext('2d');
        if (healthChartInstance) healthChartInstance.destroy();

        // Sortăm metricile cronologic pentru grafic (backend le dă DESC probabil)
        const sortedMetrics = [...metrics].reverse();

        healthChartInstance = new Chart(ctx, {
            type: 'line',
            data: {
                labels: sortedMetrics.map(m => new Date(m.date).toLocaleDateString('ro-RO')),
                datasets: [
                    { label: 'HRV (ms)', data: sortedMetrics.map(m => m.hrv), borderColor: '#2ecc71', tension: 0.1 },
                    { label: 'Somn (ore)', data: sortedMetrics.map(m => m.sleepHours), borderColor: '#3498db', tension: 0.1 },
                    { label: 'Stres', data: sortedMetrics.map(m => m.stressLevel), borderColor: '#e74c3c', tension: 0.1 }
                ]
            },
            options: { 
                responsive: true, 
                plugins: { 
                    legend: { labels: { color: '#fff' } } 
                },
                scales: {
                    x: { ticks: { color: '#888' }, grid: { color: '#222' } },
                    y: { ticks: { color: '#888' }, grid: { color: '#222' } }
                }
            }
        });
    }

    // --- 7. ALTE FUNCȚII AI ---
    const btnPredict = document.getElementById('btn-predictie');
    if (btnPredict) {
        btnPredict.onclick = async () => {
            const summary = document.getElementById('ai-summary');
            btnPredict.classList.add('btn-loading');
            try {
                // Endpoint corectat pentru port 8080 care apelează intern AI-ul pe 8006
                const res = await fetch(`${API_BASE}/ai/prediction?userId=${user.id}`);
                if (res.ok) {
                    const data = await res.json();
                    summary.innerText = data.summary || "Analiză completă.";
                    document.getElementById('ai-recommendation').innerText = data.recommendation || "";
                }
            } catch (e) { summary.innerText = "Eroare AI."; }
            finally { btnPredict.classList.remove('btn-loading'); }
        };
    }

    // Nutriție AI
    const btnAnalyzeMeal = document.getElementById('btn-analyze-meal');
    if (btnAnalyzeMeal) {
        btnAnalyzeMeal.onclick = async () => {
            const desc = document.getElementById('meal-input').value;
            const feedbackBox = document.getElementById('meal-feedback');
            btnAnalyzeMeal.classList.add('btn-loading');
            try {
                const res = await fetch(`${API_BASE}/meals/analyze?userId=${user.id}`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ description: desc })
                });
                if (res.ok) {
                    const data = await res.json();
                    feedbackBox.style.display = "block";
                    feedbackBox.innerText = `Calorii: ${data.calories} | Feedback: ${data.feedback}`;
                }
            } catch (e) { console.error(e); }
            finally { btnAnalyzeMeal.classList.remove('btn-loading'); }
        };
    }

    // Recuperare AI
    const btnAnalyzeRec = document.getElementById('btn-analyze-recovery');
    if (btnAnalyzeRec) {
        btnAnalyzeRec.onclick = async () => {
            const sore = document.getElementById('sore-parts').value;
            const feedbackBox = document.getElementById('recovery-feedback');
            btnAnalyzeRec.classList.add('btn-loading');
            try {
                const res = await fetch(`${API_BASE}/recovery/analyze?userId=${user.id}`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ soreParts: sore })
                });
                if (res.ok) {
                    const data = await res.json();
                    feedbackBox.style.display = "block";
                    feedbackBox.innerText = data.protocol || data.feedback;
                }
            } catch (e) { console.error(e); }
            finally { btnAnalyzeRec.classList.remove('btn-loading'); }
        };
    }
});