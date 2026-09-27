@echo off
REM Heartbeat relaunch of SIMS1337 GodHand app (uses compiled classes from 2026-09-26 14:21)
setlocal
set NAME=SIMS1337_GodHand
set REPO=C:\Users\viper\AIGEN_SYS\repos\sims-java-neo-fx
set JAVA=C:\Program Files\Java\jdk-17\bin\javaw
set M2=C:\Users\viper\.m2\repository
set JFX=%M2%\org\openjfx

set MP=%JFX%\javafx-base\17.0.6\javafx-base-17.0.6-win.jar;%JFX%\javafx-controls\17.0.6\javafx-controls-17.0.6-win.jar;%JFX%\javafx-graphics\17.0.6\javafx-graphics-17.0.6-win.jar;%JFX%\javafx-fxml\17.0.6\javafx-fxml-17.0.6-win.jar
set CP=%REPO%\target\classes;%M2%\com\fasterxml\jackson\core\jackson-databind\2.15.2\jackson-databind-2.15.2.jar;%M2%\com\fasterxml\jackson\core\jackson-core\2.15.2\jackson-core-2.15.2.jar;%M2%\com\fasterxml\jackson\core\jackson-annotations\2.15.2\jackson-annotations-2.15.2.jar;%M2%\org\apache\httpcomponents\client5\httpclient5\5.2.1\httpclient5-5.2.1.jar;%M2%\org\apache\httpcomponents\core5\httpcore5\5.2\httpcore5-5.2.jar;%M2%\org\apache\httpcomponents\core5\httpcore5-h2\5.2\httpcore5-h2-5.2.jar;%M2%\org\slf4j\slf4j-api\2.0.9\slf4j-api-2.0.9.jar;%M2%\org\java-websocket\Java-WebSocket\1.5.3\Java-WebSocket-1.5.3.jar

set JVM_FLAGS=-Dprism.order=sw -Djavafx.allowjs=true -Dprism.vsync=false -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:GCTimeRatio=9 -XX:+DisableExplicitGC -Xms256m -Xmx512m

"%JAVA%" %JVM_FLAGS% --module-path "%MP%" --add-modules javafx.controls,javafx.fxml -cp "%CP%" com.aigen.sims.GodHandApp
endlocal