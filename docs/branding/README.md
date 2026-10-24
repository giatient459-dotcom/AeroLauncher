# AeroLauncher brand assets

Primary palette:
- Cyan: `#00D1FF`
- Blue: `#0066FF`
- Midnight navy: `#080F20`
- Off-white: `#F4F7FF`

## Assets
- `aerolauncher-logo.svg`: scalable logo artwork.
- `aerolauncher-wordmark.svg`: horizontal wordmark for documentation and websites.
- Android adaptive launcher icon foreground: `TurtleLauncher/src/main/res/mipmap-anydpi-v26/ic_launcher_foreground.xml`.
- Android adaptive icon background: `TurtleLauncher/src/main/res/drawable/ic_launcher_background.xml`.
- Startup window background: `TurtleLauncher/src/main/res/drawable/aerolauncher_splash.xml`.

The Android resource identifiers and application ID are intentionally retained for upgrade compatibility. The splash drawable is used as the app theme's window background, so it appears while the first screen is being created; it is not an animated, timed splash activity.
