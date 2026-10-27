/* Native Blockbench enemy authoring. Geometry comes from the concept-sheet spec. */
window.CreateEnemyModel = async function(job) {
 await newProject(Formats.free);
 Project.name=job.id;Project.texture_width=2048;Project.texture_height=1024;Project.box_uv=false;
 const image=new Image();image.src=job.image;await image.decode();
 const canvas=document.createElement('canvas');canvas.width=2048;canvas.height=1024;
 const ctx=canvas.getContext('2d');ctx.drawImage(image,0,0);
 // Original concept projections retain their painted light. Hidden cut faces use
 // a separate deterministic pixel material strip instead of background pixels.
 const rgb=hex=>hex.replace('#','').match(/../g).map(x=>parseInt(x,16));
 job.palette.forEach((hex,m)=>{
  const c=rgb(hex);ctx.fillStyle=hex;ctx.fillRect(m*64,960,64,64);
  for(let y=0;y<64;y+=4)for(let x=0;x<64;x+=4){const d=((x*17+y*11+m*31)%13)-6;
   ctx.fillStyle=`rgb(${c.map(v=>Math.max(0,Math.min(255,v+d))).join(',')})`;ctx.fillRect(m*64+x,960+y,4,4)}
  ctx.strokeStyle='rgba(0,0,0,.13)';ctx.lineWidth=1;ctx.strokeRect(m*64+1,961,62,62);
 });
 const texture=new Texture({name:job.id+'_orthographic_atlas.png',id:'0',uv_width:2048,uv_height:1024});
 texture.fromDataURL(canvas.toDataURL()).add(false);await new Promise(r=>setTimeout(r,100));
 const groups={};
 for(const bone of job.bones){const g=new Group({name:bone.name,origin:bone.pivot}).init();
  if(bone.parent)g.addTo(groups[bone.parent]);groups[bone.name]=g;}
 const cross=(a,b)=>new THREE.Vector3(...a).cross(new THREE.Vector3(...b));
 const subtract=(a,b)=>a.map((x,i)=>x-b[i]);
 for(const part of job.parts){
  const mesh=new Mesh({name:part.name,vertices:{}});
  const keys=mesh.addVertices(...part.vertices);
  const center=part.vertices.reduce((a,p)=>a.map((x,i)=>x+p[i]/part.vertices.length),[0,0,0]);
  for(let face of part.faces){
   face=face.slice();let normal=new THREE.Vector3();
   for(let i=0;i<face.length;i++){const a=part.vertices[face[i]],b=part.vertices[face[(i+1)%face.length]];
    normal.x+=(a[1]-b[1])*(a[2]+b[2]);normal.y+=(a[2]-b[2])*(a[0]+b[0]);normal.z+=(a[0]-b[0])*(a[1]+b[1]);}
   if(normal.lengthSq()<1e-12)continue;
   const fc=face.reduce((a,k)=>a.map((v,i)=>v+part.vertices[k][i]/face.length),[0,0,0]);
   if(!part.oriented&&normal.dot(new THREE.Vector3(...subtract(fc,center)))<0){face.reverse();normal.negate()}
   normal.normalize();
   const axis=[0,1,2].sort((a,b)=>Math.abs(normal.getComponent(b))-Math.abs(normal.getComponent(a)))[0];
   const axes=[0,1,2].filter(i=>i!==axis);
   const contour=face.map(i=>new THREE.Vector2(part.vertices[i][axes[0]],part.vertices[i][axes[1]]));
   const triangles=THREE.ShapeUtils.triangulateShape(contour,[]);
   for(const tri of triangles){
    let ids=tri.map(i=>face[i]);
    const n=cross(subtract(part.vertices[ids[1]],part.vertices[ids[0]]),subtract(part.vertices[ids[2]],part.vertices[ids[0]]));
    if(n.dot(normal)<0)ids.reverse();
    const uv={};
    for(const k of ids){const [x,y,z]=part.vertices[k];let p;
     if(part.material!==null||axis===1){const m=part.material===null?0:part.material;
      const vs=part.vertices.map(v=>[v[axes[0]],v[axes[1]]]),lo=axes.map((_,i)=>Math.min(...vs.map(v=>v[i]))),hi=axes.map((_,i)=>Math.max(...vs.map(v=>v[i])));
      p=[m*64+3+(part.vertices[k][axes[0]]-lo[0])/Math.max(hi[0]-lo[0],.001)*58,
          963+(part.vertices[k][axes[1]]-lo[1])/Math.max(hi[1]-lo[1],.001)*58];
     } else if(axis===2){const [cx,ground,s]=normal.z<0?job.front:job.back;p=[cx+(normal.z<0?-x:x)/s,ground-y/s]}
     else {const [origin,ground,s]=job.side;p=[origin+z/s,ground-y/s]}
     uv[keys[k]]=p;
    }
    mesh.addFaces(new MeshFace(mesh,{vertices:ids.map(k=>keys[k]),uv,texture:texture.uuid}));
   }
  }
  mesh.addTo(groups[part.bone]).init();
 }
 Canvas.updateAll();scene.updateMatrixWorld(true);
 return {project:Codecs.project.compile({raw:true,bitmaps:true,absolute_paths:false}),
  tool:Blockbench.version,parts:Outliner.elements.length,bones:Group.all.length};
};

window.ExtractEnemyModel = function(){
 Canvas.updateAll();scene.updateMatrixWorld(true);
 const bones=Group.all.map(g=>({name:g.name,parent:g.parent instanceof Group?g.parent.name:null,pivot:g.origin.slice()}));
 const parts=[];
 for(const element of Outliner.elements){
  if(!element.mesh?.geometry)continue;
  const g=element.mesh.geometry,p=g.getAttribute('position'),uv=g.getAttribute('uv'),index=g.getIndex(),triangles=[];
  for(let i=0;i<(index?index.count:p.count);i+=3){const face=[];
   for(let k=0;k<3;k++){const n=index?index.getX(i+k):i+k;
    const v=new THREE.Vector3(p.getX(n),p.getY(n),p.getZ(n)).applyMatrix4(element.mesh.matrixWorld);
    face.push([v.x,v.y,v.z,uv.getX(n),1-uv.getY(n)].map(x=>Math.round(x*1e7)/1e7));}
   triangles.push(face);
  }
  parts.push({name:element.name,bone:element.parent.name,emissive:element.name.includes('[glow]'),triangles});
 }
 return {bones,parts};
};
'Enemy native authoring ready';
