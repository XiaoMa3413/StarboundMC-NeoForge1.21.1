# Asset and pixel-art guidelines

## Principle

Use dedicated art when the art is carrying identity, fiction, or hierarchy.
Use generic UI primitives where the element is merely functional.

A distinctive machine should not be reduced to colored rectangles solely because rectangles are easy to implement.

## Logical pixel scale

Useful project targets:
- ordinary inventory item: 16–32 logical pixels
- signature weapon/tool icon: around 32 logical pixels
- machine UI icon: 16–24 logical pixels
- star-map body sprite: roughly 48–96 logical pixels
- N.O.V.A. portrait/body-class assets: roughly 96×112 scale where appropriate
- world/3D planet surface textures: may remain 2K–4K

These are not hard limits. Maintain consistent apparent pixel density.

## Pixel art

Prioritize in this order:
1. silhouette
2. proportion
3. large color groups
4. functional structure
5. small accents

Prefer hard pixel edges and nearest-neighbor scaling where the asset is intended to read as pixel art.
Do not create a detailed high-resolution painting and simply shrink it to simulate pixel art.
Avoid random one-pixel noise, excessive scratches, tiny bolts, and anti-aliased micro-detail.

## Matter Manipulator

Treat it as a major project symbol.
Preserve a strong side-view silhouette and major structural relationships before adding detail.
Technology should read from believable structure, major color groups, and energy components—not from an accumulation of tiny lines.

## Star-map bodies

World planet assets and UI map sprites do not need to be the same representation.
World planets may use high-resolution spherical textures and lighting.
Map sprites should emphasize instant type recognition, silhouette, atmospheric/ring/crater motifs, and strong pixel grouping.

## Texture-backed UI

Good candidates:
- machine housings or bespoke frames
- engineering blueprints
- hologram grids
- scan lines / manufacturing masks
- decorative structural plates that reinforce device fiction

Do not use texture art to bake text, dynamic numbers, or interaction state that should remain live.

## Hybrid UI

A strong feature screen may combine:
- one or two identity-carrying textures
- custom-drawn effects
- transparent or minimally visible interactive hit areas
- normal labels/buttons/lists for secondary operations

This is often better than forcing the whole screen into generic widgets.
