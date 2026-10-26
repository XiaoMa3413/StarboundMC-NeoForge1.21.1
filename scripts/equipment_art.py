"""Equipment pixel icons and resource export from native Blockbench model sources.

Run the weapon/armor resource generators. All production art is reproducible;
accepted V04 models retain their concept-projected textures and painted lighting.
"""
from pathlib import Path
import json
import math
import struct
import zlib
from equipment_mesh_export import export_armor, export_weapon

PROJECT = Path(__file__).resolve().parents[1]
ROOT = PROJECT / 'src/main/resources'
ASSETS = ROOT / 'assets/starboundmc'
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


def generate_weapon(name, kind):
    weapon_icon(kind).save(ASSETS / f'textures/item/{name}.png')
    export_weapon(name)
    write(ASSETS / f'models/item/{name}.json', {'loader': 'neoforge:separate_transforms',
          'base': {'parent': f'starboundmc:item/{name}_hand'}, 'perspectives': {'gui': {
              'parent': 'minecraft:item/generated', 'loader': 'neoforge:item_layers',
              'textures': {'layer0': f'starboundmc:item/{name}'}}}})


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
    # Inventory silhouettes reflect the differentiated wearable shells.
    if part == 'helmet':
        if profile == 'pioneer':
            im.rect(7, 13, 17, 7, p[5]); im.rect(8, 14, 1, 5, p[6])
        elif profile == 'ranger':
            im.rect(4, 13, 2, 5, p[6])
        elif star:
            for x in (7, 23): im.rect(x, 17, 2, 6, p[4])
    elif part == 'chestplate':
        if profile == 'pioneer':
            for x in (9, 21): im.rect(x, 7, 2, 17, p[5])
        elif heavy:
            for x in (1, 24):
                for y in (6, 9, 12): im.rect(x, y, 7, 2, p[3])
            for y in (17, 21): im.rect(10, y, 12, 1, p[3])
        elif profile == 'ranger':
            im.poly([(21, 6), (23, 7), (12, 26), (10, 25)], p[5])
            im.rect(23, 7, 5, 7, p[1]); im.rect(23, 9, 5, 1, p[5])
        else:
            im.poly([(1, 3), (10, 6), (9, 11), (3, 8)], p[4]); im.poly([(31, 3), (22, 6), (23, 11), (29, 8)], p[4])
            for x in (7, 20): im.rect(x, 27, 5, 3, p[3])
    elif part == 'leggings':
        for x in (7, 18):
            if heavy or star:
                for y in (11, 14, 17): im.rect(x, y, 7, 1, p[3])
            elif profile == 'pioneer': im.rect(x - 1, 10, 3, 6, p[2])
    im.outline(p[0]); return im


def generate_armor(profile):
    for part in ('helmet', 'chestplate', 'leggings', 'boots'):
        armor_icon(profile, part).save(ASSETS / f'textures/item/{profile}_{part}.png')
    export_armor()
