"""Package Blockbench's locker OBJ export; geometry remains authored in Blockbench."""
import base64
import json
import math
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ART = ROOT / 'docs/models'
ASSETS = ROOT / 'src/main/resources/assets/starboundmc'
MODELS = ASSETS / 'models/block'


def main():
    project = json.loads((ART / 'ship-locker-textured-v1.bbmodel').read_text(encoding='utf-8'))
    elements = {e['uuid']: e for e in project['elements']}
    props = {g['uuid']: g for g in project.get('groups', [])}
    groups = {(g.get('name') or props[g['uuid']]['name']):
              {elements[i]['name'] for i in g['children']} for g in project['outliner']}
    textures = {'m_' + t['uuid']: t for t in project['textures']}
    arrays = {'v': [], 'vt': [], 'vn': []}
    objects, used = [], {}
    current, material = None, None
    for line in (ART / 'ship-locker-export.obj').read_text().splitlines():
        if not line.strip():
            continue
        key, *values = line.split()
        if key in arrays:
            vector = tuple(map(float, values))
            assert all(math.isfinite(v) for v in vector)
            arrays[key].append(vector)
        elif key == 'o':
            current = {'name': line[2:], 'faces': []}
            objects.append(current)
        elif key == 'usemtl':
            texture = textures[values[0]]
            material = texture['name']
            used[material] = texture
        elif key == 'f':
            current['faces'].append((material, [tuple(int(i) - 1 for i in v.split('/')) for v in values]))
    assert len(objects) == len(elements)
    for name, texture in used.items():
        assert name.startswith('locker_')
        folder = 'effect' if name.startswith('locker_hologram') else 'block'
        path = ASSETS / f'textures/{folder}/{name}.png'
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(base64.b64decode(texture['source'].split(',', 1)[1]))

    def export(stem, names):
        selected = [o for o in objects if o['name'] in names]
        ids = [sorted({v[k] for o in selected for _, face in o['faces'] for v in face}) for k in range(3)]
        maps = [{old: new + 1 for new, old in enumerate(indices)} for indices in ids]
        lines = ['# Blockbench MCP; packaged by tools/export_ship_locker.py', 'mtllib ship_locker.mtl']
        for k, kind in enumerate(['v', 'vt', 'vn']):
            for index in ids[k]:
                lines.append(kind + ' ' + ' '.join(f'{v:.9g}' for v in arrays[kind][index]))
        last = None
        for number, obj in enumerate(selected):
            lines.append('o ' + re.sub('[^a-z0-9]+', '_', obj['name'].lower()) + '_' + str(number))
            for mat, face in obj['faces']:
                if last != mat:
                    lines.append('usemtl ' + mat)
                    last = mat
                lines.append('f ' + ' '.join('/'.join(str(maps[k][v[k]]) for k in range(3)) for v in face))
        (MODELS / f'{stem}.obj').write_text('\n'.join(lines) + '\n')
        model = {'parent': 'minecraft:block/block', 'loader': 'neoforge:obj',
                 'model': f'starboundmc:models/block/{stem}.obj', 'automatic_culling': False,
                 'shade_quads': True, 'flip_v': True, 'emissive_ambient': True,
                 'render_type': 'minecraft:cutout', 'ambientocclusion': False,
                 'textures': {'particle': 'starboundmc:block/ship_crate_side'}}
        (MODELS / f'{stem}.json').write_text(json.dumps(model, indent=2) + '\n')
        print(stem, len(selected), 'meshes')

    export('ship_locker_chassis', groups['cabinet'])
    export('ship_locker_left_door', groups['door_left'])
    export('ship_locker_right_door', groups['door_right'])
    export('ship_locker_complete', set(elements[i]['name'] for i in elements))
    mtl = '# Emissive optics supply surface brightness only, not world light.\n'
    for name in sorted(used):
        ambient = '0 0 0' if name == 'locker_surface_atlas' else '1 1 1'
        folder = 'effect' if name.startswith('locker_hologram') else 'block'
        mtl += f'\nnewmtl {name}\nKa {ambient}\nKd 1 1 1\nmap_Kd starboundmc:{folder}/{name}\n'
    (MODELS / 'ship_locker.mtl').write_text(mtl)
    (MODELS / 'ship_crate.json').write_text(json.dumps({'parent': 'starboundmc:block/ship_locker_chassis'}, indent=2) + '\n')
    item = {'parent': 'starboundmc:block/ship_locker_complete', 'display': {
        'gui': {'rotation': [20, 225, 0], 'translation': [0, 0, 0], 'scale': [.8, .8, .8]},
        'ground': {'translation': [0, 2.5, 0], 'scale': [.4, .4, .4]},
        'fixed': {'rotation': [0, 180, 0], 'translation': [0, 0, -3], 'scale': [.65, .65, .65]}}}
    (ASSETS / 'models/item/ship_crate.json').write_text(json.dumps(item, indent=2) + '\n')


if __name__ == '__main__':
    main()
