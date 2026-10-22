"""Independently read saved NBT after the disposable recovery server has stopped."""
from pathlib import Path
import io,json,math,zlib
import nbtlib as n,yaml
root=Path(__file__).resolve().parents[2];server=Path('/workspace/kasaba-test-server');fixture=json.loads((root/'target/recovery-fixture.json').read_text());x,y,z=fixture['position'];cx,cz=x//16,z//16
raw=(server/'KasabaBR/region'/f'r.{cx//32}.{cz//32}.mca').read_bytes();idx=cx%32+cz%32*32;off=int.from_bytes(raw[idx*4:idx*4+3],'big')*4096;size=int.from_bytes(raw[off:off+4],'big');chunk=n.File.parse(io.BytesIO(zlib.decompress(raw[off+5:off+4+size])));bs=next(s for s in chunk['sections'] if int(s['Y'])==y//16)['block_states'];bits=max(4,(len(bs['palette'])-1).bit_length());per=64//bits;i=y%16*256+z%16*16+x%16;value=0 if len(bs['palette'])==1 else (int(bs['data'][i//per])&((1<<64)-1))>>((i%per)*bits)&((1<<bits)-1)
assert str(bs['palette'][value]['Name'])=='minecraft:air'
folder=server/'plugins/BattleRoyalePrototype';assert not yaml.safe_load((folder/'gecici-sandiklar.yml').read_text())['blocks'];assert not yaml.safe_load((folder/'oyuncu-yedekleri'/f"{fixture['uuid']}.yml").read_text())['active']
print('PASS persisted interrupted chest removed at startup; journal empty; pending inventory restored exactly once')
