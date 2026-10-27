"""Check that the actual closed helmet shell contains the player's outer head layer."""
import itertools,json
from pathlib import Path
from equipment_mesh_export import ROOT

def sub(a,b):return [x-y for x,y in zip(a,b)]
def dot(a,b):return sum(x*y for x,y in zip(a,b))
def cross(a,b):return [a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]]
def hit(point,triangle):
 a,b,c=[v[:3] for v in triangle];direction=[.853,.244,.461]
 edge1,edge2=sub(b,a),sub(c,a);h=cross(direction,edge2);det=dot(edge1,h)
 if abs(det)<1e-10:return False
 inv=1/det;s=sub(point,a);u=inv*dot(s,h)
 if u<0 or u>1:return False
 q=cross(s,edge1);v=inv*dot(direction,q)
 if v<0 or u+v>1:return False
 return inv*dot(edge2,q)>1e-9

def validate():
 mesh=json.loads((ROOT/'dev-assets/equipment-fit/starwarden_helmet.json').read_text(encoding='utf-8'))
 shell=next(p for p in mesh['parts'] if p['name']=='守望头盔')['triangles']
 points=list(itertools.product([-4.5,-2.25,0,2.25,4.5],[-8.5,-6.25,-4,-1.75,.5],[-4.5,-2.25,0,2.25,4.5]))
 failed=[p for p in points if sum(hit(p,t) for t in shell)%2!=1]
 assert not failed, f'Head outer layer outside shell: {failed}'
 return {'head_outer_layer_samples':len(points),'outside_shell':0,'runtime_scale':[1,1,1]}

if __name__=='__main__':print(json.dumps(validate(),indent=2))
