# SPDX-License-Identifier: MPL-2.0
"""Deterministic, original pixel art and cuboid models for the first EPP generation.

Requires Pillow. Generated non-code artwork follows LICENSE-ASSETS.md.
No external art or existing project textures are modified.
"""
from pathlib import Path
import json
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/starboundmc'


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + '\n', encoding='utf-8')


def save(image, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)


def board():
    im = Image.new('RGBA', (32, 32))
    d = ImageDraw.Draw(im)
    d.rectangle((3, 5, 28, 26), fill='#142831', outline='#79959a', width=2)
    d.rectangle((5, 7, 26, 24), fill='#23564b')
    for x in (7, 11, 15, 19, 23):
        d.rectangle((x, 26, x + 1, 29), fill='#d8ba6a')
    for x, y, tx in ((6, 9, 14), (6, 20, 13), (23, 9, 19), (24, 22, 19)):
        d.line([(x, y), (tx, y), (tx, 16)], fill='#a5b977', width=1)
        d.rectangle((x, y, x + 1, y + 1), fill='#f1dd9e')
    d.rectangle((12, 12, 21, 20), fill='#0b1c27', outline='#7f969c')
    for y in (13, 16, 19):
        d.line((10, y, 11, y), fill='#d5ceaa')
        d.line((22, y, 23, y), fill='#d5ceaa')
    d.rectangle((15, 14, 18, 17), fill='#426677')
    save(im, ASSETS / 'textures/item/basic_circuit_board.png')


def canister(full):
    im = Image.new('RGBA', (32, 32))
    d = ImageDraw.Draw(im)
    d.rectangle((12, 2, 19, 5), fill='#435b65', outline='#0d1b25')
    d.rectangle((13, 1, 18, 2), fill='#d9b66c')
    d.rectangle((9, 7, 22, 27), fill='#142630')
    d.rectangle((11, 5, 20, 29), fill='#718995')
    d.rectangle((10, 8, 12, 25), fill='#adc3c8')
    d.rectangle((19, 8, 21, 25), fill='#314750')
    d.rectangle((10, 10, 21, 12), fill='#203d47')
    d.rectangle((10, 23, 21, 25), fill='#203d47')
    d.rectangle((13, 15, 18, 21), fill='#11242e')
    d.rectangle((14, 16, 17, 20), fill='#76d6d2' if full else '#354952')
    d.point((14, 16), fill='#d9ffff' if full else '#718995')
    save(im, ASSETS / f'textures/item/{"oxygen_canister" if full else "empty_oxygen_canister"}.png')


def surfaces():
    for name, base, edge in [('epp_plate', '#3f5562', '#93a7ad'), ('epp_tank', '#98adb5', '#d0dedd'),
                             ('epp_dark', '#20313c', '#526671')]:
        im = Image.new('RGB', (16, 16), base)
        d = ImageDraw.Draw(im)
        d.rectangle((0, 0, 15, 15), outline='#172a34')
        d.line((1, 1, 14, 1), fill=edge)
        d.line((1, 2, 1, 14), fill=edge)
        d.line((3, 13, 12, 13), fill='#2b404b')
        for x, y in [(2, 2), (13, 2), (2, 13), (13, 13)]:
            d.point((x, y), fill='#b5c6ca')
        save(im, ASSETS / f'textures/block/{name}.png')
    im = Image.new('RGB', (16, 16), '#102735')
    d = ImageDraw.Draw(im)
    d.rectangle((1, 1, 14, 14), outline='#426778')
    for y in (4, 7, 10):
        d.line((3, y, 11 - (y % 3), y), fill='#7de3d1')
    d.rectangle((11, 11, 12, 12), fill='#e6b975')
    save(im, ASSETS / 'textures/block/epp_display.png')


def box(start, end, texture):
    # UVs follow face dimensions: a narrow face uses a narrow strip of tileable material.
    x, y, z = (end[i] - start[i] for i in range(3))
    return {'from': start, 'to': end, 'faces': {
        direction: {'texture': '#' + texture, 'uv': [0, 0, w, h]}
        for direction, w, h in [('north', x, y), ('south', x, y), ('east', z, y), ('west', z, y), ('up', x, z), ('down', x, z)]}}


def service_assets():
    """Mk.II's integrated chassis and single thermal-module service tooling."""
    textures = {name: 'starboundmc:block/epp_' + name for name in ['plate', 'tank', 'dark', 'display']}
    textures['particle'] = textures['plate']
    old = json.loads((ASSETS / 'models/item/epp_mk1.json').read_text())
    pack = [box([4, 2, 5], [12, 14, 10], 'plate'),
            box([2, 4, 6], [4, 12, 10], 'tank'), box([12, 4, 6], [14, 12, 10], 'tank'),
            box([3, 1, 7], [6, 4, 11], 'dark'), box([10, 1, 7], [13, 4, 11], 'dark'),
            box([5, 9, 4], [11, 13, 5], 'display'),
            box([6, 4, 4], [10, 8, 5], 'dark'), box([7, 5, 3.5], [9, 7, 4], 'tank')]
    write_json(ASSETS / 'models/item/epp_mk2.json', {'textures': textures, 'elements': pack, 'display': old['display']})
    station = [box([0, 0, 0], [16, 2, 16], 'dark'), box([1, 2, 4], [15, 10, 15], 'plate'),
               box([0, 10, 2], [16, 12, 16], 'tank'), box([2, 12, 11], [14, 16, 14], 'plate'),
               box([3, 13, 10.5], [9, 15, 11], 'display'), box([11, 12, 10], [13, 15, 11], 'dark')]
    write_json(ASSETS / 'models/block/epp_service_station.json', {'parent': 'minecraft:block/block', 'textures': textures, 'elements': station})
    write_json(ASSETS / 'models/item/epp_service_station.json', {'parent': 'starboundmc:block/epp_service_station'})
    write_json(ASSETS / 'blockstates/epp_service_station.json', {'variants': {'': {'model': 'starboundmc:block/epp_service_station'}}})
    for name, heating in [('heating_module_1', True), ('epp_mk2_upgrade_kit', False)]:
        im = Image.new('RGBA', (32, 32)); d = ImageDraw.Draw(im)
        d.rectangle((4, 5, 27, 26), fill='#243842', outline='#99b4be', width=2)
        d.rectangle((7, 8, 24, 23), fill='#101f29', outline='#536f7b')
        for x in (9, 13, 17, 21):
            d.rectangle((x, 27, x+1, 29), fill='#dbbd78')
        if heating:
            d.line([(10, 11), (21, 11), (21, 15), (10, 15), (10, 19), (21, 19)], fill='#ee9960', width=2)
            d.line((10, 10, 20, 10), fill='#ffe1a3')
        else:
            d.rectangle((10, 10, 13, 21), fill='#70dad2')
            d.rectangle((18, 10, 21, 21), fill='#70dad2')
            d.line((10, 10, 13, 10), fill='#ddfffa')
            d.line((18, 10, 21, 10), fill='#ddfffa')
        save(im, ASSETS / f'textures/item/{name}.png')
        write_json(ASSETS / f'models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'starboundmc:item/' + name}})


def cooling_assets():
    im = Image.new('RGBA', (32, 32)); d = ImageDraw.Draw(im)
    d.rectangle((4, 5, 27, 26), fill='#243842', outline='#99b4be', width=2)
    d.rectangle((7, 8, 24, 23), fill='#101f29', outline='#536f7b')
    for x in (9, 13, 17, 21):
        d.rectangle((x, 27, x+1, 29), fill='#dbbd78')
    for x in (10, 14, 18, 22):
        d.line((x, 11, x, 20), fill='#7cd9f0', width=2)
    d.line((9, 10, 23, 10), fill='#dbfbff')
    d.line((9, 21, 23, 21), fill='#4193b9')
    save(im, ASSETS / 'textures/item/cooling_module_1.png')
    write_json(ASSETS / 'models/item/cooling_module_1.json', {
        'parent': 'minecraft:item/generated', 'textures': {'layer0': 'starboundmc:item/cooling_module_1'}})


def models():
    textures = {name: 'starboundmc:block/epp_' + name for name in ['plate', 'tank', 'dark', 'display']}
    textures['particle'] = textures['plate']
    epp = [box([5, 2, 6], [11, 14, 10], 'plate'),
           box([2, 3, 5], [5, 13, 10], 'tank'), box([11, 3, 5], [14, 13, 10], 'tank'),
           box([2, 4, 4.5], [5, 6, 10.5], 'dark'), box([11, 4, 4.5], [14, 6, 10.5], 'dark'),
           box([2, 10, 4.5], [5, 12, 10.5], 'dark'), box([11, 10, 4.5], [14, 12, 10.5], 'dark'),
           box([6, 7, 5], [10, 11, 6], 'display'), box([6, 1, 7], [10, 2, 9], 'dark')]
    write_json(ASSETS / 'models/item/epp_mk1.json', {'textures': textures, 'elements': epp,
        'display': {'gui': {'rotation': [20, -25, 0], 'translation': [0, 0, 0], 'scale': [.95, .95, .95]},
                    'ground': {'translation': [0, 3, 0], 'scale': [.5, .5, .5]},
                    'thirdperson_righthand': {'rotation': [0, 0, 0], 'scale': [.5, .5, .5]}}})
    station = [box([0, 0, 0], [16, 2, 16], 'dark'), box([1, 2, 3], [15, 15, 15], 'plate'),
               box([2, 4, 1], [6, 13, 3], 'tank'), box([9, 8, 2], [14, 13, 3], 'display'),
               box([8, 3, 1], [14, 6, 4], 'dark')]
    write_json(ASSETS / 'models/block/life_support_station.json', {'parent': 'minecraft:block/block', 'textures': textures, 'elements': station})
    write_json(ASSETS / 'models/item/life_support_station.json', {'parent': 'starboundmc:block/life_support_station'})
    write_json(ASSETS / 'blockstates/life_support_station.json', {'variants': {'': {'model': 'starboundmc:block/life_support_station'}}})
    for name in ['basic_circuit_board', 'oxygen_canister', 'empty_oxygen_canister']:
        write_json(ASSETS / f'models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'starboundmc:item/' + name}})


if __name__ == '__main__':
    board()
    canister(True)
    canister(False)
    surfaces()
    models()
    service_assets()
    cooling_assets()
