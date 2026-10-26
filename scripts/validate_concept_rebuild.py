"""Validate delivered native model structure, UVs, mesh closure, and evidence."""
from collections import Counter
import hashlib
import json
import math
from concept_rebuild_specs import JOBS, MODELS, OUT

results=[]
for job in JOBS:
    ident=job['id']; path=MODELS/(ident+'.bbmodel')
    raw=path.read_bytes(); model=json.loads(raw)
    assert model['meta']['model_format']=='free', (ident,model['meta'])
    assert len(model['textures'])==1 and model['textures'][0]['source'].startswith('data:image/png;base64,')
    cubes=meshes=0; errors=[]
    for element in model['elements']:
        if element['type']=='cube':
            cubes+=1
            assert all(math.isfinite(v) for key in ['from','to'] for v in element[key]), ident
            assert all(b>a for a,b in zip(element['from'],element['to'])), (ident,element['name'])
            uvs=[uv for face in element['faces'].values() for uv in [face['uv'][:2],face['uv'][2:]]]
        else:
            assert element['type']=='mesh', (ident,element['type']);meshes+=1
            vertices=element['vertices'];edges=Counter()
            for face in element['faces'].values():
                points=face['vertices'];assert len(points)>=3
                assert all(key in vertices for key in points)
                for a,b in zip(points,points[1:]+points[:1]):edges[tuple(sorted([a,b]))]+=1
            bad=[edge for edge,n in edges.items() if n!=2]
            if bad:errors.append({'part':element['name'],'non_manifold_edges':len(bad)})
            uvs=[uv for face in element['faces'].values() for uv in face['uv'].values()]
        assert all(0<=u<=1536 and 0<=v<=1088 for u,v in uvs), (ident,element['name'],'UV outside atlas')
    receipt=json.loads((OUT/ident/'receipt.json').read_text(encoding='utf-8'))
    assert cubes==receipt['reopened']['cubes'] and meshes==receipt['reopened']['meshes']
    digest=hashlib.sha256(raw).hexdigest();assert digest==receipt['model_sha256']
    views=['front','side','back','top','perspective']+(['reference_side','reference_back'] if job['side'] else [])
    evidence={}
    for view in views:
        for ending in ['.png','-editor.png']:
            file=OUT/ident/(view+ending);content=file.read_bytes()
            assert content.startswith(b'\x89PNG\r\n\x1a\n'),file
            evidence[file.name]=hashlib.sha256(content).hexdigest()
    results.append(dict(id=ident,cubes=cubes,meshes=meshes,closed_meshes=not errors,errors=errors,sha256=digest,evidence=evidence))
    print(ident, 'cubes',cubes,'meshes',meshes,'closure_errors',errors,flush=True)
(MODELS/'validation.json').write_text(json.dumps(results,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
assert not any(row['errors'] for row in results), 'Open mesh edges require correction before delivery'
