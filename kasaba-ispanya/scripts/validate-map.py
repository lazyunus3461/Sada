#!/usr/bin/env python3
"""Check the supplied Anvil assets and the exact coordinates consumed by the plugin.
Requires nbtlib and PyYAML; no writes to the supplied world.
"""
from pathlib import Path
import hashlib,io,json,zlib,zipfile
import nbtlib,yaml

root=Path(__file__).resolve().parents[1]
resources=root/'src/main/resources';world=resources/'map-template'
arena=json.loads((world/'arena.json').read_text());track=yaml.safe_load((resources/'track.yml').read_text())
assert len(arena['tiles'])==len(track['tiles'])==135
for index,(tile,row) in enumerate(zip(arena['tiles'],track['tiles'])):
    x,y,z,kind=row.split(',');assert tile==dict(index=index,x=float(x),y=float(y),z=float(z),kind=kind)
assert [(s['from'],s['to']) for s in arena['shortcuts']]==[tuple(map(int,row.split(','))) for row in track['shortcuts']]
assert track['shortcuts']==['9,32','54,79','108,117']
level=nbtlib.load(world/'level.dat')['Data'];assert int(level['DataVersion'])==4556
generator=level['WorldGenSettings']['dimensions']['minecraft:overworld']['generator']
assert str(generator['type'])=='minecraft:flat' and len(generator['settings']['layers'])==0
chunks={}
for path in sorted((world/'region').glob('*.mca')):
    data=path.read_bytes();assert len(data)%4096==0
    rx,rz=map(int,path.stem.split('.')[1:])
    for slot in range(1024):
        header=int.from_bytes(data[slot*4:slot*4+4],'big');offset=(header>>8)*4096
        if not offset:continue
        assert offset>=8192 and offset+(header&255)*4096<=len(data)
        length=int.from_bytes(data[offset:offset+4],'big');assert data[offset+4]==2
        chunk=nbtlib.File.parse(io.BytesIO(zlib.decompress(data[offset+5:offset+4+length])))
        cx,cz=rx*32+slot%32,rz*32+slot//32
        assert int(chunk['xPos'])==cx and int(chunk['zPos'])==cz
        assert int(chunk['DataVersion'])==4556;chunks[cx,cz]=chunk
        for section in chunk['sections']:
            states=section.get('block_states')
            if states:
                for state in states['palette']:assert str(state['Name']) not in ('minecraft:command_block','minecraft:repeating_command_block','minecraft:chain_command_block')
for tile in arena['tiles']:assert (int(tile['x']//16),int(tile['z']//16)) in chunks
assert len(chunks)==58
assets={str(p.relative_to(world)):hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(world.rglob('*')) if p.is_file()}
jar=root/'target/KasabaIspanya-0.4.0.jar'
with zipfile.ZipFile(jar) as archive:
    assert archive.testzip() is None
    for name,digest in assets.items():assert hashlib.sha256(archive.read('map-template/'+name)).hexdigest()==digest
    assert not any('Probe' in name or name.startswith('tr/kasaba/test/') for name in archive.namelist())
result={'chunks':len(chunks),'tiles':len(arena['tiles']),'shortcuts':arena['shortcuts'],'emptyNewChunkSettings':True,'sourceAssetHashes':assets,'embeddedMapMatches':True}
out=root/'target/map-validation.json';out.parent.mkdir(exist_ok=True);out.write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result,indent=2))
