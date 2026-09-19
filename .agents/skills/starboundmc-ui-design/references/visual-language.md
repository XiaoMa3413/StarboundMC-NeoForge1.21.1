# StarboundMC visual language

This file describes the current project-wide visual language. It is a shared vocabulary, not a rigid skin.

## Product character

Keywords:
- deep space
- shipboard terminal
- engineered equipment
- pixel sci-fi
- restrained glow
- clear interaction

The player should feel that they are operating equipment inside a spacecraft, not opening a modern web dashboard.

A useful blend is:
- Minecraft-scale world interaction
- Starbound-like pixel readability and identity
- Subnautica-like device interaction and feedback
- light aerospace / avionics information hierarchy

Avoid drifting into:
- neon cyberpunk everywhere
- glassmorphism
- mobile-app gradients
- SaaS dashboards
- excessive rounded cards
- decorative RGB glow
- ultra-thin modern web typography

## Core palette

Use these as anchors, not mandatory exact values for every pixel.

| Role | Anchor | Meaning |
|---|---|---|
| Deep space | `#050912` | space / full-screen background |
| Ship dark | `#0B151D` | primary equipment background |
| Graphite blue | `#101E28` | secondary surface |
| Titanium dark | `#192D37` | structure / headers |
| Muted cyan-gray | `#304852` | borders / dividers |
| Primary cyan | `#63E2DF` | navigation / data / normal interaction |
| Light cyan-white | `#D8EDF0` | primary text |
| Secondary blue-gray | `#85A5B0` | secondary text |
| Amber | `#E2B66F` | fuel / cost / energy pressure / warning |
| Engineering green | `#83E3A2` | blueprint / ready / upgrade / completion |
| Fault red | `#E46B72` | real fault / danger / hard failure |

## Semantic color rules

Cyan is the default digital ship language.
Amber is not decoration; it should imply energy, fuel, resource cost, warning, or thermal pressure.
Green should stay concentrated in engineering/blueprint/ready/completion contexts.
Red should be rare enough that it still means something serious.

## Geometry

Physical machine interfaces:
- hard edge, cut edge, or micro-radius
- stronger structural framing
- fewer floating glass cards

Purely digital/spatial interfaces such as the star map may be lighter, more floating, and less physically framed.

## Glow

Treat glow as a scarce hierarchy resource.

Brightness priority should usually climb roughly as:
background → structure → text → interaction → selected → critical state.

Do not make every cyan element glow.

## Typography

Prefer a small number of clear levels. For existing LDLib2-era screens, a practical target is often roughly 6 / 7 / 9 logical-size tiers, adjusted when needed for readability.

Use spacing, alignment, and contrast before adding another box.

## Panel discipline

Avoid visible five-level nesting of rectangles.

Prefer:
- whitespace
- alignment
- thin dividers
- change in tone/value
- selective framing only when it communicates grouping, interaction, or state

Rule of thumb: if a container exists only because the implementation wanted another `UIElement`, it probably should not be visible.
