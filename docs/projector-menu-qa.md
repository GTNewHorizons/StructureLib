# Projector settings acceptance checks

Related issue: https://github.com/GTNewHorizons/GT-New-Horizons-Modpack/issues/26806

Before implementation: the current GUI has only raw key/value editing and writes changes on close; the following new interactions are absent.

Run in a disposable client/world and on a dedicated server, with GT5 registrations and NEI installed:

1. Open the projector. Search settings, including one with no saved override. All registered channels and saved custom channels are discoverable; configured channels sort first.
2. Select coils, search Kanthal and choose its icon/name. The overview shows the registered item name. Repeat with glass and another registered channel.
3. Cancel, Escape and inventory-close must discard changes. Reopen: original settings remain. Repeat with Apply: reopen and server construction use the saved value.
4. Reset one override: Default is shown, not a guessed block. Reset all requires a second confirmation. Cancel after reset all preserves the original projector.
5. Advanced: create a custom lowercase channel, edit a numeric value, use +/- at both bounds. Blank, zero, negative, nonnumeric and >268435455 inputs cannot be applied and do not crash.
6. Drag a registered item from NEI. It stages its mapping; ambiguous mappings require choosing the channel. Cancel still discards. Real inventory items must not be consumed.
7. At GUI scales yielding 320x240 and larger, test long translated names, empty searches, long descriptions, scrolling, Tab/Shift-Tab and arrow keys. Take screenshots of overview, item selection and Advanced.
8. Verify unrelated projector NBT is unchanged after Apply; no packet is sent on Cancel. Check reopening after server synchronization.

Automated tests cover draft isolation, reset semantics, numeric boundaries, snapshots and channel ordering. In-game checks are separately required; a successful build alone is not visual or multiplayer evidence.

## Development-client verification (2026-09-14)

The new screen was exercised in a real Minecraft 1.7.10 development client with temporary, explicitly named vanilla-item fixtures. Verified named item selection; keyboard selection (including the last option); Apply, Cancel and Escape; reset confirmation; invalid/blank numeric editing; unrelated NBT preservation; integrated-server receipt of the Apply packet; and ambiguous NEI callback choices. Screenshots were inspected at larger GUI sizes and at 320x240, including German labels. The temporary harness was excluded from the final artifact.

The screenshot `projector-menu-fixtures.png` uses QA fixture blocks, not actual GTNH coil textures. Full GTNH 2.9 beta 3 registrations, dedicated-server construction and physical NEI dragging end to end remain integration checks for the draft PR.
