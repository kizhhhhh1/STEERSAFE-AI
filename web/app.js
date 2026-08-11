document.addEventListener('DOMContentLoaded', () => {
    // DOM Elements
    const circle = document.querySelector('.circle');
    const scoreText = document.getElementById('main-risk-score');
    const labelText = document.getElementById('main-risk-label');
    
    const valSpeed = document.getElementById('val-speed');
    const valBrake = document.getElementById('val-brake');
    const valAccel = document.getElementById('val-accel');
    const valTurns = document.getElementById('val-turns');

    // Configuration
    const riskColors = {
        safe: 'var(--risk-safe)',
        moderate: 'var(--risk-moderate)',
        high: 'var(--risk-high)'
    };

    // State
    let currentScore = 0;

    // Simulation loop
    setInterval(() => {
        // Generate random simulated telemetry
        const speed = Math.floor(Math.random() * (130 - 40 + 1)) + 40;
        
        // Simulating accumulating events
        const brakeEvents = Math.floor(Math.random() * 3);
        const accelEvents = Math.floor(Math.random() * 2);
        const turnEvents = Math.floor(Math.random() * 2);
        
        // Calculate a basic risk score out of 100
        let newScore = Math.floor((speed / 120) * 40 + (brakeEvents * 10) + (accelEvents * 10) + (turnEvents * 5));
        newScore = Math.min(Math.max(newScore, 0), 100); // Clamp 0-100

        updateDashboard(newScore, speed, brakeEvents, accelEvents, turnEvents);
    }, 2000); // Update every 2 seconds

    function updateDashboard(score, speed, brake, accel, turns) {
        // Animate score text
        animateValue(scoreText, currentScore, score, 500);
        currentScore = score;

        // Update metrics text
        valSpeed.innerHTML = `${speed} <span class="unit">km/h</span>`;
        // For events, we just simulate them accumulating slightly or keeping a running total for realism
        valBrake.innerHTML = `${parseInt(valBrake.innerText) + brake} <span class="unit">events</span>`;
        valAccel.innerHTML = `${parseInt(valAccel.innerText) + accel} <span class="unit">events</span>`;
        valTurns.innerHTML = `${parseInt(valTurns.innerText) + turns} <span class="unit">events</span>`;

        // Determine Risk Level
        let color, label;
        if (score <= 30) {
            color = riskColors.safe;
            label = "Safe";
        } else if (score <= 65) {
            color = riskColors.moderate;
            label = "Moderate";
        } else {
            color = riskColors.high;
            label = "High Risk";
        }

        // Update Gauge
        // SVG circle dasharray is calculated as: stroke-dasharray="progress, 100"
        circle.style.strokeDasharray = `${score}, 100`;
        circle.style.stroke = color;
        scoreText.style.color = color;
        
        labelText.innerText = label;
    }

    // Helper to animate numbers
    function animateValue(obj, start, end, duration) {
        let startTimestamp = null;
        const step = (timestamp) => {
            if (!startTimestamp) startTimestamp = timestamp;
            const progress = Math.min((timestamp - startTimestamp) / duration, 1);
            obj.innerHTML = Math.floor(progress * (end - start) + start);
            if (progress < 1) {
                window.requestAnimationFrame(step);
            }
        };
        window.requestAnimationFrame(step);
    }
});
