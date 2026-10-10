# SIMS1337 app launcher - heartbeat-reusable (created 2026-10-03 heartbeat)
# Fixes the "no reusable launch command" gap that caused the 09-27 -> 10-03 outage.
# Usage: powershell.exe -ExecutionPolicy Bypass -File scripts\launch_app.ps1
# NOTE: slf4j pinned to 2.0.9 (pom version; 2.0.7 jar absent in .m2 - silent gap trap)
# NOTE: sqlite-jdbc included (pom 3.45.1.0) for persistence system.
$REPO = "C:\Users\viper\AIGEN_SYS\repos\sims-java-neo-fx"
$JAVA = "C:\Program Files\Java\jdk-17\bin\javaw.exe"
$M2   = "C:\Users\viper\.m2\repository"
$JFX  = "$M2\org\openjfx"

$MP = "$JFX\javafx-base\17.0.6\javafx-base-17.0.6-win.jar;$JFX\javafx-controls\17.0.6\javafx-controls-17.0.6-win.jar;$JFX\javafx-graphics\17.0.6\javafx-graphics-17.0.6-win.jar;$JFX\javafx-fxml\17.0.6\javafx-fxml-17.0.6-win.jar"

$CP = "$REPO\target\classes" + `
      ";$M2\com\fasterxml\jackson\core\jackson-databind\2.15.2\jackson-databind-2.15.2.jar" + `
      ";$M2\com\fasterxml\jackson\core\jackson-core\2.15.2\jackson-core-2.15.2.jar" + `
      ";$M2\com\fasterxml\jackson\core\jackson-annotations\2.15.2\jackson-annotations-2.15.2.jar" + `
      ";$M2\org\apache\httpcomponents\client5\httpclient5\5.2.1\httpclient5-5.2.1.jar" + `
      ";$M2\org\apache\httpcomponents\core5\httpcore5\5.2\httpcore5-5.2.jar" + `
      ";$M2\org\apache\httpcomponents\core5\httpcore5-h2\5.2\httpcore5-h2-5.2.jar" + `
      ";$M2\org\slf4j\slf4j-api\2.0.9\slf4j-api-2.0.9.jar" + `
      ";$M2\org\java-websocket\Java-WebSocket\1.5.3\Java-WebSocket-1.5.3.jar" + `
      ";$M2\org\xerial\sqlite-jdbc\3.45.1.0\sqlite-jdbc-3.45.1.0.jar"

# JVM stability flags (cold-shot doctrine: software renderer, bounded heap)
$args = @(
    "-Dprism.order=sw",
    "-Dprism.vsync=false",
    "-XX:+UseG1GC",
    "-XX:MaxGCPauseMillis=200",
    "-Xms256m",
    "-Xmx512m",
    "--module-path", $MP,
    "--add-modules", "javafx.controls,javafx.fxml",
    "-cp", $CP,
    "com.aigen.sims.GodHandApp"
)

Start-Process -FilePath $JAVA -ArgumentList $args -WorkingDirectory $REPO -WindowStyle Hidden
Write-Output ("[LAUNCH] javaw started (PID family), boot ~4-6min. WorkingDir=" + $REPO)