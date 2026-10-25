"""Convert the supplied pre-1.21.9 chain block ID to the Paper 1.21.10 iron_chain ID.
Only the embedded release template is modified. No live server worlds are opened.
Requires nbtlib. Unchanged compressed chunks are retained verbatim.
"""
from pathlib import Path
import io,zlib
import nbtlib
root=Path(__file__).resolve().parents[1]/'src/main/resources/map-template'
changed=0
for f in root.rglob('*.mca'):
 data=f.read_bytes();header=bytearray(data[:8192]);chunks=[];offset=2;local=0
 for index in range(1024):
  start=int.from_bytes(data[index*4:index*4+3],'big')*4096
  if not start:continue
  length=int.from_bytes(data[start:start+4],'big');kind=data[start+4];assert kind==2
  raw=data[start+5:start+4+length];nbt=nbtlib.File.parse(io.BytesIO(zlib.decompress(raw)));dirty=False
  for section in nbt.get('sections',[]):
   for p in section.get('block_states',{}).get('palette',[]):
    if str(p['Name'])=='minecraft:chain':p['Name']=nbtlib.String('minecraft:iron_chain');dirty=True;changed+=1;local+=1
  if dirty:
   out=io.BytesIO();nbt.write(out);raw=zlib.compress(out.getvalue());payload=(len(raw)+1).to_bytes(4,'big')+bytes([kind])+raw
  else:payload=data[start:start+4+length]
  sectors=(len(payload)+4095)//4096;assert sectors<256
  header[index*4:index*4+4]=offset.to_bytes(3,'big')+bytes([sectors]);chunks.append(payload+b'\0'*(sectors*4096-len(payload)));offset+=sectors
 if local:f.write_bytes(header+b''.join(chunks));print(f.name,'fixed palette entries',local)
print('Total fixed',changed)
