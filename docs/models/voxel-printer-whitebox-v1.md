# Voxel printer whitebox v1

Created in Blockbench through the local Blockbench MCP server on 2026-09-23.
Status: silhouette approved by the user. Materials and runtime integration are documented in
`voxel-printer.md`; this file records the preserved whitebox stage.

## Assets

- `voxel-printer-whitebox-v1.bbmodel`: editable Generic Model, 71 meshes in nine named groups, four embedded neutral clay textures.
- `voxel-printer-whitebox-v1-perspective.png`: three-quarter view.
- `voxel-printer-whitebox-v1-front.png`: front orthographic view.
- `voxel-printer-whitebox-v1-side.png`: side orthographic view.

## Form

Wall-mounted open fabrication bay with tapered side shoulders, a chamfered overhead bridge,
layered projecting build tray, recessed service panels, and two independently grouped optical heads.
Neutral gray values distinguish parts only; these are not the final material colors.
At this whitebox stage the GUI, recipes, queue, printing behavior, runtime model, and renderer were unchanged.

## Coordinate and integration anchors

Model units: 16 units per block. Open side faces north (-Z); wall attachment is at Z=16.
Bounds: X=0.8..15.2, Y=1.5..16, Z=2.52..16.

- `07_probe_left` pivot: [5.76, 12.8, 8].
- `08_probe_right` pivot: [10.24, 12.8, 8].
- Both heads point down local -Y at rest; bezel tip Y=10.88, 1.92 units below the pivot.
- Fixed mounting shoes and yokes belong to `06_probe_mounts`.
- Build surface Y=5.29, registration markers up to Y=5.32.
- Existing forming item lower plane: Y=5.44, center X=8, Z=5.6.
- Existing formation upper plane: Y=10.56.

Pivots and nozzle length match VoxelPrintingStationRenderer's existing normalized constants.
Sampled scan poses at three heights and three horizontal target positions put the lowest head vertex
at Y=10.8464, above the tray. This is a geometric check, not in-game animation validation.
Front, side and perspective views were inspected; the shoulder/bridge junction was refined.

This is a mesh authoring asset. Runtime integration will need an appropriate mesh export/render path
and separately rendered moving head groups; it is not a direct vanilla Java block JSON replacement.
