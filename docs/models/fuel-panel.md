# Wall-mounted fuel control panel

Completed model and integration pass: 2026-09-24.

## Appearance and geometry

The approved direction is a thin control panel, with the locker providing the
mounting and shipboard material reference. The panel has one connected chamfered
frame, a recessed amber energy schematic, three tactile keys, a small rotary
control and a narrow refuelling inlet. There is no cabinet door or storage-lock
graphic. The schematic and controls are decorative static model details; actual
fuel amounts and refuelling remain in the existing right-click interface.

- Source: `fuel-panel-textured-v1.bbmodel`, authored through Blockbench MCP.
- Actual Blockbench export: `fuel-panel-export.obj`, at 1/16 model scale.
- 25 meshes, 1,028 faces, five functional groups, seven materials.
- Bounds: X 0.5–15.5, Y 2–14, Z 12.36–16 model units. North-facing front is -Z.
- Width and height align with the locker; total depth is 3.64 units, about half
  its depth. The backplate touches the mounting wall at Z = 16.
- `fuel_panel_screen`: 128 × 64, continuous planar UVs across the triangulated
  display. `fuel_panel_markings`: 128 × 128, pixel lettering and key symbols.
- Structure materials are 16 × 16 solid swatches. Only the display uses emissive
  ambient shading. This does not add world light.
- Uses the existing NeoForge OBJ loader with no new renderer or dependency.

## Runtime compatibility

`starboundmc:fuel_controller`, its facing-only blockstate, block entity, five
persistent fuel slots, loot, screen, capacity rules and network handlers are
unchanged. The active implementation is `Stage2Blocks.FuelController`; the old
excluded `FuelControllerBlock` has not been edited.

Horizontal wall placement follows the clicked face. Top/bottom placement keeps
the player-facing fallback. Selection and collision use a thin rotated envelope
that includes the controls and leaves the front of the block empty. Rotation,
mirror and `noOcclusion` match the open silhouette. Wall support is deliberately
not mandatory, preserving blocks already in saves and ship templates.

## Re-export

1. Edit the source in Blockbench.
2. Export codec `project` to `fuel-panel-textured-v1.bbmodel` through MCP.
3. Export codec `obj` to `fuel-panel-export.obj` through MCP.
4. Run `python tools/export_fuel_panel.py` from the repository root.

The packager validates export indices, bounds, material references and UVs;
extracts the embedded PNGs; and writes OBJ/MTL plus block/item wrappers. It does
not construct replacement geometry. The existing four blockstate variants are
retained. Recreate the preview captures after visible changes.

## Verification

- `gradlew test build`: 672 tests, no failures/errors/skips.
- Resource regression checks validate OBJ material-to-texture resolution, the
  thin bounds, continuous display UVs and connected outer-frame topology.
- `gradlew runClient -PfuelPanelSmoke`: seven stages in a fresh disposable world,
  including all four wall facings, loaded menu, refuelled menu and resource reload.
- Checks cover mounting orientation, top/bottom fallback, collision/selection,
  rotation/mirror, unsupported legacy placements, no occlusion, five-slot NBT
  round trip and retention of fuel items that do not entirely fit.
- The existing right-click block handler opens the real menu. A real client
  `AddFuelPacket` consumes the fixture's coal, charcoal, blaze powder and fuel
  crystal for exactly 105 fuel; server fuel, client fuel and all five client
  slots are checked. This does not automate a physical mouse click.
- Baked geometry, all seven atlas textures, item-model resolution and resource
  reload pass. Normal builds exclude the opt-in render-test classes.
- Retained screenshots and success marker: `fuel-panel-validation/`.
- Multi-angle Blockbench review: `fuel-panel-textured-review.png`.

Separate inventory/hand visual review, multiplayer and full voyage gameplay
are not implied by these checks. The screen schematic remains static artwork.
