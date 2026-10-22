"""Independent read-back checks for prepared Anvil data and every gameplay candidate."""
from pathlib import Path
from functools import lru_cache
import argparse,io,json,zlib
import nbtlib as n
import numpy as np
p=argparse.ArgumentParser();p.add_argument('world',type=Path);p.add_argument('--original',type=Path,required=True);a=p.parse_args()
@lru_cache(maxsize=16)
def region(root,rx,rz):return (Path(root)/'region'/f'r.{rx}.{rz}.mca').read_bytes()
@lru_cache(maxsize=256)
def chunk(root,cx,cz):
 raw=region(root,cx//32,cz//32);idx=(cx%32+cz%32*32)*4;off=int.from_bytes(raw[idx:idx+3],'big')*4096
 assert off;length=int.from_bytes(raw[off:off+4],'big');return n.File.parse(io.BytesIO(zlib.decompress(raw[off+5:off+4+length])))
def block(x,y,z):
 c=chunk(str(a.world),x//16,z//16);section=next((s for s in c['sections'] if int(s['Y'])==y//16),None)
 if section is None:return 'minecraft:air'
 states=section['block_states'];palette=states['palette']
 if len(palette)==1:return str(palette[0]['Name'])
 bits=max(4,(len(palette)-1).bit_length());per=64//bits;index=y%16*256+z%16*16+x%16;word=int(states['data'][index//per])&((1<<64)-1);idx=(word>>((index%per)*bits))&((1<<bits)-1);return str(palette[idx]['Name'])
profile=json.loads((a.world/'kasaba-br-map.yml').read_text());checked=0
for data in profile['arenas'].values():
 for text in data['spawns']+data['candidates']:
  x,y,z=map(float,text.split('|'));x,y,z=int(np.floor(x)),int(y),int(np.floor(z));assert block(x,y,z)=='minecraft:air',(x,y,z);assert block(x,y+1,z)=='minecraft:air',(x,y+1,z);assert block(x,y-1,z) not in {'minecraft:air','minecraft:water','minecraft:lava','minecraft:spruce_leaves','minecraft:oak_leaves'},(x,y,z);checked+=1
# Every original upper section is preserved in every translated copy.
compared=0
for cx in range(-16,16):
 for cz in range(-16,16):
  original=chunk(str(a.original),cx,cz)
  for shift in [0,48,96]:
   c=chunk(str(a.world),cx+shift,cz);assert int(c['xPos'])==cx+shift and int(c['zPos'])==cz
   for section in original['sections']:
    sy=int(section['Y'])
    if sy<4:continue
    other=next(s for s in c['sections'] if int(s['Y'])==sy)
    assert section['block_states']['palette']==other['block_states']['palette']
    assert np.array_equal(section['block_states'].get('data',[]),other['block_states'].get('data',[]))
    compared+=1
   # Filled underside in original air-layer sections and below the sea floor.
   for y in [-63,-1,47,48,57]:assert block((cx+shift)*16,y,cz*16)=='minecraft:stone'
lx,ly,lz=map(float,profile['lobby']['position'].split('|'));assert block(int(lx),int(ly),int(np.floor(lz)))=='minecraft:air';assert block(int(lx),int(ly)-1,int(np.floor(lz)))=='minecraft:spruce_planks'
border=n.load(a.world/'data/world_border.dat')['data'];assert float(border['size'])==4096
assert not (a.world/'playerdata').exists() and not (a.world/'uid.dat').exists()
print(f'PASS {checked} safe gameplay locations, {compared} preserved upper sections across three islands, filled underside, safe lobby, expanded global border and no player/test data')
