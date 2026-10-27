"""Enemy model construction specifications, measured against the new concept sheets.

Coordinates are Blockbench pixels (16 per block), front is -Z. Each part belongs
explicitly to a pivoted bone. Concept orthographic projections are the surface
reference; unseen cut faces receive a separately painted material patch.
"""
import math
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
JOBS=[]

def job(ident,cx,ground,scale,side,back,palette):
 j=dict(id=ident,front=[cx,ground,scale],side=[side,ground,scale],back=[back,ground,scale],palette=palette,bones=[],parts=[])
 JOBS.append(j);bone(j,'root',None,[0,0,0]);return j

def bone(j,name,parent,pivot):j['bones'].append(dict(name=name,parent=parent,pivot=pivot))
def fp(j,x,y,z=0):
 cx,ground,s=j['front'];return [(cx-x)*s,(ground-y)*s,z]
def fb(j,name,parent,x,y,z=0):bone(j,name,parent,fp(j,x,y,z))

def mesh(j,name,vertices,faces,bone='body',mat=None):
 j['parts'].append(dict(name=name,vertices=vertices,faces=faces,bone=bone,material=mat))

def box(j,name,lo,hi,bone='body',mat=None):
 x,y,z=lo;X,Y,Z=hi
 mesh(j,name,[[x,y,z],[X,y,z],[X,Y,z],[x,Y,z],[x,y,Z],[X,y,Z],[X,Y,Z],[x,Y,Z]],[[0,3,2,1],[4,5,6,7],[0,1,5,4],[3,7,6,2],[0,4,7,3],[1,2,6,5]],bone,mat)

def rect(j,name,x,y,w,h,depth,z=0,bone='body',mat=None):
 a=fp(j,x+w,y+h,z-depth/2);b=fp(j,x,y,z+depth/2);box(j,name,a,b,bone,mat)

def poly(j,name,points,depth,z=0,bone='body',mat=None):
 p=[fp(j,x,y,z-depth/2) for x,y in points];q=[fp(j,x,y,z+depth/2) for x,y in points];n=len(p)
 area=sum(p[i][0]*p[(i+1)%n][1]-p[(i+1)%n][0]*p[i][1] for i in range(n))
 front=list(range(n));back=list(range(n,2*n));sides=[[i,(i+1)%n,(i+1)%n+n,i+n] for i in range(n)]
 if area>0:front.reverse()
 else:back.reverse();sides=[list(reversed(face)) for face in sides]
 mesh(j,name,p+q,[front,back]+sides,bone,mat)
 j['parts'][-1]['oriented']=True

def bevel(j,name,lo,hi,b=.3,bone='body',mat=None,front_drop=0):
 x,y,z=lo;X,Y,Z=hi;b=min(b,(X-x)/4,(Y-y)/4,(Z-z)/4)
 ring=lambda inset:[[x+b+inset,z+inset],[X-b-inset,z+inset],[X-inset,z+b+inset],[X-inset,Z-b-inset],[X-b-inset,Z-inset],[x+b+inset,Z-inset],[x+inset,Z-b-inset],[x+inset,z+b+inset]]
 vs=[]
 for yy,inset in ((y,b*.5),(y+b,0),(Y-b,0),(Y,b*.5)):
  vs.extend([[xx,yy-front_drop*(Z-zz)/(Z-z)*(1 if yy>Y-b-.001 else 0),zz] for xx,zz in ring(inset)])
 faces=[list(range(8)),list(range(24,32))]
 for layer in range(3):
  faces.extend([[layer*8+i,layer*8+(i+1)%8,(layer+1)*8+(i+1)%8,(layer+1)*8+i] for i in range(8)])
 mesh(j,name,vs,faces,bone,mat)

def joint(j,name,center,radius,length,axis='x',bone='body',mat=2,n=8):
 vs=[];axis='xyz'.index(axis)
 for t in (-length/2,length/2):
  for k in range(n):
   a=2*math.pi*k/n+math.pi/8;p=[radius*math.cos(a),radius*math.sin(a)];v=[];it=iter(p)
   for i in range(3):v.append(center[i]+(t if i==axis else next(it)))
   vs.append(v)
 faces=[list(range(n)),list(range(n,2*n))]+[[i,(i+1)%n,(i+1)%n+n,i+n] for i in range(n)]
 mesh(j,name,vs,faces,bone,mat)

def rod(j,name,a,b,width,bone='body',mat=2):
 # Arbitrary oriented rectangular hydraulic segment.
 d=[b[i]-a[i] for i in range(3)];l=math.sqrt(sum(x*x for x in d));d=[x/l for x in d]
 ref=[0,0,1] if abs(d[2])<.9 else [1,0,0]
 u=[d[1]*ref[2]-d[2]*ref[1],d[2]*ref[0]-d[0]*ref[2],d[0]*ref[1]-d[1]*ref[0]];q=math.sqrt(sum(x*x for x in u));u=[x/q*width/2 for x in u]
 v=[d[1]*u[2]-d[2]*u[1],d[2]*u[0]-d[0]*u[2],d[0]*u[1]-d[1]*u[0]]
 vs=[[p[i]+sx*u[i]+sy*v[i] for i in range(3)] for p in (a,b) for sx,sy in ((-1,-1),(1,-1),(1,1),(-1,1))]
 mesh(j,name,vs,[[0,1,2,3],[4,7,6,5],[0,4,5,1],[1,5,6,2],[2,6,7,3],[3,7,4,0]],bone,mat)

def bolts(j,name,x0,x1,y0,y1,z,bone='body',radius=.09):
 for x in (x0,x1):
  for y in (y0,y1):joint(j,name,[x,y,z],radius,.12,'z',bone,2,6)

# E01: short quadruped, four complete leg chains and two small front mandibles.
j=job('scrap_skitter',885,585,14.4/337,1332,1779,['#74777a','#24282d','#b7babe','#b18c24','#f6b93f','#72503a','#43525a','#d7d3c4'])
bone(j,'body','root',[0,7,0]);bone(j,'sensor','body',[0,7.7,-8])
bevel(j,'切角采矿主壳',[-6,5.7,-8.3],[6,11.7,8.2],.65,front_drop=1.2)
bevel(j,'深色下部机腹',[-5.2,3.9,-6.9],[5.2,6.2,7],.5,mat=1)
box(j,'主壳侧黄识别带',[-6.1,6.5,-5],[6.1,8.1,6.7],mat=3)
bevel(j,'前端钢质保险杠',[-5.8,6.8,-8.7],[5.8,8.1,-7.8],.2)
rect(j,'左琥珀眼框',818,404,44,30,.55,-8.9,'sensor',1);rect(j,'右琥珀眼框',909,404,44,30,.55,-8.9,'sensor',1)
rect(j,'左琥珀传感光 [glow]',827,411,25,14,.16,-9.22,'sensor',4);rect(j,'右琥珀传感光 [glow]',918,411,25,14,.16,-9.22,'sensor',4)
bevel(j,'顶部独立电池盖',[-3.4,11.55,-1],[3.4,12,5.3],.12)
for x in (-3.4,3.4):
 outline=[(11.05,1.8),(14.4,3.8),(14.4,6.9),(11.05,6.8)]
 vs=[[xx,y,z] for xx in (x-.32,x+.32) for y,z in outline]
 mesh(j,'后掠斜面散热鳍',vs,[[0,1,2,3],[4,7,6,5],[0,4,5,1],[1,5,6,2],[2,6,7,3],[3,7,4,0]])
 rod(j,'散热鳍黄色前护边',[x,11.1,1.8],[x,14.4,3.8],.18,mat=3)
 for yy in (12.1,12.9,13.7):
  for sign in (-1,1):box(j,'散热鳍凹入风槽',[x+sign*.34-.03,yy,4.6],[x+sign*.34+.03,yy+.22,6.35],mat=1)
for x in (-4.2,4.2):
 rod(j,'主壳顶部黄色纵向识别线',[x,10.72,-6.8],[x,11.8,6.5],.28,mat=3)
 for z in (-5,3):joint(j,'顶部检修锁扣',[x,11.2 if z<0 else 11.95,z],.14,.15,'y',mat=2,n=6)
bevel(j,'电池盖独立黄色嵌板',[-2.7,12.02,.1],[2.7,12.15,4.6],.05,mat=3)
bevel(j,'后电池插口框',[-1.8,6.7,8.1],[1.8,9,8.55],.2)
box(j,'后电池暗插座',[-1.3,7.1,8.55],[1.3,8.6,8.7],mat=1)
for x in (-.35,.35):box(j,'电池黄铜插针',[x-.06,7.45,8.7],[x+.06,8.25,8.8],mat=4)
for prefix,z,sgnZ in [('front',-5.9,-1),('rear',5.7,1)]:
 for side,sign in [('right',1),('left',-1)]:
  upper=prefix+'_'+side;lower=upper+'_lower';foot=upper+'_foot'
  hip=[sign*6.3,7.9,z];knee=[sign*7.1,3.25,z+sgnZ*1.5];ankle=[sign*7.4,1.1,z+sgnZ*2.4]
  bone(j,upper,'body',hip);bone(j,lower,upper,knee);bone(j,foot,lower,ankle)
  joint(j,'锈褐髋关节',hip,1.0,1.45,'x',upper,5);joint(j,'银色髋销',[hip[0]+sign*.78,hip[1],hip[2]],.45,.24,'x',upper,2)
  rod(j,'外张上足骨架',hip,knee,1.5,upper,0);rod(j,'上足黄色护板',[hip[0]+sign*.7,hip[1]-.3,hip[2]-.35],[knee[0]+sign*.7,knee[1]+.45,knee[2]-.35],.58,upper,3)
  joint(j,'足部膝铰链',knee,.68,1.5,'x',lower,5);rod(j,'下足液压杆',knee,ankle,.7,lower,2)
  joint(j,'膝部外侧钢销',[knee[0]+sign*.8,knee[1],knee[2]],.28,.18,'x',lower,2,6)
  joint(j,'踝部轴承',ankle,.46,1.2,'x',foot,1)
  joint(j,'踝部轴承外销',[ankle[0]+sign*.65,ankle[1],ankle[2]],.2,.16,'x',foot,2,6)
  bevel(j,'分层足垫',[ankle[0]-1.35,0,ankle[2]-1.05],[ankle[0]+1.35,.8,ankle[2]+1.05],.18,foot,1)
  for dx in (-.9,0,.9):bevel(j,'独立三趾钢爪',[ankle[0]+dx-.23,0,ankle[2]-1.6],[ankle[0]+dx+.23,.6,ankle[2]-.5],.1,foot,2)
for name,x in [('mandible_right',1.8),('mandible_left',-1.8)]:
 bone(j,name,'sensor',[x,5.8,-8.6]);joint(j,'前钳根销',[x,5.8,-8.6],.36,.5,'x',name,5)
 rod(j,'前钳臂',[x,5.7,-8.8],[x*1.15,4.8,-10.1],.35,name,2);rod(j,'前钳弯爪',[x*1.15,4.8,-10.1],[x*.75,4.2,-10.5],.3,name,2)
bolts(j,'壳体检修铆钉',-4.9,4.9,8.4,9.8,-8.35)

# E02: one lens, two accelerator coils and exactly three stabilizer fins.
j=job('coil_sentry',766,625,20.8/568,1220,1725,['#cdcac1','#272e34','#999d9f','#b46d35','#5fe4ed','#75553c','#40525c','#ece8de'])
bone(j,'body','root',[0,10,0]);bone(j,'eye','body',[0,12,-3.5]);bone(j,'cannon','body',[0,7,-4])
poly(j,'切角菱形主机壳',[[719,184],[813,184],[849,235],[849,425],[803,516],[764,590],[722,513],[677,426],[677,238]],6.7)
poly(j,'深色内层框架',[[723,201],[807,201],[831,244],[831,418],[790,505],[763,558],[738,505],[695,418],[695,244]],7.1,mat=1)
poly(j,'前侧象牙装甲',[[718,187],[812,187],[842,239],[842,419],[795,509],[764,562],[731,509],[687,419],[687,242]],.5,-3.8)
rect(j,'独眼黑色凹槽',736,260,65,79,.5,-4.3,'eye',1);rect(j,'单枚青色传感镜 [glow]',747,274,44,51,.22,-4.61,'eye',4)
bevel(j,'镜头银色上眉',[-1.55,13.8,-4.58],[1.45,14.2,-4.18],.1,'eye',2)
for x in (-2.8,2.8):box(j,'机体青色状态条 [glow]',[x-.08,8.7,-4.1],[x+.08,10.6,-3.9],'body',4)
# Short hollow squared barrel.
for lo,hi in [([-1.05,6.5,-7.8],[1.05,6.8,-4]),([-1.05,8.4,-7.8],[1.05,8.7,-4]),([-1.05,6.8,-7.8],[-.75,8.4,-4]),([.75,6.8,-7.8],[1.05,8.4,-4])]:box(j,'独立炮口侧壁',lo,hi,'cannon',6)
joint(j,'炮根八角承力环',[0,7.6,-4],1.48,.55,'z','cannon',2)
box(j,'炮膛暗孔',[-.72,6.85,-4.2],[.72,8.35,-4.0],'cannon',1);box(j,'炮膛蓝芯 [glow]',[-.25,7.3,-4.23],[.25,7.8,-4.21],'cannon',4)
for side,sign in [('coil_right',1),('coil_left',-1)]:
 bone(j,side,'body',[sign*5.45,11,0])
 for k in range(7):
  x=sign*(4.25+k*.4)
  outer=[(8,-2.1),(8.6,-2.7),(14.2,-2.7),(14.8,-2.1),(14.8,2.1),(14.2,2.7),(8.6,2.7),(8,2.1)]
  inner=[(8.5,-1.9),(8.9,-2.2),(13.9,-2.2),(14.3,-1.9),(14.3,1.9),(13.9,2.2),(8.9,2.2),(8.5,1.9)]
  vs=[[xx,y,z] for xx,ring in ((x-.15,outer),(x+.15,outer),(x-.15,inner),(x+.15,inner)) for y,z in ring]
  faces=[]
  for q in range(8):
   r=(q+1)%8;faces.extend([[q,r,r+8,q+8],[q+16,q+24,r+24,r+16],[q,q+16,r+16,r],[q+8,r+8,r+24,q+24]])
  mesh(j,'八角闭合铜绕组',vs,faces,side,3)
  j['parts'][-1]['oriented']=True
 bevel(j,'线圈中央钢质固定带',[sign*5.45-1.5,10.8,-2.9],[sign*5.45+1.5,11.35,2.9],.12,side,2)
for name,points in [('fin_top',[[751,64],[776,64],[776,166],[751,179]]),('fin_right',[[688,434],[710,466],[604,611],[557,624],[557,552]]),('fin_left',[[820,434],[795,466],[934,624],[974,623],[974,553]])]:
 pivot=fp(j,763,179,1) if name=='fin_top' else fp(j,*points[0],1)
 bone(j,name,'body',pivot);poly(j,'独立稳定翼',points,.85,1,name)
 # Narrow light strip in the front face, following the angular wing.
 a=fp(j,*points[0],.45);b=fp(j,*points[-2],.45)
 if name=='fin_top':box(j,'顶部翼青色灯 [glow]',[-.08,17.5,.4],[.08,19.8,.6],name,4)
 else:rod(j,'下稳定翼青色灯 [glow]',[a[0],a[1]-.3,.45],[b[0],b[1]+.3,.45],.11,name,4)
bone(j,'rear_turbine','body',[0,11,3.7])
bone(j,'rear_rotor','rear_turbine',[0,11,3.7])
for k in range(8):
 a=2*math.pi*k/8;b=a+math.pi/4
 ring=[(math.cos(t)*r,11+math.sin(t)*r) for r,t in ((2.2,a),(2.8,a),(2.8,b),(2.2,b))]
 vs=[[x,y,z] for z in (3.65,4.35) for x,y in ring]
 mesh(j,'涡轮八角护罩段',vs,[[0,1,2,3],[4,7,6,5],[0,4,5,1],[1,5,6,2],[2,6,7,3],[3,7,4,0]],'rear_turbine',2)
 blade=[(math.cos(t)*r,11+math.sin(t)*r) for r,t in ((.55,a),(1.95,a+.18),(1.9,a+.53),(.65,a+.4))]
 vs=[[x,y,z] for z in (3.95,4.12) for x,y in blade]
 mesh(j,'独立斜叶片',vs,[[0,1,2,3],[4,7,6,5],[0,4,5,1],[1,5,6,2],[2,6,7,3],[3,7,4,0]],'rear_rotor',6)
 rod(j,'涡轮叶缘青光 [glow]',[math.cos(a+.3)*1.25,11+math.sin(a+.3)*1.25,4.2],[math.cos(a+.43)*1.9,11+math.sin(a+.43)*1.9,4.2],.16,'rear_rotor',4)
joint(j,'涡轮中央轮毂',[0,11,4.25],.6,.6,'z','rear_rotor',2)
bevel(j,'背面独立检修口',[-1.6,3.3,3.5],[1.6,6.5,4.0],.2)
bolts(j,'前装甲螺钉',-2.5,2.5,7.0,15.5,-3.9)

# E03: tall asymmetric biped, left shield and right drill, each on its own chain.
j=job('breach_guard',800,732,37.6/692,1211,1605,['#4b555d','#242a30','#a5abad','#ad602d','#ed5d2b','#67574a','#5bc7d4','#d0cbc1'])
bone(j,'body','root',[0,23,0]);bone(j,'head','body',fp(j,798,155));
poly(j,'分层胸甲主体',[[709,152],[882,152],[902,180],[889,264],[839,286],[752,286],[707,254],[696,181]],7)
rect(j,'独立中央胸板',751,169,92,78,.7,-3.9)
rect(j,'胸部深色进气口',775,215,46,10,.16,-4.3,mat=1)
bevel(j,'腰部液压骨架',[-1.25,19.3,-1.8],[1.25,24.1,1.8],.2,mat=1)
bevel(j,'腹部独立活塞罩',[-.95,20.6,-2.8],[.95,22.8,-2.1],.15,mat=2)
for x in (-1.5,1.5):rod(j,'腹部银色液压杆',[x,19.5,-2.4],[x*1.3,24,-2.8],.4,mat=2)
bevel(j,'分体骨盆护板',[-3.8,16,-3],[3.8,19.4,3],.35)
poly(j,'装甲头壳',[[757,61],[821,57],[842,72],[840,133],[819,149],[770,141],[753,116]],4.7,bone='head')
rect(j,'窄目镜凹槽',758,90,80,25,.3,-2.55,'head',1);rect(j,'橙红目镜 [glow]',766,97,65,9,.12,-2.76,'head',4)
for x in (754,826):rect(j,'头部钢色耳轴',x,86,10,35,3,0,'head',2)
for name,sign in [('right_arm',1),('left_arm',-1)]:
 upper=[sign*5.6,30.1,0];elbow=[sign*6.8,23,-.3]
 bone(j,name,'body',upper);bone(j,name+'_lower',name,elbow)
 joint(j,'肩部八角轮毂',upper,1.3,2.1,'x',name,1)
 bevel(j,'厚重肩甲',[sign*6.4-2.1,28.2,-3],[sign*6.4+2.1,32.6,3],.45,name)
 # Orange rim remains separate geometry, not a painted outline.
 bevel(j,'肩甲氧化橙边',[sign*6.4-2.25,28.0,-3.15],[sign*6.4+2.25,28.55,3.15],.15,name,3)
 rod(j,'上臂液压主轴',upper,elbow,1.3,name,1);rod(j,'外露上臂银色活塞',[upper[0]+sign*.65,upper[1]-1.5,-1],[elbow[0]+sign*.65,elbow[1]+.8,-1],.4,name,2)
 bevel(j,'上臂独立装甲',[sign*6.4-1.1,24.5,-1.7],[sign*6.4+1.1,28.8,1.4],.2,name)
 joint(j,'肘关节',elbow,.9,1.8,'x',name+'_lower',2)
 bevel(j,'前臂机匣',[elbow[0]-1.5,17.4,-2],[elbow[0]+1.5,22.8,2],.3,name+'_lower')
# Shield on robot's left (negative BB X; front-view right).
bone(j,'shield','left_arm_lower',[-8.5,20,-2.8])
rod(j,'盾面连接支架',[-6.8,20,-.3],[-9,20,-2.8],.6,'left_arm_lower',1)
poly(j,'左侧独立盾体',[[930,200],[1007,192],[1039,218],[1039,511],[1004,533],[938,512]],1.6,-3.3,'shield')
poly(j,'盾面内板',[[946,220],[1001,212],[1021,229],[1021,487],[1000,509],[951,492]],.25,-4.28,'shield')
for yy in (269,343,417):poly(j,'盾面斜施工条纹',[[950,yy+32],[1019,yy-15],[1019,yy+6],[950,yy+52]],.12,-4.48,'shield',3)
bolts(j,'盾板独立铆钉',-12.3,-8.2,12.2,27.1,-4.6,'shield',.12)
# Drill on robot's right, vertical rest pose; spiral collars have staggered flats.
bone(j,'drill','right_arm_lower',[7.2,18,-.2]);joint(j,'钻机橙色防护领',[7.2,18,-.2],2,1.8,'y','drill',3)
for k in range(8):
 y=16.9-k*.95;r=1.7-k*.19
 joint(j,'阶梯钻体',[7.2,y,-.2],r,.95,'y','drill',1)
 joint(j,'阶梯银色钻刃',[7.2,y+.14,-.2],r+.13,.22,'y','drill',2)
joint(j,'钻尖',[7.2,9.4,-.2],.12,.9,'y','drill',2,4)
for name,sign in [('right_leg',1),('left_leg',-1)]:
 hip=[sign*2.8,16,0];knee=[sign*3.4,8.5,.2];foot=[sign*3.6,2.0,-.7]
 bone(j,name,'body',hip);bone(j,name+'_lower',name,knee);bone(j,name+'_foot',name+'_lower',foot)
 joint(j,'髋部轮毂',hip,1.0,1.8,'x',name,1)
 rod(j,'大腿液压骨架',hip,knee,1.8,name,1)
 bevel(j,'独立大腿护甲',[sign*3.0-1.3,10.1,-2.1],[sign*3.0+1.3,15.2,-.8],.2,name)
 box(j,'大腿橙色外护边',[sign*3.8-.35,10.3,-2.2],[sign*3.8+.35,14.8,-1.9],name,3)
 joint(j,'膝部铰链',knee,.9,2.4,'x',name+'_lower',2)
 bevel(j,'厚重分层护胫',[sign*3.5-1.6,2.8,-2.2],[sign*3.5+1.6,8.2,1.7],.4,name+'_lower')
 bevel(j,'橙边工靴',[sign*3.6-2.0,0,-4.2],[sign*3.6+2.0,2.5,2.1],.35,name+'_foot')
 box(j,'工靴橙色包边',[sign*3.6-1.9,.5,-4.35],[sign*3.6+1.9,1.0,-4.13],name+'_foot',3)
 for zz in (-3,-2,-1):box(j,'鞋头银色折阶',[sign*3.6-1.5,2.46,zz-.18],[sign*3.6+1.5,2.62,zz+.18],name+'_foot',2)
for x in (-2.7,2.7):
 joint(j,'背部独立排气管',[x,33.2,3.4],.45,8.1,'y','body',1)
 joint(j,'排气管橙色顶帽',[x,37.3,3.4],.49,.6,'y','body',3)
 for yy in (29,30,31,32):box(j,'排气散热孔',[x-.2,yy,3.78],[x+.2,yy+.25,3.88],mat=1)
bevel(j,'后背冷却模块',[-2.1,23.8,3.55],[2.1,30.2,4.3],.3)
box(j,'背部青色冷却窗 [glow]',[-.45,25.2,4.31],[.45,29.1,4.45],mat=6)
bolts(j,'胸甲细铆钉',-3.5,3.5,25,30,-3.6,radius=.12)

# E04: broad four-leg chassis, distinct clamp and hammer work arms.
j=job('quarry_warden',858,605,45.6/440,1288,1703,['#ac8c36','#252b30','#aeb6b8','#d4a52d','#54dce4','#6f6248','#6c8b90','#ffc54c'])
bone(j,'body','root',[0,24,0]);bone(j,'cab','body',[0,33,0]);bone(j,'core','cab',[0,42,0])
bevel(j,'宽体工程下底盘',[-9.2,17.8,-7.8],[9.2,22.2,8],.8,mat=1)
bevel(j,'下底盘独立进气格栅',[-4.8,18.4,-8.3],[4.8,21.6,-7.7],.3)
for y in (19,20,21):box(j,'下底盘格栅百叶',[-4.2,y,-8.4],[4.2,y+.2,-8.3],mat=2)
bevel(j,'独立黄色主机舱',[-10.8,23.0,-9.0],[10.8,38.0,8.5],1.0)
bevel(j,'中央驾驶舱',[-8.5,28,-10.3],[8.5,41,4.8],.8,'cab')
bevel(j,'驾驶舱上框',[-8.8,39.3,-10.5],[8.8,41.7,5.0],.4,'cab',3)
box(j,'双目暗色窗框',[-6.4,32.4,-10.7],[6.4,37,-10.2],'cab',1)
for x in (-3.8,3.8):
 bevel(j,'驾驶舱独立琥珀窗',[x-1.7,33.4,-10.9],[x+1.7,36,-10.65],.2,'cab',3)
 box(j,'驾驶舱琥珀窗光 [glow]',[x-1.3,33.8,-10.94],[x+1.3,35.5,-10.9],'cab',7)
bevel(j,'分层前保险杠',[-11.5,23,-11.7],[11.5,27.7,-9.4],.45)
for yy in (23.7,24.8,25.9):box(j,'前格栅暗槽',[-4.2,yy,-11.85],[4.2,yy+.35,-11.7],mat=1)
for x in (-9.6,9.6):
 bevel(j,'角部独立工作灯壳',[x-1,24.0,-12.0],[x+1,26.5,-10.9],.15)
 box(j,'角部工作灯 [glow]',[x-.55,24.5,-12.04],[x+.55,26,-11.98],mat=7)
# Segmented exposed cooling core, not a flat decal.
for x in (-3.8,3.8):bevel(j,'冷却核心开放框架',[x-.35,40.8,-3.4],[x+.35,42.2,3.8],.15,'core',1)
for z in (-3,3.4):bevel(j,'冷却核心开放横梁',[-3.5,40.8,z-.3],[3.5,42.0,z+.3],.15,'core',1)
for x in (-2.3,0,2.3):
 joint(j,'屋顶冷却罐',[x,43,0],.85,3.7,'y','core',4)
 joint(j,'冷却罐顶部钢盖',[x,44.95,0],.96,.35,'y','core',2)
 box(j,'冷却核心窄光 [glow]',[x-.2,41.5,-.87],[x+.2,44.3,-.84],'core',4)
for prefix,z,sgnZ in [('front',-7.7,-1),('rear',7.3,1)]:
 for side,sign in [('right',1),('left',-1)]:
  name=prefix+'_'+side;hip=[sign*10.8,19.3,z];knee=[sign*14.0,8.8,z+sgnZ*2.5];ankle=[sign*14.8,3,z+sgnZ*4.0]
  bone(j,name,'body',hip);bone(j,name+'_lower',name,knee);bone(j,name+'_foot',name+'_lower',ankle)
  joint(j,'外张重型髋轴',hip,2,3.0,'x',name,1);
  joint(j,'髋轴黄色独立外圈',[hip[0]+sign*1.45,hip[1],hip[2]],1.75,.35,'x',name,3);joint(j,'髋轴银边',[hip[0]+sign*1.6,hip[1],hip[2]],1.0,.25,'x',name,2)
  rod(j,'上步足液压骨架',hip,knee,2.5,name,1)
  rod(j,'上步足黄色装甲',[hip[0],hip[1]-.2,hip[2]-.9],[knee[0],knee[1]+1.3,knee[2]-.9],4.7,name,None)
  joint(j,'膝轴钢制轮毂',knee,1.3,2.6,'x',name+'_lower',2)
  rod(j,'下步足伸缩杆',knee,ankle,1.4,name+'_lower',1)
  bevel(j,'下足独立护板',[ankle[0]-2.4,4.4,ankle[2]-2.7],[ankle[0]+2.4,9.9,ankle[2]+1.4],.3,name+'_lower')
  bevel(j,'宽接地垫',[ankle[0]-4.5,0,ankle[2]-4],[ankle[0]+4.5,2.6,ankle[2]+4],.35,name+'_foot',1)
  bevel(j,'接地垫银色上层',[ankle[0]-4.3,2.5,ankle[2]-3.8],[ankle[0]+4.3,3.2,ankle[2]+3.8],.15,name+'_foot',2)
  box(j,'接地垫真实前护板',[ankle[0]-4.2,.7,ankle[2]-4.08],[ankle[0]+4.2,2.2,ankle[2]-3.98],name+'_foot')
  for dx in (-1.7,0,1.7):box(j,'接地垫黄色施工条',[ankle[0]+dx-.35,1.0,ankle[2]-4.05],[ankle[0]+dx+.35,2.25,ankle[2]-3.95],name+'_foot',3)
for name,sign in [('clamp_arm',1),('hammer_arm',-1)]:
 shoulder=[sign*13.6,38,-.7];elbow=[sign*17.6,28,-2.0]
 bone(j,name,'body',shoulder);bone(j,name+'_lower',name,elbow)
 joint(j,'工作臂主轴',shoulder,2.0,3,'x',name,1)
 bevel(j,'黄色肩部执行器',[shoulder[0]-2.1,37,-3.3],[shoulder[0]+2.1,41,2.0],.4,name,3)
 rod(j,'工作上臂骨架',shoulder,elbow,1.8,name,1)
 for dx in (-.65,.65):rod(j,'双工作臂银色液压杆',[shoulder[0]+dx,36.9,-2],[elbow[0]+dx,29,-2.5],.5,name,2)
 joint(j,'工作臂肘轴',elbow,1.25,2.5,'x',name+'_lower',2)
 rod(j,'工具承力前臂',elbow,[sign*18.4,23,-3.0],1.8,name+'_lower',1)
bone(j,'clamp','clamp_arm_lower',[18.4,23,-3]);bevel(j,'夹钳驱动壳',[16.5,20.7,-4.7],[20.7,24.2,-1.1],.4,'clamp',3)
for name,sgn in [('clamp_inner',-1),('clamp_outer',1)]:
 bone(j,name,'clamp',[18.5+sgn*1.6,21,-3])
 points=[[18.5+sgn*1.6,21],[18.5+sgn*3.5,18.6],[18.5+sgn*3.6,15],[18.5+sgn*1.2,13.6],[18.5+sgn*1.8,16.1],[18.5+sgn*1.7,18.2]]
 # This custom front outline is already in model pixels; convert to reference pixels once.
 poly(j,'双钳独立钢爪',[[858-x/j['front'][2],605-y/j['front'][2]] for x,y in points],2.7,-3,name,2)
bone(j,'hammer','hammer_arm_lower',[-18.4,22.5,-3]);bevel(j,'方形冲击锤黄壳',[-22.8,17.2,-6.4],[-14.3,24,-.2],.65,'hammer',3)
bevel(j,'冲击锤独立银色击面',[-23.2,17.6,-6.85],[-14.0,23.6,-6.3],.25,'hammer',2)
bolts(j,'冲击锤螺钉',-22.6,-14.6,18.2,23,-6.9,'hammer',.22)
# Rear twin ventilation boxes and center battery.
for x in (-6.3,6.3):
 bevel(j,'独立后背通风箱',[x-3,29.8,8.1],[x+3,40.2,10.1],.4)
 for yy in (30.6,31.8,33.0,34.2,35.4,36.6,37.8,39):box(j,'通风箱真实百叶',[x-2.45,yy,10.1],[x+2.45,yy+.34,10.35],mat=1)
bevel(j,'中央独立电池仓',[-4.3,19.5,9.4],[4.3,27.7,11],.5)
for x in (-2.5,0,2.5):box(j,'电池仓青色光条 [glow]',[x-.16,20.7,11.05],[x+.16,26.5,11.2],mat=4)
bolts(j,'前保险杠铆钉',-10.3,10.3,24,26.8,-11.9,radius=.17)
