# Restart Ollama through the ONLY sanctioned start path: logon task Aria-Ollama-Serve.
#
# WHAT (rewrite 2026-11-20, card t_b58a8120 response to the 2026-10-10 19:29 review):
#   1. Preflight: logon task exists + HKCU\Environment holds the full OLLAMA_* set.
#   2. Stop ONLY the production serve process(es) (the Programs\Ollama\ollama.exe owning
#      port 11434). Never a blanket taskkill /IM ollama.exe: the MPBox copy
#      (C:\MPBox\ollama\ollama.exe serve on 127.0.0.1:11436) must survive.
#   3. Start-ScheduledTask Aria-Ollama-Serve -> watchdog.py start-ollama cold-starts
#      `ollama serve` DETACHED with env read FRESH from HKCU\Environment (its
#      ollama_env() overrides any stale parent env). No UAC, no direct spawn here.
#   4. Verify the live server: its own startup "server config" banner in
#      server-task.err.log (only a banner appended AFTER this restart counts), plus
#      loopback-only bind and cloud/inference markers.
#   LESSON encoded (GodHand outage 2026-11): no program may start its own ollama serve.
#   The watchdog/task path is the single writer of the serve process.
#
# USE:  powershell -ExecutionPolicy Bypass -File scripts\apply_ollama_env_restart.ps1
#       optional: -DryRun   (preflight + live checks WITHOUT stopping/restarting)
#
# Exit codes:
#   0 = verified (or dry-run checks pass) | 2 = serve did not die / port owner not
#   ours | 3 = live verify failed (values, bind, or markers) | 4 = prerequisites
#   missing (task or registry values)
$paramDryRun = $false
foreach ($a in $args) { if ($a -eq '-DryRun') { $paramDryRun = $true } }
$ErrorActionPreference = 'Continue'
$exe      = 'C:\Users\viper\AppData\Local\Programs\Ollama\ollama.exe'
$port     = 11434
$api      = "http://127.0.0.1:$port/api/version"
$taskName = 'Aria-Ollama-Serve'
$logErr   = 'C:\Users\viper\AppData\Local\Ollama\server-task.err.log'
# want as written in HKCU\Environment (source of truth)
$registryWant = [ordered]@{
  'OLLAMA_HOST'              = '127.0.0.1:11434'
  'OLLAMA_MAX_LOADED_MODELS' = '2'
  'OLLAMA_NUM_PARALLEL'      = '1'
  'OLLAMA_KEEP_ALIVE'        = '10m'
  'OLLAMA_FLASH_ATTENTION'   = '1'
  'OLLAMA_CONTEXT_LENGTH'    = '8192'
  'OLLAMA_MAX_QUEUE'         = '64'
  'OLLAMA_NO_CLOUD'          = '1'
  'OLLAMA_LLM_LIBRARY'       = 'cpu'
  'OLLAMA_VULKAN'            = '0'
  'OLLAMA_KV_CACHE_TYPE'     = 'q8_0'
}
# want as the server prints them in its "server config" map (normalized spellings)
$liveWant = [ordered]@{
  'OLLAMA_HOST'              = 'http://127.0.0.1:11434'
  'OLLAMA_MAX_LOADED_MODELS' = '2'
  'OLLAMA_NUM_PARALLEL'      = '1'
  'OLLAMA_KEEP_ALIVE'        = '10m0s'
  'OLLAMA_FLASH_ATTENTION'   = 'true'
  'OLLAMA_CONTEXT_LENGTH'    = '8192'
  'OLLAMA_MAX_QUEUE'         = '64'
  'OLLAMA_NO_CLOUD'          = 'true'
  'OLLAMA_LLM_LIBRARY'       = 'cpu'
  'OLLAMA_VULKAN'            = 'false'
  'OLLAMA_KV_CACHE_TYPE'     = 'q8_0'
}

function Read-LogRange([string]$path, [int64]$offset, [int64]$count) {
  # the ollama process holds this log open for writing; share ReadWrite or the open fails.
  # Latin-1 (28591) = byte<->char 1:1, so string positions == byte offsets. UTF-8 is NOT
  # safe here: invalid bytes in llama-server dumps decode to replacement chars that shift
  # positions vs the file (caused a silent ~7 KB offset drift on 2026-11-20).
  $fs = [System.IO.FileStream]::new($path,
    [System.IO.FileMode]::Open, [System.IO.FileAccess]::Read,
    [System.IO.FileShare]::ReadWrite)
  try {
    if ($offset -gt $fs.Length) { return '' }
    $count = [Math]::Min($count, $fs.Length - $offset)
    $fs.Seek($offset, [System.IO.SeekOrigin]::Begin) | Out-Null
    $buf = New-Object byte[] $count
    $got = 0
    while ($got -lt $count) { $r = $fs.Read($buf, $got, $count - $got); if ($r -le 0) { break }; $got += $r }
    return [System.Text.Encoding]::GetEncoding(28591).GetString($buf, 0, $got)
  } finally { $fs.Close() }
}

function Find-LastConfigBanner([string]$path, [int64]$maxScan) {
  # the log grows constantly (game traffic ~2 KB/s); scan BACKWARDS in chunks until the
  # newest `msg="server config"` line is found. Returns (line, absoluteByteOffset) or $null.
  $needle = 'msg="server config"'
  $chunk = 4MB
  $overlap = 8KB
  $fs = [System.IO.FileStream]::new($path,
    [System.IO.FileMode]::Open, [System.IO.FileAccess]::Read,
    [System.IO.FileShare]::ReadWrite)
  try {
    $len = $fs.Length
    $end = $len
    while ($end -gt 0 -and ($len - $end) -lt $maxScan) {
      $start = [Math]::Max(0, $end - $chunk)
      $fs.Seek($start, [System.IO.SeekOrigin]::Begin) | Out-Null
      $buf = New-Object byte[] ($end - $start)
      $got = 0
      while ($got -lt $buf.Length) { $r = $fs.Read($buf, $got, $buf.Length - $got); if ($r -le 0) { break }; $got += $r }
      $text = [System.Text.Encoding]::GetEncoding(28591).GetString($buf, 0, $got)
      $i = $text.LastIndexOf($needle)
      if ($i -ge 0) {
        $abs = $start + $i
        $line = Read-LogRange $path $abs 4000
        $nl = $line.IndexOf("`n"); if ($nl -ge 0) { $line = $line.Substring(0, $nl) }
        return $line, $abs
      }
      $end = $start + $overlap
      if ($end -le 0) { break }
    }
    return $null
  } finally { $fs.Close() }
}

function Get-PortListenerPids([int]$p) {
  $pids = @()
  $conns = Get-NetTCPConnection -LocalPort $p -State Listen -ErrorAction SilentlyContinue
  if ($conns) {
    foreach ($c in $conns) { $pids += [int]$c.OwningProcess }
  } else {
    # fallback: the LocalAddress is '<addr>:<port>' so anchor on ':<port>' not whitespace
    foreach ($l in (netstat -ano)) {
      if ($l -match (':' + [regex]::Escape([string]$p) + '\s.*LISTENING\s+(\d+)\s*$')) { $pids += [int]$Matches[1] }
    }
  }
  return ,@($pids | Sort-Object -Unique)
}

function Test-LiveBanner([string]$dump, [string]$afterText) {
  # compares the live "server config" dump against $liveWant; prints each verdict;
  # returns $true only when every value AND every marker matches.
  $ok = $true
  foreach ($k in $liveWant.Keys) {
    $m = [regex]::Match($dump, [regex]::Escape($k) + ':([^ ]*)')
    $got = if ($m.Success) { $m.Groups[1].Value } else { '(absent)' }
    $st = if ($got -eq $liveWant[$k]) { 'OK' } else { 'MISMATCH' }
    if ($st -ne 'OK') { $ok = $false }
    Write-Host "$k want=$($liveWant[$k]) got=$got -> $st"
  }
  foreach ($pat in @('Ollama cloud disabled: true', 'inference compute" id=cpu')) {
    $present = $afterText.Contains($pat)
    Write-Host "marker '${pat}' -> $(if ($present) { 'OK' } else { 'MISSING' })"
    if (-not $present) { $ok = $false }
  }
  return $ok
}

Write-Host '=== 1. preflight: task + registry ==='
$t = Get-ScheduledTask -TaskName $taskName -ErrorAction SilentlyContinue
if (-not $t) { Write-Host "FATAL: scheduled task '$taskName' not found"; exit 4 }
Write-Host "task ok: state=$($t.State)"
$reg = Get-ItemProperty -Path 'HKCU:\Environment'
$missing = @()
foreach ($k in $registryWant.Keys) {
  $v = [string]$reg.$k
  if ($v -eq '') { $missing += $k; continue }
  if ($v -ne $registryWant[$k]) { Write-Host "REG $k=$v (expected $($registryWant[$k]))"; $missing += $k }
  else { Write-Host "REG $k=$v" }
}
if ($missing.Count -gt 0) { Write-Host "MISSING/WRONG in HKCU\Environment: $($missing -join ', ')"; exit 4 }

Write-Host '=== 2. listener inventory on :11434 ==='
$prodPids = @()
foreach ($lp in (Get-PortListenerPids $port)) {
  $proc = Get-Process -Id $lp -ErrorAction SilentlyContinue
  if (-not $proc) { Write-Host "listener ${lp}: process gone (ok)"; continue }
  if ($proc.Path -eq $exe) { Write-Host "listener $lp = production ollama ($($proc.StartTime))"; $prodPids += $lp }
  else { Write-Host "FATAL: :$port owned by non-production process: pid=$lp path=$($proc.Path) - not touching it"; exit 2 }
}
if ($prodPids.Count -eq 0) { Write-Host "nothing on :$port from the production binary" }

Write-Host '=== 3. production serve inventory (by exe path; the MPBox copy is a different path and is LEFT ALONE) ==='
$running = Get-Process -Name ollama -ErrorAction SilentlyContinue | Where-Object { $_.Path -eq $exe }
foreach ($r in $running) { Write-Host "prod ollama pid=$($r.Id) started=$($r.StartTime)" }

if ($paramDryRun) {
  Write-Host '=== DRYRUN: stop/start skipped; verifying CURRENT live state ==='
  $up = $false; $v = $null
  try { $v = Invoke-RestMethod -Uri $api -TimeoutSec 5; $up = $true } catch {}
  if (-not $up) { Write-Host 'API down on :11434 (dry-run)'; exit 3 }
  Write-Host "API UP: $($v.version)"
  $banner = Find-LastConfigBanner $logErr 512MB
  if (-not $banner) { Write-Host 'no server config banner found in task log'; exit 3 }
  $dump, $bannerOff = $banner
  $tm = [regex]::Match($dump, 'time=([0-9T:\.\-]+)')
  if ($tm.Success) { Write-Host "banner dated $($tm.Groups[1].Value) (offset $bannerOff)" }
  $afterText = Read-LogRange $logErr $bannerOff 256KB
  $listening = [regex]::Match($afterText, 'Listening on ([^ )]+)').Success
  Write-Host "marker 'Listening on 127.0.0.1:11434' -> $(if ($listening) { 'OK' } else { 'MISSING' })"
  $ok = Test-LiveBanner $dump $afterText
  if (-not $listening) { $ok = $false }
  if (-not $ok) { Write-Host 'VERIFY FAILED (dry-run)'; exit 3 }
  Write-Host 'DRYRUN: ALL CURRENT-LIVE CHECKS PASS'
  exit 0
}

Write-Host '=== 4. stop ONLY production ollama pids ==='
$preLen = (Get-Item $logErr).Length
$stopAt = Get-Date
if ($prodPids.Count -gt 0) {
  foreach ($pp in $prodPids) {
    taskkill /PID $pp /T /F 1>$null 2>&1
    Stop-Process -Id $pp -Force -ErrorAction SilentlyContinue
  }
  $dead = $false
  for ($i = 0; $i -lt 12; $i++) {
    Start-Sleep -Seconds 5
    $left = @(Get-Process -Name ollama -ErrorAction SilentlyContinue | Where-Object { $_.Path -eq $exe })
    $onPort = @(Get-PortListenerPids $port)
    if ($left.Count -eq 0 -and $onPort.Count -eq 0) { $dead = $true; Write-Host "CONFIRMED_DOWN after ~$(( $i + 1 ) * 5)s"; break }
    Write-Host "poll_$($i): procs=$($left.Count) port owners=$($onPort -join ',')"
  }
  if (-not $dead) { Write-Host 'FATAL: production serve did not die'; exit 2 }
} else {
  Write-Host 'no production serve running; restarting via task anyway to pick up env changes'
  $preLen = 0
}

Write-Host '=== 5. start via scheduled task (ONLY start path; no direct spawn) ==='
Start-ScheduledTask -TaskName $taskName
Write-Host 'START_REQUESTED (watchdog.py start-ollama will cold-start serve from HKCU env)'

Write-Host '=== 6. wait for API on :11434 ==='
$up = $false; $v = $null
for ($i = 0; $i -lt 24; $i++) {
  Start-Sleep -Seconds 5
  try { $v = Invoke-RestMethod -Uri $api -TimeoutSec 4; $up = $true; break } catch {}
}
if (-not $up) {
  Write-Host 'API never came up on :11434 - task log tail:'
  Write-Host (Read-LogRange $logErr ([Math]::Max(0, (Get-Item $logErr).Length - 8000)) 8000)
  exit 3
}
Write-Host "API UP: $($v.version)"

Write-Host '=== 7. verify live banner (only lines appended after this restart count) ==='
Start-Sleep -Seconds 3
$banner = Find-LastConfigBanner $logErr 512MB
if (-not $banner) { Write-Host 'no server config banner in task log'; exit 3 }
$dump, $bannerOff = $banner
if ($bannerOff -lt $preLen) { Write-Host "banner at offset $bannerOff predates restart (file len was $preLen) - stale dump, refusing"; exit 3 }
try {
  $tm = [regex]::Match($dump, 'time=([0-9T:\.\-]+)')
  if ($tm.Success) {
    $bannerAt = [datetime]::Parse($tm.Groups[1].Value, [System.Globalization.CultureInfo]::InvariantCulture)
    if ($bannerAt -lt $stopAt.AddSeconds(-10)) { Write-Host "banner time $bannerAt predates stop request $stopAt - stale, refusing"; exit 3 }
    Write-Host "banner time ok: $bannerAt"
  }
} catch { Write-Host "banner timestamp not parseable ($($_.Exception.Message)); offset gate still applies" }
$afterText = Read-LogRange $logErr $bannerOff 256KB
$ok = Test-LiveBanner $dump $afterText

Write-Host '=== 8. loopback-only bind check ==='
foreach ($l in (netstat -ano)) {
  if ($l -match 'LISTENING' -and $l -match ':11434\s') {
    if ($l -match '^\s*TCP\s+(0\.0\.0\.0|::|\[::\]):11434') { Write-Host "BIND CHECK FAILED: non-loopback listening line: $($l.Trim())"; $ok = $false }
  }
}
foreach ($lp in (Get-PortListenerPids $port)) {
  $proc = Get-Process -Id $lp -ErrorAction SilentlyContinue
  if ($proc -and $proc.Path -ne $exe) { Write-Host "BIND UNEXPECTED: pid=$lp path=$($proc.Path) on :$port"; $ok = $false }
}

if (-not $ok) { Write-Host 'VERIFY FAILED'; exit 3 }
Write-Host 'ALL VERIFIED IN RUNNING SERVER'
Write-Host 'Done. Live log: server-task.err.log'
exit 0
