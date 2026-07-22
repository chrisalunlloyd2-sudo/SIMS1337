@echo off
REM Player Grid Launcher - FIXED
set "JAVA_HOME=C:\Program Files\Java\jdk-17"
set "MAVEN_HOME=C:\ProgramData\chocolatey\lib\maven\apache-maven-3.9.16"
set "PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%PATH%"

cd /d C:\Users\viper\AIGEN_SYS\repos\sims-java-neo-fx

echo ========================================
echo 🎮 PLAYER GRID - 4D CITY SKYLINE
echo ========================================
echo.
echo Compiling...
call "%MAVEN_HOME%\bin\mvn.cmd" clean compile

echo.
echo Launching Player Grid...
call "%MAVEN_HOME%\bin\mvn.cmd" javafx:run -Djavafx.mainClass=com.aigen.sims.PlayerGridApp

echo.
echo Player Grid closed.
pause
