# RAM, parallel work, and peer networking rollout

## Current source findings

- JVM arguments and RAM warnings already live in `launch/JvmArguments.kt` and `launch/LaunchGame.kt`; do not replace the user's selected heap size or collector without an explicit policy and compatibility tests.
- `task/TaskExecutors.kt` already owns a shared executor. Its unbounded queue means its configured maximum pool size is not a reliable back-pressure limit. Changing that global queue requires auditing all submitters first; blindly bounding it could reject downloads or move work onto the UI thread.
- `feature/turtle/MemoryPressureMonitor.kt` already samples Android memory. Its start/stop lifecycle needed synchronization and prompt interruption, now addressed in this branch.
- The current `feature/discord` classes implement Discord Rich Presence only; they are not a player-to-player transport.
- No existing WebRTC/P2P transport or signaling/TURN configuration was found in the source tree reviewed for this change.

## Rollout plan

### Phase 1 — safe RAM-monitor lifecycle

- Ensure only one monitor thread can run.
- Interrupt the sleeping worker on stop instead of waiting for the full polling interval.
- Keep monitoring diagnostic-only: do not trigger forced GC, lower the game's heap at runtime, or alter renderer settings automatically.
- Verify the Android build and existing regression checks before merging.

### Phase 2 — bounded parallel work

- Audit all call sites of the shared executor and classify UI, download, file I/O, and CPU work before changing queue semantics.
- Add a dedicated bounded executor for network/session work with explicit cancellation, lifecycle shutdown, queue limits, and observable rejection; do not run rejected work on the UI thread.
- Add deterministic tests for queue saturation, cancellation, shutdown, and thread-count limits.

### Phase 3 — player-to-player transport

- Use a maintained WebRTC implementation for ICE/STUN direct connectivity, with TURN relay fallback when direct connectivity fails. WebRTC still requires a separate signaling channel to exchange session descriptions and ICE candidates.
- Keep this transport separate from Discord Rich Presence and from Minecraft's existing game protocol.
- Before enabling a real connection, provide a signaling endpoint and a TURN service/configuration with short-lived credentials. No server endpoint or credentials are currently configured in this repository, so shipping a supposedly working P2P client without these would be misleading.
- Initially support bounded, versioned data messages and connection/status chat only. Synchronizing a Minecraft world or making one peer a game host requires a separate compatibility design and must not be claimed as supported by a generic data channel.

## Acceptance checks

- Android Gradle release build succeeds.
- Existing native/JNI regression scripts and JVM argument checks still pass.
- Memory monitor start/stop/restart does not create duplicate worker threads.
- Parallel-work tests prove bounded queueing and deterministic rejection/cancellation.
- P2P integration is not considered complete until two real Android clients connect directly, force TURN relay, exchange versioned messages, and recover from disconnects.

## Java runtime compatibility correction

- Keep the Android launcher build JDK separate from the Java runtime used by Minecraft; the existing JDK 21 CI build does not need to become JDK 25 simply because Minecraft 26.1 needs Java 25.
- Correct the Java requirement boundary for Minecraft 1.17: it requires Java 16+, so this launcher's supported Java 17 tier is the compatible choice (Java 8 is not).
- Validate profile-pinned and default runtimes before launch. If no runtime with a known Java major meets the game's requirement, stop with the existing user-facing error instead of launching with an older JVM.
- Treat downloaded runtime metadata as a post-install invariant and do not select an older fallback for a future Java major the launcher does not yet support.
- Add unit tests for 1.16.5 / 1.17 / 1.20.4 / 1.20.5 / 1.21.11 / 26.1 boundaries and Java-major compatibility.
- Java 25 support on Android still needs device validation: verify signed runtime downloads for each packaged ABI, boot vanilla 26.1+, and separately test Fabric/Forge/Quilt plus renderer paths. Minecraft/JDK compatibility does not guarantee every mod-loader and ASM dependency is Java 25 compatible.

- For future year-based Minecraft majors, trust a positive version-metadata Java requirement; if metadata is missing, stop with a clear error rather than defaulting to Java 25.
