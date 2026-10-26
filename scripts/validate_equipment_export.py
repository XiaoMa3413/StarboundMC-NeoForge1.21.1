"""Validate source identity and lossless UV/topology transfer into shipped resources."""
import hashlib
import json
import math
from collections import Counter
from equipment_mesh_export import ROOT, SOURCE, CACHE, ASSETS, WEAPONS, ARMOR

def uv_key(triangle):
 return tuple(tuple(round(v,7) for v in vertex[-2:]) for vertex in triangle)

def validate():
 result={}
 for name in (*WEAPONS,*ARMOR):
  source=json.loads((CACHE/f'{name}.json').read_text(encoding='utf-8'))
  assert hashlib.sha256((SOURCE/f'{name}.bbmodel').read_bytes()).hexdigest()==source['source_sha256'], name
  triangles=[t for p in source['parts'] for t in p['triangles']]
  if name in WEAPONS:
   lines=(ASSETS/f'models/item/{name}.obj').read_text(encoding='utf-8').splitlines()
   uv=[tuple(map(float,l.split()[1:])) for l in lines if l.startswith('vt ')]
   assert len(uv)==len(triangles)*3, name
   assert uv==[tuple(v[-2:]) for t in triangles for v in t], name
   assert sum(l.startswith('f ') for l in lines)==len(triangles), name
  else:
   geometry=json.loads((ASSETS/'equipment/armor_meshes.json').read_text(encoding='utf-8'))[name]
   allowed={'head':{'head'},'chest':{'body','right_arm','left_arm'},'legs':{'body','right_leg','left_leg'},'feet':{'right_leg','left_leg'}}
   exported=[]
   for slot,parts in geometry.items():
    assert any(parts.values()), (name,slot)
    for bone,faces in parts.items():
     assert not faces or bone in allowed[slot], (name,slot,bone)
     assert all(math.isfinite(x) for t in faces for v in t for x in v)
     exported.extend(faces)
   original=Counter(map(uv_key,triangles)); actual=Counter(map(uv_key,exported))
   assert not original-actual, name
   assert len(exported)==len(triangles)+96, name
  result[name]={'source_sha256':source['source_sha256'],'native_triangles':len(triangles),'uv_topology_preserved':True}
 return result

if __name__=='__main__':
 print(json.dumps(validate(),ensure_ascii=False,indent=2))
