# Starmap terminal: slanted command console

Date: 2026-09-24

Status: continuous-frame whitebox approved; textured and integrated into
`starmap_terminal`. Build and client verification are recorded in
`starmap-console.md`. The user authorized saving, committing and pushing this
version after confirming interaction was sufficient for delivery.

## Design contract

**Purpose / fantasy:** plot a course at a physical shipboard navigation console.
This is the world-space entry device for the existing experience-oriented star
map. Right-click still opens the existing star map and its navigation behavior.

**Primary visual:** one broad, inset navigation display carried by a continuous
inclined deck. Its silhouette has a low operator edge, a raised rear edge and
a continuous chamfered protective frame. A rearward pedestal leaves visible foot clearance
under the front overhang. The floor plate anchors the mass.

**Priority:** display → reachable front controls → supporting structure → service
details. The star chart is the only large visual information area. A narrow
side instrument strip is subordinate; it does not become a second main screen.

**Spatial organization:** model coordinates use one block = 16 units, center
X/Z = 8. The operator stands north (-Z). The deck rises toward +Z at roughly
23 degrees; target footprint stays within 16 × 16, height within 16 units.
The front band is a wrist rest and a few grouped physical controls, with a
small raised navigation control. The back has one recessed service hatch and
a modest vent. Side forms express the load path instead of adding greebles.

**Art:** real chamfers, tapered pedestal and sloping mesh surfaces. Whitebox
uses neutral value groups for structure, protective edges, glass and controls.
Final materials, after proportion review, use graphite/blue-black recesses,
titanium structure and sparse cyan navigation graphics. Any amber is a small
physical-state indication. Texture detail must remain readable at block scale.

**Interaction flow:** approach → identify the inset chart → right-click → inspect
the existing star map → choose destination → use existing navigation feedback.
Decorative control geometry adds no new click target or command semantics.

**Motion / states:** no idle bobbing or whole-console pulse. The physical model
does not claim new online, warning or travel-state logic. Existing menu state
behavior remains the authority. Any later animated display requires an explicit
implementation decision, not a decorative assertion of runtime state.

**Do not:** split the console into upright monitors; make every edge luminous;
fill the underside with a solid cube; add floating holograms without purpose;
copy the old terminal geometry; redesign the star-map GUI in this asset task.

## Implementation boundary

- Target only `starboundmc:starmap_terminal`; keep the deprecated `ship_console`
  outside this work.
- Preserve block/item IDs, loot, four facings and menu/network behavior.
- Author geometry in the live Blockbench project through MCP and retain an
  editable `.bbmodel`. Exported geometry must come from Blockbench.
- Use the established Generic Model → Blockbench OBJ → NeoForge loader route
  if this freeform whitebox is accepted. Packaging must not generate replacement
  geometry independently.
- Update collision/selection shapes to the accepted outline at integration,
  with four-facing verification. Existing shapes are not whitebox acceptance.
- Final integration must verify item display, placement, resource reload and
  approach/use at the terminal. GUI Scale 2/3/4 applies to retained menu
  readability; no GUI layout changes are authorized by this model direction.

## Review deliverables

Editable whitebox plus front three-quarter, front, side and rear previews. Check
the deck angle, screen/control hierarchy, chamfer readability, floor contact,
support intersections, underside clearance and all face normals. Whitebox
review does not establish final texture quality or in-game acceptance.

## Handoff baseline

Referenced task: `01a0d16f-0aaf-7101-8cdf-f55fcea12f73`.
Last explicit design decision: “肯定是新的，斜面板操作台样式，不用管旧的那个”.
Branch: `codex/refactor-block-modeling`; starting HEAD:
`79aa32988135808c7781f41c02f3d12d279fc63e`.

The live project is `StarboundMC Starmap Console — Command v1`, format `free`.
On takeover it contained zero geometry elements and one `console_whitebox`
texture. The older tool call claiming a base placement had not left geometry.
The host exposes MCP 1.7.0; it lacks `get_capabilities` and indexed faces on
`place_mesh`. Read-only project inspection and native format flags confirm
mesh support. Complex topology is created inside Blockbench through MCP
`risky_eval` using native Mesh/MeshFace operations and undo checkpoints.

Pre-existing local changes to `ship_engine_side.png` and `.zcodeignore` are
outside this asset task.

## Whitebox v1 result

- Editable source: `starmap-console-whitebox-v1.bbmodel`, exported directly by
  the live Blockbench project codec through MCP.
- Review sheet: `starmap-console-whitebox-review.png`; individual original MCP
  captures use `starmap-console-whitebox-{perspective,front,side,rear}.png`.
- 23 meshes, 960 faces, eight named functional groups and five neutral clay
  materials. No final star-chart graphics or emissive treatment yet.
- Bounds: X 0.72–15.28, Y 0–15.6085, Z 1.3–14.65. The deck slope is 0.43
  (approximately 23.27 degrees), with a rearward support and clear front overhang.
- Visual review found and corrected a solid screen surround that hid the glass;
  the surround now has a real aperture. Front and rear service details were
  adjusted to follow the tapered support surfaces.
- Export inspection: all positions finite; no zero-area faces; every undirected
  mesh edge has exactly two incident faces; all signed mesh volumes positive;
  every texture reference and UV coordinate valid. Source format is `free`.
- The four exported views were visually inspected. The review sheet only crops,
  scales and arranges these captures; it does not replace or repaint geometry.
- `git diff --check` passed. No Java or runtime asset definitions were changed,
  and no Gradle or in-game validation was claimed for this whitebox.

The user approved the frame refinement and requested the next stage. Final
Blockbench materials/chart graphics, OBJ packaging, shape alignment and client
validation are now complete; see `starmap-console.md`.

### Outer-frame refinement, 2026-09-24

The user noted that the perimeter looked like four detached pieces. The side
rails, rear brow and wrist-rest strip were replaced with one closed mesh,
`continuous_chamfered_outer_frame`. Its four cut corners share vertices; the
upper surface, inner lip and outer bevel now run continuously around the deck.
The existing sloped deck angle, display, controls and pedestal are retained.

The frame contains 64 vertices and 64 faces in one connected component, with
two consistently oriented incident faces per edge, positive volume, no
duplicate vertices and no zero-area triangles. Comparing the exported source
against the pre-edit checkpoint confirms all other 22 meshes are unchanged.
Four MCP views and the review sheet were refreshed and visually inspected.
`starmap-console-whitebox-preview.png` is a cropped perspective preview.

### Material and integration pass, 2026-09-24

Preserved all 23 mesh geometries and the approved continuous frame. Seven used
materials provide graphite recesses, titanium structure, muted metal edges,
amber/cyan control accents, a 128 × 128 chart and a 128 × 128 instrument atlas.
The chart and instrument graphics were painted inside Blockbench through MCP.
UVs share one planar mapping across the triangulated chart surface; the initial
per-triangle repetition was corrected and is covered by a resource regression
test. The chart is decorative, not a live replica of the navigation state.

The existing terminal now loads the actual Blockbench OBJ. Selection/collision
shapes follow the sloping shell with depth slices and a rearward support. Four
facings preserve the operator opening, and `noOcclusion` prevents the open model
from hiding neighboring block faces. Existing menu and network code is retained.
