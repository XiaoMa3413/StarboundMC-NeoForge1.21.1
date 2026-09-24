# Captain chair redesign

Status: modeled and textured through Blockbench MCP, exported and integrated as the existing captain-chair block. Runtime validation is recorded in `captain-chair.md`.

## Direction

An original shipboard command chair, informed by the Enterprise-D captain's chair reference photographs. Keep the commanding, comfortable silhouette: a reclined back, separate headrest, substantial seat bolsters and forward control armrests. Do not reproduce the reference's central backrest slot, exact control layout, beige shell or franchise insignia.

Use the material language established by the shuttle thruster, voxel printer and ship locker: titanium-gray structure, graphite recesses, restrained cyan instrumentation. Warm gray upholstery gives the chair a softer identity than those machines. A small muted amber indicator may distinguish a physical control; avoid luminous outlines around the whole chair.

## Shape direction

One Minecraft block is 16 model units. These targets informed the model; the exported source is the authority for final geometry.

- Footprint: at most 16 × 16 units, centered on the existing block.
- Total height: 22.55 units.
- Seat cushion top: 7.775 units. The existing passenger attachment height is retained.
- Base: a compact chamfered floor plate and short central support, leaving visible air below the seat. No office-chair wheels.
- Seat: one broad cushion with a softened front edge, framed by two bolsters. Keep the sitting area visually open rather than filling it with a thick cuboid.
- Back: reclined about 10–12 degrees, gently narrowing above the shoulders. Use a continuous central upholstered pad, a separate lumbar pad, and restrained side bolsters.
- Headrest: a distinct shallow padded volume with softened corners, connected to the back rather than floating above it.
- Armrests: two narrow structural bridges attached near the back and supported near the front; preserve visible space below the bridges. Short upward-tilted control pods at the ends, with their faces readable from the sitting position.
- Rear: a shaped protective shell with a central structural spine and a modest service cover. Avoid decorative cables and dense greebles.

Freeform meshes and a few bevel segments should establish the shape. Use actual chamfers and tapered cross-sections rather than stacked axis-aligned cubes to approximate upholstery. Reserve small seams, controls and labels for texture work after checking the large forms.

Groups: base, seat_shell, upholstery, back_shell, headrest, arm_left, arm_right, controls. This chair does not require an idle animation to communicate its purpose.

## Integration constraints

- Preserve the existing `starboundmc:captain_chair` block/item IDs, loot and four-facing blockstate.
- The current unrotated chair faces south (+Z). Backrest remains on the north side.
- Keep the existing right-click mounting and safe dismount behavior. Update selection/collision shape to match the accepted geometry and rotate it for all four directions.
- Inspect the seat and head clearances in the actual two-block-high cockpit. A taller visible backrest alone does not justify raising the passenger.
- Follow the existing Blockbench-authored Generic Model → OBJ → NeoForge OBJ loader workflow if freeform geometry is retained.
- Keep source geometry in an editable `.bbmodel`; a packaging script may process the actual export but must not substitute independently generated geometry.

## Review views

Inspect front, side, rear and three-quarter views; check the control pods from the seated eye position and the silhouette beside a player. Verify no unsupported-looking arm bridges, intersecting cushions, open back-shell gaps, or excessively thick headrest. Runtime verification must include all four facings, mounting/dismounting and resource reload.

## Handoff

Read the prior task `block bench2` (01a0ccc3-1046-7141-9ad5-6eaa75a2a354). Its most recent completed asset was the animated wall locker. Existing uncommitted locker work is preserved.

Reference folder: `E:/Develop/doing/Minecraft-Modding-MCP/references/enterprise_d_captain_chair`. The three-quarter and side photographs were visually inspected for this concept.

The local MCP endpoint is `http://localhost:3000/bb-mcp`. Blockbench 5.2.1 reported MCP server version 1.7.0. Its older mesh tool lacks indexed faces, so topology was authored through the server's `risky_eval` tool using native Mesh/MeshFace operations with undo checkpoints. Groups, base textures, cameras and exports used the dedicated MCP tools. The initially unavailable approval service was resolved before modeling began.

The active project is `StarboundMC Captain Chair — Command v1`, Generic Model (`free`), with an editable source at `captain-chair-textured-v1.bbmodel`.
