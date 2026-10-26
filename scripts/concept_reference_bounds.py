"""Measure supplied armor reference silhouettes, independently of model geometry."""
import base64
import json
from pathlib import Path
from blockbench_bridge import evaluate
from concept_rebuild_specs import JOBS, MODELS

EXPR=r'''async function(job) {
 const im=new Image();im.src=job.image;await im.decode();
 const cv=document.createElement('canvas');cv.width=1536;cv.height=1024;
 const cx=cv.getContext('2d');cx.drawImage(im,0,0);const px=cx.getImageData(0,0,1536,1024).data;
 const result={};
 for(const [name,box] of Object.entries(job.regions)) {
  const [l,t,r,b]=box,w=r-l,h=b-t,seen=new Uint8Array(w*h),parts=[];
  const selected=new Uint8Array(w*h);
  const fg=(x,y)=>{const i=((y+t)*1536+x+l)*4,R=px[i],G=px[i+1],B=px[i+2];return !(R<36&&G-R>4&&B-R>8&&B-R<40)};
  for(let y=0;y<h;y++)for(let x=0;x<w;x++) {
   const n=y*w+x;if(seen[n]||!fg(x,y))continue;
   seen[n]=1;const q=[n];let minx=x,maxx=x,miny=y,maxy=y;
   for(let at=0;at<q.length;at++) {
    const v=q[at],xx=v%w,yy=Math.floor(v/w);minx=Math.min(minx,xx);maxx=Math.max(maxx,xx);miny=Math.min(miny,yy);maxy=Math.max(maxy,yy);
    for(const [nx,ny]of [[xx-1,yy],[xx+1,yy],[xx,yy-1],[xx,yy+1]])if(nx>=0&&nx<w&&ny>=0&&ny<h){const nn=ny*w+nx;if(!seen[nn]&&fg(nx,ny)){seen[nn]=1;q.push(nn)}}
   }
   if(q.length>800&&maxx-minx>6&&maxy-miny>6){parts.push({area:q.length,box:[l+minx,t+miny,l+maxx+1,t+maxy+1]});for(const i of q)selected[i]=1}
  }
  // Fill enclosed dark painted details (e.g. opaque helmet visor), not gaps
  // connected to the outside. Weapon holes are preserved for review.
  {const outside=new Uint8Array(w*h),queue=[];
   for(let x=0;x<w;x++)for(const y of [0,h-1])if(!selected[y*w+x]){outside[y*w+x]=1;queue.push(y*w+x)}
   for(let y=0;y<h;y++)for(const x of [0,w-1])if(!selected[y*w+x]&&!outside[y*w+x]){outside[y*w+x]=1;queue.push(y*w+x)}
   for(let at=0;at<queue.length;at++){const i=queue[at],x=i%w,y=Math.floor(i/w);for(const [xx,yy]of [[x-1,y],[x+1,y],[x,y-1],[x,y+1]])if(xx>=0&&xx<w&&yy>=0&&yy<h){const n=yy*w+xx;if(!selected[n]&&!outside[n]){outside[n]=1;queue.push(n)}}}
   for(let i=0;i<selected.length;i++)if(!outside[i])selected[i]=1;
  }
  // Preserve manually identified real openings in weapons after filling painted
  // dark details. These are annotations of the source, not model-derived masks.
  for(const hole of job.holes||[]){for(let y=hole[1];y<hole[3];y++)for(let x=hole[0];x<hole[2];x++)if(x>=l&&x<r&&y>=t&&y<b)selected[(y-t)*w+x-l]=0}
  const mask=document.createElement('canvas');mask.width=w;mask.height=h;const ctx=mask.getContext('2d'),rgba=ctx.createImageData(w,h);
  for(let i=0;i<selected.length;i++)if(selected[i]){rgba.data[i*4]=rgba.data[i*4+1]=rgba.data[i*4+2]=255;rgba.data[i*4+3]=255}
  ctx.putImageData(rgba,0,0);
  result[name]={components:parts,region:box,mask:mask.toDataURL(),bounds:[Math.min(...parts.map(p=>p.box[0])),Math.min(...parts.map(p=>p.box[1])),Math.max(...parts.map(p=>p.box[2])),Math.max(...parts.map(p=>p.box[3]))]};
 }
 return result;
}'''

if __name__=='__main__':
    results={}
    for j in JOBS:
        armor=bool(j['side']);x,y,r,b=j['bounds']
        payload={'image':'data:image/png;base64,'+base64.b64encode(Path(j['concept']).read_bytes()).decode(),
                 'armor':armor,'holes':{'coil_pistol':[[548,244,642,251]],'plasma_carbine':[[120,295,140,371],[378,325,418,343]],'starcore_railgun':[[108,233,133,249],[505,207,812,215],[248,257,284,273]]}.get(j['id'],[]),
                 'regions':({'back':[1010,84,1460,731],'side':[640,94,880,731],'front':[100,84,530,730]} if armor else {'front':[max(0,x-3),y-3,r+3,b+3]})}
        results[j['id']]=evaluate('('+EXPR+')('+json.dumps(payload)+')')
        for view,values in results[j['id']].items():
            path=MODELS/'reference-masks'/(j['id']+'-'+view+'.png');path.parent.mkdir(exist_ok=True)
            path.write_bytes(base64.b64decode(values.pop('mask').split(',',1)[1]))
            values['mask']=str(path.relative_to(MODELS))
        print(j['id'],json.dumps(results[j['id']]),flush=True)
    (MODELS/'reference-bounds.json').write_text(json.dumps(results,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
