# GodHand safe stopper (L1-safe) - created 2026-10-10, task t_ef84df83
# Kills ONLY the PID recorded in logs\godhand.pid by start_godhand.ps1, and only
# after verifying it is a java-family process running GodHandApp.
# NEVER sweeps other java processes. Never called with a hand-typed PID.
$ErrorActionPreference = "Stop"
$PIDFILE = "C:\Users\viper\AIGEN_SYS\repos\sims-java-neo-fx\logs\godhand.pid"

if (-not (Test-Path $PIDFILE)) {
    Write-Output "[OK] no godhand.pid state file - nothing of ours to stop"
    exit 0
}
$ownPid = (Get-Content $PIDFILE -ErrorAction SilentlyContinue | Select-Object -First 1)
if ($ownPid -notmatch '^\d+$') {
    Write-Output "[WARN] pid file unreadable (''$ownPid'') - refusing to act"; exit 1
}

$proc = Get-Process -Id $ownPid -ErrorAction SilentlyContinue
if (-not $proc) {
    Write-Output "[OK] our PID $ownPid already gone"
    Remove-Item $PIDFILE -ErrorAction SilentlyContinue
    exit 0
}
if ($proc.ProcessName -notmatch '^(java|javaw|javax)$') {
    Write-Output "[REFUSE] our recorded PID $ownPid is now '$($proc.ProcessName)' - not java; pid-reuse risk, refusing to kill"
    Remove-Item $PIDFILE -ErrorAction SilentlyContinue
    exit 2
}
# identity check: command line must contain GodHandApp
$cmdline = ""
try {
    $w = Get-CimInstance Win32_Process -Filter "ProcessId = $ownPid"
    if ($w) { $cmdline = [string]$w.CommandLine }
} catch { }
if ($cmdline -notmatch 'GodHandApp') {
    Write-Output "[REFUSE] PID $ownPid command line does not contain GodHandApp - refusing to kill"
    exit 3
}
Stop-Process -Id $ownPid -Force
Start-Sleep -Seconds 2
if (Get-Process -Id $ownPid -ErrorAction SilentlyContinue) {
    Write-Output "[WARN] PID $ownPid still alive after Stop-Process"
    exit 4
}
Write-Output "[OK] stopped our GodHand instance PID $ownPid"
Remove-Item $PIDFILE -ErrorAction SilentlyContinue
exit 0