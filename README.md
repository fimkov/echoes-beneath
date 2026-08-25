# Echoes Beneath

> The lens remembers shape and sound, but not light.

Echoes Beneath is a Minecraft Java Edition mod for Fabric. It adds a single item — the **Echo Lens** —
that records up to two minutes of a place and lets you stand inside that memory later.

Inside a memory the present world goes almost completely dark. Recorded blocks and creatures are only
black silhouettes. What makes them visible again are the **sounds that actually happened during the
recording**: each old sound replays at its original tick and position and sends an expanding wave
outward. Wherever that wave touches a surface, a bright white-cyan front briefly traces it before
fading through cyan, deep blue and violet.

## Requirements

| Component | Version |
|---|---|
| Minecraft Java Edition | 1.21.1 |
| Fabric Loader | 0.19.3 or newer |
| Fabric API | 0.116.15+1.21.1 |
| Java | 21 |
| Fabric Loom (build only) | 1.17.19 |
| Gradle (build only) | 9.5.0, via the bundled wrapper |

Mappings are the official Mojang mappings.

## Installation

1. Install Fabric Loader 0.19.3 for Minecraft 1.21.1.
2. Put Fabric API `0.116.15+1.21.1` into `mods/`.
3. Put `echoes-beneath-1.0.0.jar` into `mods/`.

The mod is required on both the client and the server. The server owns all recording and playback
logic; the client owns rendering and audio.

## Recipe

The recipe unlocks the first time you pick up a vanilla **Echo Shard**, through a recipe-unlock
advancement. Echo Shards still come from Ancient City chests — this mod does not add a new source
for them.

```
Echo Shard | Echo Shard     | Echo Shard
Echo Shard | Amethyst Shard | Echo Shard
Echo Shard | Echo Shard     | Echo Shard
```

Eight vanilla Echo Shards around one vanilla Amethyst Shard.

## Game loop

1. Craft an Echo Lens.
2. Right-click the ground. The lens leaves your hand, flies to the spot, hovers at
   about 1.4 blocks and starts recording. It gives off more particles and spins faster the closer it gets
   to full — there is no timer text anywhere.
3. For up to two minutes it records the space around it: a snapshot of the blocks, every block change,
   the movement and actions of nearby creatures, and every spatial sound that actually plays inside
   the radius.
4. Recording ends after 2400 ticks, or earlier if you right-click the hovering lens.
5. The lens flies back and the **same item stack** returns to your inventory, now carrying one more
   memory.
6. Come back to that place later holding the lens. It glints, emits rare particles and a quiet
   directional tone tells you a memory resonates here. The item tooltip names which memory it is.
7. Right-click. The lens flies to the exact original centre, the memory region forms, and the
   recording replays from tick zero.

## Irreversible memories

A fresh Echo Lens has **one** memory slot. It can be widened to three at a Resonance Table, and slots stay
irreversible either way: **Memory I**, **Memory II**, **Memory III**.

- You cannot pick a slot. Recording always takes the first free one, in order.
- More slots cost amethyst shards and experience at a Resonance Table.
- Once a slot is filled it is sealed. It cannot be cleared, overwritten, moved to another lens,
  copied or merged.
- Playback never consumes a memory. Each one can be replayed without limit.
- When every slot the lens has is full, no new recording can start:
  - English: `This Echo Lens can remember no more.`
  - Russian: `Эта эхо-линза больше не может ничего запомнить.`

There is no survival mechanism for deleting a memory. `/echo admin_clear_memory` exists only to
recover from corrupted test data and requires permission level 3.

## Automatic memory selection

You never choose which memory plays. When you hold a lens with at least one memory, the mod checks
your dimension and position against every stored region:

- If you are inside exactly one region, that memory is used.
- If several regions overlap, the one with the **nearest centre** wins.
- If two centres are equally close, the **lower Memory number** wins.
- If nothing matches: `No memory resonates with this place.` /
  `Ни одно воспоминание не отзывается в этом месте.`

Progress and resonance are shown the way vanilla shows things — through particles, sound, item glint
and the tooltip. The mod never prints a clock or a countdown to the HUD.

## Old sounds

This is the core mechanic. During a recording the mod captures the spatial sounds the server actually
plays inside the radius — footsteps, doors, chests, levers, buttons, block breaking and placing, hits,
damage, shots, explosions, sculk events, mob sounds. Music, records, interface sounds and non-positional
ambience are never recorded.

For each sound the mod stores its identifier, the tick relative to the start, the relative position,
volume, pitch and sound category.

At playback the client replays exactly those stored events — never a fresh random sound. If a zombie
made a sound at tick 120 at relative `(4, 1, -8)`, then at playback tick 120 that same sound plays at
`center + (4, 1, -8)` with the recorded volume and pitch. Each sound also gets two quieter, slightly
lower-pitched repeats a few ticks later, which reads as memory rather than as a cascade of echoes.

Every sound spawns a wave. The wave radius comes from the volume, the sound category and the config,
and expands at a finite speed (`waveRadius = elapsedTicks * waveSpeed`), so a memory reveals itself
gradually instead of lighting up all at once.

## Hostile mobs temporarily vanish

When playback starts, the **real, present-day** hostile mobs inside the radius are suppressed. They are
not deleted and not respawned. The mod stores their position, rotation, velocity, health, effects, fire
and freeze ticks, target and their original invisible/silent/invulnerable/no-AI flags, then makes them
invisible, silent, invulnerable and inert, and removes their collision and targetability.

When playback ends, every stored value is restored. Recovery also runs on server stop, server start,
dimension unload, playback failure, owner disconnect and lens loss, and `/echo recover_suppressed_mobs`
forces it manually. The suppression list is written to the world save, so a crash mid-playback cannot
leave a mob permanently invisible.

Which mobs are affected is controlled by the `echoes_beneath:temporarily_suppressed` entity type tag.

## The Resonance Table

Found only in a Resonance Vault — it has no recipe. Put a lens in the upper slot and amethyst shards in
the lower one. Each track has a meter and a pair of buttons: filled segments are what the lens already
carries, hollow ones are what you have staged. Nothing is spent while you plan, and a staged step can be
taken back — but a segment the lens already owns cannot, because upgrades are permanent.

The dial shows the resulting lens; hover it to read the tuning it would have. The price of everything
staged sits beside it, in red when you cannot pay. **Taking the lens off the dial is what applies the
plan and charges the levels and shards.**

| Track | Cost per step | Range | Effect |
|---|---|---|---|
| Memory | 12 levels + 8 amethyst shards | 1 → 3 | one more irreversible memory slot |
| Radius | 6 levels | 16 → 64 blocks | +8 blocks of recorded radius |
| Hearing | 9 levels | 0 → 4 | quieter sounds still make it into the recording |
| Clarity | 20 levels | 0 → 4 | lifts the darkness during playback |

**Clarity is the expensive one for a reason.** A memory records the whole cylinder, including what is
buried and what sits behind walls. At low clarity the region is almost black and you only make out what a
passing wave happens to trace. Each level of clarity lifts that darkness, so a fully tuned lens turns a
recording into a survey of everything within its radius — caves, voids, buried rooms. Place a lens, make
some noise, walk away, and read the ground on the replay.

## Controls

| Action | Input |
|---|---|
| Start recording | Right-click the top face of a block with a lens that has a free slot |
| End recording early | Right-click the hovering lens (owner only) |
| Start playback | Right-click while standing inside a matching region |
| End playback early | Right-click the hovering lens (owner only) |

There is no GUI, and by design there is nothing to configure per-lens.

While a lens is deployed it gives off sculk particles, and both the particle count and its spin speed rise
as the recording or the playback fills. There is no dial and no counter — the same way vanilla never puts
a timer on a world object.

## Configuration

Server-side, written to `config/echoes_beneath.properties` on first launch. Values outside the
supported range are clamped and unreadable values fall back to the default.

| Key | Default | Range |
|---|---|---|
| `recordingRadius` | 50 | 8–64 |
| `recordingVerticalRadius` | 32 | 4–64 |
| `maxRecordingSeconds` | 120 | 5–120 |
| `entitySampleRate` | 5 | 1–20 |
| `maxRecordedEntities` | 128 | 1–512 |
| `maxSoundEvents` | 4096 | 64–32768 |
| `maxConcurrentSoundWaves` | 64 | 4–256 |
| `allowOverlappingPlayback` | false | boolean |
| `suppressHostileMobs` | true | boolean |
| `disableBlockInteractionDuringPlayback` | true | boolean |

## Development commands

All require permission level 2, except `admin_clear_memory` which requires level 3.

```
/echo give
/echo status
/echo start_recording
/echo stop_recording
/echo play_nearby
/echo stop_playback
/echo debug_bounds
/echo debug_sounds
/echo debug_entities
/echo locate_structure <echo_listening_post|collapsed_resonance_shrine|surface_echo_ruin>
/echo recover_suppressed_mobs
/echo admin_clear_memory <memory>
```

`admin_clear_memory` is a recovery and debugging tool for damaged test data, not a survival feature.

## Structures

Three small vanilla-styled structures hint at the mechanic. None of them is a reliable source of Echo
Shards; Ancient City remains the intended source.

- **Echo Listening Post** — a small deepslate room in the deep dark with a ruined lens platform, a
  themed chest, and a floor pattern of eight sculk blocks around one amethyst block that mirrors the
  recipe.
- **Collapsed Resonance Shrine** — a rare broken circular shrine in the deep dark, dripstone caves and
  lush caves, with damaged arches and a central lens point.
- **Surface Echo Ruin** — a very rare small surface ruin in common overworld biomes, carrying a compass,
  a map and a hint that the source of the echo lies below.
- **Resonance Vault** — a sealed deep dark chamber in the ancient city style: deepslate brick walls,
  soul lanterns on chains, sculk veins across the floor, and a **Resonance Table** standing clear on a
  raised platform in the middle. It generates at the same depth the ancient cities sit at, so it reads as
  another room of that world rather than something dropped above it. The table has no recipe, so this room
  is the only place to find one.

## Building

```bash
./gradlew clean build
```

The mod jar lands in `build/libs/echoes-beneath-1.0.0.jar`.

Other useful tasks:

```bash
./gradlew test
```

```bash
./gradlew runGameTest
```

```bash
./gradlew runDatagen
```

```bash
./gradlew runClient
```

```bash
./gradlew runServer
```

## Dedicated server

The mod runs on a dedicated server without extra setup.

1. Install Fabric Loader 0.19.3 for 1.21.1 on the server.
2. Place Fabric API and `echoes-beneath-1.0.0.jar` in the server's `mods/` folder.
3. Start the server once to generate `config/echoes_beneath.properties`, adjust it if needed, restart.
4. Every connecting client needs the same two mods.

The server decides everything: recording, the timeline, slot allocation, sound events, mob suppression,
playback start and end, and the return of the lens. Clients receive the memory in chunked packets when
they enter the region and then only a periodic tick sync, so all players inside a region see the same
moment of the same memory. Payload sizes are bounded and validated on decode, and the client never
sends memory data to the server.

Memories are stored under `<world>/echoes_beneath/<uuid>.echo` as compressed NBT, so they survive
restarts of both single-player worlds and dedicated servers.

## Known limitations

- Playback restores the **visual** state only. Redstone logic, block entity contents and container
  inventories are never recorded or replayed, by design.
- Container contents, book text and command block commands are deliberately not stored, so no memory
  can be used to duplicate items.
- Regions of two active playbacks may not overlap while `allowOverlappingPlayback` is false. The second
  attempt is refused with a message.
- The Surface Echo Ruin uses common overworld surface biomes rather than "above a deep dark region",
  because data-driven worldgen cannot see a cave biome from the surface. It uses no terrain adaptation,
  so on steep slopes part of its floor can sit proud of the ground rather than being cut into it.
- Block changes recorded during the first ~1 second of a recording, in a chunk section that has not yet
  been snapshotted, use a pre-change override; this is exact for the recorded block but the surrounding
  section is captured slightly later.
- Echo entities reuse the vanilla model of their type. Variant data beyond age and equipment (for
  example specific villager professions) is not reproduced.
- The darkening and fog effects apply to the whole client view while the camera is inside a region.
  Other rendering mods that also override the light texture or fog may conflict.
- The darkening leaves the full-bright lightmap texel alone so that the HUD, the deployed lens and every
  particle stay readable. A world block lit to both full block light and full sky light therefore keeps
  its brightness; this only happens next to a light source under open sky.

## Trailer scenario

A short capture that shows the whole idea in about 60 seconds.

1. **0:00–0:06** — Ancient City, torchlight, the player picks up an Echo Shard. The recipe toast pops.
2. **0:06–0:12** — Crafting table, the 3×3 of eight Echo Shards around one Amethyst Shard closes into
   the lens. Hold on the item for a beat.
3. **0:12–0:20** — A small built room. The player right-clicks the floor; the lens flies out, rises and
   begins to turn, a ring of teal particles opening around it.
4. **0:20–0:32** — Ordinary life inside the radius: open a door, walk a lap, a zombie groans in the
   corner, place a block, break it. Nothing looks special yet.
5. **0:32–0:36** — The player right-clicks the lens. Particles pull inward, it chimes and flies back
   to the hand. Tooltip: `Memories: 1 / 3`.
6. **0:36–0:42** — Cut. The room has been changed: the wall is gone, the door replaced. The player
   walks in holding the lens; it glints, sparks drift off it and the quiet resonance tone plays.
7. **0:42–0:50** — Right-click. The lens flies to the old centre. Light drains out of the frame, the
   sky disappears, everything past the boundary goes black. Only faint grey outlines remain.
8. **0:50–1:00** — The old door opens. Its recorded sound plays, and a wave rolls outward, tracing the
   wall that no longer exists in white-cyan fading to violet. The old zombie groans; the wave picks its
   silhouette out of the dark. The waves overlap, the room is briefly legible, then it all dissolves,
   light returns, and the lens comes back to the hand.

Hold the last frame on the line: *The lens remembers shape and sound, but not light.*

## License

MIT. See `LICENSE`.
