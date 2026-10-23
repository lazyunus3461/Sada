#!/usr/bin/env python3
"""Package only the production plugin, supplied map, documentation and sources."""
from pathlib import Path
import hashlib,json,shutil,zipfile
root=Path(__file__).resolve().parents[1];dist=root.parent/'dist';dist.mkdir(exist_ok=True)
version='1.2.1';stem=f'KasabaVampirKoylu-{version}';jar=root/'target'/f'{stem}.jar'
with zipfile.ZipFile(jar) as z:
 assert 'plugin.yml' in z.namelist()
 assert not any('Probe' in n or n.startswith('tr/kasaba/auth/') or n.startswith('tr/kasaba/hub/') for n in z.namelist())
shutil.copy2(jar,dist/jar.name)
shutil.copy2(root/'validation/three-villages-preview.png',dist/f'{stem}-KOY.png')
readme=root/'README.md';shutil.copy2(readme,dist/f'{stem}-KURULUM.md')
commands=readme.read_text(encoding='utf-8');(dist/f'{stem}-KOMUTLAR.txt').write_text(commands,encoding='utf-8')
report=root/'test-results.txt';assert report.is_file(),'Run live regression and write test evidence before packaging.'
shutil.copy2(report,dist/f'{stem}-TEST-SONUCLARI.txt')
with zipfile.ZipFile(dist/f'{stem}-Kurulum.zip','w',zipfile.ZIP_DEFLATED) as z:
 z.write(jar,f'plugins/{jar.name}');z.write(readme,'KURULUM-VE-KOMUTLAR.md');z.write(dist/f'{stem}-KOMUTLAR.txt','KOMUTLAR.txt');z.write(report,'KasabaVampirKoylu-TEST-SONUCLARI.txt')
 for p in sorted((root/'world-template/vampirkoylu_uc_oda').rglob('*')):
  if p.is_file():
   assert p.name not in {'uid.dat','session.lock'}
   z.write(p,str(p.relative_to(root/'world-template')))
 z.write(root/'world-template/vampirkoylu_uc_oda-konumlar.json','vampirkoylu_uc_oda-konumlar.json')
 z.write(root/'validation/three-villages-preview.png','KOY-PLANI.png')
 z.write(root/'validation/three-villages-preview.png','UC-ODA-GORUNUMU.png')
with zipfile.ZipFile(dist/f'{stem}-Kaynak.zip','w',zipfile.ZIP_DEFLATED) as z:
 for p in sorted(root.rglob('*')):
  if p.is_file() and not {'target','.git','__pycache__'}.intersection(p.relative_to(root).parts):z.write(p,str(p.relative_to(root)))
files=[dist/'KasabaVampirKoylu-1.2.0-3-ODA-DUNYA.zip',dist/jar.name,dist/f'{stem}-KOY.png',dist/f'{stem}-Kurulum.zip',dist/f'{stem}-Kaynak.zip',dist/f'{stem}-KURULUM.md',dist/f'{stem}-KOMUTLAR.txt',dist/f'{stem}-TEST-SONUCLARI.txt']
(dist/f'{stem}-SHA256SUMS.txt').write_text(''.join(f'{hashlib.sha256(p.read_bytes()).hexdigest()}  {p.name}\n' for p in files),encoding='utf-8')
print(json.dumps({p.name:p.stat().st_size for p in files},indent=2))
