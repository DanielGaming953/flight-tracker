# Builds Windows beta artifacts for Flight Tracker Desktop.
# Run on Windows 10/11 with JDK 17+ installed (JAVA_HOME set).
# The .exe and .msi installers need the WiX toolset:
#   choco install wixtoolset -y
Write-Host "Building Windows artifacts (exe + msi + portable app + fat jar)..."
.\gradlew.bat --no-daemon clean createDistributable packageMsi packageExe fatJar

Write-Host ""
Write-Host "Done! Artifacts:"
Write-Host "  Installer : build\compose\binaries\main\exe\"
Write-Host "  MSI       : build\compose\binaries\main\msi\"
Write-Host "  Portable  : build\compose\binaries\main\app\  (launch via FlightTrackerDesktop.exe)"
Write-Host "  Fat jar   : build\libs\flight-tracker.jar      (java -jar flight-tracker.jar)"
