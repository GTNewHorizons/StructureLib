# Multiblock Projector Dismantle (work in progress)

This is an experimental source checkpoint, **not a tested, installable mod JAR**.
It has not been compiled because the build environment could not reach the
Gradle 9.4 distribution server. Do not use on a valued world yet.

## Implemented in source

- Per-projector Build/Dismantle NBT mode, default Build.
- Sneak + left-click in air to switch; localized chat warning and tooltip.
- Ctrl + right mouse continuously on a constructable controller to confirm.
- Server-side heartbeat/tick accounting, revalidation and configurable 60-tick
  default hold (20-tick minimum). Only one job per activation.
- Server-side structure capture from the real hologram structure pieces.
  Unloaded chunks, unrecognized blocks and absent drops abort safely.
- Original Build budget/interval applied to dismantle batches.
- AE2 wireless network discovery from main inventory or Baubles, via the same
  terminal/locatable/grid model used by StructureCompat; insert permission
  checked and capacity simulated before each block is removed.
- The selected controller is excluded from removal; break events are fired.
- In the rare event of a partial insert, residual drops go to player inventory
  first and then into the world as a recoverable emergency fallback.

## Known limitations and required QA

- Not compiled; there may be API mismatches or formatting violations.
- The hold protocol requires client and server to both run this modified
  StructureLib. A client can always forge input packets; the server ensures at
  least the configured number of distinct server ticks, not cryptographic proof
  of a physical key hold.
- The structure-capture path depends on controller implementations calling the
  standard StructureLib hint traversal. Unsupported controllers are rejected.
- Modular/variable-piece structures are **not certified**; do not test the
  initial build against modular controller assemblies.
- Any block breaking or tile invalidation side effects (particularly inventories
  inside GT hatches) remain the player's responsibility.
- AE2 + world changes do not share an atomic transaction. Capacity simulation
  and emergency fallback avoid common losses, but crash-consistency across a
  server shutdown in the middle of a block operation is not guaranteed.
- Multiple linked wireless terminals may select the first eligible network
  instead of a uniquely defined network.
- No runtime gameplay tests, including low TPS, packet loss, full AE2 storage,
  hatches, disconnected terminal, logout, or protection plugins, have run yet.

## Next actions

1. Compile against GTNH 2.9 with the repository's Gradle wrapper.
2. Run formatting/lint tasks and fix any API errors.
3. Test mode switching and full hold/cancellation without any world mutation.
4. Test structure capture on a small, fixed GT multiblock in a throwaway world.
5. Verify each recovered item against the original machine's breaking drops.
6. Verify storage-space refusal, partial insertion, logout and server restart.
7. Only then produce a game-test JAR and expand support to modular multiblocks.
