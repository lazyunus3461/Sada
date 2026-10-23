#!/usr/bin/env python3
"""Package exactly the production JARs verified by the integrated server tests."""
from pathlib import Path
import argparse,hashlib,json,re,zipfile,gzip,struct

root=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser();parser.add_argument('--tested-server',type=Path,required=True);args=parser.parse_args()
dist=root/'dist';dist.mkdir(exist_ok=True);stem='KasabaTR-Sunucu-Paketi-1.0.0'
proof=json.loads((root/'kasaba-sosyal/validation/live-results.json').read_text())
assert proof['physicalReset'] and proof['creditAndVipReplayIdempotent']
modules={
 'KasabaAuth':('target/KasabaAuth-1.3.0.jar','1.3.0','tr/kasaba/auth/'),
 'KasabaHub':('kasaba-hub/target/KasabaHub-1.3.0.jar','1.3.0','tr/kasaba/hub/'),
 'KasabaRoles':('kasaba-roles/target/KasabaRoles-1.3.0.jar','1.3.0','tr/kasaba/roles/'),
 'KasabaAkce':('kasaba-akce/target/KasabaAkce-1.1.0.jar','1.1.0','tr/kasaba/akce/'),
 'KasabaSosyal':('kasaba-sosyal/target/KasabaSosyal-1.0.0.jar','1.0.0','tr/kasaba/social/'),
 'KasabaArena':('kasaba-arena/target/KasabaArena-1.3.0.jar','1.3.0','tr/kasaba/arena/'),
 'KasabaAdalar':('kasaba-adalar/target/KasabaAdalar-1.4.0.jar','1.4.0','tr/kasabatr/islands/'),
 'BattleRoyalePrototype':('kasaba-battle/target/KasabaBattleRoyale-1.1.0.jar','1.1.0','tr/kasaba/battle/'),
 'KasabaVampirKoylu':('kasaba-vampir/target/KasabaVampirKoylu-1.3.0.jar','1.3.0','tr/kasabatr/vampir/'),
}
files={};manifest={}
for name,(relative,version,package) in modules.items():
 source=root/relative;data=source.read_bytes();tested=args.tested_server/'plugins'/source.name
 assert data==tested.read_bytes(),f'{name}: tested JAR differs from production target'
 with zipfile.ZipFile(source) as archive:
  metadata=archive.read('plugin.yml').decode();assert re.search(r'^name:\s*'+re.escape(name)+r'\s*$',metadata,re.M);assert re.search(r'^version:\s*'+re.escape(version)+r'\s*$',metadata,re.M)
  assert archive.testzip() is None
  for member in archive.namelist():
   assert 'Probe' not in member and not member.startswith('tr/kasaba/test/'),member
   if member.endswith('.class'):assert member.startswith(package),(name,member)
  if name=='KasabaAdalar':
   for island in range(1,301):
    content=gzip.decompress(archive.read(f'reset-template/{island}.bin.gz'));assert struct.unpack('>iii',content[:12])==(0x4b415331,58,40);assert len(content)==12+40*96*96*2
  if name=='KasabaVampirKoylu':
   for p in (root/'kasaba-vampir/world-template/vampirkoylu_uc_oda').rglob('*'):
    if p.is_file():assert archive.read('map-template/'+str(p.relative_to(root/'kasaba-vampir/world-template/vampirkoylu_uc_oda')))==p.read_bytes()
 files['plugins/'+source.name]=data;manifest[name]={'version':version,'jar':source.name,'sha256':hashlib.sha256(data).hexdigest(),'bytes':len(data)}
files['KURULUM-VE-KOMUTLAR.txt']=(root/'kasaba-sosyal/README.md').read_bytes()
files['TEST-SONUCLARI.txt']=(root/'kasaba-sosyal/validation/test-results.txt').read_bytes()
files['SURUMLER-VE-SHA256.json']=(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n').encode()
files['SHA256SUMS.txt']=''.join(hashlib.sha256(data).hexdigest()+'  '+name+'\n' for name,data in files.items()).encode()
archive=dist/(stem+'.zip');assert not archive.exists(),'Do not overwrite a published release.'
with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as output:
 for name,data in files.items():output.writestr(name,data)
with zipfile.ZipFile(archive) as output:assert output.testzip() is None and len([n for n in output.namelist() if n.endswith('.jar')])==9
(dist/(stem+'-SHA256.txt')).write_text(hashlib.sha256(archive.read_bytes()).hexdigest()+'  '+archive.name+'\n')
(dist/(stem+'-KURULUM.txt')).write_bytes(files['KURULUM-VE-KOMUTLAR.txt'])
(dist/(stem+'-SURUMLER.json')).write_bytes(files['SURUMLER-VE-SHA256.json'])
print(json.dumps({'archive':str(archive),'bytes':archive.stat().st_size,'productionPlugins':len(manifest),'sha256':hashlib.sha256(archive.read_bytes()).hexdigest()},indent=2))
