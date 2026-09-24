/* Run through Blockbench MCP risky_eval in a new Generic Model project.
 * Import nova_body and nova_eyes first. All meshes, UVs and paint are authored
 * in Blockbench; export_nova_terminal.py only packages its actual OBJ export.
 */
(() => {
  const created = [];
  Undo.initEdit({elements: created, outliner: true, textures: Texture.all});
  Project.texture_width = 128;
  Project.texture_height = 128;
  for(const t of Texture.all.filter(t=>['nova_body','nova_eyes'].includes(t.name))){t.uv_width=96;t.uv_height=112;}
  const groups = Object.fromEntries(Group.all.map(g => [g.name, g]));
  const tex = {};
  function texture(name, w, h, draw, emissive = false, uvh = h) {
    const c = document.createElement('canvas'); c.width = w; c.height = h;
    const ctx = c.getContext('2d'); ctx.imageSmoothingEnabled = false;
    draw(ctx);
    const t = new Texture({name}).add();
    t.layers_enabled = false; t.uv_width = w; t.uv_height = uvh;
    t.fps = 10; t.frame_time = 2;
    t.render_mode = emissive ? 'emissive' : 'default';
    t.fromDataURL(c.toDataURL('image/png'));
    tex[name] = t;
    return t;
  }
  for (const [name, color, light, dark] of [
    ['nova_titanium','#667d88','#8098a2','#506572'],
    ['nova_graphite','#1d2c36','#293e4a','#14222c'],
    ['nova_edge','#364b57','#49606d','#273b47'],
    ['nova_recess','#0c1822','#172731','#08121a']
  ]) texture(name,128,128,c=>{
    c.fillStyle=color; c.fillRect(0,0,128,128);
    c.fillStyle=light; c.fillRect(0,0,128,2); c.fillRect(0,64,128,1);
    c.fillStyle=dark; c.fillRect(0,62,128,2); c.fillRect(0,126,128,2);
    c.fillRect(31,0,1,128); c.fillRect(95,0,1,128);
  });
  texture('nova_cyan',16,16,c=>{c.fillStyle='#63d9df';c.fillRect(0,0,16,16);},true);
  const glyphs={N:['101','111','111','111','101'],O:['111','101','101','101','111'],V:['101','101','101','101','010'],A:['010','101','111','101','101'],'.':['0','0','0','0','1']};
  function label(c,text,x,y,color,scale=1) {
    c.fillStyle=color;
    for(const ch of text){const g=glyphs[ch];if(g)g.forEach((row,j)=>[...row].forEach((v,i)=>{if(v==='1')c.fillRect(x+i*scale,y+j*scale,scale,scale);}));x+=(ch==='.'?2:4)*scale;}
  }
  texture('nova_ident',128,32,c=>{
    c.fillStyle='#142530';c.fillRect(0,0,128,32);
    c.fillStyle='#314b58';c.fillRect(0,0,128,2);c.fillRect(0,30,128,2);
    label(c,'N.O.V.A.',24,10,'#bbd4dc',2);
    c.fillStyle='#45636e';c.fillRect(6,11,5,10);c.fillRect(117,11,5,10);
  });
  const body = Texture.all.find(t=>t.name==='nova_body').img;
  const eyes = Texture.all.find(t=>t.name==='nova_eyes').img;
  texture('nova_screen',128,112*48,c=>{
    for(let f=0;f<48;f++) {
      const top=f*112, bob=Math.round(Math.sin(f*Math.PI/24));
      const gaze=f>=12&&f<24?1:f>=36&&f<42?-1:0;
      c.save();c.translate(0,top);
      c.fillStyle='#071724';c.fillRect(0,0,128,112);
      c.drawImage(body,0,0,1,112,0,-1+bob,16,112);
      c.drawImage(body,95,0,1,112,112,-1+bob,16,112);
      c.drawImage(body,16, -1+bob,96,112);
      if(f===31) {
        c.fillStyle='#83f8f4';c.fillRect(55+gaze,52+bob,6,2);c.fillRect(74+gaze,50+bob,6,2);
      } else if(f===30||f===32) {
        c.drawImage(eyes,0,36,96,36,16+gaze,43+bob,96,18);
      } else c.drawImage(eyes,16+gaze,-1+bob,96,112);
      c.fillStyle='#173a48';c.fillRect(7,16,1,72);c.fillRect(120,16,1,72);
      for(const x of [6,119])for(const y of [14,89]){c.fillStyle='#3e7180';c.fillRect(x,y,3,2);}
      c.fillStyle='#295668';c.fillRect(16,9,10,1);c.fillRect(102,9,10,1);
      c.fillStyle='#4b8997';c.fillRect(16,8,2,3);c.fillRect(110,8,2,3);
      c.strokeStyle='#275160';c.lineWidth=1;c.beginPath();c.ellipse(64,97,25,4,0,0,Math.PI*2);c.stroke();
      const a=f*Math.PI/24;
      c.fillStyle='#59afb9';c.fillRect(Math.round(63+25*Math.cos(a)),Math.round(96+4*Math.sin(a)),3,1);
      c.fillStyle='#204554';c.fillRect(43,106,42,1);
      c.fillStyle='#508d97';c.fillRect(57,106,14,1);
      c.restore();
    }
  },true,112);
  function mesh(name, verts, faces, material, group, uvMap) {
    const m=new Mesh({name,vertices:{},faces:{},origin:[0,0,0]});
    const keys=m.addVertices(...verts);
    for(const face of faces) {
      const a=verts[face[0]],b=verts[face[1]],c=verts[face[2]];
      const u=b.map((v,i)=>v-a[i]),v=c.map((v,i)=>v-a[i]);
      const n=[u[1]*v[2]-u[2]*v[1],u[2]*v[0]-u[0]*v[2],u[0]*v[1]-u[1]*v[0]];
      const axis=n.map(Math.abs).indexOf(Math.max(...n.map(Math.abs)));
      const uv={};
      for(const i of face){const p=verts[i];uv[keys[i]]=uvMap?uvMap(p):axis===2?[p[0]*8,128-p[1]*8]:axis===1?[p[0]*8,p[2]*8]:[p[2]*8,128-p[1]*8];}
      if(material==='nova_cyan')for(const p of Object.values(uv)){p[0]/=8;p[1]/=8;}
      m.addFaces(new MeshFace(m,{vertices:face.map(i=>keys[i]),texture:tex[material].uuid,uv}));
    }
    m.addTo(groups[group]);m.init();created.push(m);return m;
  }
  function oct(x,y,w,h,c){return [[x+c,y],[x+w-c,y],[x+w,y+c],[x+w,y+h-c],[x+w-c,y+h],[x+c,y+h],[x,y+h-c],[x,y+c]];}
  function solid(name,x,y,w,h,z0,z1,cut,bevel,material,group) {
    const vs=[];
    for(const [p,z] of [[oct(x+bevel,y+bevel,w-2*bevel,h-2*bevel,Math.max(.03,cut-bevel)),z0],[oct(x,y,w,h,cut),z0+bevel],[oct(x,y,w,h,cut),z1-bevel],[oct(x+bevel,y+bevel,w-2*bevel,h-2*bevel,Math.max(.03,cut-bevel)),z1]])vs.push(...p.map(q=>[...q,z]));
    const fs=[];
    for(let r=0;r<3;r++)for(let i=0;i<8;i++)fs.push([r*8+i,r*8+(i+1)%8,(r+1)*8+(i+1)%8,(r+1)*8+i]);
    vs.push([x+w/2,y+h/2,z0],[x+w/2,y+h/2,z1]);
    for(let i=0;i<8;i++){fs.push([32,(i+1)%8,i]);fs.push([33,24+i,24+(i+1)%8]);}
    return mesh(name,vs,fs,material,group);
  }
  function frame(name,outer,inner,z0,z1,material,group) {
    const vs=[...outer.map(p=>[...p,z0]),...inner.map(p=>[...p,z0+.17]),...outer.map(p=>[...p,z1]),...inner.map(p=>[...p,z1])],fs=[];
    for(let i=0;i<8;i++){let j=(i+1)%8;fs.push([i,i+8,j+8,j],[i,j,j+16,i+16],[i+8,i+24,j+24,j+8],[i+16,j+16,j+24,i+24]);}
    return mesh(name,vs,fs,material,group);
  }
  solid('rear_wall_mount',2.2,2.0,11.6,12.0,14.35,16,.65,.15,'nova_recess','mount');
  solid('faceted_main_housing',.65,1.0,14.7,14.4,10.5,14.9,1.25,.38,'nova_graphite','housing');
  frame('titanium_display_surround',oct(.8,3.0,14.4,12.3,1.15),oct(1.85,3.58,12.3,10.8,.45),9.85,11.1,'nova_titanium','bezel');
  frame('recessed_glass_gasket',oct(1.85,3.58,12.3,10.8,.45),oct(2,3.73,12,10.5,.35),10.02,10.5,'nova_recess','bezel');
  const p=oct(2,3.73,12,10.5,.35),sv=p.map(v=>[...v,10.22]);sv.push([8,8.98,10.22]);
  mesh('nova_animated_display',sv,p.map((_,i)=>[8,(i+1)%8,i]),'nova_screen','display',p=>[(14-p[0])/12*128,(14.23-p[1])/10.5*112]);
  solid('lower_communications_chin',1.15,.8,13.7,2.95,8.7,13.9,.7,.25,'nova_titanium','controls');
  solid('chin_graphite_inset',2.5,1.25,11,1.88,8.39,8.83,.28,.12,'nova_graphite','controls');
  mesh('nova_identification_plate',[[4.5,2.08,8.26],[11.5,2.08,8.26],[11.5,3.02,8.26],[4.5,3.02,8.26]],[[0,3,2,1]],'nova_ident','controls',p=>[(11.5-p[0])/7*128,(3.02-p[1])/.94*32]);
  for(let i=0;i<9;i++)solid('microphone_slot_'+i,5.0+i*.67,1.47,.4,.23,8.24,8.4,.055,.025,'nova_recess','controls');
  solid('access_key',3.0,1.65,1.0,1.07,8.08,8.5,.22,.11,'nova_edge','controls');
  solid('access_key_marker',3.22,1.94,.56,.25,8.035,8.1,.06,.02,'nova_cyan','controls');
  solid('status_key',12.0,1.65,1,1.07,8.08,8.5,.22,.11,'nova_edge','controls');
  solid('status_key_marker',12.29,1.89,.42,.42,8.035,8.1,.09,.02,'nova_cyan','controls');
  for(const [side,x] of [['left',.78],['right',14.53]]) {
    solid(side+'_shoulder',x,4.1,.69,9.7,10.7,13.5,.26,.11,'nova_edge','housing');
    for(let i=0;i<5;i++)solid(side+'_vent_'+i,x+.10,5.1+i*1.3,.49,.58,10.59,10.85,.12,.04,'nova_recess','ventilation');
  }
  for(const [side,x] of [['left',.64],['right',15.36]])for(let i=0;i<6;i++) {
    const poly=oct(11.55,4.6+i*1.25,2.5,.44,.14);
    const vs=poly.map(p=>[x,p[1],p[0]]);vs.push([x,4.82+i*1.25,12.8]);
    mesh(side+'_side_exhaust_'+i,vs,poly.map((_,j)=>side==='left'?[8,j,(j+1)%8]:[8,(j+1)%8,j]),'nova_recess','ventilation');
  }
  solid('upper_sensor_socket',7.2,14.52,1.6,.47,9.59,9.95,.19,.07,'nova_recess','bezel');
  solid('upper_sensor_lens',7.81,14.64,.38,.2,9.54,9.64,.06,.02,'nova_cyan','bezel');
  for(const x of [1.4,14.1])solid('lower_frame_fastener_'+x,x,3.4,.5,.5,9.55,9.92,.15,.06,'nova_edge','bezel');
  Canvas.updateAll();
  Undo.finishEdit('Build NOVA communication terminal with layered animated portrait');
  return {meshes:created.length,faces:created.reduce((n,m)=>n+Object.keys(m.faces).length,0),textures:Texture.all.map(t=>({name:t.name,uuid:t.uuid}))};
})()
