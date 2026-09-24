# Voxel printer material and runtime model

The user approved the whitebox silhouette on 2026-09-23. The textured source is
`voxel-printer-textured-v1.bbmodel`; mesh vertices and face topology match the approved
`voxel-printer-whitebox-v1.bbmodel` exactly (71 meshes). Both were authored with Blockbench MCP.

## Materials

Light titanium-gray coated shells, graphite chamber surfaces, steel pivots and tray edging,
with sparse cyan optical surfaces and amber registration marks. Two used textures are embedded:
`printer_surface_atlas` (256x256) and `printer_optics` (64x64).
UV coordinates use 256 texture units. Neutral clay/unused backup textures in the authoring file
are excluded from runtime export.

## Runtime

- The existing `voxel_printing_station` block ID, four facing states, wall support, collision,
  recipes, queues, networking and GUI are preserved.
- NeoForge's built-in OBJ loader loads the 57-part chassis as the static block model.
- The seven-part left optical head is exported around its pivot, registered as a standalone
  baked model and reused by the block-entity renderer for both heads. Fixed mounts stay on the chassis.
- Pivot X=0.36/0.64, Y=0.80, Z=0.50 and nozzle length=0.12 retain the existing scan trajectory.
- The complete 71-part model is used by the inventory/held item, including both heads at rest.
- MTL ambient emission is enabled only for optical surfaces. It supplies unshaded, fully lit
  surfaces without changing block light levels. The existing beam, scan layer and clipped item
  rendering are preserved.

## Re-export

1. Export `project` from Blockbench to `docs/models/voxel-printer-textured-v1.bbmodel`.
2. Export `obj` at the default 1/16 scale to `docs/models/voxel-printer-export.obj`.
3. Run `python tools/export_voxel_printer.py`.

For MCP export on Blockbench 5.2.1, supply `options: {}` to avoid the interactive export-options
dialog. The packager splits the exported meshes, reindexes OBJ references, offsets the moving head,
extracts only referenced embedded textures and writes the three runtime model JSON files and MTL.
The legacy cube generator refuses to overwrite the new mesh model.

## Validation

- `gradlew test build`: successful.
- `gradlew runClient -PprinterSmoke`: successful, six captures covering idle, early/middle/late
  formation, completion and resource reload. The isolated fresh world never opens a user save.
- Captures in `run-printer-smoke/screenshots/`; selected copies in `voxel-printer-validation/`.
- Smoke fixture exercises the normal baked models and renderer with controlled client snapshots;
  it is not a new end-to-end recipe/queue test. Those implementations were not changed.
- Checked geometry preservation against the approved whitebox, normalized UV bounds, finite
  positions, valid OBJ indices and baked model registration after resource reload.
