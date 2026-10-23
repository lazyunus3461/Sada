#!/usr/bin/env python3
"""Export the supplied pristine islands, never a player's live world."""
from pathlib import Path
from functools import lru_cache
import io,zlib,gzip,struct
import nbtlib,numpy as np
root=Path(__file__).resolve().parents[1];source=root/'target/ada';output=root/'src/main/resources/reset-template';output.mkdir(exist_ok=True,parents=True)
palette=['minecraft:air'];registry={palette[0]:0}
def register(entry):
 name=str(entry['Name']);props=entry.get('Properties',{});name+=('['+','.join(str(k)+'='+str(v) for k,v in sorted(props.items()))+']') if props else ''
 if name not in registry:registry[name]=len(palette);palette.append(name)
 return registry[name]
@lru_cache(maxsize=16)
def region(x,z):return (source/'region'/f'r.{x}.{z}.mca').read_bytes()
@lru_cache(maxsize=256)
def chunk(x,z):
 d=region(x//32,z//32);i=x%32+z%32*32;offset=(int.from_bytes(d[i*4:i*4+4],'big')>>8)*4096;tile=np.zeros((40,16,16),dtype=np.uint16)
 if not offset:return tile
 n=int.from_bytes(d[offset:offset+4],'big');c=nbtlib.File.parse(io.BytesIO(zlib.decompress(d[offset+5:offset+4+n])))
 for section in c['sections']:
  sy=int(section['Y'])*16;states=section.get('block_states')
  if states is None or sy+16<=58 or sy>=98:continue
  names=np.asarray([register(e) for e in states['palette']],dtype=np.uint16)
  if len(names)==1:values=np.full((16,16,16),names[0],dtype=np.uint16)
  else:
   bits=max(4,(len(names)-1).bit_length());per=64//bits;p=np.arange(4096,dtype=np.uint64);words=np.asarray(states['data'],dtype=np.int64).view(np.uint64);indices=((words[(p//per).astype(int)]>>((p%per)*bits))&((1<<bits)-1)).astype(int);values=names[indices].reshape(16,16,16)
  lo,hi=max(58,sy),min(98,sy+16);tile[lo-58:hi-58]=values[lo-sy:hi-sy]
 return tile
for island in range(1,301):
 cx=(island-1)%20*100;cz=(island-1)//20*100;tile=np.zeros((40,96,96),dtype=np.uint16)
 for x in range((cx-48)//16,(cx+47)//16+1):
  for z in range((cz-48)//16,(cz+47)//16+1):
   lo,hi=max(cx-48,x*16),min(cx+48,x*16+16);zl,zh=max(cz-48,z*16),min(cz+48,z*16+16);tile[:,zl-cz+48:zh-cz+48,lo-cx+48:hi-cx+48]=chunk(x,z)[:,zl-z*16:zh-z*16,lo-x*16:hi-x*16]
 with gzip.open(output/f'{island}.bin.gz','wb') as f:f.write(struct.pack('>iii',0x4b415331,58,40)+tile.astype('>u2').tobytes())
assert len(palette)<65536
(output/'palette.txt').write_text('\n'.join(palette)+'\n')
print('Exported 300 pristine islands;',len(palette),'block states;',sum(p.stat().st_size for p in output.iterdir()),'bytes')
