"""Deterministic code-native weapon models, bilingual text and manufacturing recipes.
Run with --stage 1, 2, or 3 while bringing up each weapon tier.
"""
import argparse
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources'
WEAPONS = [
    ('tungsten_sabre', '钨钢军刀', 'Tungsten Sabre', 1, 'blade', 'iron_block',
     '轻型近战武器；主手攻击，适合连续交锋。', 'Light melee weapon for sustained close combat.'),
    ('coil_pistol', '线圈手枪', 'Coil Pistol', 1, 'pistol', 'copper_block',
     '主手右键射击；消耗铁粒，弹道被墙体阻挡。', 'Fire in main hand. Uses iron nuggets; walls stop shots.'),
    ('durasteel_glaive', '耐钢战戟', 'Durasteel Glaive', 2, 'glaive', 'emerald_block',
     '攻击恢复后主手右键：扇形震击；消耗 8 耐久，冷却 3 秒。', 'Right-click at full attack recovery: shock sweep; costs 8 durability, 3s cooldown.'),
    ('plasma_carbine', '等离子卡宾枪', 'Plasma Carbine', 2, 'carbine', 'emerald_block',
     '主手右键射击；消耗红石，命中后点燃 2 秒。', 'Fire in main hand. Uses redstone; ignites targets for 2s.'),
    ('starcore_blade', '星核裁决刃', 'Starcore Verdict', 3, 'greatblade', 'amethyst_block',
     '攻击恢复后主手右键：星核弧斩；消耗 16 耐久，冷却 5 秒。', 'Right-click at full attack recovery: stellar arc; costs 16 durability, 5s cooldown.'),
    ('starcore_railgun', '星核轨道炮', 'Starcore Railgun', 3, 'railgun', 'amethyst_block',
     '主手按住右键蓄力，1 秒后可发射，2 秒满蓄；18–30 伤害，最多穿透 3 敌人。',
     'Hold in main hand: release after 1s, full charge at 2s; 18–30 damage, pierces up to 3 foes.'),
]

def write(path, value):
    target = ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

def printing(name, materials, cost, seconds, count=1, recipe=None):
    entries = [{'ingredient': {'item': item}, 'count': n} for item, n in materials]
    entries.append({'ingredient': {'item': 'starboundmc:voxel'}, 'count': cost})
    write(f'data/starboundmc/recipe/{recipe or "print_" + name}.json', {
        'type': 'starboundmc:voxel_printing', 'materials': entries, 'print_seconds': seconds,
        'result': {'id': 'starboundmc:' + name, 'count': count}})

def model(kind, accent):
    cubes = []
    gun = kind in ('pistol', 'carbine', 'railgun')
    def box(start, end, texture):
        cubes.append({'from': start, 'to': end, 'faces': {
            face: {'texture': '#' + texture} for face in ['north', 'south', 'east', 'west', 'up', 'down']}})
    if kind in ('blade', 'glaive', 'greatblade'):
        box([7, 0, 7], [9, 5, 9], 'grip')
        box([4, 4, 6.5], [12, 5.5, 9.5], 'metal')
        width = 2 if kind == 'blade' else 3
        box([8-width, 5.5, 7], [8+width, 13.5, 9], 'metal')
        box([7.5, 5.5, 6.8], [8.5, 14.5, 9.2], 'accent')
        box([7, 13.5, 7], [9, 16, 9], 'metal')
        if kind == 'glaive':
            box([3, 10, 7], [6, 14, 9], 'accent')
            box([10, 8, 7], [12, 12, 9], 'metal')
        if kind == 'greatblade':
            box([3, 6, 7], [5, 12, 9], 'accent')
            box([11, 6, 7], [13, 12, 9], 'accent')
    else:
        box([3, 2, 7], [6, 8, 9], 'grip')
        box([2, 7, 6], [11, 11, 10], 'metal')
        box([5, 8, 5.8], [9, 10, 10.2], 'accent')
        box([10, 8, 7], [16, 10, 9], 'grip')
        box([7, 11, 7], [9, 12.5, 9], 'grip')
        if kind != 'pistol':
            box([0, 5, 6.5], [3, 10, 9.5], 'grip')
            box([8, 4, 7], [10, 7, 9], 'metal')
        if kind == 'railgun':
            box([10, 7, 5.5], [16, 11, 6.5], 'accent')
            box([10, 7, 9.5], [16, 11, 10.5], 'accent')
    return {'credit': 'StarboundMC code-native weapon geometry', 'textures': {
        'particle': 'minecraft:block/iron_block', 'metal': 'minecraft:block/iron_block',
        'grip': 'minecraft:block/deepslate_tiles', 'accent': 'minecraft:block/' + accent},
        'elements': cubes, 'display': {
            'gui': {'rotation': [25, 225, 0], 'translation': [0, 0, 0], 'scale': [0.95]*3},
            'ground': {'translation': [0, 3, 0], 'scale': [0.45]*3},
            'fixed': {'rotation': [0, 180, 0], 'scale': [0.8]*3},
            'firstperson_righthand': {'rotation': [0, 110, 0] if gun else [0, -90, 25],
                                      'translation': [-1.5, 3.2, -2] if gun else [1.13, 3.2, 1.13], 'scale': [0.68]*3},
            'firstperson_lefthand': {'rotation': [0, -70, 0] if gun else [0, 90, -25],
                                     'translation': [-1.5, 3.2, -2] if gun else [1.13, 3.2, 1.13], 'scale': [0.68]*3},
            'thirdperson_righthand': {'rotation': [0, -90, 55], 'translation': [0, 4, 0.5], 'scale': [0.85]*3},
            'thirdperson_lefthand': {'rotation': [0, 90, -55], 'translation': [0, 4, 0.5], 'scale': [0.85]*3}}}

def main(stage):
    translations = {lang: json.loads((ROOT / f'assets/starboundmc/lang/{lang}.json').read_text(encoding='utf-8'))
                    for lang in ('zh_cn', 'en_us')}
    for name, zh, en, tier, kind, accent, zhtip, entip in WEAPONS:
        if tier > stage: continue
        write(f'assets/starboundmc/models/item/{name}.json', model(kind, accent))
        for lang, label, tip in [('zh_cn', zh, zhtip), ('en_us', en, entip)]:
            translations[lang]['item.starboundmc.' + name] = label
            translations[lang]['item.starboundmc.' + name + '.tooltip'] = tip
    common = {
        'no_ammo': ('需要 %s × %s', 'Requires %s × %s'),
        'ammo': ('每发消耗：%s × %s', 'Per shot: %s × %s'),
        'gun_stats': ('伤害上限 %s · 射程 %s 格 · 冷却 %s 秒', 'Max damage %s · Range %s blocks · Cooldown %ss'),
        'recover': ('等待攻击恢复后再发动技能', 'Wait for your attack to recover before using the skill'),
    }
    for key, (zh, en) in common.items():
        translations['zh_cn']['weapon.starboundmc.' + key] = zh
        translations['en_us']['weapon.starboundmc.' + key] = en
    for lang, data in translations.items(): write(f'assets/starboundmc/lang/{lang}.json', data)
    m = lambda name: 'starboundmc:' + name
    printing('survival_knife', [('minecraft:iron_ingot', 2)], 5, 1)
    printing('tungsten_ingot', [('minecraft:iron_ingot', 4), ('minecraft:copper_ingot', 4), ('minecraft:coal', 2)], 20, 3, 2, 'synthesize_tungsten')
    printing('tungsten_sabre', [(m('tungsten_ingot'), 4), ('minecraft:iron_ingot', 2)], 30, 3)
    printing('coil_pistol', [(m('tungsten_ingot'), 4), (m('basic_circuit_board'), 2), ('minecraft:copper_ingot', 4)], 60, 5)
    if stage >= 2:
        printing('titanium_ingot', [(m('tungsten_ingot'), 2), ('minecraft:amethyst_shard', 4), ('minecraft:iron_ingot', 4)], 40, 5, 2, 'synthesize_titanium')
        printing('durasteel_ingot', [(m('titanium_ingot'), 4), ('minecraft:obsidian', 2), ('minecraft:redstone', 4)], 60, 6, 2, 'synthesize_durasteel')
        printing('durasteel_glaive', [(m('durasteel_ingot'), 6), (m('titanium_ingot'), 4), (m('basic_circuit_board'), 2)], 150, 8)
        printing('plasma_carbine', [(m('durasteel_ingot'), 6), (m('titanium_ingot'), 4), (m('basic_circuit_board'), 4)], 200, 10)
    if stage >= 3:
        printing('star_core_fragment', [(m('durasteel_ingot'), 4), ('minecraft:diamond', 2), ('minecraft:echo_shard', 1), (m('fuel_crystal'), 4)], 200, 12, 2, 'synthesize_star_core')
        printing('starcore_blade', [(m('star_core_fragment'), 8), (m('durasteel_ingot'), 8), (m('basic_circuit_board'), 4)], 400, 15)
        printing('starcore_railgun', [(m('star_core_fragment'), 12), (m('durasteel_ingot'), 8), (m('basic_circuit_board'), 6)], 500, 20)
    tag_path = ROOT / 'data/starboundmc/tags/item/printing/survival.json'
    tag = json.loads(tag_path.read_text(encoding='utf-8'))
    for name in ['survival_knife'] + [w[0] for w in WEAPONS if w[3] <= stage]:
        if m(name) not in tag['values']: tag['values'].append(m(name))
    write('data/starboundmc/tags/item/printing/survival.json', tag)
    write('data/minecraft/tags/item/swords.json', {'replace': False, 'values': [
        m(w[0]) for w in WEAPONS if w[3] <= stage and w[4] in ('blade', 'glaive', 'greatblade')]})
    write('data/minecraft/tags/item/enchantable/durability.json', {'replace': False, 'values': [
        m(w[0]) for w in WEAPONS if w[3] <= stage and w[4] in ('pistol', 'carbine', 'railgun')]})

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--stage', type=int, choices=[1, 2, 3], default=3)
    main(parser.parse_args().stage)
