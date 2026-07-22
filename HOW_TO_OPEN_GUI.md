# 🎮 OPEN GODHAND GUI

**Quick Launch:** Double-click this file:
```
C:\Users\viper\AIGEN_SYS\repos\sims-java-neo-fx\LAUNCH_GODHAND.bat
```

---

## 📍 LOCATION

**Project:** `sims-java-neo-fx`  
**Path:** `C:\Users\viper\AIGEN_SYS\repos\sims-java-neo-fx\`

---

## 🚀 HOW TO OPEN

### Option 1: Double-Click Launcher (Easiest)
1. Navigate to: `C:\Users\viper\AIGEN_SYS\repos\sims-java-neo-fx\`
2. Double-click: `LAUNCH_GODHAND.bat`
3. Wait 10-15 seconds
4. GodHand GUI window appears! 🎮

### Option 2: Command Line
```bash
cd C:\Users\viper\AIGEN_SYS\repos\sims-java-neo-fx
set JAVA_HOME=C:\Program Files\Java\jdk-17
mvn javafx:run
```

### Option 3: PowerShell
```powershell
cd C:\Users\viper\AIGEN_SYS\repos\sims-java-neo-fx
$env:JAVA_HOME="C:\Program Files\Java\jdk-17"
mvn javafx:run
```

---

## 🎮 WHAT YOU'LL SEE

**GodHand Dashboard:**
- 🧠 Model Pool (4 tiers: Fast, Balanced, Reasoning, Deep)
- 🔄 LoRA Adapters (6 types: CHAT, CODE, PATHFIND, etc.)
- 📋 Task Queue (live stats, progress bar)
- 🎛️ Control Panel (Start, Pause, Stop, Switch Adapter)
- 📜 Activity Log (real-time console)

**Features:**
- ✅ Live model status (Warm/Cold indicators)
- ✅ Visual adapter switching
- ✅ Queue utilization tracking
- ✅ One-click agent control
- ✅ Timestamped activity log

---

## ⚙️ REQUIREMENTS

**Already Installed:**
- ✅ Java 17 (Chocolatey)
- ✅ Maven 3.9.16 (Chocolatey)
- ✅ JavaFX 17 (Maven dependency)
- ✅ All code compiled

**Ready to run!**

---

## 🐛 TROUBLESHOOTING

**If GUI doesn't open:**
1. Check Java installed: `java --version`
2. Check Maven installed: `mvn --version`
3. Re-run launcher as Administrator
4. Check logs in console window

**Common Issues:**
- JAVA_HOME not set → Launcher sets it automatically
- Port in use → Close other JavaFX apps
- Missing dependencies → Run `mvn clean compile` first

---

## 🎯 NEXT STEPS

**Once GUI is open:**
1. Click "▶️ Start Agent" to test
2. Click "🔄 Switch Adapter" to cycle through LoRA types
3. Watch activity log for live updates
4. Click "📊 Refresh Stats" to update dashboard

**Then:**
- Wire up viper-scripts integration
- Connect to Ollama models
- Add WebSocket streaming
- Build Markov Logic Reviews

---

*Ready when you are, Architect. Just double-click the launcher.* 💙🚀
