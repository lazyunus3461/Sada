#!/usr/bin/env python3
"""Package the exact 13 production JARs exercised by the 1.5.0 server suite."""
from pathlib import Path
import argparse,hashlib,json,re,zipfile
root=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser();parser.add_argument('--tested-server',type=Path,required=True);args=parser.parse_args()
stem='KasabaTR-Sunucu-Paketi-1.5.0';dist=root/'dist';archive=dist/(stem+'.zip');assert not archive.exists(),'Never overwrite a released download'
proof=json.loads((root/'validation/1.5.0/live-results.json').read_text())
assert len(proof['checks'])>=9 and proof['restart'] and proof['finalArtifactsSmoke']
assert proof['visibleFall']
assert proof['layout']['version']==3 and proof['layout']['oldWorldExists']
assert proof['map']['boat']['all_six_checkpoints'] and proof['map']['horse']['all_six_checkpoints'] and proof['map']['lobby']['enclosed']
assert proof['unit']['tests']==102 and proof['unit']['failures']==0 and proof['unit']['errors']==0
modules={
 'KasabaAuth':('target/KasabaAuth-1.3.0.jar','1.3.0','tr/kasaba/auth/'),
 'KasabaHub':('kasaba-hub/target/KasabaHub-1.6.0.jar','1.6.0','tr/kasaba/hub/'),
 'KasabaRoles':('kasaba-roles/target/KasabaRoles-1.3.1.jar','1.3.1','tr/kasaba/roles/'),
 'KasabaAkce':('kasaba-akce/target/KasabaAkce-1.1.0.jar','1.1.0','tr/kasaba/akce/'),
 'KasabaSosyal':('kasaba-sosyal/target/KasabaSosyal-1.2.0.jar','1.2.0','tr/kasaba/social/'),
 'KasabaArena':('kasaba-arena/target/KasabaArena-1.3.4.jar','1.3.4','tr/kasaba/arena/'),
 'KasabaAdalar':('kasaba-adalar/target/KasabaAdalar-1.4.2.jar','1.4.2','tr/kasabatr/islands/'),
 'BattleRoyalePrototype':('kasaba-battle/target/KasabaBattleRoyale-1.2.0.jar','1.2.0','tr/kasaba/battle/'),
 'KasabaVampirKoylu':('kasaba-vampir/target/KasabaVampirKoylu-1.3.4.jar','1.3.4','tr/kasabatr/vampir/'),
 'KasabaIspanya':('kasaba-ispanya/target/KasabaIspanya-0.8.0.jar','0.8.0','tr/kasabatr/ispanya/'),
 'KasabaRenk':('kasaba-renk/target/KasabaRenk-1.4.0.jar','1.4.0','tr/kasabatr/renk/'),
 'KasabaYaris':('kasaba-yaris/target/KasabaYaris-1.4.0.jar','1.4.0','tr/kasabatr/yaris/'),
 'KasabaHapis':('kasaba-hapis/target/KasabaHapis-1.2.1.jar','1.2.1','tr/kasabatr/prison/'),
}
files={};manifest={};standalone=[]
for name,(relative,version,prefix) in modules.items():
 source=root/relative;data=source.read_bytes();assert data==(args.tested_server/'plugins'/source.name).read_bytes(),f'Untested artifact: {name}'
 with zipfile.ZipFile(source) as z:
  assert z.testzip() is None
  meta=z.read('plugin.yml').decode();assert re.search(r'^name:\s*'+re.escape(name)+r'\s*$',meta,re.M);assert re.search(r'^version:\s*'+re.escape(version)+r'\s*$',meta,re.M)
  for member in z.namelist():
   assert 'Probe' not in member and not member.startswith('tr/kasaba/test/')
   if member.endswith('.class'):assert member.startswith(prefix),member
  if name in ['KasabaYaris','KasabaHapis','KasabaIspanya']:
   folder=source.parent.parent/'src/main/resources'
   for p in (folder/'map-template').rglob('*'):
    if p.is_file():assert z.read('map-template/'+str(p.relative_to(folder/'map-template')))==p.read_bytes()
  if name=='KasabaYaris':
   for p in (folder/'map-v2').rglob('*'):
    if p.is_file():assert z.read('map-v2/'+str(p.relative_to(folder/'map-v2')))==p.read_bytes()
 files['plugins/'+source.name]=data
 manifest[name]={'version':version,'jar':source.name,'bytes':len(data),'sha256':hashlib.sha256(data).hexdigest()}
 if name not in ['KasabaAuth','KasabaAkce','KasabaRoles']:standalone.append((dist/source.name,data))
files['KURULUM-VE-KOMUTLAR.txt']=(root/'validation/1.5.0/KURULUM-VE-KOMUTLAR.txt').read_bytes()
files['TEST-SONUCLARI.txt']=(root/'validation/1.5.0/TEST-SONUCLARI.txt').read_bytes()
files['BELGELER/map-validation.json']=(root/'validation/1.5.0/map-validation.json').read_bytes()
files['BELGELER/test-results.json']=(root/'validation/1.5.0/live-results.json').read_bytes()
for module in ['hub','arena','battle','vampir','yaris','hapis','ispanya','renk','sosyal','roles','adalar']:
 files['BELGELER/'+module+'-KILAVUZ.md']=(root/('kasaba-'+module)/'README.md').read_bytes()
files['SURUMLER-VE-SHA256.json']=(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n').encode()
files['SHA256SUMS.txt']=''.join(hashlib.sha256(data).hexdigest()+'  '+name+'\n' for name,data in files.items()).encode()
assert len(manifest)==13
with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
 for name,data in files.items():z.writestr(name,data)
with zipfile.ZipFile(archive) as z:assert z.testzip() is None and len([n for n in z.namelist() if n.endswith('.jar')])==13
for dest,data in standalone:
 if dest.exists():assert dest.read_bytes()==data,'Never overwrite a previously released JAR'
 else:dest.write_bytes(data)
sha=hashlib.sha256(archive.read_bytes()).hexdigest()
(dist/(stem+'-SHA256.txt')).write_text(sha+'  '+archive.name+'\n')
(dist/(stem+'-KURULUM.txt')).write_bytes(files['KURULUM-VE-KOMUTLAR.txt'])
(dist/(stem+'-SURUMLER.json')).write_bytes(files['SURUMLER-VE-SHA256.json'])
(root/'validation/1.5.0/artifacts.json').write_text(json.dumps({'archive':archive.name,'bytes':archive.stat().st_size,'sha256':sha,'plugins':manifest},ensure_ascii=False,indent=2)+'\n')
print(json.dumps({'archive':str(archive),'bytes':archive.stat().st_size,'sha256':sha,'plugins':13},indent=2))
