# Slanted starmap terminal

Completed material/integration pass: 2026-09-24.

## Model and behavior

The current `starboundmc:starmap_terminal` uses the accepted slanted command
console: one continuous chamfered frame, inset chart, front controls and a
rearward support. The deprecated `ship_console` remains outside this change.

- Source: `starmap-console-textured-v1.bbmodel` (23 meshes, 960 faces, eight
  functional groups). Geometry matches the approved continuous-frame whitebox.
- Actual Blockbench export: `starmap-console-export.obj`, scaled to 1/16 units.
- Seven used materials. `terminal_chart` and `terminal_instruments` are 128 × 128;
  uniform structure/control materials are 16 × 16. Only the chart, instrument
  graphics and small cyan control edges use emissive ambient shading.
- Chart geometry uses a continuous planar UV mapping. The display is decorative
  static artwork; it does not report the actual destination or flight state.
- Footprint bounds: X 0.72–15.28, Z 1.3–14.65; Y 0–15.6085 model units. The
  approximately 23.27-degree deck remains inside one block.
- Uses NeoForge's existing OBJ loader; no custom renderer or dependency added.
- Retains block/item IDs, loot, four-facing blockstates, right-click menu,
  server-side validity checks and existing navigation/network behavior.
- Selection/collision follows the base, tapered rearward pedestal and sloping
  deck. One-unit depth slices approximate the incline; this is not a triangle
  collision mesh. Foot space and the air above the front edge remain open.

## Re-export

1. Edit `starmap-console-textured-v1.bbmodel` in Blockbench.
2. MCP `export_model`, codec `project`, options `{}`, to that source path.
3. MCP `export_model`, codec `obj`, options `{}`, to `starmap-console-export.obj`.
4. Run `python tools/export_starmap_console.py` from the repository root.

The packager consumes those actual exports, validates bounds/UVs/indices and
material references, extracts only referenced embedded textures, and writes the
OBJ/MTL and block/item wrappers. It never creates replacement geometry.

## Validation

- `gradlew test build`: 670 tests passed, no failures, errors or skips.
- Resource checks cover the OBJ/MTL texture chain and continuous shared UVs on
  the chart, preventing the earlier repeated-triangle texture defect.
- `gradlew runClient -PstarmapConsoleSmoke`: seven stages passed in a fresh,
  disposable world under `run-starmap-console-smoke`. This opt-in fixture is
  excluded from normal builds.
- Client checks cover all four facings, collision and selection openings,
  footprint/height, no occlusion, right-click menu opening and server validity,
  supported server snapshots and menu lock state with the core offline/online,
  baked OBJ/chart quads, all seven texture-atlas entries, item-model lookup and
  successful resource reload. The fixture invokes the existing right-click
  block handler; it does not automate a real mouse click or warp journey.
- Retained client screenshots and success marker: `starmap-console-validation/`.
  `menu-offline.png` shows the existing `FATAL ERROR` core-offline lock, not a
  crash; `menu-online.png` checks the map after the fixture advances its own
  disposable world's core through reboot. Production progression is unchanged.
- Blockbench front, side, rear and three-quarter views were captured and reviewed;
  `starmap-console-textured-review.png` arranges the native captures.
- The item model resolves in the client, but a separate inventory/hand display
  visual review has not been performed. Full navigation, multiplayer, GUI Scale
  2/3/4 and user visual acceptance are not implied by this asset smoke test.

On 2026-09-24 the user authorized saving, committing and pushing this version,
with working interaction sufficient for delivery. This is delivery approval;
it does not imply a separate user-run visual or gameplay test. Unrelated
pre-existing local changes are outside this model's delivery scope.
