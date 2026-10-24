"""Check boat-width clearance, reachable checkpoints, wall continuity and lobby containment."""
from pathlib import Path
import runpy,json,math
from collections import deque
model=runpy.run_path(str(Path(__file__).with_name('build-spaced-world.py')));chunks=model['chunks'];palette=model['palette']
def get(x,y,z):
 a=chunks.get((x//16,z//16),{}).get(y//16)
 return 'air' if a is None else str(palette[int(a[y%16,z%16,x%16])]['Name']).split(':')[-1]
proof={}
for mode,cx in [('boat',-370),('horse',370)]:
 def free(x,z):return get(x,64,z) in (['water'] if mode=='boat' else ['water','packed_mud','white_concrete','black_concrete']) and get(x,65,z) in (['air'] if mode=='boat' else ['air','oak_log']) and get(x,66,z)=='air'
 safe={(x,z) for x in range(cx-180,cx+181) for z in range(-90,91) if all(free(x+dx,z+dz) for dx in [-1,0,1] for dz in [-1,0,1])}
 start=(cx-75,80);assert start in safe
 visited={start};queue=deque([start])
 while queue:
  x,z=queue.popleft()
  for nx,nz in [(x+1,z),(x-1,z),(x,z+1),(x,z-1)]:
   if (nx,nz) in safe and (nx,nz) not in visited:visited.add((nx,nz));queue.append((nx,nz))
 for gate in [(cx+90,80),(cx+170,0),(cx+90,-80),(cx-90,-80),(cx-170,0),(cx-90,80)]:assert gate in visited,(mode,gate)
 assert not any((cx,z) in visited for z in range(-65,66)),mode
 proof[mode]={'reachable_with_3_by_3_clearance':len(visited),'all_six_checkpoints':True,'interior_shortcut':False}
# A standing player's reachable footprint cannot leave the enclosed lounge.
start=(0,9);visited={start};q=deque([start])
while q:
 x,z=q.popleft()
 for nx,nz in [(x+1,z),(x-1,z),(x,z+1),(x,z-1)]:
  if (nx,nz) not in visited and get(nx,64,nz)!='air' and get(nx,65,nz)=='air' and get(nx,66,nz)=='air':visited.add((nx,nz));q.append((nx,nz))
assert all(-28<x<28 and -24<z<24 for x,z in visited)
proof['lobby']={'enclosed':True,'connected_to_courses':False,'walkable':len(visited)}
out=Path('/workspace/scratch/race-v2-validation.json');out.write_text(json.dumps(proof,indent=2));print(json.dumps(proof,indent=2))
