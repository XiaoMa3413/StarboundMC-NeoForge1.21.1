"""Apply the explicit concept-edit operation in the real Blockbench application.

This is an authoring action, not a build step. Normal builds consume saved models.
Use --ids for a subset, --preview-only to capture the current native project.
"""
import argparse
import base64
import json
import subprocess
from pathlib import Path
from blockbench_bridge import evaluate

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'dev-assets/equipment'
PREVIEW = ROOT.parent / '概念图/2026-10-26_Blockbench建模_V03'
IDS = ['tungsten_sabre', 'coil_pistol', 'durasteel_glaive', 'plasma_carbine', 'starcore_blade', 'starcore_railgun']
IDS += [p + '_' + layer for p in ('pioneer', 'vanguard', 'ranger', 'starwarden') for layer in ('outer', 'inner')]


def save_images(ident, images):
    for view, data in images.items():
        path = PREVIEW / '工具预览' / f'{ident}-{view}.png'
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(base64.b64decode(data.split(',', 1)[1]))


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--ids', nargs='+', choices=IDS, default=IDS)
    parser.add_argument('--baseline', help='Explicit Git ref to read before applying this authoring pass')
    args = parser.parse_args()
    evaluate((ROOT / 'scripts/blockbench_equipment.js').read_text(encoding='utf-8'))
    receipts = []
    for ident in args.ids:
        if args.baseline:
            model = json.loads(subprocess.check_output(['git', 'show', f'{args.baseline}:dev-assets/equipment/{ident}.bbmodel'], cwd=ROOT))
        else:
            model = json.loads((SOURCE / f'{ident}.bbmodel').read_text(encoding='utf-8'))
            if model['meta']['format_version'] == '5.0':
                raise RuntimeError('Already authored. Edit the saved model directly, or explicitly select an authoring --baseline.')
        tile = 64 if ident.endswith(('_inner', '_outer')) else 16
        materials = {}
        for cube in model['elements']:
            uv = cube['faces']['north']['uv']
            materials[cube['uuid']] = int(min(uv[0], uv[2]) // tile) + int(min(uv[1], uv[3]) // tile) * 4
        # Use a previously committed baseline for another authoring pass; never accumulate edits.
        hand = ROOT / f'src/main/resources/assets/starboundmc/models/item/{ident}_hand.json'
        display = json.loads(hand.read_text(encoding='utf-8'))['display'] if hand.exists() else {}
        result = evaluate('SBEquipment(' + json.dumps({'id': ident, 'model': model, 'display': display, 'materials': materials}) + ')')
        raw_path = ROOT / 'logs' / f'blockbench-{ident}.json'
        raw_path.write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
        (SOURCE / f'{ident}.bbmodel').write_text(json.dumps(result['project'], ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
        save_images(ident, evaluate('SBPreview()'))
        # Reopen the actual native project data and verify a complete save/load round trip.
        reopened = evaluate('(()=>{const data=' + json.dumps(result['project']) + ';Codecs.project.load(data,{path:'
                             + json.dumps(ident + '.bbmodel') + ',no_file:true});Project.saved=true;'
                             + 'return {cubes:Cube.all.length,textures:Texture.all.length,version:Project.java_block_version}})()')
        if reopened['cubes'] != result['cubes'] or reopened['textures'] != 1:
            raise RuntimeError(f'Native project round trip failed: {ident}')
        receipts.append({'id': ident, 'tool': 'Blockbench', 'version': result['version'], 'cubes': result['cubes'],
                         'operations': result['operations'], 'reopened': reopened})
        print(json.dumps(receipts[-1], ensure_ascii=True), flush=True)
        evaluate('(async()=>{for(const p of ModelProject.all.slice()){p.saved=true;await p.close(true)}return true})()')
    receipt = PREVIEW / ('建模回执-' + args.ids[0] + '.json')
    receipt.parent.mkdir(parents=True, exist_ok=True)
    receipt.write_text(json.dumps(receipts, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
