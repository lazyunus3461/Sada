#!/usr/bin/env python3
"""Package tested Esya + Hub add-on and the complete 14-plugin distribution."""
from pathlib import Path
import argparse,ast,hashlib,json,re,zipfile
root=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser();parser.add_argument('--tested-server',type=Path,required=True);args=parser.parse_args()
dist=root/'dist';proof=json.loads((root/'validation/1.6.0/live-results.json').read_text())
assert len(proof['checks'])>=14 and proof['restart'] and proof['finalArtifactsSmoke']
assert proof['unit']=={'tests':27,'failures':0,'errors':0}
assert len(proof['maps'])>=5 and len(set(proof['maps']))==3
# Reuse the last immutable release's reviewed whitelist, then change exactly Hub and add Esya.
old=(root/'scripts/package-lobby-release.py').read_text();modules=ast.literal_eval(re.search(r'modules=(\{.*?\n\})',old,re.S).group(1))
modules['KasabaHub']=('kasaba-hub/target/KasabaHub-1.6.1.jar','1.6.1','tr/kasaba/hub/')
modules['KasabaEsya']=('kasaba-esya/target/KasabaEsya-1.3.0.jar','1.3.0','tr/kasabatr/esya/')
assert len(modules)==14
files={};manifest={}
with zipfile.ZipFile(root/'dist/KasabaTR-Sunucu-Paketi-1.5.0.zip') as baseline:
 for name,(relative,version,prefix) in modules.items():
  source=root/relative;data=source.read_bytes();assert data==(args.tested_server/'plugins'/source.name).read_bytes(),f'Untested artifact {name}'
  if name not in ['KasabaHub','KasabaEsya']:assert data==baseline.read('plugins/'+source.name),f'Unrequested old-game change: {name}'
  with zipfile.ZipFile(source) as z:
   assert z.testzip() is None;meta=z.read('plugin.yml').decode()
   assert re.search(r'^name:\s*'+re.escape(name)+r'\s*$',meta,re.M)
   assert re.search(r'^version:\s*'+re.escape(version)+r'\s*$',meta,re.M)
   for member in z.namelist():
    assert 'Probe' not in member and not member.startswith('tr/kasaba/test/')
    if member.endswith('.class'):assert member.startswith(prefix),member
   if name=='KasabaEsya':
    folder=root/'kasaba-esya/src/main/resources/map-template'
    for p in folder.rglob('*'):
     if p.is_file():assert z.read('map-template/'+str(p.relative_to(folder)))==p.read_bytes()
  files['plugins/'+source.name]=data;manifest[name]={'version':version,'jar':source.name,'bytes':len(data),'sha256':hashlib.sha256(data).hexdigest()}
files['KURULUM-VE-KOMUTLAR.txt']=(root/'validation/1.6.0/KURULUM-VE-KOMUTLAR.txt').read_bytes()
files['TEST-SONUCLARI.txt']=(root/'validation/1.6.0/TEST-SONUCLARI.txt').read_bytes()
files['BELGELER/esya-KILAVUZ.md']=(root/'kasaba-esya/README.md').read_bytes()
files['BELGELER/test-results.json']=(root/'validation/1.6.0/live-results.json').read_bytes()
files['BELGELER/input-and-map-manifest.json']=(root/'validation/1.6.0/input-and-map-manifest.json').read_bytes()
for p in (root/'kasaba-esya/docs').glob('*.png'):files['ONIZLEMELER/'+p.name]=p.read_bytes()
artifacts={}
def package(stem,entries,plugins):
 archive=dist/(stem+'.zip');assert not archive.exists(),'Never overwrite a released archive'
 entries=dict(entries);entries['SURUMLER-VE-SHA256.json']=(json.dumps(plugins,ensure_ascii=False,indent=2)+'\n').encode()
 entries['SHA256SUMS.txt']=''.join(hashlib.sha256(data).hexdigest()+'  '+name+'\n' for name,data in entries.items()).encode()
 with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
  for name,data in entries.items():z.writestr(name,data)
 with zipfile.ZipFile(archive) as z:assert z.testzip() is None and len([n for n in z.namelist() if n.endswith('.jar')])==len(plugins)
 sha=hashlib.sha256(archive.read_bytes()).hexdigest();(dist/(stem+'-SHA256.txt')).write_text(sha+'  '+archive.name+'\n')
 (dist/(stem+'-KURULUM.txt')).write_bytes(entries['KURULUM-VE-KOMUTLAR.txt'])
 artifacts[stem]={'archive':archive.name,'bytes':archive.stat().st_size,'sha256':sha,'plugins':plugins}
addon={k:v for k,v in files.items() if not k.startswith('plugins/') or k in ['plugins/KasabaHub-1.6.1.jar','plugins/KasabaEsya-1.3.0.jar']}
package('KasabaEsya-KasabaTR-Hazir-Paket-1.3.0',addon,{k:manifest[k] for k in ['KasabaHub','KasabaEsya']})
for module in ['hub','arena','battle','vampir','yaris','hapis','ispanya','renk','sosyal','roles','adalar']:
 files['BELGELER/'+module+'-KILAVUZ.md']=(root/('kasaba-'+module)/'README.md').read_bytes()
package('KasabaTR-Sunucu-Paketi-1.6.0',files,manifest)
for name in ['KasabaHub','KasabaEsya']:
 relative=modules[name][0];source=root/relative;dest=dist/source.name;assert not dest.exists(),'Never overwrite a released JAR';dest.write_bytes(source.read_bytes())
(root/'validation/1.6.0/artifacts.json').write_text(json.dumps(artifacts,ensure_ascii=False,indent=2)+'\n')
print(json.dumps({k:{x:y for x,y in v.items() if x!='plugins'} for k,v in artifacts.items()},indent=2))
