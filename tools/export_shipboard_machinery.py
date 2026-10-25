"""Package real Blockbench MCP exports; geometry and UVs are never synthesized here."""
import base64
import hashlib
import io
import json
import math
import re
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ART = ROOT / 'docs/models'
ASSETS = ROOT / 'src/main/resources/assets/starboundmc'
KINDS = ['ship_light', 'voxel_refinery', 'titanium_alloy_furnace',
         'matter_manipulator_workbench', 'life_support_station', 'epp_service_station',
         'ship_console', 'ship_door', 'ship_door_open', 'ship_engine_unit', 'beacon_emitter']


def write_json(path, value):
    path.write_text(json.dumps(value, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')


def package(kind, shared):
    project = json.loads((ART / f'{kind}.bbmodel').read_text(encoding='utf-8'))
    textures = {'m_' + t['uuid']: t for t in project['textures']}
    vertices, uv, normals, faces, lines, used, objects = [], [], [], [], [], {}, []
    source = ART / f'{kind}.obj'
    for line in source.read_text(encoding='utf-8').splitlines():
        if line.startswith('mtllib '):
            line = f'mtllib {kind}.mtl'
        elif line.startswith('usemtl '):
            texture = textures[line.split()[1]]
            name = texture['name']
            assert name in ('shipboard_surfaces', 'shipboard_signals', 'ship_engine_unit_core')
            used[name] = texture
            line = 'usemtl ' + name
        elif line.startswith('o '):
            objects.append(line[2:])
            line = 'o ' + re.sub(r'[^a-z0-9]+', '_', line[2:].lower()).strip('_')
        elif line.startswith(('v ', 'vt ', 'vn ')):
            tag, *values = line.split()
            point = tuple(map(float, values))
            assert all(math.isfinite(v) for v in point), line
            if tag == 'v':
                assert all(-1e-6 <= v <= 1.000001 for v in point), (kind, line)
                vertices.append(point)
            elif tag == 'vt':
                assert all(-1e-6 <= v <= 1.000001 for v in point), (kind, line)
                uv.append(point)
            else:
                normals.append(point)
        elif line.startswith('f '):
            face = [tuple(map(int, c.split('/'))) for c in line.split()[1:]]
            assert len(face) in (3, 4), line
            faces.append(face)
        lines.append(line)
    expected = [e for e in project['elements'] if e.get('export', True)]
    assert len(objects) == len(expected), (kind, len(objects), len(expected))
    # A coordinate multiset check catches incomplete or substituted exports.
    expected_positions = sorted(tuple(round(v / 16, 6) for v in p)
                                for e in expected for p in e['vertices'].values())
    actual_positions = sorted(tuple(round(v, 6) for v in p) for p in vertices)
    assert len(actual_positions) == len(expected_positions), kind
    for point in expected_positions:
        match = next((i for i, candidate in enumerate(actual_positions)
                      if all(abs(a-b) <= 1.1e-6 for a, b in zip(point, candidate))), None)
        assert match is not None, (kind, 'source vertex missing', point)
        actual_positions.pop(match)
    for face in faces:
        for vi, ti, ni in face:
            assert 1 <= vi <= len(vertices) and 1 <= ti <= len(uv) and 1 <= ni <= len(normals)
        p = [vertices[v - 1] for v, _, _ in face]
        a, b = [[p[i][j] - p[0][j] for j in range(3)] for i in (1, 2)]
        cross = [a[1]*b[2]-a[2]*b[1], a[2]*b[0]-a[0]*b[2], a[0]*b[1]-a[1]*b[0]]
        assert sum(v*v for v in cross) > 1e-16, (kind, 'degenerate face')
    for name, texture in used.items():
        data = base64.b64decode(texture['source'].split(',', 1)[1])
        bitmap = Image.open(io.BytesIO(data)).convert('RGBA')
        assert bitmap.getchannel('A').getextrema() == (255, 255), name
        if name in shared:
            assert shared[name] == data, 'Shared atlas differs between projects: ' + name
        shared[name] = data
        destination = ASSETS / f'textures/block/{name}.png'
        if name == 'ship_engine_unit_core':
            assert destination.read_bytes() == data, 'Preserve original core artwork'
        else:
            destination.write_bytes(data)
    triangles = sum(len(face) - 2 for face in faces)
    assert triangles <= (400 if kind == 'ship_light' else 1200), kind
    models = ASSETS / 'models/block'
    (models / f'{kind}.obj').write_text('\n'.join(lines) + '\n', encoding='utf-8')
    mtl = '# Blockbench materials; small signal surfaces use ambient emissivity.\n'
    for name in sorted(used):
        ambient = '0 0 0' if name == 'shipboard_surfaces' else '1 1 1'
        mtl += f'\nnewmtl {name}\nKa {ambient}\nKd 1 1 1\nmap_Kd starboundmc:block/{name}\n'
    (models / f'{kind}.mtl').write_text(mtl, encoding='utf-8')
    write_json(models / f'{kind}.json', {
        'parent': 'minecraft:block/block', 'loader': 'neoforge:obj',
        'model': f'starboundmc:models/block/{kind}.obj', 'automatic_culling': False,
        'shade_quads': True, 'flip_v': True, 'emissive_ambient': True,
        'render_type': 'minecraft:solid', 'ambientocclusion': False,
        'textures': {'particle': 'starboundmc:block/shipboard_surfaces'}})
    if kind != 'ship_door_open':
        write_json(ASSETS / f'models/item/{kind}.json', {
            'parent': f'starboundmc:block/{kind}', 'display': {
                'gui': {'rotation': [24, 145, 0], 'translation': [0, 0, 0], 'scale': [.7, .7, .7]},
                'ground': {'translation': [0, 3, 0], 'scale': [.35, .35, .35]},
                'fixed': {'rotation': [0, 180, 0], 'scale': [.65, .65, .65]},
                'firstperson_righthand': {'rotation': [0, 135, 0], 'scale': [.4, .4, .4]},
                'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [.35, .35, .35]}}})
    return {'meshes': len(objects), 'faces': len(faces), 'triangles': triangles,
            'materials': sorted(used), 'source_obj_sha256': hashlib.sha256(source.read_bytes()).hexdigest()}


if __name__ == '__main__':
    shared = {}
    manifest = {kind: package(kind, shared) for kind in KINDS}
    write_json(ART / 'shipboard-machinery-manifest.json', manifest)
    print(json.dumps(manifest, indent=2))
