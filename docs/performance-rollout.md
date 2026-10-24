# RAM, parallel work, and Java runtime compatibility

## Implemented in this branch

- `MemoryPressureMonitor` samples Android memory for diagnostics only. Its lifecycle is synchronized, stop interrupts the sleeping worker, and a generation token prevents a rapid stop/start from leaving the monitor stopped or allowing an older worker to clear the newer worker's state.
- `TaskExecutors` caps active worker threads to the available CPU count (minimum one, maximum four) and allows idle workers to time out. Shared queue semantics remain unchanged until all callers can be audited for safe cancellation and rejection handling.
- Java runtime selection validates profile-pinned and default runtimes against the target Minecraft Java requirement. Unknown or too-old runtimes are not launched.
- The auto-installer verifies the Java major after installation and only returns a fallback runtime if its reported Java major satisfies the requirement.
- Java requirement boundary tests cover Minecraft 1.16.5, 1.17, 1.20.4, 1.20.5, 1.21.11, 26.1, and unknown future majors.

## Remaining safe performance work

- Audit every shared-executor submitter before introducing a bounded queue. The migration must explicitly handle cancellation, queue saturation, and rejection; rejected work must not run on the UI thread.
- Add deterministic executor tests for queue saturation, cancellation, shutdown, and thread-count limits before migrating download/session call sites.
- Measure memory use, GC pauses, frame pacing, and launch reliability on representative Android devices before changing heap defaults or collectors.
- Do not force GC, change the game's heap while it is running, or switch renderer settings automatically in response to memory warnings.

## Java 25 validation limits

- Keep the Android launcher build JDK separate from the Java runtime used by Minecraft. CI can use JDK 21 while the launcher installs Java 25 for Minecraft 26.1+.
- Minecraft 1.17 uses the launcher's Java 17 tier; 1.20.5–1.21.11 uses Java 21; 26.1+ uses Java 25.
- CI build success does not prove that the Java 25 package exists for every ABI or that vanilla 26.1+, each mod loader, and each renderer works on-device. Those require real-device validation.

## Out of scope

- Player-to-player networking and relay transport are excluded from this change.
