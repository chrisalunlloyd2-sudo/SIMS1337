# SIMS1337 Terminal CLI v1.0 — One-word commands for full environment control
# Usage: .\sims.ps1 <command> [args]
# All commands chain real local SLM models via Ollama API.
param([string]$Cmd, [string]$Arg1, [string]$Arg2, [string]$Arg3)

$API = "http://localhost:8899"
$OLLAMA = "http://localhost:11434/api/generate"
$DATA = "$PSScriptRoot\logs\sims-state.json"

function api { param($path) try { (Invoke-RestMethod "$API$path" -TimeoutSec 5) } catch { $null } }
function ollama { param($model,$system,$prompt,$tokens=80)
  $body = @{model=$model; system=$system; prompt=$prompt; stream=$false; options=@{num_predict=$tokens; temperature=0.7}} | ConvertTo-Json -Compress
  try { $r = Invoke-RestMethod $OLLAMA -Method Post -Body $body -ContentType "application/json" -TimeoutSec 30; return $r.response } catch { return "[offline]" }
}
function log { param($msg) $d = Get-Date -Format "HH:mm:ss"; Write-Host "[$d] $msg" -ForegroundColor Cyan }

switch ($Cmd) {

  "status" {
    $s = api "/api/status"
    if ($s) {
      Write-Host "`nSIMS1337 v$($s.version)" -ForegroundColor Cyan
      Write-Host "   Models: $($s.models) online  |  KG: $($s.kgNodes) nodes  |  Errors: $($s.errors)" -ForegroundColor Green
    } else { Write-Host "Dashboard offline" -ForegroundColor Red }
  }

  "dashboard" { Start-Process "http://localhost:8899"; log "Dashboard opened" }

  "help" {
    Write-Host @"
SIMS1337 TERMINAL CLI — ONE-WORD COMMANDS
==========================================
CORE:       status  dashboard  help  refresh  save
MODELS:     models  ask <m> <q>  chain <m1> <m2> <q>
AGENTS:     agents  agent <name>  move <a> <q> <r>
PROPOSALS:  proposals  vote <id>  debate  deploy
NIGHT:      dream  night  owl  wizard  topo
HEX:        hex  fow  todo <hex> <task>
DATA:       data  summary  export
GAME:       play  join <name>  look  map  act <action>
"@
  }

  "models" {
    Write-Host "`nLocal SLM Models:" -ForegroundColor Cyan
    $ms = @(
      @{n="qwen2.5:0.5b";s="494M";r="Fast Responder"},
      @{n="tinyllama:1.1b";s="638M";r="Synthesizer"},
      @{n="llama3.2:1b";s="1.2B";r="Pragmatist"},
      @{n="deepseek-r1:1.5b";s="1.1B";r="Deep Thinker"},
      @{n="phi:latest";s="1.6B";r="Innovator"},
      @{n="gemma2:2b";s="1.6B";r="Guardian"},
      @{n="phi3:mini";s="2.2B";r="Logician"},
      @{n="codellama:7b";s="3.8B";r="Architect"}
    )
    foreach ($m in $ms) { Write-Host "  $($m.n.PadRight(22)) $($m.s.PadRight(8)) $($m.r)" -ForegroundColor Green }
  }

  "ask" {
    if (-not $Arg1 -or -not $Arg2) { Write-Host "Usage: sims ask <model> <question>"; return }
    log "Asking $Arg1..."
    $r = ollama $Arg1 "You are an SLM agent in SIMS1337. Answer concisely." $Arg2 150
    Write-Host "`n$Arg1 : $r" -ForegroundColor White
  }

  "chain" {
    if (-not $Arg1 -or -not $Arg2 -or -not $Arg3) { Write-Host "Usage: sims chain <m1> <m2> <question>"; return }
    log "Chain: $Arg1 -> $Arg2"
    $r1 = ollama $Arg1 "Answer concisely, max 80 chars." $Arg3 80
    Write-Host "$Arg1 : $r1" -ForegroundColor Gray
    $r2 = ollama $Arg2 "Previous model said: '$r1'. Build on this. One sentence, max 80 chars." "Previous: $r1. Your response?" 80
    Write-Host "$Arg2 : $r2" -ForegroundColor Magenta
  }

  "agents" {
    Write-Host "`nAgents on Hex Grid:" -ForegroundColor Cyan
    Write-Host "  Alpha  — Orchestrator  | deepseek-r1:1.5b  | hex (0,0)" -ForegroundColor Green
    Write-Host "  Beta   — Builder       | phi3:mini         | hex (2,-1)" -ForegroundColor Cyan
    Write-Host "  Gamma  — Analyst       | llama3.2:1b       | hex (-2,1)" -ForegroundColor Yellow
  }

  "agent" {
    if (-not $Arg1) { Write-Host "Usage: sims agent <Alpha|Beta|Gamma>"; return }
    $m = switch ($Arg1) { "Alpha" { "deepseek-r1:1.5b" } "Beta" { "phi3:mini" } "Gamma" { "llama3.2:1b" } default { "llama3.2:1b" } }
    $r = ollama $m "You are Agent $Arg1 in SIMS1337. Report your status in one sentence." "What is your status, Agent $Arg1?" 60
    Write-Host "`nAgent $Arg1 : $r" -ForegroundColor White
  }

  "proposals" {
    Write-Host "`nActive Proposals: open http://localhost:8899 for live list" -ForegroundColor Cyan
    Write-Host "Commands: sims vote <id>  |  sims debate  |  sims deploy" -ForegroundColor Yellow
  }

  "vote" {
    if (-not $Arg1) { Write-Host "Usage: sims vote <proposal-id>"; return }
    log "Voting on $Arg1 with all 8 models..."
    $models = @("qwen2.5:0.5b","tinyllama:1.1b","llama3.2:1b","deepseek-r1:1.5b","phi:latest","gemma2:2b","phi3:mini","codellama:7b")
    $yes = 0; $no = 0
    foreach ($m in $models) {
      $r = ollama $m "Vote YES or NO on proposal $Arg1. One word only." "Vote on $Arg1: YES or NO?" 5
      if ($r -match "YES") { $yes++; Write-Host "  $($m.PadRight(22)) YES" -ForegroundColor Green }
      else { $no++; Write-Host "  $($m.PadRight(22)) NO" -ForegroundColor Red }
    }
    $result = if ($yes -gt $no) { "APPROVED" } else { "REJECTED" }
    Write-Host "`n  $yes YES / $no NO  |  $result" -ForegroundColor $(if ($yes -gt $no) { "Green" } else { "Red" })
  }

  "debate" {
    $topic = if ($Arg1) { $Arg1 } else { "How should SIMS1337 evolve?" }
    log "Debate: $topic"
    $models = @("deepseek-r1:1.5b","phi3:mini","codellama:7b","llama3.2:1b","gemma2:2b","phi:latest","tinyllama:1.1b","qwen2.5:0.5b")
    foreach ($m in $models) {
      $r = ollama $m "Argue FOR or AGAINST: '$topic'. Start with FOR: or AGAINST:. Max 100 chars." "Debate: $topic. Your argument?" 100
      $color = if ($r -match "^FOR") { "Green" } else { "Red" }
      Write-Host "  $($m.PadRight(22)) $r" -ForegroundColor $color
    }
  }

  "dream" {
    log "Dream phase: 8 models generating ideas..."
    $models = @("qwen2.5:0.5b","tinyllama:1.1b","llama3.2:1b","deepseek-r1:1.5b","phi:latest","gemma2:2b","phi3:mini","codellama:7b")
    $cats = @("tool","node","backend","logic","ability","grid","tool","logic")
    for ($i=0; $i -lt 8; $i++) {
      $r = ollama $models[$i] "Dream a new $($cats[$i]) for SIMS1337. Format: 'CATEGORY: Name - description'. Max 100 chars." "Dream a $($cats[$i]) for the 61-hex grid." 100
      Write-Host "  $($models[$i].PadRight(22)) $r" -ForegroundColor Magenta
    }
  }

  "night" { sims dream; sims debate "Best next step for SIMS1337"; Write-Host "Night cycle complete" -ForegroundColor Cyan }

  "owl" {
    $topic = if ($Arg1) { $Arg1 } else { "What is the optimal topology for 8 models?" }
    log "Night Owl: $topic"
    $models = @("deepseek-r1:1.5b","codellama:7b","phi3:mini","llama3.2:1b","qwen2.5:0.5b","tinyllama:1.1b","gemma2:2b","phi:latest")
    $personas = @("Philosopher","Architect","Logician","Pragmatist","Scout","Synthesizer","Guardian","Innovator")
    $insights = @()
    for ($i=0; $i -lt 8; $i++) {
      $r = ollama $models[$i] "As the $($personas[$i]) of Night Owl Collective, one insight on: $topic. Max 80 chars." "Your insight on: $topic?" 80
      $insights += $r
      Write-Host "  $($personas[$i].PadRight(16)) $r" -ForegroundColor Cyan
    }
    $all = $insights -join "; "
    $synth = ollama "deepseek-r1:1.5b" "Synthesize 8 insights into ONE recommendation. Max 100 chars." "Insights: $all. Synthesis:" 100
    Write-Host "`n  SYNTHESIS: $synth" -ForegroundColor Yellow
  }

  "wizard" {
    $r = ollama "codellama:7b" "As Code Wizard of SIMS1337, suggest ONE code improvement. Max 100 chars." "What ONE improvement for SIMS1337?" 100
    Write-Host "`nCode Wizard: $r" -ForegroundColor Yellow
  }

  "topo" {
    $r = ollama "deepseek-r1:1.5b" "As Topologist of SIMS1337, find ONE bottleneck or suggest ONE connection. Max 100 chars." "Analyze 3-agent, 8-model topology." 100
    Write-Host "`nTopologist: $r" -ForegroundColor Yellow
  }

  "hex" {
    Write-Host "`nHex Map: 61 hexes, 4D Q/R/Z+time" -ForegroundColor Cyan
    Write-Host "  Agents: Alpha(0,0) Beta(2,-1) Gamma(-2,1)" -ForegroundColor Green
    Write-Host "  FOW: 1-hop visibility  |  TODOs: 16 pending" -ForegroundColor Gray
    Write-Host "  Live map: http://localhost:8899" -ForegroundColor Gray
  }

  "fow" {
    Write-Host "`nFog of War: 1-hop visibility around each agent" -ForegroundColor Cyan
    Write-Host "  Alpha sees 7 hexes  |  Beta sees 7  |  Gamma sees 7" -ForegroundColor Green
  }

  "data" {
    $f = "$PSScriptRoot\logs\overnight-data.json"
    if (Test-Path $f) {
      $size = (Get-Item $f).Length
      Write-Host "`nOvernight Data: $([math]::Round($size/1024,1)) KB" -ForegroundColor Cyan
    } else { Write-Host "`nNo overnight data yet" -ForegroundColor Yellow }
  }

  "summary" {
    $f = "$PSScriptRoot\logs\overnight-data.json"
    if (Test-Path $f) {
      $data = Get-Content $f | ConvertFrom-Json
      $votes = ($data | Where { $_.type -eq "vote" }).Count
      $yes = ($data | Where { $_.type -eq "vote" -and $_.vote -eq "YES" }).Count
      Write-Host "`nMORNING SUMMARY: $votes votes ($yes YES)" -ForegroundColor Cyan
      $r = ollama "deepseek-r1:1.5b" "Summarize overnight SIMS1337 data. $votes votes, $yes yes." "Summary of $votes votes" 120
      Write-Host "AI: $r" -ForegroundColor White
    } else { Write-Host "`nNo overnight data yet" -ForegroundColor Yellow }
  }

  "play" {
    Write-Host "`nSIMS1337 GAME MODE" -ForegroundColor Cyan
    Write-Host "  sims join <name>  |  sims look  |  sims map  |  sims act <action>" -ForegroundColor Green
  }

  "join" {
    $name = if ($Arg1) { $Arg1 } else { "Player" }
    Write-Host "`n$name joined at hex (0,0)! Type 'sims look'" -ForegroundColor Green
  }

  "look" {
    Write-Host "`nHex (0,0). Neighbors: N(0,-1) NE(1,-1) SE(1,0) S(0,1) SW(-1,1) NW(-1,0)" -ForegroundColor Cyan
    Write-Host "  Agent Alpha at SE(1,0)" -ForegroundColor Green
  }

  "map" {
    Write-Host "`n   / \__/ \__/ \__/ \__/ \"
    Write-Host "  / \__/ \__/A \__/ \__/ \__/"
    Write-Host "  \__/ \__/ \__/ \__/B \__/"
    Write-Host "  / \__/G \__/ \__/ \__/ \"
    Write-Host "  \__/ \__/ \__/ \__/ \__/"
    Write-Host "`n  A=Alpha B=Beta G=Gamma *=You" -ForegroundColor Gray
  }

  default {
    if (-not $Cmd) { $Cmd = "help" }
    Write-Host "Unknown: '$Cmd'. Type 'sims help'" -ForegroundColor Red
  }
}

# Save state
try {
  @{lastCommand=$Cmd; timestamp=(Get-Date -Format "o"); dashboardAlive=($null -ne (api "/api/status"))} | ConvertTo-Json | Out-File $DATA -Force
} catch {}
