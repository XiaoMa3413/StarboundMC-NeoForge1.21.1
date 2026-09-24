# Shipboard captain chair

An original command chair inspired by the Enterprise-D reference silhouette: reclined upholstered back, independent headrest, low pedestal and forward control arms. The continuous back pad, titanium/graphite shell and cyan instrument graphics follow StarboundMC's own shipboard style.

## Assets and runtime

- Editable source: `captain-chair-textured-v1.bbmodel` (28 meshes, 2,230 faces, eight functional groups).
- Nine used 128 × 128 material textures; only the instrument texture is emissive. Upholstery uses restrained weave and stitching; shell surfaces are painted metal.
- Geometry and textures were created in Blockbench through MCP. `captain-chair-export.obj` is the actual Blockbench export at 1/16 scale.
- The built-in NeoForge OBJ loader renders the static model; no custom renderer or new dependency is needed.
- Existing block/item ID, loot, facing-only blockstate, right-click seating and safe dismount logic remain in place. Existing ship templates remain compatible.
- The collision/selection shape follows the pedestal, cushion, back, headrest and control arms, with open space below the arm bridges. It rotates for all four facings.
- Model height is 22.55 units (about 1.41 blocks). Its X/Z footprint stays inside one block. The cushion top is 7.775 units; the original passenger attachment height is retained.
- Control panels are decorative instrument graphics; no new chair menu or command action was introduced.

## Re-export

1. Edit the source in Blockbench.
2. MCP `export_model`, codec `project`, options `{}`, to `docs/models/captain-chair-textured-v1.bbmodel`.
3. Export codec `obj`, options `{}`, to `docs/models/captain-chair-export.obj`.
4. Run `python tools/export_captain_chair.py`.

The packager validates mesh count, finite positions, footprint/height, UV bounds and OBJ indices. It extracts used embedded textures, replaces UUID materials with stable resource names and writes the model/item definitions. It never generates substitute geometry.

## Validation

- `gradlew test build`: 669 tests passed in the current workspace (including the pending locker work), no failures, errors or skips.
- Opt-in fixture: `gradlew runClient -PchairSmoke`, isolated fresh world under `run-chair-smoke`, excluded from the normal mod JAR.
- Fixture checks four-facing raised shapes and arm openings, right-click mounting, repeated-click seat reuse, seated head clearance under a two-block ceiling, safe dismount and unused seat cleanup. It also checks item model lookup and instrument atlas registration after resource reload.
- Blockbench views: `captain-chair-perspective.png`, `captain-chair-front.png`, `captain-chair-side.png`, `captain-chair-rear.png`. `captain-chair-preview.png` crops the perspective capture onto a dark background.
- Eight client stages completed successfully. Front and seated captures were visually reviewed; the seated player fits below the two-block ceiling. Selected captures are in `captain-chair-validation/`.
- The user independently tested the model in game and accepted it on 2026-09-24.

Existing uncommitted locker work is retained separately from this chair change.
