@echo off
REM GodHand GUI Launcher - FIXED
REM Opens the GodHand Agent Orchestration Dashboard

echo ========================================
echo 🎮 GODHAND GUI LAUNCHER
echo ========================================

REM Set Java paths CORRECTLY
set "JAVA_HOME=C:\Program Files\Java\jdk-17"
set "MAVEN_HOME=C:\ProgramData\chocolatey\lib\maven\apache-maven-3.9.16"
set "PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%PATH%"

echo JAVA_HOME=%JAVA_HOME%
echo MAVEN_HOME=%MAVEN_HOME%
echo.
echo Launching GodHand GUI...
echo.

cd /d C:\Users\viper\AIGEN_SYS\repos\sims-java-neo-fx

REM Run JavaFX app with correct JAVA_HOME
set JAVA_HOME=C:\Program Files\Java\jdk-17
"%MAVEN_HOME%\bin\mvn.cmd" javafx:run

echo.
echo GUI closed.
pause
