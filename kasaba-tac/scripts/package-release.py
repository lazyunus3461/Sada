#!/usr/bin/env python3
"""Five exact, tested production JARs. Never overwrite a published download."""
from pathlib import Path
import argparse,hashlib,json,re,zipfile
root=Path(__file__).resolve().parents[2];proofdir=root/'kasaba-tac/validation/1.1.0'
p=argparse.ArgumentParser();p.add_argument('--tested-server',type=Path,required=True);args=p.parse_args()
proof=json.loads((proofdir/'live-results.json').read_text());unit=json.loads((proofdir/'unit-results.json').read_text());esya=json.loads((root/'kasaba-esya/validation/1.3.2/live-results.json').read_text())
assert len(proof['checks'])>=13 and proof['restart'] and proof['finalArtifactsSmoke'];assert len(esya['checks'])>=15 and esya['restart'];assert unit['summary']==dict(tests=34,failures=0,errors=0)
modules={
'KasabaTac':('kasaba-tac/target/KasabaTac-1.1.0.jar','1.1.0','tr/kasabatr/tac/'),
'KasabaHub':('kasaba-hub/target/KasabaHub-1.6.3.jar','1.6.3','tr/kasaba/hub/'),
'KasabaEsya':('kasaba-esya/target/KasabaEsya-1.3.2.jar','1.3.2','tr/kasabatr/esya/'),
'KasabaBilgi':('kasaba-bilgi/target/KasabaBilgi-1.0.1.jar','1.0.1','tr/kasaba/bilgi/'),
'KasabaRoles':('kasaba-roles/target/KasabaRoles-1.3.2.jar','1.3.2','tr/kasaba/roles/')}
files={};manifest={}
for name,(relative,version,prefix) in modules.items():
 source=root/relative;data=source.read_bytes();sha=hashlib.sha256(data).hexdigest();assert data==(args.tested_server/'plugins'/source.name).read_bytes();assert sha==proof['artifactHashes'][source.name]
 with zipfile.ZipFile(source) as z:
  assert z.testzip() is None;meta=z.read('plugin.yml').decode();assert re.search(r'^name:\s*'+name+r'\s*$',meta,re.M);assert re.search(r'^version:\s*'+re.escape(version)+r'\s*$',meta,re.M)
  for member in z.namelist():
   assert 'Probe' not in member
   if member.endswith('.class'):assert member.startswith(prefix)
  if name in ['KasabaTac','KasabaEsya']:
   base=root/relative.split('/')[0]/'src/main/resources/map-template'
   for f in base.rglob('*'):
    if f.is_file():assert z.read('map-template/'+str(f.relative_to(base)))==f.read_bytes()
 if name=='KasabaRoles':assert data==(root/'dist'/source.name).read_bytes(),'Previously published role JAR changed'
 files['plugins/'+source.name]=data;manifest[name]=dict(version=version,jar=source.name,bytes=len(data),sha256=sha)
for out,path in {'KURULUM-VE-KOMUTLAR.txt':proofdir/'KURULUM-VE-KOMUTLAR.txt','LANETLI-TAC-KILAVUZ.md':root/'kasaba-tac/README.md','SAKLAMBAC-KILAVUZ.md':root/'kasaba-esya/README.md','BILGI-TABLOSU-KILAVUZ.md':root/'kasaba-bilgi/README.md','TEST-SONUCLARI.json':proofdir/'live-results.json','BIRIM-TESTLERI.json':proofdir/'unit-results.json','SAKLAMBAC-REGRESYON.json':root/'kasaba-esya/validation/1.3.2/live-results.json','HARITA-VE-GIRDI-SHA256.json':proofdir/'input-and-map-manifest.json'}.items():files[out]=path.read_bytes()
for image in (root/'kasaba-tac/docs').glob('*.png'):files['ONIZLEMELER/'+image.name]=image.read_bytes()
files['SURUMLER.json']=(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n').encode();files['SHA256SUMS.txt']=''.join(hashlib.sha256(data).hexdigest()+'  '+name+'\n' for name,data in files.items()).encode()
stem='KasabaTR-Lanetli-Tac-ve-Duzeltmeler-1.1.0';dist=root/'dist';archive=dist/(stem+'.zip');assert not archive.exists()
for name in modules:
 if name!='KasabaRoles':assert not (dist/Path(modules[name][0]).name).exists()
with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
 for name,data in files.items():z.writestr(name,data)
with zipfile.ZipFile(archive) as z:assert z.testzip() is None
for name in modules:
 if name!='KasabaRoles':source=root/modules[name][0];(dist/source.name).write_bytes(source.read_bytes())
sha=hashlib.sha256(archive.read_bytes()).hexdigest();(dist/(stem+'-SHA256.txt')).write_text(sha+'  '+archive.name+'\n');(dist/(stem+'-KURULUM.txt')).write_bytes(files['KURULUM-VE-KOMUTLAR.txt'])
result=dict(archive=archive.name,bytes=archive.stat().st_size,sha256=sha,plugins=manifest);(proofdir/'artifacts.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');print(json.dumps(result,ensure_ascii=False,indent=2))
