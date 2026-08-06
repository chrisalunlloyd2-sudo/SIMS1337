# SIMS1337 - Launch AegisSwingSphere Java GUI
# Forces GUI window to display on desktop

$javaExe = "C:\Program Files\Android\openjdk\jdk-21.0.8\bin\java.exe"
$classPath = "C:\Users\viper\local_desktop_main\build"

# Kill any existing instances
Get-Process java -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Milliseconds 500

# Launch with explicit display settings
$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = $javaExe
$psi.Arguments = "-cp `"$classPath`" -Dsun.java2d.d3d=false AegisSwingSphere"
$psi.UseShellExecute = $true
$psi.WindowStyle = [System.Diagnostics.ProcessWindowStyle]::Normal
$psi.WorkingDirectory = $classPath

$process = [System.Diagnostics.Process]::Start($psi)
Write-Host "AegisSwingSphere launched with PID: $($process.Id)"
Write-Host "Window should be visible on your desktop now."
