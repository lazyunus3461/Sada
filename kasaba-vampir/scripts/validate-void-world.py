#!/usr/bin/env python3
"""Offline verification after the local Paper server has stopped. Requires nbtlib."""
from pathlib import Path
import io,zlib,json,sys
import nbtlib
root=Path(__file__).resolve().parents[1]
world=Path(sys.argv[1])
def chunks(path):
 for region in sorted((path/'region').glob('r.*.*.mca')):
  rx,rz=map(int,region.stem.split('.')[1:]);data=region.read_bytes()
  for i in range(1024):
   pointer=int.from_bytes(data[i*4:i*4+4],'big');sector=pointer>>8
   if not sector:continue
   offset=sector*4096;length=int.from_bytes(data[offset:offset+4],'big');kind=data[offset+4]
   assert kind==2,(region,kind)
   yield (rx*32+i%32,rz*32+i//32),nbtlib.File.parse(io.BytesIO(zlib.decompress(data[offset+5:offset+4+length])))
mask={pos for pos,_ in chunks(root/'world-template/KasabaTR-VampirKoylu-TamDunya')}
empty=0;kept=set()
for pos,chunk in chunks(world):
 if pos in mask:kept.add(pos);continue
 empty+=1
 for section in chunk['sections']:
  if 'block_states' not in section:continue
  assert all(str(s['Name']) in {'minecraft:air','minecraft:cave_air','minecraft:void_air'} for s in section['block_states']['palette']),(pos,section['Y'])
assert mask==kept and empty>0
print(json.dumps({'originalMapChunks':len(mask),'preservedMapChunks':len(kept),'verifiedEmptyOutsideChunks':empty,'naturalTerrainOutsideMap':False},indent=2))
