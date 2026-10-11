# GodHand safe launcher (L1-safe) - created 2026-10-10, task t_ef84df83
# L1 contract:
#   - dashboard binds 127.0.0.1:8899 ONLY (loopback) - verified after boot; exits non-zero if violated
#   - NEVER kills any process it did not launch itself (paired stop_godhand.ps1 kills only the
#     PID recorded in logs\godhand.pid, after verifying it is a java GodHandApp process)
# Usage:
#   powershell.exe -ExecutionPolicy Bypass -File scripts\start_godhand.ps1            # GUI (javaw)
#   powershell.exe -ExecutionPolicy Bypass -File scripts\start_godhand.ps1 -Headless  # server (java, no stage)
param(
    [switch]$Headless,
    [switch]$ForceReplace   # only replaces OUR OWN previously-launched instance (logs\godhand.pid)
)
$ErrorActionPreference = "Stop"

$REPO  = "C:\Users\viper\AIGEN_SYS\repos\sims-java-neo-fx"
$M2    = "C:\Users\viper\.m2\repository"
$JFX   = "$M2\org\openjfx"
$LOGS  = "$REPO\logs"
$PIDFILE = "$LOGS\godhand.pid"
$PORT  = 8899

function Test-ApiHealthy {
    try {
        $r = Invoke-WebRequest -Uri "http://127.0.0.1:$PORT/api/status" -UseBasicParsing -TimeoutSec 5
        return ($r.StatusCode -eq 200)
    } catch { return $false }
}

function Get-ListenLine {
    # returns owning PID(s) of LISTENING sockets on $PORT, via netstat text parsing (no CIM inline pitfalls)
    $pids = @()
    foreach ($line in (netstat -ano)) {
        if ($line -match "LISTENING\s+(\d+)\s*$" -and $line -match ":$PORT\s") { $pids += $Matches[1] }
    }
    return ($pids | Select-Object -Unique)
}

# ---- 0. already-up fast path ----
if (Test-ApiHealthy) {
    $bind = ""
    foreach ($line in (netstat -ano)) {
        if ($line -match "LISTENING" -and $line -match ":$PORT\s") { $bind = $line.Trim(); break }
    }
    Write-Output "[OK] GodHand already healthy on :$PORT"
    Write-Output "[BIND] $bind"
    if ($bind -match "0\.0\.0\.0:$PORT") {
        Write-Output "[L1-WARN] running instance binds 0.0.0.0 - relaunch with fixed build required (not our kill; report to owner)"
        exit 2
    }
    exit 0
}

# ---- 1. stale OUR instance replacement (own PID only) ----
if ($ForceReplace -and (Test-Path $PIDFILE)) {
    $ownPid = (Get-Content $PIDFILE -ErrorAction SilentlyContinue | Select-Object -First 1)
    if ($ownPid -match '^\d+$') {
        $proc = Get-Process -Id $ownPid -ErrorAction SilentlyContinue
        if ($proc -and $proc.ProcessName -match '^(java|javaw|javax)$') {
            Write-Output "[REPLACE] stopping our own stale instance PID $ownPid"
            Stop-Process -Id $ownPid -Force -ErrorAction SilentlyContinue
            Start-Sleep -Seconds 3
        }
    }
    Remove-Item $PIDFILE -ErrorAction SilentlyContinue
}

# ---- 2. port free? (something unhealthy may squat it; we NEVER kill it) ----
$squatters = Get-ListenLine
if ($squatters.Count -gt 0) {
    Write-Output "[BLOCKED] port $PORT occupied by PID(s) $($squatters -join ',') and /api/status not 200."
    Write-Output "[BLOCKED] launcher never kills processes it did not start. Ask the owner to stop it or use a different port."
    exit 4
}

# ---- 3. build artifacts present? ----
if (-not (Test-Path "$REPO\target\classes\com\aigen\sims\GodHandApp.class")) {
    Write-Output "[FAIL] target\classes missing GodHandApp.class - run: cd $REPO ; mvn.cmd -o -DskipTests compile"
    exit 5
}

# ---- 4. classpath (keep in sync with scripts\launch_app.ps1 / logs\gov_java_args.txt) ----
$MP = "$JFX\javafx-base\17.0.6\javafx-base-17.0.6-win.jar;$JFX\javafx-controls\17.0.6\javafx-controls-17.0.6-win.jar;$JFX\javafx-graphics\17.0.6\javafx-graphics-17.0.6-win.jar;$JFX\javafx-fxml\17.0.6\javafx-fxml-17.0.6-win.jar"
$CP = "$REPO\target\classes" + `
      ";$M2\com\fasterxml\jackson\core\jackson-databind\2.15.2\jackson-databind-2.15.2.jar" + `
      ";$M2\com\fasterxml\jackson\core\jackson-core\2.15.2\jackson-core-2.15.2.jar" + `
      ";$M2\com\fasterxml\jackson\core\jackson-annotations\2.15.2\jackson-annotations-2.15.2.jar" + `
      ";$M2\org\apache\httpcomponents\client5\httpclient5\5.2.1\httpclient5-5.2.1.jar" + `
      ";$M2\org\apache\httpcomponents\core5\httpcore5\5.2\httpcore5-5.2.jar" + `
      ";$M2\org\apache\httpcomponents\core5\httpcore5-h2\5.2\httpcore5-h2-5.2.jar" + `
      ";$M2\org\slf4j\slf4j-api\2.0.9\slf4j-api-2.0.9.jar" + `
      ";$M2\org\java-websocket\Java-WebSocket\1.5.3\Java-WebSocket-1.5.3.jar" + `
      ";$M2\org\xerial\sqlite-jdbc\3.45.1.0\sqlite-jdbc-3.45.1.0.jar"

if ($Headless) {
    $JAVA = "C:\Program Files\Java\jdk-17\bin\java.exe"
    $jargs = @("-Dprism.order=sw","-Dprism.vsync=false","-XX:+UseG1GC","-Xms256m","-Xmx512m",
               "-Dfile.encoding=UTF-8",
               "-Dcom.aigen.sims.headless=true",
               "-cp", $CP,
               "com.aigen.sims.GodHandApp")
} else {
    $JAVA = "C:\Program Files\Java\jdk-17\bin\javaw.exe"
    $jargs = @("-Dprism.order=sw","-Dprism.vsync=false","-XX:+UseG1GC","-XX:MaxGCPauseMillis=200","-Xms256m","-Xmx512m",
               "-Dfile.encoding=UTF-8",
               "--module-path", $MP,
               "--add-modules", "javafx.controls,javafx.fxml",
               "-cp", $CP,
               "com.aigen.sims.GodHandApp")
}

# ---- 5. launch (record our child PID - the ONLY pid we may ever stop) ----
if ($Headless) {
    $stdout = "$LOGS\godhand_headless_console.log"
    $stderr = "$LOGS\godhand_headless_stderr.log"
    $p = Start-Process -FilePath $JAVA -ArgumentList $jargs -WorkingDirectory $REPO `
           -WindowStyle Hidden -PassThru -RedirectStandardOutput $stdout -RedirectStandardError $stderr
} else {
    $p = Start-Process -FilePath $JAVA -ArgumentList $jargs -WorkingDirectory $REPO -WindowStyle Hidden -PassThru
}
Set-Content -Path $PIDFILE -Value $p.Id
Write-Output ("[LAUNCH] " + $(if ($Headless) { "headless java" } else { "GUI javaw" }) + " PID " + $p.Id + " (recorded in logs\godhand.pid); boot poll starts")

# ---- 6. health wait ----
$deadline = (Get-Date).AddMinutes(8)
while ((Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 5
    if ($p.HasExited) {
        Write-Output "[FAIL] process exited during boot (code $($p.ExitCode)); see logs\godhand_headless_stderr.log"
        exit 6
    }
    if (Test-ApiHealthy) {
        # ---- 7. L1 verification: 8899 must be loopback-only ----
        $bind = ""
        foreach ($line in (netstat -ano)) {
            if ($line -match "LISTENING" -and $line -match ":$PORT\s") { $bind = $line.Trim(); break }
        }
        if ($bind -match "0\.0\.0\.0:$PORT" -or ($bind -notmatch "127\.0\.0\.1:$PORT" -and $bind -notmatch "\[::1\]:$PORT")) {
            Write-Output "[L1-FAIL] dashboard not loopback-bound after boot: $bind"
            exit 3
        }
        Write-Output "[OK] /api/status 200 - L1 verified: $bind"
        exit 0
    }
}
Write-Output "[TIMEOUT] no healthy /api/status within 8 min (PID $($p.Id) still running; boot may still be in progress)"
exit 7