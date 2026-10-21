"""Package the verified clan build and its two matching API dependencies."""
from pathlib import Path
import hashlib
import shutil
import zipfile

root=Path(__file__).resolve().parents[2]
dist=root/'dist';dist.mkdir(exist_ok=True)
jars=[('kasaba-clan','KasabaKlan-1.0.0.jar'),('kasaba-roles','KasabaRoles-1.2.0.jar'),('kasaba-warp','KasabaWarp-1.2.0.jar')]
for module,filename in jars:
    shutil.copyfile(root/module/'target'/filename,dist/filename)
    with zipfile.ZipFile(dist/filename) as archive:
        assert archive.testzip() is None
        assert not any('Probe' in p or '/test/' in p for p in archive.namelist())
files=[name for _,name in jars]+['KasabaKlan-KURULUM.txt','KasabaKlan-TEST-SONUCLARI.txt']
checksums=''.join(hashlib.sha256((dist/name).read_bytes()).hexdigest()+'  '+name+'\n' for name in files)
(dist/'KasabaKlan-SHA256SUMS.txt').write_text(checksums,encoding='utf-8')
with zipfile.ZipFile(dist/'KasabaKlan-Kurulum.zip','w',zipfile.ZIP_DEFLATED) as archive:
    for name in files+['KasabaKlan-SHA256SUMS.txt']:archive.write(dist/name,name)
with zipfile.ZipFile(dist/'KasabaKlan-Kaynak.zip','w',zipfile.ZIP_DEFLATED) as archive:
    paths=[root/p for p in ['README.md','pom.xml','.gitignore','dist/KasabaKlan-KURULUM.txt','dist/KasabaKlan-TEST-SONUCLARI.txt']]
    for folder in ['src','scripts','kasaba-roles','kasaba-warp','kasaba-clan']:paths.extend((root/folder).rglob('*'))
    for path in sorted(paths):
        relative=path.relative_to(root)
        if path.is_file() and not any(part in {'target','node_modules','__pycache__','.git'} for part in relative.parts):archive.write(path,'Sada/'+relative.as_posix())
print(checksums.splitlines()[0])
