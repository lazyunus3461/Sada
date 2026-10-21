"""Package KasabaSpawn and its plain-Turkish install instructions."""
from pathlib import Path
import hashlib
import shutil
import zipfile

root=Path(__file__).resolve().parents[2]
source=root/'kasaba-spawn/target/KasabaSpawn-1.0.0.jar'
dist=root/'dist'
dist.mkdir(exist_ok=True)
jar=dist/source.name
shutil.copyfile(source,jar)
instructions=dist/'KasabaSpawn-KURULUM.txt'
zip_path=dist/'KasabaSpawn-Kurulum.zip'
for path in (jar,instructions):
    if not path.is_file():raise SystemExit(f'Missing release file: {path}')
with zipfile.ZipFile(jar) as archive:
    assert archive.testzip() is None
    plugin=archive.read('plugin.yml').decode()
    assert 'name: KasabaSpawn' in plugin and 'version: 1.0.0' in plugin
    assert not any('Probe' in name or '/test/' in name for name in archive.namelist())
with zipfile.ZipFile(zip_path,'w',zipfile.ZIP_DEFLATED) as archive:
    archive.write(jar,jar.name)
    archive.write(instructions,instructions.name)
hashes=''.join(f'{hashlib.sha256(path.read_bytes()).hexdigest()}  {path.name}\n' for path in (jar,zip_path,instructions))
(dist/'KasabaSpawn-SHA256SUMS.txt').write_text(hashes,encoding='utf-8')
with zipfile.ZipFile(zip_path) as archive:assert archive.testzip() is None
print(hashes,end='')
