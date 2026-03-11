document.addEventListener('DOMContentLoaded', () => {
    
    const userStr = localStorage.getItem('user');
    if (!userStr) return;
    const user = JSON.parse(userStr);
    
    document.getElementById('display-username').innerText = user.username;

    // Setăm data implicită la momentul actual
    const now = new Date();
    now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
    document.getElementById('w-datetime').value = now.toISOString().slice(0, 16);

    loadWorkoutHistory();

    // Logout
    document.getElementById('btn-logout').addEventListener('click', () => {
        localStorage.removeItem('user');
        window.location.href = 'auth.html';
    });

    // Modal logic
    const modal = document.getElementById('workout-modal');
    document.getElementById('btn-show-workout').onclick = () => modal.style.display = "flex";
    document.getElementById('btn-close-workout').onclick = () => modal.style.display = "none";

    // Navigare Tab-uri (Sidebar)
    const tabBtns = document.querySelectorAll('.tab-btn');
    const tabContents = document.querySelectorAll('.tab-content');

    tabBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            tabContents.forEach(content => content.style.display = 'none');
            tabBtns.forEach(b => b.classList.remove('active'));
            
            const targetId = btn.getAttribute('data-target');
            document.getElementById(targetId).style.display = 'block';
            btn.classList.add('active');

            // Dacă am dat click pe tab-ul de PR, încărcăm PR-urile
            if (targetId === 'pr-section') {
                loadPersonalRecords();
            }
        });
    });

    async function loadPersonalRecords() {
        const prContainer = document.getElementById('pr-container');
        if (!prContainer) return;

        prContainer.innerHTML = '<p style="grid-column: span 2; text-align: center;">AI-ul scanează istoricul tău pentru recorduri...</p>';

        try {
            const res = await fetch(`http://localhost:8080/api/workouts/prs?userId=${user.userId}`);
            if (res.ok) {
                const data = await res.json();
                prContainer.innerHTML = '';

                if (data.records && data.records.length > 0) {
                    data.records.forEach(pr => {
                        prContainer.innerHTML += `
                            <div class="card" style="text-align: center; border: 1px solid #333; transition: transform 0.3s;">
                                <i class="fas fa-${pr.icon}" style="font-size: 2em; color: var(--verde-ai); margin-bottom: 15px;"></i>
                                <h3 style="margin: 10px 0; font-size: 1.1em;">${pr.exercise}</h3>
                                <div style="font-size: 1.5em; font-weight: bold; color: white;">${pr.value}</div>
                            </div>
                        `;
                    });
                } else {
                    prContainer.innerHTML = '<p style="grid-column: span 2; text-align: center; color: #666;">Încă nu am găsit recorduri. Adaugă detalii specifice în antrenamente (ex: Bench Press 100kg)!</p>';
                }
            }
        } catch (e) {
            console.error(e);
            prContainer.innerHTML = '<p>Eroare la încărcarea recordurilor.</p>';
        }
    }

    // Dinamica formularului
    const typeSelect = document.getElementById('w-type');
    const dynamicFields = document.getElementById('dynamic-fields');

    typeSelect.addEventListener('change', () => {
        const val = typeSelect.value;
        if (val === "Haltere") {
            dynamicFields.innerHTML = `
                <label>Exerciții și Seturi</label>
                <textarea id="w-details" class="custom-textarea" placeholder="Ex: Împins la piept: 3 seturi x 10 rep, Genoflexiuni..."></textarea>
            `;
        } else if (val === "CrossFit") {
            dynamicFields.innerHTML = `
                <label>Descriere WOD (Workout of the Day)</label>
                <textarea id="w-details" class="custom-textarea" placeholder="Ex: AMRAP 20 min, 10 Burpees, 20 Kettlebell Swings..."></textarea>
            `;
        } else {
            dynamicFields.innerHTML = `
                <label>Detalii suplimentare</label>
                <textarea id="w-details" class="custom-textarea" placeholder="Descrie cum a fost antrenamentul..."></textarea>
            `;
        }
    });

    // Salvare Antrenament
    document.getElementById('workout-form').onsubmit = async (e) => {
        e.preventDefault();
        
        const workoutData = {
            type: document.getElementById('w-type').value,
            duration: parseInt(document.getElementById('w-duration').value),
            intensity: document.getElementById('w-intensity').value,
            averageHeartRate: document.getElementById('w-pulse').value ? parseInt(document.getElementById('w-pulse').value) : null,
            date: document.getElementById('w-datetime').value,
            details: document.getElementById('w-details').value
        };

        try {
            const res = await fetch(`http://localhost:8080/api/workouts/add?userId=${user.userId}`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(workoutData)
            });
            if (res.ok) {
                modal.style.display = "none";
                document.getElementById('workout-form').reset();
                loadWorkoutHistory();
            }
        } catch (e) { alert('Eroare la salvare.'); }
    };

    async function loadWorkoutHistory() {
        const historyBody = document.getElementById('workout-history-body');
        try {
            const res = await fetch(`http://localhost:8080/api/workouts/history?userId=${user.userId}`);
            if (res.ok) {
                const workouts = await res.json();
                historyBody.innerHTML = '';
                workouts.forEach(w => {
                    const dateObj = new Date(w.date);
                    const dateStr = dateObj.toLocaleDateString('ro-RO', { day: '2-digit', month: '2-digit' });
                    const timeStr = dateObj.toLocaleTimeString('ro-RO', { hour: '2-digit', minute: '2-digit' });

                    historyBody.innerHTML += `
                        <tr>
                            <td>
                                <strong>${dateStr}</strong><br>
                                <small style="color:#666">${timeStr} (${w.dayPart})</small>
                            </td>
                            <td>
                                <span class="badge">${w.type}</span><br>
                                <small style="color:var(--verde-ai)">${w.intensity}</small>
                            </td>
                            <td style="font-size: 0.9em; max-width: 200px;">${w.details || '-'}</td>
                            <td>${w.duration} min</td>
                            <td>${w.averageHeartRate ? w.averageHeartRate + ' BPM' : '--'}</td>
                        </tr>
                    `;
                });
            }
        } catch (e) { console.error(e); }
    }

    // NUTRI-COACH AI
    const btnAnalyzeMeal = document.getElementById('btn-analyze-meal');
    const mealInput = document.getElementById('meal-input');
    const mealFeedback = document.getElementById('meal-feedback');
    const mealFeedbackText = document.getElementById('meal-feedback-text');

    btnAnalyzeMeal.onclick = async () => {
        const description = mealInput.value;
        if (!description) return alert("Descrie ce ai mâncat!");

        btnAnalyzeMeal.disabled = true;
        btnAnalyzeMeal.innerText = "Se analizează...";
        
        try {
            const res = await fetch(`http://localhost:8080/api/meals/analyze?userId=${user.userId}`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ description })
            });
            
            if (res.ok) {
                const data = await res.json();
                mealFeedback.style.display = "block";
                mealFeedbackText.innerHTML = `
                    <br><br>
                    <div style="display:grid; grid-template-columns: 1fr 1fr; gap:10px; margin-bottom:15px;">
                        <div class="metric-item" style="border-color: #ffaa00">Calorii: ${data.calories} kcal</div>
                        <div class="metric-item" style="border-color: #00ff88">Proteine: ${data.protein}g</div>
                        <div class="metric-item" style="border-color: #0088ff">Carbohidrați: ${data.carbs}g</div>
                        <div class="metric-item" style="border-color: #ff4444">Grăsimi: ${data.fats}g</div>
                    </div>
                    <p><i>${data.feedback}</i></p>
                `;
                mealInput.value = '';
            }
        } catch (e) {
            alert("Eroare la analiza nutrițională.");
        } finally {
            btnAnalyzeMeal.disabled = false;
            btnAnalyzeMeal.innerText = "Analizează Masa";
        }
    };

    // SMART RECOVERY
    const btnAnalyzeRecovery = document.getElementById('btn-analyze-recovery');
    const sorePartsInput = document.getElementById('sore-parts');
    const painLevelSelect = document.getElementById('pain-level');
    const recoveryFeedback = document.getElementById('recovery-feedback');
    const recoveryFeedbackText = document.getElementById('recovery-feedback-text');

    btnAnalyzeRecovery.onclick = async () => {
        const soreParts = sorePartsInput.value;
        const painLevel = painLevelSelect.value;
        if (!soreParts) return alert("Introdu zonele cu dureri!");

        btnAnalyzeRecovery.disabled = true;
        btnAnalyzeRecovery.innerText = "Se analizează protocolul...";

        try {
            const res = await fetch(`http://localhost:8080/api/recovery/analyze?userId=${user.userId}`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ soreParts, painLevel })
            });

            if (res.ok) {
                const data = await res.json();
                console.log("DEBUG Recovery Response:", data);
                
                const protocolText = data.protocol || data.feedback || "AI-ul nu a putut genera un text. Reîncearcă.";
                const timeText = data.estimated_recovery || data.recovery_time || "Indeterminată";

                recoveryFeedback.style.display = "block";
                recoveryFeedbackText.innerHTML = `
                    <p style="color:var(--accent); margin-bottom:10px;">
                        <i class="fas fa-clock"></i> Timp estimat recuperare: <strong>${timeText}</strong>
                    </p>
                    <p style="white-space: pre-line;">${protocolText}</p>
                `;
            }
        } catch (e) {
            alert("Eroare la analiza recuperării.");
        } finally {
            btnAnalyzeRecovery.disabled = false;
            btnAnalyzeRecovery.innerText = "Generează Protocol";
        }
    };

    // Predicție AI
    document.getElementById('btn-predictie').addEventListener('click', async () => {
        const summary = document.getElementById('ai-summary');
        const recommendation = document.getElementById('ai-recommendation');
        const btn = document.getElementById('btn-predictie');
        
        btn.innerText = "Analizăm...";
        btn.disabled = true;
        summary.innerText = "Scanăm activitatea ta recentă...";
        
        try {
            const res = await fetch(`http://localhost:8080/api/ai/prediction?userId=${user.userId}`);
            if (res.ok) {
                const data = await res.json();
                console.log("DEBUG AI Response:", data);

                // Verificăm dacă avem datele în formatul așteptat sau în rădăcina obiectului
                const s = data.summary || data.analysis || "Analiză finalizată.";
                const r = data.recommendation || data.advice || data.protocol || "Continuă cu planul actual!";
                const vo2 = data.estimated_vo2_max || data.vo2_max || "--";
                const bb = data.body_battery || data.energy || "--";

                summary.innerHTML = `<strong>Status:</strong> ${s}`;
                recommendation.innerHTML = `<i class="fas fa-lightbulb" style="color:var(--accent)"></i> ${r}`;
                
                document.getElementById('vo2-val').innerText = vo2;
                document.getElementById('bb-val').innerText = bb;
            } else {
                summary.innerText = "AI-ul nu a putut genera un răspuns acum.";
            }
        } catch (e) { 
            console.error(e);
            summary.innerText = "Eroare de conexiune cu serverul AI."; 
        } finally {
            btn.innerText = "Scanează";
            btn.disabled = false;
        }
    });
});
