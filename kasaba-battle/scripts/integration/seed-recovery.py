"""Loopback fixture, STOPPED disposable server only: seed a pending snapshot and chest journal.
Requires nbtlib, numpy and PyYAML. Never run against a real/player server.
"""
from pathlib import Path
import argparse,hashlib,io,json,math,struct,uuid,zlib
import numpy as np,nbtlib as n,yaml
p=argparse.ArgumentParser();p.add_argument('server',type=Path);a=p.parse_args();server=a.server
assert 'server-ip=127.0.0.1' in (server/'server.properties').read_text()
name=json.loads((Path(__file__).resolve().parents[2]/'target/restart-state.json').read_text())['name']
h=hashlib.md5(('OfflinePlayer:'+name).encode()).digest();uid=uuid.UUID(bytes=h,version=3)
folder=server/'plugins/BattleRoyalePrototype';snapshot=folder/'oyuncu-yedekleri'/f'{uid}.yml'
text=snapshot.read_text();assert text.startswith(('active: false','active: true'));assert 'mode: ADVENTURE' in text
snapshot.write_text(text.replace('active: false','active: true',1))
playerfile=server/'world/playerdata'/f'{uid}.dat';player=n.load(playerfile)
player['Inventory']=n.List[n.Compound]([n.Compound({'Slot':n.Byte(0),'id':n.String('minecraft:stone'),'count':n.Int(9)})]);player['playerGameType']=n.Int(0);player.save(playerfile,gzipped=True)
loc=yaml.safe_load((folder/'arenalar.yml').read_text())['arenalar']['1']['sandik-adaylari'][0]
x,y,z=map(lambda k:math.floor(loc[k]),('x','y','z'));cx,cz=x//16,z//16
path=server/'KasabaBR/region'/f'r.{cx//32}.{cz//32}.mca';raw=path.read_bytes();idx=cx%32+cz%32*32
records={}
for i in range(1024):
 off=int.from_bytes(raw[i*4:i*4+3],'big')*4096
 if off:
  length=int.from_bytes(raw[off:off+4],'big');records[i]=raw[off:off+4+length]
record=records[idx];chunk=n.File.parse(io.BytesIO(zlib.decompress(record[5:])));section=next(s for s in chunk['sections'] if int(s['Y'])==y//16);bs=section['block_states'];pal=bs['palette'];bits=max(4,(len(pal)-1).bit_length());per=64//bits
if len(pal)==1:values=np.zeros(4096,dtype=np.uint64)
else:
 pos=np.arange(4096,dtype=np.uint64);data=np.asarray(bs['data'],dtype=np.int64).view(np.uint64);values=(data[(pos//per).astype(np.int64)]>>((pos%per)*bits))&((1<<bits)-1)
i=(y%16)*256+(z%16)*16+x%16;assert str(pal[int(values[i])]['Name'])=='minecraft:air'
pal.append(n.Compound({'Name':n.String('minecraft:chest'),'Properties':n.Compound({'facing':n.String('north'),'type':n.String('single'),'waterlogged':n.String('false')})}));values[i]=len(pal)-1;bits=max(4,(len(pal)-1).bit_length());per=64//bits;words=np.zeros(math.ceil(4096/per),dtype=np.uint64)
for j in range(per):part=values[j::per];words[:len(part)]|=part<<np.uint64(j*bits)
bs['data']=n.LongArray(words.view(np.int64));chunk['block_entities']=n.List[n.Compound](list(chunk.get('block_entities',[]))+[n.Compound({'id':n.String('minecraft:chest'),'x':n.Int(x),'y':n.Int(y),'z':n.Int(z)})])
buf=io.BytesIO();chunk.write(buf);payload=zlib.compress(buf.getvalue());records[idx]=struct.pack('>I',len(payload)+1)+b'\x02'+payload
header=bytearray(raw[:8192]);body=bytearray();sector=2
for i,record in sorted(records.items()):
 count=math.ceil(len(record)/4096);header[i*4:i*4+3]=sector.to_bytes(3,'big');header[i*4+3]=count;body.extend(record);body.extend(bytes(count*4096-len(record)));sector+=count
path.write_bytes(header+body);(folder/'gecici-sandiklar.yml').write_text(yaml.safe_dump({'blocks':[loc]},allow_unicode=True))
state={'name':name,'uuid':str(uid),'position':[x,y,z]};(Path(__file__).resolve().parents[2]/'target/recovery-fixture.json').write_text(json.dumps(state));print('Seeded interrupted inventory/chest recovery:',state)
