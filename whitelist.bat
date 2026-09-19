@echo off
REM SIMS1337 Windows Defender Whitelist — Run as Administrator
echo Adding Windows Defender exclusions for SIMS1337 + Ollama...

powershell -Command "Add-MpPreference -ExclusionPath 'C:\Users\viper\AIGEN_SYS\repos\sims-java-neo-fx'" 2>&1
powershell -Command "Add-MpPreference -ExclusionPath 'C:\Users\viper\.ollama'" 2>&1
powershell -Command "Add-MpPreference -ExclusionPath 'C:\Program Files\Java\jdk-17'" 2>&1
powershell -Command "Add-MpPreference -ExclusionProcess 'javaw.exe'" 2>&1
powershell -Command "Add-MpPreference -ExclusionProcess 'ollama.exe'" 2>&1
powershell -Command "Add-MpPreference -ExclusionProcess 'ollama_llama_server.exe'" 2>&1
powershell -Command "Add-MpPreference -ExclusionProcess 'java.exe'" 2>&1

echo.
echo === Current Exclusions ===
powershell -Command "Get-MpPreference | Select-Object ExclusionPath, ExclusionProcess" 2>&1
echo.
echo Done. SIMS1337 + Ollama whitelisted.
pause
