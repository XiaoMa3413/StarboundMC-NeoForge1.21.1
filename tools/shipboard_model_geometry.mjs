// Geometry descriptions are sent to Blockbench MCP place_mesh; this module never writes models.
export function build(kind) {
  const parts = [];
  const add = (name,vertices,faces,paint='metal',group='structure',uv=null) => {
    parts.push({name,vertices,faces,paint,group,uv}); return parts.at(-1);
  };
  const oct=(x,y,w,h,c)=>[[x+c,y],[x+w-c,y],[x+w,y+c],[x+w,y+h-c],[x+w-c,y+h],[x+c,y+h],[x,y+h-c],[x,y+c]];
  function box(n,a,b,p='metal',g='structure',cut=.4,bevel=.12) {
    const [x,y,z]=a,[X,Y,Z]=b,w=X-x,h=Y-y;
    cut=Math.min(cut,w/3,h/3);bevel=Math.min(bevel,(Z-z)/3,w/6,h/6,cut*.6);
    const verts=[...oct(x+bevel,y+bevel,w-2*bevel,h-2*bevel,cut-bevel).map(q=>[...q,z]),
      ...oct(x,y,w,h,cut).map(q=>[...q,z+bevel]),...oct(x,y,w,h,cut).map(q=>[...q,Z])];
    const faces=[];
    for(let i=0;i<8;i++){const j=(i+1)%8;faces.push([i,j,j+8,i+8],[i+8,j+8,j+16,i+16]);}
    for(let i=1;i<7;i++)faces.push([0,i+1,i],[16,16+i,17+i]);
    return add(n,verts,faces,p,g);
  }
  function tube(n,c,axis,r,length,p='edge',g='mechanism',r2=r,sides=12) {
    const v=[],f=[];const [cx,cy,cz]=c;
    const point=(rad,t,h)=>axis==='y'?[cx+rad*Math.cos(t),cy+h,cz+rad*Math.sin(t)]:[cx+rad*Math.cos(t),cy+rad*Math.sin(t),cz+h];
    for(const [rad,h] of [[r,0],[r2,length]])for(let i=0;i<sides;i++)v.push(point(rad,i*Math.PI*2/sides,h));
    for(let i=0;i<sides;i++){let j=(i+1)%sides;f.push(axis==='z'?[i,j,j+sides,i+sides]:[i,i+sides,j+sides,j]);}
    for(let i=1;i<sides-1;i++){
      f.push(axis==='z'?[0,i+1,i]:[0,i,i+1]);
      f.push(axis==='z'?[sides,sides+i,sides+i+1]:[sides,sides+i+1,sides+i]);
    }
    return add(n,v,f,p,g);
  }
  function ring(n,c,r,inner,length,p='edge',g='mechanism',axis='z',sides=12) {
    const v=[],f=[];
    for(const [radius,h] of [[r,0],[inner,0],[r,length],[inner,length]])for(let i=0;i<sides;i++) {
      let t=i*2*Math.PI/sides;
      v.push(axis==='z'?[c[0]+radius*Math.cos(t),c[1]+radius*Math.sin(t),c[2]+h]:[c[0]+radius*Math.cos(t),c[1]+h,c[2]+radius*Math.sin(t)]);
    }
    for(let i=0;i<sides;i++){let j=(i+1)%sides;for(let q of [[i,i+sides,j+sides,j],[i,j,j+2*sides,i+2*sides],[i+sides,i+3*sides,j+3*sides,j+sides],[i+2*sides,j+2*sides,j+3*sides,i+3*sides]])f.push(axis==='y'?q.reverse():q);}
    return add(n,v,f,p,g);
  }
  function panel(n,a,b,paint='screen',group='instruments') {
    const [x,y,z]=a,[X,Y,Z]=b;
    return add(n,[[x,y,z],[X,y,z],[X,Y,Z],[x,Y,Z]],[[0,3,2,1]],paint,group,'panel');
  }
  function top(n,x,z,w,d,y,p='screen') {
    return add(n,[[x,y,z],[x+w,y,z],[x+w,y,z+d],[x,y,z+d]],[[0,3,2,1]],p,'instruments','panel');
  }
  function base(){box('plinth',[.7,0,.7],[15.3,2,15.3],'dark');box('plinth_trim',[1.1,1.2,1.1],[14.9,2.3,14.9],'edge');}
  function feet(){for(const x of [1,12])for(const z of [1,12])box(`foot_${x}_${z}`,[x,0,z],[x+3,1.6,z+3],'dark');}
  if(kind==='ship_light') {
    box('wall_mount',[2.1,5.8,14.6],[13.9,10.2,16],'dark','mount');
    box('cut_corner_housing',[1,5,13.05],[15,11,15.6],'edge','housing',.7,.22);
    box('recessed_diffuser_bed',[2.3,5.75,12.85],[13.7,10.25,13.2],'recess','housing');
    for(const y of [6.15,8.2])box('diffuser_'+y,[2.8,y,12.62],[13.2,y+1.65,13.05],'white','diffusers',.32,.12);
    for(const x of [1.25,13.35])box('retaining_endcap_'+x,[x,5.25,12.45],[x+1.4,10.75,13.65],'metal','end_caps',.32,.15);
    box('central_baffle',[2.55,7.8,12.65],[13.45,8.2,13.3],'edge','housing',.09,.04);
  }
  if(kind==='voxel_refinery') {
    base();box('armored_rear_chassis',[1.4,2,5],[14.6,13.7,14.5],'metal');
    box('inset_processing_bay',[3,3,4.2],[13,11.8,5.2],'recess');
    ring('chamber_rim',[8,8,2.55],3.65,2.95,.85,'edge');
    tube('chamber_back',[8,8,3.65],'z',2.95,.2,'glass');
    for(const x of [6.3,8.7])box('voxel_column_'+x,[x,6.7,3.2],[x+1,9.1,3.55],'cyan','mechanism',.25,.08);
    for(const x of [1.25,13])box('shoulder_'+x,[x,2.5,2.8],[x+1.75,12.4,13.4],'edge');
    box('feed_hopper',[4,12.8,5],[12,15.7,12.8],'edge');
    top('hopper_mouth',4.8,5.8,6.4,6.2,15.72,'recess');
    box('collection_drawer',[4,2.35,1.6],[12,4.5,4.2],'dark');
    panel('collection_mark',[4.6,2.85,1.57],[11.4,3.8,1.57],'refinery');
    box('service_control',[11.5,10.7,2.25],[14.2,13.2,3.1],'dark');
    panel('refinery_gauge',[11.8,11,2.23],[13.9,12.85,2.23],'refinery');
    for(const x of [1.42,14.58])for(let i=0;i<3;i++)box('cooling_louver_'+x+'_'+i,[x-.2,4+i*2,7],[x+.2,4.65+i*2,13],'recess','ventilation',.08,.03);
  }
  if(kind==='titanium_alloy_furnace') {
    base();box('insulated_furnace_body',[1.2,2,3.4],[14.8,15,14.5],'dark', 'structure',.85,.3);
    box('refractory_front',[2,3,2.6],[14,13.9,4.2],'metal');
    ring('pressure_door_rim',[7.1,8.7,1.95],4.25,3.15,.9,'edge');
    tube('sealed_door',[7.1,8.7,2.35],'z',3.15,.5,'dark');
    box('inspection_window',[5.2,8.2,2.03],[9,9.25,2.38],'recess','door');
    panel('temperature_strip',[5.45,8.45,2.0],[8.75,9.0,2.0],'amber');
    box('locking_crossbar',[6.6,5.9,1.5],[7.6,11.5,2.4],'edge','door');
    for(const x of [4,9.5])tube('locking_hub_'+x,[x,8.8,1.5],'z',.7,.9,'metal','door',.7,8);
    box('ash_drawer',[3.5,2.4,1.75],[10.7,4.1,3.1],'edge');
    panel('thermal_gauge',[11.3,5,2.5],[13.5,12.6,2.5],'thermal');
    for(const x of [1.05,14.55])for(let i=0;i<4;i++)box('heat_fin_'+x+'_'+i,[x,4.3+i*2.1,5],[x+.4,5.1+i*2.1,13.7],'edge','ventilation',.12,.04);
    box('top_heat_exchanger',[4,14.6,6],[12,15.75,12.3],'metal');
  }
  if(kind==='matter_manipulator_workbench') {
    feet();box('tool_cabinet',[2,1.5,7],[14,10.5,14.2],'dark');
    box('front_drawer',[3,3,6.5],[13,7.2,7.3],'metal');
    box('drawer_handle',[5.5,5.6,6.1],[10.5,6.2,6.7],'edge');
    box('worktop',[.6,10,1],[15.4,11.7,15],'metal');
    top('inset_engineering_mat',2,2.5,12,8.5,11.72,'blueprint');
    box('rear_equipment_rail',[1.5,11.7,12.6],[14.5,15.1,14.7],'dark');
    panel('engineering_screen',[2.3,12.3,12.55],[10.5,14.6,12.55],'blueprint');
    for(const x of [3,11])box('tool_clamp_'+x,[x,11.7,5.2],[x+1.6,13.1,8],'edge','tools');
    box('manipulator_mount',[5.2,11.8,6.2],[10.5,12.35,7.2],'green','tools');
    box('calibration_control',[11.4,12.3,12.1],[13.8,14.6,12.7],'edge');
    panel('control_dials',[11.65,12.55,12.07],[13.55,14.35,12.07],'tools');
    for(const x of [1.4,13.5])box('front_leg_'+x,[x,1.3,2.4],[x+1.1,10.4,4],'edge');
  }
  if(kind==='life_support_station') {
    base();box('air_processor',[8.8,2.3,5.4],[14.6,14.7,14.4],'metal');
    box('filter_bank',[2,2.3,10.5],[9,14.7,14.4],'dark');
    for(const x of [3.9,7.8]) {
      tube('pressure_tank_'+x,[x,3.0,6.2],'y',1.65,9.3,'tank','tanks');
      tube('tank_shoulder_'+x,[x,12.3,6.2],'y',1.65,1.0,'edge','tanks',.85);
      tube('valve_'+x,[x,13.3,6.2],'y',.45,.9,'metal','tanks',.45,8);
      for(const y of [4.3,10.8])ring('tank_band_'+x+'_'+y,[x,y,6.2],1.78,1.6,.6,'edge','tanks','y');
    }
    box('manifold',[2.8,14.05,5.6],[10.6,14.7,6.8],'edge');
    box('oxygen_display_bezel',[10,9.4,4.75],[14.1,13.3,5.5],'dark');
    panel('oxygen_display',[10.35,9.75,4.72],[13.75,12.95,4.72],'oxygen');
    box('canister_dock',[10.1,3.1,3.3],[13.9,6.6,5.9],'edge');
    tube('charging_socket',[12,4.9,3.0],'z',1.2,.5,'recess','mechanism',1.2,10);
    panel('service_label',[10.1,7.1,5.35],[13.9,8.7,5.35],'oxygen');
  }
  if(kind==='epp_service_station') {
    feet();box('tool_base',[1.2,1.4,3],[14.8,7.2,14.7],'dark');
    box('service_deck',[.6,7,1.2],[15.4,8.4,15],'metal');
    top('service_mat',2.5,3.5,10.6,7.6,8.42,'tools');
    box('equipment_backboard',[3.2,8.4,11.8],[12.8,15.7,14.2],'edge');
    box('backpack_dock',[5,9.6,10.4],[11,14.5,12.1],'dark');
    panel('pack_wiring_schematic',[5.5,10.1,10.37],[10.5,14.05,10.37],'epp');
    for(const x of [3.6,11.3])box('charging_clamp_'+x,[x,10,9.6],[x+1.1,14.9,12.2],'metal','tools');
    box('diagnostic_pod',[1.1,9,6.5],[4,12.1,9.1],'dark');
    panel('diagnostic_display',[1.45,9.5,6.46],[3.65,11.7,6.46],'epp');
    tube('probe_holder',[13.35,8.5,7.6],'y',.8,3.3,'edge','tools',.65,8);
    box('lower_tool_drawer',[3,2.5,2.55],[13,5.6,3.3],'edge');
    panel('tool_markers',[4,3.2,2.52],[12,4.9,2.52],'tools');
  }
  if(kind==='ship_console') {
    base();box('rear_pedestal',[3,2.1,8],[13,11.5,14.3],'dark');
    box('console_deck',[1,9.3,2.1],[15,11,14.7],'metal');
    const slope=box('slanted_panel_support',[2,11,6],[14,14.1,13.9],'edge');
    slope.vertices.forEach(v=>{v[1]=9.8+(v[1]-11)/3.1*(14.1-(13.9-v[2])*.45-9.8);});
    panel('heading_display',[3,10.6,5.9],[10.6,14.1,13.7],'navigation');
    panel('auxiliary_controls',[11,10.6,5.9],[13.2,14.1,13.7],'tools');
    top('key_strip',2.5,2.8,11,2,11.03,'tools');
    for(const x of [1.2,14])box('raised_guard_'+x,[x,10.2,3],[x+.8,12.1,14.4],'edge');
    box('front_service_cover',[4,3.6,7.6],[12,8.3,8.1],'metal');
  }
  if(kind==='ship_door') {
    for(const x of [0,14]){
      box('frame_'+x,[x,0,5],[x+2,16,11],'metal','frame',.18,.12);
      box('guide_'+x,[x+.6,0,4.85],[x+1.4,16,5.15],'dark','frame',.1,.05);
      panel('frame_indicator_'+x,[x+.7,6.5,4.81],[x+1.3,9.5,4.81],'cyan','frame');
    }
    for(const [name,x,X] of [['left',2,7.9],['right',8.1,14]]) {
      box(name+'_sliding_panel',[x,0,6.4],[X,16,9.6],'edge',name+'_door',.25,.18);
      box(name+'_raised_panel',[x+.55,1.1,6.0],[X-.55,14.9,6.5],'dark',name+'_door',.65,.15);
      panel(name+'_hatch_marking',[x+.8,4.4,5.97],[X-.8,11.6,5.97],'hatch',name+'_door');
      box(name+'_rear_panel',[x+.55,1.1,9.5],[X-.55,14.9,9.85],'dark',name+'_door',.65,.12);
    }
    box('central_latch',[7.2,6.8,5.65],[8.8,9.2,6.4],'metal','right_door',.22,.1);
  }
  if(kind==='ship_engine_unit') {
    base();box('ignition_housing',[1.5,2.2,4.8],[14.5,14.9,14.5],'dark', 'structure',.85,.22);
    box('reactor_front',[2.4,3,4.5],[13.6,14.1,5.2],'metal');
    ring('containment_ring',[8,8.7,2.55],4.4,3.15,1.2,'edge');
    ring('inner_power_bus',[8,8.7,3.1],3.16,2.7,.6,'cyan');
    tube('ignition_socket',[8,8.7,3.75],'z',2.7,.6,'recess');
    panel('socket_core',[6.25,7.0,3.71],[9.75,10.4,3.71],'ignition');
    for(const x of [1.6,12.7])box('side_module_'+x,[x,4.1,4.7],[x+1.7,13.4,13.2],'edge');
    for(const y of [5,8.6,12.2])box('bus_strap_'+y,[1.1,y,8.4],[14.9,y+.7,10],'metal');
    box('lower_interface',[4.5,2.4,2.5],[11.5,4.1,4.6],'edge');
    panel('ignition_status',[5.0,2.75,2.47],[11,3.75,2.47],'ignition');
    box('top_access_port',[5,14.9,6],[11,15.95,12],'edge');
    top('access_port_inset',5.7,6.7,4.6,4.6,16,'recess');
  }
  if(kind==='beacon_emitter') {
    box('mast_coupler',[3,0,3],[13,2.5,13],'dark');
    tube('lower_insulator',[8,2.3,8],'y',4.5,2,'edge','structure');
    tube('transmitter_drum',[8,4.3,8],'y',3.3,6.1,'metal','mechanism');
    ring('signal_band',[8,6.6,8],3.38,3.15,1.1,'cyan','mechanism','y');
    tube('antenna_dish_base',[8,10.2,8],'y',3.3,2.3,'dark','mechanism',5.2);
    ring('dish_rim',[8,12.2,8],5.2,4.5,.6,'edge','mechanism','y');
    tube('dish_reflector',[8,12.3,8],'y',4.5,.25,'glass','mechanism');
    tube('feed_stalk',[8,12.5,8],'y',.65,2.3,'edge','mechanism',.65,8);
    tube('feed_cap',[8,14.8,8],'y',1.2,1,'metal','mechanism',.65,8);
  }
  if(!parts.length)throw new Error('Unknown asset '+kind);
  return {kind,parts,groups:[...new Set(parts.map(p=>p.group))],triangles:parts.reduce((sum,p)=>sum+p.faces.reduce((s,f)=>s+f.length-2,0),0)};
}
