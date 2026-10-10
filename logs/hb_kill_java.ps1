$jp = Get-CimInstance Win32_Process -Filter "Name='javaw.exe' OR Name='java.exe'"
foreach ($p in $jp) {
  Write-Output ("KILLING PID=" + $p.ProcessId + " NAME=" + $p.Name)
  Stop-Process -Id $p.ProcessId -Force -ErrorAction SilentlyContinue
}
Start-Sleep -Seconds 2
$after = Get-CimInstance Win32_Process -Filter "Name='javaw.exe' OR Name='java.exe'"
if (-not $after) { Write-Output "ALL_JAVA_CLEARED" } else { Write-Output ("STILL_RUNNING: " + ($after | ForEach-Object { $_.ProcessId }) -join ',' ) }