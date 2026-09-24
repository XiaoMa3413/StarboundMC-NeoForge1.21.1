# Ship wall locker

## Model and materials

The sealed whitebox was approved before texturing. All 43 approved meshes retain identical vertices and face topology. Two flat display planes were added; neither is a physical padlock.

- Titanium-gray case, graphite doors, metal runners and subdued panel markings.
- Cyan display, projector and internal diffuser use emissive materials without adding world light.
- The amber lock graphic turns within a flat display plane. Its brackets and underline are a separate fixed layer: their position and width do not follow the icon.
- Open: hologram fades away, doors retreat into the housing and slide sideways. Close reverses the sequence.
- Blockbench includes idle, open, close and demonstration clips. Runtime evaluates a smooth door pose within the same travel limits.

## Runtime and compatibility

- Existing block/item/entity IDs and the original facing-only block state are retained.
- The shipped implementation is Stage2Blocks.ShipCrate; the legacy ShipCrateBlock is excluded from compilation.
- Chassis is a static NeoForge OBJ model. Left and right doors are standalone baked models rendered by ShipLockerRenderer.
- The complete item model contains the closed doors and both flat hologram layers.
- Selection/collision is a thin cabinet shape rotated for all four directions.
- Placement on a wall follows the clicked wall face. Top/bottom placement retains the player-facing fallback.
- Wall support is not made mandatory: existing crates in saves, ship templates and rocky-moon structures must remain in place.
- Server menu lifecycle tracks unique viewers. First viewer opens the doors; the last closes them. Periodic reconciliation removes stale viewers after disconnection or changing menus/dimensions.
- Door state uses a lightweight block-entity update packet and chunk update tag. It is not persisted or added to the block state, keeping strict default-ship template validation compatible.
- Update packets carry only the visual flag and do not load or replace inventory NBT.
- Existing 54-slot storage, item saving, loot and storage UI remain in place.

## Authoring and export

1. Edit through Blockbench MCP and export project to ship-locker-textured-v1.bbmodel.
2. Switch to the rest pose and export OBJ to ship-locker-export.obj.
3. Run python tools/export_ship_locker.py.

The packager splits the actual Blockbench OBJ into chassis, doors and complete item. It extracts embedded material textures, writes MTL emission and runtime model definitions. It does not generate replacement geometry.

The vanilla blocks atlas explicitly includes the two effect textures for item rendering. The block-entity renderer samples those textures directly.

## Validation

- gradlew test build: 669 tests.
- gradlew runClient -PlockerSmoke: isolated fresh-world render and lifecycle checks.
- Eight captures: closed north, open, reclosed, south, east, west, darkness, resource reload.
- Fixture checks two-viewer last-close behaviour, inventory save/load, visual update safety, client synchronization, removed-entity handling, shape bounds and atlas registration.
- Door travel unit tests verify retreat before sliding, hologram removal before sliding, exact rest position and outer cabinet limits.
- Blockbench timeline samples verify fixed frame scale while the lock icon changes width.
- Selected game captures are under ship-locker-validation; no user save is used.

The visual lock is an access indicator only; this change does not add a permission or ownership system.
