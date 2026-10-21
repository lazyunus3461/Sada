#!/usr/bin/env bash
set -euo pipefail
python3 - <<'PY'
from pathlib import Path
import subprocess,shutil,zipfile
root=Path('/workspace/Sada/kasaba-clan');out=root/'target/clan-probe-classes'
if out.exists():shutil.rmtree(out)
out.mkdir(parents=True)
cp=':'.join(str(p) for p in Path('/workspace/.m2/repository').rglob('*.jar'))
subprocess.run(['/workspace/jdk-21.0.9+10/bin/javac','-proc:none','--release','21','-encoding','UTF-8','-cp',cp,'-d',str(out),str(root/'scripts/integration/ClanProbe.java')],check=True)
with zipfile.ZipFile(root/'target/KasabaClanProbe.jar','w',zipfile.ZIP_DEFLATED) as z:
 z.writestr('plugin.yml',"name: KasabaClanProbe\nversion: 1.0\nmain: tr.kasaba.test.ClanProbe\napi-version: '1.21.10'\ndepend: [KasabaKlan]\n")
 for p in out.rglob('*.class'):z.write(p,str(p.relative_to(out)))
print('Loopback-only clan probe built')
PY
