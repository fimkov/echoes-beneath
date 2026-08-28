# Echoes Beneath

> The lens remembers shape and sound, but not light.

A Minecraft Java Edition mod for Fabric and NeoForge, built on 26.2.

Record a place with an Echo Lens: its blocks, the creatures moving through it, and the real sounds that
played there. Come back later and stand inside that memory. The world goes near black, remembered shapes
are silhouettes, and every old sound returns at its own moment as a wave that briefly lights the surfaces
it touches.

Tune the lens at a Resonance Table. Read what a recording heard at a Sounding Table, which draws a place
out of the sound that reached it and hands it back as an Echo Chart you can set down and walk around.

## Requirements

| | Fabric | NeoForge |
|---|---|---|
| Minecraft | 26.2 | 26.2 |
| Loader | Fabric Loader 0.19.3 or newer | NeoForge 26.2.0.69 or newer |
| Also needed | Fabric API | none |
| Java | 25 | 25 |

Runs on the client and on a dedicated server, and is required on both: the server owns recording and
playback, the client owns everything drawn and heard.

## Installation

Take the jar for your loader and drop it into `mods/`.

| Loader | File |
|---|---|
| Fabric | `echoes-beneath-fabric-1.1.0.jar`, plus Fabric API |
| NeoForge | `echoes-beneath-neoforge-1.1.0.jar` |

## Building

Java 25 and nothing else; Gradle comes with the wrapper.

```bash
./gradlew build
```

That builds both loaders, since an unqualified task name runs in every project. The jars land in `build/libs/` and `neoforge/build/libs/`. Ignore the `-sources` jars, they are not
meant for `mods/`.

Checks:

```bash
./gradlew test
```

Running the game. Keep the colon: without it the task runs in every project and
two clients start at once.

```bash
./gradlew :runClient
```

```bash
./gradlew :neoforge:runClient
```
