"""Validate and package the actual Blockbench MCP transporter export (no geometry generation)."""
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
    project = json.loads((ART / 'transporter-textured-v1.bbmodel').read_text(encoding='utf-8'))
    elements = {e['uuid']: e for e in project['elements']}
    props = {g['uuid']: g for g in project.get('groups', [])}
    groups = {(g.get('name') or props[g['uuid']]['name']):
              {elements[i]['name'] for i in g['children']} for g in project['outliner']}
    assert set(groups) == {'lower', 'middle', 'upper'}
    textures = {'m_' + t['uuid']: t for t in project['textures']}
    arrays = {'v': [], 'vt': [], 'vn': []}
    objects, used = [], {}
    current, material = None, None
    for line in (ART / 'transporter-export.obj').read_text(encoding='utf-8').splitlines():
        if not line.strip():
            continue
        key, *values = line.split()
        if key in arrays:
            vector = tuple(map(float, values))
            assert all(math.isfinite(v) for v in vector)
            if key == 'vt':
                assert all(-1e-5 <= v <= 1.00001 for v in vector), vector
            arrays[key].append(vector)
        elif key == 'o':
            current = {'name': line[2:], 'faces': []}
            objects.append(current)
        elif key == 'usemtl':
            texture = textures[values[0]]
            material = texture['name']
            used[material] = texture
        elif key == 'f':
            face = [tuple(int(i) - 1 for i in v.split('/')) for v in values]
            assert len(face) in (3, 4)
            current['faces'].append((material, face))
    assert len(objects) == len(elements)
    assert {o['name'] for o in objects} == {e['name'] for e in elements.values()}
    for obj in objects:
        for material, face in obj['faces']:
            assert material in used
            for corner in face:
                assert len(corner) == 3
                for kind, index in zip(arrays, corner):
                    assert 0 <= index < len(arrays[kind])
    for name, texture in used.items():
        assert re.fullmatch(r'transporter_[a-z]+', name), name
        source = texture['source']
        assert source.startswith('data:image/png;base64,')
        (ASSETS / f'textures/block/{name}.png').write_bytes(base64.b64decode(source.split(',', 1)[1]))

    def export(stem, names, y_offset=0, height=1):
        selected = [o for o in objects if o['name'] in names]
        assert len(selected) == len(names)
        ids = [sorted({v[k] for o in selected for _, face in o['faces'] for v in face}) for k in range(3)]
        maps = [{old: new + 1 for new, old in enumerate(indices)} for indices in ids]
        lines = ['# Actual Blockbench MCP geometry; packaged by tools/export_transporter.py', 'mtllib transporter.mtl']
        for k, kind in enumerate(arrays):
            for index in ids[k]:
                vector = list(arrays[kind][index])
                if kind == 'v':
                    vector[1] -= y_offset
                    assert all(-1e-5 <= vector[i] <= 1.00001 for i in (0, 2)), (stem, vector)
                    assert -1e-5 <= vector[1] <= height + 1e-5, (stem, vector)
                lines.append(kind + ' ' + ' '.join(f'{v:.9g}' for v in vector))
        last = None
        for obj in selected:
            lines.append('o ' + obj['name'])
            for mat, face in obj['faces']:
                if last != mat:
                    lines.append('usemtl ' + mat)
                    last = mat
                lines.append('f ' + ' '.join('/'.join(str(maps[k][v[k]]) for k in range(3)) for v in face))
        (MODELS / f'{stem}.obj').write_text('\n'.join(lines) + '\n', encoding='utf-8')
        model = {'parent': 'minecraft:block/block', 'loader': 'neoforge:obj',
                 'model': f'starboundmc:models/block/{stem}.obj', 'automatic_culling': False,
                 'shade_quads': True, 'flip_v': True, 'emissive_ambient': True,
                 'render_type': 'minecraft:solid', 'ambientocclusion': False,
                 'textures': {'particle': 'starboundmc:block/transporter_titanium'}}
        (MODELS / f'{stem}.json').write_text(json.dumps(model, indent=2) + '\n', encoding='utf-8')
        print(stem, len(selected), 'meshes', sum(len(o['faces']) for o in selected), 'faces; bounds and indices valid')

    for part, name in enumerate(('lower', 'middle', 'upper')):
        export('transporter_' + name, groups[name], part)
    export('transporter_complete', {e['name'] for e in elements.values()}, height=3)
    mtl = '# Restrained emitter surfaces and instruments are emissive.\n'
    for name in sorted(used):
        ambient = '1 1 1' if name in {'transporter_light', 'transporter_cyan', 'transporter_instruments'} else '0 0 0'
        mtl += f'\nnewmtl {name}\nKa {ambient}\nKd 1 1 1\nmap_Kd starboundmc:block/{name}\n'
    (MODELS / 'transporter.mtl').write_text(mtl, encoding='utf-8')
    (MODELS / 'teleporter.json').write_text(json.dumps({'parent': 'starboundmc:block/transporter_lower'}, indent=2) + '\n')
    variants = {f'facing={facing},part={part}': {'model': 'starboundmc:block/transporter_' + name, 'y': angle}
                for facing, angle in [('north', 0), ('east', 90), ('south', 180), ('west', 270)]
                for part, name in enumerate(('lower', 'middle', 'upper'))}
    (ASSETS / 'blockstates/teleporter.json').write_text(json.dumps({'variants': variants}, indent=2) + '\n')
    item = {'parent': 'starboundmc:block/transporter_complete', 'display': {
        'gui': {'rotation': [15, 225, 0], 'translation': [0, -5, 0], 'scale': [.3, .3, .3]},
        'ground': {'translation': [0, 1, 0], 'scale': [.2, .2, .2]},
        'fixed': {'rotation': [0, 180, 0], 'translation': [0, -7, -1], 'scale': [.4, .4, .4]},
        'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, -2, 0], 'scale': [.22, .22, .22]},
        'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2, 0], 'scale': [.2, .2, .2]}}}
    (ASSETS / 'models/item/teleporter.json').write_text(json.dumps(item, indent=2) + '\n')


if __name__ == '__main__':
    main()
