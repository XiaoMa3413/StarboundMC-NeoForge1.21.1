"""Original pixel textures + cuboid model for the shipboard engine (not the external thruster).

Run from any directory with Python/Pillow. Only writes the named new engine assets.
The optional preview is an orthographic asset inspection, not a Minecraft screenshot.
"""
import argparse
import json
import math
from pathlib import Path
from PIL import Image, ImageDraw, ImageEnhance

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/starboundmc'
NAME = 'ship_engine_unit'


def texture(kind):
    im = Image.new('RGBA', (32, 32), (33, 46, 57, 255))
    d = ImageDraw.Draw(im)
    for y in range(32):
        for x in range(32):
            n = (x * 17 + y * 7 + x * y) % 5 - 2
            im.putpixel((x, y), (33 + n, 46 + n, 57 + n, 255))
    d.rectangle((0, 0, 31, 31), outline='#111c26', width=2)
    d.line((2, 29, 2, 2, 29, 2), fill='#687e89')
    d.line((3, 30, 30, 30, 30, 3), fill='#17232d')
    if kind == 'casing':
        d.rectangle((5, 6, 26, 25), fill='#293c4b', outline='#182934')
        d.line((7, 8, 24, 8), fill='#45606b')
        for y in (19, 22):
            d.line((8, y, 23, y), fill='#1a2a36')
        d.rectangle((7, 11, 9, 15), fill='#dfa564')
        d.line((12, 12, 22, 12), fill='#6c8590')
        d.line((12, 14, 18, 14), fill='#546b76')
    elif kind == 'metal':
        d.rectangle((3, 3, 28, 28), fill='#617782')
        for y in range(4, 28, 3):
            d.line((4, y, 27, y), fill='#6b8089')
        d.rectangle((10, 3, 12, 28), fill='#9eb0b6')
        d.rectangle((19, 3, 21, 28), fill='#314652')
        d.line((5, 4, 26, 4), fill='#c5d2d6')
    elif kind == 'vent':
        d.rectangle((5, 4, 26, 27), fill='#0d1a23')
        for y in range(6, 27, 4):
            d.line((7, y, 24, y), fill='#6b818a')
            d.line((7, y + 1, 24, y + 1), fill='#2b414f')
        d.rectangle((2, 7, 3, 24), fill='#ae784c')
        d.rectangle((28, 7, 29, 24), fill='#ae784c')
    elif kind == 'panel':
        d.rectangle((4, 4, 27, 26), fill='#0b1923', outline='#607d88')
        d.rectangle((6, 6, 25, 18), fill='#103b4a')
        d.line((8, 15, 11, 15, 13, 9, 16, 16, 19, 12, 23, 12), fill='#9af8ec', width=1)
        for x in range(7, 26, 4):
            d.rectangle((x, 21, x + 1, 22), fill='#edb779' if x < 14 else '#68969e')
        d.line((7, 25, 17, 25), fill='#536c77')
    elif kind == 'coil':
        d.rectangle((3, 3, 28, 28), fill='#162d38')
        for x in range(5, 28, 5):
            d.rectangle((x, 4, x + 1, 27), fill='#64b8c0')
            d.line((x + 2, 5, x + 2, 26), fill='#284d5b')
        d.line((4, 5, 27, 5), fill='#a4d9d9')
        d.line((4, 26, 27, 26), fill='#8e6747')
    for x, y in ((3, 3), (27, 3), (3, 27), (27, 27)):
        d.rectangle((x, y, x + 1, y + 1), fill='#a1b4bd')
        d.point((x + 1, y + 1), fill='#2b3d47')
    return im


def core_frames():
    atlas = Image.new('RGBA', (32, 128))
    for frame in range(4):
        im = Image.new('RGBA', (32, 32), '#164352')
        d = ImageDraw.Draw(im)
        d.rectangle((3, 0, 28, 31), fill='#237789')
        d.rectangle((7, 0, 24, 31), fill='#3aafbb')
        d.rectangle((11, 0, 20, 31), fill='#9cf3df')
        d.rectangle((14, 0, 17, 31), fill='#e1fff0')
        for y in range(0, 32, 8):
            yy = (y + frame * 2) % 32
            d.line((3, yy, 28, yy), fill='#b8f6ed')
            d.line((0, yy + 1, 5, yy + 1), fill='#081e2b')
        atlas.paste(im, (0, frame * 32))
    return atlas


def model():
    elements = []
    def box(name, lo, hi, tex='casing', front=None, top=None, angle=None):
        faces = {f: {'uv': [0, 0, 16, 16], 'texture': '#' + tex}
                 for f in ('north', 'south', 'east', 'west', 'up', 'down')}
        if front: faces['north']['texture'] = '#' + front
        if top: faces['up']['texture'] = '#' + top
        part = {'name': name, 'from': lo, 'to': hi, 'faces': faces}
        if angle is not None:
            part['rotation'] = {'origin': [8, 8, 8], 'axis': 'y', 'angle': angle}
        elements.append(part)
    box('mounting plinth', [1, 0, 1], [15, 2, 15], 'metal')
    box('lower manifold', [2, 2, 2], [14, 3.5, 14], 'casing')
    box('rear exchanger', [4, 3.5, 11], [12, 13, 14], 'vent')
    box('left exchanger', [2, 3.5, 4], [4, 13, 12], 'vent')
    box('right exchanger', [12, 3.5, 4], [14, 13, 12], 'vent')
    box('crystal reaction chamber', [5.5, 3.5, 5.5], [10.5, 13, 10.5], 'core', angle=45)
    for y in (4, 7, 10):
        box('front field coil', [4.5, y, 4.5], [11.5, y + 0.8, 5.5], 'coil')
        box('rear field coil', [4.5, y, 10.5], [11.5, y + 0.8, 11.5], 'coil')
        box('left field coil', [4.5, y, 5.5], [5.5, y + 0.8, 10.5], 'coil')
        box('right field coil', [10.5, y, 5.5], [11.5, y + 0.8, 10.5], 'coil')
    for x in (3, 11.75):
        box('front load strut', [x, 3.5, 3], [x + 1.25, 13, 4.25], 'metal')
    for y in (5, 7, 9, 11):
        box('left cooling fin', [1.5, y, 5], [3, y + 0.7, 11], 'metal')
        box('right cooling fin', [13, y, 5], [14.5, y + 0.7, 11], 'metal')
    box('module interface', [4, 3, 2], [12, 7, 4], 'casing', front='panel')
    box('upper manifold', [3, 13, 3], [13, 15, 13], 'casing', front='coil', top='vent')
    box('service cap', [5, 15, 5], [11, 16, 11], 'metal', top='panel')
    return {'parent': 'minecraft:block/block', 'textures': {
        **{k: f'starboundmc:block/{NAME}_{k}' for k in ('casing', 'metal', 'vent', 'panel', 'coil', 'core')},
        'particle': f'starboundmc:block/{NAME}_casing'}, 'elements': elements,
        'display': {'gui': {'rotation': [30, 225, 0], 'scale': [0.72, 0.72, 0.72]},
                    'ground': {'translation': [0, 3, 0], 'scale': [0.35, 0.35, 0.35]},
                    'fixed': {'rotation': [0, 180, 0], 'scale': [0.65, 0.65, 0.65]},
                    'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.375]*3},
                    'firstperson_righthand': {'rotation': [0, 45, 0], 'scale': [0.4]*3}}}


def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + '\n', encoding='utf-8')


def preview(data, textures, destination):
    import numpy as np
    canvas = Image.new('RGBA', (1200, 730), '#101923')
    draw = ImageDraw.Draw(canvas)
    draw.text((35, 26), 'SHIP ENGINE / REACTION & IGNITION UNIT', fill='#d3e7ec', font_size=24)
    draw.text((35, 62), 'Original 32px textures  |  Orthographic asset preview (not in-game)', fill='#8cabb8', font_size=15)
    def render(center, yaw):
        faces = []
        for part in data['elements']:
            x, y, z = part['from']; X, Y, Z = part['to']
            quads = {
                'north': [(x,Y,z),(X,Y,z),(X,y,z),(x,y,z)],
                'south': [(X,Y,Z),(x,Y,Z),(x,y,Z),(X,y,Z)],
                'east': [(X,Y,z),(X,Y,Z),(X,y,Z),(X,y,z)],
                'west': [(x,Y,Z),(x,Y,z),(x,y,z),(x,y,Z)],
                'up': [(x,Y,Z),(X,Y,Z),(X,Y,z),(x,Y,z)],
                'down': [(x,y,z),(X,y,z),(X,y,Z),(x,y,Z)]}
            angle = math.radians(yaw + part.get('rotation', {}).get('angle', 0))
            rot = np.array([[math.cos(angle),0,math.sin(angle)], [0,1,0],[-math.sin(angle),0,math.cos(angle)]])
            for side, quad in quads.items():
                points = (np.array(quad) - 8) @ rot.T
                normal = np.cross(points[1]-points[0], points[2]-points[1])
                normal /= np.linalg.norm(normal)
                if np.dot(normal, [1,18/23,-1]) <= 1e-6: continue
                projected = [(center[0]+(p[0]+p[2])*16, center[1]+(p[0]-p[2])*7.6-p[1]*19.4) for p in points]
                depth = np.mean(points @ np.array([1,0.8,-1]))
                shade = 0.7 + 0.3*max(0, np.dot(normal, [0.35,0.8,-0.5]))
                faces.append((depth, projected, part['faces'][side]['texture'][1:], shade))
        for _, quad, tex, shade in sorted(faces, key=lambda f: f[0]):
            tile = ImageEnhance.Brightness(textures[tex].crop((0,0,32,32))).enhance(shade)
            pts = np.array([[*quad[i],1] for i in (0,1,3)])
            u = np.linalg.solve(pts, [0,32,0]); v = np.linalg.solve(pts, [0,0,32])
            projected = tile.transform(canvas.size, Image.Transform.AFFINE, tuple(u)+tuple(v), Image.Resampling.NEAREST)
            mask = Image.new('L', canvas.size)
            ImageDraw.Draw(mask).polygon(quad, fill=255)
            canvas.paste(projected, (0,0), mask)
    render((305,320), 0)
    render((875,320), 180)
    draw = ImageDraw.Draw(canvas)
    draw.text((180,584), 'FRONT / module interface', fill='#c0d5df', font_size=18)
    draw.text((760,584), 'REAR / heat exchanger', fill='#c0d5df', font_size=18)
    for i, (name, im) in enumerate(textures.items()):
        canvas.paste(im.crop((0,0,32,32)).resize((64,64), Image.Resampling.NEAREST), (160+i*150,620))
        draw.text((160+i*150,691), name.upper(), fill='#8cabb8', font_size=13)
    destination.parent.mkdir(parents=True, exist_ok=True)
    canvas.convert('RGB').save(destination)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--preview', type=Path)
    args = parser.parse_args()
    textures = {k: texture(k) for k in ('casing','metal','vent','panel','coil')}
    textures['core'] = core_frames()
    for kind, im in textures.items():
        im.save(ASSETS / f'textures/block/{NAME}_{kind}.png')
    write_json(ASSETS / f'textures/block/{NAME}_core.png.mcmeta', {'animation': {'frametime': 5, 'interpolate': True}})
    data = model()
    write_json(ASSETS / f'models/block/{NAME}.json', data)
    write_json(ASSETS / f'models/item/{NAME}.json', {'parent': f'starboundmc:block/{NAME}'})
    write_json(ASSETS / f'blockstates/{NAME}.json', {'variants': {
        f'facing={direction}': {'model': f'starboundmc:block/{NAME}', 'y': angle}
        for direction, angle in [('north',0),('east',90),('south',180),('west',270)]}})
    write_json(ROOT / f'src/main/resources/data/starboundmc/loot_table/blocks/{NAME}.json', {
        'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'starboundmc:{NAME}'}],
         'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
    if args.preview: preview(data, textures, args.preview)
    print(f'Wrote {len(data["elements"])} model elements, 6 original textures, animation and block/item definitions.')


if __name__ == '__main__':
    main()
