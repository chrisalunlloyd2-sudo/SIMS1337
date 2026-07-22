@echo off
setlocal

REM Set JAVA_HOME before anything else
set "JAVA_HOME=C:\Program Files\Java\jdk-17"

REM Now run Maven with JAVA_HOME properly set
cd /d C:\Users\viper\AIGEN_SYS\repos\sims-java-neo-fx

REM Execute Maven JavaFX plugin
call C:\ProgramData\chocolatey\lib\maven\apache-maven-3.9.16\bin\mvn.cmd javafx:run

pause
