"""Read actual native Blockbench render geometry from accepted, immutable projects."""
import hashlib
import json
from pathlib import Path
from blockbench_bridge import evaluate

ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/'dev-assets/concept-rebuild-v04'
CACHE=ROOT/'dev-assets/runtime-v04'
EXTRACT=r'''async function(data){
 Codecs.project.load(data,{path:data.name+'.bbmodel',no_file:true});
 await new Promise(r=>setTimeout(r,150));Canvas.updateAll();scene.updateMatrixWorld(true);
 const parts=[];
 for(const e of Outliner.elements){
  if(!e.mesh||!e.mesh.geometry)continue;
  const g=e.mesh.geometry,p=g.getAttribute('position'),uv=g.getAttribute('uv'),index=g.getIndex(),triangles=[];
  for(let i=0;i<(index?index.count:p.count);i+=3){const face=[];
   for(let k=0;k<3;k++){const n=index?index.getX(i+k):i+k;
    const v=new THREE.Vector3(p.getX(n),p.getY(n),p.getZ(n)).applyMatrix4(e.mesh.matrixWorld);
    face.push([v.x,v.y,v.z,uv.getX(n),1-uv.getY(n)].map(x=>Math.round(x*1e7)/1e7));
   }triangles.push(face);
  }
  parts.push({name:e.name,group:e.parent.name,triangles});
 }
 Project.saved=true;await Project.close(true);return {version:Blockbench.version,parts};
}'''

if __name__=='__main__':
    CACHE.mkdir(exist_ok=True)
    accepted=json.loads((SOURCE/'delivery.json').read_text(encoding='utf-8'))
    assert accepted['accepted'], 'Art must be accepted before production export'
    validations=json.loads((SOURCE/'validation.json').read_text(encoding='utf-8'))
    for row in validations:
        ident=row['id'];raw=(SOURCE/(ident+'.bbmodel')).read_bytes()
        assert hashlib.sha256(raw).hexdigest()==row['sha256'],ident+' changed since acceptance'
        data=json.loads(raw);data['name']=ident
        result=evaluate('('+EXTRACT+')('+json.dumps(data)+')')
        result.update(id=ident,source_sha256=row['sha256'])
        (CACHE/(ident+'.json')).write_text(json.dumps(result,ensure_ascii=False,separators=(',',':'))+'\n',encoding='utf-8')
        print(ident,len(result['parts']),sum(len(p['triangles']) for p in result['parts']),flush=True)
