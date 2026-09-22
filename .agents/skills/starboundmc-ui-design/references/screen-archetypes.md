# Screen archetypes

Use these archetypes to choose the *kind* of experience. Do not treat the layouts below as templates.

## Utility UI

Purpose: fast, repeated, low-drama operations.

Typical examples:
- storage
- furnace-like processing
- simple fuel slot/controller
- small configuration panels

Design behavior:
- clarity first
- compact
- low bespoke-art cost
- common controls are fine
- keep hierarchy obvious

Do not promote every utility screen into a cinematic console.

## Feature UI

Purpose: give a signature system or machine a memorable interaction.

Typical examples:
- Voxel Printing Station
- Matter Manipulator upgrade workbench
- N.O.V.A. terminal/dialogue
- Teleporter

Design behavior:
- one clear primary visual
- supporting information should orbit that primary visual
- allow texture-backed art, animation, custom drawing, and hybrid implementation
- generic lists/buttons should remain supporting actors

## Experience UI

Purpose: make spatial exploration/navigation/story interaction itself become the interface.

Typical examples:
- Star Map
- major ship event interfaces
- large diegetic navigation/plotting systems

Design behavior:
- begin from space, object, motion, selection, and navigation
- overlays appear only when needed
- avoid fixed dashboard chrome that steals attention from the experience

## Page-specific direction

### Star Map
Interaction fantasy: navigating real space.
Primary visual: the starfield and celestial objects.
Keep permanent UI chrome subordinate.
Selection can use thin targeting rings/corner marks/subtle pulse rather than heavy Minecraft-style boxes.
Routes should normally be dimmer than selected targets.

### N.O.V.A.
Interaction fantasy: communicating with a shipboard intelligence.
Primary visual: N.O.V.A.'s holographic identity.
Preserve the spherical/orbital visual motif where it remains applicable.
Avoid generic chat bubbles and messaging-app metaphors.
Motion should suggest a living projection, not a bouncing mascot.

### Matter Manipulator engineering
Interaction fantasy: physically/technically modifying a complex tool.
Primary visual: the Matter Manipulator schematic/blueprint.
Upgrade relationships should feel connected to physical subsystems of the tool rather than a generic RPG skill tree.
Green can become the local engineering accent inside the broader graphite ship language.

### Voxel Printing Station
Interaction fantasy: manufacturing an object from digital matter.
Primary visual: the item being fabricated or prepared for fabrication.
Recipe navigation, materials, quantity, and queue are supporting information.
A useful fabrication progression can move from wireframe/outline → holographic partial form → materialized object, but only if the implementation cost is justified.
Avoid letting a recipe list dominate the first read of the screen.

### Fuel / engine systems
Interaction fantasy: managing real ship energy.
Primary visual should make reserve/flow/energy state legible.
Amber is the natural local accent.
Do not reuse cyan for every resource simply because cyan is the project base color.

### Teleporter
Interaction fantasy: selecting a destination and committing to a transfer.
May lean slightly more avionics/locator-like than other machine screens.
Keep destination selection mechanically clear.
Do not make every destination a glowing card.

## Same language, different accents

The systems above should feel related without becoming visually identical.

A useful mental model:
- Star Map: black / cyan / spatial
- Engineering: graphite / green schematic
- Fabrication: graphite / cyan holographic manufacturing
- Fuel: graphite / amber energy
- Teleport: graphite / cold white/cyan locator
- N.O.V.A.: cyan AI projection

Do not copy this list mechanically. Use it as a semantic map when it matches the fiction.
