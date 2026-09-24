"""Package the MCP-authored printer as a chassis, pivot-local head and complete item.

Export project and OBJ from Blockbench before running. No geometry is generated here.
"""
import base64
import json
import math
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
AUTHORING = ROOT / 'docs/models'
ASSETS = ROOT / 'src/main/resources/assets/starboundmc'
MODELS = ASSETS / 'models/block'


def main():
    project = json.loads((AUTHORING / 'voxel-printer-textured-v1.bbmodel').read_text())
    elements = {e['uuid']: e for e in project['elements']}
    properties = {g['uuid']: g for g in project.get('groups', [])}
    groups = {(g.get('name') or properties[g['uuid']]['name']):
              {**properties.get(g['uuid'], {}), **g} for g in project['outliner']}
    left = groups['07_probe_left']
    right = groups['08_probe_right']
    assert left['origin'] == [5.76, 12.8, 8]
    assert right['origin'] == [10.24, 12.8, 8]
    left_names = {elements[i]['name'] for i in left['children']}
    head_names = left_names | {elements[i]['name'] for i in right['children']}
    materials = {'m_' + t['uuid']: t for t in project['textures']}
    vertices, uvs, normals, objects = [], [], [], []
    current = None
    referenced_materials = set()
    material = None
    for line in (AUTHORING / 'voxel-printer-export.obj').read_text().splitlines():
        if line.startswith('o '):
            current = {'name': line[2:], 'faces': []}
            objects.append(current)
        elif line.startswith(('v ', 'vt ', 'vn ')):
            key, *values = line.split()
            vector = tuple(float(v) for v in values)
            assert all(math.isfinite(v) for v in vector), line
            if key in ('v', 'vt'):
                assert all(-1e-6 <= v <= 1.000001 for v in vector), line
            {'v': vertices, 'vt': uvs, 'vn': normals}[key].append(vector)
        elif line.startswith('usemtl '):
            material_id = line.split()[1]
            referenced_materials.add(material_id)
            material = materials[material_id]['name']
        elif line.startswith('f '):
            assert current is not None and material is not None
            face = [tuple(int(n) - 1 for n in token.split('/')) for token in line.split()[1:]]
            assert len(face) in (3, 4) and all(len(v) == 3 for v in face)
            current['faces'].append((material, face))
    assert len(objects) == len(elements) and left_names and head_names
    used = {mat for o in objects for mat, _ in o['faces']}
    assert used == {'printer_surface_atlas', 'printer_optics'}, used
    for name in sorted(used):
        # Only extract the referenced texture; unused clay/backup textures may share names.
        texture = next(materials[key] for key in referenced_materials if materials[key]['name'] == name)
        assert texture['source'].startswith('data:image/png;base64,')
        (ASSETS / f'textures/block/{name}.png').write_bytes(base64.b64decode(texture['source'].split(',', 1)[1]))

    def write_obj(stem, selected, pivot=(0, 0, 0)):
        selected = list(selected)
        ids = [sorted({v[k] for o in selected for _, face in o['faces'] for v in face}) for k in range(3)]
        maps = [{old: new + 1 for new, old in enumerate(indices)} for indices in ids]
        lines = ['# Blockbench MCP export; packed by tools/export_voxel_printer.py', 'mtllib voxel_printer.mtl']
        for k, (kind, values) in enumerate([('v', vertices), ('vt', uvs), ('vn', normals)]):
            for index in ids[k]:
                vector = values[index]
                if k == 0:
                    vector = tuple(v - pivot[a] for a, v in enumerate(vector))
                lines.append(kind + ' ' + ' '.join(f'{v:.9g}' for v in vector))
        last_material = None
        for number, obj in enumerate(selected):
            slug = re.sub('[^a-z0-9]+', '_', obj['name'].lower()).strip('_')
            lines.append(f'o {slug}_{number}')
            for mat, face in obj['faces']:
                if mat != last_material:
                    lines.append('usemtl ' + mat)
                    last_material = mat
                lines.append('f ' + ' '.join('/'.join(str(maps[k][v[k]]) for k in range(3)) for v in face))
        (MODELS / (stem + '.obj')).write_text('\n'.join(lines) + '\n')
        print(stem, len(selected), 'objects,', sum(len(o['faces']) for o in selected), 'faces')

    write_obj('voxel_printer_chassis', (o for o in objects if o['name'] not in head_names))
    write_obj('voxel_printer_head', (o for o in objects if o['name'] in left_names), tuple(v / 16 for v in left['origin']))
    write_obj('voxel_printer_complete', objects)
    mtl = '# Ka marks only optical surfaces as emissive; no block light is emitted.\n'
    for name in sorted(used):
        ambient = '1 1 1' if name == 'printer_optics' else '0 0 0'
        mtl += f'\nnewmtl {name}\nKa {ambient}\nKd 1 1 1\nmap_Kd starboundmc:block/{name}\n'
    (MODELS / 'voxel_printer.mtl').write_text(mtl)
    for model, mesh in [('voxel_printing_station', 'voxel_printer_chassis'),
                        ('voxel_printer_head', 'voxel_printer_head'),
                        ('voxel_printer_complete', 'voxel_printer_complete')]:
        data = {'parent': 'minecraft:block/block', 'loader': 'neoforge:obj',
                'model': f'starboundmc:models/block/{mesh}.obj', 'automatic_culling': False,
                'shade_quads': True, 'flip_v': True, 'emissive_ambient': True,
                'render_type': 'minecraft:solid', 'ambientocclusion': False,
                'textures': {'particle': 'starboundmc:block/voxel_machine_casing'}}
        (MODELS / (model + '.json')).write_text(json.dumps(data, indent=2) + '\n')


if __name__ == '__main__':
    main()
