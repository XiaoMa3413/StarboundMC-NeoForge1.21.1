"""Approved V01 equipment: exact pixel assets and shared cuboid authoring source.

Run the weapon/armor resource generators. All production art is reproducible;
concept illustrations are references, never resized into game textures.
"""
from pathlib import Path
import base64
import copy
import json
import math
import struct
import uuid
import zlib

PROJECT = Path(__file__).resolve().parents[1]
ROOT = PROJECT / 'src/main/resources'
ASSETS = ROOT / 'assets/starboundmc'
EDITABLE = PROJECT / 'dev-assets/equipment'
FACES = ['north', 'south', 'east', 'west', 'up', 'down']
PALETTES = {
    'pioneer': [(16, 23, 30), (42, 49, 58), (91, 104, 113), (151, 165, 173), (218, 226, 224), (193, 129, 43), (247, 185, 67), (255, 226, 133)],
    'vanguard': [(15, 22, 29), (32, 43, 55), (61, 79, 98), (119, 141, 155), (192, 208, 215), (165, 68, 34), (226, 108, 56), (255, 177, 91)],
    'ranger': [(13, 25, 30), (28, 44, 50), (42, 78, 75), (106, 139, 143), (191, 211, 211), (32, 127, 109), (61, 221, 175), (157, 255, 222)],
    'starwarden': [(17, 20, 35), (35, 35, 61), (57, 51, 96), (130, 143, 172), (217, 229, 240), (86, 72, 178), (104, 159, 255), (184, 218, 255)],
}
WEAPON_PALETTE = [(17, 24, 32), (45, 54, 65), (111, 125, 135), (197, 207, 209),
                  (236, 236, 225), (161, 87, 35), (235, 159, 67), (255, 213, 123),
                  (31, 115, 97), (60, 217, 175), (139, 235, 255), (50, 42, 85),
                  (89, 69, 163), (121, 119, 255), (183, 209, 255), (43, 75, 102)]


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


def shade(color, amount):
    return tuple(max(0, min(255, c + amount)) for c in color[:3])


class Pixels:
    def __init__(self, width=32, height=32):
        self.w, self.h = width, height
        self.rows = [[(0, 0, 0, 0) for _ in range(width)] for _ in range(height)]

    def dot(self, x, y, color):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.rows[y][x] = (*color[:3], color[3] if len(color) == 4 else 255)

    def rect(self, x, y, w, h, color):
        for py in range(y, y + h):
            for px in range(x, x + w): self.dot(px, py, color)

    def poly(self, points, color):
        for y in range(max(0, min(p[1] for p in points)), min(self.h, max(p[1] for p in points) + 1)):
            for x in range(max(0, min(p[0] for p in points)), min(self.w, max(p[0] for p in points) + 1)):
                inside = False
                for a, b in zip(points, points[1:] + points[:1]):
                    if (a[1] > y + .5) != (b[1] > y + .5):
                        cross = (b[0] - a[0]) * (y + .5 - a[1]) / (b[1] - a[1]) + a[0]
                        if x + .5 < cross: inside = not inside
                if inside: self.dot(x, y, color)

    def outline(self, color):
        old = [row[:] for row in self.rows]
        for y in range(self.h):
            for x in range(self.w):
                if old[y][x][3]: continue
                if any(0 <= x + dx < self.w and 0 <= y + dy < self.h and old[y + dy][x + dx][3]
                       for dx, dy in [(1, 0), (-1, 0), (0, 1), (0, -1)]): self.dot(x, y, color)

    def diagonal(self):
        result = Pixels(self.w, self.h)
        # Nearest-neighbor rotation of our authored pixel grid, never an illustration resize.
        for y in range(self.h):
            for x in range(self.w):
                dx, dy = x - 15.5, y - 15.5
                sx = round((dx + dy) / math.sqrt(2) + 15.5)
                sy = round((-dx + dy) / math.sqrt(2) + 15.5)
                if 0 <= sx < self.w and 0 <= sy < self.h: result.rows[y][x] = self.rows[sy][sx]
        return result

    def png(self):
        def chunk(kind, data):
            return struct.pack('!I', len(data)) + kind + data + struct.pack('!I', zlib.crc32(kind + data))
        raw = b''.join(b'\0' + bytes(v for pixel in row for v in pixel) for row in self.rows)
        return b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('!2I5B', self.w, self.h, 8, 6, 0, 0, 0)) + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b'')

    def save(self, path):
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(self.png())


def atlas(palette, tile, size):
    image = Pixels(size, size)
    for i, color in enumerate(palette):
        ox, oy = i % 4 * tile, i // 4 * tile
        for y in range(tile):
            for x in range(tile):
                delta = 8 if y == 0 or x == 0 else -10 if x == tile - 1 or y == tile - 1 else -5 if x % 8 == 0 or y % 8 == 0 else 3 if (x // 4 + y // 4) % 2 == 0 else 0
                image.dot(ox + x, oy + y, shade(color, delta))
    return image


def weapon_icon(kind):
    im = Pixels(); p = WEAPON_PALETTE
    if kind in ('blade', 'glaive', 'greatblade'):
        im.rect(15, 21, 3, 9, p[1]); im.rect(15, 22, 1, 7, p[2]); im.rect(14, 29, 5, 2, p[3])
        if kind == 'blade':
            im.poly([(15, 2), (18, 4), (18, 23), (14, 23), (14, 3)], p[3])
            im.rect(14, 4, 1, 18, p[4]); im.rect(17, 5, 1, 17, p[2]); im.rect(11, 23, 10, 2, p[2]); im.rect(15, 20, 2, 2, p[6])
        elif kind == 'glaive':
            im.rect(15, 5, 2, 25, p[2]); im.rect(15, 12, 2, 2, p[9]); im.rect(15, 21, 2, 2, p[9])
            im.poly([(16, 4), (25, 2), (26, 7), (23, 12), (17, 14)], p[3])
            im.poly([(18, 5), (24, 3), (24, 6), (22, 9), (18, 10)], p[2]); im.rect(18, 7, 5, 1, p[9])
            im.poly([(15, 4), (10, 5), (9, 8), (11, 7), (15, 7)], p[3])
            im.rect(13, 11, 6, 2, p[2])
        else:
            im.poly([(14, 2), (21, 6), (20, 18), (18, 22), (12, 22), (11, 7)], p[3])
            im.poly([(14, 4), (19, 7), (18, 18), (16, 21), (13, 20), (12, 8)], p[11])
            im.rect(15, 7, 2, 12, p[13]); im.rect(15, 8, 1, 10, p[14]); im.rect(9, 22, 14, 2, p[3])
            im.poly([(16, 20), (19, 23), (16, 26), (13, 23)], p[13]); im.dot(16, 22, p[14])
        im = im.diagonal()
    else:
        if kind == 'pistol':
            im.poly([(7, 15), (13, 16), (10, 28), (4, 26)], p[1]); im.poly([(7, 17), (9, 18), (6, 25), (5, 25)], p[2])
            im.rect(7, 8, 12, 10, p[3]); im.rect(8, 8, 11, 2, p[4]); im.rect(11, 11, 4, 2, p[10])
            im.rect(18, 8, 11, 11, p[1]); im.rect(19, 10, 8, 2, p[6]); im.rect(19, 15, 8, 2, p[6])
            for x in (20, 25): im.rect(x, 9, 1, 9, p[5])
            im.rect(28, 9, 2, 9, p[3]); im.rect(29, 12, 1, 3, p[0]); im.rect(11, 6, 4, 2, p[1])
            im.rect(14, 18, 3, 5, p[1]); im.rect(15, 19, 1, 3, p[10])
        elif kind == 'carbine':
            im.rect(2, 9, 5, 13, p[1]); im.rect(2, 9, 2, 12, p[3]); im.rect(4, 12, 1, 7, p[9])
            im.rect(7, 10, 20, 8, p[3]); im.rect(7, 9, 16, 2, p[4]); im.rect(24, 11, 6, 4, p[1])
            im.poly([(12, 17), (17, 18), (13, 27), (9, 25)], p[1]); im.rect(7, 17, 4, 6, p[2])
            im.rect(12, 12, 9, 4, p[8]); im.rect(13, 12, 7, 2, p[9]); im.dot(14, 12, p[10])
            im.rect(24, 16, 4, 2, p[1]); im.rect(17, 7, 4, 2, p[1])
        else:
            im.poly([(2, 8), (7, 9), (8, 18), (3, 22), (2, 21)], p[11]); im.rect(2, 9, 1, 11, p[3])
            im.rect(7, 10, 10, 9, p[2]); im.rect(8, 9, 9, 2, p[3]); im.rect(10, 12, 5, 5, p[12]); im.rect(11, 13, 3, 3, p[13]); im.dot(12, 13, p[14])
            im.poly([(10, 19), (14, 20), (11, 27), (7, 26)], p[1])
            for y in (10, 16):
                im.rect(17, y, 13, 3, p[11]); im.rect(18, y + 1, 10, 1, p[13]); im.rect(29, y, 2, 3, p[3])
            im.rect(12, 7, 4, 2, p[2])
    im.outline(WEAPON_PALETTE[0])
    return im


def weapon_cubes(kind):
    cubes = []
    def box(name, a, b, material): cubes.append({'name': name, 'from': a, 'to': b, 'material': material})
    if kind in ('blade', 'glaive', 'greatblade'):
        box('wrapped_grip', [7, -3, 7], [9, 3, 9], 1)
        for y in (-2, 0, 2): box('grip_band', [6.9, y, 6.9], [9.1, y + .4, 9.1], 2)
        box('pommel', [6.5, -4, 6.5], [9.5, -3, 9.5], 3)
        if kind == 'blade':
            box('guard', [4, 3, 6.5], [12, 4, 9.5], 2)
            box('tungsten_blade', [6.7, 4, 7.5], [9.3, 21, 8.5], 3)
            box('silver_edge', [6.3, 4, 7.5], [6.7, 21.5, 8.5], 4)
            box('clipped_tip', [6.3, 21, 7.5], [8.3, 22, 8.5], 3)
            for y in (4.8, 6): box('amber_mark', [7, y, 7.35], [7.8, y + .65, 7.5], 6)
        elif kind == 'glaive':
            box('shaft', [7.4, -8, 7.4], [8.6, 23, 8.6], 1)
            box('shaft_edge', [7.3, -8, 7.3], [7.6, 23, 7.6], 15)
            for y in (-5, 3, 12, 21):
                box('collar', [7, y, 7], [9, y + 1.2, 9], 3)
                box('teal_collar', [6.9, y + .4, 6.9], [9.1, y + .7, 9.1], 9)
            box('blade_root', [8.5, 21, 7.3], [13, 30, 8.7], 2)
            box('blade_face', [13, 23, 7.3], [17, 31, 8.7], 3)
            box('outer_edge', [17, 24, 7.3], [18, 31.5, 8.7], 4)
            box('head_tip', [13, 30, 7.3], [17.5, 32, 8.7], 3)
            box('short_rear_hook', [4, 28, 7.3], [7.5, 29.5, 8.7], 3)
            box('hook_tip', [4, 26.5, 7.3], [5, 28.5, 8.7], 3)
            box('teal_channel', [9, 25, 7.15], [15.5, 25.6, 7.3], 9)
        else:
            box('guard_center', [3, 3, 6], [13, 5, 10], 3)
            box('guard_left', [2, 4, 6.5], [4, 7, 9.5], 11)
            box('guard_right', [12, 4, 6.5], [14, 7, 9.5], 11)
            box('starcore_blade', [4.5, 5, 7], [11.5, 24, 9], 11)
            box('silver_edge', [4, 5, 7], [4.5, 24, 9], 3)
            box('silver_spine', [11.5, 5, 7], [12, 23, 9], 3)
            box('tip_step', [4, 24, 7], [10.5, 26, 9], 3)
            box('clipped_tip', [4, 26, 7], [8, 27, 9], 4)
            box('split_channel', [7.5, 8, 6.8], [8.5, 23, 7], 13)
            box('core_mount', [6, 4, 6], [10, 8, 10], 2)
            box('starcore', [6.7, 4.7, 5.6], [9.3, 7.3, 6], 13)
            box('core_highlight', [7.3, 5.2, 5.4], [8.5, 6.4, 5.6], 14)
    else:
        box('angled_grip', [1.5, 1, 7], [4.5, 7, 9], 1)
        box('grip_heel', [1, .5, 6.7], [5, 1.5, 9.3], 2)
        box('receiver', [1, 7, 5.8], [11, 12, 10.2], 3)
        box('receiver_top', [1, 11.5, 5.5], [11.5, 12.5, 10.5], 4)
        box('top_sight', [5, 12.5, 7], [8, 14, 9], 1)
        box('sight_lens', [7.8, 13, 7.3], [8.1, 13.7, 8.7], 10 if kind == 'pistol' else 9 if kind == 'carbine' else 13)
        if kind == 'pistol':
            box('coil_back_brace', [10.5, 7, 5.8], [12, 12.5, 10.2], 2)
            box('coil_front_brace', [19, 7, 5.8], [20.5, 12.5, 10.2], 3)
            for y in (7.8, 10.4):
                box('copper_coil', [12, y, 6.7], [19, y + 1.4, 9.3], 6)
                for x in (12.2, 14, 16, 17.8): box('coil_winding', [x, y - .1, 6.5], [x + .4, y + 1.5, 9.5], 5)
            box('cage_upper', [11, 12, 5.8], [20, 12.6, 10.2], 1)
            box('cage_lower', [11, 7, 5.8], [20, 7.6, 10.2], 1)
            box('square_muzzle', [20.5, 8, 6.3], [22, 11.8, 9.7], 1)
            box('muzzle_port', [22, 9, 7.3], [22.1, 10.5, 8.7], 0)
            box('energy_well', [7, 3.5, 6.8], [9.5, 7, 9.2], 1)
            box('cell_indicator', [7.6, 4.2, 6.65], [8.4, 6.4, 6.8], 10)
            box('receiver_indicator', [5, 9, 5.6], [7.5, 10, 5.8], 10)
        elif kind == 'carbine':
            box('bullpup_stock', [-7, 4, 6], [0, 12, 10], 1)
            box('stock_shell', [-7, 4, 5.8], [-5.5, 12, 10.2], 3)
            box('rear_cell', [-5, 4.5, 5.6], [-3.8, 10.5, 5.8], 9)
            box('handguard', [11, 7, 5.8], [22, 12, 10.2], 3)
            box('plasma_window', [7.2, 8.2, 5.5], [14.7, 10.7, 5.8], 8)
            box('plasma_core', [8, 9.5, 5.3], [14, 10.5, 5.5], 9)
            for x in (17, 19, 21): box('vent', [x, 8, 5.5], [x + .8, 10.5, 5.8], 1)
            box('emitter', [22, 8, 6.5], [24, 11, 9.5], 1)
            box('lower_rail', [11, 6.5, 6.5], [21, 7, 9.5], 1)
        else:
            box('braced_stock_upper', [-8, 9, 6.5], [1, 11.5, 9.5], 11)
            box('braced_stock_lower', [-8, 4, 6.5], [-1, 6.5, 9.5], 11)
            box('stock_butt', [-8, 4, 6], [-6.5, 12, 10], 3)
            box('core_chamber', [4, 7.5, 5.3], [11, 12.5, 10.7], 11)
            box('core_window', [5.2, 8.3, 5], [9.8, 11.5, 5.3], 12)
            box('starcore', [6, 9, 4.8], [9, 11, 5], 13)
            box('starcore_light', [6.5, 9.5, 4.7], [8.5, 10.5, 4.8], 14)
            for y in (7, 11):
                box('accelerator_rail', [11, y, 6], [28, y + 2, 10], 11)
                box('rail_channel', [12, y + .6, 5.8], [27, y + 1.2, 6], 13)
                box('rail_end', [27.5, y - .2, 5.8], [29, y + 2.2, 10.2], 3)
            box('rail_bridge', [10, 7, 6], [12, 13, 10], 2)
    if kind in ('pistol', 'carbine', 'railgun'):
        for cube in cubes:
            if cube['name'] in ('angled_grip', 'grip_heel'):
                cube['rotation'] = {'origin': [3, 7, 8], 'axis': 'z', 'angle': -22.5}
    if kind == 'greatblade':
        for cube in cubes:
            if cube['name'] in ('starcore', 'core_highlight'):
                cube['rotation'] = {'origin': [8, 6, 5.6], 'axis': 'z', 'angle': 45}
    # Small surface details exist on both sides, so left-handed equipment remains authored.
    for cube in list(cubes):
        if cube['to'][2] <= 8 and cube['to'][2] - cube['from'][2] < .65:
            opposite = copy.deepcopy(cube)
            opposite['name'] += '_reverse'
            opposite['from'][2], opposite['to'][2] = 16 - cube['to'][2], 16 - cube['from'][2]
            if 'rotation' in opposite: opposite['rotation']['origin'][2] = 16 - opposite['rotation']['origin'][2]
            cubes.append(opposite)
    return cubes


def generate_weapon(name, kind):
    texture_name = 'item/equipment_palette'
    image = atlas(WEAPON_PALETTE, 16, 64); image.save(ASSETS / f'textures/{texture_name}.png')
    weapon_icon(kind).save(ASSETS / f'textures/item/{name}.png')
    elements = []
    for cube in weapon_cubes(kind):
        i = cube['material']; uv = [i % 4 * 4 + .25, i // 4 * 4 + .25, i % 4 * 4 + 3.75, i // 4 * 4 + 3.75]
        element = {'name': cube['name'], 'from': cube['from'], 'to': cube['to'], 'faces': {f: {'texture': '#palette', 'uv': uv} for f in FACES}}
        if 'rotation' in cube: element['rotation'] = cube['rotation']
        elements.append(element)
    gun = kind in ('pistol', 'carbine', 'railgun')
    scale = .52 if kind == 'pistol' else .43 if gun else .38 if kind == 'glaive' else .55
    hand = {'credit': 'StarboundMC approved V01 cuboid source', 'textures': {'palette': f'starboundmc:{texture_name}', 'particle': f'starboundmc:{texture_name}'},
            'elements': elements, 'display': {
                'firstperson_righthand': {'rotation': [0, 110, 0] if gun else [0, -90, 25], 'translation': [-1.5, 2.2, -2] if gun else [1.13, 3.2 if kind == 'glaive' else 4.2, 1.13], 'scale': [scale] * 3},
                'firstperson_lefthand': {'rotation': [0, -70, 0] if gun else [0, 90, -25], 'translation': [-1.5, 2.2, -2] if gun else [1.13, 3.2 if kind == 'glaive' else 4.2, 1.13], 'scale': [scale] * 3},
                'thirdperson_righthand': {'rotation': [0, -90, 55] if gun else [0, 0, 0], 'translation': [0, 3, .5], 'scale': [.58] * 3},
                'thirdperson_lefthand': {'rotation': [0, 90, -55] if gun else [0, 0, 0], 'translation': [0, 3, .5], 'scale': [.58] * 3},
                'ground': {'translation': [0, 3, 0], 'scale': [.35] * 3}, 'fixed': {'rotation': [0, 180, 0], 'scale': [.55] * 3}}}
    write(ASSETS / f'models/item/{name}_hand.json', hand)
    write(ASSETS / f'models/item/{name}.json', {'loader': 'neoforge:separate_transforms',
          'base': {'parent': f'starboundmc:item/{name}_hand'}, 'perspectives': {'gui': {
              'parent': 'minecraft:item/generated', 'loader': 'neoforge:item_layers',
              'textures': {'layer0': f'starboundmc:item/{name}'}}}})
    export_blockbench(name, weapon_cubes(kind), image, 64, 'java_block')


def armor_icon(profile, part):
    p = PALETTES[profile]; im = Pixels(); heavy = profile == 'vanguard'; star = profile == 'starwarden'
    if part == 'helmet':
        im.poly([(6, 6), (10, 3), (23, 3), (27, 7), (27, 25), (23, 28), (8, 28), (5, 24)], p[2])
        im.rect(8, 6, 15, 3, p[3]); im.rect(25, 8, 2, 15, p[1]); im.rect(7, 11, 17, 10, p[0])
        im.rect(7, 22, 17, 3, p[3]); im.rect(5, 12, 2, 9, p[3]); im.rect(23, 12, 2, 9, p[3])
        if profile == 'pioneer':
            im.rect(13, 4, 7, 7, p[1]); im.rect(14, 5, 5, 5, p[6]); im.rect(15, 6, 3, 3, p[7])
            im.rect(8, 13, 2, 6, p[5])
        elif heavy:
            im.rect(13, 4, 5, 7, p[6]); im.rect(9, 12, 13, 3, p[0]); im.rect(13, 15, 5, 10, p[0])
            im.rect(10, 17, 2, 8, p[3]); im.rect(19, 17, 2, 8, p[3])
        else:
            im.rect(7, 13, 17, 3, p[6]); im.rect(8, 13, 14, 1, p[7]); im.rect(24, 12, 4, 7, p[3]); im.rect(25, 14, 2, 3, p[6])
            if star: im.rect(14, 3, 4, 7, p[5]); im.rect(15, 4, 2, 5, p[7])
    elif part == 'chestplate':
        width = 28 if heavy else 24 if star else 22
        left = (32 - width) // 2
        im.poly([(left, 7), (10, 4), (13, 7), (19, 7), (22, 4), (32 - left, 7), (30 - left, 15), (24, 16), (23, 28), (9, 28), (8, 16), (left + 2, 15)], p[2])
        im.rect(11, 8, 10, 16, p[3]); im.rect(12, 10, 8, 13, p[2]); im.rect(9, 25, 14, 3, p[1])
        if profile == 'pioneer':
            im.rect(10, 11, 12, 2, p[6]); im.rect(10, 17, 5, 6, p[1]); im.rect(17, 17, 5, 6, p[1]); im.rect(11, 18, 3, 1, p[6]); im.rect(18, 18, 3, 1, p[6])
        elif heavy:
            for x in (3, 23): im.rect(x, 7, 6, 7, p[3]); im.rect(x + 1, 8, 4, 5, p[2])
            im.rect(10, 11, 12, 4, p[4]); im.rect(14, 18, 4, 5, p[6]); im.rect(10, 8, 3, 2, p[6]); im.rect(19, 8, 3, 2, p[6])
        elif star:
            im.poly([(16, 10), (21, 15), (16, 20), (11, 15)], p[4]); im.poly([(16, 12), (19, 15), (16, 18), (13, 15)], p[6]); im.dot(16, 14, p[7])
            for x in (10, 21): im.rect(x, 20, 1, 4, p[6])
        else:
            im.rect(5, 7, 5, 5, p[5]); im.rect(23, 8, 4, 4, p[3]); im.rect(13, 12, 6, 2, p[6]); im.rect(13, 17, 6, 4, p[5])
    elif part == 'leggings':
        im.rect(6, 4, 20, 5, p[2]); im.rect(13, 4, 6, 4, p[3]); im.rect(7, 9, 7, 19, p[1]); im.rect(18, 9, 7, 19, p[1])
        for x in (7, 18):
            im.rect(x, 10, 7, 7, p[2]); im.rect(x, 18, 7, 5, p[3]); im.rect(x + 1, 19, 5, 3, p[2]); im.rect(x, 25, 7, 3, p[3])
            if heavy: im.rect(x + 2, 10, 2, 6, p[6])
            elif profile in ('ranger', 'starwarden'): im.rect(x + 1, 11, 1, 5, p[6])
    else:
        for x in (5, 19):
            w = 8 if heavy else 7
            im.rect(x, 5, w, 18, p[2]); im.rect(x, 5, w, 3, p[3]); im.rect(x + 1, 10, w - 2, 6, p[1])
            im.rect(x - 1, 23, w + 1, 5, p[3]); im.rect(x - 1, 26, w + 1, 2, p[1])
            im.rect(x + 2, 11, 2 if profile != 'ranger' else 1, 4, p[6])
            if star: im.rect(x, 23, w - 1, 1, p[7])
    im.outline(p[0]); return im


def armor_geometry(profile, inner):
    parts = {n: [] for n in ['head', 'hat', 'body', 'right_arm', 'left_arm', 'right_leg', 'left_leg']}
    def box(part, name, xyz, size, material): parts[part].append({'name': name, 'from': xyz, 'size': size, 'material': material})
    heavy, star = profile == 'vanguard', profile == 'starwarden'
    if inner:
        box('body', 'hip_belt', [-4.2, 9.5, -2.2], [8.4, 2.6, 4.4], 1)
        box('body', 'belt_buckle', [-1.1, 10, -2.7], [2.2, 1.6, .5], 3)
        for leg in ['right_leg', 'left_leg']:
            box(leg, 'flexible_trouser', [-2.34, -.2, -2.34], [4.68, 11.8, 4.68], 1)
            box(leg, 'thigh_plate', [-2.3, .7, -2.7], [4.6, 3.5 if heavy else 2.8, .8], 2)
            box(leg, 'knee_frame', [-2.4, 5, -2.9], [4.8, 2.8, 1], 3)
            box(leg, 'knee_plate', [-1.8, 5.4, -3.05], [3.6, 2, .3], 2)
            if heavy: box(leg, 'orange_thigh', [-.5, 1, -2.8], [1, 3, .2], 6)
            elif profile in ('ranger', 'starwarden'): box(leg, 'leg_channel', [-1.7, 1, -2.85], [.45, 3, .25], 6)
        return parts
    # Cuboid shell and separate inset visor geometry; head is fully helmeted.
    box('head', 'helmet_shell', [-4.4, -8.4, -4.4], [8.8, 8.8, 8.8], 2)
    box('head', 'visor_recess', [-3.6, -5.6, -4.58], [7.2, 3.8, .25], 0)
    box('head', 'brow', [-4.5, -6.5, -4.8], [9, .8, .65], 3)
    box('head', 'chin_guard', [-4.5, -1.7, -4.8], [9, 1.2, .65], 3)
    box('head', 'helmet_crown_plate', [-3.7, -8.65, -3.7], [7.4, .35, 7.4], 3)
    box('head', 'rear_helmet_panel', [-3.7, -7.7, 4.4], [7.4, 2.3, .4], 3)
    box('head', 'rear_helmet_inset', [-2.7, -7.3, 4.8], [5.4, 1.4, .2], 2)
    for x in (-4.8, 3.9): box('head', 'helmet_side', [x, -5.8, -4.7], [.9, 4.5, .6], 3)
    if profile == 'pioneer':
        box('head', 'lamp_housing', [-1.4, -8.2, -5], [2.8, 2.7, .8], 1)
        box('head', 'amber_mining_lamp', [-1, -7.8, -5.3], [2, 1.9, .35], 6)
        box('head', 'lamp_lens', [-.5, -7.3, -5.4], [1, .9, .15], 7)
    elif heavy:
        box('head', 'orange_brow', [-.7, -8.6, -4.9], [1.4, 2.8, .35], 6)
        box('head', 't_visor_left', [-3.4, -4.6, -4.9], [2.5, 3.1, .4], 2)
        box('head', 't_visor_right', [.9, -4.6, -4.9], [2.5, 3.1, .4], 2)
    else:
        box('head', 'lit_visor', [-3.5, -4.2, -5.1], [7, .8, .45], 7 if star else 6)
        box('head', 'sensor', [-5, -4.7, -1.5], [.7, 2, 2], 3)
        box('head', 'sensor_light', [-5.15, -4.3, -1], [.2, 1, 1], 6)
        if star: box('head', 'central_crest', [-.75, -9, -4.6], [1.5, 3, .7], 6)
    box('body', 'torso_undersuit', [-4.34, -.3, -2.34], [8.68, 12.6, 4.68], 1)
    box('body', 'chest_plate', [-4.4, .8, -2.8], [8.8, 7.6, .9], 2)
    box('body', 'back_plate', [-4.3, .8, 2], [8.6, 7.5, .8], 2)
    box('body', 'lower_belt', [-4.2, 9.4, -2.6], [8.4, 1.5, 5.2], 1)
    if profile == 'pioneer':
        box('body', 'amber_chest_stripe', [-3.8, 2.7, -2.95], [7.6, .6, .2], 6)
        for x in (-3.5, .6): box('body', 'utility_pocket', [x, 5, -3.4], [2.9, 2.8, .8], 1)
        box('body', 'back_identification', [-3.5, 2, 2.8], [7, .55, .2], 6)
    elif heavy:
        for x in (-4.1, .3):
            box('body', 'stacked_breastplate', [x, 1.6, -3.35], [3.8, 3.1, .8], 3)
            box('body', 'back_reinforcement', [x, 1.3, 2.8], [3.8, 5.4, .6], 2)
        box('body', 'abdomen_plate', [-2.9, 5.6, -3], [5.8, 2.2, .7], 3)
        box('body', 'orange_abdomen', [-.65, 6.1, -3.75], [1.3, 1.3, .2], 6)
        box('body', 'back_spine', [-.5, 1, 3.5], [1, 7.5, .5], 3)
    elif star:
        box('body', 'core_frame', [-1.9, 2, -3.5], [3.8, 3.8, .7], 3)
        box('body', 'starcore', [-1.2, 2.7, -3.8], [2.4, 2.4, .4], 6)
        box('body', 'core_light', [-.6, 3.3, -3.95], [1.2, 1.2, .2], 7)
        for cube in parts['body']:
            if cube['name'] in ('core_frame', 'starcore', 'core_light'):
                cube['rotation'] = {'origin': [0, 3.9, -3.6], 'axis': 'z', 'angle': 45}
        for x in (-3, 2.6): box('body', 'power_channel', [x, 6, -3], [.4, 2.8, .25], 6)
        box('body', 'back_distribution', [-.5, 1.2, 2.9], [1, 6, .4], 6)
        for x in (-4.5, 2): box('body', 'angular_hip_plate', [x, 9, -2.9], [2.5, 2.8, 1], 2)
    else:
        box('body', 'light_harness', [-2.6, 2, -3], [5.2, 1.2, .3], 5)
        box('body', 'ranger_status', [-1.5, 3.5, -3.1], [3, .4, .3], 6)
        for y in (1.5, 4, 6.5): box('body', 'spine_segment', [-1.6, y, 2.8], [3.2, 1.4, .5], 3)
    for arm, sign in [('right_arm', -1), ('left_arm', 1)]:
        x = -3 if sign < 0 else -1
        # Cover the player's optional jacket/sleeve skin layer as well as the base skin.
        box(arm, 'undersleeve', [x - .34, -2.34, -2.34], [4.68, 12.68, 4.68], 1)
        shoulder_width = 5.8 if heavy else 4.7 if star else 4 if profile == 'pioneer' else 3.8
        box(arm, 'shoulder_guard', [x - .35, -2.5, -2.5], [shoulder_width, 3.5 if heavy else 2.8, 5], 3)
        box(arm, 'shoulder_plate', [x - .2, -2.6, -2.6], [shoulder_width - .3, 2.2, 5.2], 2)
        if heavy or star:
            box(arm, 'shoulder_outer_rim', [x - .45, -2.85, -2.8], [shoulder_width + .2, .55, 5.6], 3)
            box(arm, 'shoulder_lower_layer', [x - .5, .7, -2.6], [shoulder_width + .2, 1, 5.2], 3)
        box(arm, 'forearm_guard', [x - .5, 5, -2.7], [5, 3.8, 5.4], 3)
        box(arm, 'forearm_face', [x + .2, 5.4, -2.8], [3.6, 3, .4], 2)
        box(arm, 'identification', [x + 1.3, 5.8, -3], [.7, 2.1, .25], 6)
    for leg in ['right_leg', 'left_leg']:
        box(leg, 'armored_boot', [-2.25, 8, -2.3], [4.5, 4.2, 4.6], 2)
        box(leg, 'boot_cuff', [-2.4, 7.8, -2.45], [4.8, .9, 4.9], 3)
        box(leg, 'boot_toe', [-2.3, 10.5, -3.3], [4.6, 1.5, 5.7], 3)
        box(leg, 'sole', [-2.4, 11.9, -3.4], [4.8, .5, 5.9], 1)
        box(leg, 'ankle_indicator', [-.6, 8.8, -2.55], [1.2, .9, .3], 6)
        if heavy: box(leg, 'heavy_shin', [-2.4, 6.3, -2.8], [4.8, 3.5, .8], 3)
    return parts


def generate_armor(profile):
    image = atlas(PALETTES[profile], 64, 256)
    for layer in (1, 2): image.save(ASSETS / f'textures/models/armor/{profile}_layer_{layer}.png')
    for part in ('helmet', 'chestplate', 'leggings', 'boots'):
        armor_icon(profile, part).save(ASSETS / f'textures/item/{profile}_{part}.png')
    geometry = {name: {'outer': armor_geometry(name, False), 'inner': armor_geometry(name, True)} for name in PALETTES}
    write(ASSETS / 'equipment/armor_geometry.json', geometry)
    for layer in ('outer', 'inner'):
        export_blockbench(profile + '_' + layer, geometry[profile][layer], image, 256, 'modded_entity')


def export_blockbench(name, geometry, image, size, model_format):
    """Editable Blockbench exports share exact source coordinates and material UVs."""
    elements, outliner = [], []
    def uid(label): return str(uuid.uuid5(uuid.NAMESPACE_URL, 'starboundmc/equipment/' + name + '/' + label))
    entries = {'root': geometry} if isinstance(geometry, list) else geometry
    for part, cubes in entries.items():
        children = []
        pivot = {'right_arm': [-5, 22, 0], 'left_arm': [5, 22, 0],
                 'right_leg': [-1.9, 12, 0], 'left_leg': [1.9, 12, 0]}.get(part, [0, 24, 0])
        if model_format == 'java_block': pivot = [0, 0, 0]
        for index, cube in enumerate(cubes):
            start = cube['from']; end = cube.get('to') or [a + b for a, b in zip(start, cube['size'])]
            if model_format == 'modded_entity':
                start, end = [start[0] + pivot[0], pivot[1] - end[1], start[2]], [end[0] + pivot[0], pivot[1] - start[1], end[2]]
            i = cube['material']; tile = size // 4; u, v = i % 4 * tile, i // 4 * tile
            ident = uid(part + '/' + str(index)); children.append(ident)
            elements.append({'name': cube['name'], 'uuid': ident, 'type': 'cube', 'from': start, 'to': end,
                             'origin': pivot, 'box_uv': False, 'faces': {f: {'uv': [u + 1, v + 1, u + tile - 1, v + tile - 1], 'texture': 0} for f in FACES}})
            if 'rotation' in cube:
                rotation_origin = cube['rotation']['origin']
                elements[-1]['origin'] = ([rotation_origin[0] + pivot[0], pivot[1] - rotation_origin[1], rotation_origin[2]]
                                          if model_format == 'modded_entity' else rotation_origin)
                elements[-1]['rotation'] = [0, 0, -cube['rotation']['angle'] if model_format == 'modded_entity' else cube['rotation']['angle']]
        outliner.append({'name': part, 'uuid': uid(part), 'origin': pivot, 'children': children})
    write(EDITABLE / f'{name}.bbmodel', {'meta': {'format_version': '4.10', 'model_format': model_format, 'box_uv': False},
          'name': name, 'resolution': {'width': size, 'height': size}, 'elements': elements, 'outliner': outliner,
          'textures': [{'name': name + '.png', 'id': '0', 'uuid': uid('texture'), 'source': 'data:image/png;base64,' + base64.b64encode(image.png()).decode(), 'width': size, 'height': size}]})
