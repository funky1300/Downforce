🏎️ DOWNFORCE — PROJECT CHECKLIST

━━━ CORE APP & UI ━━━
☐ Race calendar — main screen 
☐ Race detail dialog 
☐ Countdown timer to next race
☐ Google Maps — pin circuit location

━━━ NOTIFICATIONS & ALARMS ━━━
☐ NotificationHelper + RaceActionReceiver
☐ "Watched / Skipped" action buttons 
☐ Auto-schedule race reminders from API
☐ Post-race notification — trigger bet resolution
☐ BootReceiver — reschedule alarms after reboot

━━━ USERS & AUTHENTICATION ━━━
☐ Login screen
☐ Registration screen with ActivityResultLauncher
☐ Firebase Firestore — user document
☐ Camera & gallery — profile picture

━━━ BETTING SYSTEM ━━━
☐ Bet placement screen (bet_f1 activity)
☐ Automatic bet resolution via OpenF1 API
☐ Points scoring system (+10 / +7 / +5 / 0 / −3)
☐ Season auto-reset on March 1st

━━━ STATS & LEADERBOARD ━━━
☐ Personal "Wrapped" stats screen
☐ Global leaderboard
☐ Tap user → view their stats

━━━ AI ASSISTANT ━━━
☐ F1 AI chat screen

━━━ BROADCAST RECEIVERS ━━━
☐ NotificationReceiver
☐ RaceActionReceiver 
☐ BootReceiver — reboot persistence