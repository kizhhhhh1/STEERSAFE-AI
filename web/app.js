document.addEventListener('DOMContentLoaded', () => {
    // Check configuration is set up
    if (typeof firebaseConfig === 'undefined' || firebaseConfig.apiKey === "YOUR_API_KEY") {
        console.warn("Firebase configuration has not been set up yet. Please update web/firebase-config.js with your keys.");
        // Fallback to simulation mode if no Firebase configuration is supplied
        initializeSimulationMode();
        return;
    }

    // Initialize Firebase Compat
    firebase.initializeApp(firebaseConfig);
    const auth = firebase.auth();
    const db = firebase.firestore();

    // DOM Elements
    const loginOverlay = document.getElementById('login-overlay');
    const appContainer = document.getElementById('app-container');
    const signInBtn = document.getElementById('google-signin-btn');
    const logoutBtn = document.getElementById('logout-btn');
    
    const userAvatar = document.getElementById('user-avatar');
    const userName = document.getElementById('user-name');

    const circle = document.querySelector('.circle');
    const scoreText = document.getElementById('main-risk-score');
    const labelText = document.getElementById('main-risk-label');
    
    const valSpeed = document.getElementById('val-speed');
    const valBrake = document.getElementById('val-brake');
    const valAccel = document.getElementById('val-accel');
    const valTurns = document.getElementById('val-turns');

    // Risk Color Configuration
    const riskColors = {
        safe: 'var(--risk-safe)',
        moderate: 'var(--risk-moderate)',
        high: 'var(--risk-high)'
    };

    let currentScore = 0;
    let firestoreUnsubscribe = null;

    // 1. Authentication State Listener
    auth.onAuthStateChanged((user) => {
        if (user) {
            // User is signed in
            loginOverlay.style.display = 'none';
            appContainer.style.display = 'flex';

            // Set user profile
            userAvatar.src = user.photoURL || 'https://i.pravatar.cc/150?img=11';
            userName.innerText = user.displayName || 'Admin';

            // Start listening to live telemetry from Firestore
            startRealtimeListener();
        } else {
            // User is signed out
            loginOverlay.style.display = 'flex';
            appContainer.style.display = 'none';

            // Unsubscribe from Firestore if listening
            if (firestoreUnsubscribe) {
                firestoreUnsubscribe();
                firestoreUnsubscribe = null;
            }
        }
    });

    // 2. Google Sign-In Event Handler
    signInBtn.addEventListener('click', () => {
        const provider = new firebase.auth.GoogleAuthProvider();
        auth.signInWithPopup(provider).catch((error) => {
            console.error("Sign in failed: ", error);
            alert("Failed to sign in. Please verify your Firebase project setup.");
        });
    });

    // 3. Log Out Event Handler
    logoutBtn.addEventListener('click', () => {
        auth.signOut();
    });

    // 4. Firestore Real-time listener
    function startRealtimeListener() {
        // Query the live_sessions collection for the most recent updates
        // Note: Make sure the Android app writes updates containing:
        // { riskScore, currentSpeed, harshBraking, suddenAccel, sharpTurns, timestamp }
        firestoreUnsubscribe = db.collection("live_sessions")
            .orderBy("timestamp", "desc")
            .limit(1)
            .onSnapshot((snapshot) => {
                if (!snapshot.empty) {
                    const latestSessionDoc = snapshot.docs[0];
                    const data = latestSessionDoc.data();
                    
                    updateDashboard(
                        data.riskScore || 0,
                        data.currentSpeed || 0,
                        data.harshBraking || 0,
                        data.suddenAccel || 0,
                        data.sharpTurns || 0
                    );
                } else {
                    // Fallback visual state if no drives are active
                    updateDashboard(0, 0, 0, 0, 0);
                    labelText.innerText = "STANDBY";
                }
            }, (error) => {
                console.error("Firestore listening error: ", error);
            });
    }

    function updateDashboard(score, speed, brake, accel, turns) {
        animateValue(scoreText, currentScore, score, 500);
        currentScore = score;

        valSpeed.innerHTML = `${Math.round(speed)} <span class="unit">km/h</span>`;
        valBrake.innerHTML = `${brake} <span class="unit">events</span>`;
        valAccel.innerHTML = `${accel} <span class="unit">events</span>`;
        valTurns.innerHTML = `${turns} <span class="unit">events</span>`;

        let color, label;
        if (score === 0 && speed === 0) {
            color = 'var(--text-secondary)';
            label = "STANDBY";
        } else if (score <= 30) {
            color = riskColors.safe;
            label = "Safe";
        } else if (score <= 65) {
            color = riskColors.moderate;
            label = "Moderate";
        } else {
            color = riskColors.high;
            label = "High Risk";
        }

        circle.style.strokeDasharray = `${score}, 100`;
        circle.style.stroke = color;
        scoreText.style.color = color;
        labelText.innerText = label;
    }

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

    // Fallback simulation mode in case Firebase setup hasn't been done yet
    function initializeSimulationMode() {
        console.log("Initializing simulation mode for demo purposes...");
        
        loginOverlay.style.display = 'none';
        appContainer.style.display = 'flex';
        userName.innerText = "Demo Admin";
        userAvatar.src = "https://i.pravatar.cc/150?img=11";

        setInterval(() => {
            const speed = Math.floor(Math.random() * (100 - 45 + 1)) + 45;
            let score = Math.floor((speed / 120) * 40);
            score = Math.min(Math.max(score, 0), 100);
            
            updateDashboard(score, speed, Math.floor(Math.random() * 2), Math.floor(Math.random() * 2), Math.floor(Math.random() * 2));
        }, 3000);
    }
});
