"""Deterministic original pixel armor UVs, inventory icons, printing recipes and translations.
Uses only Python's standard library. No third-party image assets or dependencies.
"""
from pathlib import Path
import json
import struct
import zlib

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources'
TYPES = ['helmet', 'chestplate', 'leggings', 'boots']
ZH_PARTS = ['头盔', '胸甲', '护腿', '战靴']
SETS = [
    ('pioneer', '钨钢拓荒', 'Tungsten Pioneer', 'tungsten_ingot', 18, 3,
     (112, 123, 131), (233, 166, 61),
     ['采掘速度 +15%', '生命上限 +2', '移动速度 +4%', '安全跌落距离 +2 格'],
     ['Mining speed +15%', 'Max health +2', 'Movement speed +4%', 'Safe fall distance +2 blocks'],
     '四件套：额外采掘速度 +15%，生命上限 +2', '4 pieces: additional mining speed +15%, max health +2'),
    ('vanguard', '耐钢先锋', 'Durasteel Vanguard', 'durasteel_ingot', 55, 6,
     (73, 88, 103), (235, 103, 66),
     ['爆炸击退抗性 +15%', '生命上限 +4', '普通近战伤害 +1', '击退抗性 +10%'],
     ['Explosion knockback resistance +15%', 'Max health +4', 'Normal melee damage +1', 'Knockback resistance +10%'],
     '四件套：普通近战伤害额外 +2，击退抗性额外 +10%',
     '4 pieces: additional normal melee damage +2, knockback resistance +10%'),
    ('ranger', '钛合金游骑', 'Titanium Ranger', 'titanium_ingot', 45, 5,
     (53, 100, 91), (77, 232, 178),
     ['弹射物命中伤害 +5%', '生命上限 +2', '移动速度 +8%', '安全跌落距离 +3 格'],
     ['Projectile hit damage +5%', 'Max health +2', 'Movement speed +8%', 'Safe fall distance +3 blocks'],
     '四件套：弹射物伤害额外 +10%，移动速度额外 +4%',
     '4 pieces: additional projectile damage +10%, movement speed +4%'),
    ('starwarden', '星核守望', 'Starcore Warden', 'star_core_fragment', 150, 10,
     (77, 68, 111), (128, 180, 255),
     ['采掘速度 +25%', '生命上限 +6', '普通近战伤害 +2', '安全跌落距离 +4 格'],
     ['Mining speed +25%', 'Max health +6', 'Normal melee damage +2', 'Safe fall distance +4 blocks'],
     '四件套：生命上限额外 +4，攻速 +10%，弹射物伤害 +15%',
     '4 pieces: additional max health +4, attack speed +10%, projectile damage +15%'),
]


def write(path, obj):
    target = ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(obj, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


class Pixels:
    def __init__(self, w, h):
        self.w, self.h = w, h
        self.rows = [[(0, 0, 0, 0) for _ in range(w)] for _ in range(h)]

    def rect(self, x, y, w, h, color):
        for py in range(y, y + h):
            for px in range(x, x + w):
                self.rows[py][px] = (*color[:3], color[3] if len(color) == 4 else 255)

    def save(self, path):
        def chunk(kind, data):
            return struct.pack('!I', len(data)) + kind + data + struct.pack('!I', zlib.crc32(kind + data))
        raw = b''.join(b'\0' + bytes(v for pixel in row for v in pixel) for row in self.rows)
        content = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('!2I5B', self.w, self.h, 8, 6, 0, 0, 0))
        content += chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b'')
        target = ROOT / path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(content)


def shade(color, amount):
    return tuple(max(0, min(255, value + amount)) for value in color)


def armor_uv(name, base, accent, layer):
    image = Pixels(64, 32)
    # Vanilla biped armor UV islands: helmet, torso, arm and mirrored leg.
    islands = [(16, 16, 24, 16), (0, 16, 16, 16)]
    if layer == 1:
        islands += [(0, 0, 32, 16), (40, 16, 16, 16)]
    for x, y, w, h in islands:
        image.rect(x, y, w, h, base)
        for row in range(y, y + h):
            image.rect(x, row, w, 1, shade(base, 10 if row % 4 == 0 else -8 if row % 4 == 3 else 0))
    # Chest armor panels, central power cell, waist belt and knee / shin strips.
    image.rect(20, 20, 8, 12, shade(base, 20))
    image.rect(23, 22, 2, 4, accent)
    image.rect(20, 29, 8, 2, shade(base, -35))
    image.rect(4, 23, 4, 2, accent)
    image.rect(4, 28, 4, 3, shade(base, -30))
    if layer == 1:
        # Opaque dark visor framed by a colored brow; original face remains covered.
        image.rect(8, 9, 8, 1, accent)
        image.rect(8, 10, 8, 3, (16, 25, 36))
        image.rect(9, 10, 6, 1, shade(accent, -15))
        image.rect(44, 20, 4, 3, accent)
        image.rect(44, 29, 4, 3, shade(base, -30))
        if name == 'vanguard':
            image.rect(20, 20, 3, 8, shade(base, 35)); image.rect(25, 20, 3, 8, shade(base, 35))
        if name == 'starwarden':
            image.rect(20, 25, 8, 1, accent); image.rect(11, 2, 2, 5, accent)
    image.save(f'assets/starboundmc/textures/models/armor/{name}_layer_{layer}.png')


def icon(name, part, base, accent):
    image = Pixels(16, 16)
    dark, light = shade(base, -35), shade(base, 30)
    if part == 'helmet':
        image.rect(3, 3, 10, 10, dark); image.rect(4, 3, 8, 4, light)
        image.rect(3, 7, 10, 2, accent); image.rect(5, 9, 6, 4, (0, 0, 0, 0))
    elif part == 'chestplate':
        image.rect(2, 3, 12, 5, dark); image.rect(4, 5, 8, 9, base)
        image.rect(5, 6, 6, 5, light); image.rect(7, 7, 2, 4, accent)
        image.rect(6, 3, 4, 2, (0, 0, 0, 0))
    elif part == 'leggings':
        image.rect(4, 2, 8, 5, base); image.rect(4, 7, 3, 7, light); image.rect(9, 7, 3, 7, light)
        image.rect(4, 9, 3, 2, accent); image.rect(9, 9, 3, 2, accent)
    else:
        image.rect(3, 5, 4, 8, base); image.rect(9, 5, 4, 8, base)
        image.rect(2, 11, 5, 3, dark); image.rect(9, 11, 5, 3, dark)
        image.rect(3, 6, 4, 2, accent); image.rect(9, 6, 4, 2, accent)
    image.save(f'assets/starboundmc/textures/item/{name}_{part}.png')


def main():
    langs = {lang: json.loads((ROOT / f'assets/starboundmc/lang/{lang}.json').read_text(encoding='utf-8')) for lang in ['zh_cn', 'en_us']}
    for name, zh, en, material, cost, seconds, base, accent, zhtips, entips, zhset, enset in SETS:
        for layer in [1, 2]: armor_uv(name, base, accent, layer)
        for i, part in enumerate(TYPES):
            item = f'{name}_{part}'
            icon(name, part, base, accent)
            write(f'assets/starboundmc/models/item/{item}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'starboundmc:item/{item}'}})
            amount = [5, 8, 7, 4][i]
            materials = [{'ingredient': {'item': f'starboundmc:{material}'}, 'count': amount},
                         {'ingredient': {'item': 'starboundmc:voxel'}, 'count': cost * [1, 2, 2, 1][i]}]
            if name != 'pioneer': materials.append({'ingredient': {'item': 'starboundmc:basic_circuit_board'}, 'count': 2 if name != 'starwarden' else 4})
            if name == 'starwarden': materials.append({'ingredient': {'item': 'starboundmc:durasteel_ingot'}, 'count': 4})
            write(f'data/starboundmc/recipe/print_{item}.json', {'type': 'starboundmc:voxel_printing', 'materials': materials,
                  'print_seconds': seconds + i, 'result': {'id': f'starboundmc:{item}', 'count': 1}})
            for lang, label, tip in [('zh_cn', zh + ZH_PARTS[i], zhtips[i]), ('en_us', en + ' ' + part.title(), entips[i])]:
                langs[lang]['item.starboundmc.' + item] = label
                langs[lang][f'armor.starboundmc.{name}.{part}'] = tip
        langs['zh_cn'][f'armor.starboundmc.{name}.set'] = zhset
        langs['en_us'][f'armor.starboundmc.{name}.set'] = enset
    for lang, data in langs.items(): write(f'assets/starboundmc/lang/{lang}.json', data)
    category = 'data/starboundmc/tags/item/printing/survival.json'
    data = json.loads((ROOT / category).read_text(encoding='utf-8'))
    for config in SETS:
        for part in TYPES:
            value = f'starboundmc:{config[0]}_{part}'
            if value not in data['values']: data['values'].append(value)
    write(category, data)
    for part, tag in zip(TYPES, ['head_armor', 'chest_armor', 'leg_armor', 'foot_armor']):
        path = f'data/minecraft/tags/item/{tag}.json'
        target = ROOT / path
        data = json.loads(target.read_text()) if target.exists() else {'replace': False, 'values': []}
        for config in SETS:
            value = f'starboundmc:{config[0]}_{part}'
            if value not in data['values']: data['values'].append(value)
        write(path, data)


if __name__ == '__main__':
    main()
