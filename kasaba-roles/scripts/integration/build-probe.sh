#!/usr/bin/env bash
set -euo pipefail
python3 - <<'PY'
from pathlib import Path
import subprocess,shutil,zipfile
root=Path('/workspace/Sada/kasaba-roles');out=root/'target/roles-probe-classes'
if out.exists():shutil.rmtree(out)
out.mkdir(parents=True)
cp=':'.join(str(p) for p in Path('/workspace/.m2/repository').rglob('*.jar'))
subprocess.run(['/workspace/jdk-21.0.9+10/bin/javac','-proc:none','--release','21','-encoding','UTF-8','-cp',cp,'-d',str(out),str(root/'scripts/integration/RolesProbe.java')],check=True)
with zipfile.ZipFile(root/'target/KasabaRolesProbe.jar','w',zipfile.ZIP_DEFLATED) as z:
 z.writestr('plugin.yml','name: KasabaRolesProbe\nversion: 1.0\nmain: tr.kasaba.test.RolesProbe\napi-version: \'1.21.10\'\ndepend: [KasabaRoles]\n')
 for p in out.rglob('*.class'):z.write(p,str(p.relative_to(out)))
print('Local-only roles integration probe built.')
PY
