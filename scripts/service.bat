@echo off
REM PHASE 14: SIMS1337 Windows Service Wrapper
REM Run as Administrator to install as a Windows scheduled task
REM that auto-starts on boot and restarts on crash.

set NAME=SIMS1337_GodHand
set REPO=C:\Users\viper\AIGEN_SYS\repos\sims-java-neo-fx
set JAVA=C:\Program Files\Java\jdk-17\bin\javaw
set LOGS=%REPO%\logs
set M2=C:\Users\viper\.m2\repository
set JFX=%M2%\org\openjfx

REM Build classpath
set MP=%JFX%\javafx-base\17.0.6\javafx-base-17.0.6-win.jar;%JFX%\javafx-controls\17.0.6\javafx-controls-17.0.6-win.jar;%JFX%\javafx-graphics\17.0.6\javafx-graphics-17.0.6-win.jar;%JFX%\javafx-fxml\17.0.6\javafx-fxml-17.0.6-win.jar
set CP=%REPO%\target\classes;%M2%\com\fasterxml\jackson\core\jackson-databind\2.15.2\jackson-databind-2.15.2.jar;%M2%\com\fasterxml\jackson\core\jackson-core\2.15.2\jackson-core-2.15.2.jar;%M2%\com\fasterxml\jackson\core\jackson-annotations\2.15.2\jackson-annotations-2.15.2.jar;%M2%\org\apache\httpcomponents\client5\httpclient5\5.2.1\httpclient5-5.2.1.jar;%M2%\org\apache\httpcomponents\core5\httpcore5\5.2\httpcore5-5.2.jar;%M2%\org\apache\httpcomponents\core5\httpcore5-h2\5.2\httpcore5-h2-5.2.jar;%M2%\org\slf4j\slf4j-api\2.0.7\slf4j-api-2.0.7.jar;%M2%\org\java-websocket\Java-WebSocket\1.5.3\Java-WebSocket-1.5.3.jar

REM JVM stability flags
set JVM_FLAGS=-Dprism.order=sw -Djavafx.allowjs=true -Dprism.vsync=false -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:GCTimeRatio=9 -XX:+DisableExplicitGC -Xms256m -Xmx512m

if "%1"=="install" goto :install
if "%1"=="remove" goto :remove
if "%1"=="start" goto :start
if "%1"=="stop" goto :stop
if "%1"=="status" goto :status
goto :usage

:install
echo Installing %NAME% as Windows scheduled task (auto-start on boot)...
schtasks /create /tn "%NAME%" /tr "\"%JAVA%\" %JVM_FLAGS% --module-path \"%MP%\" --add-modules javafx.controls,javafx.fxml -cp \"%CP%\" com.aigen.sims.GodHandApp" /sc onstart /ru SYSTEM /rl highest /f
if %ERRORLEVEL%==0 (
    echo [OK] %NAME% installed. Will auto-start on boot.
    schtasks /run /tn "%NAME%"
) else (
    echo [FAIL] Could not install task. Run as Administrator.
)
goto :eof

:remove
echo Removing %NAME%...
schtasks /delete /tn "%NAME%" /f
taskkill /F /IM javaw.exe 2>nul
echo [OK] Removed.
goto :eof

:start
echo Starting %NAME%...
schtasks /run /tn "%NAME%"
goto :eof

:stop
echo Stopping %NAME%...
taskkill /F /IM javaw.exe 2>nul
echo [OK] Stopped.
goto :eof

:status
tasklist /FI "IMAGENAME eq javaw.exe" 2>nul | findstr javaw
if %ERRORLEVEL%==0 (echo [RUNNING]) else (echo [STOPPED])
goto :eof

:usage
echo SIMS1337 Service Wrapper
echo Usage: %0 [install^|remove^|start^|stop^|status]
echo   install  - Install as auto-start Windows scheduled task (run as Admin)
echo   remove   - Remove the scheduled task
echo   start    - Start the service
echo   stop     - Stop the service
echo   status   - Check if running
goto :eof
