# Echoes Beneath

> The lens remembers shape and sound, but not light.

A Minecraft Java Edition mod for Fabric and NeoForge, built on 26.2.

Record a place with an Echo Lens: its blocks, the creatures moving through it, and the real sounds that
played there. Come back later and stand inside that memory. The world goes near black, remembered shapes
are silhouettes, and every old sound returns at its own moment as a wave that briefly lights the surfaces
it touches.

Tune the lens at a Resonance Table. At a Sounding Table, choose a memory, trim its start and end,
adjust the volume from 0 to 100%, and listen before extracting an Echo Record. Put the record in a vanilla
jukebox to play the recorded sounds. Returning it to a lens restores the complete original memory.

Sneak-use a recorded lens on a Sounding Table to replay all its memories there in slot order,
even if they were recorded elsewhere or in another dimension. The lens returns when the last
memory ends; interact with the floating lens to stop early. Use the table normally to open its sound editor.
Note blocks and jukebox starts and stops are recorded along with other world sounds.

## Server configuration

Settings are saved in `config/echoes_beneath.properties` on first launch. Restart the server
(or the game in singleplayer) after editing them.

Each upgrade track has three settings. Replace `vision` below with `memory_slot`, `radius`,
or `sensitivity` to configure another track:

```properties
upgrades.vision.maxSteps=0
upgrades.vision.experiencePerStep=20
upgrades.vision.shardsPerStep=5
```

A maximum of `0` disables that track. Vision and sensitivity support 0–4 steps, radius 0–6
(16 blocks plus 8 per step), and memory slots 0–2 (one base slot plus one per step).
Experience prices support 0–1000 levels per step, and shard prices 0–64. Prices apply only
when going beyond a lens's previously purchased peak. Defaults keep the original upgrade limits and prices.
Limits also apply to existing lenses, and the vision limit applies when playing old memories.
Lowering memory capacity preserves sealed memories; it prevents new recordings above the limit.

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
| Fabric | `echoes-beneath-fabric-26.2-1.2.0.jar`, plus Fabric API |
| NeoForge | `echoes-beneath-neoforge-26.2-1.2.0.jar` |

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
