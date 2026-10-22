"""Compact the supplied, unbuilt 300-island template; never edit the input world.
Requires nbtlib and numpy. Creative builds must not be passed to this template converter.
"""
from pathlib import Path
from functools import lru_cache
import argparse,copy,gzip,io,json,math,struct,time,zlib
import numpy as np
import nbtlib as n

parser=argparse.ArgumentParser()
parser.add_argument('--input',type=Path,required=True)
parser.add_argument('--output',type=Path,required=True)
args=parser.parse_args()
source=args.input.resolve();output=args.output.resolve()
if source==output or output.is_relative_to(source):raise SystemExit('Input and output must be separate worlds')
if output.exists() and any(output.iterdir()):raise SystemExit('Output must be empty')
if (source/'entities').exists():raise SystemExit('This converter accepts the unbuilt supplied template only')
output.mkdir(parents=True,exist_ok=True);(output/'region').mkdir(exist_ok=True)
MIN_X=MIN_Z=-64;WIDTH=2016;DEPTH=1520;LOW=58;HIGH=96
palette=[];registry={}
def register(state):
    key=str(state['Name'])+json.dumps({str(k):str(v) for k,v in state.get('Properties',{}).items()},sort_keys=True)
    if key not in registry:registry[key]=len(palette);palette.append(copy.deepcopy(state))
    return registry[key]
def material(name):return register(n.Compound({'Name':n.String('minecraft:'+name)}))
AIR=material('air');WATER=material('water');STONE=material('stone');BEDROCK=material('bedrock');GRAVEL=material('gravel')
@lru_cache(maxsize=12)
def region(rx,rz):
    p=source/'region'/f'r.{rx}.{rz}.mca'
    return p.read_bytes() if p.exists() else None
@lru_cache(maxsize=90)
def chunk(cx,cz):
    raw=region(cx//32,cz//32)
    if raw is None:return {},[]
    idx=(cx%32+cz%32*32)*4;offset=int.from_bytes(raw[idx:idx+3],'big')*4096
    if offset==0:return {},[]
    length=int.from_bytes(raw[offset:offset+4],'big');compression=raw[offset+4];payload=raw[offset+5:offset+4+length]
    data=n.File.parse(io.BytesIO(zlib.decompress(payload) if compression==2 else gzip.decompress(payload)))
    sections={}
    for section in data.get('sections',[]):
        sy=int(section['Y']);states=section.get('block_states')
        if not states:continue
        ids=np.array([register(value) for value in states['palette']],dtype=np.uint16)
        if len(ids)==1:blocks=np.full((16,16,16),ids[0],dtype=np.uint16)
        else:
            bits=max(4,(len(ids)-1).bit_length());per=64//bits;values=np.asarray(states['data'],dtype=np.int64).view(np.uint64);positions=np.arange(4096,dtype=np.uint64)
            local=(values[(positions//per).astype(np.int64)]>>((positions%per)*bits))&((1<<bits)-1)
            blocks=ids[local.astype(np.int64)].reshape(16,16,16)
        if sy>=HIGH//16 and np.any(blocks!=AIR):raise ValueError('Template contains blocks above the supported height; original preserved')
        if sy*16+16>LOW and sy*16<HIGH:sections[sy]=blocks
    return sections,list(data.get('block_entities',[]))

top=np.full((HIGH-LOW,DEPTH,WIDTH),AIR,dtype=np.uint16);top[0]=GRAVEL;top[1:6]=WATER
block_entities={}
for island in range(1,301):
    col=(island-1)%20;row=(island-1)//20;ox=col*200;oz=row*200;nx=col*100;nz=row*100
    tile=np.full((HIGH-LOW,96,96),AIR,dtype=np.uint16);tile[0]=GRAVEL;tile[1:6]=WATER
    for cx in range((ox-48)//16,(ox+47)//16+1):
        for cz in range((oz-48)//16,(oz+47)//16+1):
            sections,entities=chunk(cx,cz);x1=max(ox-48,cx*16);x2=min(ox+48,cx*16+16);z1=max(oz-48,cz*16);z2=min(oz+48,cz*16+16)
            # Every existing section has explicit air above the seabed; preserve surface and decoration.
            for sy,blocks in sections.items():
                y1=max(LOW,sy*16);y2=min(HIGH,sy*16+16)
                tile[y1-LOW:y2-LOW,z1-(oz-48):z2-(oz-48),x1-(ox-48):x2-(ox-48)]=blocks[y1-sy*16:y2-sy*16,z1-cz*16:z2-cz*16,x1-cx*16:x2-cx*16]
            for entity in entities:
                x=int(entity['x']);z=int(entity['z'])
                if ox-48<=x<ox+48 and oz-48<=z<oz+48:
                    moved=copy.deepcopy(entity);moved['x']=n.Int(x-col*100);moved['z']=n.Int(z-row*100);key=(int(moved['x'])//16,int(moved['z'])//16);block_entities.setdefault(key,[]).append(moved)
    solid_ids=[i for i,state in enumerate(palette) if str(state['Name']).split(':')[-1] in {'stone','andesite','diorite','granite','dirt','grass_block','sand','gravel','clay','coarse_dirt','podzol'}]
    ground=np.isin(tile,solid_ids);height=np.max(np.where(ground,np.arange(HIGH-LOW)[:,None,None],-1),axis=0)
    under=np.arange(HIGH-LOW)[:,None,None]<=height[None,:,:]
    tile[under&((tile==AIR)|(tile==WATER))]=STONE
    top[:,nz-48-MIN_Z:nz+48-MIN_Z,nx-48-MIN_X:nx+48-MIN_X]=tile
    if island%50==0:print(f'Prepared {island}/300 surfaces',flush=True)

# Every land column is solid through the seabed; outside it is a continuous, five-block-deep ocean.
non_air=top!=AIR
heights=np.max(np.where(non_air,np.arange(LOW,HIGH)[:,None,None],LOW-1),axis=0)
assert all(int(heights[row*100-MIN_Z,col*100-MIN_X])>=65 for row in range(15) for col in range(20))

def pack(values,bits):
    flat=np.asarray(values,dtype=np.uint64).reshape(-1);per=64//bits;words=np.zeros(math.ceil(len(flat)/per),dtype=np.uint64)
    for index in range(per):
        part=flat[index::per];words[:len(part)]|=part<<np.uint64(index*bits)
    return n.LongArray(words.view(np.int64))
def block_states(blocks):
    ids,inverse=np.unique(blocks,return_inverse=True)
    value=n.Compound({'palette':n.List[n.Compound]([palette[int(i)] for i in ids])})
    if len(ids)>1:value['data']=pack(inverse,max(4,(len(ids)-1).bit_length()))
    return value
def section(sy,blocks):return n.Compound({'Y':n.Byte(sy),'block_states':block_states(blocks),'biomes':n.Compound({'palette':n.List[n.String]([n.String('minecraft:ocean')])})})
stone_section=block_states(np.full((16,16,16),STONE,dtype=np.uint16))
bottom=np.full((16,16,16),STONE,dtype=np.uint16);bottom[0]=BEDROCK
base=[section(-4,bottom)]+[n.Compound({'Y':n.Byte(sy),'block_states':stone_section,'biomes':n.Compound({'palette':n.List[n.String]([n.String('minecraft:ocean')])})}) for sy in range(-3,3)]
regions={};total=WIDTH//16*(DEPTH//16)
for j,cz in enumerate(range(MIN_Z//16,(MIN_Z+DEPTH)//16)):
    for cx in range(MIN_X//16,(MIN_X+WIDTH)//16):
        x=cx*16-MIN_X;z=cz*16-MIN_Z;sections=list(base)
        for sy in range(3,6):
            blocks=np.full((16,16,16),STONE if sy==3 else AIR,dtype=np.uint16);y1=max(LOW,sy*16);y2=min(HIGH,sy*16+16);blocks[y1-sy*16:y2-sy*16]=top[y1-LOW:y2-LOW,z:z+16,x:x+16]
            if sy==3 or np.any(blocks!=AIR):sections.append(section(sy,blocks))
        heightmap=pack(heights[z:z+16,x:x+16]+65,9)
        data=n.File({'DataVersion':n.Int(4556),'xPos':n.Int(cx),'yPos':n.Int(-4),'zPos':n.Int(cz),'Status':n.String('minecraft:full'),'LastUpdate':n.Long(0),'InhabitedTime':n.Long(0),'isLightOn':n.Byte(0),'sections':n.List[n.Compound](sections),'block_entities':n.List[n.Compound](block_entities.get((cx,cz),[])),'block_ticks':n.List[n.Compound]([]),'fluid_ticks':n.List[n.Compound]([]),'structures':n.Compound({'starts':n.Compound({}),'References':n.Compound({})}),'Heightmaps':n.Compound({key:heightmap for key in ['WORLD_SURFACE','MOTION_BLOCKING','MOTION_BLOCKING_NO_LEAVES','OCEAN_FLOOR']})})
        buffer=io.BytesIO();data.write(buffer);compressed=zlib.compress(buffer.getvalue(),6);payload=struct.pack('>I',len(compressed)+1)+b'\x02'+compressed;regions.setdefault((cx//32,cz//32),{})[cx%32+cz%32*32]=payload
    if j%20==0:print(f'Encoded chunk row {j+1}/{DEPTH//16}',flush=True)
for (rx,rz),chunks in regions.items():
    header=bytearray(8192);payload=bytearray();sector=2
    for idx,raw in sorted(chunks.items()):
        count=math.ceil(len(raw)/4096);assert count<256;header[idx*4:idx*4+3]=sector.to_bytes(3,'big');header[idx*4+3]=count;header[4096+idx*4:4100+idx*4]=int(time.time()).to_bytes(4,'big');payload.extend(raw);payload.extend(bytes(count*4096-len(raw)));sector+=count
    (output/'region'/f'r.{rx}.{rz}.mca').write_bytes(header+payload)
level=n.load(source/'level.dat');data=level['Data'];data['LevelName']=n.String('ada');data['GameType']=n.Int(1);data['Difficulty']=n.Byte(0);data['LastPlayed']=n.Long(int(time.time()*1000));data['WorldGenSettings']['dimensions']['minecraft:overworld']['generator']['settings']['layers'][1]['block']=n.String('minecraft:stone');data['GameRules']['doMobSpawning']=n.String('false');level.save(output/'level.dat',gzipped=True)
(output/'kasaba-layout.properties').write_text('version=1.3.0\nspacing=100\ncolumns=20\nrows=15\ncount=300\n',encoding='ascii')
(output/'kasaba-world-report.json').write_text(json.dumps({'islands':300,'old_center_spacing':200,'center_spacing':100,'claim_width':96,'chunks':total,'regions':len(regions),'solid_bottom':-64,'ocean_floor':58,'sea_surface':63,'center_ground_min':int(min(heights[row*100-MIN_Z,col*100-MIN_X] for row in range(15) for col in range(20))),'preserved_block_entities':sum(map(len,block_entities.values()))},indent=2))
print('World ready:',output,'chunks:',total,'regions:',len(regions),flush=True)
