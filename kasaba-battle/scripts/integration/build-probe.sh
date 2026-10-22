#!/usr/bin/env bash
set -euo pipefail
python3 - <<'PY'
from pathlib import Path
import subprocess,zipfile
root=Path('/workspace/Sada/kasaba-battle');out=root/'target/probe';out.mkdir(parents=True,exist_ok=True)
classpath=':'.join(str(p) for p in Path('/workspace/kasaba-test-server/libraries').rglob('*.jar'))
subprocess.run(['/workspace/jdk-21.0.9+10/bin/javac','-proc:none','--release','21','-encoding','UTF-8','-cp',classpath,'-d',str(out),str(root/'scripts/integration/BattleProbe.java')],check=True)
with zipfile.ZipFile(root/'target/KasabaBattleProbe.jar','w',zipfile.ZIP_DEFLATED) as z:
 z.writestr('plugin.yml',"name: KasabaBattleProbe\nversion: 1.0\nmain: tr.kasaba.test.BattleProbe\napi-version: '1.21.10'\ndepend: [BattleRoyalePrototype, KasabaHub]\n")
 for p in out.rglob('*.class'):z.write(p,str(p.relative_to(out)))
print('Loopback Battle fixture built')
PY
