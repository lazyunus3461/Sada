"""Package a verified auth build without altering the roles distribution."""
from pathlib import Path
import hashlib
import shutil
import zipfile
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
dist = root / "dist"
dist.mkdir(exist_ok=True)
version = ET.parse(root / "pom.xml").getroot().findtext("{http://maven.apache.org/POM/4.0.0}version")
jar_name = f"KasabaAuth-{version}.jar"
shutil.copyfile(root / "target" / jar_name, dist / jar_name)
with zipfile.ZipFile(dist / jar_name) as archive:
    assert f"version: {version}" in archive.read("plugin.yml").decode()
    assert not any("Probe" in name or "/test/" in name for name in archive.namelist())
with zipfile.ZipFile(dist / "KasabaAuth-Kaynak.zip", "w", zipfile.ZIP_DEFLATED) as archive:
    paths = [root / p for p in ("README.md", "pom.xml", ".gitignore", "dist/KURULUM.txt", "dist/TEST-SONUCLARI.txt")]
    for folder in ("src", "scripts", "kasaba-roles"):
        paths.extend((root / folder).rglob("*"))
    for path in sorted(paths):
        relative = path.relative_to(root)
        if path.is_file() and not any(p in {"target", "node_modules", "__pycache__", ".git"} for p in relative.parts):
            archive.write(path, "Sada/" + relative.as_posix())
checksum = hashlib.sha256((dist / jar_name).read_bytes()).hexdigest() + "  " + jar_name + "\n"
(dist / "SHA256SUMS.txt").write_text(checksum, encoding="utf-8")
with zipfile.ZipFile(dist / "KasabaAuth-Kurulum.zip", "w", zipfile.ZIP_DEFLATED) as archive:
    for name in [jar_name, "KURULUM.txt", "TEST-SONUCLARI.txt", "SHA256SUMS.txt"]:
        archive.write(dist / name, name)
print(checksum.strip())
