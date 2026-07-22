@echo off
REM SIMS NEO 1337 - Master Launcher
REM Opens both GodHand + Player Grid GUIs

echo ========================================
echo 🎮 SIMS NEO 1337 - MASTER LAUNCHER
echo ========================================
echo.
echo Launching 2 GUIs:
echo   1. GodHand Dashboard (Agent Orchestration)
echo   2. Player Grid (4D City Skyline)
echo.
echo Please wait for both windows to open...
echo.

set "JAVA_HOME=C:\Program Files\Java\jdk-17"
set "MAVEN_HOME=C:\ProgramData\chocolatey\lib\maven\apache-maven-3.9.16"
set "PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%PATH%"

cd /d C:\Users\viper\AIGEN_SYS\repos\sims-java-neo-fx

REM Launch GodHand (in background)
start "GodHand Dashboard" cmd /k "%MAVEN_HOME%\bin\mvn.cmd" javafx:run -Djavafx.mainClass=com.aigen.sims.MainApp

timeout /t 5 /nobreak >nul

REM Launch Player Grid (in background)
start "Player Grid" cmd /k "%MAVEN_HOME%\bin\mvn.cmd" javafx:run -Djavafx.mainClass=com.aigen.sims.PlayerGridApp

echo.
echo ✅ Both GUIs launching...
echo.
pause
