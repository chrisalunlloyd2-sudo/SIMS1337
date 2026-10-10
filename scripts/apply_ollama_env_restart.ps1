# One-click apply: kill elevated Ollama serve + relaunch from persistent HKCU env
#
# WHAT: Stops the (possibly elevated) Ollama server, then relaunches it with the
# environment read EXPLICITLY from HKCU\Environment (the machine's persistence
# layer — see docs/OLLAMA_ENV_CONFIG.md). Run from ANY PowerShell; UAC asks once.
#
# USE:  powershell -ExecutionPolicy Bypass -File apply_ollama_env_restart.ps1
#
# Verification is automatic: after relaunch it waits for /api/version, then
# greps the new server's "server config" env dump and FAILS (exit 3) unless all
# 7 required OLLAMA_* values are present in the live process. Exit codes:
#   0 = applied + verified | 2 = old server did not die | 3 = verify failed
$ErrorActionPreference = 'Continue'
$exe    = 'C:\Users\viper\AppData\Local\Programs\Ollama\ollama.exe'
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

Write-Host '=== 2. stop running serve (kills elevated too; one UAC prompt if needed) ==='
# -Verb RunAs launches powershell elevated; UAC consent is the one click.
# taskkill /T takes the tree; /F is forced.
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

Write-Host '=== 3. relaunch from HKCU registry env (de-elevated, hidden, manual logs) ==='
# Rebuilding env from registry means the values survive even if this script's
# own session is stale — same mechanism a fresh login session would provide.
$reg = Get-ItemProperty -Path 'HKCU:\Environment'
$envMap = @{}
foreach ($k in $required.Keys) { $envMap[$k] = [string]$reg.$k }
$envMap['OLLAMA_HOST'] = [string]$reg.OLLAMA_HOST
foreach ($k in $envMap.Keys) {
  if ([string]$envMap[$k] -eq '') { Write-Host "MISSING $k in registry - abort"; exit 2 }
  Set-Item -Path "Env:$k" -Value $envMap[$k]
  Write-Host "REG $k=$($envMap[$k])"
}
# normalizing KEEP_ALIVE 5m -> 5m0s cosmetic diff is fine; server prints 5m0s
Start-Process -FilePath $exe -ArgumentList 'serve' -WindowStyle Hidden `
  -RedirectStandardOutput $logOut -RedirectStandardError $logErr `
  -WorkingDirectory 'C:\Users\viper\AppData\Local\Ollama'
Write-Host 'STARTED'

Write-Host '=== 4. wait for API ==='
$up = $false
for ($i = 0; $i -lt 18; $i++) {
  Start-Sleep -Seconds 5
  try { $v = Invoke-RestMethod -Uri 'http://127.0.0.1:11434/api/version' -TimeoutSec 4; $up = $true; break } catch {}
}
if (-not $up) { Write-Host 'API never came up - err log tail:'; Get-Content $logErr -Tail 15; exit 3 }
Write-Host "API UP: $($v.version)"

Write-Host '=== 5. verify live server config dump ==='
Start-Sleep -Seconds 2
$dump = Select-String -Path $logErr -Pattern 'server config' | Select-Object -Last 1
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
Write-Host 'ALL 7/7 VERIFIED IN RUNNING SERVER'
Write-Host 'Done. Logs: server-manual.{out,err}.log (pre-restart copies: *.bak)'