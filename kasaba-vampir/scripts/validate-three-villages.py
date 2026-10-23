#!/usr/bin/env python3
"""Independently verify the world contains only three identical villages and their lobby."""
from pathlib import Path
import io,json,zlib,hashlib
import nbtlib
import numpy as np

root=Path(__file__).resolve().parents[1]
world=root/'world-template/vampirkoylu_uc_oda'
origins=[0,320,640];builds=[np.zeros((32,137,137),dtype=np.uint16) for _ in origins]
registry={'minecraft:air':0};chunks=0;outside=0;filled=0
for region in sorted((world/'region').glob('*.mca')):
    rx,rz=map(int,region.stem.split('.')[1:]);raw=region.read_bytes()
    for i in range(1024):
        sector=int.from_bytes(raw[i*4:i*4+4],'big')>>8
        if not sector:continue
        offset=sector*4096;length=int.from_bytes(raw[offset:offset+4],'big');assert raw[offset+4]==2
        chunk=nbtlib.File.parse(io.BytesIO(zlib.decompress(raw[offset+5:offset+4+length])))
        cx,cz=rx*32+i%32,rz*32+i//32;chunks+=1
        assert int(chunk['xPos'])==cx and int(chunk['zPos'])==cz
        assert not chunk['structures']['starts'] and not chunk['structures']['References']
        xs=np.arange(cx*16,cx*16+16);zs=np.arange(cz*16,cz*16+16)
        allowed=np.zeros((16,16),dtype=bool)
        for origin in origins:allowed|=(np.abs(zs[:,None])<=68)&(np.abs(xs[None,:]-origin)<=68)
        allowed|=((zs[:,None]>=-32)&(zs[:,None]<=31))&((xs[None,:]>=112)&(xs[None,:]<=175))
        allowed|=(np.abs(zs[:,None])<=2)&((xs[None,:]>=69)&(xs[None,:]<=111))
        for section in chunk['sections']:
            if 'block_states' not in section:continue
            state=section['block_states'];names=[]
            for entry in state['palette']:
                name=str(entry['Name']);key='minecraft:air' if name in {'minecraft:air','minecraft:cave_air','minecraft:void_air'} else json.dumps(entry.unpack(),sort_keys=True)
                if key not in registry:registry[key]=len(registry)
                names.append(registry[key])
            if len(names)==1:values=np.full((16,16,16),names[0],dtype=np.uint16)
            else:
                bits=max(4,(len(names)-1).bit_length());per=64//bits;positions=np.arange(4096,dtype=np.uint64)
                words=np.asarray(state['data'],dtype=np.int64).view(np.uint64)
                indices=((words[(positions//per).astype(int)]>>((positions%per)*bits))&((1<<bits)-1)).astype(int)
                values=np.asarray(names,dtype=np.uint16)[indices].reshape(16,16,16)
            outside+=int(np.count_nonzero(values[:,~allowed]));filled+=int(np.count_nonzero(values))
            sy=int(section['Y'])*16
            if sy not in [64,80]:continue
            for origin,build in zip(origins,builds):
                x1,x2=max(cx*16,origin-68),min(cx*16+16,origin+69)
                z1,z2=max(cz*16,-68),min(cz*16+16,69)
                if x1<x2 and z1<z2:build[sy-64:sy-48,z1+68:z2+68,x1-origin+68:x2-origin+68]=values[:,z1-cz*16:z2-cz*16,x1-cx*16:x2-cx*16]
assert outside==0,outside
assert chunks==320,chunks
assert all(np.array_equal(builds[0],build) for build in builds[1:]),'Village copies differ'
inverse={v:k for k,v in registry.items()}
for build in builds:
    assert 'minecraft:bell' in inverse[int(build[6,68,68])]
    assert np.all(build[4]>0),'Village floor missing'
level=nbtlib.load(world/'level.dat')['Data']
generator=level['WorldGenSettings']['dimensions']['minecraft:overworld']['generator']
assert str(generator['type'])=='minecraft:flat' and not generator['settings']['layers']
assert not generator['settings']['structure_overrides'] and not int(level['WorldGenSettings']['generate_features'])
report={'world':world.name,'chunks':chunks,'identicalVillageCopies':3,'centers':origins,'blocksOutsideBuildFootprints':outside,'builtBlocks':filled,'emptyGenerator':True,'naturalStructures':False,'normalizedVillageSha256':hashlib.sha256(builds[0].tobytes()).hexdigest()}
(root/'validation/three-villages-scan.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report,indent=2))
