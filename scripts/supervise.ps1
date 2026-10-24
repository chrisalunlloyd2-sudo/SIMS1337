# UNIFY-2 — one supervisor for the local daemons.
#
# Reconciles the local daemon set to a declared state, then writes
# AIGEN_SYS/reports/supervisor_status.json. Every step is idempotent and
# de-elevated (RunLevel Limited only; the single elevated allowlist action,
# the Ollama env task, is owned by unify2_install_tasks.ps1 / Stop* policy —
# this script never touches elevated surfaces).
#
# Daemons in scope (canonical ports):
#   ollama_serve   127.0.0.1:11434  -> task Aria-Ollama-Serve (already restart-on-failure)
#   orchestrator   127.0.0.1:11435  -> task Unify-Ollama-Orchestrator (scripts/ollama_orchestrator.py);
#                   task not installed yet? falls back to direct Start-Process
#   sophia         11500, dual-listen loopback + tailnet BY DESIGN (run_sophia.pyw hardcodes
#                   both listeners; same policy class as hub 9444/1340 in ops_health.py)
#                   -> task Unify-Sophia (same action as owner-locked legacy SophiaBots;
#                   registered by unify2_install_tasks.ps1)
#   node_poller    hub :9444        -> task Unify-NodePoller (falls back to pythonw direct start)
#   crew_loop      gateway :11435   -> NOT supervised (invocation-terminated by design; polled, not restarted)
#
# Windows Task Scheduler already gives restart-on-failure for task-launched
# processes (RestartCount 3 / RestartInterval 1m). This script adds what the
# scheduler cannot see: duplicate-process (L1) enforcement via the netstat
# listen map, start-if-absent for hand-killed daemons, and one JSON status
# file for humans and other agents.
#
# Stdout is a one-line per-daemon verdict; exit code 0 = all healthy or
# repaired, 1 = something still down after the pass (watchdog/monitor reads it).

param([switch]$DryRun)
$ErrorActionPreference = 'Continue'
Set-StrictMode -Version 2.0

$Root = 'C:\Users\viper\AIGEN_SYS'
$StatusPath = Join-Path $Root 'reports\supervisor_status.json'
$SimsScripts = Join-Path $Root 'repos\SIMS1337\scripts'
$AriaEngine = 'C:\Users\viper\.aria\engine'

# pythonw that survives updates (watchdog convention, STABLE_PY)
$PythonW = 'C:\Users\viper\AppData\Local\Python\bin\pythonw.exe'
$Python  = 'C:\Users\viper\AppData\Local\Python\pythoncore-3.14-64\python.exe'

function Add-Note([string]$msg) {
    if (-not (Get-Variable -Name Notes -Scope Script -ErrorAction SilentlyContinue)) { $script:Notes = @() }
    $script:Notes = @($script:Notes) + $msg
}

function Get-ListenMap {
    # %{"port" -> [addr,pid,...]} for LISTENING sockets, from netstat (no admin needed for own processes)
    $map = @{}
    foreach ($line in (netstat -ano)) {
        if ($line -match '^\s*TCP\s+(\S+):(\d+)\s+\S+\s+LISTENING\s+(\d+)') {
            $port = [int]$Matches[2]; $procId = [int]$Matches[3]
            if (-not $map.ContainsKey($port)) { $map[$port] = @() }
            $map[$port] += @{ addr = $Matches[1]; pid = $procId }
        }
    }
    return $map
}

function Test-HttpOk([string]$url, [int]$timeoutSec = 4) {
    try {
        $r = Invoke-WebRequest -UseBasicParsing -Uri $url -TimeoutSec $timeoutSec
        return ($r.StatusCode -ge 200 -and $r.StatusCode -lt 500)
    } catch { return $false }
}

function Get-Proc([int]$procId) {
    if ($procId -le 0) { return $null }
    return Get-CimInstance Win32_Process -Filter "ProcessId=$procId" -ErrorAction SilentlyContinue
}

function Start-TaskDetached([string]$name) {
    Start-ScheduledTask -TaskName $name -ErrorAction SilentlyContinue
}

# --- per-daemon handlers ------------------------------------------------------

function Repair-OllamaServe($listen, [bool]$dryRun) {
    # owned by Aria-Ollama-Serve; verify + start the task if the port is dark.
    $entries = @(if ($listen.ContainsKey(11434)) { $listen[11434] } else { @() })
    $up = ($entries.Count -gt 0) -and (Test-HttpOk 'http://127.0.0.1:11434/api/version')
    if ($up) {
        # L1: more than one listener on 11434 = the 2026-10-10 duplicate-server race. Keep the
        # LISTENING one, kill stragglers via Stop-Process (same token, no elevation).
        if ($entries.Count -gt 1) {
            $keep = $entries[0].pid
            foreach ($e in ($entries | Select-Object -Skip 1)) {
                if (-not $dryRun) { Stop-Process -Id $e.pid -Force -ErrorAction SilentlyContinue }
                Add-Note ("ollama_serve: killed duplicate pid {0} (kept {1})" -f $e.pid, $keep)
            }
        }
        return @{ ok = $true; state = 'up'; pid = $entries[0].pid }
    }
    if (-not $dryRun) { Start-TaskDetached 'Aria-Ollama-Serve' }
    return @{ ok = $false; state = 'start_requested'; action = 'Start-ScheduledTask Aria-Ollama-Serve' }
}

function Repair-Orchestrator($listen, [bool]$dryRun) {
    $entries = @(if ($listen.ContainsKey(11435)) { $listen[11435] } else { @() })
    $up = ($entries.Count -gt 0) -and (Test-HttpOk 'http://127.0.0.1:11435/health')
    if ($up) {
        if ($entries.Count -gt 1) {
            $keep = $entries[0].pid
            foreach ($e in ($entries | Select-Object -Skip 1)) {
                if (-not $dryRun) { Stop-Process -Id $e.pid -Force -ErrorAction SilentlyContinue }
                Add-Note ("orchestrator: killed duplicate pid {0} (kept {1})" -f $e.pid, $keep)
            }
        }
        return @{ ok = $true; state = 'up'; pid = $entries[0].pid }
    }
    if (-not $dryRun) {
        if (Get-ScheduledTask -TaskName 'Unify-Ollama-Orchestrator' -ErrorAction SilentlyContinue) {
            Start-TaskDetached 'Unify-Ollama-Orchestrator'
        } else {
            # bootstrap path before unify2_install_tasks.ps1 registers the task.
            # Absolute script path: relative 'scripts/...' + WD scripts/ would
            # double-up to scripts\scripts\ollama_orchestrator.py.
            $orchScript = Join-Path $SimsScripts 'ollama_orchestrator.py'
            Start-Process -FilePath $Python -ArgumentList @('"' + $orchScript + '"') `
                -WorkingDirectory $SimsScripts -WindowStyle Hidden -RedirectStandardOutput (Join-Path $Root 'logs\orchestrator_sup.out.log') `
                -RedirectStandardError (Join-Path $Root 'logs\orchestrator_sup.err.log')
        }
    }
    return @{ ok = $false; state = 'start_requested'; action = 'Start Unify-Ollama-Orchestrator (fallback: python ollama_orchestrator.py)' }
}

function Repair-Sophia($listen, [bool]$dryRun) {
    $entries = @(if ($listen.ContainsKey(11500)) { $listen[11500] } else { @() })
    if ($entries.Count -gt 0) { return @{ ok = $true; state = 'up'; pid = $entries[0].pid } }
    if (-not $dryRun) {
        if (Get-ScheduledTask -TaskName 'Unify-Sophia' -ErrorAction SilentlyContinue) {
            Start-TaskDetached 'Unify-Sophia'
        } else {
            Start-TaskDetached 'SophiaBots'  # legacy task, pre-installer fallback
        }
    }
    return @{ ok = $false; state = 'start_requested'; action = 'Start Unify-Sophia (fallback: SophiaBots)' }
}

function Repair-NodePoller([bool]$dryRun) {
    # poller self-heals enrolment; detect by command line, enforce exactly one.
    $procs = @(Get-CimInstance Win32_Process -Filter "Name='python.exe' or Name='pythonw.exe'" |
        Where-Object { $_.CommandLine -match 'node_poller\.py' })
    if ($procs.Count -gt 1) {
        $keep = $procs[0]
        foreach ($p in ($procs | Select-Object -Skip 1)) {
            if (-not $dryRun) {
                Stop-Process -Id $p.ProcessId -Force -ErrorAction SilentlyContinue
                # the hand-launched bash login shell that spawned it is collateral: kill it too
                $par = Get-Proc $p.ParentProcessId
                if ($par -and $par.Name -match 'bash') { Stop-Process -Id $par.ProcessId -Force -ErrorAction SilentlyContinue }
            }
            Add-Note ("node_poller: killed duplicate pid {0} (kept {1})" -f $p.ProcessId, $keep.ProcessId)
        }
    }
    if ($procs.Count -ge 1) { return @{ ok = $true; state = 'up'; pid = $procs[0].ProcessId; dups = ($procs.Count - 1) } }
    if (-not $dryRun) {
        if (Get-ScheduledTask -TaskName 'Unify-NodePoller' -ErrorAction SilentlyContinue) {
            Start-TaskDetached 'Unify-NodePoller'
        } else {
            # bootstrap path before the installer registers the task
            Start-Process -FilePath $PythonW -ArgumentList @('C:\Users\viper\.aria\engine\node_poller.py', 'run') `
                -WorkingDirectory $AriaEngine -WindowStyle Hidden
        }
    }
    return @{ ok = $false; state = 'start_requested'; action = 'Start Unify-NodePoller (fallback: pythonw node_poller.py run)' }
}

function Read-CrewState {
    # crew_loop is invocation-terminated (NOT supervised into permanence). Health = the
    # orchestrator's /stats crew counters when present; absence is normal between rounds.
    try {
        $s = Invoke-RestMethod -Uri 'http://127.0.0.1:11435/stats' -TimeoutSec 4
        $crew = $null
        if ($s.PSObject.Properties.Name -contains 'crew') { $crew = $s.crew }
        return @{ ok = $true; state = 'invocation-terminated'; detail = $crew }
    } catch { return @{ ok = $true; state = 'invocation-terminated'; detail = $null } }
}

# --- main pass -----------------------------------------------------------------


$script:Notes = @()
$listen = Get-ListenMap
$report = @{
    ts          = (Get-Date -Format 'yyyy-MM-ddTHH:mm:sszzz')
    host        = $env:COMPUTERNAME
    dry_run     = [bool]$DryRun
    daemons     = @{}
}
$allOk = $true

foreach ($d in @(
        @{ name = 'ollama_serve';  fn = { param($l, $dry) Repair-OllamaServe $l $dry } },
        @{ name = 'orchestrator';  fn = { param($l, $dry) Repair-Orchestrator $l $dry } },
        @{ name = 'sophia';        fn = { param($l, $dry) Repair-Sophia $l $dry } },
        @{ name = 'node_poller';   fn = { param($l, $dry) Repair-NodePoller $dry } },
        @{ name = 'crew_loop';     fn = { param($l, $dry) Read-CrewState } }
    )) {
    $r = & $d.fn $listen ([bool]$DryRun)
    $report.daemons[$d.name] = $r
    if (-not $r.ok) { $allOk = $false }
    Write-Output ("{0,-13} {1}" -f $d.name, $r.state)
}

# loopback-only gate (L3), port-aware policy (mirrors ops_health.py's TAILNET_OK_PORTS):
#   11434 (ollama) and 11435 (gateway/orchestrator): loopback ONLY (127.0.0.1 / [::1]).
#   11500 (sophia), 9444 (poller hub): loopback + the tailnet IP is ALLOWED;
#       run_sophia.pyw deliberately double-listens on the tailnet (same class as 9444/1340).
#   Wildcard binds (0.0.0.0 / ::) are EXPOSED on every port, no exceptions.
$TailnetIp = '100.77.137.74'
$exposed = @()
foreach ($port in @(11434, 11435, 11500, 9444)) {
    $okAddrs = @('127.0.0.1', '[::1]')
    if ($port -in @(11500, 9444)) { $okAddrs = @($okAddrs + $TailnetIp) }
    foreach ($e in @(if ($listen.ContainsKey($port)) { $listen[$port] } else { @() })) {
        if ($e.addr -notin $okAddrs) { $exposed += ("port {0} on {1} pid {2}" -f $port, $e.addr, $e.pid) }
    }
}
$report.exposed_bindings = $exposed
if ($exposed.Count -gt 0) { $allOk = $false; Add-Note ("EXPOSED: " + ($exposed -join '; ')) }

$report.notes = $script:Notes
$report.all_ok = $allOk
$report | ConvertTo-Json -Depth 5 | Set-Content -Path $StatusPath -Encoding UTF8
foreach ($n in $report.notes) { Write-Output $n }
Write-Output ("status -> {0}" -f $StatusPath)
if ($allOk) { exit 0 } else { exit 1 }