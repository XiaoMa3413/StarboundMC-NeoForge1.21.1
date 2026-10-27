"""Side-by-side concept and actual native model views; no invented similarity score."""
import base64,json
from pathlib import Path
from blockbench_bridge import evaluate
ROOT=Path(__file__).resolve().parents[1]
GALLERY=ROOT.parent/'概念图/2026-10-27_敌人设计与建模'
BOXES={
 'scrap_skitter':{'front':[680,245,1095,590],'side':[1110,240,1570,590],'back':[1595,240,1980,590]},
 'coil_sentry':{'front':[540,50,995,633],'side':[1025,22,1395,635],'back':[1480,50,1975,635]},
 'breach_guard':{'front':[560,35,1055,735],'side':[1070,30,1325,735],'back':[1348,30,1880,735]},
 'quarry_warden':{'front':[620,155,1102,610],'side':[1105,155,1478,610],'back':[1483,155,1915,610]}}
SCRIPT=r'''async function(data){
 const load=async src=>{const im=new Image();im.src=src;await im.decode();return im};
 const [concept,model]=await Promise.all([load(data.concept),load(data.model)]);
 const scratch=document.createElement('canvas');scratch.width=model.width;scratch.height=model.height;const sx=scratch.getContext('2d');sx.drawImage(model,0,0);
 const pixels=sx.getImageData(0,0,model.width,model.height).data;
 let l=model.width,r=0,t=model.height,b=0;
 for(let y=0;y<model.height;y++)for(let x=0;x<model.width;x++){
  const i=(y*model.width+x)*4;if(pixels[i+3]>16&&pixels[i]+pixels[i+1]+pixels[i+2]>12){l=Math.min(l,x);r=Math.max(r,x+1);t=Math.min(t,y);b=Math.max(b,y+1)}}
 const box=data.box,height=600,refw=(box[2]-box[0])/(box[3]-box[1])*height,modelw=(r-l)/(b-t)*height;
 const c=document.createElement('canvas');c.width=Math.ceil(refw+modelw+140);c.height=750;const ctx=c.getContext('2d');
 ctx.fillStyle='#e9e8e4';ctx.fillRect(0,0,c.width,c.height);ctx.fillStyle='#283744';ctx.font='bold 22px sans-serif';ctx.fillText(data.id+' / '+data.view.toUpperCase(),30,34);
 ctx.font='18px sans-serif';ctx.fillText('CONCEPT REFERENCE',30,78);ctx.fillText('ACTUAL BLOCKBENCH MODEL',refw+100,78);
 ctx.drawImage(concept,box[0],box[1],box[2]-box[0],box[3]-box[1],30,110,refw,height);
 ctx.drawImage(model,l,t,r-l,b-t,refw+100,110,modelw,height);
 ctx.fillStyle='#4d5d66';ctx.font='14px sans-serif';ctx.fillText('Common height; proportions retained. No automatic artistic acceptance score.',30,735);
 return {png:c.toDataURL('image/png'),model_bounds:[l,t,r,b]};
}'''
def data(path):return 'data:image/png;base64,'+base64.b64encode(path.read_bytes()).decode()
if __name__=='__main__':
 rows=[]
 for ident,views in BOXES.items():
  folder=GALLERY/ident
  for view,box in views.items():
   result=evaluate('('+SCRIPT+')('+json.dumps(dict(id=ident,view=view,box=box,concept=data(folder/'concept.png'),model=data(folder/(view+'.png'))))+')')
   (folder/('comparison-'+view+'.png')).write_bytes(base64.b64decode(result['png'].split(',',1)[1]))
   rows.append({'id':ident,'view':view,'reference_crop':box,'model_bounds':result['model_bounds'],'score':None})
 (ROOT/'dev-assets/enemies/comparison-layouts.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
 print('Saved 12 concept/native-model comparison boards; no fabricated score.')
