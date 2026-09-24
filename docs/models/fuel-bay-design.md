# Wall-mounted fuel control panel

Direction approved on 2026-09-24: a thin wall-mounted fuel control panel.
The user clarified that the locker is the mounting/style reference; this is
a control panel, not a storage cabinet or a large fuel hatch.

- Locker-aligned width/height: 15 units wide, 12 high, about 3.5 deep, roughly
  half the locker depth. Rear at Z = 16, front toward negative Z when north-facing.
- One continuous chamfered titanium perimeter surrounds an inset graphite
  instrument fascia. A large central amber display is the main visual feature.
- The display shows a static energy-system schematic. A compact lower row of
  tactile controls, a refuelling symbol and a narrow inlet complete the panel.
- Restrained graphite/titanium palette, with amber energy accents.
- Mounting backplate and shallow side panels make the wall connection readable.
- Static model details are decorative. They do not claim to show live fuel
  quantities or add independent clickable buttons or door animation.
- Preserve `starboundmc:fuel_controller`, five persistent fuel slots, existing
  menu/refuelling behaviour, loot and facing-only blockstate compatibility.
- Wall clicks determine outward facing, with player-facing fallback on top or
  bottom placement, as on the locker. Existing free-standing blocks remain
  valid; no new mandatory support requirement.
- Author the mesh and embedded textures in a separate Blockbench MCP project;
  package its actual exports through the existing NeoForge OBJ pipeline.

Scope: model, materials, mounting orientation, selection/collision and visual
integration checks. No UI redesign or fuel-mechanics change. Commit/push is a
separate user decision for this new task.
