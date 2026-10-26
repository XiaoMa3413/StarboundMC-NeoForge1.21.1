"""Deterministic code-native weapon models, bilingual text and manufacturing recipes.
Run with --stage 1, 2, or 3 while bringing up each weapon tier.
"""
import argparse
import json
import equipment_art
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources'
WEAPONS = [
    ('tungsten_sabre', '钨钢军刀', 'Tungsten Sabre', 1, 'blade', 'iron_block',
     '轻型近战武器；主手攻击，适合连续交锋。', 'Light melee weapon for sustained close combat.'),
    ('coil_pistol', '线圈手枪', 'Coil Pistol', 1, 'pistol', 'copper_block',
     '主手按住左键连射；每秒 5 发，消耗铁粒。', 'Hold left attack in main hand: 5 shots/s. Uses iron nuggets.'),
    ('durasteel_glaive', '耐钢战戟', 'Durasteel Glaive', 2, 'glaive', 'emerald_block',
     '攻击恢复后主手右键：5.5 格青绿震击扇面；消耗 8 耐久，冷却 3 秒。', 'Right-click at attack recovery: 5.5-block teal shock fan; costs 8 durability, 3s cooldown.'),
    ('plasma_carbine', '等离子卡宾枪', 'Plasma Carbine', 2, 'carbine', 'emerald_block',
     '主手按住左键连射；每秒 10 发，消耗红石，点燃 2 秒。', 'Hold left attack: 10 shots/s. Uses redstone; ignites targets for 2s.'),
    ('starcore_blade', '星核裁决刃', 'Starcore Verdict', 3, 'greatblade', 'amethyst_block',
     '攻击恢复后主手右键：蓝紫星核弧斩；消耗 16 耐久，冷却 5 秒。', 'Right-click at attack recovery: blue-violet stellar arc; costs 16 durability, 5s cooldown.'),
    ('starcore_railgun', '星核轨道炮', 'Starcore Railgun', 3, 'railgun', 'amethyst_block',
     '按住左键蓄力，0.5 秒后松开发射，1 秒满蓄；蓝紫拖尾，落点 3.5 格范围爆炸。',
     'Hold left attack: release after 0.5s, full charge at 1s; blue-violet trail, 3.5-block blast.'),
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

def main(stage):
    translations = {lang: json.loads((ROOT / f'assets/starboundmc/lang/{lang}.json').read_text(encoding='utf-8'))
                    for lang in ('zh_cn', 'en_us')}
    for name, zh, en, tier, kind, accent, zhtip, entip in WEAPONS:
        if tier > stage: continue
        equipment_art.generate_weapon(name, kind)
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
