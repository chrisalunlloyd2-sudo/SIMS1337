@echo off
set PID=16448
set JAVA_HOME=C:\Program Files\Java\jdk-17
set OUT=C:\Users\viper\AIGEN_SYS\repos\sims-java-neo-fx\target\classes
set M2=C:\Users\viper\.m2\repository
set JFX=%M2%\org\openjfx
set MP=%JFX%\javafx-base\17.0.6\javafx-base-17.0.6-win.jar;%JFX%\javafx-controls\17.0.6\javafx-controls-17.0.6-win.jar;%JFX%\javafx-graphics\17.0.6\javafx-graphics-17.0.6-win.jar;%JFX%\javafx-fxml\17.0.6\javafx-fxml-17.0.6-win.jar
set CP=%OUT%;%M2%\com\fasterxml\jackson\core\jackson-databind\2.15.2\jackson-databind-2.15.2.jar;%M2%\com\fasterxml\jackson\core\jackson-core\2.15.2\jackson-core-2.15.2.jar;%M2%\com\fasterxml\jackson\core\jackson-annotations\2.15.2\jackson-annotations-2.15.2.jar;%M2%\org\apache\httpcomponents\client5\httpclient5\5.2.1\httpclient5-5.2.1.jar;%M2%\org\apache\httpcomponents\core5\httpcore5\5.2\httpcore5-5.2.jar;%M2%\org\apache\httpcomponents\core5\httpcore5-h2\5.2\httpcore5-h2-5.2.jar;%M2%\org\slf4j\slf4j-api\2.0.7\slf4j-api-2.0.7.jar;%M2%\org\java-websocket\Java-WebSocket\1.5.3\Java-WebSocket-1.5.3.jar
:loop
timeout /t 30 /nobreak >nul
tasklist /FI "PID eq %PID%" 2>nul | find "%PID%" >nul
if errorlevel 1 (
  echo [%date% %time%] SIMS1337 crashed — restarting... >> C:\Users\viper\AIGEN_SYS\repos\sims-java-neo-fx\logs\guardian.log
  start "SIMS1337" "%JAVA_HOME%\bin\javaw" --module-path "%MP%" --add-modules javafx.controls,javafx.fxml -cp "%CP%" com.aigen.sims.GodHandApp
  timeout /t 15 /nobreak >nul
  for /f "tokens=2" %%a in ('tasklist /FI "IMAGENAME eq java.exe" /FO LIST ^| find "PID:"') do set PID=%%a
  echo [%date% %time%] Restarted with PID %PID% >> C:\Users\viper\AIGEN_SYS\repos\sims-java-neo-fx\logs\guardian.log
)
goto loop
