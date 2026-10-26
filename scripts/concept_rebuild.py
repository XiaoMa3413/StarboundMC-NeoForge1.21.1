"""Create fresh studio models inside Blockbench; save native views and UI evidence."""
import argparse
import base64
from contextlib import closing
import hashlib
import json
import shutil
from datetime import datetime
import urllib.request
import websocket
from blockbench_bridge import evaluate
from concept_rebuild_specs import ROOT, OUT, MODELS, JOBS


def capture_ui():
    opener=urllib.request.build_opener(urllib.request.ProxyHandler({}))
    with opener.open('http://127.0.0.1:9228/json/list') as response:
        target=next(t for t in json.load(response) if t['type']=='page' and 'index.html' in t['url'])
    with closing(websocket.create_connection(target['webSocketDebuggerUrl'],origin='http://localhost:9228',timeout=180)) as conn:
        conn.send(json.dumps({'id':9,'method':'Page.captureScreenshot','params':{'format':'png','captureBeyondViewport':False}}))
        while True:
            result=json.loads(conn.recv())
            if result.get('id')==9:
                if 'error' in result: raise RuntimeError(result['error'])
                return base64.b64decode(result['result']['data'])


def run(ids):
    OUT.mkdir(parents=True,exist_ok=True); MODELS.mkdir(parents=True,exist_ok=True)
    evaluate((ROOT/'scripts/concept_rebuild_native.js').read_text(encoding='utf-8'))
    for original in JOBS:
        if ids and original['id'] not in ids: continue
        job=dict(original); ident=job['id']; source=__import__('pathlib').Path(job.pop('concept'))
        job['image']='data:image/png;base64,'+base64.b64encode(source.read_bytes()).decode()
        folder=OUT/ident
        if folder.exists() and (folder/'receipt.json').exists():
            history=OUT/'iterations'/datetime.now().strftime('%Y%m%d-%H%M%S')/ident
            shutil.copytree(folder,history)
            existing=MODELS/(ident+'.bbmodel')
            if existing.exists():shutil.copy2(existing,history/existing.name)
        folder.mkdir(exist_ok=True)
        shutil.copy2(source,folder/'concept.png')
        result=evaluate('V04Create('+json.dumps(job,ensure_ascii=True)+')')
        model_path=MODELS/(ident+'.bbmodel')
        model_path.write_text(json.dumps(result.pop('project'),ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
        result.update(id=ident,concept_sha256=hashlib.sha256(source.read_bytes()).hexdigest(),
                      model_sha256=hashlib.sha256(model_path.read_bytes()).hexdigest())
        (folder/'construction.json').write_text(json.dumps(original,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
        # Re-open native saved bytes, not an unsaved in-memory stand-in.
        evaluate('(async()=>{Project.saved=true;await Project.close(true);Codecs.project.load('+model_path.read_text(encoding='utf-8')+', {path:'+json.dumps(str(model_path))+',no_file:true});return true})()')
        counts=evaluate('({cubes:Cube.all.length,meshes:Mesh.all.length,textures:Texture.all.length,groups:Group.all.length})')
        assert counts['cubes']==result['cubes'] and counts['meshes']==result['meshes'] and counts['textures']==1, (ident,counts,result)
        result['reopened']=counts
        views=['front','side','back','top','perspective']+(['reference_side','reference_back'] if original['side'] else [])
        for view in views:
            shot=evaluate('V04View('+json.dumps(view)+')')
            (folder/(view+'.png')).write_bytes(base64.b64decode(shot.pop('png').split(',',1)[1]))
            (folder/(view+'-editor.png')).write_bytes(capture_ui())
            (folder/(view+'-camera.json')).write_text(json.dumps(shot,indent=2)+'\n',encoding='utf-8')
        (folder/'receipt.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
        print(json.dumps({k:result[k] for k in ['id','version','cubes','meshes','groups','parts','reopened']},ensure_ascii=True),flush=True)
        evaluate('(async()=>{Project.saved=true;await Project.close(true);return true})()')


if __name__=='__main__':
    parser=argparse.ArgumentParser(); parser.add_argument('--ids',nargs='*')
    run(parser.parse_args().ids)
