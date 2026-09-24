# Single-person transporter

Implemented 2026-09-24 from the approved `transporter-design.md` contract.

## Model and source

- Editable Blockbench Generic Model: `transporter-textured-v1.bbmodel`.
- Actual Blockbench MCP OBJ export: `transporter-export.obj`.
- Four-angle review: `transporter-textured-review.png`.
- 68 meshes, 3,216 faces, eight materials. The instruments and standing-disc
  artwork use continuous 128 x 128 textures; structural materials use 16 x 16.
- One-block footprint, three-block height. Mesh bounds in Blockbench units:
  X 0.25–15.75, Y 0–47.95, Z 0.1120–15.9. The standing surface is Y=9.
- Chamfered plinth, paired circular emitters, flared rear-to-disc transition shoulders, continuous rear spine, recessed
  instrument panel, titanium shell, graphite interior and narrow cyan lights.
- The native source faces north. Runtime blockstates rotate the same three
  height-local OBJ parts through all four horizontal directions. The item
  displays the complete assembly at a reduced scale.

Re-export the `project` and `obj` codecs through Blockbench MCP to the source
files above, then run `python tools/export_transporter.py`. The packager validates
indices, materials, UVs and part bounds; extracts embedded textures; translates
the upper groups to local Y; and writes OBJ/MTL, model wrappers and blockstates.
It does not construct substitute geometry. Re-export twice produces identical
runtime assets.

## Runtime behavior

The registered ID remains `starboundmc:teleporter`. `Stage2Blocks.Teleporter`
extends `TransporterBlock`, with `facing` and `part=0/1/2` state properties.
All parts occupy real block cells and have rotated collision/selection shapes.
The front and passenger bay stay open. Placement requires two free, dry cells
above the base and space for the upper collision shapes. Pistons cannot move it.

Right-clicking any part resolves the original lower anchor and opens the existing
teleporter menu. Naming, destination selection and core-online server authority
are retained. Only lower anchors are valid named destinations. Breaking any part
removes the assembly; the original broken part owns the existing one-item loot
table and sibling cleanup never generates loot. The block is now included in
the pickaxe mining tag. Creative and no-drop removal generate no sibling drops.

Named and ship arrivals use the rotated disc center at base Y + 9/16 and check
the standing player's clearance. Blocked destinations refuse travel. Successful
arrivals clear velocity/fall distance and face the station's opening. The older
`shipTeleporterDestination` BlockPos helper remains a nominal compatibility
coordinate; actual travel through an existing station uses the precise vector.

Pre-facing saved blocks default south, opening the authored ship's station into
the cabin. Chunk-load checks run after promotion and schedule assembly updates.
Upper parts are added only when both cells are free or already matching parts;
occupied blocks and fluids are never overwritten. Orphan parts remove themselves.
The archived ship template and its 1,178 layout entries remain unchanged. The
runtime parser accepts only the exact former property-less teleporter encoding
as a narrow exception to its strict state round-trip validation.

## Travel effects

Successful server travel sends a dimension-scoped clientbound effect event to
nearby players, including the arriving traveller. Protocol version is 17.
The 22-tick effect combines paired emitter flashes, slim vertical light filaments,
a moving scan ring and small sparkling billboards. Surface arrivals have no
device canopy ring. Arrival playback waits for destination chunk/model readiness,
then begins after four client ticks; this does not delay the actual teleport.
The renderer caps pending effects at 32, expires stale events and clears them
on world change/logout. There is no player invisibility or body fading.

## Verification and limits

- `gradlew test build`: **673 tests, zero failures/errors/skips**, successful normal
  build. The final JAR excludes opt-in render-smoke and GameTest classes.
- `python tools/export_transporter.py`: all three part bounds, full assembly,
  material chains, UVs and indices pass. Native source/project and live MCP
  project were saved and checked.
- `gradlew runClient -PtransporterSmoke`: **14 stages passed** in a new disposable
  world. Four facings; three-part menu anchoring; core-offline refusal; live
  rename and named-travel packets; real deck landing; ship return; surface/ship
  cross-dimension travel; client arrival effects; all textures/parts/item;
  inventory display and resource reload.
- Setup assertions exercise placement direction, upper occupancy/fluid/build
  limits, all four standing clearances, rotation/mirror, real survival and creative
  player mining on each part, blocked-bay refusal, invalid upper anchor pruning,
  and a property-less-style lower block loaded through the chunk event path.
- `gradlew runGameTestServer -PshipGameTests`: **all 46 required GameTest assertions
  passed**, including generated ship/template preservation and passenger clearance,
  single-item explosion removal for every part, no-drop removal, replacement and
  orphan cleanup. After reporting success, the test server remained in
  `ChunkMap.processUnloads` while shutting down. A thread dump was retained and
  the test process was stopped; this command did **not** exit cleanly through
  Gradle. The integrated client runs and normal build exited successfully.
- Retained evidence: `transporter-validation/`. Screenshots were reviewed for
  model/material resolution, readability, station fit and successful travel VFX.
  Menus were opened through the real block handler and actions used real network
  packets; this is not physical mouse automation or a separate multiplayer test.

No commit or push is included in this implementation approval. Unrelated working
tree changes are outside this asset task.
