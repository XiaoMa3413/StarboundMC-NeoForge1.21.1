# Shuttle-style ship thruster

Approved shape: `shuttle-thruster-greybox-v1.bbmodel`. Finished editable source:
`shuttle-thruster-textured-v1.bbmodel`. The 16-sided bell, liner, thin lip,
mount and reinforcement geometry are unchanged from the approved greybox.

## Runtime assets

- Block/item ID remains `starboundmc:ship_engine`; the separate ignition machine
  `ship_engine_unit` is not changed.
- `models/block/ship_engine.json` loads `shuttle_thruster.obj` through NeoForge's
  built-in `neoforge:obj` loader. The existing four horizontal blockstates and
  item parent reuse this model. No additional model-loader dependency is needed.
- Six 128-pixel material textures distinguish cold steel, warm scorched liner,
  rim, throat and mounting hardware. Textures are embedded in the BBModel.
- Exhaust is rendered by `ShipThrusterRenderer` only for thruster blocks in the
  ship dimension. It follows each block's facing and uses the synchronized
  flight phase/clock. TURN warms up, acceleration grows the plume, cruise and
  hyperspace sustain it, and arrival fades it. Docking and crew hold extinguish it.
- Animation freezes while the integrated client is paused. A solid block directly
  in front of the nozzle suppresses the effect. An expanded render box keeps the
  plume visible when the nozzle itself is outside the camera frustum.
- The blue-white plume and liner are emissive visual effects; they do not create
  damaging fire or dynamic terrain lighting. The former permanent level-14
  block light is removed so a parked nozzle is dark. The block no longer occludes
  its neighbors as a solid full cube.
- Bright emission revision: dedicated unlit shaders omit both directional face
  shading and the lightmap. Alpha-weighted additive blending builds a white-hot
  core, saturated blue perimeter and three luminous knots along the jet. Four
  camera-facing analytic halos supply soft optical glow without a bloom mod.
  These passes test scene depth and do not write depth or change block lighting.

## Re-export

1. Edit the textured project in Blockbench via MCP.
2. Export `project` to `docs/models/shuttle-thruster-textured-v1.bbmodel`.
3. Export `obj` to `docs/models/shuttle-thruster-export.obj` (default 1/16 scale).
4. Run `python tools/export_shuttle_thruster.py`.

The packager extracts embedded PNGs, replaces UUID material names with stable
resource names, writes the Minecraft MTL and validates finite, in-block positions
and atlas UV bounds. Blockbench's bottom-origin OBJ UVs are flipped by the loader.

## Validation (2026-09-23)

- `gradlew test build`: 667 tests, zero failures/errors/skips.
- Added envelope and synchronized snapshot tests for hold, stale packets,
  reconnect, short routes and phase boundaries.
- `gradlew runClient -PthrusterSmoke`: isolated fresh world, real OBJ baking and
  block-entity rendering. Seven captures cover docked, acceleration, cruise,
  crew hold, arrival, stopped and all four horizontal facings.
- The smoke fixture feeds controlled flight snapshots to the normal client
  state; it is not a full multiplayer navigation acceptance test.
- Captures/logs: `run-thruster-smoke/screenshots/`,
  `build/shuttle-thruster-client.log`, `build/shuttle-thruster-build.log`.
- Bright-emission revision also passed all seven client captures, including a
  cruise close-up against the bright sea-lantern test floor. Its logs are
  `build/shuttle-thruster-glow-client.log` and `build/shuttle-thruster-glow-build.log`.
  The previous transparent appearance is retained in
  `shuttle-thruster-validation/exhaust-before-glow.png` for comparison.
- A pre-existing printing-station assertion now normalizes CRLF before matching
  multiline LSS, so the full suite also passes on this Windows checkout.

Remaining manual acceptance: start a real star-map route in the user's ship,
observe departure/arrival from outside, and check the effect with their graphics
settings. Test fixture classes are opt-in and excluded from the normal mod JAR.
