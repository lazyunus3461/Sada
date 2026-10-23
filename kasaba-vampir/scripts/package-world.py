#!/usr/bin/env python3
"""Package the tested village as a standalone Minecraft Java world download."""
import hashlib
import json
from pathlib import Path
import zipfile


root = Path(__file__).resolve().parents[1]
world = root / 'world-template' / 'vampirkoylu_uc_oda'
dist = root.parent / 'dist'
dist.mkdir(exist_ok=True)
archive = dist / 'KasabaVampirKoylu-1.2.0-3-ODA-DUNYA.zip'
files = [world / 'level.dat', world / 'data/world_border.dat']
files.extend(sorted((world / 'region').glob('r.*.*.mca')))
assert all(path.is_file() for path in files)
assert len(files) == 8, 'Expected level.dat, world border and six tested regions.'

instructions = '''KASABA VAMPİR KÖYLÜ — DÜNYA HARİTASI (Minecraft Java 1.21.10)

Harita klasörü: vampirkoylu_uc_oda

1. Sunucuyu kapat.
2. vampirkoylu_uc_oda klasörünü server.properties dosyasının yanına çıkar.
   Aynı adlı klasör varsa önce mevcut klasörün yedeğini al; üzerine birleştirme.
3. KasabaVampirKoylu-1.2.0.jar dosyasını plugins içine koy; eski Vampir Köylü JAR’ını kaldır.
4. Sunucuyu aç. Eklenti yeni dünyayı yükleyip üç odayı ona bağlar.

Bu ZIP yalnızca dünya dosyalarıdır. Oyun kuralları ve görevler için eklenti de gerekir.
Oda 101 merkezi: 0 / 69 / 0.
Oda 102 merkezi: 320 / 69 / 0.
Oda 103 merkezi: 640 / 69 / 0.
Ortak lobi: 144.5 / 69 / 0.5.
Üç köy tek dünya içindedir. Bu yapıların çevresinde yalnız boşluk vardır.
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
