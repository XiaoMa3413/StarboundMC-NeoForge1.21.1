# Ship wall locker — whitebox v1

Built through Blockbench MCP in a separate Generic Model project. This is an art and motion prototype; runtime assets and storage behaviour have not been replaced.

## Approved direction

A thin wall-mounted ship locker with two recessed sliding doors, a right-hand control column and a mounting backplate. Keep the existing 54-slot storage UI and inventory behaviour when integrating later.

The user's clarification overrides the initial physical lock experiment: the central lock is a **flat holographic screen graphic**, not a three-dimensional padlock. All physical lock geometry and its rig have been removed. A small projector socket remains below the door seam; the screen graphic and its light treatment are deferred to the material stage.

## Geometry

- 43 named meshes, grouped as cabinet, door_left and door_right.
- Door carcasses extend behind the perimeter frame and control column. A dark overlapping lip behind the central seam seals the closed doors without blocking their opening motion.
- 16 model units per block; rear mounting surface at Z = 15.95; front faces toward negative Z.
- Shell: X = 0.5–15.5, Y = 2–14, Z = 10.1–15.95. Small front details extend to Z = 9.02.
- Bevelled frame, independent doors, inset pulls, display, keypad, reader, internal shelf and rear panels.
- Neutral clay colours only. Embedded textures are temporary swatches, not final materials.

## Animation preview

- animation.locker_open: retreat 0.65 units into the cabinet, then slide each door 1.2 units outward.
- animation.locker_close: reverse the slide, then return to the closed depth.
- animation.locker_demo: an 8-second loop with closed and open holds.
- Mirrored model coordinates: screen-left door travels positive X; screen-right travels negative X.
- Opening is deliberately a short travel gap rather than doors disappearing outside the block.

## Review files

- ship-locker-whitebox-v1.bbmodel — editable geometry and animation tracks.
- ship-locker-whitebox-v1-closed.png — closed perspective.
- ship-locker-whitebox-v1-open.png — same camera at the open hold.

## Later integration

After shape approval, apply the shipboard material palette, add a flat emissive hologram texture, and integrate the model and viewer-count-driven door animation in game. Update the selection/collision shape and placement orientation while preserving existing crate inventories, world generation placements and the storage UI. Animation currently runs only in Blockbench.
