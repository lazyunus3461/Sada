"""Independent checks of every written Anvil chunk and all 300 spawn columns."""
from pathlib import Path
import argparse,io,zlib,math
import nbtlib as n
import numpy as np
parser=argparse.ArgumentParser();parser.add_argument('world',type=Path);args=parser.parse_args();world=args.world
count=0;centers={}
def value(section,x,y,z):
 states=section['block_states'];palette=states['palette']
 if len(palette)==1:return str(palette[0]['Name'])
 bits=max(4,(len(palette)-1).bit_length());per=64//bits;index=(y%16)*256+(z%16)*16+x%16;word=int(states['data'][index//per])&((1<<64)-1);return str(palette[(word>>((index%per)*bits))&((1<<bits)-1)]['Name'])
for region in sorted((world/'region').glob('r.*.mca')):
 raw=region.read_bytes()
 for i in range(1024):
  offset=int.from_bytes(raw[i*4:i*4+3],'big')*4096
  if not offset:continue
  length=int.from_bytes(raw[offset:offset+4],'big');assert raw[offset+4]==2
  data=n.File.parse(io.BytesIO(zlib.decompress(raw[offset+5:offset+4+length])));sections={int(s['Y']):s for s in data['sections']};cx=int(data['xPos']);cz=int(data['zPos']);count+=1
  for sy in range(-4,3):
   assert sy in sections,(cx,cz,sy)
   names={str(s['Name']) for s in sections[sy]['block_states']['palette']}
   assert names<=({'minecraft:bedrock','minecraft:stone'} if sy==-4 else {'minecraft:stone'}),(cx,cz,sy,names)
  for row in range(15):
   z=row*100
   if z//16!=cz:continue
   for col in range(20):
    x=col*100
    if x//16!=cx:continue
    assert value(sections[4],x,65,z)=='minecraft:grass_block',(col,row,'surface')
    assert value(sections[4],x,66,z)=='minecraft:air',(col,row,'spawn head')
    for y in range(48,66):assert value(sections[y//16],x,y,z) not in {'minecraft:air','minecraft:water'},(col,row,y)
    centers[row*20+col+1]=(x,z)
assert len(centers)==300
assert count==11970
assert (world/'kasaba-layout.properties').read_text().find('spacing=100')>=0
level=n.load(world/'level.dat');assert str(level['Data']['WorldGenSettings']['dimensions']['minecraft:overworld']['generator']['settings']['layers'][1]['block'])=='minecraft:stone'
print('PASS',count,'Anvil chunks have a solid lower floor; 300 original grass spawn surfaces and filled island columns; 100-block spacing; solid fallback world generation')
