"""Package the verified foundation and matching auth API; exclude all test adapters."""
from pathlib import Path
import hashlib,shutil,zipfile
root=Path(__file__).resolve().parents[2];dist=root/'dist';dist.mkdir(exist_ok=True)
files=['KasabaHub-1.1.0.jar','KasabaAuth-1.3.0.jar','KasabaHub-KURULUM.txt','KasabaHub-TEST-SONUCLARI.txt']
for source,name in [(root/'kasaba-hub/target','KasabaHub-1.1.0.jar'),(root/'target','KasabaAuth-1.3.0.jar')]:
 shutil.copyfile(source/name,dist/name)
 with zipfile.ZipFile(dist/name) as z:
  assert z.testzip() is None
  assert not any('Probe' in entry or '/test/' in entry for entry in z.namelist())
checksums=''.join(hashlib.sha256((dist/name).read_bytes()).hexdigest()+'  '+name+'\n' for name in files)
(dist/'KasabaHub-SHA256SUMS.txt').write_text(checksums,encoding='utf-8')
with zipfile.ZipFile(dist/'KasabaHub-Kurulum.zip','w',zipfile.ZIP_DEFLATED) as z:
 for name in files+['KasabaHub-SHA256SUMS.txt']:z.write(dist/name,name)
with zipfile.ZipFile(dist/'KasabaHub-Kaynak.zip','w',zipfile.ZIP_DEFLATED) as z:
 paths=[root/p for p in ['README.md','pom.xml','.gitignore','dist/KasabaHub-KURULUM.txt','dist/KasabaHub-TEST-SONUCLARI.txt']]
 for folder in ['src','scripts','kasaba-hub','kasaba-roles']:paths.extend((root/folder).rglob('*'))
 for path in sorted(paths):
  rel=path.relative_to(root)
  if path.is_file() and not any(part in {'target','node_modules','__pycache__','.git'} for part in rel.parts):z.write(path,'Sada/'+rel.as_posix())
print(checksums.splitlines()[0])
