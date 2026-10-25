#!/usr/bin/env python3
"""Four tested production JARs: new paint game, live HUD, role API and hub game entry."""
from pathlib import Path
import argparse,hashlib,json,re,zipfile
root=Path(__file__).resolve().parents[2];validation=root/'kasaba-boya/validation/1.1.0'
p=argparse.ArgumentParser();p.add_argument('--tested-server',type=Path,required=True);args=p.parse_args()
proof=json.loads((validation/'live-results.json').read_text());unit=json.loads((validation/'unit-results.json').read_text())
assert len(proof['checks'])>=11 and proof['restart'] and proof['finalArtifactsSmoke']
assert unit['summary']=={'tests':27,'failures':0,'errors':0}
modules={
 'KasabaBoya':('kasaba-boya/target/KasabaBoya-1.1.0.jar','1.1.0','tr/kasabatr/boya/'),
 'KasabaHub':('kasaba-hub/target/KasabaHub-1.6.2.jar','1.6.2','tr/kasaba/hub/'),
 'KasabaBilgi':('kasaba-bilgi/target/KasabaBilgi-1.0.0.jar','1.0.0','tr/kasaba/bilgi/'),
 'KasabaRoles':('kasaba-roles/target/KasabaRoles-1.3.2.jar','1.3.2','tr/kasaba/roles/')}
files={};manifest={}
for name,(relative,version,prefix) in modules.items():
 source=root/relative;data=source.read_bytes();sha=hashlib.sha256(data).hexdigest();assert data==(args.tested_server/'plugins'/source.name).read_bytes();assert sha==proof['artifactHashes'][source.name]
 with zipfile.ZipFile(source) as z:
  assert z.testzip() is None;meta=z.read('plugin.yml').decode();assert re.search(r'^name:\s*'+name+r'\s*$',meta,re.M);assert re.search(r'^version:\s*'+re.escape(version)+r'\s*$',meta,re.M)
  for member in z.namelist():
   assert 'Probe' not in member
   if member.endswith('.class'):assert member.startswith(prefix)
  if name=='KasabaBoya':
   base=root/'kasaba-boya/src/main/resources/map-template'
   for f in base.rglob('*'):
    if f.is_file():assert z.read('map-template/'+str(f.relative_to(base)))==f.read_bytes()
 if name in ['KasabaBilgi','KasabaRoles']:assert data==(root/'dist'/source.name).read_bytes(),'Previously published HUD changed'
 files['plugins/'+source.name]=data;manifest[name]={'version':version,'jar':source.name,'bytes':len(data),'sha256':sha}
files['KURULUM-VE-KOMUTLAR.txt']=(validation/'KURULUM-VE-KOMUTLAR.txt').read_bytes()
files['BOYA-KILAVUZ.md']=(root/'kasaba-boya/README.md').read_bytes()
files['BILGI-TABLOSU-KILAVUZ.md']=(root/'kasaba-bilgi/README.md').read_bytes()
files['TEST-SONUCLARI.json']=(validation/'live-results.json').read_bytes()
files['BIRIM-TESTLERI.json']=(validation/'unit-results.json').read_bytes()
files['HARITA-VE-GIRDI-SHA256.json']=(validation/'input-and-map-manifest.json').read_bytes()
for image in (root/'kasaba-boya/docs').glob('*.png'):files['ONIZLEMELER/'+image.name]=image.read_bytes()
files['SURUMLER.json']=(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n').encode()
files['SHA256SUMS.txt']=''.join(hashlib.sha256(data).hexdigest()+'  '+name+'\n' for name,data in files.items()).encode()
dist=root/'dist';stem='KasabaTR-Boya-ve-Bilgi-Hazir-Paket-1.1.0';archive=dist/(stem+'.zip');assert not archive.exists(),'Never overwrite a published archive'
for name in ['KasabaBoya','KasabaHub']:assert not (dist/Path(modules[name][0]).name).exists(),'Never overwrite a published JAR'
with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
 for name,data in files.items():z.writestr(name,data)
with zipfile.ZipFile(archive) as z:assert z.testzip() is None
for name in ['KasabaBoya','KasabaHub']:
 source=root/modules[name][0];(dist/source.name).write_bytes(source.read_bytes())
sha=hashlib.sha256(archive.read_bytes()).hexdigest();(dist/(stem+'-SHA256.txt')).write_text(sha+'  '+archive.name+'\n');(dist/(stem+'-KURULUM.txt')).write_bytes(files['KURULUM-VE-KOMUTLAR.txt'])
result={'archive':archive.name,'bytes':archive.stat().st_size,'sha256':sha,'plugins':manifest};(validation/'artifacts.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');print(json.dumps(result,ensure_ascii=False,indent=2))
