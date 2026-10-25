(() => {
  const textures=[];
  Undo.initEdit({textures,elements:Mesh.all.slice()});
  function make(name,emissive,paint){
    const canvas=document.createElement('canvas');canvas.width=canvas.height=256;
    const ctx=canvas.getContext('2d');ctx.imageSmoothingEnabled=false;paint(ctx);
    const t=new Texture({name}).add();t.layers_enabled=false;t.uv_width=t.uv_height=256;
    t.render_mode=emissive?'emissive':'default';t.fromDataURL(canvas.toDataURL('image/png'));textures.push(t);return t;
  }
  const surfaces=make('shipboard_surfaces',false,c=>{
    const colors=['#6b808a','#23343f','#415865','#0b171f','#12313d','#779297','#2c414d','#405963'];
    for(let tile=0;tile<16;tile++){
      let x=tile%4*64,y=Math.floor(tile/4)*64;c.fillStyle=colors[tile%colors.length];c.fillRect(x,y,64,64);
      if(tile!==3&&tile!==4){c.fillStyle='#ffffff0c';c.fillRect(x+1,y+1,62,1);c.fillStyle='#07101930';c.fillRect(x,y+62,64,2);}
      if(tile===5){c.fillStyle='#c4d9da';c.fillRect(x,y+29,64,3);c.fillStyle='#355661';c.fillRect(x,y+33,64,2);}
      if(tile===0||tile===2){c.fillStyle='#c2d2d544';for(const [a,b] of [[5,6],[56,6],[5,56],[56,56]]){c.fillRect(x+a,y+b,3,3);c.fillStyle='#263943';c.fillRect(x+a+1,y+b,1,3);c.fillStyle='#c2d2d544';}}
      if(tile===6){c.strokeStyle='#597780';c.lineWidth=2;c.strokeRect(x+12,y+4,40,56);c.fillStyle='#97afb4';c.fillRect(x+21,y+28,22,2);c.fillRect(x+29,y+24,6,10);}
    }
  });
  const signals=make('shipboard_signals',true,c=>{
    c.fillStyle='#0c1b25';c.fillRect(0,0,256,256);
    const colors=['#75d9dd','#e2b66f','#91d3a4','#e4eeed'];
    for(let t=0;t<4;t++){c.fillStyle=colors[t];c.fillRect(t*64,0,64,64);}
    c.fillStyle='#c8dcdf';for(let x=192;x<256;x+=4)c.fillRect(x,0,1,64);
    for(let tile=4;tile<16;tile++){
      const x=tile%4*64,y=Math.floor(tile/4)*64;
      c.save();c.translate(x,y);c.fillStyle='#29434f';c.fillRect(3,3,58,1);c.fillRect(3,60,58,1);
      const col=tile===5?'#d5a562':tile===6?'#81c6a0':'#72c9d3';c.strokeStyle=col;c.fillStyle=col;c.lineWidth=1;
      if(tile===4){c.strokeRect(9,20,20,24);for(let i=0;i<3;i++){c.fillRect(36,21+i*9,18-i*4,3);c.fillRect(13+i*5,25,3,13);}}
      if(tile===5){c.strokeRect(27,10,7,41);c.fillRect(29,30,3,19);for(let y=13;y<50;y+=6)c.fillRect(36,y,4,1);c.fillRect(26,54,10,2);}
      if(tile===6){c.strokeRect(7,27,26,7);c.strokeRect(30,29,17,3);c.strokeRect(21,34,9,5);c.fillRect(10,28,3,5);c.fillRect(7,24,10,1);c.fillRect(7,37,10,1);c.fillRect(49,26,8,1);}
      if(tile===7){c.beginPath();c.arc(32,32,20,0,Math.PI*2);c.stroke();c.fillRect(31,9,2,46);c.fillRect(9,31,46,2);c.beginPath();c.moveTo(29,37);c.lineTo(39,21);c.lineTo(37,40);c.closePath();c.stroke();}
      if(tile===8){c.beginPath();c.arc(30,30,18,0,Math.PI*2);c.stroke();c.strokeRect(22,21,10,17);c.fillRect(36,29,5,2);c.fillRect(39,31,2,3);c.fillRect(35,35,6,2);c.fillRect(35,37,2,3);c.fillRect(35,41,6,2);}
      if(tile===9){c.strokeRect(22,16,21,31);c.strokeRect(16,22,6,20);c.strokeRect(43,22,6,20);c.fillRect(28,22,9,2);c.fillRect(27,29,11,11);}
      if(tile===10){c.beginPath();for(let i=0;i<6;i++){let a=i*Math.PI/3;c.lineTo(32+19*Math.cos(a),32+19*Math.sin(a));}c.closePath();c.stroke();c.fillRect(29,21,6,22);c.fillRect(23,28,18,8);}
      if(tile>=11){for(let j=0;j<3;j++)for(let i=0;i<4;i++){c.fillStyle=i===0?col:'#355560';c.fillRect(9+i*12,20+j*10,7,4);}}
      c.restore();
    }
  });
  const mapping={metal:[surfaces,0],dark:[surfaces,1],edge:[surfaces,2],recess:[surfaces,3],glass:[surfaces,4],tank:[surfaces,5],hatch:[surfaces,6],
    cyan:[signals,0],amber:[signals,1],green:[signals,2],white:[signals,3],refinery:[signals,4],thermal:[signals,5],blueprint:[signals,6],navigation:[signals,7],oxygen:[signals,8],epp:[signals,9],ignition:[signals,10],tools:[signals,11],screen:[signals,7]};
  const dot=(a,b)=>a.reduce((s,v,i)=>s+v*b[i],0),sub=(a,b)=>a.map((v,i)=>v-b[i]),cross=(a,b)=>[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]],unit=a=>{let l=Math.hypot(...a);return a.map(v=>v/l);};
  for(const m of Mesh.all){
    const spec=SHIPBOARD_SPEC.parts.find(p=>p.name===m.name);if(!spec)throw new Error('Missing material specification '+m.name);
    const [texture,tile]=mapping[spec.paint],tx=tile%4*64,ty=Math.floor(tile/4)*64;
    for(const f of Object.values(m.faces)){
      const keys=f.getSortedVertices(),points=keys.map(k=>m.vertices[k]);
      let u=unit(sub(points[1],points[0])),normal=unit(cross(sub(points[1],points[0]),sub(points[2],points[0]))),v=unit(cross(normal,u));
      if(spec.uv==='panel'){
        const xs=points.map(p=>p[0]),ys=points.map(p=>p[1]),zs=points.map(p=>p[2]);
        const width=Math.max(...xs)-Math.min(...xs),height=Math.hypot(Math.max(...ys)-Math.min(...ys),Math.max(...zs)-Math.min(...zs));
        const scale=56/Math.max(width,height),topY=Math.max(...ys),topZ=Math.max(...zs);
        for(let i=0;i<keys.length;i++){
          let p=points[i],vertical=topY-Math.min(...ys)<1e-7?Math.max(...zs)-p[2]:Math.hypot(topY-p[1],topZ-p[2]);
          f.uv[keys[i]]=[tx+32+(Math.max(...xs)-p[0]-width/2)*scale,ty+32+(vertical-height/2)*scale];
        }
      }else{
        const axis=normal.map(Math.abs).indexOf(Math.max(...normal.map(Math.abs)));
        const seed=axis===0?[0,0,1]:[-1,0,0];
        u=unit(sub(seed,normal.map(n=>n*dot(seed,normal))));
        v=unit(cross(normal,u));
        const down=axis===1?[0,0,-1]:[0,-1,0];if(dot(v,down)<0)v=v.map(n=>-n);
        const all=Object.values(m.vertices).map(p=>[dot(p,u)*4,dot(p,v)*4]);
        const uv=points.map(p=>[dot(p,u)*4,dot(p,v)*4]);const mins=[Math.min(...all.map(p=>p[0])),Math.min(...all.map(p=>p[1]))];
        const sizes=[Math.max(...all.map(p=>p[0]))-mins[0],Math.max(...all.map(p=>p[1]))-mins[1]];
        const scale=Math.min(1,60/Math.max(...sizes));
        for(let i=0;i<keys.length;i++)f.uv[keys[i]]=[tx+2+(uv[i][0]-mins[0])*scale,ty+2+(uv[i][1]-mins[1])*scale];
      }
      f.texture=texture.uuid;
    }
  }
  Canvas.updateAll();Undo.finishEdit('Paint shipboard atlas and map proportional mesh UVs');
  return {meshes:Mesh.all.length,textures:textures.map(t=>({name:t.name,uuid:t.uuid}))};
})()
