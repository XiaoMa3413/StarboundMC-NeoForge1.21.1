"""Export accepted Blockbench triangle caches to NeoForge resources (no art rebuilding)."""
import base64
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'dev-assets/concept-rebuild-v04'
CACHE = ROOT / 'dev-assets/runtime-v04'
ASSETS = ROOT / 'src/main/resources/assets/starboundmc'
BONES = ('head', 'hat', 'body', 'right_arm', 'left_arm', 'right_leg', 'left_leg')
WEAPONS = {
 'tungsten_sabre': ([35,225,879,377], [140,300]),
 'coil_pistol': ([65,134,782,499], [180,340]),
 'durasteel_glaive': ([39,168,929,315], [260,244]),
 'plasma_carbine': ([37,173,769,447], [300,340]),
 'starcore_blade': ([33,165,877,370], [165,264]),
 'starcore_railgun': ([37,135,858,331], [225,280]),
}
# Concept-space landmarks, independently fitted to the vanilla humanoid rig.
ARMOR = {
 'pioneer': ([176,133,515,727],345,267,485,280,518,[(225,213),(455,477)],[(305,295),(383,395)]),
 'vanguard': ([136,117,514,694],325,245,478,230,490,[(195,180),(450,470)],[(270,243),(375,393)]),
 'ranger': ([115,109,472,724],307,243,480,248,481,[(190,145),(410,440)],[(260,240),(350,376)]),
 'starwarden': ([166,89,502,685],335,218,445,214,458,[(215,197),(450,472)],[(295,290),(375,390)]),
}

def write(path, data):
 path.parent.mkdir(parents=True, exist_ok=True)
 path.write_text(json.dumps(data, ensure_ascii=False, separators=(',',':'))+'\n', encoding='utf-8')

def load(name):
 data = json.loads((CACHE/f'{name}.json').read_text(encoding='utf-8'))
 raw = (SOURCE/f'{name}.bbmodel').read_bytes()
 if hashlib.sha256(raw).hexdigest() != data['source_sha256']:
  raise ValueError(f'{name}: native source changed; re-extract in Blockbench before export')
 model = json.loads(raw)
 return data, base64.b64decode(model['textures'][0]['source'].split(',',1)[1])

def export_weapon(name):
 data, png = load(name)
 bounds, grip = WEAPONS[name]
 s = 32/max(bounds[2]-bounds[0], bounds[3]-bounds[1])
 ax=((bounds[0]+bounds[2])/2-grip[0])*s; ay=(bounds[3]-grip[1])*s
 gun = name in ('coil_pistol','plasma_carbine','starcore_railgun')
 lines = [f'# Accepted V04: {data["source_sha256"]}',f'mtllib {name}.mtl','usemtl equipment']
 n=1
 for part in data['parts']:
  for tri in part['triangles']:
   for x,y,z,u,v in tri:
    x,y,z = (-x+ax+6,y-ay+4,-z+8) if gun else (y-ay+8,-x+ax+2,z+8)
    lines.append('v %.7f %.7f %.7f'%(x/16,y/16,z/16))
    lines.append('vt %.7f %.7f'%(u,v))
   lines.append('f '+' '.join(f'{i}/{i}' for i in range(n,n+3))); n+=3
 folder = ASSETS/'models/item'
 (folder/f'{name}.obj').write_text('\n'.join(lines)+'\n',encoding='utf-8')
 (folder/f'{name}.mtl').write_text(f'newmtl equipment\nKa 0 0 0\nKd 1 1 1\nd 1\nmap_Kd starboundmc:item/{name}_model\n',encoding='utf-8')
 hand = folder/f'{name}_hand.json'
 display=json.loads(hand.read_text(encoding='utf-8'))['display']
 write(hand,{'loader':'neoforge:obj','model':f'starboundmc:models/item/{name}.obj',
  'automatic_culling':False,'shade_quads':True,'flip_v':False,'emissive_ambient':False,
  'textures':{'particle':f'starboundmc:item/{name}_model'},'display':display})
 (ASSETS/f'textures/item/{name}_model.png').write_bytes(png)

def lining_box(target, low, high):
 # Skin outer layers are larger than the concept mannequin. A fitted dark
 # undersuit prevents exposed skin at articulation seams, without changing source art.
 vertices=[(x,y,z) for x in (low[0],high[0]) for y in (low[1],high[1]) for z in (low[2],high[2])]
 for face in ((0,1,3,2),(4,6,7,5),(0,4,5,1),(2,3,7,6),(0,2,6,4),(1,5,7,3)):
  for ids in ((face[0],face[1],face[2]),(face[0],face[2],face[3])):
   target.append([[*vertices[i],96/1536,1056/1088] for i in ids])

def export_armor():
 output={}
 for name,(b,cx,neck,hip,shoulder,wrist,arms,legs) in ARMOR.items():
  data,png=load(name); s=32/max(b[2]-b[0],b[3]-b[1]); middle=(b[0]+b[2])/2
  slots={slot:{bone:[] for bone in BONES} for slot in ('head','chest','legs','feet')}
  for part in data['parts']:
   group=part['group']
   if name=='starwarden' and group=='head': continue
   vertices=[v for t in part['triangles'] for v in t]
   px=middle-sum(v[0] for v in vertices)/len(vertices)/s
   side=0 if px<cx else 1
   if group=='head': slot,bone='head','head'
   elif group in ('body','backplate','body_liner'):
    slot,bone=('legs' if '骨盆' in part['name'] else 'chest'),'body'
   elif group in ('belt','tassets'): slot,bone='legs','body'
   elif group in ('rightarm','leftarm','shoulders','bracers'):
    slot,bone='chest',('right_arm' if side==0 else 'left_arm')
   elif group in ('rightleg','leftleg','thighs','knees','shins','boots'):
    slot,bone=('feet' if group=='boots' else 'legs'),('right_leg' if side==0 else 'left_leg')
   else: raise ValueError(f'Unbound armor group: {name}/{group}')
   for tri in part['triangles']:
    face=[]
    for x,y,z,u,v in tri:
     px,py,d=middle-x/s,b[3]-y/s,z/s
     if bone=='head':
      width={'pioneer':1.2,'vanguard':1.35,'ranger':1.3}[name]
      q=((px-cx)*9/175*width,((py-neck)*8.8/(neck-b[1])+4)*1.08-4,d*9/145*1.1)
     elif bone=='body':
      yy=(py-neck)*12/(hip-neck) if py<=hip else 12+(py-hip)*12/(b[3]-hip)
      q=((px-cx)*8.8/155,yy,d*5.2/92)
     elif 'arm' in bone:
      t=(py-shoulder)/(wrist-shoulder); a,e=arms[side]
      q=((px-(a+(e-a)*t))*6.35/70+(-1 if side==0 else 1),t*12-2,d*6.35/65)
     else:
      t=(py-hip)/(b[3]-hip); a,e=legs[side]
      q=((px-(a+(e-a)*t))*6.0/75,t*12,d*6.1/70)
     face.append([round(a,7) for a in (*q,u,v)])
    slots[slot][bone].append(face)
  if name=='starwarden':
   # This mesh was remodeled at its final head-bone dimensions in Blockbench.
   fitted=ROOT/'dev-assets/equipment-fit/starwarden_helmet.bbmodel'
   mesh=json.loads(fitted.with_suffix('.json').read_text(encoding='utf-8'))
   if hashlib.sha256(fitted.read_bytes()).hexdigest()!=mesh['source_sha256']:
    raise ValueError('Starwarden fitted helmet source changed; re-extract before export')
   fitted_model=json.loads(fitted.read_text(encoding='utf-8'))
   png=base64.b64decode(fitted_model['textures'][0]['source'].split(',',1)[1])
   for part in mesh['parts']: slots['head']['head'].extend(part['triangles'])
  else:
   lining_box(slots['head']['head'],(-4.55,-8.55,-4.55),(4.55,.15,4.55))
  lining_box(slots['chest']['body'],(-4.3,-.3,-2.3),(4.3,12.25,2.3))
  for side in ('right','left'):
   center=-1 if side=='right' else 1
   lining_box(slots['chest'][side+'_arm'],(center-2.3,-2.3,-2.3),(center+2.3,10.3,2.3))
   lining_box(slots['legs'][side+'_leg'],(-2.3,-.15,-2.3),(2.3,12.25,2.3))
   lining_box(slots['feet'][side+'_leg'],(-2.35,8.7,-2.35),(2.35,12.3,2.35))
  output[name]=slots
  for layer in (1,2): (ASSETS/f'textures/models/armor/{name}_layer_{layer}.png').write_bytes(png)
 write(ASSETS/'equipment/armor_meshes.json',output)

if __name__=='__main__':
 for name in WEAPONS: export_weapon(name)
 export_armor()
 print('Exported 6 OBJ weapons and 16 armor slot meshes from accepted native sources.')
