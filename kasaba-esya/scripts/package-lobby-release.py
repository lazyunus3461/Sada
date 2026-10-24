#!/usr/bin/env python3
"""Immutable 1.3.1 lobby update; no previous download is overwritten."""
from pathlib import Path
import argparse,hashlib,json,re,zipfile
root=Path(__file__).resolve().parents[2]
parser=argparse.ArgumentParser();parser.add_argument('--tested-server',type=Path,required=True);args=parser.parse_args()
validation=root/'kasaba-esya/validation/1.3.1';proof=json.loads((validation/'live-results.json').read_text())
assert len(proof['checks'])>=15 and proof['restart'] and proof['finalArtifactsSmoke'] and proof['lobbyEditPreserved']
assert proof['lobby']['world']=='KasabaTR-Esya-Lobi-v01' and proof['lobby']['templatesSeparate']
assert proof['lobby']['floor'] and proof['lobby']['walls'] and proof['lobby']['boards']==2 and proof['lobby']['outside']=='AIR'
assert proof['unit']['failures']==proof['unit']['errors']==0
sources={
 'KasabaEsya':root/'kasaba-esya/target/KasabaEsya-1.3.1.jar',
 'KasabaHub':root/'kasaba-hub/target/KasabaHub-1.6.1.jar',
}
files={};manifest={}
for name,source in sources.items():
 data=source.read_bytes();assert data==(args.tested_server/'plugins'/source.name).read_bytes();sha=hashlib.sha256(data).hexdigest();assert sha==proof['artifactHashes'][source.name]
 with zipfile.ZipFile(source) as z:
  assert z.testzip() is None
  for member in z.namelist():
   assert 'Probe' not in member
   if member.endswith('.class'):assert member.startswith('tr/kasabatr/esya/' if name=='KasabaEsya' else 'tr/kasaba/hub/')
  meta=z.read('plugin.yml').decode();version='1.3.1' if name=='KasabaEsya' else '1.6.1';assert re.search(r'^version:\s*'+re.escape(version)+r'\s*$',meta,re.M)
  if name=='KasabaEsya':
   assert 'tr/kasabatr/esya/LobbyWorld.class' in z.namelist()
   with zipfile.ZipFile(root/'dist/KasabaEsya-1.3.0.jar') as original:
    for member in original.namelist():
     if member.startswith('map-template/') and not member.endswith('/'):assert z.read(member)==original.read(member),'Old game map changed'
  else:assert data==(root/'dist/KasabaHub-1.6.1.jar').read_bytes()
 files['plugins/'+source.name]=data;manifest[name]={'version':version,'jar':source.name,'bytes':len(data),'sha256':sha}
files['KURULUM.txt']=(validation/'KURULUM.txt').read_bytes()
files['KILAVUZ.md']=(root/'kasaba-esya/README.md').read_bytes()
files['TEST-SONUCLARI.json']=(validation/'live-results.json').read_bytes()
files['SURUMLER-VE-SHA256.json']=(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n').encode()
files['SHA256SUMS.txt']=''.join(hashlib.sha256(data).hexdigest()+'  '+name+'\n' for name,data in files.items()).encode()
dist=root/'dist';stem='KasabaEsya-KasabaTR-Hazir-Paket-1.3.1';archive=dist/(stem+'.zip');jar=dist/'KasabaEsya-1.3.1.jar'
assert not archive.exists() and not jar.exists(),'Never overwrite a published artifact'
with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
 for name,data in files.items():z.writestr(name,data)
with zipfile.ZipFile(archive) as z:assert z.testzip() is None
jar.write_bytes(sources['KasabaEsya'].read_bytes());sha=hashlib.sha256(archive.read_bytes()).hexdigest()
(dist/(stem+'-SHA256.txt')).write_text(sha+'  '+archive.name+'\n')
(dist/(stem+'-KURULUM.txt')).write_bytes(files['KURULUM.txt'])
result={'archive':archive.name,'bytes':archive.stat().st_size,'sha256':sha,'plugins':manifest}
(validation/'artifacts.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');print(json.dumps(result,ensure_ascii=False,indent=2))
