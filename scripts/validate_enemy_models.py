"""Validate actual reopened Blockbench meshes: closed topology, winding, UV and bones."""
import json,math
from pathlib import Path
from collections import defaultdict
ROOT=Path(__file__).resolve().parents[1]

def validate():
 rows=[]
 for path in sorted((ROOT/'dev-assets/enemies/geometry').glob('*.json')):
  model=json.loads(path.read_text(encoding='utf-8'));issues=[];triangles=0
  for part in model['parts']:
   edges=defaultdict(list)
   for tri in part['triangles']:
    triangles+=1;pts=[tuple(round(x,5) for x in v[:3]) for v in tri]
    if len(set(pts))<3:issues.append((part['name'],'degenerate'));continue
    assert all(math.isfinite(x) for v in tri for x in v)
    assert all(0<=v[3]<=1 and 0<=v[4]<=1 for v in tri),(model['id'],part['name'],'UV outside atlas')
    for a,b in zip(pts,pts[1:]+pts[:1]):edges[tuple(sorted((a,b)))].append((a,b))
   bad=[(e,d) for e,d in edges.items() if len(d)!=2 or d[0]==d[1]]
   if bad:issues.append((part['name'],len(bad)))
  rows.append({'id':model['id'],'parts':len(model['parts']),'bones':len(model['bones']),'triangles':triangles,'issues':issues})
 return rows
if __name__=='__main__':
 rows=validate();print(json.dumps(rows,ensure_ascii=False,indent=2));assert all(not r['issues'] for r in rows),'Native geometry topology requires correction'
