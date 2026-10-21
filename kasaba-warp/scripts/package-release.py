"""Package verified warp artifacts without changing existing auth/roles releases."""
from pathlib import Path
import hashlib
import shutil
import zipfile
import xml.etree.ElementTree as ET

root=Path(__file__).resolve().parents[2]
dist=root/'dist';dist.mkdir(exist_ok=True)
version=ET.parse(root/'kasaba-warp/pom.xml').getroot().findtext('{http://maven.apache.org/POM/4.0.0}version')
jar=f'KasabaWarp-{version}.jar'
shutil.copyfile(root/'kasaba-warp/target'/jar,dist/jar)
with zipfile.ZipFile(dist/jar) as archive:
    assert f'version: {version}' in archive.read('plugin.yml').decode()
    assert not any('Probe' in name or '/test/' in name for name in archive.namelist())
with zipfile.ZipFile(dist/'KasabaWarp-Kaynak.zip','w',zipfile.ZIP_DEFLATED) as archive:
    paths=[root/p for p in ['README.md','pom.xml','.gitignore','dist/KasabaWarp-KURULUM.txt','dist/KasabaWarp-TEST-SONUCLARI.txt']]
    for folder in ['src','scripts','kasaba-roles','kasaba-warp']:paths.extend((root/folder).rglob('*'))
    for path in sorted(paths):
        relative=path.relative_to(root)
        if path.is_file() and not any(p in {'target','node_modules','__pycache__','.git'} for p in relative.parts):archive.write(path,'Sada/'+relative.as_posix())
files=[jar,'KasabaWarp-KURULUM.txt','KasabaWarp-TEST-SONUCLARI.txt']
checksums=''.join(hashlib.sha256((dist/name).read_bytes()).hexdigest()+'  '+name+'\n' for name in files)
(dist/'KasabaWarp-SHA256SUMS.txt').write_text(checksums,encoding='utf-8')
with zipfile.ZipFile(dist/'KasabaWarp-Kurulum.zip','w',zipfile.ZIP_DEFLATED) as archive:
    for name in files+['KasabaWarp-SHA256SUMS.txt']:archive.write(dist/name,name)
print(checksums.splitlines()[0])
