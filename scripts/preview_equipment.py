"""Native Blockbench assembled armor inspection copies, separate from production sources."""
import json
from blockbench_bridge import evaluate
from model_equipment import ROOT, SOURCE, PREVIEW, save_images

evaluate((ROOT / 'scripts/blockbench_equipment.js').read_text(encoding='utf-8'))
for name in ('pioneer', 'vanguard', 'ranger', 'starwarden'):
    models = {layer: json.loads((SOURCE / f'{name}_{layer}.bbmodel').read_text(encoding='utf-8')) for layer in ('outer', 'inner')}
    if models['outer']['textures'][0]['source'] != models['inner']['textures'][0]['source']:
        raise ValueError('Assembly inspection expects a shared palette; use separate textures for distinct painted layers')
    result = evaluate('SBAssembly(' + json.dumps({'id': name, **models}) + ')')
    save_images(name + '_assembled', result['images'])
    out = PREVIEW / '整套预览工程' / f'{name}_assembled.bbmodel'
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(result['project'], ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(name, 'native assembled preview saved', flush=True)
    evaluate('(async()=>{for(const p of ModelProject.all.slice()){p.saved=true;await p.close(true)}return true})()')
