"""Package an already verified build, preserving the other plugin's releases."""
from pathlib import Path
import hashlib
import shutil
import zipfile
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[2]
dist = root / "dist"
dist.mkdir(exist_ok=True)
version = ET.parse(root / "kasaba-roles/pom.xml").getroot().findtext("{http://maven.apache.org/POM/4.0.0}version")
jar_name = f"KasabaRoles-{version}.jar"
shutil.copyfile(root / "kasaba-roles/target" / jar_name, dist / jar_name)

source_zip = dist / "KasabaRoles-Kaynak.zip"
with zipfile.ZipFile(source_zip, "w", zipfile.ZIP_DEFLATED) as archive:
    paths = [root / p for p in ("README.md", "pom.xml", ".gitignore", "dist/KasabaRoles-KURULUM.txt", "dist/KasabaRoles-TEST-SONUCLARI.txt")]
    for folder in ("src", "scripts", "kasaba-roles"):
        paths.extend((root / folder).rglob("*"))
    for path in sorted(paths):
        relative = path.relative_to(root)
        if path.is_file() and not any(p in {"target", "node_modules", "__pycache__", ".git"} for p in relative.parts):
            archive.write(path, "Sada/" + relative.as_posix())

files = [jar_name, "KasabaRoles-KURULUM.txt", "KasabaRoles-TEST-SONUCLARI.txt"]
checksums = "".join(hashlib.sha256((dist / name).read_bytes()).hexdigest() + "  " + name + "\n" for name in files)
(dist / "KasabaRoles-SHA256SUMS.txt").write_text(checksums, encoding="utf-8")
with zipfile.ZipFile(dist / "KasabaRoles-Kurulum.zip", "w", zipfile.ZIP_DEFLATED) as archive:
    for name in [jar_name, "KasabaRoles-KURULUM.txt", "KasabaRoles-TEST-SONUCLARI.txt", "KasabaRoles-SHA256SUMS.txt"]:
        archive.write(dist / name, name)
print(checksums.splitlines()[0])
