# StarboundMC Agent Guidance

## UI / Art

For new or substantially redesigned UI, establish the visual and interaction concept before choosing implementation primitives.

1. Read `docs/ui-art-direction.md` for the project-wide visual language.
2. Use `.agents/skills/starboundmc-ui-design/` for UI design, redesign, critique, and Design Contract work.
3. Treat LDLib2 as an implementation runtime, not as the source of visual structure.
4. Do not derive screen layout from whichever LDLib2 widgets are easiest to use.
5. Existing UI, LSS, and component trees are implementation evidence, not automatic visual authority.
6. Distinctive screens may use texture-backed art, custom rendering, bespoke UI elements, or hybrid approaches.
7. Generic utility screens should remain simple; not every machine needs hero-screen treatment.
8. Once a design is approved, do not silently redesign it during implementation.
9. Enter the LDLib2 implementation phase only after the Design Contract is known; use `$ldlib2-ui` when concrete LDLib2 implementation, migration, debugging, or API verification is needed.
10. Keep GUI Scale 2/3/4 readability in mind throughout design and implementation.
11. For the Voxel Printing Station and appearance reuse in manufacturing UIs, read `docs/ui-fabrication-style.md`. Preserve the approved compact layout; this style contract does not authorize structural changes or copying the printer layout into other screens.

For tiny implementation-only fixes to an already approved screen, do not reopen the design unless the requested change exposes a real hierarchy or usability defect.
