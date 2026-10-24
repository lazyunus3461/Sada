"""Build the enclosed lobby and two separate stadium courses as a new void world.
No running server or existing world is touched. Requires nbtlib and numpy.
"""
from pathlib import Path
import io,json,math,struct,zlib,copy,gzip
import nbtlib as n
import numpy as np
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'src/main/resources/map-v2';OUT.mkdir(parents=True,exist_ok=True);(OUT/'region').mkdir(exist_ok=True)
palette=[];registry={};chunks={}
def state(name,props=None):
 key=(name,tuple(sorted((props or {}).items())))
 if key not in registry:
  value=n.Compound({'Name':n.String('minecraft:'+name)})
  if props:value['Properties']=n.Compound({k:n.String(v) for k,v in props.items()})
  registry[key]=len(palette);palette.append(value)
 return registry[key]
AIR=state('air')
def block(x,y,z,kind):
 x,y,z=round(x),round(y),round(z);cx,cz=x//16,z//16;sy=y//16
 chunk=chunks.setdefault((cx,cz),{});a=chunk.setdefault(sy,np.zeros((16,16,16),dtype=np.uint16));a[y%16,z%16,x%16]=state(kind)
def stadium(cx,x,z):return math.hypot(max(0,abs(x-cx)-90),z)-80
for cx,boat in [(-370,True),(370,False)]:
 for x in range(cx-181,cx+182):
  for z in range(-92,93):
   lateral=abs(stadium(cx,x,z))
   if lateral<=7.4:
    block(x,63,z,'prismarine' if boat else 'stone_bricks');block(x,64,z,'water' if boat else 'packed_mud')
   elif lateral<=10:
    block(x,63,z,'deepslate_bricks');block(x,64,z,'white_concrete')
    for y in range(65,69):block(x,y,z,'blue_stained_glass' if boat else 'dark_oak_planks')
    block(x,69,z,'polished_deepslate')
   if 9<lateral<10 and (x+z)%29==0:block(x,69,z,'sea_lantern')
 # Six checkpoint arches/flags are beside, never across, the driving surface.
 for x,z in [(cx+90,80),(cx+170,0),(cx+90,-80),(cx-90,-80),(cx-170,0),(cx-90,80)]:
  for sign in [-1,1]:
   if z:bx,bz=x,z+sign*10
   else:bx,bz=x+sign*10,z
   for y in range(65,73):block(bx,y,bz,'polished_andesite')
   block(bx,73,bz,'sea_lantern')
 # Start markings are submerged in boat mode, solid floor in horse mode.
 for x in [cx-76,cx-50]:
  for z in range(74,87):block(x,63 if boat else 64,z,'white_concrete' if z%2 else 'black_concrete')

def feature(cx,theta,side,boat=False,jump=False):
 """theta is angle around a bend, or (x,z) on a straight; across-track normal."""
 if isinstance(theta,tuple):x,z=theta;nx,nz=0,1;tx,tz=1,0
 else:
  center=cx+(90 if math.cos(math.radians(theta))>=0 else -90);angle=math.radians(theta);nx,nz=math.cos(angle),math.sin(angle);x,z=center+80*nx,80*nz;tx,tz=-nz,nx
 offsets=np.arange(-7,7.01,.45) if jump else np.arange(-7,2.01,.45) if side<0 else np.arange(-2,7.01,.45)
 for across in offsets:
  for along in np.arange(-.5 if jump else -2,.51 if jump else 2.01,.45):
   bx,bz=round(x+nx*across+tx*along),round(z+nz*across+tz*along)
   for y in range(65,66 if jump else 68):block(bx,y,bz,'oak_log' if jump else 'smooth_sandstone' if boat else 'hay_block')
 if not jump:
  for y in range(68,71):block(x+nx*side*6,y,z+nz*side*6,'sea_lantern' if boat and y==70 else 'orange_concrete')
features=[]
for x,z,angle,kind in [(380,80,None,'jump'),(0,0,45,'jump'),(405,-80,None,'water'),(335,-80,None,'slalom'),(305,-80,None,'slalom'),(0,0,205,'jump')]:
 if kind=='water':
  for dx in range(-5,6):
   for dz in range(-7,8):block(x+dx,64,z+dz,'water')
 else:feature(370,(x,z) if angle is None else angle,-1 if x==335 else 1,jump=kind=='jump')
 features.append({'mode':'horse','kind':kind,'x':x,'z':z,'bendAngle':angle})
for point,side in [((-350,80),-1),(45,1),(-45,-1),((-345,-80),1),((-425,-80),-1),(225,1),(135,-1)]:
 feature(-370,point,side,boat=True);features.append({'mode':'boat','kind':'island','point':point,'side':side})
# Large shared lounge, enclosed on every side; no walkway leads onto either course.
for x in range(-28,29):
 for z in range(-24,25):
  block(x,63,z,'deepslate_bricks');block(x,64,z,'smooth_quartz')
  if abs(x)==28 or abs(z)==24:
   for y in range(65,76):block(x,y,z,'polished_deepslate' if y in [65,66,75] or (abs(x)==28 and z%6==0) or (abs(z)==24 and x%7==0) else 'light_blue_stained_glass')
  block(x,76,z,'sea_lantern' if x%9==0 and z%8==0 else 'tinted_glass')
# Backdrop and thematic mounted flags, kept outside walking lanes.
for side in [-1,1]:
 for dx in range(-8,9):
  for y in range(65,73):block(side*14+dx,y,-20,'dark_oak_planks' if side>0 else 'prismarine_bricks')
 for dx in range(-4,5):
  for z in range(-17,-13):block(side*14+dx,65,z,'oak_planks' if side>0 else 'smooth_sandstone')
 for x in [side*14-8,side*14+8]:
  for y in range(65,75):block(x,y,-20,'polished_andesite')
  block(x,75,-20,'sea_lantern')
# Benches, planting boxes and small checkerboard finish flags.
for side in [-1,1]:
 for z in range(-8,12,6):
  for dx in range(3):block(side*24,65,z+dx,'oak_planks')
 for dx in range(5):
  for dy in range(4):block(side*14+dx-2,70+dy,-19,'white_concrete' if (dx+dy)%2 else 'black_concrete')

for cx in [-370,370]:
 for x in range(cx-5,cx+6):
  for z in range(95,102):block(x,73,z,'glass')

def pack(values,bits):
 flat=np.asarray(values,dtype=np.uint64).ravel();per=64//bits;words=np.zeros(math.ceil(len(flat)/per),dtype=np.uint64)
 for i in range(per):
  part=flat[i::per];words[:len(part)]|=part<<np.uint64(i*bits)
 return n.LongArray(words.view(np.int64))
def section(sy,a):
 ids,inverse=np.unique(a,return_inverse=True);data=n.Compound({'palette':n.List[n.Compound]([palette[int(i)] for i in ids])})
 if len(ids)>1:data['data']=pack(inverse,max(4,(len(ids)-1).bit_length()))
 return n.Compound({'Y':n.Byte(sy),'block_states':data,'biomes':n.Compound({'palette':n.List[n.String]([n.String('minecraft:the_void')])})})
regions={}
for (cx,cz),sections in chunks.items():
 root=n.File({'DataVersion':n.Int(4556),'xPos':n.Int(cx),'yPos':n.Int(-4),'zPos':n.Int(cz),'Status':n.String('minecraft:full'),'LastUpdate':n.Long(0),'InhabitedTime':n.Long(0),'isLightOn':n.Byte(0),'sections':n.List[n.Compound]([section(sy,a) for sy,a in sorted(sections.items())]),'block_entities':n.List[n.Compound]([]),'block_ticks':n.List[n.Compound]([]),'fluid_ticks':n.List[n.Compound]([]),'structures':n.Compound({'starts':n.Compound({}),'References':n.Compound({})})})
 buf=io.BytesIO();root.write(buf);data=zlib.compress(buf.getvalue(),6);regions.setdefault((cx//32,cz//32),{})[cx%32+cz%32*32]=struct.pack('>I',len(data)+1)+b'\x02'+data
for (rx,rz),parts in regions.items():
 header=bytearray(8192);payload=bytearray();sector=2
 for idx,data in sorted(parts.items()):
  count=math.ceil(len(data)/4096);header[idx*4:idx*4+3]=sector.to_bytes(3,'big');header[idx*4+3]=count;payload.extend(data);payload.extend(bytes(count*4096-len(data)));sector+=count
 (OUT/'region'/f'r.{rx}.{rz}.mca').write_bytes(header+payload)
level=n.load(ROOT/'src/main/resources/map-template/level.dat');d=level['Data'];d['LevelName']=n.String('KasabaTR-Yaris-Arenasi-v02');d['SpawnX']=n.Int(0);d['SpawnY']=n.Int(65);d['SpawnZ']=n.Int(9);d['SpawnAngle']=n.Float(180);d['BorderSize']=n.Double(1200);d['BorderSizeLerpTarget']=n.Double(1200);d['BorderCenterX']=n.Double(.5);d['BorderCenterZ']=n.Double(.5);d['WorldGenSettings']['dimensions']['minecraft:overworld']['generator']=n.Compound({'type':n.String('minecraft:flat'),'settings':n.Compound({'biome':n.String('minecraft:the_void'),'layers':n.List[n.Compound]([]),'lakes':n.Byte(0),'features':n.Byte(0),'structure_overrides':n.List[n.String]([])})});level_buffer=io.BytesIO();level.write(level_buffer);(OUT/'level.dat').write_bytes(gzip.compress(level_buffer.getvalue(),mtime=0))
(OUT/'race-layout.properties').write_text('version=3\nhorse-center=370\nboat-center=-370\nstraight-half=90\nradius=80\nwidth=14\n')
# YAML is intentionally written without a YAML package.
lines=['world: KasabaTR-Yaris-Arenasi-v02','lobby: {x: 0.5, y: 65.0, z: 9.5, yaw: 180.0, pitch: 0.0}']
for mode,cx in [('boat',-370),('horse',370)]:
 lines += [mode+':','  start_lanes:']
 for z in [74.5,77.5,80.5,83.5,86.5]:lines.append(f'    - {{x: {cx-75}.5, y: 65.0, z: {z}, yaw: -90.0, pitch: 0.0}}')
 lines+=['  checkpoints:']
 for x,z in [(cx+90,80),(cx+170,0),(cx+90,-80),(cx-90,-80),(cx-170,0),(cx-90,80)]:lines.append(f'    - {{x: {x}.0, y: 65.0, z: {z}.0, radius: 12.0}}')
 lines += [f'  finish: {{x: {cx-50}.5, y: 65.0, z: 80.5, radius: 8.0}}',f'  spectator: {{x: {cx}.0, y: 74.0, z: 98.0, yaw: 180.0, pitch: 15.0}}']
(ROOT/'src/main/resources/race-map-v2.yml').write_text('\n'.join(lines)+'\n');(ROOT/'src/main/resources/world-assets-v2.txt').write_text('\n'.join(str(p.relative_to(OUT)) for p in sorted(OUT.rglob('*')) if p.is_file())+'\n');(ROOT/'src/main/resources/course-features-v2.json').write_text(json.dumps(features,indent=2))
print(json.dumps({'chunks':len(chunks),'regions':len(regions),'courses':2,'width':14,'separation':740,'firstBoatObstacleFromStart':95,'world':str(OUT)}))
