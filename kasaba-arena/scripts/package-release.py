"""Package the already tested Arena and Akçe builds; no server data or probe plugins are shipped."""
from pathlib import Path
import hashlib
import shutil
import zipfile

root = Path(__file__).resolve().parents[2]
dist = root / 'dist'
dist.mkdir(exist_ok=True)
jars = [('kasaba-arena', 'KasabaArena-1.2.1.jar', '1.2.1'), ('kasaba-akce', 'KasabaAkce-1.0.0.jar', '1.0.0')]
for module, name, version in jars:
    source = root / module / 'target' / name
    with zipfile.ZipFile(source) as archive:
        assert f'version: {version}' in archive.read('plugin.yml').decode()
        assert not any('Probe' in path or '/test/' in path for path in archive.namelist())
    shutil.copyfile(source, dist / name)

(dist / 'KasabaArena-KURULUM.txt').write_text((root / 'kasaba-arena/README.md').read_text(), encoding='utf-8')
(dist / 'KasabaAkce-KURULUM.txt').write_text((root / 'kasaba-akce/README.md').read_text(), encoding='utf-8')
source_files = []
for module in ('kasaba-arena', 'kasaba-akce'):
    for path in (root / module).rglob('*'):
        relative = path.relative_to(root)
        if path.is_file() and not any(part in {'target', 'node_modules', '.git', '__pycache__'} for part in relative.parts):
            source_files.append(path)
with zipfile.ZipFile(dist / 'KasabaArena-Kaynak.zip', 'w', zipfile.ZIP_DEFLATED) as archive:
    for path in sorted(source_files):
        archive.write(path, 'Sada/' + path.relative_to(root).as_posix())
checksums = []
for name in ('KasabaArena-1.2.1.jar', 'KasabaAkce-1.0.0.jar', 'KasabaArena-Kaynak.zip'):
    checksums.append(hashlib.sha256((dist / name).read_bytes()).hexdigest() + '  ' + name)
(dist / 'KasabaArena-SHA256SUMS.txt').write_text('\n'.join(checksums) + '\n', encoding='utf-8')
contents = ['KasabaArena-1.2.1.jar', 'KasabaAkce-1.0.0.jar', 'KasabaArena-KURULUM.txt', 'KasabaAkce-KURULUM.txt', 'KasabaArena-TEST-SONUCLARI.txt', 'KasabaArena-SHA256SUMS.txt']
for name in ('KasabaArena-Kurulum.zip', 'KasabaArena-Akce-Kurulum.zip'):
    with zipfile.ZipFile(dist / name, 'w', zipfile.ZIP_DEFLATED) as archive:
        for filename in contents:
            archive.write(dist / filename, filename)
print('\n'.join(checksums))
