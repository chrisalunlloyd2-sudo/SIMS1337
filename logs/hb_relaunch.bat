@echo off
REM Heartbeat relaunch wrapper - invoked via wmic process call create
cd /d C:\Users\viper\AIGEN_SYS\repos\sims-java-neo-fx
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\launch_app.ps1