#!/usr/bin/env python3
"""Build the enclosed village from the supplied world, leaving that source untouched.
Requires nbtlib, numpy and Pillow. Produces ordinary Java 1.21.10 Anvil chunks.
"""
from pathlib import Path
from collections import deque
import copy,io,json,math,random,struct,zlib
import nbtlib as n
import numpy as np
from PIL import Image,ImageDraw

ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/'world-template/KasabaTR-VampirKoylu-TamDunya'
OUTPUT=ROOT/'world-template/vampirkoylu_sokaklar'
MIN_X=MIN_Z=-80;WIDTH=256;DEPTH=160;LOW=64;HIGH=96
palette=[];registry={};original={};entities=[]
def register(value):
    key=json.dumps(value.unpack(),sort_keys=True)
    if key not in registry:registry[key]=len(palette);palette.append(copy.deepcopy(value))
    return registry[key]
def block(name,**properties):
    value=n.Compound({'Name':n.String('minecraft:'+name)})
    if properties:value['Properties']=n.Compound({k:n.String(str(v)) for k,v in properties.items()})
    return register(value)
AIR=block('air');STONE=block('stone');BEDROCK=block('bedrock');PAVING=block('stone_bricks')
top=np.full((HIGH-LOW,DEPTH,WIDTH),AIR,dtype=np.uint16)
for path in sorted((SOURCE/'region').glob('*.mca')):
    rx,rz=map(int,path.stem.split('.')[1:]);data=path.read_bytes()
    for index in range(1024):
        offset=(int.from_bytes(data[index*4:index*4+4],'big')>>8)*4096
        if not offset:continue
        length=int.from_bytes(data[offset:offset+4],'big');assert data[offset+4]==2
        chunk=n.File.parse(io.BytesIO(zlib.decompress(data[offset+5:offset+4+length])))
        cx,cz=rx*32+index%32,rz*32+index//32;original[cx,cz]=chunk
        for section in chunk['sections']:
            sy=int(section['Y']);states=section.get('block_states')
            if not states or sy*16<LOW or sy*16>=HIGH:continue
            ids=np.array([register(s) for s in states['palette']],dtype=np.uint16)
            if len(ids)==1:values=np.full((16,16,16),ids[0],dtype=np.uint16)
            else:
                bits=max(4,(len(ids)-1).bit_length());per=64//bits;words=np.asarray(states['data'],dtype=np.int64).view(np.uint64);positions=np.arange(4096,dtype=np.uint64)
                indices=(words[(positions//per).astype(np.int64)]>>((positions%per)*bits))&((1<<bits)-1);values=ids[indices.astype(np.int64)].reshape(16,16,16)
            top[sy*16-LOW:sy*16-LOW+16,cz*16-MIN_Z:cz*16-MIN_Z+16,cx*16-MIN_X:cx*16-MIN_X+16]=values
        entities.extend(copy.deepcopy(list(chunk.get('block_entities',[]))))

def put(x,y,z,name,**properties):
    assert MIN_X<=x<MIN_X+WIDTH and MIN_Z<=z<MIN_Z+DEPTH and LOW<=y<HIGH,(x,y,z)
    top[y-LOW,z-MIN_Z,x-MIN_X]=name if isinstance(name,int) else block(name,**properties)
def fill(x1,y1,z1,x2,y2,z2,name,**properties):
    value=name if isinstance(name,int) else block(name,**properties)
    top[y1-LOW:y2-LOW+1,z1-MIN_Z:z2-MIN_Z+1,x1-MIN_X:x2-MIN_X+1]=value
def container(x,y,z,name):
    put(x,y,z,name,facing='north',open='false')
    entities.append(n.Compound({'id':n.String('minecraft:'+name),'x':n.Int(x),'y':n.Int(y),'z':n.Int(z),'Items':n.List[n.Compound]([])}))

# A small square for the twenty existing meeting seats. All routes remain on y=69.
fill(-68,69,-68,68,95,68,AIR);fill(-68,64,-68,68,64,68,BEDROCK);fill(-68,65,-68,68,67,68,STONE);fill(-68,68,-68,68,68,68,'cobblestone')
entities=[e for e in entities if not(-68<=int(e['x'])<=68 and -68<=int(e['z'])<=68)]
for x in range(-68,69):
    for z in range(-68,69):
        if (x*17+z*11)%13<3:put(x,68,z,'mossy_cobblestone')
for v in range(-68,69):
    for x,z in [(v,-68),(v,68),(-68,v),(68,v)]:
        if x==68 and abs(z)<=2:continue
        fill(x,69,z,x,72,z,'deepslate_bricks')
fill(-10,68,-10,10,68,10,'polished_andesite')
for v in range(-11,12):
    for x,z,opening in [(11,v,3<=v<=5),(-11,v,-5<=v<=-3),(v,-11,3<=v<=5),(v,11,-5<=v<=-3)]:
        if not opening:fill(x,69,z,x,72,z,'stone_bricks')
for x,z in [(-9,-9),(-9,9),(9,-9),(9,9)]:put(x,69,z,'chiseled_stone_bricks');put(x,70,z,'lantern',hanging='false',waterlogged='false')
put(0,69,0,'chiseled_stone_bricks');put(0,70,0,'bell',attachment='floor',facing='north',powered='false')
entities.append(n.Compound({'id':n.String('minecraft:bell'),'x':n.Int(0),'y':n.Int(70),'z':n.Int(0)}))

centers=[(x,z) for z in [-48,-24,0,24,48] for x in [-48,-24,0,24,48] if (x,z)!=(0,0)]
stations=[(-24,-48),(-48,-48),(48,-48),(0,-48),(48,-24),(48,24),(0,48),(-24,-24),(24,-24),(24,24),(-48,0),(48,0),(-48,48),(-48,24),(48,48),(24,48),(-24,24)]
old_info=json.loads((ROOT/'world-template/KasabaTR-VampirKoylu-TamDunya-konumlar.json').read_text())
materials=[entry['etkilesim_blogu'] for entry in old_info['gorev_istasyonlari']]
houses=[]
for number,(cx,cz) in enumerate(centers):
    tone=['brown_terracotta','gray_terracotta','light_gray_terracotta','mud_bricks'][number%4]
    roof=['dark_oak_planks','spruce_planks','dark_prismarine'][number%3]
    fill(cx-8,68,cz-8,cx+8,68,cz+8,'coarse_dirt')
    # Two offset courtyard exits prevent a straight view through the plot.
    for d in range(-9,10):
        for x,z,opening in [(cx-9,cz+d,-4<=d<=-2),(cx+9,cz+d,2<=d<=4),(cx+d,cz-9,False),(cx+d,cz+9,False)]:
            if not opening:fill(x,69,z,x,71,z,'mossy_stone_bricks' if (x+z)%4==0 else 'stone_bricks')
    fill(cx-8,68,cz-8,cx,68,cz-7,'andesite');fill(cx-8,68,cz-8,cx-7,68,cz-3,'andesite');fill(cx-9,68,cz-4,cx-7,68,cz-2,'andesite')
    fill(cx-1,68,cz+7,cx+8,68,cz+8,'andesite');fill(cx+7,68,cz+3,cx+8,68,cz+8,'andesite');fill(cx+7,68,cz+2,cx+9,68,cz+4,'andesite')
    fill(cx-5,68,cz-6,cx+5,68,cz+6,'spruce_planks')
    for y in range(69,73):
        for d in range(-5,6):
            for z in [cz-6,cz+6]:put(cx+d,y,z,'stone_bricks' if y==69 else tone)
        for d in range(-6,7):
            for x in [cx-5,cx+5]:put(x,y,cz+d,'stone_bricks' if y==69 else tone)
    for x in [cx-5,cx+5]:
        for z in [cz-6,cz+6]:fill(x,69,z,x,73,z,'stripped_spruce_log',axis='y')
    # Front and rear doors have separate paths to the surrounding street network.
    for z,facing in [(cz-6,'north'),(cz+6,'south')]:
        for dx,hinge in [(-1,'left'),(0,'right')]:
            for y,half in [(69,'lower'),(70,'upper')]:put(cx+dx,y,z,'spruce_door',facing=facing,half=half,hinge=hinge,open='false',powered='false')
        for dx in [-3,3]:put(cx+dx,71,z,'tinted_glass')
    fill(cx-5,73,cz-6,cx+5,73,cz+6,'spruce_planks')
    for dx in range(-6,7):
        y=74+(6-abs(dx))//2
        fill(cx+dx,y,cz-7,cx+dx,y,cz+7,roof)
        if y>74:fill(cx+dx,74,cz-7,cx+dx,y-1,cz+7,roof)
    put(cx,72,cz,'lantern',hanging='true',waterlogged='false')
    fill(cx+3,75,cz+3,cx+3,79,cz+3,'bricks');put(cx+3,80,cz+3,'flower_pot')
    put(cx-4,69,cz-4,'bookshelf');put(cx-4,70,cz-4,'bookshelf');put(cx+4,69,cz+4,'crafting_table')
    for dx,dz in [(-7,-7),(7,7)]:
        put(cx+dx,69,cz+dz,'stone_bricks');put(cx+dx,70,cz+dz,'lantern',hanging='false',waterlogged='false')
    # Persistent shrubs are decorative cover, not consumable random-tick foliage.
    for dx,dz in [(-7,6),(7,-6)]:
        put(cx+dx,69,cz+dz,'azalea_leaves',distance='1',persistent='true',waterlogged='false')
    houses.append({'center':[cx,cz],'doors':[[cx-1,cz-6],[cx-1,cz+6]],'courtyard_exits':[[cx-9,cz-3],[cx+9,cz+3]]})

# Visible roof landmarks help navigation without adding accessible high ground.
fill(-24,77,-49,-24,82,-49,'polished_deepslate');fill(-26,80,-49,-22,80,-49,'polished_deepslate')
fill(46,77,-50,50,83,-46,'stone_bricks');fill(47,79,-50,49,81,-50,'tinted_glass');fill(45,84,-51,51,84,-45,'dark_prismarine')
put(48,85,-48,'lantern',hanging='false',waterlogged='false')
fill(-48,76,40,-48,84,40,'stripped_spruce_log',axis='y');fill(-52,80,40,-44,80,40,'spruce_planks')

# Short staggered screens break the five-block streets into readable S bends.
for street in [-36,-12,12,36]:
    for row in [-48,-24,0,24,48]:
        for offset,side in [(0,-1),(8,1)]:
            along=row+offset
            if abs(street)<=14 and abs(along)<=20:continue
            lo,hi=(-2,0) if side<0 else (0,2)
            fill(street+lo,69,along,street+hi,71,along,'deepslate_bricks')
            fill(along,69,street+lo,along,71,street+hi,'deepslate_bricks')

# Narrow the outer ring too; there is no unobstructed route around the entire village.
for center in [-48,-24,0,24,48]:
    for side in [-1,1]:
        for shift in [-3,3]:
            fill(center+shift,69,61,center+shift,71,65,'stone_bricks') if side>0 else fill(center+shift,69,-65,center+shift,71,-61,'stone_bricks')
            fill(61,69,center+shift,65,71,center+shift,'stone_bricks')
            fill(-65,69,center+shift,-61,71,center+shift,'stone_bricks')

for i,((cx,cz),material) in enumerate(zip(stations,materials)):
    x,z=cx+2,cz
    if material=='barrel':container(x,69,z,material)
    elif material=='anvil':put(x,69,z,material,facing='north')
    elif material=='loom':put(x,69,z,material,facing='north')
    elif material=='stonecutter':put(x,69,z,material,facing='north')
    elif material=='composter':put(x,69,z,material,level='0')
    else:put(x,69,z,material)
    old_info['gorev_istasyonlari'][i]['konum']=[x+.5,69,z+.5]

def name_at(x,y,z):return str(palette[int(top[y-LOW,z-MIN_Z,x-MIN_X])]['Name']).split(':')[1]
def passable(x,z):
    return all(name_at(x,y,z) in {'air','spruce_door','lantern'} for y in [69,70])
walk={(x,z) for x in range(-67,68) for z in range(-67,68) if passable(x,z)}
reachable={(-5,6)};queue=deque(reachable)
while queue:
    x,z=queue.popleft()
    for p in [(x+1,z),(x-1,z),(x,z+1),(x,z-1)]:
        if p in walk and p not in reachable:reachable.add(p);queue.append(p)
for i,(cx,cz) in enumerate(stations):assert (cx+3,cz) in reachable,('Unreachable station',i)
for house in houses:
    for x,z in house['doors']:assert (x,z) in reachable,('Unreachable door',house)

def los(a,b):
    dx,dz=b[0]-a[0],b[1]-a[1];steps=max(abs(dx),abs(dz))*4
    return all(name_at(round(a[0]+dx*t/steps),70,round(a[1]+dz*t/steps))=='air' for t in range(1,steps))
random.seed(9184);points=random.sample(sorted(walk),min(500,len(walk)));long_views=0;long_total=0
for a,b in zip(points,points[1:]):
    if math.dist(a,b)>=35:long_total+=1;long_views+=los(a,b)
assert long_total>100 and long_views/long_total<.12,(long_views,long_total)

def pack(values,bits):
    flat=np.asarray(values,dtype=np.uint64).reshape(-1);per=64//bits;words=np.zeros(math.ceil(len(flat)/per),dtype=np.uint64)
    for i in range(per):
        part=flat[i::per];words[:len(part)]|=part<<np.uint64(i*bits)
    return n.LongArray(words.view(np.int64))
def states(values):
    ids,inverse=np.unique(values,return_inverse=True);value=n.Compound({'palette':n.List[n.Compound]([palette[int(i)] for i in ids])})
    if len(ids)>1:value['data']=pack(inverse,max(4,(len(ids)-1).bit_length()))
    return value
mask=set(original)|{(cx,cz) for cx in range(-5,5) for cz in range(-5,5)};regions={}
for cx,cz in sorted(mask):
    chunk=copy.deepcopy(original.get((cx,cz),n.File({'DataVersion':n.Int(4556),'xPos':n.Int(cx),'zPos':n.Int(cz),'yPos':n.Int(-4),'Status':n.String('minecraft:full'),'structures':n.Compound({'starts':n.Compound({}),'References':n.Compound({})})})))
    sections=[s for s in chunk.get('sections',[]) if int(s['Y']) not in [4,5]]
    for sy in [4,5]:
        values=top[sy*16-LOW:sy*16-LOW+16,cz*16-MIN_Z:cz*16-MIN_Z+16,cx*16-MIN_X:cx*16-MIN_X+16]
        sections.append(n.Compound({'Y':n.Byte(sy),'block_states':states(values),'biomes':n.Compound({'palette':n.List[n.String]([n.String('minecraft:dark_forest')])})}))
    chunk['sections']=n.List[n.Compound](sorted(sections,key=lambda s:int(s['Y'])));chunk['isLightOn']=n.Byte(0)
    chunk['block_entities']=n.List[n.Compound]([e for e in entities if int(e['x'])//16==cx and int(e['z'])//16==cz]);chunk['block_ticks']=n.List[n.Compound]([]);chunk['fluid_ticks']=n.List[n.Compound]([])
    values=top[:,cz*16-MIN_Z:cz*16-MIN_Z+16,cx*16-MIN_X:cx*16-MIN_X+16];height=np.max(np.where(values!=AIR,np.arange(LOW,HIGH)[:,None,None],-65),axis=0)
    chunk['Heightmaps']=n.Compound({key:pack(height+65,9) for key in ['WORLD_SURFACE','MOTION_BLOCKING','MOTION_BLOCKING_NO_LEAVES','OCEAN_FLOOR']})
    buffer=io.BytesIO();chunk.write(buffer);compressed=zlib.compress(buffer.getvalue(),6);raw=struct.pack('>I',len(compressed)+1)+b'\x02'+compressed
    regions.setdefault((cx//32,cz//32),{})[cx%32+cz%32*32]=raw
(OUTPUT/'region').mkdir(parents=True,exist_ok=True);(OUTPUT/'data').mkdir(exist_ok=True)
for (rx,rz),chunks in regions.items():
    header=bytearray(8192);body=bytearray();sector=2
    for index,raw in sorted(chunks.items()):
        count=math.ceil(len(raw)/4096);header[index*4:index*4+4]=((sector<<8)|count).to_bytes(4,'big');body.extend(raw);body.extend(bytes(count*4096-len(raw)));sector+=count
    (OUTPUT/'region'/f'r.{rx}.{rz}.mca').write_bytes(header+body)
for relative in ['level.dat','data/world_border.dat']:(OUTPUT/relative).write_bytes((SOURCE/relative).read_bytes())
info=old_info;info['world']='vampirkoylu_sokaklar';info['duzen']='24 çift çıkışlı ev, dar S sokaklar, kapalı avlular';info['evler']=houses
(ROOT/'world-template/vampirkoylu_sokaklar-konumlar.json').write_text(json.dumps(info,ensure_ascii=False,indent=2)+'\n')
report={'canonicalChunks':len(mask),'houses':len(houses),'taskRooms':17,'reachableTaskRooms':17,'reachableHouseExits':48,'walkableBlocks':len(walk),'connectedWalkableBlocks':len(reachable),'longSightPairs':long_total,'unobstructedLongSightPairs':long_views}
(ROOT/'validation/village-layout.json').write_text(json.dumps(report,indent=2)+'\n')
colors={'air':(38,44,40),'spruce_planks':(115,82,46),'dark_oak_planks':(65,44,28),'dark_prismarine':(40,81,74),'stone_bricks':(122,126,126),'mossy_stone_bricks':(93,111,86),'deepslate_bricks':(57,63,70),'coarse_dirt':(126,98,66),'polished_andesite':(158,160,156),'cobblestone':(115,118,112),'mossy_cobblestone':(91,111,77)}
image=Image.new('RGB',(137,137));pixels=image.load()
for z in range(-68,69):
    for x in range(-68,69):
        non=np.flatnonzero(top[:,z-MIN_Z,x-MIN_X]!=AIR);y=LOW+int(non[-1]) if len(non) else LOW
        pixels[x+68,z+68]=colors.get(name_at(x,y,z),(145,135,106))
image=image.resize((822,822),Image.Resampling.NEAREST);draw=ImageDraw.Draw(image)
for i,(cx,cz) in enumerate(stations):
    x,z=(cx+2+68)*6,(cz+68)*6;draw.ellipse((x-7,z-7,x+7,z+7),fill=(255,214,45),outline=(255,255,255));draw.text((x+8,z-6),str(i+1),fill='white')
image.save(ROOT/'validation/village-preview.png')
# Render the actual generated block model for review; this is a schematic, not a client screenshot.
scale=4;iso=Image.new('RGB',(1160,760),(24,34,42));pen=ImageDraw.Draw(iso)
def project(x,y,z):return (580+(x-z)*scale,400+(x+z)*scale/2-(y-68)*scale)
def occupied(x,y,z):return -68<=x<=68 and -68<=z<=68 and 64<=y<HIGH and int(top[y-LOW,z-MIN_Z,x-MIN_X])!=AIR
def shade(color,factor):return tuple(max(0,min(255,int(v*factor))) for v in color)
voxel=[]
for z in range(-68,69):
 for x in range(-68,69):
  for y in range(64,87):
   if occupied(x,y,z) and (not occupied(x+1,y,z) or not occupied(x,y,z+1) or not occupied(x,y+1,z)):voxel.append((x,y,z))
for x,y,z in sorted(voxel,key=lambda p:p[0]+p[1]+p[2]):
 name=name_at(x,y,z);color=colors.get(name,(113,111,99))
 if name in ['lantern','glowstone']:color=(245,198,79)
 elif 'leaves' in name or name=='azalea':color=(73,105,61)
 elif name in ['stone','bedrock']:color=(74,77,79)
 elif name=='tinted_glass':color=(46,63,72)
 elif 'terracotta' in name:color={'brown_terracotta':(126,91,67),'gray_terracotta':(101,92,91),'light_gray_terracotta':(148,132,119)}.get(name,(125,112,99))
 if not occupied(x+1,y,z):pen.polygon([project(x+1,y,z),project(x+1,y,z+1),project(x+1,y+1,z+1),project(x+1,y+1,z)],fill=shade(color,.66))
 if not occupied(x,y,z+1):pen.polygon([project(x,y,z+1),project(x+1,y,z+1),project(x+1,y+1,z+1),project(x,y+1,z+1)],fill=shade(color,.83))
 if not occupied(x,y+1,z):pen.polygon([project(x,y+1,z),project(x+1,y+1,z),project(x+1,y+1,z+1),project(x,y+1,z+1)],fill=color)
pen.text((28,28),'KASABA | VAMPIR KOYLU - YENI KOY PLANI',fill=(234,213,174))
pen.text((28,49),'24 ev - 17 gorev odasi - iki cikis - dar sokaklar',fill=(170,185,191))
iso.save(ROOT/'validation/village-isometric.png');print(json.dumps(report,indent=2))
