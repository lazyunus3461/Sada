#!/usr/bin/env python3
"""Three real village areas and one shared lobby, surrounded entirely by void."""
from pathlib import Path
import copy, gzip, io, json, math, struct, zlib
import nbtlib as n
import numpy as np
from PIL import Image, ImageDraw

ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/'world-template/vampirkoylu_sokaklar'
OUTPUT=ROOT/'world-template/vampirkoylu_uc_oda'
original={}
for region in sorted((SOURCE/'region').glob('*.mca')):
    rx,rz=map(int,region.stem.split('.')[1:]);raw=region.read_bytes()
    for i in range(1024):
        sector=int.from_bytes(raw[i*4:i*4+4],'big')>>8
        if not sector:continue
        offset=sector*4096;length=int.from_bytes(raw[offset:offset+4],'big');assert raw[offset+4]==2
        original[rx*32+i%32,rz*32+i//32]=n.File.parse(io.BytesIO(zlib.decompress(raw[offset+5:offset+4+length])))

def decode(states):
    if len(states['palette'])==1:return np.zeros((16,16,16),dtype=np.uint16)
    bits=max(4,(len(states['palette'])-1).bit_length());per=64//bits
    words=np.asarray(states['data'],dtype=np.int64).view(np.uint64);p=np.arange(4096,dtype=np.uint64)
    return ((words[(p//per).astype(int)]>>((p%per)*bits))&((1<<bits)-1)).astype(np.uint16).reshape(16,16,16)

def pack(values,bits):
    values=np.asarray(values,dtype=np.uint64).reshape(-1);per=64//bits
    words=np.zeros(math.ceil(len(values)/per),dtype=np.uint64)
    for i in range(per):
        part=values[i::per];words[:len(part)]|=part<<np.uint64(i*bits)
    return n.LongArray(words.view(np.int64))

def encode(values,palette):
    ids,inverse=np.unique(values,return_inverse=True)
    state=n.Compound({'palette':n.List[n.Compound]([copy.deepcopy(palette[int(i)]) for i in ids])})
    if len(ids)>1:state['data']=pack(inverse,max(4,(len(ids)-1).bit_length()))
    return state

def allowed(x,z,lobby=False):
    return (-68<=x<=68 and -68<=z<=68) or (lobby and ((112<=x<=175 and -32<=z<=31) or (69<=x<=111 and -2<=z<=2)))

chunks={};nonempty_columns=0
for origin in [0,320,640]:
    for (cx,cz),source in original.items():
        if origin and not(-5<=cx<=4 and -5<=cz<=4):continue
        chunk=copy.deepcopy(source);chunk['xPos']=n.Int(cx+origin//16);chunk['zPos']=n.Int(cz)
        mask=np.array([[allowed(cx*16+x,cz*16+z,origin==0) for x in range(16)] for z in range(16)])
        height=np.full((16,16),-65,dtype=int)
        for section in chunk['sections']:
            if 'block_states' not in section:continue
            state=section['block_states'];palette=list(state['palette']);air=next((i for i,b in enumerate(palette) if str(b['Name'])=='minecraft:air'),None)
            if air is None:air=len(palette);palette.append(n.Compound({'Name':n.String('minecraft:air')}))
            values=decode(state);values[:,~mask]=air
            section['block_states']=encode(values,palette)
            filled=np.array([str(b['Name']) not in {'minecraft:air','minecraft:cave_air','minecraft:void_air'} for b in palette])[values]
            sy=int(section['Y'])*16
            height=np.maximum(height,np.max(np.where(filled,np.arange(sy,sy+16)[:,None,None],-65),axis=0))
        entities=[]
        for entry in chunk.get('block_entities',[]):
            if not allowed(int(entry['x']),int(entry['z']),origin==0):continue
            entry['x']=n.Int(int(entry['x'])+origin);entities.append(entry)
        chunk['block_entities']=n.List[n.Compound](entities)
        chunk['block_ticks']=n.List[n.Compound]([]);chunk['fluid_ticks']=n.List[n.Compound]([])
        chunk['structures']=n.Compound({'starts':n.Compound({}),'References':n.Compound({})})
        chunk['isLightOn']=n.Byte(0)
        chunk['Heightmaps']=n.Compound({k:pack(height+65,9) for k in ['WORLD_SURFACE','MOTION_BLOCKING','MOTION_BLOCKING_NO_LEAVES','OCEAN_FLOOR']})
        chunks[cx+origin//16,cz]=chunk;nonempty_columns+=int(np.sum(height>=-64))

regions={}
for (cx,cz),chunk in sorted(chunks.items()):
    buffer=io.BytesIO();chunk.write(buffer);compressed=zlib.compress(buffer.getvalue(),6)
    raw=struct.pack('>I',len(compressed)+1)+b'\x02'+compressed
    regions.setdefault((cx//32,cz//32),{})[cx%32+cz%32*32]=raw
(OUTPUT/'region').mkdir(parents=True,exist_ok=True);(OUTPUT/'data').mkdir(exist_ok=True)
for (rx,rz),entries in regions.items():
    header=bytearray(8192);body=bytearray();sector=2
    for index,raw in sorted(entries.items()):
        count=math.ceil(len(raw)/4096);header[index*4:index*4+4]=((sector<<8)|count).to_bytes(4,'big')
        body.extend(raw);body.extend(bytes(count*4096-len(raw)));sector+=count
    (OUTPUT/'region'/f'r.{rx}.{rz}.mca').write_bytes(header+body)

level=n.load(SOURCE/'level.dat');data=level['Data'];data['LevelName']=n.String('Kasaba Vampir Koylu - 3 Oda')
generator=data['WorldGenSettings']['dimensions']['minecraft:overworld']['generator']
assert str(generator['type'])=='minecraft:flat';generator['settings']['layers']=n.List[n.Compound]([])
generator['settings']['structure_overrides']=n.List[n.String]([])
data['WorldGenSettings']['generate_features']=n.Byte(0)
data['SpawnX']=n.Int(144);data['SpawnY']=n.Int(69);data['SpawnZ']=n.Int(0)
buffer=io.BytesIO();level.write(buffer);(OUTPUT/'level.dat').write_bytes(gzip.compress(buffer.getvalue(),mtime=0))
border=n.load(SOURCE/'data/world_border.dat');border['data']['center_x']=n.Double(320.5);border['data']['center_z']=n.Double(.5)
border['data']['size']=n.Double(800);border['data']['lerp_target']=n.Double(800)
buffer=io.BytesIO();border.write(buffer);(OUTPUT/'data/world_border.dat').write_bytes(gzip.compress(buffer.getvalue(),mtime=0))

info=json.loads((ROOT/'world-template/vampirkoylu_sokaklar-konumlar.json').read_text())
info['world']='vampirkoylu_uc_oda';info['rooms']=[{'id':101+i,'center':[x+.5,69,.5],'bounds':{'min':[x-68,64,-68],'max':[x+68,95,68]},'stations':[[x+s['konum'][0],s['konum'][1],s['konum'][2]] for s in info['gorev_istasyonlari']]} for i,x in enumerate([0,320,640])]
info['boyut']=[777,137];info['oyun_bolgesi']={'min':[-68,64,-68],'max':[708,95,68]};info['only_distributed_buildings']=True;info['outside']='void';info['bekleme_lobisi']['not']='Tek ortak lobi; oda 101, 102, 103 merkezleri 0, 320, 640.'
(ROOT/'world-template/vampirkoylu_uc_oda-konumlar.json').write_text(json.dumps(info,ensure_ascii=False,indent=2)+'\n')
image=Image.new('RGB',(1140,460),(22,29,37));draw=ImageDraw.Draw(image)
draw.text((25,20),'KASABA VAMPIR KOYLU | TEK DUNYA - 3 AYRI KOY + 1 ORTAK LOBI',fill=(245,214,110))
tile=Image.open(ROOT/'validation/village-preview.png').resize((320,320))
for i,origin in enumerate([0,320,640]):
    left=25+i*370;image.paste(tile,(left,62));draw.text((left,390),f'ODA {101+i} | MERKEZ: {origin} / 69 / 0',fill=(90,230,255))
draw.text((25,425),'Ortak lobi: 144.5 / 69 / 0.5 | Alanlar disinda yalniz bosluk vardir.',fill=(188,197,207))
image.save(ROOT/'validation/three-villages-preview.png')
report={'world':'vampirkoylu_uc_oda','rooms':3,'centers':[0,320,640],'lobbies':1,'chunks':len(chunks),'regions':len(regions),'nonemptyColumns':nonempty_columns,'onlyAllowedBuildFootprints':True,'naturalStructures':False}
(ROOT/'validation/three-villages-layout.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report,indent=2))
