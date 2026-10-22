"""Package the tested unbuilt island world and production plugins, excluding all test data."""
from pathlib import Path
import hashlib,shutil,zipfile
root=Path(__file__).resolve().parents[2];dist=root/'dist';dist.mkdir(exist_ok=True)
for module,name in [('kasaba-adalar','KasabaAdalar-1.3.2.jar'),('kasaba-hub','KasabaHub-1.2.0.jar')]:
 source=root/module/'target'/name
 with zipfile.ZipFile(source) as archive:
  assert archive.testzip() is None
  assert not any('Probe' in path or '/test/' in path for path in archive.namelist())
 shutil.copyfile(source,dist/name)
world=root/'kasaba-adalar/target/ada'
assert 'spacing=100' in (world/'kasaba-layout.properties').read_text()
assert not (world/'playerdata').exists() and not (world/'uid.dat').exists()
world_files=[p for p in sorted(world.rglob('*')) if p.is_file()]
with zipfile.ZipFile(dist/'KasabaAdalar-Dunya.zip','w',zipfile.ZIP_DEFLATED) as archive:
 for path in world_files:archive.write(path,'ada/'+path.relative_to(world).as_posix())
with zipfile.ZipFile(dist/'KasabaAdalar-Kaynak.zip','w',zipfile.ZIP_DEFLATED) as archive:
 for name in ['pom.xml','README.md','.gitignore']:archive.write(root/name,'Sada/'+name)
 for folder in ['src','scripts','kasaba-adalar','kasaba-hub','kasaba-arena','kasaba-akce','kasaba-roles']:
  for path in sorted((root/folder).rglob('*')):
   relative=path.relative_to(root)
   if path.is_file() and not any(part in {'target','node_modules','.git','__pycache__'} for part in relative.parts):archive.write(path,'Sada/'+relative.as_posix())
names=['KasabaAdalar-1.3.2.jar','KasabaHub-1.2.0.jar','KasabaAdalar-Dunya.zip','KasabaAdalar-Kaynak.zip']
checksums=''.join(hashlib.sha256((dist/name).read_bytes()).hexdigest()+'  '+name+'\n' for name in names)
(dist/'KasabaAdalar-SHA256SUMS.txt').write_text(checksums,encoding='utf-8')
with zipfile.ZipFile(dist/'KasabaAdalar-Kurulum.zip','w',zipfile.ZIP_DEFLATED) as archive:
 for name in names[:2]:archive.write(dist/name,'plugins/'+name)
 for path in world_files:archive.write(path,'ada/'+path.relative_to(world).as_posix())
 for name in ['KasabaAdalar-KURULUM.txt','KasabaAdalar-TEST-SONUCLARI.txt','KasabaAdalar-SHA256SUMS.txt']:archive.write(dist/name,name)
print(checksums)
