# Voxel Printing Station — UI Design Contract

## SCREEN TYPE
Feature UI.

## SCREEN PURPOSE
Choose a printable blueprint, verify resources, set quantity, submit work, and observe the public FIFO print queue.

## PLAYER FANTASY
The player is operating a wall-mounted matter fabricator. Selecting a recipe loads a fabrication blueprint into a chamber; submitting work commits resources and the device begins materializing the object.

## PRIMARY VISUAL
The fabrication chamber and the object currently being previewed/printed. Recipe browsing, requirements and queue management are supporting instrumentation.

## INFORMATION PRIORITY
1. Object being fabricated / previewed.
2. Whether the selected print can be submitted.
3. Missing materials and voxel/resource requirements.
4. Quantity.
5. Current machine/queue progress.
6. Recipe browsing and requester metadata.

## INTERACTION FLOW
Browse recipe → blueprint appears in fabrication chamber → inspect requirements → adjust quantity → submit → receive accepted/rejected feedback → chamber/queue show progress → collect real output from the vanilla-owned output slot.

## SPATIAL ORGANIZATION
Wide: recipe navigator left, fabrication chamber center, queue rail right, unchanged player inventory below. Compact mode keeps the existing recipe/queue tab switch.

## ART / CUSTOM VISUALS
Dark graphite ship shell, cyan fabrication chamber, subtle engineering grid, progress-driven scan line, restrained corner registration marks, cyan recipe-selection rail, amber only for shortages/warnings.

## MOTION / FEEDBACK
During active printing, the chamber scan line moves vertically using the existing synchronized progress value. No idle pulsing or decorative particles.

## DO NOT
Do not return to three equally weighted dashboard columns. Do not make the queue stronger than the object being fabricated. Do not move or duplicate server-authoritative slots. Do not change queue ownership, refund, reservation, wallet, recipe or network behavior.

## IMPLEMENTATION CONTRACT
Do not change `PANEL_W = 440`, `COMPACT_W = 320`, `PANEL_H = 240`, real output/player slot ownership and coordinates, FIFO queue semantics, requester cancellation rules, compact queue-tab behavior or server authority.
