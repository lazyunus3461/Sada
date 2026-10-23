"""Inspect the supplied 512-block island and prepare three isolated copies in ONE world.
Input is read-only. Chest candidates are data, not fixed loot; the plugin places a
fresh subset at match start. Requires numpy, nbtlib, Pillow.
"""
from pathlib import Path
from functools import lru_cache
import argparse,copy,io,json,math,struct,time,zlib
import numpy as np
import nbtlib as n
from PIL import Image,ImageDraw
p=argparse.ArgumentParser();p.add_argument('--input',type=Path,required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args()
source=a.input.resolve();out=a.output.resolve()
if source==out or out.is_relative_to(source) or (out.exists() and any(out.iterdir())):raise SystemExit('Output must be a separate, empty directory')
out.mkdir(parents=True,exist_ok=True);(out/'region').mkdir()
chunks={};palette=[];registry={}
def register(state):
 key=json.dumps(state.unpack(),sort_keys=True)
 if key not in registry:registry[key]=len(palette);palette.append(copy.deepcopy(state))
 return registry[key]
def material(name):return register(n.Compound({'Name':n.String('minecraft:'+name)}))
AIR=material('air');STONE=material('stone');BEDROCK=material('bedrock');GRAVEL=material('gravel');WATER=material('water');BRICK=material('stone_bricks');WOOD=material('spruce_planks');FENCE=material('spruce_fence')
LOW,HIGH=48,112
blocks=np.full((HIGH-LOW,512,512),AIR,dtype=np.uint16)
def unpack(bs):
 ids=np.array([register(v) for v in bs['palette']],dtype=np.uint16)
 if len(ids)==1:return np.full((16,16,16),ids[0],dtype=np.uint16)
 bits=max(4,(len(ids)-1).bit_length());per=64//bits;pos=np.arange(4096,dtype=np.uint64);data=np.asarray(bs['data'],dtype=np.int64).view(np.uint64)
 local=(data[(pos//per).astype(np.int64)]>>((pos%per)*bits))&((1<<bits)-1)
 return ids[local.astype(np.int64)].reshape(16,16,16)
for path in sorted((source/'region').glob('*.mca')):
 raw=path.read_bytes()
 for i in range(1024):
  off=int.from_bytes(raw[i*4:i*4+3],'big')*4096
  if not off:continue
  size=int.from_bytes(raw[off:off+4],'big');c=n.File.parse(io.BytesIO(zlib.decompress(raw[off+5:off+4+size])));cx,cz=int(c['xPos']),int(c['zPos']);chunks[cx,cz]=c
  if not(-16<=cx<16 and -16<=cz<16):raise ValueError('Expected the uploaded 512-block template')
  for section in c.get('sections',[]):
   sy=int(section['Y']);y1=max(LOW,sy*16);y2=min(HIGH,sy*16+16)
   if y1<y2:blocks[y1-LOW:y2-LOW,cz*16+256:cz*16+272,cx*16+256:cx*16+272]=unpack(section['block_states'])[y1-sy*16:y2-sy*16]
names=[str(s['Name']).split(':')[-1] for s in palette]
full={'stone','grass_block','dirt','sand','gravel','spruce_planks','oak_planks','dark_oak_planks','stone_bricks','bricks','andesite','cobblestone','mossy_cobblestone','smooth_sandstone','coarse_dirt','red_terracotta','hay_block','white_wool','red_wool'}
solid=np.isin(blocks,[i for i,s in enumerate(names) if s in full]);empty=blocks==AIR
valid=solid[:-2]&empty[1:-1]&empty[2:]
inside_ids=[i for i,s in enumerate(names) if s.endswith('_planks') or s in {'stone_bricks','bricks','cobblestone','mossy_cobblestone','smooth_sandstone'}]
roofs=np.max(np.where(~empty,np.arange(LOW,HIGH)[:,None,None],LOW-1),axis=0)
walk=[]
for iy,z,x in np.argwhere(valid):
 y=int(iy)+LOW+1;wx,wz=int(x)-256,int(z)-256
 if abs(wx)>245 or abs(wz)>245 or math.hypot(wx+.5,wz+.5)>238 or y<63:continue
 neigh=[]
 for dx,dz in [(1,0),(0,1),(-1,0),(0,-1)]:neigh.append(bool(0<=x+dx<512 and 0<=z+dz<512 and valid[iy,z+dz,x+dx]))
 if sum(neigh)<2 or (sum(neigh)==2 and (neigh[0] and neigh[2] or neigh[1] and neigh[3])):continue
 indoor=int(blocks[iy,z,x]) in inside_ids
 covered=roofs[z,x]>=y+2
 score=(5 if indoor and covered else 2 if indoor else 1)+(.3 if sum(neigh)==2 else 0)
 walk.append((score,wx+.5,y,wz+.5,'interior' if indoor and covered else 'roof' if indoor else 'outdoors'))
# Deterministic analysis produces a broad pool; match choices remain randomized at runtime.
rng=np.random.default_rng(20261023);rng.shuffle(walk);walk.sort(key=lambda v:-v[0])
selected=[]
# Limit each sector to stop village interiors consuming the entire candidate pool.
sectors={}
for q in walk:
 sector=(int((q[1]+256)//64),int((q[3]+256)//64))
 if sectors.get(sector,0)>=5 or any((q[1]-r[1])**2+(q[3]-r[3])**2+(q[2]-r[2])**2<36 for r in selected):continue
 selected.append(q);sectors[sector]=sectors.get(sector,0)+1
 if len(selected)==160:break
if len(selected)<80:raise ValueError('Not enough safe, distributed chest candidates')
# Keep the original 160 indices stable for existing servers, then add open-sky ground.
extra=[];outdoor_sectors={}
for q in walk:
 if q[4]!='outdoors':continue
 x,z=int(q[1]-.5)+256,int(q[3]-.5)+256
 if roofs[z,x]>=q[2] or names[int(blocks[q[2]-LOW-1,z,x])] not in {'grass_block','dirt','sand','gravel','stone','andesite','coarse_dirt'}:continue
 sector=(int((q[1]+256)//64),int((q[3]+256)//64))
 if outdoor_sectors.get(sector,0)>=4 or any((q[1]-r[1])**2+(q[3]-r[3])**2+(q[2]-r[2])**2<36 for r in selected):continue
 selected.append(q);extra.append(q);outdoor_sectors[sector]=outdoor_sectors.get(sector,0)+1
 if len(extra)==96:break
if len(extra)!=96:raise ValueError('Cannot expand exterior supply safely')
resource=Path(__file__).resolve().parents[1]/'src/main/resources/hazir-dis-sandiklar.yml'
resource.write_text('# Read-only template additions; existing arena/lobby/spawns are preserved.\nlocations:\n'+''.join(f'  - "{q[1]}|{q[2]}|{q[3]}"\n' for q in extra))
# 800 CENTRAL additions support 240-320 chests inside a 250x250 square.
# Preserve the existing 256 indices, then spread additions evenly over 48-block cells.
from collections import deque
dense_extra=[];dense_rng=np.random.default_rng(1032026)
def add_dense(candidates,count,required=True):
 dense_rng.shuffle(candidates);groups={}
 for q in candidates:groups.setdefault((int((q[1]+256)//48),int((q[3]+256)//48)),deque()).append(q)
 keys=list(groups);dense_rng.shuffle(keys);added=0
 while keys and added<count:
  for key in list(keys):
   group=groups[key];point=None
   while group:
    q=group.popleft()
    if all((q[1]-r[1])**2+(q[3]-r[3])**2+(q[2]-r[2])**2>=25 for r in selected):point=q;break
   if point is None:keys.remove(key);continue
   selected.append(point);dense_extra.append(point);added+=1
   if added==count:break
 if required and added!=count:raise ValueError(f'Cannot add dense candidates safely: {added}/{count}')
central=lambda q: -125<=q[1]-.5<125 and -125<=q[3]-.5<125
add_dense([q for q in walk if q[4]=='interior' and central(q)],240,False)
natural={'grass_block','dirt','sand','gravel','stone','andesite','coarse_dirt'}
add_dense([q for q in walk if q[4]=='outdoors' and central(q) and roofs[int(q[3]-.5)+256,int(q[1]-.5)+256]<q[2] and names[int(blocks[q[2]-LOW-1,int(q[3]-.5)+256,int(q[1]-.5)+256])] in natural],800-len(dense_extra))
resource.with_name('hazir-yogun-sandiklar.yml').write_text('# Dense pool additions; append once without changing saved locations or world blocks.\nlocations:\n'+''.join(f'  - "{q[1]}|{q[2]}|{q[3]}"\n' for q in dense_extra))
spawn_pool=[q for q in walk if q[4]=='outdoors' and roofs[int(q[3]-.5)+256,int(q[1]-.5)+256]<q[2]+2]
spawns=[]
for i in range(20):
 angle=2*math.pi*i/20;target=(160*math.cos(angle),160*math.sin(angle))
 choices=sorted(spawn_pool,key=lambda q:(q[1]-target[0])**2+(q[3]-target[1])**2)
 point=next((q for q in choices if all((q[1]-r[1])**2+(q[3]-r[3])**2>=28**2 for r in spawns)),None)
 if point is None:raise ValueError('Cannot place 20 safe team landing points')
 spawns.append(point)
center=min(spawn_pool,key=lambda q:q[1]**2+q[3]**2)
def pack(values,bits):
 flat=np.asarray(values,dtype=np.uint64).reshape(-1);per=64//bits;words=np.zeros(math.ceil(len(flat)/per),dtype=np.uint64)
 for i in range(per):part=flat[i::per];words[:len(part)]|=part<<np.uint64(i*bits)
 return n.LongArray(words.view(np.int64))
def states(array):
 ids,inverse=np.unique(array,return_inverse=True);d=n.Compound({'palette':n.List[n.Compound]([palette[int(i)] for i in ids])})
 if len(ids)>1:d['data']=pack(inverse,max(4,(len(ids)-1).bit_length()))
 return d
def section(sy,array):return n.Compound({'Y':n.Byte(sy),'block_states':states(array),'biomes':n.Compound({'palette':n.List[n.String]([n.String('minecraft:ocean')])})})
base=[]
for sy in range(-4,3):
 arr=np.full((16,16,16),STONE,dtype=np.uint16)
 if sy==-4:arr[0]=BEDROCK
 base.append(section(sy,arr))
all_chunks={}
for shift in [0,768,1536]:
 for (cx,cz),original in chunks.items():
  c=copy.deepcopy(original);c['xPos']=n.Int(cx+shift//16);c['zPos']=n.Int(cz);c['isLightOn']=n.Byte(0);c['InhabitedTime']=n.Long(0)
  upper=[s for s in c['sections'] if int(s['Y'])>=3]
  for s in upper:
   if int(s['Y'])==3:
    arr=unpack(s['block_states']);arr[:10]=STONE;s['block_states']=states(arr)
  c['sections']=n.List[n.Compound](copy.deepcopy(base)+upper)
  for entity in c.get('block_entities',[]):entity['x']=n.Int(int(entity['x'])+shift)
  all_chunks[cx+shift//16,cz]=c
# A small safe starting deck outside all three battle regions, ready for the user's lobby build.
for cz in range(-29,-25):
 for cx in range(46,50):
  arrays={3:np.full((16,16,16),AIR,dtype=np.uint16),4:np.full((16,16,16),AIR,dtype=np.uint16)}
  arrays[3][:10]=STONE;arrays[3][10]=GRAVEL;arrays[3][11:15]=WATER
  for z in range(16):
   for x in range(16):
    wx,wz=cx*16+x,cz*16+z
    if 744<=wx<=792 and -464<=wz<=-416:
     edge=wx in (744,792) or wz in (-464,-416);arrays[4][8,z,x]=BRICK if edge else WOOD
     if edge:arrays[4][9,z,x]=FENCE
  heights=np.full((16,16),63,dtype=np.int64)
  for sy,arr in arrays.items():
   for y in range(16):heights=np.where(arr[y]!=AIR,np.maximum(heights,sy*16+y+1),heights)
  hm=pack(heights+64,9)
  all_chunks[cx,cz]=n.File({'DataVersion':n.Int(4556),'xPos':n.Int(cx),'yPos':n.Int(-4),'zPos':n.Int(cz),'Status':n.String('minecraft:full'),'LastUpdate':n.Long(0),'InhabitedTime':n.Long(0),'isLightOn':n.Byte(0),'sections':n.List[n.Compound](copy.deepcopy(base)+[section(sy,arr) for sy,arr in arrays.items()]),'block_entities':n.List[n.Compound]([]),'block_ticks':n.List[n.Compound]([]),'fluid_ticks':n.List[n.Compound]([]),'structures':n.Compound({'starts':n.Compound({}),'References':n.Compound({})}),'Heightmaps':n.Compound({key:hm for key in ['WORLD_SURFACE','MOTION_BLOCKING','MOTION_BLOCKING_NO_LEAVES','OCEAN_FLOOR']})})
regions={}
for (cx,cz),c in all_chunks.items():
 buf=io.BytesIO();c.write(buf);payload=zlib.compress(buf.getvalue(),6);record=struct.pack('>I',len(payload)+1)+b'\x02'+payload;regions.setdefault((cx//32,cz//32),{})[cx%32+cz%32*32]=record
for (rx,rz),records in regions.items():
 header=bytearray(8192);body=bytearray();sector=2
 for idx,record in sorted(records.items()):
  size=math.ceil(len(record)/4096);header[idx*4:idx*4+3]=sector.to_bytes(3,'big');header[idx*4+3]=size;body.extend(record);body.extend(bytes(size*4096-len(record)));sector+=size
 (out/'region'/f'r.{rx}.{rz}.mca').write_bytes(header+body)
level=n.load(source/'level.dat');d=level['Data'];d['LevelName']=n.String('KasabaBR');d['SpawnX']=n.Int(768);d['SpawnY']=n.Int(73);d['SpawnZ']=n.Int(-440);d['BorderCenterX']=n.Double(768);d['BorderCenterZ']=n.Double(-100);d['BorderSize']=d['BorderSizeLerpTarget']=n.Double(4096);d['BorderSizeLerpTime']=n.Long(0);d['WorldGenSettings']['dimensions']['minecraft:overworld']['generator']['settings']['layers'][1]['block']=n.String('minecraft:stone');d['GameRules']['doDaylightCycle']=n.String('false');d['GameRules']['doWeatherCycle']=n.String('false');level.save(out/'level.dat',gzipped=True)
border=n.load(source/'data/world_border.dat');b=border['data'];b['size']=b['lerp_target']=n.Double(4096);b['center_x']=n.Double(768);b['center_z']=n.Double(-100);(out/'data').mkdir();border.save(out/'data/world_border.dat',gzipped=True)
profile={'schema':1,'lobby':{'position':'768.5|73|-440.5','area':'744|792|-464|-416'},'arenas':{}}
def pos(q,shift):return f'{q[1]+shift}|{q[2]}|{q[3]}'
for i,shift in enumerate([0,768,1536],1):profile['arenas'][str(i)]={'center':f'{shift+.5}|{center[2]}|0.5','area':f'{shift-256}|{shift+255}|-256|255','spawns':[pos(q,shift) for q in spawns],'candidates':[pos(q,shift) for q in selected]}
(out/'kasaba-br-map.yml').write_text(json.dumps(profile,ensure_ascii=False,indent=2),encoding='utf-8')
report={'original_chunks':len(chunks),'prepared_chunks':len(all_chunks),'regions':len(regions),'islands':3,'offsets':[0,768,1536],'chest_candidates_per_island':len(selected),'candidates_by_kind':{s:sum(q[4]==s for q in selected) for s in ['interior','roof','outdoors']},'active_chest_area_size':250,'active_candidates_per_island':sum(central(q) for q in selected),'team_landing_points_per_island':20,'lobby':[768.5,73,-440.5],'global_border_size':4096,'gameplay_chests':'spawned and removed by plugin per match; selected locations rotate'}
(out.parent/'map-report.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
# Actual block-derived overview of island 1 with candidate markers, not a concept image.
colors={'water':(49,112,164),'grass_block':(96,144,74),'dirt_path':(163,142,93),'sand':(217,207,154),'spruce_leaves':(45,92,52),'oak_leaves':(57,116,59),'spruce_planks':(125,91,56),'oak_planks':(164,130,77),'dark_oak_planks':(78,57,38),'stone':(126,131,132),'gravel':(144,142,132),'stone_bricks':(145,149,150),'bricks':(150,94,76),'glass_pane':(172,203,207)}
image=np.zeros((512,512,3),dtype=np.uint8)
for z in range(512):
 for x in range(512):
  y=int(roofs[z,x]);name=names[int(blocks[y-LOW,z,x])] if LOW<=y<HIGH else 'water';image[z,x]=colors.get(name,(112,122,91))
im=Image.fromarray(image).resize((1024,1024));draw=ImageDraw.Draw(im)
for q in selected:
 if not central(q):continue
 x,z=int((q[1]+256)*2),int((q[3]+256)*2);draw.ellipse((x-3,z-3,x+3,z+3),fill=(255,211,80),outline=(65,36,12))
draw.rectangle((262,262,762,762),outline=(85,235,245),width=3)
im.save(out.parent/'KasabaBR-Sandik-Adaylari.png')
print(json.dumps(report,indent=2),flush=True)
