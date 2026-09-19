# UI review checklist

Use this for substantial redesigns and critique. Do not mechanically reject a design because one item is true; use the questions to locate accidental generic-UI patterns.

## Hierarchy

- Is there a clear first visual target?
- Does that target correspond to the core player action/fantasy?
- Can the player tell what is actionable without reading every label?
- Are status, warnings, and disabled states semantically distinct?
- Does the screen still make sense at Minecraft GUI Scale 2/3/4?

## Anti-generic / anti-dashboard smell test

Ask:
- Is every logical group inside another visible rectangle?
- Are there cards inside cards without a real grouping reason?
- Are most elements rounded rectangles simply because they are easy to style?
- Did the design default to left-list / center-detail / right-status because it resembles desktop software?
- Are all regions given equal visual weight?
- Is the screen explained mainly through labels rather than through visual structure?
- Is cyan glow used everywhere, destroying hierarchy?
- Could I rename the sections to `Customers / Revenue / Tasks / Status` and still have a believable SaaS dashboard?

If the last answer is yes, revisit the fictional device/interaction before adding polish.

## Art versus structure

Separate these diagnoses:

### Structural issue
Examples:
- wrong primary visual
- wrong information hierarchy
- confusing action flow
- list dominates an experience that should center on an object
- permanent chrome steals space from the core interaction

Fix structure first.

### Art/polish issue
Examples:
- weak icon silhouette
- poor palette balance
- inconsistent pixel density
- weak state animation
- bland frame texture

Do not use art polish to hide a structural problem.

## Restraint

- Does every animation communicate state, progress, targeting, or device behavior?
- Does every glow indicate importance?
- Does every bespoke texture carry identity or hierarchy?
- Could a simple utility control remain simple instead?

## Existing-system review

When redesigning existing UI:
- preserve successful interaction behavior unless change is justified
- preserve strong project motifs
- do not preserve weak layout only because code already exists
- do not treat current LSS classes as product requirements
- distinguish migration cost from design quality

## Final test

A strong StarboundMC screen should answer, in roughly this order:
1. What am I operating?
2. What is happening now?
3. What can I do next?
4. What will it cost / require?
5. What changed after I acted?
