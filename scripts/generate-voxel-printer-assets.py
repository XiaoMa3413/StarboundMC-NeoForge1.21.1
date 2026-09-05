"""Build the shipboard printer model and its dedicated pixel-art atlas.

Run with Python and Pillow from any directory. No shared machine assets are changed.
Model coordinates are Minecraft pixels; atlas coordinates below are image pixels.
"""
import json
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/starboundmc'
ATLAS = ASSETS / 'textures/block/voxel_printing_station_atlas.png'
MODEL = ASSETS / 'models/block/voxel_printing_station.json'

REGIONS = {
    'armor': (0, 0, 32, 32), 'vent': (32, 0, 64, 32),
    'bed': (0, 32, 32, 48), 'chamber': (32, 32, 64, 48),
    'fascia': (0, 48, 32, 56), 'console': (32, 48, 48, 64),
    'dark': (48, 48, 56, 56), 'metal': (56, 48, 64, 56),
    'warning': (0, 56, 16, 64), 'cyan': (16, 56, 24, 64),
    'amber': (24, 56, 32, 64), 'rib': (48, 56, 64, 64),
}
SHELL, EDGE, HIGHLIGHT = '#a8b9bd', '#526870', '#d0dbd8'
DARK, PANEL, LINE = '#182b36', '#243d49', '#365662'
CYAN, PALE, AMBER = '#36bec9', '#b0eeeb', '#d9a44d'


def make_atlas():
    atlas = Image.new('RGB', (64, 64), DARK)
    def tile(name, color):
        x0, y0, x1, y1 = REGIONS[name]
        im = Image.new('RGB', (x1-x0, y1-y0), color)
        return im, ImageDraw.Draw(im), (x0, y0)
    def finish(im, pos):
        atlas.paste(im, pos)

    im, d, pos = tile('armor', SHELL)
    d.rectangle((0, 0, 31, 31), outline=EDGE)
    d.line((1, 1, 30, 1), fill=HIGHLIGHT)
    d.line((1, 1, 1, 30), fill='#bbcacc')
    d.line((2, 27, 29, 27), fill='#8a9fa5')
    d.rectangle((4, 5, 27, 24), outline='#8ea4aa')
    d.line((5, 6, 26, 6), fill='#bdcdcc')
    for x in (2, 28):
        for y in (2, 28):
            d.rectangle((x, y, x+1, y+1), fill=EDGE)
            d.point((x, y), fill='#e1e6dd')
    d.rectangle((7, 21, 13, 22), fill=EDGE)
    d.line((17, 22, 24, 22), fill='#718b93')
    finish(im, pos)

    im, d, pos = tile('vent', PANEL)
    d.rectangle((0, 0, 31, 31), outline=EDGE)
    d.line((1, 1, 30, 1), fill='#748e97')
    d.rectangle((4, 5, 27, 24), fill=DARK)
    for y in (7, 11, 15, 19):
        d.line((6, y, 25, y), fill='#48616c')
        d.line((6, y+1, 25, y+1), fill='#0e202a')
    d.rectangle((5, 27, 12, 28), fill=AMBER)
    d.line((22, 27, 27, 27), fill='#82959a')
    for x in (2, 29):
        for y in (2, 29):
            d.point((x, y), fill='#96acb0')
    finish(im, pos)

    im, d, pos = tile('bed', DARK)
    d.rectangle((0, 0, 31, 15), outline=EDGE)
    d.rectangle((3, 2, 28, 13), outline=LINE)
    for x in (7, 15, 23):
        d.line((x, 3, x, 12), fill=PANEL)
    for y in (5, 9):
        d.line((4, y, 27, y), fill=PANEL)
    # Registration brackets, leaving the build surface visually quiet.
    for x, sign in ((4, 1), (27, -1)):
        for y, dy in ((3, 1), (12, -1)):
            d.line((x, y, x+3*sign, y), fill=CYAN)
            d.line((x, y, x, y+2*dy), fill=CYAN)
    d.line((13, 7, 18, 7), fill=EDGE)
    d.line((15, 6, 15, 9), fill=EDGE)
    finish(im, pos)

    im, d, pos = tile('chamber', DARK)
    d.rectangle((0, 0, 31, 15), outline=LINE)
    for x in (6, 15, 24):
        d.line((x, 2, x, 13), fill=PANEL)
    d.line((2, 11, 29, 11), fill=PANEL)
    d.rectangle((10, 3, 21, 8), outline=LINE)
    for x in (3, 28):
        d.line((x, 3, x, 8), fill=EDGE)
    finish(im, pos)

    im, d, pos = tile('fascia', SHELL)
    d.line((0, 0, 31, 0), fill=HIGHLIGHT)
    d.line((0, 7, 31, 7), fill=EDGE)
    # Abstract voxel mark and engraved equipment stripes, no illegible fake text.
    d.polygon(((3, 3), (5, 1), (7, 3), (5, 5)), fill=DARK)
    d.point((5, 2), fill=CYAN)
    d.line((10, 3, 18, 3), fill=EDGE)
    d.line((10, 5, 14, 5), fill='#7d969d')
    d.rectangle((24, 2, 28, 4), fill=DARK)
    d.line((25, 3, 27, 3), fill=CYAN)
    finish(im, pos)

    im, d, pos = tile('console', DARK)
    d.rectangle((0, 0, 15, 15), outline=EDGE)
    d.rectangle((2, 2, 13, 10), fill='#183d4b')
    d.line((4, 4, 10, 4), fill=CYAN)
    d.line((4, 6, 8, 6), fill='#558790')
    d.line((4, 8, 11, 8), fill='#558790')
    d.rectangle((3, 12, 5, 13), fill=AMBER)
    d.rectangle((8, 12, 12, 13), fill=EDGE)
    finish(im, pos)

    for name, color in (('dark', DARK), ('metal', SHELL), ('cyan', CYAN), ('amber', AMBER)):
        im, d, pos = tile(name, color)
        d.line((0, 0, 7, 0), fill=PALE if name == 'cyan' else HIGHLIGHT if name == 'metal' else EDGE)
        d.line((0, 7, 7, 7), fill=EDGE if name == 'metal' else PANEL)
        finish(im, pos)
    im, d, pos = tile('warning', DARK)
    for x in range(-8, 24, 8):
        d.polygon(((x, 7), (x+3, 7), (x+10, 0), (x+7, 0)), fill=AMBER)
    d.line((0, 0, 15, 0), fill=EDGE)
    d.line((0, 7, 15, 7), fill=EDGE)
    finish(im, pos)
    im, d, pos = tile('rib', PANEL)
    for x in (2, 6, 10, 14):
        d.line((x, 1, x, 6), fill=EDGE)
        d.line((x+1, 1, x+1, 6), fill=DARK)
    finish(im, pos)
    atlas.save(ATLAS)


def make_model():
    elements = []
    def box(name, start, end, material='metal', **sides):
        faces = {}
        for face in ('north', 'east', 'south', 'west', 'up', 'down'):
            region = REGIONS[sides.get(face, material)]
            faces[face] = {'uv': [v/4 for v in region], 'texture': '#atlas'}
            if face == 'south' and end[2] == 16:
                faces[face]['cullface'] = 'south'
        elements.append({'name': name, 'from': start, 'to': end, 'faces': faces})

    box('wall mounting spine', [1, 2, 14], [15, 15.5, 16], 'dark', south='armor')
    box('lower equipment chassis', [2, 1, 4], [14, 3, 14], 'dark', north='rib', east='vent', west='vent')
    box('platform carrier', [3, 3, 4], [13, 4.25, 14], 'metal', east='rib', west='rib')
    box('front tray support', [3, 3, 3], [13, 4.25, 4], 'metal', north='fascia')
    box('build surface', [4, 4.25, 4], [12, 5.25, 13.5], 'dark', up='bed')
    box('front tray rim', [3, 4.25, 3], [13, 5.25, 4], 'metal', north='rib', up='dark')
    for x in (3, 12):
        box('tray guide', [x, 4.25, 4], [x+1, 5.5, 13.5], 'metal', up='dark')
        box('tray registration light', [x+0.3, 5.5, 5], [x+0.7, 5.6, 10], 'cyan')
    for x in (1, 13):
        box('structural side housing', [x, 3, 8.5], [x+2, 13.5, 14], 'metal', east='vent', west='vent')
        box('stepped front shoulder', [x, 6, 7], [x+2, 13.5, 8.5], 'metal', north='armor')
        box('lower side brace', [x, 3, 6], [x+2, 6, 8.5], 'dark', east='rib', west='rib')
        box('front guard plate', [x+0.25, 6.5, 6.65], [x+1.75, 12.75, 7], 'dark', north='rib')
        box('shoulder marker', [x+0.5, 11.25, 6.5], [x+1.5, 12.25, 6.65], 'cyan' if x == 1 else 'amber')
        box('tray warning tab', [x+0.25, 3.5, 5.8], [x+1.75, 4.75, 6], 'dark', north='warning')
    box('overhead equipment beam', [1, 13.5, 7], [15, 15.5, 14], 'metal', up='armor', down='dark', east='rib', west='rib')
    box('front header fascia', [2, 13.5, 6.25], [14, 15.5, 7], 'metal', north='fascia')
    box('roof service panel', [3, 15.5, 9], [13, 16, 14.5], 'dark', up='vent')
    box('chamber backing', [3, 5.5, 13.5], [13, 13.5, 14], 'dark', north='chamber')
    box('rear equipment plinth', [3, 4.25, 13.5], [13, 5.5, 14], 'dark', north='rib')
    for x in (3, 12.5):
        box('inner frame upright', [x, 5.5, 12.75], [x+0.5, 13.5, 13.5], 'metal')
    box('rear task light', [4, 12.75, 13.25], [12, 13.125, 13.5], 'cyan')
    # The rail touches the beam and both existing animated probe mounts (y=0.84).
    box('probe mounting rail', [4.5, 13.44, 7.1], [11.5, 13.5, 8.9], 'dark')
    box('recessed diagnostic panel', [10.5, 9.5, 13.25], [12.25, 11.5, 13.5], 'dark', north='console')
    box('front tray latch', [6.5, 3.375, 2.75], [9.5, 4, 3], 'dark', north='metal')

    # Reject interpenetrating parts before writing; touching faces are allowed.
    for i, a in enumerate(elements):
        for b in elements[i+1:]:
            overlap = [min(a['to'][k], b['to'][k])-max(a['from'][k], b['from'][k]) for k in range(3)]
            assert not all(v > 0 for v in overlap), (a['name'], b['name'])
    model = {'parent': 'minecraft:block/block', 'ambientocclusion': True,
             'textures': {'atlas': 'starboundmc:block/voxel_printing_station_atlas',
                          'particle': 'starboundmc:block/voxel_machine_casing'}, 'elements': elements}
    # Keep faces compact so model coordinates remain easy to review and hand-edit.
    content = json.dumps(model, indent=2)
    import re
    content = re.sub(r'\[\s+([\d.]+),\s+([\d.]+),\s+([\d.]+)(?:,\s+([\d.]+))?\s+\]',
                     lambda m: '[' + ', '.join(v for v in m.groups() if v is not None) + ']', content)
    content = re.sub(r'\{\n\s+"uv": (\[[^\]]+\]),\n\s+"texture": ("[^"]+")(?:,\n\s+"cullface": ("[^"]+"))?\n\s+\}',
                     lambda m: '{ "uv": ' + m[1] + ', "texture": ' + m[2]
                     + (', "cullface": ' + m[3] if m[3] else '') + ' }', content)
    MODEL.write_text(content + '\n', encoding='utf-8')
    print(f'Wrote {len(elements)} non-overlapping model parts and {ATLAS.name}')


if __name__ == '__main__':
    make_model()
    make_atlas()
