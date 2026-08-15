#!/usr/bin/env bash
# Runs Flight Tracker Desktop from the fat jar.
# Requires Java 17 or newer (https://adoptium.net).
# Usage: ./scripts/FlightTracker.sh
set -euo pipefail
cd "$(dirname "$0")/.."

JAR="build/libs/flight-tracker.jar"
if [[ ! -f "$JAR" ]]; then
    echo "flight-tracker.jar not found. Building it first..."
    ./gradlew --no-daemon fatJar
fi

exec java -jar "$JAR" "$@"
