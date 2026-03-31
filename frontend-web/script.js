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
    // Normalizăm ID-ul: asigurăm că avem și .id și .userId cu aceeași valoare
    const user = { 
        ...userRaw, 
        id: userRaw.id || userRaw.userId, 
        userId: userRaw.userId || userRaw.id 
    };
    console.log("Sesiune activă pentru:", user.username, "ID:", user.id);

    const API_BASE = "http://127.0.0.1:8080/api";
    
    const displayUsername = document.getElementById('display-username');
    if (displayUsername) displayUsername.innerText = user.username;

    // --- 2. NAVIGARE ȘI UI ---
    const tabBtns = document.querySelectorAll('.tab-btn');
    const tabContents = document.querySelectorAll('.tab-content');

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
        });
    });

    // Logout
    const btnLogout = document.getElementById('btn-logout');
    if (btnLogout) {
        btnLogout.onclick = () => {
            console.log("Logout...");
            localStorage.removeItem('user');
            window.location.href = 'auth.html';
        };
    }

    // Modal Antrenament
    const modalWorkout = document.getElementById('workout-modal');
    const btnShowWorkout = document.getElementById('btn-show-workout');
    if (btnShowWorkout) {
        btnShowWorkout.onclick = () => {
            modalWorkout.style.display = 'flex';
            // Setăm data actuală implicită
            const dateInput = document.getElementById('w-datetime');
            if (dateInput) {
                const now = new Date();
                now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
                dateInput.value = now.toISOString().slice(0, 16);
            }
        };
    }

    // --- 3. ISTORIC ANTRENAMENTE (Funcție centrală) ---
    async function loadWorkoutHistory() {
        console.log("Încărcare istoric pentru utilizatorul:", user.id);
        const historyBody = document.getElementById('workout-history-body');
        if (!historyBody) return;

        try {
            const res = await fetch(`${API_BASE}/workouts/history?userId=${user.id}`);
            if (res.ok) {
                const workouts = await res.json();
                console.log("Antrenamente primite:", workouts.length);
                
                if (workouts.length === 0) {
                    historyBody.innerHTML = '<tr><td colspan="5" style="text-align:center; padding:30px; color:var(--text-muted);">Niciun antrenament găsit. Adaugă unul folosind butonul din stânga!</td></tr>';
                    return;
                }

                historyBody.innerHTML = workouts.map(w => {
                    const dateObj = new Date(w.date);
                    const dateStr = dateObj.toLocaleDateString('ro-RO');
                    const timeStr = dateObj.toLocaleTimeString('ro-RO', {hour: '2-digit', minute:'2-digit'});
                    
                    return `
                    <tr>
                        <td><strong>${dateStr}</strong><br><small style="color:var(--text-muted)">${timeStr}</small></td>
                        <td><span class="badge">${w.type}</span><br><small style="color:var(--accent-neon)">${w.intensity || 'Moderat'}</small></td>
                        <td style="font-size: 0.9em; max-width: 200px;">${w.details || '-'}</td>
                        <td>${w.duration} min</td>
                        <td>${w.averageHeartRate ? w.averageHeartRate + ' BPM' : '--'}</td>
                    </tr>
                    `;
                }).join('');
            } else {
                console.error("Eroare server istoric:", res.status);
            }
        } catch (e) { 
            console.error("Eroare rețea istoric:", e); 
        }
    }

    // Încărcăm istoricul imediat
    loadWorkoutHistory();

    // --- 4. SALVARE ANTRENAMENT ---
    const workoutForm = document.getElementById('workout-form');
    if (workoutForm) {
        workoutForm.onsubmit = async (e) => {
            e.preventDefault();
            console.log("Salvare antrenament detaliat...");
            
            const workoutData = {
                type: document.getElementById('w-type').value,
                duration: parseInt(document.getElementById('w-duration').value),
                averageHeartRate: parseInt(document.getElementById('w-avg-hr').value) || null,
                maxHeartRate: parseInt(document.getElementById('w-max-hr').value) || null,
                caloriesBurned: parseInt(document.getElementById('w-calories').value) || null,
                distance: parseFloat(document.getElementById('w-distance').value) || null,
                intensity: document.getElementById('w-intensity').value,
                details: document.getElementById('w-details').value,
                date: new Date().toISOString()
            };

            const btnSubmit = e.target.querySelector('button[type="submit"]');
            btnSubmit.innerText = "Se salvează datele...";
            btnSubmit.disabled = true;

            try {
                const res = await fetch(`${API_BASE}/workouts/add?userId=${user.id}`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(workoutData)
                });
                
                if (res.ok) {
                    console.log("Salvat cu succes!");
                    modalWorkout.style.display = "none";
                    workoutForm.reset();
                    loadWorkoutHistory();
                } else {
                    alert("Eroare la salvare: " + await res.text());
                }
            } catch (error) {
                console.error("Eroare salvare:", error);
                alert("Eroare de conexiune.");
            } finally {
                btnSubmit.innerText = "Salvează Antrenamentul";
                btnSubmit.disabled = false;
            }
        };
    }

    // --- 5. PREDICȚIE ZILNICĂ (DASHBOARD) ---
    const btnPredict = document.getElementById('btn-predictie');
    if (btnPredict) {
        btnPredict.onclick = async () => {
            console.log("Buton scanare apăsat pentru utilizator:", user.id);
            const summary = document.getElementById('ai-summary');
            const recommendation = document.getElementById('ai-recommendation');
            
            if (!summary) return;

            summary.innerText = "Analizăm datele tale biometrice...";
            recommendation.innerText = "";
            btnPredict.disabled = true;

            try {
                const res = await fetch(`${API_BASE}/ai/prediction?userId=${user.id}`);
                if (res.ok) {
                    const data = await res.json();
                    console.log("Răspuns AI primit:", data);
                    
                    // Verificăm dacă summary este obiect sau string
                    let summaryText = typeof data.summary === 'object' ? JSON.stringify(data.summary) : (data.summary || "Analiză completă.");
                    summary.innerText = summaryText;
                    
                    let recText = typeof data.recommendation === 'object' ? JSON.stringify(data.recommendation) : (data.recommendation || "Continuă progresul!");
                    recommendation.innerHTML = `<i class="fas fa-bolt" style="color:var(--accent-neon)"></i> ${recText}`;
                } else {
                    summary.innerText = "Serviciul AI nu a putut fi contactat.";
                }
            } catch (e) { 
                summary.innerText = "Eroare de comunicare cu serverul.";
            } finally {
                btnPredict.disabled = false;
                btnPredict.innerText = "Scanează din nou";
            }
        };
    }

    const btnSaveHealth = document.getElementById('btn-save-health');
    if (btnSaveHealth) {
        btnSaveHealth.onclick = async () => {
            console.log("Salvare metrici inițiată...");

            const elHrv = document.getElementById('h-hrv');
            const elSleep = document.getElementById('h-sleep');
            const elStress = document.getElementById('h-stress');
            const elRhr = document.getElementById('h-rhr');

            if (!elHrv || !elSleep || !elStress || !elRhr) {
                alert("Eroare în HTML: ID-urile câmpurilor nu se potrivesc!");
                return;
            }

            const hData = {
                hrv: parseInt(elHrv.value) || 0,
                sleepHours: parseFloat(elSleep.value) || 0,
                stressLevel: parseInt(elStress.value) || 0,
                morningRestingHeartRate: parseInt(elRhr.value) || 0,
                date: new Date().toISOString().split('T')[0]
            };

            if (hData.hrv === 0 && hData.sleepHours === 0 && hData.stressLevel === 0 && hData.morningRestingHeartRate === 0) {
                alert("Te rog să completezi cel puțin o valoare (ex: Ore Somn).");
                return;
            }

            btnSaveHealth.innerText = "Se salvează...";
            btnSaveHealth.disabled = true;

            try {
                const res = await fetch(`${API_BASE}/metrics/add/${user.id}`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(hData)
                });
                
                if (res.ok) {
                    alert("Metricile de sănătate au fost salvate cu succes!");
                    // Curățăm câmpurile după salvare
                    elHrv.value = '';
                    elSleep.value = '';
                    elStress.value = '';
                    elRhr.value = '';
                } else {
                    const errText = await res.text();
                    alert("Serverul a refuzat datele: " + errText);
                }
            } catch (e) { 
                console.error("Eroare de rețea la metrici:", e);
                alert("Eroare de conexiune cu serverul."); 
            } finally { 
                btnSaveHealth.innerText = "Salvează Metricile"; 
                btnSaveHealth.disabled = false;
            }
        };
    }

    // Analiză Nutriție
    const btnAnalyzeMeal = document.getElementById('btn-analyze-meal');
    if (btnAnalyzeMeal) {
        btnAnalyzeMeal.onclick = async () => {
            const desc = document.getElementById('meal-input').value;
            const feedbackBox = document.getElementById('meal-feedback');
            if (!desc) return;

            feedbackBox.style.display = "block";
            feedbackBox.innerHTML = '<div style="color:var(--accent-neon)">Analizăm compoziția nutrițională...</div>';

            try {
                const res = await fetch(`${API_BASE}/meals/analyze?userId=${user.id}`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ description: desc })
                });
                if (res.ok) {
                    const data = await res.json();
                    feedbackBox.innerHTML = `
                        <div style="background:rgba(0,0,0,0.4); padding:10px; border-radius:8px; margin-bottom:12px; display:flex; justify-content:space-around;">
                            <span>🔥 ${data.calories || 0} kcal</span>
                            <span>🥩 ${data.protein || 0}g P</span>
                            <span>🍞 ${data.carbs || 0}g C</span>
                            <span>🥑 ${data.fats || 0}g G</span>
                        </div>
                        <p style="color:#fff; line-height:1.5;">${data.feedback || data.summary || "Analiză completă."}</p>
                    `;
                }
            } catch (e) { 
                console.error(e);
                feedbackBox.innerText = "Eroare la analiza AI Nutriție.";
            }
        };
    }

    // Recuperare
    const btnAnalyzeRec = document.getElementById('btn-analyze-recovery');
    if (btnAnalyzeRec) {
        btnAnalyzeRec.onclick = async () => {
            const sore = document.getElementById('sore-parts').value;
            const feedbackBox = document.getElementById('recovery-feedback');
            if (!sore) return;

            feedbackBox.style.display = "block";
            feedbackBox.innerHTML = '<div style="color:var(--blue-neon)">Generăm protocol de recuperare...</div>';

            try {
                const res = await fetch(`${API_BASE}/recovery/analyze?userId=${user.id}`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ soreParts: sore, painLevel: 5 })
                });
                if (res.ok) {
                    const data = await res.json();
                    feedbackBox.innerHTML = `
                        <div style="color:var(--blue-neon); font-weight:bold; margin-bottom:10px;">
                            <i class="fas fa-clock"></i> Timp estimat: ${data.estimated_recovery || '24h'}
                        </div>
                        <p style="white-space: pre-line; color:#fff;">${data.protocol || data.feedback || data.summary}</p>
                    `;
                }
            } catch (e) { 
                console.error(e);
                feedbackBox.innerText = "Eroare la generarea protocolului.";
            }
        };
    }

});