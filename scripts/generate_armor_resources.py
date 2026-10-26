"""Deterministic original pixel armor UVs, inventory icons, printing recipes and translations.
Uses only Python's standard library. No third-party image assets or dependencies.
"""
from pathlib import Path
import json
import equipment_art

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


def main():
    langs = {lang: json.loads((ROOT / f'assets/starboundmc/lang/{lang}.json').read_text(encoding='utf-8')) for lang in ['zh_cn', 'en_us']}
    for name, zh, en, material, cost, seconds, base, accent, zhtips, entips, zhset, enset in SETS:
        equipment_art.generate_armor(name)
        for i, part in enumerate(TYPES):
            item = f'{name}_{part}'
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
