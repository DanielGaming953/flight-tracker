@echo off
rem Runs Flight Tracker Desktop from the fat jar.
rem Requires Java 17 or newer (https://adoptium.net).
setlocal
cd /d "%~dp0\.."

set "JAR=build\libs\flight-tracker.jar"
if not exist "%JAR%" (
    echo flight-tracker.jar not found. Building it first...
    call gradlew.bat --no-daemon fatJar
    if errorlevel 1 exit /b 1
)

java -jar "%JAR%" %*
