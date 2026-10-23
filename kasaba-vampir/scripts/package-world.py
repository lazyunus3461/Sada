#!/usr/bin/env python3
"""Package the tested village as a standalone Minecraft Java world download."""
import hashlib
import json
from pathlib import Path
import zipfile


root = Path(__file__).resolve().parents[1]
world = root / 'world-template' / 'vampirkoylu_sokaklar'
dist = root.parent / 'dist'
dist.mkdir(exist_ok=True)
archive = dist / 'KasabaVampirKoylu-1.1.0-DUNYA.zip'
files = [world / 'level.dat', world / 'data/world_border.dat']
files.extend(sorted((world / 'region').glob('r.*.*.mca')))
assert all(path.is_file() for path in files)
assert len(files) == 6, 'Expected level.dat, world border and four tested regions.'

instructions = '''KASABA VAMPİR KÖYLÜ — DÜNYA HARİTASI (Minecraft Java 1.21.10)

Harita klasörü: vampirkoylu_sokaklar

1. Sunucuyu kapat.
2. vampirkoylu_sokaklar klasörünü server.properties dosyasının yanına çıkar.
   Aynı adlı klasör varsa önce mevcut klasörün yedeğini al; üzerine birleştirme.
3. KasabaVampirKoylu 1.1.0 eklentisi kuruluysa sunucuyu açınca dünyayı yükler.

Bu ZIP yalnızca dünya dosyalarıdır. Oyun kuralları ve görevler için eklenti de gerekir.
Köy merkezi: 0 / 69 / 0. Ortak lobi: 144.5 / 69 / 0.5.
'''

with zipfile.ZipFile(archive, 'w', zipfile.ZIP_DEFLATED) as output:
    for path in files:
        entry = zipfile.ZipInfo(path.relative_to(world.parent).as_posix())
        entry.compress_type = zipfile.ZIP_DEFLATED
        entry.external_attr = 0o100644 << 16
        output.writestr(entry, path.read_bytes())
    entry = zipfile.ZipInfo('HARITA-KURULUM.txt')
    entry.compress_type = zipfile.ZIP_DEFLATED
    entry.external_attr = 0o100644 << 16
    output.writestr(entry, instructions.encode('utf-8'))

with zipfile.ZipFile(archive) as packaged:
    assert packaged.testzip() is None
    assert len(packaged.namelist()) == len(files) + 1
    for path in files:
        assert packaged.read(path.relative_to(world.parent).as_posix()) == path.read_bytes()

digest = hashlib.sha256(archive.read_bytes()).hexdigest()
archive.with_suffix('.zip.sha256').write_text(f'{digest}  {archive.name}\n', encoding='ascii')
print(json.dumps({'file': str(archive), 'bytes': archive.stat().st_size,
                  'sha256': digest, 'world_files_verified': len(files)}, indent=2))
