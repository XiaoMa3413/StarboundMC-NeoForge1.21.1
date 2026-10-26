/* Executed INSIDE Blockbench 5.2.2 by blockbench_bridge.py.
 * Uses native Cube edits, Undo transactions, project codec and Java item codec.
 * Normal resource builds read the saved bbmodels; they never rerun these edits.
 */
window.SBEquipment = async function(job) {
    Codecs.project.load(job.model, {path: job.id + '.bbmodel', no_file: true});
    Project.name = job.id;
    await new Promise(resolve => setTimeout(resolve, 200));
    const armor = job.id.endsWith('_outer') || job.id.endsWith('_inner');
    if (!armor) {
        Project.java_block_version = '1.9.0'; // Native editor option: MC 1.9 through 1.21.5.
        for (const [slot, data] of Object.entries(job.display || {}))
            Project.display_settings[slot] = new DisplaySlot(slot).extend(data);
    }
    const texture = Texture.all[0];
    texture.name = armor ? job.id.split('_')[0] + '_layer_' + (job.id.endsWith('_inner') ? 2 : 1) + '.png' : job.id + '_model.png';
    texture.folder = armor ? 'models/armor' : 'item'; texture.namespace = 'starboundmc'; texture.id = '0';
    const operations = [];
    const materialOf = cube => {
        const stored = cube.name.match(/\[m(\d+)\]$/);
        if (stored) return +stored[1];
        const uv = cube.faces.north.uv;
        const tile = armor ? 64 : 16;
        return job.materials?.[cube.uuid] ?? (Math.floor(Math.min(uv[0], uv[2]) / tile) + Math.floor(Math.min(uv[1], uv[3]) / tile) * 4);
    };
    for (const cube of Cube.all) {
        const m = materialOf(cube);
        cube.name = cube.name.replace(/\s*\[m\d+\]$/, '') + ` [m${m}]`;
    }
    Undo.initEdit({elements: Cube.all.slice(), outliner: true, textures: Texture.all});
    const remove = regex => {
        for (const cube of Cube.all.slice()) if (regex.test(cube.name)) cube.remove();
    };
    const group = name => Group.all.find(g => g.name === name);
    function box(name, from, to, m, parent = group('root'), rotation, origin) {
        const tile = armor ? 64 : 16;
        const u = m % 4 * tile, v = Math.floor(m / 4) * tile;
        const faces = Object.fromEntries(['north','south','east','west','up','down'].map(f => [f, {
            uv: [u + .5, v + .5, u + tile - .5, v + tile - .5], texture: texture.uuid
        }]));
        const cube = new Cube({name: `${name} [m${m}]`, from, to, faces, autouv: 0,
            rotation: rotation || [0,0,0], origin: origin || [0,0,0]});
        cube.addTo(parent).init();
        return cube;
    }
    function surface(name, a, b, m) {
        box(name, a, b, m);
        box(name + '_reverse', [a[0],a[1],16-b[2]], [b[0],b[1],16-a[2]], m);
    }
    function ab(part, name, a, size, m, angle=0) {
        const parent = group(part), p = parent.origin;
        const from = [a[0]+p[0],p[1]-a[1]-size[1],a[2]];
        const to = [from[0]+size[0],from[1]+size[1],from[2]+size[2]];
        const origin = [(from[0]+to[0])/2,(from[1]+to[1])/2,a[2]];
        const cube = box(name,from,to,m,parent,[0,0,-angle],origin);
        cube.box_uv = true; cube.uv_offset = [m%4*64,Math.floor(m/4)*64];
        return cube;
    }
    function frame(part, name, a, w, h, z, rim, m) {
        ab(part,name+'_left',a,[rim,h,z],m);
        ab(part,name+'_right',[a[0]+w-rim,a[1],a[2]],[rim,h,z],m);
        ab(part,name+'_top',[a[0]+rim,a[1],a[2]],[w-2*rim,rim,z],m);
        ab(part,name+'_bottom',[a[0]+rim,a[1]+h-rim,a[2]],[w-2*rim,rim,z],m);
    }
    if (!armor) {
        if (job.id === 'tungsten_sabre') {
            remove(/tungsten_blade|silver_edge|clipped_tip|beveled_spine|blade_fuller/);
            box('forged_blade_body',[6.65,4.8,7.55],[9.25,20.8,8.45],3);
            for (let y=5; y<21; y+=2) surface('forged_blade_facet',[7.15,y,7.4],[8.9,y+1.7,7.55],y%4===1?2:3);
            box('blade_edge',[6.3,5,7.7],[6.65,21.4,8.3],4);
            for(let i=0;i<5;i++) box('stepped_clip_tip',[6.3,20.8+i*.3,7.6],[9.25-i*.5,21.1+i*.3,8.4],3);
            surface('blade_root_hallmark',[7,5.2,7.15],[7.7,5.85,7.4],6);
            operations.push('Rebuilt slim forged blade, continuous edge and five-step clipped tip.');
        } else if (job.id === 'durasteel_glaive') {
            remove(/blade_root|blade_face|outer_edge|head_tip|blade_recess|blade_inset|split_edge|teal_channel|short_rear_hook|hook_tip|head_rivet/);
            for(let i=0;i<18;i++) {
                let x=8.8+i*.5, low=21.4+4*Math.pow(i/17,1.6), high=29.2+2.7*i/17;
                box('curved_blade_body',[x,low+.55,7.25],[x+.5,high,8.75],2);
                box('curved_cutting_edge',[x,low,7.55],[x+.5,low+.55,8.45],4);
                if(i<16) surface('blade_inset_panel',[x,low+1.2,7.05],[x+.5,high-.7,7.25],3);
            }
            surface('continuous_teal_channel',[9,26.1,6.85],[16.8,26.55,7.05],9);
            surface('head_anchor',[10,27.2,6.85],[10.55,27.75,7.05],2);
            surface('head_anchor',[16,29.1,6.85],[16.55,29.65,7.05],2);
            for(let i=0;i<7;i++) box('rear_hook_curve',[4+i*.5,26.5+i*.45,7.35],[4.6+i*.5,27.3+i*.45,8.65],3);
            for(let y=-6;y<19;y+=4) {
                surface('blue_shaft_wrap',[7.3,y,7.1],[8.7,y+2.4,7.4],15);
                box('shaft_wrap_end',[7.15,y+2.4,7.15],[8.85,y+2.7,8.85],2);
            }
            operations.push('Rebuilt crescent edge from 18 narrow steps, swept rear hook and segmented blue shaft.');
        } else if (job.id === 'starcore_blade') {
            remove(/starcore_blade|silver_edge|silver_spine|tip_step|clipped_tip|blade_facet|spine_facet|channel_bridge|edge_bevel/);
            for(let i=0;i<16;i++) {
                const x=4+i*.5, top=27.1-Math.max(0,i-5)*.42;
                box('tapered_starsteel_body',[x,6.5,7.05],[x+.5,top-.5,8.95],11);
                box('stepped_silver_tip',[x,top-.5,7.4],[x+.5,top,8.6],4);
            }
            box('long_cutting_edge',[3.65,7,7.6],[4,26.6,8.4],4);
            surface('raised_blade_spine',[10.5,7.5,6.8],[11.6,22.5,7.05],12);
            surface('lower_blade_bevel',[4.15,7.5,6.8],[5.25,24.6,7.05],2);
            for(let y=9;y<23;y+=2) surface('channel_energy_segment',[7.45,y,6.6],[8.55,y+1.65,6.8],13);
            for(const x of [5.5,9.9]) surface('core_mount_claw',[x,4.8,5.65],[x+.6,7.5,6.2],3);
            for(let i=0;i<3;i++) surface('root_stepped_chevron',[4.8+i*.65,7.4+i*.8,6.6],[5.5+i*.65,8.2+i*.8,7],3);
            operations.push('Rebuilt tapering blade and silver clip tip; layered spine, segmented light channel and core claws.');
        } else {
            // Replace the flat receiver by layered load-bearing shells.
            const isRail=job.id==='starcore_railgun', isPistol=job.id==='coil_pistol';
            remove(/^receiver \[|receiver_top|square_muzzle|muzzle_port|muzzle_cheek|muzzle_top|emitter_inset|emitter_core/);
            box('upper_receiver_shell',[1.2,10.8,5.65],[11.2,12.4,10.35],isRail?2:3);
            box('lower_receiver_shell',[1.2,7,5.8],[10.8,8.1,10.2],isRail?2:3);
            box('receiver_inner_chassis',[1.5,8.1,6.1],[10.5,10.8,9.9],isRail?11:2);
            surface('beveled_upper_panel',[2,10.25,5.4],[10.5,11,5.75],isRail?3:4);
            surface('beveled_lower_panel',[5.2,7.5,5.45],[10.3,8.3,5.8],isRail?3:3);
            if(isPistol) {
                for(const z of [5.9,9.6]) box('hollow_muzzle_side',[21.4,7.9,z],[23,12,z+.5],3);
                for(const y of [7.9,11.5]) box('hollow_muzzle_cap',[21.4,y,6.4],[23,y+.5,9.6],3);
                box('recessed_bore',[21.1,8.5,6.9],[21.3,11.4,9.1],0);
                surface('battery_frame_left',[7.1,3.6,6.35],[7.65,6.8,6.8],2);
                surface('battery_frame_right',[8.65,3.6,6.35],[9.2,6.8,6.8],2);
                for(let y=4;y<6.6;y+=.65) surface('battery_gauge',[7.8,y,6.3],[8.5,y+.4,6.55],10);
                operations.push('Hollow four-wall muzzle, layered ivory receiver and framed segmented energy cell.');
            } else if(!isRail) {
                remove(/plasma_window|plasma_core|vent \[/);
                surface('chamber_dark_recess',[7.3,8.25,5.3],[15,10.85,5.75],0);
                surface('plasma_chamber_glass',[7.8,8.65,5.1],[14.5,10.45,5.35],8);
                surface('plasma_hot_center',[9.1,9.6,4.95],[13.5,10.2,5.15],9);
                for(let x=7.3;x<15.1;x+=7.2) surface('chamber_frame',[x,8.1,5.05],[x+.5,11.1,5.6],3);
                for(let x=17;x<22;x+=1.5) surface('forward_energy_vent',[x,9,5.25],[x+.55,10.7,5.65],9);
                for(const z of [6,9.5]) box('square_emitter_cheek',[23,8,z],[25,11.4,z+.5],2);
                for(const y of [8,10.9]) box('square_emitter_cap',[23,y,6.5],[25,y+.5,9.5],2);
                operations.push('Inset framed plasma glass, three luminous forward vents and hollow square emitter.');
            } else {
                remove(/focusing_aperture|rail_end/);
                for(const y of [7,11]) {
                    for(const z of [5.2,10.3]) box('rail_aperture_side',[27.7,y-.2,z],[29.7,y+2.35,z+.5],3);
                    for(const yy of [y-.2,y+1.85]) box('rail_aperture_cap',[27.7,yy,5.7],[29.7,yy+.5,10.3],3);
                    box('rail_inner_aperture',[27.1,y+.4,6.4],[27.4,y+1.5,9.6],12);
                    box('rail_aperture_glow',[27.4,y+.7,7.3],[27.6,y+1.2,8.7],14);
                    for(let x=12;x<27;x+=2.2) surface('segmented_accelerator',[x,y+.65,5.1],[x+1.8,y+1.1,5.45],13);
                }
                surface('core_chamber_outer_frame',[4.5,8,4.4],[10.2,12.2,4.9],2);
                surface('core_chamber_violet_glass',[5,8.5,4.15],[9.7,11.7,4.45],12);
                surface('core_chamber_bright_cell',[5.8,9.2,3.95],[8.9,11,4.2],13);
                surface('core_chamber_center',[6.5,9.7,3.8],[8.2,10.5,4],14);
                for(let i=0;i<6;i++) {
                    let x=-6.4+i*.95,y=5+i*.55;
                    box('triangular_stock_brace',[x,y,6.2],[x+1.2,y+.75,9.8],2);
                }
                operations.push('Twin hollow focusing mouths, segmented accelerator strips, deep layered core glazing and triangular stock brace.');
            }
        }
    } else {
        const profile=job.id.split('_')[0], inner=job.id.endsWith('_inner');
        if(!inner) {
            // Original concepts use compact framed helmets, not tall fins or industrial goggles.
            for(const cube of Cube.all.slice()) if(cube.parent?.name==='head') cube.remove();
            const star=profile==='starwarden', heavy=profile==='vanguard', miner=profile==='pioneer';
            ab('head','compact_helmet_shell',[-4.35,-8.35,-4.3],[8.7,8.7,8.6],2);
            ab('head','dark_face_recess',[-3.85,-6.1,-4.55],[7.7,5.2,.4],0);
            frame('head','helmet_silver_face_frame',[-4.35,-6.7,-4.9],8.7,6.9,.6,.65,3);
            ab('head','crown_center_plate',[-2.6,-8.8,-3.8],[5.2,.5,7.2],3);
            for(const x of [-4.65,4.2]) {
                ab('head','ear_sensor_housing',[x,-5.5,-1.8],[.45,3.2,3.4],3);
                ab('head','ear_sensor_light',[x-.05,-4.9,-1],[.55,1.7,1],6);
            }
            frame('head','rear_service_frame',[-2.6,-7,4.3],5.2,3.7,.4,.5,3);
            ab('head','rear_service_inset',[-2.1,-6.5,4.7],[4.2,2.7,.2],2);
            if(miner) {
                ab('head','amber_full_face_glass',[-3.65,-5.5,-4.75],[7.3,3.5,.25],5);
                for(const x of [-3.3,2.8]) ab('head','visor_amber_edge',[x,-5.3,-4.85],[.45,3.1,.15],6);
                frame('head','headlamp_frame',[-1.45,-8.7,-5.05],2.9,2.8,.7,.45,1);
                ab('head','amber_headlamp',[-1,-8.25,-5.3],[2,1.9,.3],6);
                ab('head','headlamp_center',[-.55,-7.8,-5.45],[1.1,1,.2],7);
                remove(/utility_harness|work_bib|bib_pocket|harness_clasp/);
                ab('body','work_breastplate',[-3.8,2,-2.8],[7.6,7.3,.75],2);
                frame('body','work_breastplate_rim',[-4,1.7,-3.05],8,8,.3,.45,3);
                ab('body','amber_chest_band',[-3.35,3.6,-3.45],[6.7,.6,.25],6);
                for(const x of [-3.15,.55]) {
                    frame('body','tool_pocket_frame',[x,5.2,-3.75],2.6,3.1,.6,.35,1);
                    ab('body','tool_pocket_face',[x+.35,5.55,-3.9],[1.9,2.4,.25],2);
                    ab('body','tool_pocket_clasp',[x+.65,5.95,-4.1],[1.2,.4,.2],6);
                }
                for(const arm of ['right_arm','left_arm']) {
                    let x=arm==='right_arm'?-3.5:-1.5;
                    ab(arm,'segmented_work_pauldron',[x,-2.8,-2.8],[5,2.2,5.6],3);
                    ab(arm,'work_shoulder_face',[x+.4,-2.4,-3],[4.2,2.8,.4],2);
                }
            } else if(heavy) {
                // Rebuild the T visor surround from a crown bridge and two inward cheek steps.
                remove(/helmet_silver_face_frame/);
                ab('head','t_visor_crown',[-4.4,-6.5,-4.95],[8.8,.7,.6],3);
                for(const side of [-1,1]) {
                    const x=side<0?-4.4:3.7;
                    ab('head','t_visor_outer_cheek',[x,-5.8,-4.95],[.7,5.7,.6],3);
                    ab('head','t_visor_inward_cheek',[side<0?-3.7:1,-4.5,-5.05],[2.7,4.4,.6],3);
                    ab('head','cheek_dark_inset',[side<0?-3.1:1.4,-3.9,-5.2],[1.7,3.1,.2],2);
                }
                ab('head','orange_crown_rank',[-.7,-8.9,-4.5],[1.4,.6,7.9],6);
                remove(/siege_pauldron|pauldron_edge|heavy_chest_block|chest_dark_inset|recessed_fastener|abdomen_lamella/);
                for(const x of [-4.3,.4]) {
                    ab('body','concept_breastplate',[x,1.2,-3.45],[3.9,4.4,1.1],2);
                    frame('body','breastplate_silver_rim',[x,1.2,-3.7],3.9,4.4,.35,.4,3);
                    ab('body','breastplate_orange_rank',[x+.7,1.8,-4],[2.5,.5,.25],6);
                }
                for(const y of [6,8.3]) ab('body','compact_abdominal_plate',[-2.5,y,-3.2],[5,1.7,.8],3);
                for(const arm of ['right_arm','left_arm']) {
                    const c=arm==='right_arm'?-1:1;
                    ab(arm,'concept_heavy_pauldron',[c-2.9,-3,-2.9],[5.8,3,5.8],2);
                    ab(arm,'pauldron_top_rim',[c-3,-3.15,-3],[6,.55,6],3);
                    ab(arm,'pauldron_lower_lip',[c-2.75,-.1,-2.95],[5.5,.55,5.9],3);
                }
            } else if(star) {
                ab('head','iceblue_horizontal_visor',[-3.65,-4.85,-5.15],[7.3,.7,.35],7);
                for(let i=0;i<3;i++) for(const side of [-1,1]) {
                    ab('head','stepped_silver_cheek',[side<0?-3.8+i*.7:3.1-i*.7,-3.7+i*.8,-5],[.7,.9,.5],4);
                }
                ab('head','compact_crown_keystone',[-.8,-8.9,-4.8],[1.6,2,.6],5);
                ab('head','crown_keystone_light',[-.3,-8.65,-5],[.6,1.5,.25],7);
                remove(/raised_wing|lower_wing|pauldron_energy|swept_breastplate|split_hip_tasset/);
                for(const x of [-4.1,.3]) {
                    ab('body','starsteel_chest_lobe',[x,1,-3],[3.8,4.3,.9],2);
                    frame('body','silver_chest_lobe_frame',[x,1,-3.3],3.8,4.3,.4,.4,4);
                }
                for(const side of [-1,1]) {
                    for(let i=0;i<4;i++) ab('body','stepped_hip_tasset',[side<0?-4.6+i*.4:2.1-i*.4,9.8+i*.7,-3],[2.5,.9,.8],2);
                    for(let i=0;i<4;i++) ab('body','tasset_silver_edge',[side<0?-4.8+i*.4:4.2-i*.4,9.8+i*.7,-3.25],[.55,.95,.35],4);
                }
                for(const arm of ['right_arm','left_arm']) {
                    const c=arm==='right_arm'?-1:1;
                    ab(arm,'compact_starwarden_pauldron',[c-2.7,-2.9,-2.8],[5.4,3,5.6],2);
                    frame(arm,'starwarden_pauldron_rim',[c-2.8,-3,-3],5.6,3.6,.4,.5,4);
                    frame(arm,'silver_bracer_frame',[c-2.3,5,-3.2],4.6,4.6,.45,.45,3);
                    ab(arm,'violet_bracer_face',[c-1.85,5.45,-3.4],[3.7,3.7,.3],2);
                    ab(arm,'bracer_luminous_strip',[c-.3,5.9,-3.6],[.6,2.8,.25],7);
                }
            } else {
                ab('head','green_sensor_visor',[-3.7,-4.9,-5.1],[7.4,.65,.3],6);
                for(const x of [-3.6,2.7]) ab('head','compact_jaw_plate',[x,-2.5,-4.95],[.9,2.2,.5],3);
                remove(/short_antenna|antenna_tip|diagonal_sling|sling_buckle|sternum_readout|light_sternum_plate/);
                for(const x of [-3.3,1.8]) {
                    ab('body','teal_light_chest_lobe',[x,1.4,-2.8],[1.5,4.8,.7],2);
                    ab('body','chest_lobe_silver_lip',[x-.15,1.2,-3],[1.8,.55,.4],3);
                }
                ab('body','ranger_sternum_bridge',[-2,3.4,-3],[4,1.2,.5],2);
                ab('body','ranger_sternum_signal',[-1.1,3.6,-3.55],[2.2,.4,.2],6);
                for(const y of [5.5,7.8]) ab('body','floating_abdominal_segment',[-1.6,y,-2.9],[3.2,1.5,.6],3);
                for(const arm of ['right_arm','left_arm']) {
                    const c=arm==='right_arm'?-1:1;
                    ab(arm,'light_shoulder_carrier',[c-2.4,-2.6,-2.65],[4.8,2.8,5.3],2);
                    ab(arm,'shoulder_signal',[c-1.4,-1,-2.9],[2.8,.35,.3],6);
                }
            }
            operations.push('Rebuilt compact concept helmet, silver face framing and profile-specific chest/shoulders.');
        } else {
            for(const leg of ['right_leg','left_leg']) {
                const c=profile==='starwarden'?2:profile==='pioneer'?2:3;
                frame(leg,'concept_knee_rim',[-1.75,5.2,-3.25],3.5,2.6,.5,.4,c);
                ab(leg,'concept_knee_inset',[-1.25,5.65,-3.5],[2.5,1.7,.3],2);
                if(profile==='vanguard') ab(leg,'vertical_thigh_rank',[-.35,1.3,-3.4],[.7,2.8,.25],6);
            }
            operations.push('Framed articulated knee plates and concept-specific thigh marks.');
        }
        for(const cube of Cube.all) {
            const m=materialOf(cube);
            cube.box_uv=true; cube.uv_offset=[m%4*64,Math.floor(m/4)*64];
        }
    }
    if (!armor) {
        // Continuous planar mapping: narrow curved-edge strips share one material surface.
        // Repeating the entire tile on every strip would create artificial vertical banding.
        for (const cube of Cube.all) {
            const m=materialOf(cube),u=m%4*16,v=Math.floor(m/4)*16,a=cube.from,b=cube.to;
            const U=x=>u+4+x*.3,V=y=>v+11-y*.3;
            cube.faces.north.uv=[U(a[0]),V(b[1]),U(b[0]),V(a[1])];
            cube.faces.south.uv=[U(a[0]),V(b[1]),U(b[0]),V(a[1])];
            for(const f of ['east','west']) cube.faces[f].uv=[U(a[2]),V(b[1]),U(b[2]),V(a[1])];
            for(const f of ['up','down']) cube.faces[f].uv=[U(a[0]),U(a[2])-u+v,U(b[0]),U(b[2])-u+v];
        }
    }
    // Paint restrained, crisp pixel patches inside Blockbench's texture canvas.
    const canvas=document.createElement('canvas'); canvas.width=texture.img.naturalWidth; canvas.height=texture.img.naturalHeight;
    const ctx=canvas.getContext('2d');ctx.drawImage(texture.img,0,0);
    const pixels=ctx.getImageData(0,0,canvas.width,canvas.height);
    for(let y=0;y<canvas.height;y++) for(let x=0;x<canvas.width;x++) {
        const i=(y*canvas.width+x)*4, tile=armor?64:16;
        const hash=((Math.floor(x/2)*13+Math.floor(y/3)*7)%11)-5;
        const delta=hash*.9;
        for(let k=0;k<3;k++) pixels.data[i+k]=Math.max(0,Math.min(255,pixels.data[i+k]+delta));
    }
    ctx.putImageData(pixels,0,0); texture.fromDataURL(canvas.toDataURL());
    await new Promise(resolve => setTimeout(resolve,150));
    Canvas.updateAll();
    Undo.finishEdit('Model equipment against approved concept: '+job.id);
    const project=Codecs.project.compile({raw:true,bitmaps:true,absolute_paths:false});
    const java=armor?null:Codecs.java_block.compile({raw:true});
    Project.saved=true;
    return {id:job.id,version:Blockbench.version,project,java,texture:texture.getDataURL(),operations,cubes:Cube.all.length};
};
window.SBPreview = async function() {
    Canvas.updateAll();
    for (const cube of Cube.selected.slice()) cube.unselect();
    const vertices=Cube.all.flatMap(c=>c.getGlobalVertexPositions());
    const min=[0,1,2].map(i=>Math.min(...vertices.map(p=>p[i])));
    const max=[0,1,2].map(i=>Math.max(...vertices.map(p=>p[i])));
    const center=min.map((v,i)=>(v+max[i])/2), span=Math.max(...max.map((v,i)=>v-min[i]));
    const preview=Preview.selected;
    preview.resize();
    const zoom=Math.min(preview.width,preview.height)/(40*span*1.4);
    const images={};
    for(const [name,offset] of Object.entries({perspective:[-64,35,-64],front:[0,0,-100],back:[0,0,100],side:[100,0,0]})) {
        preview.loadAnglePreset({position:center.map((v,i)=>v+offset[i]),target:center,projection:'orthographic',zoom});
        await new Promise(resolve=>setTimeout(resolve,80));
        images[name]=await new Promise(resolve=>preview.screenshot({crop:true,width:1200,height:1000},resolve));
    }
    return images;
};
window.SBAssembly = async function(job) {
    Codecs.project.load(job.outer,{path:job.id+'_assembled.bbmodel',no_file:true});
    Project.name=job.id+'_assembled_preview';
    const elements=Object.fromEntries(job.inner.elements.map(c=>[c.uuid,c]));
    const groups=Object.fromEntries(job.inner.groups.map(g=>[g.uuid,g]));
    for(const tree of job.inner.outliner) {
        const parent=Group.all.find(g=>g.name===groups[tree.uuid].name);
        for(const id of tree.children) {
            const data=structuredClone(elements[id]);delete data.uuid;
            const cube=new Cube(data);cube.addTo(parent).init();cube.applyTexture(Texture.all[0],true);
        }
    }
    await new Promise(resolve=>setTimeout(resolve,150));
    Canvas.updateAll();Project.saved=true;
    return {project:Codecs.project.compile({raw:true,bitmaps:true,absolute_paths:false}),images:await SBPreview()};
};
'Blockbench equipment authoring functions installed';
