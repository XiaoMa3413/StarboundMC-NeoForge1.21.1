/* Executed inside the real Blockbench renderer. Always starts a NEW free model.
 * No existing project is loaded. Every vertex/UV originates in V01 tracings.
 */
window.V04Create = async function(job) {
    await newProject(Formats.free);
    Project.name = job.id + '_concept_rebuild_v04';
    Project.texture_width=1536; Project.texture_height=1088; Project.box_uv=false;
    const source=new Image();source.src=job.image;await source.decode();
    const canvas=document.createElement('canvas');canvas.width=1536;canvas.height=1088;
    const context=canvas.getContext('2d');context.drawImage(source,0,0);
    const base={pioneer:'#696a70',vanguard:'#34495d',ranger:'#394147',starwarden:'#292959'}[job.id]||'#343943';
    const colors=[base,'#26292f','#9fa1ac','#d9d0c2','#7b4c29','#49616d','#25254c','#418c85'];
    colors.forEach((c,i)=>{context.fillStyle=c;context.fillRect(i*64,1024,64,64)});
    const texture = new Texture({name:job.id+'_concept_projection.png',id:'0',uv_width:1536,uv_height:1088});
    texture.fromDataURL(canvas.toDataURL()).add(false);
    await new Promise((resolve,reject)=>{if(texture.img.complete&&texture.img.naturalWidth)resolve();else{texture.img.onload=resolve;texture.img.onerror=reject}});
    const [bx,by,ex,ey]=job.bounds, scale=32/Math.max(ex-bx,ey-by), cx=(bx+ex)/2;
    const groups={};
    const partBounds={};
    const materialUV=part=>{
        let m=0;
        if(/黑|握|缠|扳机|电池|内衬|深处/.test(part.name))m=1;
        else if(/银|金属|钨钢|主刃/.test(part.name))m=2;
        else if(/象牙|白色/.test(part.name))m=3;
        else if(/铜线圈/.test(part.name))m=4;
        else if(/紫/.test(part.name))m=6;
        else if(/青绿/.test(part.name))m=7;
        return [m*64+16,1040,m*64+32,1056];
    };
    const allUV=(x1,y1,x2,y2,z1,z2,part)=>{
        const front=[x1,y1,x2,y2];
        let back=[x2,y1,x1,y2], top,side;
        if(job.back) {
            const [l,t,r,b]=job.back;
            const X=x=>r-(x-bx)/(ex-bx)*(r-l),Y=y=>t+(y-by)/(ey-by)*(b-t);
            back=[X(x2),Y(y1),X(x1),Y(y2)];
        }
        const p=part.polygon, px=p.reduce((v,q)=>v+q[0],0)/p.length,py=p.reduce((v,q)=>v+q[1],0)/p.length;
        if(job.top && !/握|缠|扳机|尾盖|电池|灯条|铆钉|镶块/.test(part.name)) {
            const [l,t,r,b]=job.top;
            const X=x=>l+(x-bx)/(ex-bx)*(r-l),Z=z=>(t+b)/2+z;
            top=[X(x2),Z(z1),X(x1),Z(z2)];
        } else {
            // Small material patch, never project text/background onto horizontal edges.
            top=materialUV(part);
        }
        if(job.side) {
            const [l,t,r,b]=job.side;
            const center=(l+r)/2+(part.group==='boots'?-12:0),Z=z=>center+z,Y=y=>t+(y-by)/(ey-by)*(b-t);
            side=[Z(z1),Y(y1),Z(z2),Y(y2)];
        } else {
            const px=(x1+x2)/2,py=(y1+y2)/2;
            side=materialUV(part);
        }
        return {north:front,south:back,up:top,down:top,east:[side[2],side[1],side[0],side[3]],west:side};
    };
    Undo.initEdit({outliner:true,elements:[],textures:[texture]});
    for(const part of job.parts) {
        const group=groups[part.group] ||= new Group({name:part.group,origin:[0,0,0]}).init();
        const p=part.polygon, lo=Math.min(...p.map(v=>v[1])),hi=Math.max(...p.map(v=>v[1]));
        const strips=[];
        // Axis-aligned rectangular outlines remain a single editable cube.
        const isRect=p.length===4&&new Set(p.map(v=>v[0])).size===2&&new Set(p.map(v=>v[1])).size===2;
        if(isRect) strips.push([Math.min(...p.map(v=>v[0])),lo,Math.max(...p.map(v=>v[0])),hi]);
        else {
            // True closed polygon extrusion: continuous bevel/diagonal walls,
            // rather than hundreds of overlapping thin cuboids.
            const mesh=new Mesh({name:part.name,vertices:{}});
            const points=p.map(([x,y])=>[(cx-x)*scale,(ey-y)*scale]);
            const front=mesh.addVertices(...points.map(v=>[...v,part.z[0]*scale]));
            const back=mesh.addVertices(...points.map(v=>[...v,part.z[1]*scale]));
            const uvFront=i=>p[i];
            const uvBack=i=>job.back?[
                job.back[2]-(p[i][0]-bx)/(ex-bx)*(job.back[2]-job.back[0]),
                job.back[1]+(p[i][1]-by)/(ey-by)*(job.back[3]-job.back[1])]:p[i];
            const triangles=THREE.ShapeUtils.triangulateShape(points.map(v=>new THREE.Vector2(...v)),[]);
            for(const tri of triangles) {
                const [a,b,c]=tri.map(i=>points[i]),positive=(b[0]-a[0])*(c[1]-a[1])-(b[1]-a[1])*(c[0]-a[0])>0;
                const order=positive?tri.slice().reverse():tri;
                for(const [ids,indices,uvFn] of [[front,order,uvFront],[back,order.slice().reverse(),uvBack]]) {
                    const vertices=indices.map(i=>ids[i]);
                    mesh.addFaces(new MeshFace(mesh,{vertices,uv:Object.fromEntries(indices.map(i=>[ids[i],uvFn(i)])),texture:texture.uuid}));
                }
            }
            const area=points.reduce((sum,a,i)=>{const b=points[(i+1)%points.length];return sum+a[0]*b[1]-b[0]*a[1]},0);
            for(let i=0;i<p.length;i++) {
                const n=(i+1)%p.length;
                const vertices=[front[i],front[n],back[n],back[i]];if(area<0)vertices.reverse();
                const coordinates=[[i,0],[n,0],[n,1],[i,1]],uv={};
                for(let k=0;k<4;k++) {
                    const [idx,depth]=coordinates[k],key=[front[i],front[n],back[n],back[i]][k],z=part.z[depth];
                    if(job.side&&Math.abs(p[i][1]-p[n][1])>Math.abs(p[i][0]-p[n][0])) {
                        const [l,t,r,b]=job.side;
                        uv[key]=[(l+r)/2+(part.group==='boots'?-12:0)+z,t+(p[idx][1]-by)/(ey-by)*(b-t)];
                    } else if(job.top&&!/握|缠|扳机|尾盖|电池|灯条|铆钉|镶块/.test(part.name)) {
                        const [l,t,r,b]=job.top;
                        uv[key]=[l+(p[idx][0]-bx)/(ex-bx)*(r-l),(t+b)/2+z];
                    } else {const u=materialUV(part);uv[key]=[u[k===1||k===2?2:0],u[k>1?3:1]]}
                }
                mesh.addFaces(new MeshFace(mesh,{vertices,uv,texture:texture.uuid}));
            }
            mesh.addTo(group).init();
        }
        for(const [x1,y1,x2,y2] of strips) {
            const [z1,z2]=part.z;
            const uv=allUV(x1,y1,x2,y2,z1,z2,part);
            new Cube({name:part.name,from:[(cx-x2)*scale,(ey-y2)*scale,z1*scale],to:[(cx-x1)*scale,(ey-y1)*scale,z2*scale],
                box_uv:false,autouv:0,faces:Object.fromEntries(Object.entries(uv).map(([f,u])=>[f,{uv:u,texture:texture.uuid}]))}).addTo(group).init();
        }
        partBounds[part.name]={polygon:p,z:part.z};
    }
    Canvas.updateAll();Undo.finishEdit('V04: construct new geometry from original concept');
    Project.saved=true;
    await new Promise(r=>setTimeout(r,150));
    return {project:Codecs.project.compile({raw:true,bitmaps:true,absolute_paths:false}),version:Blockbench.version,
        cubes:Cube.all.length,meshes:Mesh.all.length,groups:Group.all.length,parts:job.parts.length,source:'newProject(Formats.free); original V01 concept only',partBounds};
};

window.V04View = async function(name) {
    for(const c of Cube.selected.slice())c.unselect();
    Canvas.updateAll();
    const vertices=[...Cube.all.flatMap(c=>c.getGlobalVertexPositions()),...Mesh.all.flatMap(m=>Object.values(m.vertices).map(v=>new THREE.Vector3(...v).applyMatrix4(m.mesh.matrixWorld).toArray()))];
    const min=[0,1,2].map(i=>Math.min(...vertices.map(p=>p[i]))),max=[0,1,2].map(i=>Math.max(...vertices.map(p=>p[i])));
    const center=min.map((v,i)=>(v+max[i])/2),span=Math.max(...max.map((v,i)=>v-min[i]));
    const preview=Preview.selected;preview.resize();
    const offset={front:[0,0,-100],back:[0,0,100],side:[-100,0,0],top:[0,100,0],perspective:[-55,30,-75],reference_side:[-100,8,-30],reference_back:[20,4,100]}[name];
    preview.loadAnglePreset({position:center.map((v,i)=>v+offset[i]),target:center,projection:'orthographic',zoom:Math.min(preview.width,preview.height)/(40*span*1.18)});
    preview.camera.up.set(0,name==='top'?0:1,name==='top'?1:0);
    preview.camera.lookAt(new THREE.Vector3(...center));
    await new Promise(r=>setTimeout(r,160));
    const png=await new Promise(resolve=>preview.screenshot({crop:false,width:1400,height:1100},resolve));
    return {png,min,max,center,camera:preview.camera.position.toArray(),projection:'orthographic',view:name};
};
'V04 native empty-project authoring ready';
