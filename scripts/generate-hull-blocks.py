"""Original 16px hull-block textures for the rocky-moon mining outposts.

Run from any directory with Python/Pillow. Writes only the six hull-block
textures plus their blockstate/model/item/loot definitions.

The palette deliberately continues the mod's existing machine language
(see ship_engine_unit / voxel_*): blue-grey metal, near-black panel seams,
pale rivets, cyan instrument accents and a muted amber warning tone. That
keeps a surface outpost reading as the same civilisation as the ship.

Lighting convention, applied everywhere: light falls from the top-left, so
a bevel puts a highlight on its top and left edges and a shadow on its
bottom and right. Without that every plate reads flat.

Transparency: the window's pane and the grate's openings are genuinely
see-through. Alpha alone is not enough — a block also has to declare a
render type, which in NeoForge 1.21.1 means a `render_type` key in the block
model JSON (resolved through NamedRenderTypeManager, so the value is a
render-type name such as `minecraft:cutout`). Blocks listed in RENDER_TYPES
get that key written for them; everything else stays solid.
"""
import argparse
import json
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/starboundmc'
DATA = ROOT / 'src/main/resources/data/starboundmc'
PREFIX = 'hull'

# Shared palette. The ramp values are sampled from the mod's existing machine
# art (ship_engine_unit_*, voxel_machine_*, ship_door) rather than invented, so
# the hull set sits in the same value range as everything already shipped. Those
# textures are dark-dominant: a near-black base carrying most of the pixels,
# two or three mid tones forming the raised geometry, and a small number of
# bright pixels as highlights. Matching that distribution is what stops these
# plates reading as washed-out next to the ship.
D1 = '#111c26'      # outline / deepest seam
D2 = '#1d2b34'      # recessed shadow
M1 = '#2c3d47'      # dark metal
M2 = '#3e525d'      # plate base
M3 = '#4f6672'      # raised face
M4 = '#617782'      # lit metal
M5 = '#7d949d'      # lit edge
L1 = '#a1b4bd'      # highlight
L2 = '#d5dfdc'      # specular / bolts
CYAN_D = '#13515c'  # instrument recess
CYAN = '#41bbc6'    # instrument accent
CYAN_HI = '#87ece8' # emissive cyan
AMBER_D = '#8e6747' # warm shadow
AMBER = '#d79a3d'   # warning / warm lamp
AMBER_HI = '#ffe0a0'  # warm highlight

# Fully transparent, but tinted to the pane colour so mipmap downscaling does
# not bleed a dark halo around the opening.
CLEAR_TEAL = (19, 81, 92, 0)

BLOCKS = ('hull_plating', 'reinforced_hull', 'hull_window',
          'industrial_light', 'hull_hazard', 'hull_grate')

# Blocks whose model must declare a non-solid render type. Values are render
# type names resolved via NeoForge's NamedRenderTypeManager.
RENDER_TYPES = {
    # A tinted pane: partial alpha blends with what is behind the glass.
    'hull_window': 'minecraft:translucent',
    # Openings are fully see-through, which is what cutout means.
    'hull_grate': 'minecraft:cutout',
}


def noise(im, amount=3, seed=0):
    """Deterministic per-pixel grain, weighted towards lit surfaces.

    The existing machine art carries scatter over its metal rather than large
    flat fills, so some grain is part of the read. But grain applied uniformly
    speckles the recessed seams and shadow cavities too, which reads as a dirty
    or noisy texture rather than as clean machined metal. Scaling the amount by
    the pixel's own brightness keeps grain on the lit faces and leaves the dark
    structure dark and legible.
    """
    px = im.load()
    for y in range(16):
        for x in range(16):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            luma = (r * 299 + g * 587 + b * 114) // 1000
            # 0 below ~40 luma, ramping to full by ~110.
            weight = max(0, min(100, luma - 40)) / 100.0
            n = int(round(((x * 17 + y * 7 + x * y + seed * 13) % 7 - 3)
                          * amount / 3.0 * weight))
            if n == 0:
                continue
            px[x, y] = (max(0, min(255, r + n)), max(0, min(255, g + n)),
                        max(0, min(255, b + n)), a)


def bolts(d, inset=1, dark=D1, lit=L2):
    """Corner fasteners: a bright pip over a dark seat, as the ship art does."""
    for x, y in ((inset, inset), (15 - inset, inset),
                 (inset, 15 - inset), (15 - inset, 15 - inset)):
        d.point((x, y), fill=lit)
        d.point((x + 1 if x < 8 else x - 1, y), fill=dark)


def frame(d, box, light=M5, shadow=D1):
    """Outline a box: lit on top/left, dark on bottom/right."""
    x0, y0, x1, y1 = box
    d.line((x0, y0, x1, y0), fill=light)
    d.line((x0, y0, x0, y1), fill=light)
    d.line((x0, y1, x1, y1), fill=shadow)
    d.line((x1, y0, x1, y1), fill=shadow)


def hull_plating():
    """Riveted wall plate: two offset courses over a recessed dark seam."""
    im = Image.new('RGBA', (16, 16), D1)
    d = ImageDraw.Draw(im)
    # Recessed backing, visible in the seam and around the plate edges.
    d.rectangle((1, 1, 14, 14), fill=D2)
    # Upper course, laid slightly proud.
    d.rectangle((1, 1, 14, 6), fill=M2)
    d.rectangle((2, 2, 13, 5), fill=M3)
    noise(im, 3)
    frame(d, (1, 1, 14, 6), light=M5, shadow=D1)
    d.line((2, 2, 13, 2), fill=M4)
    # Lower course, set back a step and one shade darker, so the wall reads as
    # overlapping plates rather than a printed pattern.
    d.rectangle((1, 9, 14, 14), fill=M1)
    d.rectangle((2, 10, 13, 13), fill=M2)
    frame(d, (1, 9, 14, 14), light=M3, shadow=D1)
    # Recessed seam between the courses, with light catching its upper lip.
    d.line((1, 7, 14, 7), fill=D1)
    d.line((1, 8, 14, 8), fill=D2)
    # Staggered fastener pattern, the detail that makes a plated wall read.
    for x in (3, 11):
        d.point((x, 3), fill=D1)
        d.point((x, 4), fill=D1)
    for x in (5, 9):
        d.point((x, 11), fill=D1)
        d.point((x, 12), fill=D1)
    bolts(d, inset=1)
    return im


def reinforced_hull():
    """Load-bearing rib wall: proud ribs over a dark recessed field."""
    im = Image.new('RGBA', (16, 16), D2)
    d = ImageDraw.Draw(im)
    # Dark recessed field behind the ribs.
    d.rectangle((1, 1, 14, 14), fill=D1)
    noise(im, 2)
    # Three ribs, each a lit face with a shadowed right flank so it stands off
    # the plate. Gaps between them stay dark, which is what gives the depth.
    for x in (2, 7, 12):
        d.rectangle((x, 1, x + 1, 14), fill=M3)
        d.line((x, 1, x, 14), fill=M5)
        d.line((x + 1, 1, x + 1, 14), fill=M1)
    # Horizontal capping bands top and bottom, tying the ribs together.
    for (y0, y1) in ((1, 2), (13, 14)):
        d.rectangle((1, y0, 14, y1), fill=M2)
        frame(d, (1, y0, 14, y1), light=M4, shadow=D1)
    # Bolt rows along the caps.
    for x in (4, 9):
        d.point((x, 1), fill=L2)
        d.point((x, 14), fill=L2)
    frame(d, (0, 0, 15, 15), light=M4, shadow=D1)
    return im


def hull_window():
    """Armoured porthole: dark opaque frame around a genuinely clear pane.

    The pane is transparent, not painted — an earlier version drew an opaque
    tinted rectangle, so the block rendered as a solid slab and read as a metal
    panel with a blue sticker rather than a window. The frame stays fully opaque
    so the block is still legible from any angle, and the pane carries a little
    tinted glass and two specular streaks: a completely empty pane reads as
    missing geometry rather than as glass.
    """
    im = Image.new('RGBA', (16, 16), D1)
    d = ImageDraw.Draw(im)
    # Outer frame, bevelled: lit top/left, dark bottom/right.
    d.rectangle((0, 0, 15, 15), fill=M2)
    frame(d, (0, 0, 15, 15), light=M5, shadow=D1)
    d.rectangle((1, 1, 14, 14), fill=D2)
    frame(d, (1, 1, 14, 14), light=M4, shadow=D1)
    # Recessed inner bezel, darker than the frame so the pane sits in a well.
    d.rectangle((2, 2, 13, 13), fill=D1)
    # Cut the pane clear. Everything before this is opaque structure.
    d.rectangle((3, 3, 12, 12), fill=CLEAR_TEAL)
    # Tinted glass, partial alpha, kept to one corner so most of the pane still
    # transmits the view behind it.
    d.rectangle((3, 3, 8, 6), fill=(19, 81, 92, 150))
    d.rectangle((10, 10, 12, 12), fill=(19, 81, 92, 110))
    # A diagonal specular sweep down the glass, two pixels thick and stepped so
    # it stays a clean line on a 16px grid. This is the cue that reads as
    # reflection; without it a clear pane looks like missing geometry.
    for i in range(0, 7):
        d.point((3 + i, 3 + i), fill=CYAN_HI)
        if 3 + i + 1 <= 12:
            d.point((3 + i + 1, 3 + i), fill=CYAN)
    # Frame bolts, seated on the lit corners.
    bolts(d, inset=1, lit=L2, dark=D1)
    return im


def industrial_light():
    """Housed luminaire: dark fixture, stepped lens, hot core."""
    im = Image.new('RGBA', (16, 16), D1)
    d = ImageDraw.Draw(im)
    # Fixture shell, one shade off the wall plate so it reads as an added box.
    d.rectangle((1, 1, 14, 14), fill=M1)
    frame(d, (1, 1, 14, 14), light=M4, shadow=D1)
    # Lens well, recessed and dark.
    d.rectangle((3, 3, 12, 12), fill=CYAN_D)
    # Stepped lens: each ring a step brighter, glowing outwards from centre.
    # Concentric rings only — no cage bars. Bars across the face break the
    # bright centre into separate segments, which reads as a twin-tube fitting
    # rather than one luminaire.
    d.rectangle((4, 4, 11, 11), fill=CYAN)
    d.rectangle((5, 5, 10, 10), fill=CYAN_HI)
    d.rectangle((6, 6, 9, 9), fill='#e8fffb')
    # One lit rim per step, so each ring boundary is visible as a step in
    # brightness rather than a flat stack of squares.
    frame(d, (4, 4, 11, 11), light='#c9fbf6', shadow=CYAN_D)
    frame(d, (6, 6, 9, 9), light='#ffffff', shadow=CYAN_HI)
    bolts(d, inset=2, lit=L1, dark=D1)
    return im


def hull_hazard():
    """Hazard banding: warm/dark diagonals with a lit leading edge."""
    im = Image.new('RGBA', (16, 16), D2)
    d = ImageDraw.Draw(im)
    d.rectangle((1, 1, 14, 14), fill=M1)
    # Diagonal bands. Each gets a lit leading edge and a shaded trailing edge,
    # so the stripe has relief instead of being flat colour.
    for i in range(-16, 32, 8):
        d.polygon([(i, 15), (i + 4, 15), (i + 12, 0), (i + 8, 0)], fill=AMBER)
        d.line((i, 15, i + 8, 0), fill=AMBER_HI)
        d.line((i + 4, 15, i + 12, 0), fill=AMBER_D)
    # Corner wear, so the plate is not a perfect machine stripe.
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
        d.point((x, y), fill=D1)
    frame(d, (0, 0, 15, 15), light=M5, shadow=D1)
    bolts(d, inset=1, lit=L1, dark=D1)
    return im


def hull_grate():
    """Walkable grating: a grid of genuinely open cells between load-bearing bars.

    A two-axis grid rather than horizontal louvers: this block is used as a
    floor (landing-pad cross, camp decking, plant working floor), and a louvered
    face reads as a vent panel seen side-on, not as something you walk on.

    Like the window, an earlier version painted the voids dark, so the block
    rendered solid and read as a dark plate rather than something you can see
    through. Only the structural bars are drawn; every cell is clear.
    """
    im = Image.new('RGBA', (16, 16), CLEAR_TEAL)
    d = ImageDraw.Draw(im)
    # Bars run on both axes at 2px, leaving a 2x2 grid of 5x5 open cells.
    bars = (0, 1, 7, 8, 14, 15)
    for x in bars:
        d.line((x, 0, x, 15), fill=M2)
    for y in bars:
        d.line((0, y, 15, y), fill=M2)
    # Lit top and left edges, dark bottom and right, so each bar has thickness
    # and the grid reads as recessed rather than printed.
    for x in bars:
        d.line((x, 0, x, 15), fill=M3)
    for y in bars:
        d.line((0, y, 15, y), fill=M4)
    d.line((0, 15, 15, 15), fill=D1)
    d.line((15, 0, 15, 15), fill=D1)
    for y in (1, 8, 15):
        d.line((0, y, 15, y), fill=D1)
    for x in (1, 8, 15):
        d.line((x, 0, x, 15), fill=D1)
    # Corner bolts sit on the intersections, which are solid in every variant.
    for x, y in ((0, 0), (14, 0), (0, 14), (14, 14)):
        d.point((x, y), fill=L2)
    return im


TEXTURES = {
    'hull_plating': hull_plating,
    'reinforced_hull': reinforced_hull,
    'hull_window': hull_window,
    'industrial_light': industrial_light,
    'hull_hazard': hull_hazard,
    'hull_grate': hull_grate,
}


def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + '\n', encoding='utf-8')


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--preview', type=Path)
    args = parser.parse_args()

    images = {}
    for name, factory in TEXTURES.items():
        im = factory()
        images[name] = im
        im.save(ASSETS / f'textures/block/{name}.png')
        write_json(ASSETS / f'blockstates/{name}.json',
                   {'variants': {'': {'model': f'starboundmc:block/{name}'}}})
        model = {'parent': 'minecraft:block/cube_all',
                 'textures': {'all': f'starboundmc:block/{name}'}}
        if name in RENDER_TYPES:
            # Without this the block is baked into the solid chunk layer and
            # any transparent texels render as opaque black.
            model['render_type'] = RENDER_TYPES[name]
        write_json(ASSETS / f'models/block/{name}.json', model)
        write_json(ASSETS / f'models/item/{name}.json',
                   {'parent': f'starboundmc:block/{name}'})
        write_json(DATA / f'loot_table/blocks/{name}.json', {
            'type': 'minecraft:block',
            'pools': [{'rolls': 1,
                       'entries': [{'type': 'minecraft:item', 'name': f'starboundmc:{name}'}],
                       'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})

    if args.preview:
        scale = 12
        tile = scale * 4
        canvas = Image.new('RGBA', (tile * len(images) + 40, tile + 96), '#101923')
        draw = ImageDraw.Draw(canvas)
        draw.text((20, 16), 'ROCKY MOON HULL SET / outpost building blocks',
                  fill='#d3e7ec', font_size=22)
        draw.text((20, 46), 'Original 16px textures  |  asset inspection preview',
                  fill='#8cabb8', font_size=14)
        for i, (name, im) in enumerate(images.items()):
            big = im.resize((tile, tile), Image.Resampling.NEAREST)
            canvas.paste(big, (20 + i * tile, 76))
            draw.rectangle((20 + i * tile, 76, 19 + i * tile + tile, 75 + tile), outline='#2b3d47')
            draw.text((20 + i * tile, 80 + tile), name, fill='#8cabb8', font_size=11)
        args.preview.parent.mkdir(parents=True, exist_ok=True)
        canvas.convert('RGB').save(args.preview)

    print(f'Wrote {len(images)} original 16px hull textures with blockstate/model/item/loot.')


if __name__ == '__main__':
    main()
