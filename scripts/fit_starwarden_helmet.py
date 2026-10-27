"""Remodel the starwarden helmet shell in Blockbench at final humanoid bone size."""
import base64
import hashlib
import json
import sys
from pathlib import Path
from blockbench_bridge import evaluate
from extract_v04_meshes import EXTRACT

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'dev-assets/equipment-fit'
GALLERY=ROOT.parent/'概念图/2026-10-27_守望头盔壳体修正'
X=[(265,-5),(275,-4.9),(280,-4.75),(317,-1.7),(335,0),(351,1.5),(393,4.75),(397,4.9),(407,5)]
Y=[(89,-8.95),(99,-8.8),(121,-7),(145,-5.5),(154,-4.6),(177,-2.8),(192,-1.5),(209,-.2),(225,.7)]

def interpolate(value, knots):
 i=next((i for i in range(len(knots)-1) if value<=knots[i+1][0]),len(knots)-2)
 a,b=knots[i:i+2]
 return a[1]+(value-a[0])*(b[1]-a[1])/(b[0]-a[0])

CREATE=r'''async function(data) {
 await newProject(Formats.free);
 Project.name='starwarden_helmet_shell_fit';Project.texture_width=1536;Project.texture_height=1088;Project.box_uv=false;
 const tex=new Texture({name:'starwarden_approved_projection.png',id:'0',uv_width:1536,uv_height:1088});
 tex.fromDataURL(data.texture).add(false);
 await new Promise(r=>setTimeout(r,150));
 const group=new Group({name:'head',origin:[0,24,0]}).init();
 for(const part of data.parts){
  const mesh=new Mesh({name:part.name,vertices:{}}),vertices=new Map();
  for(const tri of part.triangles){
   const keys=tri.map(v=>{const p=[-v[0],24-v[1],v[2]],key=JSON.stringify(p);
    if(!vertices.has(key)) vertices.set(key,mesh.addVertices(p)[0]);return vertices.get(key)});
   mesh.addFaces(new MeshFace(mesh,{vertices:keys,uv:Object.fromEntries(keys.map((k,i)=>[k,[tri[i][3]*1536,tri[i][4]*1088]])),texture:tex.uuid}));
  }
  mesh.addTo(group).init();
 }
 Canvas.updateAll();scene.updateMatrixWorld(true);
 return Codecs.project.compile({raw:true,bitmaps:true,absolute_paths:false});
}'''

if __name__=='__main__':
 OUT.mkdir(exist_ok=True);GALLERY.mkdir(exist_ok=True)
 original=json.loads((ROOT/'dev-assets/runtime-v04/starwarden.json').read_text(encoding='utf-8'))
 approved=json.loads((ROOT/'dev-assets/concept-rebuild-v04/starwarden.bbmodel').read_text(encoding='utf-8'))
 s=32/596;middle=334
 parts=[]
 for part in original['parts']:
  if part['group']!='head':continue
  faces=[]
  for tri in part['triangles']:
   face=[]
   for x,y,z,u,v in tri:
    px,py,depth=middle-x/s,685-y/s,z/s
    # Piecewise shell contour: keep a shallow crown and provide temple/jaw clearance.
    gx,gy=interpolate(px,X),interpolate(py,Y)
    gz=interpolate(depth,[(-81,-5.35),(-66.96,-4.8),(66.96,4.8),(81,5.35)])
    face.append([round(a,7) for a in (gx,gy,gz,u,v)])
   faces.append(face)
  parts.append({'name':part['name'],'group':'head','triangles':faces})
 path=OUT/'starwarden_helmet.bbmodel'
 if '--extract-only' not in sys.argv:
  model=evaluate('('+CREATE+')('+json.dumps({'texture':approved['textures'][0]['source'],'parts':parts})+')')
  path.write_text(json.dumps(model,ensure_ascii=False,separators=(',',':'))+'\n',encoding='utf-8')
 # Reopen the saved native project and extract actual rendered geometry, not the input array.
 data=json.loads(path.read_text(encoding='utf-8'));data['name']='starwarden_helmet'
 native=evaluate('('+EXTRACT+')('+json.dumps(data)+')')
 for part in native['parts']:
  for tri in part['triangles']:
   for v in tri:v[0]=-v[0];v[1]=round(24-v[1],7)
 native.update(id='starwarden_helmet',coordinate_space='humanoid_head_pixels',source_sha256=hashlib.sha256(path.read_bytes()).hexdigest(),approved_base_sha256=original['source_sha256'])
 (OUT/'starwarden_helmet.json').write_text(json.dumps(native,ensure_ascii=False,separators=(',',':'))+'\n',encoding='utf-8')
 # Record dimensions separately from the earlier oversized production version.
 vs=[v for p in native['parts'] for t in p['triangles'] for v in t]
 receipt={'tool':'Blockbench 5.2.2','parts':len(parts),'triangles':sum(len(p['triangles']) for p in native['parts']),
  'min':[min(v[i] for v in vs) for i in range(3)],'max':[max(v[i] for v in vs) for i in range(3)],
  'x_contour_knots':X,'y_contour_knots':Y,'runtime_scale':[1,1,1],'source_sha256':native['source_sha256']}
 (OUT/'starwarden_helmet_receipt.json').write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
 # Save orthographic screenshots from the native editor too.
 evaluate('Codecs.project.load('+json.dumps(data)+',{path:'+json.dumps(str(path))+',no_file:true});true')
 views=(ROOT/'scripts/concept_rebuild_native.js').read_text(encoding='utf-8')
 evaluate(views[views.index('window.V04View ='):])
 for view in ('front','side','back','perspective'):
  shot=evaluate('V04View('+json.dumps(view)+')')
  (GALLERY/f'blockbench-{view}.png').write_bytes(base64.b64decode(shot['png'].split(',',1)[1]))
 print(json.dumps(receipt,ensure_ascii=True))
