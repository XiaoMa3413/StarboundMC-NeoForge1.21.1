"""Enemy spawn-egg resources, localized names, empty initial loot and a bolt sprite."""
import json
from pathlib import Path
from equipment_art import Pixels
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources'
NAMES={'scrap_skitter':('废铁掠行蛛','Scrap Skitter'),'coil_sentry':('线圈巡逻机','Coil Sentry'),'breach_guard':('重装破拆卫','Breach Guard'),'quarry_warden':('矿站监工 MK-I','Quarry Warden MK-I')}

def write(path,data):
 path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
for language,index in [('zh_cn',0),('en_us',1)]:
 path=ROOT/f'assets/starboundmc/lang/{language}.json';data=json.loads(path.read_text(encoding='utf-8'))
 for ident,names in NAMES.items():
  data['entity.starboundmc.'+ident]=names[index]
  data['item.starboundmc.'+ident+'_spawn_egg']=names[index]+('刷怪蛋' if index==0 else ' Spawn Egg')
 data['entity.starboundmc.enemy_plasma_bolt']='电浆弹' if index==0 else 'Plasma Bolt'
 write(path,data)
for ident in NAMES:
 write(ROOT/f'assets/starboundmc/models/item/{ident}_spawn_egg.json',{'parent':'minecraft:item/template_spawn_egg'})
 write(ROOT/f'data/starboundmc/loot_table/entities/{ident}.json',{'type':'minecraft:entity','pools':[]})
# A transparent pixel sprite is authored as a native resource, not concept art.
im=Pixels(16,16)
for y in range(16):
 for x in range(16):
  r=((x-7.5)**2+(y-7.5)**2)**.5
  if r<2.5:im.dot(x,y,(225,255,255,255))
  elif r<4:im.dot(x,y,(73,225,245,245))
  elif r<6:im.dot(x,y,(39,153,218,int((6-r)*65)))
im.save(ROOT/'assets/starboundmc/textures/entity/enemies/plasma_bolt.png')
print('Generated 4 spawn eggs, localized entity names and projectile sprite; no natural-spawn data.')
