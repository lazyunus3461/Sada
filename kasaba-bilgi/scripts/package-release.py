#!/usr/bin/env python3
"""Package the exact tested HUD and additive role-service update; no old artifact is overwritten."""
from pathlib import Path
import argparse,hashlib,json,re,zipfile
root=Path(__file__).resolve().parents[2];validation=root/'kasaba-bilgi/validation/1.0.0'
parser=argparse.ArgumentParser();parser.add_argument('--tested-server',type=Path,required=True);args=parser.parse_args()
proof=json.loads((validation/'live-results.json').read_text());unit=json.loads((validation/'unit-results.json').read_text())
assert len(proof['checks'])>=10 and proof['restart'] and proof['finalArtifactsSmoke']
assert unit['summary']['tests']==24 and unit['summary']['failures']==unit['summary']['errors']==0
sources={
 'KasabaBilgi':('kasaba-bilgi/target/KasabaBilgi-1.0.0.jar','1.0.0','tr/kasaba/bilgi/'),
 'KasabaRoles':('kasaba-roles/target/KasabaRoles-1.3.2.jar','1.3.2','tr/kasaba/roles/')}
files={};manifest={}
for name,(relative,version,prefix) in sources.items():
 p=root/relative;data=p.read_bytes();assert data==(args.tested_server/'plugins'/p.name).read_bytes();sha=hashlib.sha256(data).hexdigest();assert sha==proof['artifactHashes'][p.name]
 with zipfile.ZipFile(p) as z:
  assert z.testzip() is None;meta=z.read('plugin.yml').decode();assert re.search(r'^name:\s*'+name+r'\s*$',meta,re.M);assert re.search(r'^version:\s*'+re.escape(version)+r'\s*$',meta,re.M)
  for member in z.namelist():
   assert 'Probe' not in member
   if member.endswith('.class'):assert member.startswith(prefix),member
 files['plugins/'+p.name]=data;manifest[name]={'version':version,'jar':p.name,'bytes':len(data),'sha256':sha}
files['KURULUM.txt']=(validation/'KURULUM.txt').read_bytes()
files['KILAVUZ.md']=(root/'kasaba-bilgi/README.md').read_bytes()
files['TEST-SONUCLARI.json']=(validation/'live-results.json').read_bytes()
files['BIRIM-TESTLERI.json']=(validation/'unit-results.json').read_bytes()
files['SURUMLER.json']=(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n').encode()
files['SHA256SUMS.txt']=''.join(hashlib.sha256(data).hexdigest()+'  '+name+'\n' for name,data in files.items()).encode()
dist=root/'dist';stem='KasabaTR-Bilgi-Tablosu-1.0.0';archive=dist/(stem+'.zip');assert not archive.exists(),'Never overwrite a published ZIP'
for relative,_,_ in sources.values():assert not (dist/Path(relative).name).exists(),'Never overwrite a published JAR'
with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
 for name,data in files.items():z.writestr(name,data)
with zipfile.ZipFile(archive) as z:assert z.testzip() is None
for relative,_,_ in sources.values():p=root/relative;(dist/p.name).write_bytes(p.read_bytes())
sha=hashlib.sha256(archive.read_bytes()).hexdigest();(dist/(stem+'-SHA256.txt')).write_text(sha+'  '+archive.name+'\n');(dist/(stem+'-KURULUM.txt')).write_bytes(files['KURULUM.txt'])
result={'archive':archive.name,'bytes':archive.stat().st_size,'sha256':sha,'plugins':manifest};(validation/'artifacts.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');print(json.dumps(result,ensure_ascii=False,indent=2))
