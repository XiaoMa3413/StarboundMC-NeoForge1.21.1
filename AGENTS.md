# StarboundMC Agent Guidance

## Repository Engineering Rules

- Start at [docs/README.md](docs/README.md), then [architecture](docs/architecture.md), task-specific reference, [current-work](docs/current-work.md), and relevant [known issues](docs/known-issues.md).
- Current code, tests, and runtime evidence take priority over historical documents. `docs/archive/` and `docs/legacy-forge/` are not current requirements; `docs/models/` contains development art assets and is not default architecture reading.
- Before the first public Playtest, internal development saves/APIs have no backward compatibility promise. Keep registered IDs and data-driven identities stable; design released-player schema/ID migrations separately when a real compatibility target exists.
- Do not add Legacy/Compat/Deprecated wrappers, speculative schema migration, downgrade, or adapters without a real released consumer. Find the authority and invariant before adding a fallback.
- The server owns gameplay and persistence. Client state is projection/cache, never a second authority. Prefer one canonical representation of each fact.
- Corrupted authoritative state must fail explicitly, not silently become valid default state. Optional visual/GPU failure may fall back gracefully.
- Name production code and tests by current responsibility or contract; migration-stage names are temporary.
- Implementation plans are consumable construction material: stable rules go to current reference, unfinished work to current-work, and historical plans to archive. Git history records implementation; do not create parallel `xxx-v2-final-polish-new.md` current truth or append commit diaries to reference.
- Use behavioral tests and runtime evidence for correctness; source-string checks are not primary behavioral proof.
- Apply touch-to-migrate when substantially changing a feature: use existing `client/space`, `client/starmap`, `client/shipai`, `client/hud`, `client/epp`, and `client/ui` packages; group network code by feature when warranted. Avoid mass package-only moves and migration adapters.

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
