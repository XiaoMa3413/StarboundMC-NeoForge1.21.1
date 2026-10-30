# StellarView-derived cinematic star core

Source: Povstalec/StellarView (Copyright (c) 2024 Povstalec, MIT), customized in XiaoMa3413/StellarView at `f55e85f` (`0.5.4-alpha-starbound-hdr2-NeoForge`). The historical source delta is retained in `patches/stellarview/starbound-hdr.patch`. Full original notice: [LICENSE.txt](LICENSE.txt), included in the binary JAR at `META-INF/licenses/stellarview/LICENSE.txt`.

## Extraction

- `StarFieldRenderer.generateStars`, `StarInfo.randomStarType`, `StarData.LOD.newStar`, and `SphericalCoords`: adapted into `client/space/BackgroundStarCatalog.java`. Preserve Java Random seed + 1 for the current LOD2 field, RNG order, inclusive weight boundary, uniform-volume sphere, and six existing cinematic types. This specialization validates a single LOD2 profile (max visible distance in [5M,10M)); it does not implement the upstream galaxy/constellation framework.
- `StarData.getInstancedStars`, HDR2 `rendertype_star_tex_instanced.vsh` and `.fsh`: adapted into `BackgroundStarRenderer.java` and `background_stars.vsh` / `.fsh`. Preserve textured-mode size factor four, distance scaling, minimum pixel radius, area compensation, HDR scale 0.7×6.4, radial Gaussian and warp convergence.
- Project-owned integration uses static expanded quads and GLSL 150 instead of upstream instancing/custom vertex usages. The source radial profile is invariant under rotation in its tangent plane; fixed quad rotation therefore preserves it. CPU still consumes the rotation random draw. Pole azimuth is defined explicitly. Shader positions are rebased from UniversePosition in double precision, with bounded frame offsets, and resources are released on reload/disconnect.

The existing `assets/starboundmc/stellarview/effects/star_info/cinematic_stars.json` identity is retained. Its directory name does not require or load the StellarView mod. No upstream textures are needed: HDR2 uses the procedural profile. No upstream mod bootstrap, config, registry, reflection bridge or runtime dependency is shipped.

The source's small-star profile is point-sampled, not pixel-area integrated. Preserve its actual response and validate it honestly; do not claim the retired analytic renderer's subpixel flux guarantee. GPU checks cover radiance/visibility/depth and resolved-profile area; continuous camera motion remains an independent acceptance item.
