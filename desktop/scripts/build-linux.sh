#!/usr/bin/env bash
# Builds Linux beta artifacts for Flight Tracker Desktop.
# Run from anywhere; requires JDK 17+ on PATH or JAVA_HOME set.
set -euo pipefail
cd "$(dirname "$0")/.."

echo "Building Linux artifacts (deb + AppImage + portable app + fat jar)..."
./gradlew --no-daemon clean createDistributable packageDeb packageAppImage fatJar

echo
echo "Done! Artifacts:"
echo "  Installer : build/compose/binaries/main/deb/"
echo "  AppImage  : build/compose/binaries/main/appimage/  (just run it)"
echo "  Portable  : build/compose/binaries/main/app/       (launch via bin/FlightTrackerDesktop)"
echo "  Fat jar   : build/libs/flight-tracker.jar          (java -jar flight-tracker.jar)"
