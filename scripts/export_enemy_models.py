"""Export bone-local runtime geometry and textures from reopened native enemy projects."""
import base64,hashlib,json,math
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/'dev-assets/enemies/models'
CACHE=ROOT/'dev-assets/enemies/geometry'
ASSETS=ROOT/'src/main/resources/assets/starboundmc'

def export():
 rows=[]
 for path in sorted(CACHE.glob('*.json')):
  raw=json.loads(path.read_text(encoding='utf-8'));ident=raw['id'];source=SOURCE/(ident+'.bbmodel')
  digest=hashlib.sha256(source.read_bytes()).hexdigest()
  if digest!=raw['source_sha256']:raise ValueError(f'{ident}: native model changed; re-extract before export')
  names={b['name']:b for b in raw['bones']}
  if len(names)!=len(raw['bones']):raise ValueError('Duplicate enemy bone name')
  roots=[b for b in names.values() if b['parent'] is None]
  if len(roots)!=1 or roots[0]['name']!='root':raise ValueError('Enemy requires one root bone')
  bones=[]
  for bone in names.values():
   pivot=bone['pivot'];parent=names[bone['parent']]['pivot'] if bone['parent'] else [0,0,0]
   offset=[-(pivot[0]-parent[0]),-(pivot[1]-parent[1]),pivot[2]-parent[2]]
   if bone['parent'] is None:offset[1]+=24
   parts=[]
   for part in raw['parts']:
    if part['bone']!=bone['name']:continue
    faces=[]
    for tri in part['triangles']:
     face=[]
     for x,y,z,u,v in tri:
      vertex=[-x+pivot[0],pivot[1]-y,z-pivot[2],u,v]
      if not all(math.isfinite(a) for a in vertex):raise ValueError('Non-finite enemy vertex')
      face.append([round(a,7) for a in vertex])
     faces.append(face)
    parts.append(dict(name=part['name'],emissive=part['emissive'],triangles=faces))
   bones.append(dict(name=bone['name'],parent=bone['parent'],offset=offset,parts=parts))
  output=ASSETS/f'enemies/{ident}.json';output.parent.mkdir(exist_ok=True)
  output.write_text(json.dumps({'bones':bones},ensure_ascii=False,separators=(',',':'))+'\n',encoding='utf-8')
  native=json.loads(source.read_text(encoding='utf-8'));png=base64.b64decode(native['textures'][0]['source'].split(',',1)[1])
  target=ASSETS/f'textures/entity/enemies/{ident}.png';target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(png)
  rows.append({'id':ident,'source_sha256':digest,'runtime_sha256':hashlib.sha256(output.read_bytes()).hexdigest(),'texture_sha256':hashlib.sha256(png).hexdigest(),'bones':len(bones),'parts':len(raw['parts']),'triangles':sum(len(p['triangles']) for p in raw['parts'])})
 (ROOT/'dev-assets/enemies/export-validation.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
 return rows
if __name__=='__main__':print(json.dumps(export(),ensure_ascii=False,indent=2))
