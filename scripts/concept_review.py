"""Independent silhouette comparison and local, user-scored V04 review gallery."""
import base64
import hashlib
import html
import json
from pathlib import Path
from blockbench_bridge import evaluate
from concept_rebuild_specs import JOBS, MODELS, OUT


def data(path):return 'data:image/png;base64,'+base64.b64encode(path.read_bytes()).decode()


COMPARE=r'''async function(job){
 const load=async src=>{const im=new Image();im.src=src;await im.decode();return im};
 const [shot,mask,concept]=await Promise.all([load(job.shot),load(job.mask),load(job.concept)]);
 function bounds(im){const c=document.createElement('canvas');c.width=im.width;c.height=im.height;const x=c.getContext('2d');x.drawImage(im,0,0);const p=x.getImageData(0,0,c.width,c.height).data;let l=c.width,t=c.height,r=0,b=0;for(let yy=0;yy<c.height;yy++)for(let xx=0;xx<c.width;xx++)if(p[(yy*c.width+xx)*4+3]>127){l=Math.min(l,xx);r=Math.max(r,xx+1);t=Math.min(t,yy);b=Math.max(b,yy+1)}return [l,t,r,b]}
 const mb=bounds(shot),rb=bounds(mask),rw=rb[2]-rb[0],rh=rb[3]-rb[1],mw=mb[2]-mb[0],mh=mb[3]-mb[1];
 const factor=job.armor?rh/mh:rw/mw,tw=mw*factor,th=mh*factor;
 const w=Math.ceil(Math.max(rw,tw)+24),h=Math.ceil(Math.max(rh,th)+24);
 const make=()=>{const c=document.createElement('canvas');c.width=w;c.height=h;return c};
 const reference=make(),model=make(),rctx=reference.getContext('2d'),mctx=model.getContext('2d');
 rctx.drawImage(mask,...[rb[0],rb[1],rw,rh],(w-rw)/2,(h-rh)/2,rw,rh);
 mctx.drawImage(shot,...[mb[0],mb[1],mw,mh],(w-tw)/2,(h-th)/2,tw,th);
 const a=rctx.getImageData(0,0,w,h).data,b=mctx.getImageData(0,0,w,h).data;
 let intersection=0,union=0;const diff=make(),dctx=diff.getContext('2d'),pixels=dctx.createImageData(w,h);
 for(let i=0;i<w*h;i++){const r=a[i*4+3]>127,m=b[i*4+3]>127;if(r||m)union++;if(r&&m)intersection++;if(r||m){const k=i*4;pixels.data[k]=r&&!m?255:40;pixels.data[k+1]=r&&m?200:m?100:70;pixels.data[k+2]=m&&!r?255:80;pixels.data[k+3]=255}}
 dctx.putImageData(pixels,0,0);
 const board=document.createElement('canvas');board.width=1600;board.height=1000;
 const ctx=board.getContext('2d');ctx.fillStyle='#101e2b';ctx.fillRect(0,0,1600,1000);ctx.fillStyle='#e1eaf4';ctx.font='24px sans-serif';ctx.fillText(job.title+' / '+job.view,30,42);
 ctx.font='18px sans-serif';ctx.fillText('原始概念图',30,80);ctx.fillText('Blockbench 原生截图',560,80);ctx.fillText('轮廓差异：绿=重合 红=参考独有 蓝=模型独有',1090,80);
 function draw(im,box,x){const [l,t,r,b]=box,ww=r-l,hh=b-t,k=Math.min(470/ww,790/hh);ctx.drawImage(im,l,t,ww,hh,x+(490-ww*k)/2,110+(790-hh*k)/2,ww*k,hh*k)}
 const [lx,ty]=job.region;
 draw(concept,[rb[0]+lx,rb[1]+ty,rb[2]+lx,rb[3]+ty],20);draw(shot,mb,550);draw(diff,[0,0,w,h],1080);
 ctx.fillStyle='#c4d4e5';ctx.fillText('轮廓 IoU '+(intersection/union*100).toFixed(2)+'% · '+(job.armor?'统一高度':'统一长度')+'，保留宽高比例；不代表综合匹配度。',30,953);
 return {iou:intersection/union,intersection,union,model_bounds:mb,reference_bounds:rb,alignment:job.armor?'uniform scale by height + center':'uniform scale by length + center',
  png:board.toDataURL(),difference:diff.toDataURL()};
}'''

NOTES={
 'tungsten_sabre':['直护手、黑色缠柄、双琥珀铆钉、单侧切角刀尖。','检查刀刃宽厚比和刀尖方向；端面使用重新绘制的金属色块。'],
 'coil_pistol':['双铜色线圈、上下承力框、独立电池、倾斜握柄、矩形凹入枪口。','检查电池悬挂位置、线圈间距和枪口端面；原概念未提供反侧专图，反侧采用镜像结构。'],
 'durasteel_glaive':['分段蓝色长柄、青绿箍灯、双反钩、宽弧刃与刀根夹箍。','检查刃柄长度比例、反钩弧度及刀头厚度。'],
 'plasma_carbine':['开放枪托、侧面绿色能量仓、三条前散热窗、扳机护圈和瞄具。','检查枪托开口、能量仓凹入深度与前护木厚度。'],
 'starcore_blade':['双向护手、菱形星核、银色核心夹爪、紫蓝长槽及前端切角。','检查核心凸出层次、刀尖上缘和长槽比例。'],
 'starcore_railgun':['镂空三角枪托、厚机匣、多层紫核、分离双加速轨与双枪口。','检查轨道与炮身比例、轨间空隙、前卡箍和枪口凹入深度；此静态审查稿不展示蓄力动画。'],
 'pioneer':['整块琥珀面罩与头灯、工作胸板、双工具袋、方膝甲及工作靴。','前后姿态在原概念中略有差别，重点检查背板宽度与肩甲位置；黑色部分为穿戴展示内衬。'],
 'vanguard':['T形面框、铜色头冠、分体厚胸甲、腹部叠片和背部分节脊。','检查肩甲厚度、后背双板间距和护膝位置。'],
 'ranger':['绿色横目镜、不对称轻肩甲、青绿胸部护甲、银扣板和轻型护膝。','检查两肩差异、后背分节结构和头盔顶部过渡。'],
 'starwarden':['银框蓝目镜、胸口菱形星核、蓝色腹部光条、阶梯髋甲和长护胫。','检查背部纵向光脊、侧面头盔银边和髋甲；原图的轻微透视不能与严格正交截图完全重合。'],
}


def make_gallery(reports):
    cards=[]
    for j in JOBS:
        ident=j['id']; receipt=json.loads((OUT/ident/'receipt.json').read_text(encoding='utf-8'))
        views={'front':'正视' if j['side'] else '侧视（主参考）','side':'侧视' if j['side'] else '端视','back':'背视' if j['side'] else '反侧视','top':'顶视','perspective':'透视'}
        if j['side']:views.update(reference_side='侧面参考角度（非三视图）',reference_back='背面参考角度（非三视图）')
        buttons=''.join(f'<button onclick="show(\'{ident}\',\'{v}\')">{label}</button>' for v,label in views.items())
        comparisons=''.join(f'<a href="{ident}/{v}-comparison.png" target="_blank">{views[v]}概念对照 · 轮廓 {m["iou"]*100:.2f}%</a>' for v,m in reports[ident].items())
        fields=''.join(f'<label>{label} / {maximum}<input type="number" min="0" max="{maximum}" step="0.1" data-score="{key}" oninput="save(\'{ident}\')"></label>' for key,label,maximum in [('shape','轮廓与比例',40),('structure','部件结构',30),('color','配色',15),('detail','细节',15)])
        cards.append(f'''<section id="{ident}"><h2>{html.escape(j['title'])}</h2><p>{NOTES[ident][0]}</p><div class="views">{buttons}</div>
        <div class="pair"><a href="{ident}/concept.png" target="_blank"><img src="{ident}/concept.png" alt="原始概念图"></a><a id="link-{ident}" href="{ident}/front-editor.png" target="_blank"><img id="shot-{ident}" src="{ident}/front-editor.png" alt="真实 Blockbench 编辑器截图"></a></div>
        <div class="links">{comparisons}<a href="../../模组位置/dev-assets/concept-rebuild-v04/{ident}.bbmodel">下载/打开可编辑工程</a><a id="clean-{ident}" href="{ident}/front.png" target="_blank">纯模型截图</a></div>
        <p class="note">{NOTES[ident][1]}</p><p class="meta">原生重开验证：{receipt['cubes']} 个方块 + {receipt['meshes']} 个闭合网格；{receipt['groups']} 个部件组。</p>
        <div class="scores">{fields}<strong id="total-{ident}">待你评分</strong></div><textarea placeholder="审查意见、需修改的具体部位" oninput="save('{ident}')"></textarea></section>''')
    page='''<!doctype html><html lang="zh-CN"><meta charset="utf-8"><meta name="viewport" content="width=device-width"><title>V04 · 从零建模审查</title>
<style>*{box-sizing:border-box}body{margin:0;background:#0e1822;color:#e6eef8;font:16px/1.65 system-ui,"Microsoft YaHei",sans-serif}main{max-width:1560px;margin:auto;padding:30px}h1{font-size:32px}h2{margin:0;font-size:25px}section{margin:24px 0;padding:24px;background:#172637;border:1px solid #334b63;border-radius:12px}.pair{display:grid;grid-template-columns:1fr 1fr;gap:12px}.pair img{width:100%;height:430px;object-fit:contain;background:#0b1520}a{color:#80d8ff;text-decoration:none}.views,.links{display:flex;flex-wrap:wrap;gap:14px;margin:14px 0}button{background:#27455f;border:1px solid #5884aa;color:white;padding:9px 16px;border-radius:6px;cursor:pointer}.note{color:#e5cf9e}.meta{font-size:14px;color:#aabed2}.scores{display:flex;gap:20px;align-items:end;flex-wrap:wrap}.scores label{display:grid;gap:5px}input,textarea{background:#0d1a29;color:white;border:1px solid #54718e;padding:10px;border-radius:5px}input{width:140px}textarea{width:100%;height:75px;margin-top:15px}.status{padding:16px;border-left:4px solid #ecbc67;background:#233145}nav{display:flex;gap:18px;flex-wrap:wrap;position:sticky;top:0;background:#101e2bf0;padding:14px;z-index:2}.toolbar{margin:20px 0}@media(max-width:900px){.pair{grid-template-columns:1fr}.pair img{height:auto}}</style>
<main><h1>V04 · 武器与防具从零建模</h1><p>6 把武器 · 4 套防具 · Blockbench 5.2.2 原生空工程制作</p>
<div class="status"><b>当前状态：等待你的综合审查，尚未确认达到 95%。</b><br>验收权重：轮廓与比例 40%，部件结构 30%，配色 15%，细节 15%。每件需达到 95 分。下面的轮廓 IoU 是独立的二维检查，不等于综合评分，也不会自动填入评分表。</div>
<p>点击角度切换真实编辑器截图；点击“概念对照”查看原图、原生模型截图和轮廓差异。原图部分视角带轻微透视；轮廓提取使用背景色规则，局部阴影可能影响指标。武器没有完整反侧设计，反侧按对称结构补建。</p>
<p>本轮所有几何从空工程重建。贴图来自原概念的投影采样，含原图绘制光照；斜面为可编辑闭合网格。此为美术审查工程，保留概念人体比例，尚未改成游戏骨骼/生产模型，也没有替换已安装模组。</p>
<div class="toolbar"><button onclick="exportReview()">导出我的评分与意见 JSON</button> <span id="storage">评分自动保存在当前浏览器；交付时请导出。</span></div><nav>'''+''.join(f'<a href="#{j["id"]}">{j["title"]}</a>' for j in JOBS)+'</nav>'+''.join(cards)+'''
<script>
const ids=IDS;
function show(id,view){document.getElementById('shot-'+id).src=id+'/'+view+'-editor.png';document.getElementById('link-'+id).href=id+'/'+view+'-editor.png';document.getElementById('clean-'+id).href=id+'/'+view+'.png'}
function record(id){const s=document.getElementById(id),scores={};s.querySelectorAll('input').forEach(i=>scores[i.dataset.score]=i.value===''?null:Number(i.value));return {scores,note:s.querySelector('textarea').value}}
function refresh(id){const r=record(id),values=Object.values(r.scores),valid=[...document.getElementById(id).querySelectorAll('input')].every(i=>i.value!==''&&i.checkValidity()),total=valid?values.reduce((a,b)=>a+b,0):null;document.getElementById('total-'+id).textContent=total===null?'待填写有效评分':total.toFixed(1)+' / 100 · '+(total>=95?'达到验收分数':'需要继续修改');return {...r,total,accepted:total!==null&&total>=95}}
function save(id){refresh(id);try{localStorage.setItem('starbound-v04-'+id,JSON.stringify(record(id)))}catch(e){document.getElementById('storage').textContent='浏览器禁用本地存储，请导出评分 JSON 保存。'}}
for(const id of ids){try{const r=JSON.parse(localStorage.getItem('starbound-v04-'+id)||'null');if(r){const s=document.getElementById(id);s.querySelectorAll('input').forEach(i=>i.value=r.scores[i.dataset.score]??'');s.querySelector('textarea').value=r.note||'';refresh(id)}}catch(e){}}
function exportReview(){const review={version:'V04',threshold:95,reviewer:'user',created:new Date().toISOString(),equipment:Object.fromEntries(ids.map(id=>[id,refresh(id)]))};const a=document.createElement('a');a.href=URL.createObjectURL(new Blob([JSON.stringify(review,null,2)],{type:'application/json'}));a.download='V04-我的审查意见.json';a.click();setTimeout(()=>URL.revokeObjectURL(a.href),1000)}
</script></main></html>'''
    page=page.replace('const ids=IDS;','const ids='+json.dumps([j['id'] for j in JOBS])+';')
    delivery=MODELS/'delivery.json'
    if delivery.exists() and json.loads(delivery.read_text(encoding='utf-8')).get('accepted'):
        page=page.replace('当前状态：等待你的综合审查，尚未确认达到 95%。','当前状态：用户已确认美术验收通过（2026-10-26）。')
        page=page.replace('每件需达到 95 分。下面的轮廓 IoU','用户原文“通过”；未提供具体分数，评分栏不补填。下面的轮廓 IoU')
    (OUT/'index.html').write_text(page,encoding='utf-8')


if __name__=='__main__':
    reference=json.loads((MODELS/'reference-bounds.json').read_text(encoding='utf-8'));reports={}
    for j in JOBS:
        ident=j['id'];reports[ident]={}
        for view,ref in reference[ident].items():
            payload=dict(title=j['title'],view=view,armor=bool(j['side']),region=ref['region'],shot=data(OUT/ident/(view+'.png')),mask=data(MODELS/ref['mask']),concept=data(Path(j['concept'])))
            result=evaluate('('+COMPARE+')('+json.dumps(payload)+')')
            for key,suffix in [('png','comparison'),('difference','difference')]:
                (OUT/ident/(view+'-'+suffix+'.png')).write_bytes(base64.b64decode(result.pop(key).split(',',1)[1]))
            reports[ident][view]=result
        print(ident,{v:round(m['iou']*100,2) for v,m in reports[ident].items()},flush=True)
    (OUT/'轮廓检查.json').write_text(json.dumps(reports,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    (MODELS/'silhouette-review.json').write_text(json.dumps(reports,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    make_gallery(reports)
