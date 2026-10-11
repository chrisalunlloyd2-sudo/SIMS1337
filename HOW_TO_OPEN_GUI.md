# 🎮 OPEN GODHAND GUI

**Quick Launch (RECOMMENDED):** safe PowerShell launcher
```
powershell -ExecutionPolicy Bypass -File C:\Users\viper\AIGEN_SYS\repos\sims-java-neo-fx\scripts\start_godhand.ps1
```
It boots the dashboard, waits for `/api/status` 200, verifies 8899 is
loopback-only, records the PID in `logs\godhand.pid`, and never kills
anything it did not launch itself.

Stop it with:
```
powershell -ExecutionPolicy Bypass -File C:\Users\viper\AIGEN_SYS\repos\sims-java-neo-fx\scripts\stop_godhand.ps1
```
(Never `taskkill /F /IM javaw.exe` — that kills the MindPalace game too.)

Headless variant (API server only, no GUI window):
```
powershell -ExecutionPolicy Bypass -File C:\Users\viper\AIGEN_SYS\repos\sims-java-neo-fx\scripts\start_godhand.ps1 -Headless
```

---

## 📍 LOCATION

**Project:** `sims-java-neo-fx`
**Path:** `C:\Users\viper\AIGEN_SYS\repos\sims-java-neo-fx\`

---

## 🚀 HOW TO OPEN

### Option 1: Safe launcher (Easiest — recommended)
```powershell
powershell -ExecutionPolicy Bypass -File C:\Users\viper\AIGEN_SYS\repos\sims-java-neo-fx\scripts\start_godhand.ps1
# Wait ~15s → dashboard at http://localhost:8899 (127.0.0.1 only)
```
Exit codes: 0 ok · 2 already healthy but foreign instance binds 0.0.0.0 · 3 boot L1-FAIL (not loopback-bound) · 4 port blocked · 5 build missing · 6 boot crash · 7 boot timeout.

### Option 2: Legacy batch launcher
`LAUNCH_GODHAND.bat` runs `mvn javafx:run` (slower: full maven lifecycle).

### Option 3: Command line
```bash
cd C:\Users\viper\AIGEN_SYS\repos\sims-java-neo-fx
mvn.cmd -o -DskipTests compile     # plain `mvn` shim is broken on this box
scripts\start_godhand.ps1          # then launch
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
- ✅ Java 17 (jdk-17 at C:\Program Files\Java\jdk-17)
- ✅ Maven 3.9.16 (Chocolatey - use mvn.cmd, not plain mvn)
- ✅ JavaFX 17.0.6 (.m2 repository)
- ✅ All code compiled (target/classes)

**Ports:** 8899 = dashboard API (127.0.0.1 ONLY since d02faa1) ·
1338 = public WiFi dashboard (kept as feature, skipped in headless) ·
8900 = GUI live socket.

---

## 🐛 TROUBLESHOOTING

**If GUI doesn't open:**
1. Check java present: `tasklist | findstr javaw`
2. Rebuild: `mvn.cmd -o -DskipTests compile`
3. Check build artifacts exist: `target\classes\com\aigen\sims\GodHandApp.class`
4. Launcher logs: `logs\godhand_headless_console.log` / game_console-equivalent
5. `/api/status`: `curl http://localhost:8899/api/status`

**Common Issues:**
- Port 8899 blocked by a foreign instance → launcher exits 4 and names the PID; stop that process via its owner (never sweep javaw)
- Plain `mvn` shim broken (classworlds error) → use `mvn.cmd`
- Missing dependencies → `mvn.cmd -o -DskipTests compile` first

---

## 🎯 NEXT STEPS

**Once GUI is open:**
1. Click "▶️ Start Agent" to test
2. Click "🔄 Switch Adapter" to cycle through LoRA types
3. Watch activity log for live updates
4. Click "📊 Refresh Stats" to update dashboard

---

*Ready when you are, Architect. Just run the safe launcher.* 💙🚀