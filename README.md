# Echoes Beneath

> The lens remembers shape and sound, but not light.

A Minecraft Java Edition mod for Fabric and NeoForge, built on 1.21.1.

Record a place with an Echo Lens: its blocks, the creatures moving through it, and the real sounds that
played there. Come back later and stand inside that memory. The world goes near black, remembered shapes
are silhouettes, and every old sound returns at its own moment as a wave that briefly lights the surfaces
it touches.

Tune the lens at a Resonance Table. Read what a recording heard at a Sounding Table, which draws a place
out of the sound that reached it and hands it back as an Echo Chart you can set down and walk around.

## Requirements

| | Fabric | NeoForge |
|---|---|---|
| Minecraft | 1.21.1 | 1.21.1 |
| Loader | Fabric Loader 0.19.3 or newer | NeoForge 21.1.219 or newer |
| Also needed | Fabric API 0.116.15+1.21.1 | none |
| Java | 21 | 21 |

Runs on the client and on a dedicated server, and is required on both: the server owns recording and
playback, the client owns everything drawn and heard. Played through with Sodium on both: 0.6.0 on Fabric, 0.8.13-beta.2 on NeoForge. The NeoForge build of
Sodium carries the Forgified Fabric API inside it, which is what sets the NeoForge floor at 21.1.219.

## Installation

Take the jar for your loader and drop it into `mods/`.

| Loader | File |
|---|---|
| Fabric | `echoes-beneath-fabric-1.0.0.jar`, plus Fabric API |
| NeoForge | `echoes-beneath-neoforge-1.0.0.jar` |

## Building

Java 21 and nothing else; Gradle comes with the wrapper.

```bash
./gradlew build
```

That builds Fabric only. For both loaders:

```bash
./gradlew build :neoforge:build
```

The jars land in `build/libs/` and `neoforge/build/libs/`. Ignore the `-sources` jars, they are not
meant for `mods/`.

Checks:

```bash
./gradlew test
```

```bash
./gradlew runGameTest
```

Running the game:

```bash
./gradlew runClient
```

```bash
./gradlew :neoforge:runClient
```
