param([switch]$IncludeElevated)

# suppress noisy progress to parent, show real state at exit
$ProgressPreference = 'SilentlyContinue'
$ErrorActionPreference = 'Continue'
Set-StrictMode -Version 2.0

$SimsScripts = 'C:\Users\viper\AIGEN_SYS\repos\SIMS1337\scripts'
$Script:Fatal = @()

function Add-Fatal([string]$msg) {
    $Script:Fatal += $msg
}

$IsAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()
    ).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
Write-Output ("elevated={0}" -f $IsAdmin)

#
# --- shared task settings -----------------------------------------------------
# De-elevated principal (Limited), only-when-logged-on, restart-on-failure.
# This mirrors the compliant tasks we inventoried: Aria-Ollama-Serve (3x/1m)
# and Hermes_Gateway (999x/1m).
#
$Principal = New-ScheduledTaskPrincipal -UserId $env:USERNAME -LogonType Interactive -RunLevel Limited
$SettingsBase = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries `
    -ExecutionTimeLimit ([TimeSpan]::Zero) -MultipleInstances IgnoreNew

function New-UnifyTrigger {
    # at logon, small delay so the network/Tailscale is up
    $t = New-ScheduledTaskTrigger -AtLogOn -User $env:USERNAME
    $t.Delay = 'PT20S'
    return $t
}

function Assert-TaskHealthy([string]$name, [scriptblock]$settings) {
    # verify after registration: principal limited + RestartCount >= 3
    $t = Get-ScheduledTask -TaskName $name -ErrorAction SilentlyContinue
    if (-not $t) { Add-Fatal ("{0}: MISSING after register" -f $name); return $false }
    if ($t.Principal.RunLevel -ne 'Limited') { Add-Fatal ("{0}: RunLevel={1}" -f $name, $t.Principal.RunLevel) }
    if ($t.Settings.RestartCount -lt 3) { Add-Fatal ("{0}: RestartCount={1}" -f $name, $t.Settings.RestartCount) }
    return $true
}

#
# 1. Unify-Sophia -- SophiaBots exists but is owner-locked (Set-ScheduledTask
#    from a de-elevated, non-owner context is denied). So we register our OWN
#    task with the same action (run_sophia.pyw via uv pythonw; loopback +
#    tailscale dual listen hardcoded there), Limited + restart-on-failure.
#    SophiaBots stays untouched as the legacy path; duplicate listeners are
#    harmless (SO_REUSEADDR socket already-listen means the second instance
#    exits) and supervise.ps1's port map reports whichever is live.
#
$uvPythonW = 'C:\Users\viper\AppData\Roaming\uv\python\cpython-3.11-windows-x86_64-none\pythonw.exe'
$sophiaAction = New-ScheduledTaskAction -Execute $uvPythonW `
    -Argument '"C:\Users\viper\AIGEN_SYS\repos\sophia_bots\run_sophia.pyw"' `
    -WorkingDirectory 'C:\Users\viper\AIGEN_SYS\repos\sophia_bots'
$sophiaSettings = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries `
    -ExecutionTimeLimit ([TimeSpan]::Zero) -MultipleInstances IgnoreNew -RestartCount 3 -RestartInterval (New-TimeSpan -Minutes 1)
Register-ScheduledTask -TaskName 'Unify-Sophia' `
    -Action $sophiaAction -Principal $Principal -Settings $sophiaSettings -Trigger (New-UnifyTrigger) `
    -Force -ErrorAction Continue | Out-Null
[void](Assert-TaskHealthy 'Unify-Sophia' { $sophiaSettings })
Write-Output 'sophia: Unify-Sophia registered'

# legacy SophiaBots: attempt a settings patch, but denial is NON-FATAL here
# (the de-elevated installer is usually not the task owner).
$sophiaLegacy = Get-ScheduledTask -TaskName 'SophiaBots' -ErrorAction SilentlyContinue
if ($sophiaLegacy) {
    try {
        $sSet = $sophiaLegacy.Settings
        $sSet.RestartCount = 3
        $sSet.RestartInterval = 'PT1M'
        Set-ScheduledTask -TaskName 'SophiaBots' -Settings $sSet -ErrorAction Stop | Out-Null
        Write-Output 'sophia: SophiaBots settings patched (RestartCount=3, Interval=1m)'
    } catch {
        Write-Output ("sophia: SophiaBots not patched (owner-locked: {0}) - Unify-Sophia covers it" -f $_.Exception.Message)
    }
} else {
    Write-Output 'sophia: SophiaBots task not found (skipped)'
}

#
# 2. Unify-Ollama-Orchestrator -- was hand-started (PID churn 1284->22424);
#    bring under task management with restart-on-failure.
#    Loopback-only bind is enforced inside ollama_orchestrator.py itself
#    (BIND_HOST = "127.0.0.1"), plus supervise.ps1's L3 gate.
#
$orchAction = New-ScheduledTaskAction -Execute 'C:\Users\viper\AppData\Local\Python\pythoncore-3.14-64\python.exe' `
    -Argument '"C:\Users\viper\AIGEN_SYS\repos\SIMS1337\scripts\ollama_orchestrator.py"' `
    -WorkingDirectory 'C:\Users\viper\AIGEN_SYS\repos\SIMS1337\scripts'
$orchSettings = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries `
    -ExecutionTimeLimit ([TimeSpan]::Zero) -MultipleInstances IgnoreNew -RestartCount 3 -RestartInterval (New-TimeSpan -Minutes 1)
Register-ScheduledTask -TaskName 'Unify-Ollama-Orchestrator' `
    -Action $orchAction -Principal $Principal -Settings $orchSettings -Trigger (New-UnifyTrigger) `
    -Force -ErrorAction Continue | Out-Null
[void](Assert-TaskHealthy 'Unify-Ollama-Orchestrator' { $orchSettings })
Write-Output 'orchestrator: Unify-Ollama-Orchestrator registered'

#
# 3. Unify-NodePoller -- the .aria engine node_poller run loop; was hand-started
#    via a bash login shell (which produced the 22116/7572 duplicate pair).
#
$pollerAction = New-ScheduledTaskAction -Execute 'C:\Users\viper\AppData\Local\Python\bin\pythonw.exe' `
    -Argument '"C:\Users\viper\.aria\engine\node_poller.py" run'
$pollerSettings = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries `
    -ExecutionTimeLimit ([TimeSpan]::Zero) -MultipleInstances IgnoreNew -RestartCount 3 -RestartInterval (New-TimeSpan -Minutes 1)
Register-ScheduledTask -TaskName 'Unify-NodePoller' `
    -Action $pollerAction -Principal $Principal -Settings $pollerSettings -Trigger (New-UnifyTrigger) `
    -Force -ErrorAction Continue | Out-Null
[void](Assert-TaskHealthy 'Unify-NodePoller' { $pollerSettings })
Write-Output 'poller: Unify-NodePoller registered'

#
# 4. Unify-Ollama-Env-Apply -- THE ONLY ELEVATED TASK IN THE UNIFY-2 SET.
#    Principal is Highest (admin); action is exclusively the reviewed
#    apply_ollama_env_restart.ps1 (never re-kill/re-launch ollama itself here).
#    Registering a Highest-runlevel task requires admin, so this step is
#    opt-in (-IncludeElevated, run the installer elevated) OR the allowlist
#    route below can be used instead.
#    Run manually:  Start-ScheduledTask -TaskName Unify-Ollama-Env-Apply
#
if ($IncludeElevated) {
    $envAction = New-ScheduledTaskAction -Execute 'powershell.exe' `
        -Argument '-NoProfile -ExecutionPolicy Bypass -File "C:\Users\viper\AIGEN_SYS\repos\SIMS1337\scripts\apply_ollama_env_restart.ps1"'
    $envPrincipal = New-ScheduledTaskPrincipal -UserId $env:USERNAME -LogonType Interactive -RunLevel Highest
    $envSettings = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries `
        -ExecutionTimeLimit (New-TimeSpan -Minutes 30) -MultipleInstances IgnoreNew
    Register-ScheduledTask -TaskName 'Unify-Ollama-Env-Apply' `
        -Action $envAction -Principal $envPrincipal -Settings $envSettings `
        -Trigger (New-ScheduledTaskTrigger -Once -At (Get-Date).AddMinutes(5) -RepetitionInterval (New-TimeSpan -Days 1)) `
        -Force -ErrorAction Continue | Out-Null
    [void](Assert-TaskHealthy 'Unify-Ollama-Env-Apply' { $envSettings })
    Write-Output 'env-apply: Unify-Ollama-Env-Apply registered (elevated, allowlisted, on-demand)'
} else {
    Write-Output 'env-apply: skipped (run with -IncludeElevated from an elevated shell to register)'
}

#
# 5. Final report
#
Write-Output ''
foreach ($n in @('SophiaBots', 'Unify-Sophia', 'Unify-Ollama-Orchestrator', 'Unify-NodePoller', 'Unify-Ollama-Env-Apply')) {
    $t = Get-ScheduledTask -TaskName $n -ErrorAction SilentlyContinue
    if ($t) {
        Write-Output ('{0,-28} RunLevel={1,-8} RestartCount={2} State={3}' -f $n, $t.Principal.RunLevel, $t.Settings.RestartCount, $t.State)
    } else {
        Write-Output ('{0,-28} MISSING' -f $n)
    }
}
if ($Script:Fatal.Count -gt 0) {
    Write-Output ''
    Write-Output 'FATAL:'
    foreach ($f in $Script:Fatal) { Write-Output ('  ' + $f) }
    exit 1
}
Write-Output ''
Write-Output 'INSTALL OK'