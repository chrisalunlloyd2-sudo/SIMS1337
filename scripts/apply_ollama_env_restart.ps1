# One-click apply: restart Ollama serve under its scheduled task so it picks up
# the persistent HKCU env.
#
# WHAT: Stops the current Ollama server (however it was started: the scheduled
# task Aria-Ollama-Serve, hand-launched, or elevated leftovers), then hands the
# cold start to Aria-Ollama-Serve via Start-ScheduledTask. The task's own
# environment (HKCU\Environment, refreshed by setx in this script) guarantees
# the OLLAMA_* values on launch. Run from ANY PowerShell; UAC asks once.
#
# USE:  powershell -ExecutionPolicy Bypass -File apply_ollama_env_restart.ps1
#
# Verification is automatic: after relaunch it waits for the version probe on
# the REAL server port (127.0.0.1:11434 -- NOT the Hermes gateway: since UNIFY-1
# :11435 is the orchestrator, probing it would always answer and hide a dead
# server), then greps the new server's "server config" env dump and FAILS
# (exit 3) unless all required OLLAMA_* values are present in the live process.
#
# This script does NOT spawn ollama.exe itself (UNIFY-1 fix): if the task is
# missing or fails to start, exit 4 -- supervise.ps1 / the task service is the
# only path that starts serve.
#
# Exit codes:
#   0 = applied + verified | 2 = old server did not die | 3 = env verify failed
#   4 = could not hand start to Aria-Ollama-Serve
$ErrorActionPreference = 'Continue'
$exe     = 'C:\Users\viper\AppData\Local\Programs\Ollama\ollama.exe'
$servePort = 11434
$apiProbe = "http://127.0.0.1:$servePort/api/version"
$taskName = 'Aria-Ollama-Serve'
$logOut = 'C:\Users\viper\AppData\Local\Ollama\server-manual.out.log'
$logErr = 'C:\Users\viper\AppData\Local\Ollama\server-manual.err.log'
$required = [ordered]@{
  'OLLAMA_MAX_LOADED_MODELS' = '1'
  'OLLAMA_NUM_PARALLEL'      = '1'
  'OLLAMA_KEEP_ALIVE'        = '5m'
  'OLLAMA_FLASH_ATTENTION'   = 'true'
  'OLLAMA_CONTEXT_LENGTH'    = '8192'
  'OLLAMA_MAX_QUEUE'         = '64'
}

Write-Host '=== 1. backup manual logs ==='
$stamp = Get-Date -Format 'yyyyMMdd_HHmmss'
Copy-Item $logOut "$logOut.$stamp.bak" -ErrorAction SilentlyContinue
Copy-Item $logErr "$logErr.$stamp.bak" -ErrorAction SilentlyContinue

Write-Host '=== 2. stop running serve (kills elevated leftovers too; one UAC prompt if needed) ==='
Start-Process powershell -Verb RunAs -Wait -ArgumentList '-NoProfile', '-Command',
  "taskkill /F /T /IM ollama.exe"
Start-Sleep -Seconds 4
for ($i = 0; $i -lt 12; $i++) {
  Start-Sleep -Seconds 5
  $p = Get-Process -Name ollama -ErrorAction SilentlyContinue
  if (-not $p) { Write-Host "CONFIRMED_ALL_DEAD after ~$(( $i + 1 ) * 5)s"; break }
  Write-Host "poll_$i still $($p.Count) ollama process(es)"
}
$p = Get-Process -Name ollama -ErrorAction SilentlyContinue
if ($p) { Write-Host 'FATAL: ollama still alive - not relaunching'; exit 2 }

Write-Host '=== 3. refresh HKCU env (persistence layer, see docs/OLLAMA_ENV_CONFIG.md) ==='
$reg = Get-ItemProperty -Path 'HKCU:\Environment'
$missing = @()
foreach ($k in $required.Keys) {
  $v = [string]$reg.$k
  if ($v -eq '') { $missing += $k } else { Write-Host "REG $k=$v" }
}
$regHost = [string]$reg.OLLAMA_HOST
if ($regHost -eq '') { $missing += 'OLLAMA_HOST' }
if ($missing.Count -gt 0) {
  Write-Host "MISSING in registry: $($missing -join ', ') - abort (run docs/OLLAMA_ENV_CONFIG.md setx block first)"
  exit 4
}
Write-Host "REG OLLAMA_HOST=$regHost"

Write-Host '=== 4. cold start via scheduled task (ONLY start path; no direct spawn) ==='
$t = Get-ScheduledTask -TaskName $taskName -ErrorAction SilentlyContinue
if (-not $t) { Write-Host "FATAL: scheduled task '$taskName' not found - supervisor owns ollama start"; exit 4 }
Start-ScheduledTask -TaskName $taskName
Write-Host 'START_REQUESTED'

Write-Host '=== 5. wait for API on the real server port (:11434) ==='
$up = $false
$v = $null
for ($i = 0; $i -lt 18; $i++) {
  Start-Sleep -Seconds 5
  try { $v = Invoke-RestMethod -Uri $apiProbe -TimeoutSec 4; $up = $true; break } catch {}
}
if (-not $up) { Write-Host 'API never came up on :11434 - err log tail:'; Get-Content $logErr -Tail 15; exit 3 }
Write-Host "API UP: $($v.version)"

Write-Host '=== 6. verify live server config dump ==='
Start-Sleep -Seconds 2
$dump = Select-String -Path $logErr -Pattern 'server config' | Select-Object -Last 1
if (-not $dump) { Write-Host 'no server config dump found in err log'; exit 3 }
$ok = $true
foreach ($k in $required.Keys) {
  $want = $required[$k]
  $pattern = "$k" + ':([^ ]*)'
  $m = [regex]::Match($dump.Line, $pattern)
  if ($m.Success) { $got = $m.Groups[1].Value } else { $got = '(absent)' }
  $match = switch ($k) {
    'OLLAMA_KEEP_ALIVE'      { $got -eq '5m0s' }
    'OLLAMA_FLASH_ATTENTION' { $got -eq 'true' }
    default                  { $got -eq $want }
  }
  if ($match) { $state = 'OK' } else { $state = 'MISMATCH' }
  Write-Host "$k want=$want got=$got -> $state"
  if (-not $match) { $ok = $false }
}
if (-not $ok) { Write-Host 'VERIFY FAILED'; exit 3 }

# loopback-only check: the server must NOT be listening on non-loopback
# addresses after the env apply (OLLAMA_HOST must be 127.0.0.1:11434).
Write-Host '=== 7. loopback-only bind check (:11434) ==='
$binds = Get-NetTCPConnection -LocalPort $servePort -State Listen -ErrorAction SilentlyContinue |
  Select-Object -ExpandProperty LocalAddress -Unique
$bad = @($binds | Where-Object { $_ -notmatch '^(127\.0\.0\.1|::1|\[::1\])$' })
if ($bad.Count -gt 0) {
  Write-Host ("BIND CHECK FAILED: :$servePort also bound to {0}" -f ($bad -join ', '))
  exit 3
}
if (-not $binds) { Write-Host ("BIND CHECK FAILED: nothing listening on :$servePort"); exit 3 }
Write-Host ("BIND OK: :$servePort loopback-only ({0})" -f ($binds -join ', '))

Write-Host 'ALL VERIFIED IN RUNNING SERVER'
Write-Host 'Done. Logs: server-manual.{out,err}.log (pre-restart copies: *.bak)'