---
name: starboundmc-ui-design
description: Design, redesign, critique, or plan the visual hierarchy, interaction concept, art direction, and asset needs for StarboundMC screens, HUDs, machine interfaces, terminals, and in-game UI. Use before implementation when a UI concept or visual hierarchy is not already approved. Do not use for tiny implementation-only fixes to an already approved screen, pure LDLib2 API questions, or backend/gameplay logic with no UI design decision.
---

# StarboundMC UI Design

Design StarboundMC UI as an in-world game interface before thinking about implementation primitives.

This skill decides **what the UI should feel like, emphasize, and communicate**. It does not decide which LDLib2 widgets are most convenient.

## Core boundary

Treat LDLib2 as an implementation runtime, not as the source of visual structure.

Do not derive a screen from the available widget list.
Do not begin by choosing `Panel`, `Button`, `ScrollerView`, `TabView`, flex rows, cards, or other framework primitives.
Do not redesign an approved concept merely because another layout is easier to implement.

If implementation is also requested, first produce or recover an approved Design Contract, then hand the implementation problem to the project's LDLib2 guidance (for example `$ldlib2-ui` if available).

## First question: what fantasy is the player performing?

Before proposing a layout, identify the interaction fantasy in one sentence.

Examples of the *kind* of answer, not fixed templates:
- operating a fabrication machine and watching matter become an object
- modifying a complex tool through an engineering schematic
- navigating through physical space rather than filling out a form
- communicating with a shipboard AI rather than using a chat app

The visual hierarchy must support that fantasy.

## Classify the screen

Choose one of these broad modes. Read `references/screen-archetypes.md` when the distinction affects the design.

### Utility UI
Use for simple, repetitive operations where clarity and speed dominate.
Keep it compact. Prefer common components and minimal bespoke art.

### Feature UI
Use for signature machines and systems that deserve a memorable visual center.
Require a clear primary visual. Allow bespoke texture art, custom drawing, animation, or hybrid UI.

### Experience UI
Use when the interaction itself is spatial, cinematic, or exploratory.
Treat the experience as the UI. Start from space, objects, motion, selection, and navigation—not from panels and cards.

## Design workflow

Follow this order unless the user explicitly supplies an approved design.

1. **State the purpose.** What does the player come here to accomplish?
2. **State the fantasy.** What fictional device/action should this feel like?
3. **Choose the primary visual.** What should the eye land on first?
4. **Rank information.** List information from most to least important.
5. **Map the interaction flow.** Describe open → inspect → act → feedback → completion/error.
6. **Choose spatial organization.** Arrange content to support the primary visual and flow; do not default to a dashboard.
7. **Separate art from generic UI.** Decide what deserves dedicated art/custom rendering versus ordinary controls.
8. **Define motion and feedback.** Animation must express state changes, progress, targeting, or device behavior.
9. **Run the anti-generic review.** Read `references/review-checklist.md` for substantial new designs.
10. **Produce the Design Contract.** Use the format below.

When relevant, consult:
- `references/visual-language.md` for the project visual language
- `references/screen-archetypes.md` for screen mode and page-specific direction
- `references/asset-guidelines.md` for pixel art, textures, icons, holograms, and resolution
- `references/review-checklist.md` for critique and anti-dashboard review

## Design Contract output

For a new or substantially redesigned screen, provide this contract before implementation:

```text
SCREEN
<name>

PURPOSE
<what the player accomplishes>

INTERACTION FANTASY
<what fictional action/device this should feel like>

SCREEN MODE
Utility / Feature / Experience

PRIMARY VISUAL
<the first and strongest visual signal>

INFORMATION PRIORITY
1. ...
2. ...
3. ...

INTERACTION FLOW
Open → ... → action → feedback → completion/error

SPATIAL ORGANIZATION
<major regions and why they exist>

ART / CUSTOM VISUALS
<textures, pixel art, custom drawing, shaders/effects if truly useful>

GENERIC UI
<ordinary labels, rows, buttons, slots, scroll areas that do not need bespoke art>

MOTION / FEEDBACK
<only meaningful animation and state feedback>

STATES
<normal / hover / selected / disabled / warning / fault / in-progress / complete as applicable>

DO NOT
<screen-specific failure modes>

IMPLEMENTATION CONTRACT
Do not change:
- <approved design invariants>

Implementation may decide:
- <technical details such as container hierarchy, layout mechanics, event binding, data sync>
```

Keep the contract concrete enough that an implementation agent cannot silently turn the design into a generic three-column application.

## StarboundMC visual identity

Use the established mother language unless a specific fictional device gives a good reason to deviate:
- deep navy / graphite environment
- titanium / muted blue-gray structure
- cyan for navigation, data, selection, normal digital interaction
- amber for fuel, cost, thermal/energy warning, resource pressure
- green primarily for engineering blueprint, ready/completion, upgrade states
- red only for genuine faults, dangerous states, or hard failure
- sparse glow; active/selected elements should normally be brighter than decoration
- hard edges, cut corners, or very small radii for physical ship equipment
- pixel-art readability and strong silhouettes where pixel assets are involved

Do not turn this palette into a rigid skin. Different systems may speak the same visual language with different accents.

## Existing UI is evidence, not authority

Inspect existing StarboundMC screens to understand interaction constraints, data, and established motifs.
Do not copy an existing screen's layout merely because it exists.
Do not assume current LSS classes, current machine panels, or current LDLib2 component trees represent the desired final design.

Preserve strong established motifs when they serve the product identity. Replace weak patterns when the task is explicitly a redesign.

## Anti-framework rule

A design must not become more generic merely to fit the UI framework.

When a distinctive concept is better represented by:
- a texture-backed frame
- a blueprint or schematic
- an animated object preview
- a custom-rendered map
- a hologram layer
- transparent hit regions over art
- a bespoke `UIElement`

then keep that concept in the Design Contract and let the implementation layer solve it.

## Review mode

When asked to critique an existing screenshot, screen, LSS, or layout:

1. Identify the intended player action.
2. Identify the current primary visual (or the absence of one).
3. Explain the top 3 hierarchy/interaction problems before discussing polish.
4. Separate structural problems from art polish problems.
5. Flag generic dashboard/card nesting when it weakens the fiction.
6. Preserve successful existing motifs.
7. Propose the smallest structural changes that create the largest improvement.

Do not recommend adding decoration as a substitute for hierarchy.

## Guardrails against over-design

Do not make every machine a hero screen.
Do not add animation simply because it is possible.
Do not require bespoke assets for ordinary inventory-like workflows.
Do not force asymmetry, sci-fi decoration, or holograms onto a screen that works better as a simple utility.
Do not use visual novelty at the expense of GUI Scale 2/3/4 readability.

The target is not maximum visual complexity. The target is a recognizable, coherent StarboundMC identity with clear interaction.
