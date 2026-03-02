document.addEventListener('DOMContentLoaded', () => {
    
    const butonPredictie = document.getElementById('btn-predictie');
    const textPredictie = document.getElementById('predictie-text');

    butonPredictie.addEventListener('click', () => {
        
        textPredictie.innerHTML = "Se analizează datele în modulul de AI...";
        textPredictie.style.color = "yellow";

        setTimeout(() => {
            const raspunsPython = "Pe baza caloriilor arse în ultimele 7 zile, vei atinge obiectivul de greutate în 14 zile. Menține ritmul! 🚀";

            textPredictie.innerHTML = raspunsPython;
            textPredictie.style.color = "var(--verde-ai)"; 

        }, 2000); 
    });
});