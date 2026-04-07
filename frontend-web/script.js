/**
 * Athletica AI - Global Application State and Event Handlers
 * Orchestrates API communication, session management, and dynamic UI updates.
 */

document.addEventListener('DOMContentLoaded', () => {
    console.log("--- ATHLETICA AI STARTUP ---");

    // --- 1. SESSION CONFIGURATION ---
    const userStr = localStorage.getItem('user');
    if (!userStr) {
        console.warn("Unauthorized access, redirecting to login...");
        window.location.href = 'auth.html';
        return;
    }

    const userRaw = JSON.parse(userStr);
    const user = {
        ...userRaw,
        id: userRaw.id || userRaw.userId,
        userId: userRaw.userId || userRaw.id
    };

    if (!user.id) {
        console.error("Critical: User ID missing from session storage.", user);
        alert("Session error. Please re-authenticate.");
        localStorage.clear();
        window.location.href = 'auth.html';
        return;
    }

    console.log(`Active session for: ${user.username} (ID: ${user.id})`);

    const API_BASE = "http://127.0.0.1:8080/api";
    const AI_PR_API = "http://127.0.0.1:8006/analyze-pr-trend";

    const displayUsername = document.getElementById('display-username');
    if (displayUsername) displayUsername.innerText = user.username;

    // --- 2. NAVIGATION & UI ORCHESTRATION ---
    const tabBtns = document.querySelectorAll('.tab-btn');
    const tabContents = document.querySelectorAll('.tab-content');

    /**
     * Populates exercise selection dropdowns from the backend registry.
     */
    async function loadExerciseOptions() {
        const prSelect = document.getElementById('pr-exercise');
        if (!prSelect) return;

        try {
            const res = await fetch(`${API_BASE}/exercises`);
            if (res.ok) {
                const exercises = await res.json();
                if (exercises.length > 0) {
                    prSelect.innerHTML = '';
                    exercises.forEach(ex => {
                        const opt = document.createElement('option');
                        opt.value = ex.name;
                        opt.innerText = ex.displayName;
                        prSelect.appendChild(opt);
                    });
                    loadPRAnalytics(prSelect.value);
                }
            }
        } catch (e) {
            console.error("Exercise options load failed:", e);
        }
    }

    loadExerciseOptions();

    // Tab switching logic
    tabBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            const targetId = btn.getAttribute('data-target');
            
            tabContents.forEach(content => content.style.display = 'none');
            tabBtns.forEach(b => b.classList.remove('active'));

            const targetContent = document.getElementById(targetId);
            if (targetContent) {
                targetContent.style.display = 'block';
                btn.classList.add('active');
            }

            // Lazy-load section data
            if (targetId === 'pr-section') {
                const prSelect = document.getElementById('pr-exercise');
                if (prSelect) loadPRAnalytics(prSelect.value);
            } else if (targetId === 'health-section') {
                loadHealthMetrics();
            }
        });
    });

    // Session termination
    const btnLogout = document.getElementById('btn-logout');
    if (btnLogout) {
        btnLogout.onclick = () => {
            localStorage.clear();
            window.location.href = 'auth.html';
        };
    }

    // Workout Logging Modal
    const modalWorkout = document.getElementById('workout-modal');
    const btnShowWorkout = document.getElementById('btn-show-workout');
    if (btnShowWorkout) {
        btnShowWorkout.onclick = () => {
            modalWorkout.style.display = 'flex';
        };
    }

    // --- 3. WORKOUT HISTORY ---
    /**
     * Fetches and renders historical workout logs.
     */
    async function loadWorkoutHistory() {
        const historyBody = document.getElementById('workout-history-body');
        if (!historyBody) return;

        try {
            const res = await fetch(`${API_BASE}/workouts/history?userId=${user.id}`);
            if (res.ok) {
                const workouts = await res.json();
                if (workouts.length === 0) {
                    historyBody.innerHTML = '<tr><td colspan="5" style="text-align:center;">No workout logs found.</td></tr>';
                    return;
                }
                historyBody.innerHTML = workouts.map(w => `
                    <tr>
                        <td><strong>${new Date(w.date).toLocaleDateString()}</strong></td>
                        <td><span class="badge">${w.type}</span></td>
                        <td>${w.details || '-'}</td>
                        <td>${w.duration} min</td>
                        <td>${w.averageHeartRate || '--'} BPM</td>
                    </tr>
                `).join('');
            }
        } catch (e) { console.error("History fetch failed:", e); }
    }
    loadWorkoutHistory();

    // --- 4. WORKOUT PERSISTENCE ---
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
                    // Sync fitness profile after new training data
                    loadFitnessLevel();
                }
            } catch (error) { alert("Network error during workout save."); }
            finally { btn.classList.remove('btn-loading'); }
        };
    }

    // --- 5. PERSONAL RECORDS (PR) ANALYTICS ---
    const btnSavePR = document.getElementById('btn-save-pr');
    let prChartInstance = null;

    if (btnSavePR) {
        btnSavePR.onclick = async () => {
            const exercise = document.getElementById('pr-exercise').value;
            const weight = document.getElementById('pr-weight').value;
            const reps = document.getElementById('pr-reps').value;

            if (!weight || !reps) {
                alert("Please enter weight and repetitions!");
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
                    alert("Personal Record recorded! 🎉");
                    document.getElementById('pr-weight').value = '';
                    document.getElementById('pr-reps').value = '';
                    loadPRAnalytics(exercise);
                }
            } catch (e) { console.error("PR persistence failed:", e); }
            finally { btnSavePR.classList.remove('btn-loading'); }
        };
    }

    /**
     * Orchestrates PR history retrieval and AI-driven trend analysis.
     */
    async function loadPRAnalytics(exercise) {
        try {
            // Fetch PR logs from Java Backend
            const historyRes = await fetch(`${API_BASE}/pr/${user.id}/${exercise}`);
            const history = await historyRes.json();

            const prHistoryBody = document.getElementById('pr-history-body');
            const prHistoryTitle = document.getElementById('pr-history-title');
            if (prHistoryTitle) prHistoryTitle.innerText = exercise;

            if (prHistoryBody) {
                if (history.length === 0) {
                    prHistoryBody.innerHTML = '<tr><td colspan="3" style="text-align:center;">No records found.</td></tr>';
                } else {
                    prHistoryBody.innerHTML = history.map(h => `
                        <tr>
                            <td>${new Date(h.date).toLocaleDateString()}</td>
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

            // Fetch regression-based trend from Python AI Service
            const aiRes = await fetch(AI_PR_API, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(history)
            });

            if (aiRes.ok) {
                const aiData = await aiRes.json();
                document.getElementById('display-1rm').innerText = `${aiData.one_rm} kg`;
                document.getElementById('display-next').innerText = `${aiData.next_prediction} kg`;
                renderPRChart(history, aiData.trend);
            }
        } catch (e) { console.error("AI PR analysis failed:", e); }
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

    const prSelect = document.getElementById('pr-exercise');
    if (prSelect) {
        prSelect.onchange = () => loadPRAnalytics(prSelect.value);
    }

    // --- 6. HEALTH METRICS (HRV, SLEEP, STRESS) ---
    const btnSaveHealth = document.getElementById('btn-save-health');
    let healthChartInstance = null;

    if (btnSaveHealth) {
        btnSaveHealth.onclick = async () => {
            const hrv = document.getElementById('h-hrv').value;
            const sleep = document.getElementById('h-sleep').value;
            const stress = document.getElementById('h-stress').value;
            const rhr = document.getElementById('h-rhr').value;

            if (!hrv || !sleep || !stress || !rhr) {
                alert("Please fill in all health metrics!");
                return;
            }

            btnSaveHealth.classList.add('btn-loading');

            const metricsData = {
                hrv: parseInt(hrv),
                sleepHours: parseFloat(sleep),
                stressLevel: parseInt(stress),
                restingHeartRate: parseInt(rhr),
                date: new Date().toISOString().split('T')[0]
            };

            try {
                const res = await fetch(`${API_BASE}/metrics/add/${user.id}`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(metricsData)
                });
                if (res.ok) {
                    alert("Health metrics updated! ❤️");
                    loadHealthMetrics();
                    loadFitnessLevel();
                }
            } catch (e) { console.error("Health metrics save failed:", e); }
            finally { btnSaveHealth.classList.remove('btn-loading'); }
        };
    }

    /**
     * Loads health history and renders comparative charts.
     */
    async function loadHealthMetrics() {
        const historyBody = document.getElementById('health-history-body');
        if (!historyBody) return;

        try {
            const res = await fetch(`${API_BASE}/metrics/user/${user.id}`);
            if (res.ok) {
                const metrics = await res.json();
                if (metrics.length === 0) {
                    historyBody.innerHTML = '<tr><td colspan="5" style="text-align:center;">No health data recorded.</td></tr>';
                    return;
                }
                historyBody.innerHTML = metrics.map(m => `
                    <tr>
                        <td><strong>${new Date(m.date).toLocaleDateString()}</strong></td>
                        <td>${m.hrv} ms</td>
                        <td>${m.sleepHours} h</td>
                        <td>${m.stressLevel}/100</td>
                        <td>${m.restingHeartRate} BPM</td>
                    </tr>
                `).join('');

                renderHealthChart(metrics);
            }
        } catch (e) { console.error("Health load failed:", e); }
    }

    function renderHealthChart(metrics) {
        const canvas = document.getElementById('health-chart');
        if (!canvas) return;
        const ctx = canvas.getContext('2d');
        if (healthChartInstance) healthChartInstance.destroy();

        const sortedMetrics = [...metrics].reverse();

        healthChartInstance = new Chart(ctx, {
            type: 'line',
            data: {
                labels: sortedMetrics.map(m => new Date(m.date).toLocaleDateString()),
                datasets: [
                    { label: 'HRV (ms)', data: sortedMetrics.map(m => m.hrv), borderColor: '#2ecc71', tension: 0.1 },
                    { label: 'Sleep (h)', data: sortedMetrics.map(m => m.sleepHours), borderColor: '#3498db', tension: 0.1 },
                    { label: 'Stress', data: sortedMetrics.map(m => m.stressLevel), borderColor: '#e74c3c', tension: 0.1 }
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

    /**
     * Converts AI Markdown responses to interactive HTML.
     */
    function formatAIResponse(text) {
        if (!text) return "";
        let formatted = text.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
        formatted = formatted.replace(/(?:^\d+\.\s+.*\n?)+/gm, (match) => {
            const items = match.trim().split('\n').map(line => `<li>${line.replace(/^\d+\.\s+/, '')}</li>`).join('');
            return `<ol style="margin-left: 20px; margin-top: 10px; margin-bottom: 10px;">${items}</ol>`;
        });
        formatted = formatted.replace(/(?:^\s*[\-\*]\s+.*\n?)+/gm, (match) => {
            const items = match.trim().split('\n').map(line => `<li>${line.replace(/^\s*[\-\*]\s+/, '')}</li>`).join('');
            return `<ul style="margin-left: 20px; margin-top: 10px; margin-bottom: 10px;">${items}</ul>`;
        });
        return formatted.replace(/\n/g, '<br>');
    }

    // --- 7. FITNESS LEVEL SUMMARY ---
    /**
     * Fetches AI-derived fitness profile and populates dashboards.
     */
    async function loadFitnessLevel() {
        const summary = document.getElementById('fitness-summary');
        const loading = document.getElementById('fitness-loading');

        if (loading) loading.style.display = 'block';
        if (summary) summary.style.display = 'none';

        try {
            const res = await fetch(`${API_BASE}/fitness-level/summary/${user.id}`);
            if (!res.ok) {
                showFitnessDemoData();
                return;
            }

            const data = await res.json();
            const parseN = (val, fallback = 0) => {
                const n = parseFloat(val);
                return isNaN(n) ? fallback : n;
            };

            document.getElementById('fitness-vo2').innerText = parseN(data.vo2_max, 45).toFixed(1);
            document.getElementById('fitness-battery').innerText = parseN(data.body_battery, 70);
            document.getElementById('fitness-score').innerText = `${parseN(data.fitness_level_score, 5)} / 10`;
            document.getElementById('fitness-category').innerText = data.fitness_category || '--';

            document.getElementById('fitness-5k').innerText = `${parseN(data.estimated_5k_time, 25).toFixed(1)} min`;
            document.getElementById('fitness-10k').innerText = `${parseN(data.estimated_10k_time, 50).toFixed(1)} min`;
            document.getElementById('fitness-marathon').innerText = `${parseN(data.estimated_marathon_time, 4).toFixed(2)} hours`;

            document.getElementById('fitness-pushup').innerText = `${parseN(data.pushup_estimate, 0)} reps`;
            document.getElementById('fitness-pullup').innerText = `${parseN(data.pullup_estimate, 0)} reps`;
            document.getElementById('fitness-bench').innerText = `${parseN(data.bench_press_estimate, 0).toFixed(1)} kg`;
            document.getElementById('fitness-deadlift').innerText = `${parseN(data.deadlift_estimate, 0).toFixed(1)} kg`;

            document.getElementById('fitness-insights').innerText = data.ai_insights || 'Insufficient data.';
            document.getElementById('fitness-strengths').innerText = data.strength_weaknesses || 'Insufficient data.';

            if (summary) summary.style.display = 'block';
        } catch (e) {
            console.error("Fitness profile fetch failed:", e);
            showFitnessDemoData();
        } finally {
            if (loading) loading.style.display = 'none';
        }
    }

    function showFitnessDemoData() {
        document.getElementById('fitness-vo2').innerText = '45.5';
        document.getElementById('fitness-battery').innerText = '72';
        document.getElementById('fitness-score').innerText = '7 / 10';
        document.getElementById('fitness-category').innerText = 'Intermediate';
        document.getElementById('fitness-5k').innerText = '25.5 min';
        document.getElementById('fitness-10k').innerText = '54.0 min';
        document.getElementById('fitness-marathon').innerText = '3.50 hours';
        document.getElementById('fitness-pushup').innerText = '35 reps';
        document.getElementById('fitness-pullup').innerText = '12 reps';
        document.getElementById('fitness-bench').innerText = '100.0 kg';
        document.getElementById('fitness-deadlift').innerText = '150.0 kg';
        document.getElementById('fitness-insights').innerText = 'Demo: You are in good health!';
        document.getElementById('fitness-strengths').innerText = 'Demo: Strong cardio, moderate strength.';

        const summary = document.getElementById('fitness-summary');
        if (summary) summary.style.display = 'block';
    }

    const fitnessLevelTab = document.querySelector('[data-target="fitness-level-section"]');
    if (fitnessLevelTab) {
        fitnessLevelTab.addEventListener('click', () => loadFitnessLevel());
    }

    /**
     * Handles text injection with a smooth fade-in animation.
     */
    function typeWriter(element, text) {
        element.innerHTML = "";
        element.style.display = "block";
        const formattedText = formatAIResponse(text);
        element.style.opacity = 0;
        element.innerHTML = formattedText;
        let opacity = 0;
        const interval = setInterval(() => {
            opacity += 0.1;
            element.style.opacity = opacity;
            if (opacity >= 1) clearInterval(interval);
        }, 30);
    }

    // --- 8. AI ASSISTANTS (ADVICE, MEALS, WORKOUTS) ---
    const btnPredict = document.getElementById('btn-predictie');
    if (btnPredict) {
        btnPredict.onclick = async () => {
            const summary = document.getElementById('ai-summary');
            const recommendation = document.getElementById('ai-recommendation');
            btnPredict.classList.add('btn-loading');
            try {
                const res = await fetch(`${API_BASE}/ai/prediction?userId=${user.id}`);
                if (res.ok) {
                    const data = await res.json();
                    typeWriter(summary, data.summary);
                    typeWriter(recommendation, `**Recommendation:** ${data.recommendation}`);
                }
            } catch (e) { summary.innerText = "AI assistant unavailable."; }
            finally { btnPredict.classList.remove('btn-loading'); }
        };
    }

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
                    typeWriter(feedbackBox, `**Estimated Calories:** ${data.calories} kcal<br>**Feedback:** ${data.feedback}`);
                    loadFitnessLevel();
                }
            } catch (e) { console.error(e); }
            finally { btnAnalyzeMeal.classList.remove('btn-loading'); }
        };
    }

    const btnSendRecovery = document.getElementById('btn-send-recovery');
    const recoveryInput = document.getElementById('recovery-chat-input');
    const chatContainer = document.getElementById('recovery-chat-container');
    let recoveryChatHistory = [];

    if (btnSendRecovery && recoveryInput) {
        const sendMessage = async () => {
            const text = recoveryInput.value.trim();
            if (!text) return;

            const userDiv = document.createElement('div');
            userDiv.className = "user-msg";
            userDiv.innerText = text;
            chatContainer.appendChild(userDiv);

            recoveryInput.value = '';
            btnSendRecovery.classList.add('btn-loading');

            try {
                const res = await fetch(`${API_BASE}/ai/recovery-chat?userId=${user.id}`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        user_message: text,
                        chat_history: recoveryChatHistory
                    })
                });

                if (res.ok) {
                    const data = await res.json();
                    const aiDiv = document.createElement('div');
                    aiDiv.className = "ai-msg";
                    if (data.is_final_protocol) aiDiv.style.borderLeftColor = "#2ecc71";

                    chatContainer.appendChild(aiDiv);
                    typeWriter(aiDiv, data.message);

                    recoveryChatHistory.push({ role: "user", content: text });
                    recoveryChatHistory.push({ role: "assistant", content: data.message });
                    if (recoveryChatHistory.length > 10) recoveryChatHistory.shift();

                    chatContainer.scrollTop = chatContainer.scrollHeight;
                    loadFitnessLevel();
                }
            } catch (e) { console.error(e); }
            finally { btnSendRecovery.classList.remove('btn-loading'); }
        };

        btnSendRecovery.onclick = sendMessage;
        recoveryInput.onkeypress = (e) => { if (e.key === 'Enter') sendMessage(); };
    }

    const btnGenMeal = document.getElementById('btn-generate-meal');
    if (btnGenMeal) {
        btnGenMeal.onclick = async () => {
            const ingredients = document.getElementById('ai-ingredients-input').value;
            const resultBox = document.getElementById('ai-meal-result');

            if (!ingredients) {
                alert("Please list your available ingredients!");
                return;
            }

            btnGenMeal.classList.add('btn-loading');
            try {
                const res = await fetch(`${API_BASE}/ai/meal-proposal?userId=${user.id}`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ ingredients: ingredients })
                });

                if (res.ok) {
                    const data = await res.json();
                    resultBox.style.display = "block";
                    const content = `**${data.meal_name}**<br><br>${data.recipe}<br><br>**Nutrition:** ${data.nutritional_info}<br><br>*Rationale: ${data.ai_reasoning}*`;

                    resultBox.innerHTML = "";
                    const innerDiv = document.createElement('div');
                    resultBox.appendChild(innerDiv);
                    typeWriter(innerDiv, content);

                    resultBox.scrollIntoView({ behavior: 'smooth' });
                    loadFitnessLevel();
                }
            } catch (e) { console.error(e); }
            finally { btnGenMeal.classList.remove('btn-loading'); }
        };
    }

    const btnGenWorkout = document.getElementById('btn-generate-workout');
    if (btnGenWorkout) {
        btnGenWorkout.onclick = async () => {
            const userInput = document.getElementById('ai-workout-input').value;
            const resultBox = document.getElementById('ai-workout-result');

            if (!userInput) {
                alert("Please describe your workout preferences!");
                return;
            }

            btnGenWorkout.classList.add('btn-loading');
            try {
                const res = await fetch(`${API_BASE}/ai/workout-proposal?userId=${user.id}`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ userInput: userInput })
                });

                if (res.ok) {
                    const data = await res.json();
                    resultBox.style.display = "block";
                    const content = `**${data.workout_name}**<br><br>${data.exercises}<br><br>**Rationale:** ${data.ai_notes}`;

                    resultBox.innerHTML = "";
                    const innerDiv = document.createElement('div');
                    resultBox.appendChild(innerDiv);
                    typeWriter(innerDiv, content);

                    resultBox.scrollIntoView({ behavior: 'smooth' });
                    loadFitnessLevel();
                }
            } catch (e) { console.error(e); }
            finally { btnGenWorkout.classList.remove('btn-loading'); }
        };
    }
});
