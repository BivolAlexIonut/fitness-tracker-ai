# 🧪 Testarea Automată și Debugging în Athletica AI

Acest document descrie modul în care testele automate au fost utilizate ca instrument de **debugging** și asigurare a calității în cadrul proiectului. Testele ne-au permis să identificăm erori logice și structurale înainte ca acestea să ajungă în faza de producție.

---

## 🔍 1. Debugging prin Teste de Controller (Java)

În timpul dezvoltării `WorkoutController`, am întâmpinat o eroare de logică identificată prin testul `testAddWorkoutUserNotFound`.

*   **Problema identificată:** Testul se aștepta la un cod de stare `400 Bad Request` atunci când un utilizator nu era găsit în baza de date. Totuși, testul a eșuat raportând un cod `404 Not Found`.
*   **Debug:** Analizând eșecul testului, am realizat că implementarea din Controller folosea `ResponseEntity.notFound().build()`. 
*   **Soluție:** Am decis să păstrăm codul `404` (fiind mai corect din punct de vedere REST pentru o resursă lipsă) și am actualizat testul. Testul ne-a forțat să fim consistenți în modul în care raportăm erorile către Frontend.

---

## 📐 2. Validarea Matematică a Algoritmilor AI (Python)

Serviciul de AI folosește regresia polinomială și formula Epley pentru a calcula recordurile (1RM) și trendul de performanță.

*   **Problema potențială:** O mică greșeală în formula matematică ar fi putut oferi utilizatorilor date de antrenament periculoase sau eronate.
*   **Debug prin `test_main.py`:** Am creat cazuri de test cu date de intrare cunoscute (ex: 100kg x 5 repetări). 
*   **Rezultat:** Testul a confirmat că algoritmul returnează ~116kg pentru 1RM. Acest lucru ne-a permis să debug-uim logica matematică fără a cheltui tokeni de API Gemini și fără a porni interfața grafică.

---

## 🌐 3. Debugging-ul Integrării între Microservicii

Un punct critic al aplicației este comunicarea între serviciul Java (8080) și serviciul Python (8006).

*   **Scenariu de test:** `AIServiceIntegrationTest`.
*   **Identificarea erorii:** În timpul rulării testelor, am observat eroarea `Connection refused: connect`.
*   **Debug:** Testul a evidențiat faptul că, dacă serviciul Python este oprit, Backend-ul Java ar putea să "crape".
*   **Implementare Fallback:** Am folosit acest test pentru a verifica logica de tip "fail-safe" din `AIService.java`, asigurându-ne că utilizatorul primește un mesaj prietenos ("Serviciu AI indisponibil") în loc de o eroare generică de server (500).

---

## 📂 4. Corectarea Structurii Proiectului (Package Refactoring)

Testul generat automat `DemoApplicationTests` a eșuat inițial cu eroarea `Unable to find a @SpringBootConfiguration`.

*   **Cauza:** Pachetul testului (`com.example.demo`) nu corespundea cu pachetul aplicației (`com.fitnesstracker.demo`).
*   **Debug:** Eroarea de test ne-a semnalat imediat că structura de directoare a testelor era decalată față de codul sursă.
*   **Rezolvare:** Am mutat clasa de test și am actualizat declarația de `package`, restabilind integritatea build-ului Maven.

---

## 🚀 Concluzie

Utilizarea testelor automate ne-a oferit:
1.  **Viteză în Debugging:** Identificarea erorii în 10 secunde de la scriere, nu după 10 minute de testare manuală.
2.  **Siguranță în Refactoring:** Am putut modifica versiunea de Java (de la 17 la 21) și am știut imediat că aplicația încă funcționează corect.
3.  **Documentație Executabilă:** Testele explică exact cum trebuie să se comporte API-urile noastre în caz de succes sau eroare.
