#!/usr/bin/env bash
set -euo pipefail
python3 - <<'PY'
from pathlib import Path
import subprocess,shutil,zipfile
root=Path('/workspace/Sada/kasaba-hub');out=root/'target/hub-probe-classes'
if out.exists():shutil.rmtree(out)
out.mkdir(parents=True)
cp=str(root/'target/KasabaHub-1.1.0.jar')+':'+':'.join(str(p) for p in Path('/workspace/.m2/repository').rglob('*.jar'))
subprocess.run(['/workspace/jdk-21.0.9+10/bin/javac','-proc:none','--release','21','-encoding','UTF-8','-cp',cp,'-d',str(out),str(root/'scripts/integration/HubProbe.java')],check=True)
with zipfile.ZipFile(root/'target/KasabaHubProbe.jar','w',zipfile.ZIP_DEFLATED) as z:
 z.writestr('plugin.yml',"name: KasabaHubProbe\nversion: 1.0\nmain: tr.kasaba.test.HubProbe\napi-version: '1.21.10'\ndepend: [KasabaHub, KasabaRoles]\n")
 for p in out.rglob('*.class'):z.write(p,str(p.relative_to(out)))
print('Loopback-only hub fixture built')
PY
