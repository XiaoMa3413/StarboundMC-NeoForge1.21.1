"""Create native enemy projects in real Blockbench, reopen, extract and photograph."""
import argparse,base64,hashlib,json,shutil
from datetime import datetime
from pathlib import Path
from blockbench_bridge import evaluate
from concept_rebuild import capture_ui
from enemy_model_specs import ROOT,JOBS
SOURCE=ROOT/'dev-assets/enemies/models'
CACHE=ROOT/'dev-assets/enemies/geometry'
GALLERY=ROOT.parent/'概念图/2026-10-27_敌人设计与建模'

def run(selected):
 SOURCE.mkdir(exist_ok=True);CACHE.mkdir(exist_ok=True)
 evaluate((ROOT/'scripts/enemy_model_native.js').read_text(encoding='utf-8-sig'))
 views=(ROOT/'scripts/concept_rebuild_native.js').read_text(encoding='utf-8');evaluate(views[views.index('window.V04View ='):])
 for original in JOBS:
  if selected and original['id'] not in selected:continue
  job=dict(original);ident=job['id']
  manifest=json.loads((ROOT/'dev-assets/enemies/concepts/manifest.json').read_text(encoding='utf-8'))
  concept=ROOT/next(row['path'] for row in manifest if row['id']==ident)
  job['image']='data:image/png;base64,'+base64.b64encode(concept.read_bytes()).decode()
  folder=GALLERY/ident
  if (folder/'receipt.json').exists():
   history=GALLERY/'iterations'/datetime.now().strftime('%Y%m%d-%H%M%S')/ident
   shutil.copytree(folder,history)
   for old in (SOURCE/(ident+'.bbmodel'),CACHE/(ident+'.json')):
    if old.exists():shutil.copy2(old,history/old.name)
  folder.mkdir(exist_ok=True)
  shutil.copy2(concept,folder/'concept.png')
  result=evaluate('CreateEnemyModel('+json.dumps(job)+')')
  path=SOURCE/(ident+'.bbmodel');path.write_text(json.dumps(result.pop('project'),ensure_ascii=False,separators=(',',':'))+'\n',encoding='utf-8')
  evaluate('(async()=>{Project.saved=true;await Project.close(true);Codecs.project.load('+path.read_text(encoding='utf-8')+',{path:'+json.dumps(str(path))+',no_file:true});return true})()')
  native=evaluate('ExtractEnemyModel()');native.update(id=ident,source_sha256=hashlib.sha256(path.read_bytes()).hexdigest())
  (CACHE/(ident+'.json')).write_text(json.dumps(native,ensure_ascii=False,separators=(',',':'))+'\n',encoding='utf-8')
  result.update(id=ident,concept_sha256=hashlib.sha256(concept.read_bytes()).hexdigest(),source_sha256=native['source_sha256'],triangles=sum(len(p['triangles']) for p in native['parts']))
  for view in ('front','side','back','perspective'):
   shot=evaluate('V04View('+json.dumps(view)+')')
   (folder/(view+'.png')).write_bytes(base64.b64decode(shot['png'].split(',',1)[1]))
   (folder/(view+'-editor.png')).write_bytes(capture_ui())
  (folder/'construction.json').write_text(json.dumps(original,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
  (folder/'receipt.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
  evaluate('(async()=>{Project.saved=true;await Project.close(true);return true})()')
  print(ident,json.dumps(result,ensure_ascii=True),flush=True)

if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('ids',nargs='*');run(p.parse_args().ids)
