"""Historical V03 exporter; current resources use equipment_mesh_export.py."""
import base64
import json
from pathlib import Path
import re

PROJECT = Path(__file__).resolve().parents[1]
SOURCE = PROJECT / 'dev-assets/equipment'
ASSETS = PROJECT / 'src/main/resources/assets/starboundmc'
BONES = ('head', 'hat', 'body', 'right_arm', 'left_arm', 'right_leg', 'left_leg')


def load(name):
    model = json.loads((SOURCE / f'{name}.bbmodel').read_text(encoding='utf-8'))
    if model['meta']['model_format'] not in ('java_block', 'modded_entity'):
        raise ValueError(f'{name}: unsupported Blockbench format')
    if len(model['textures']) != 1 or not model['textures'][0].get('source', '').startswith('data:image/png;base64,'):
        raise ValueError(f'{name}: embed exactly one PNG before saving')
    for cube in model['elements']:
        if cube['type'] != 'cube' or cube.get('inflate', 0):
            raise ValueError(f'{name}: use plain cuboids without inflation')
        if any(b <= a for a, b in zip(cube['from'], cube['to'])):
            raise ValueError(f'{name}: cube has nonpositive size: {cube["name"]}')
    return model


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


def texture(model, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(base64.b64decode(model['textures'][0]['source'].split(',', 1)[1]))


def export_weapon(name):
    model = load(name)
    if model.get('java_block_version') != '1.9.0':
        raise ValueError(f'{name}: select Minecraft 1.9–1.21.5 in Blockbench project settings')
    for group in model['groups']:
        if any(group.get('rotation', [0, 0, 0])):
            raise ValueError(f'{name}: bake group rotations into cuboids before Java export')
    elements = []
    for cube in model['elements']:
        element = {key: cube[key] for key in ('name', 'from', 'to')}
        if any(v < -16 or v > 32 for key in ('from', 'to') for v in cube[key]):
            raise ValueError(f'{name}: native Java model bounds exceeded')
        rotation = cube.get('rotation', [0, 0, 0])
        axes = [i for i, angle in enumerate(rotation) if angle]
        if axes:
            if len(axes) != 1 or rotation[axes[0]] not in (-45, -22.5, 22.5, 45):
                raise ValueError(f'{name}: Java cuboid rotation must be one supported axis')
            element['rotation'] = {'angle': rotation[axes[0]], 'axis': 'xyz'[axes[0]], 'origin': cube['origin']}
        element['faces'] = {}
        for face, data in cube['faces'].items():
            if data.get('texture') is None:
                continue
            uv = [value * 16 / model['resolution']['width' if i % 2 == 0 else 'height'] for i, value in enumerate(data['uv'])]
            result = {'uv': uv, 'texture': '#0'}
            if data.get('rotation'):
                result['rotation'] = data['rotation']
            element['faces'][face] = result
        elements.append(element)
    hand = {'credit': 'Made and validated in Blockbench 5.2.2',
            'textures': {'0': f'starboundmc:item/{name}_model', 'particle': f'starboundmc:item/{name}_model'},
            'elements': elements, 'display': model['display']}
    write(ASSETS / f'models/item/{name}_hand.json', hand)
    texture(model, ASSETS / f'textures/item/{name}_model.png')


def armor_geometry(profile, inner):
    model = load(profile + ('_inner' if inner else '_outer'))
    definitions = {g['uuid']: g for g in model['groups']}
    elements = {c['uuid']: c for c in model['elements']}
    parts = {part: [] for part in BONES}
    seen = set()
    for tree in model['outliner']:
        group = definitions[tree['uuid']]
        part, pivot = group['name'], group['origin']
        if part not in parts or any(group.get('rotation', [0, 0, 0])):
            raise ValueError(f'{profile}: keep seven original humanoid bones without group rotations')
        expected = {'right_arm': [-5, 22, 0], 'left_arm': [5, 22, 0],
                    'right_leg': [-1.9, 12, 0], 'left_leg': [1.9, 12, 0]}.get(part, [0, 24, 0])
        if pivot != expected:
            raise ValueError(f'{profile}: do not move the {part} bone pivot')
        for ident in tree['children']:
            cube = elements[ident]
            seen.add(ident)
            match = re.search(r'\[m(\d+)\]$', cube['name'])
            if not match or not cube.get('box_uv'):
                raise ValueError(f'{profile}: preserve material suffix [m0..7] and box UV')
            material = int(match[1])
            if not 0 <= material < 8:
                raise ValueError(f'{profile}: unknown material')
            start, end = cube['from'], cube['to']
            width, height, depth = [b - a for a, b in zip(start, end)]
            u, v = material % 4 * 64, material // 4 * 64
            expected = [u + depth, v + depth, u + depth + width, v + depth + height]
            if any(abs(actual - wanted) > .0001 for actual, wanted in zip(cube['faces']['north']['uv'], expected)):
                raise ValueError(f'{profile}: box UV offset disagrees with material suffix on {cube["name"]}')
            node = {'name': cube['name'].split(' [m')[0], 'from': [start[0] - pivot[0], pivot[1] - end[1], start[2]],
                    'size': [b - a for a, b in zip(start, end)], 'material': material}
            rotation = cube.get('rotation', [0, 0, 0])
            if any(rotation):
                if rotation[0] or rotation[1]:
                    raise ValueError(f'{profile}: armor cuboids currently support Z-axis rotation')
                origin = cube['origin']
                node['rotation'] = {'origin': [origin[0] - pivot[0], pivot[1] - origin[1], origin[2]], 'axis': 'z', 'angle': -rotation[2]}
            parts[part].append(node)
    if seen != set(elements):
        raise ValueError(f'{profile}: every cube must belong directly to a humanoid bone')
    texture(model, ASSETS / f'textures/models/armor/{profile}_layer_{2 if inner else 1}.png')
    return parts
