# Flight Tracker

Live flight tracking for **OpenSky Network** (with airplanes.live metadata), built with Kotlin Multiplatform.

- **Android app** — in `app/`
- **Desktop app (Linux / Windows)** — in `desktop/` (Compose for Desktop)

## Desktop downloads (beta)

Grab the latest beta from the **[Releases](https://github.com/DanielGaming953/flight-tracker/releases)** page:

| Platform | File | How to run |
|---|---|---|
| Linux | `.AppImage` | `chmod +x FlightTracker*.AppImage && ./FlightTracker*.AppImage` |
| Linux | `.deb` | `sudo apt install ./FlightTracker*.deb` |
| Linux | `.tar.gz` | extract, then run `FlightTrackerDesktop/bin/FlightTrackerDesktop` |
| Windows 10/11 | `.exe` | run the installer |
| Windows 10/11 | `.msi` | install it |
| Windows 10/11 | `.zip` | extract, then run `FlightTrackerDesktop\FlightTrackerDesktop.exe` |
| Any | `.jar` | `java -jar FlightTracker-*.jar` (needs Java 17+) |

> **Windows 7** is not supported: the app needs Java 17+ and the Skia rendering backend,
> neither of which runs on Windows 7 (whose last compatible Java is 8).

## Building from source

Prerequisites: JDK 17+.

### Linux

```bash
cd desktop
./scripts/build-linux.sh        # or: ./gradlew createDistributable packageDeb packageAppImage fatJar
```

Artifacts land in `desktop/build/compose/binaries/main/` and `desktop/build/libs/`.
To just run it during development:

```bash
cd desktop
./gradlew run
# or after a build:
./scripts/FlightTracker.sh
```

### Windows

On Windows 10/11 with JDK 17+, install the WiX toolset once:

```powershell
choco install wixtoolset -y
```

Then:

```powershell
cd desktop
powershell -ExecutionPolicy Bypass -File scripts\build-windows.ps1
# or: .\gradlew.bat createDistributable packageMsi packageExe fatJar
```

## CI

`.github/workflows/beta.yml` builds Linux + Windows beta artifacts on every push to `main`
and (on manual trigger) publishes them to the `beta` release. Trigger it from
**Actions → "Build beta (Linux + Windows)" → Run workflow**.

## Notes

- Settings are persisted in `~/.flight_tracker_desktop.properties`.
- Add a free OpenSky account (Settings → OpenSky account) for ~10x more daily updates.
- Keyboard: `WASD`/arrows pan, `+`/`-` zoom, `Home` resets to your startup view.
