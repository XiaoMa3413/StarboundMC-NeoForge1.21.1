"""Original beacon-emitter block: model plus 16px textures.

The rocky moon's landing pad and comms tower both used a ship engine block as
their emitter, which read oddly — a repairable ship component standing on a
derelict mining claim. This replaces it with a purpose-built surface beacon:
a bolted collar, a ribbed housing with an emissive signal band, and a glowing
emitter face on top.

Run from any directory with Python/Pillow. Writes the three textures plus the
blockstate, block/item models and loot table.
"""
import argparse
import importlib.util
import json
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]

# The palette, grain and bolt/frame helpers are loaded from the hull generator
# rather than duplicated, so the beacon cannot drift away from the set it stands
# next to. Both scripts write into the same resource tree and must agree on the
# value ramp.
_HULL_PATH = Path(__file__).resolve().parent / 'generate-hull-blocks.py'
_spec = importlib.util.spec_from_file_location('hull_blocks', _HULL_PATH)
_hull = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_hull)

ASSETS = _hull.ASSETS
DATA = _hull.DATA

NAME = 'beacon_emitter'

# Same sampled ramp as the hull set; see that module for where these come from.
D1, D2 = _hull.D1, _hull.D2
M1, M2, M3, M4, M5 = _hull.M1, _hull.M2, _hull.M3, _hull.M4, _hull.M5
L1, L2 = _hull.L1, _hull.L2
AMBER_D, AMBER, AMBER_HI = _hull.AMBER_D, _hull.AMBER, _hull.AMBER_HI
CORE = '#fffdf5'      # emitter hot centre
noise = _hull.noise
bolts = _hull.bolts
frame = _hull.frame


def base_texture():
    """Bolted collar at the foot of the mast head."""
    im = Image.new('RGBA', (16, 16), D2)
    d = ImageDraw.Draw(im)
    # Machined step ring over a recessed field, so the collar reads as a forged
    # fitting rather than a flat plate.
    d.rectangle((0, 0, 15, 15), fill=D1)
    d.rectangle((2, 2, 13, 13), fill=M2)
    d.rectangle((3, 3, 12, 12), fill=M3)
    noise(im, 3)
    frame(d, (2, 2, 13, 13), light=M5, shadow=D1)
    d.rectangle((5, 5, 10, 10), fill=M1)
    frame(d, (5, 5, 10, 10), light=M2, shadow=D1)
    bolts(d, inset=1, lit=L2, dark=D1)
    return im


def side_texture():
    """Ribbed housing carrying the emissive signal band."""
    im = Image.new('RGBA', (16, 16), D1)
    d = ImageDraw.Draw(im)
    # Housing wall, set in from the edges so the ribs stand proud of it.
    d.rectangle((3, 0, 12, 15), fill=M2)
    d.rectangle((4, 1, 11, 14), fill=M3)
    noise(im, 3)
    # Stiffening ribs down both flanks, lit on the inner edge.
    for x in (1, 2, 13, 14):
        d.line((x, 0, x, 15), fill=M2)
    d.line((2, 0, 2, 15), fill=M4)
    d.line((13, 0, 13, 15), fill=M1)
    # Signal band spanning the full face: the part that has to read from a
    # distance, so it is not tucked into a small window. Lit top edge, shaded
    # bottom, so it looks like an inset lamp rather than a painted stripe.
    d.rectangle((0, 6, 15, 9), fill=AMBER_D)
    d.rectangle((1, 7, 14, 8), fill=AMBER)
    d.line((1, 7, 14, 7), fill=AMBER_HI)
    d.line((1, 9, 14, 9), fill=D1)
    # Rivets on the flanks, clear of the band.
    for x in (1, 14):
        d.point((x, 3), fill=L1)
        d.point((x, 12), fill=L1)
    return im


def lens_texture():
    """Emitter face: concentric rings fading from a hot centre.

    Four bands rather than two, so the face reads as light falling off outwards
    instead of a flat amber square with a white dot in the middle. Only the
    outer band gets a drawn rim: framing every band turned the face into a
    mechanical cross rather than a light source.
    """
    im = Image.new('RGBA', (16, 16), D1)
    d = ImageDraw.Draw(im)
    d.rectangle((1, 1, 14, 14), fill=AMBER_D)
    d.rectangle((2, 2, 13, 13), fill=AMBER)
    d.rectangle((4, 4, 11, 11), fill=AMBER_HI)
    d.rectangle((6, 6, 9, 9), fill=CORE)
    # A lit rim on the outermost band only, tying the emitter to the housing.
    frame(d, (1, 1, 14, 14), light=AMBER_HI, shadow=AMBER_D)
    # Corner brackets tie the emitter back to the housing palette.
    for x, y in ((0, 0), (13, 0), (0, 13), (13, 13)):
        d.rectangle((x, y, x + 2, y + 2), outline=D1)
        d.point((x + 1, y + 1), fill=L1)
    return im


TEXTURES = {
    f'{NAME}_base': base_texture,
    f'{NAME}_side': side_texture,
    f'{NAME}_lens': lens_texture,
}


def block_model():
    """Stepped mast head: collar, ribbed housing, glowing cap."""
    return {
        'parent': 'minecraft:block/block',
        'ambientocclusion': True,
        'textures': {
            'base': f'starboundmc:block/{NAME}_base',
            'side': f'starboundmc:block/{NAME}_side',
            'lens': f'starboundmc:block/{NAME}_lens',
            'particle': f'starboundmc:block/{NAME}_side',
        },
        'elements': [
            {
                'from': [0, 0, 0], 'to': [16, 3, 16],
                'faces': {
                    'north': {'uv': [0, 0, 16, 3], 'texture': '#base'},
                    'east': {'uv': [0, 0, 16, 3], 'texture': '#base'},
                    'south': {'uv': [0, 0, 16, 3], 'texture': '#base'},
                    'west': {'uv': [0, 0, 16, 3], 'texture': '#base'},
                    'up': {'uv': [0, 0, 16, 16], 'texture': '#base'},
                    'down': {'uv': [0, 0, 16, 16], 'texture': '#base', 'cullface': 'down'},
                },
            },
            {
                'from': [3, 3, 3], 'to': [13, 13, 13],
                'faces': {
                    'north': {'uv': [3, 3, 13, 13], 'texture': '#side'},
                    'east': {'uv': [3, 3, 13, 13], 'texture': '#side'},
                    'south': {'uv': [3, 3, 13, 13], 'texture': '#side'},
                    'west': {'uv': [3, 3, 13, 13], 'texture': '#side'},
                    'up': {'uv': [3, 3, 13, 13], 'texture': '#base'},
                    'down': {'uv': [3, 3, 13, 13], 'texture': '#base'},
                },
            },
            {
                'from': [2, 13, 2], 'to': [14, 16, 14],
                'faces': {
                    'north': {'uv': [2, 5, 14, 8], 'texture': '#lens'},
                    'east': {'uv': [2, 5, 14, 8], 'texture': '#lens'},
                    'south': {'uv': [2, 5, 14, 8], 'texture': '#lens'},
                    'west': {'uv': [2, 5, 14, 8], 'texture': '#lens'},
                    'up': {'uv': [2, 2, 14, 14], 'texture': '#lens'},
                    'down': {'uv': [2, 2, 14, 14], 'texture': '#base'},
                },
            },
        ],
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

    write_json(ASSETS / f'blockstates/{NAME}.json',
               {'variants': {'': {'model': f'starboundmc:block/{NAME}'}}})
    write_json(ASSETS / f'models/block/{NAME}.json', block_model())
    write_json(ASSETS / f'models/item/{NAME}.json',
               {'parent': f'starboundmc:block/{NAME}'})
    write_json(DATA / f'loot_table/blocks/{NAME}.json', {
        'type': 'minecraft:block',
        'pools': [{'rolls': 1,
                   'entries': [{'type': 'minecraft:item', 'name': f'starboundmc:{NAME}'}],
                   'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})

    if args.preview:
        scale = 12
        tile = scale * 4
        canvas = Image.new('RGBA', (tile * len(images) + 40, tile + 96), '#101923')
        draw = ImageDraw.Draw(canvas)
        draw.text((20, 16), 'BEACON EMITTER / rocky moon signal head',
                  fill='#d3e7ec', font_size=22)
        draw.text((20, 46), 'Original 16px textures  |  asset inspection preview',
                  fill='#8cabb8', font_size=14)
        for i, (name, im) in enumerate(images.items()):
            big = im.resize((tile, tile), Image.Resampling.NEAREST)
            canvas.paste(big, (20 + i * tile, 76))
            draw.rectangle((20 + i * tile, 76, 19 + i * tile + tile, 75 + tile),
                           outline='#2b3d47')
            draw.text((20 + i * tile, 80 + tile), name, fill='#8cabb8', font_size=11)
        args.preview.parent.mkdir(parents=True, exist_ok=True)
        canvas.convert('RGB').save(args.preview)

    print(f'Wrote {NAME}: {len(images)} textures, blockstate, block/item models, loot.')


if __name__ == '__main__':
    main()
