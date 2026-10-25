from pathlib import Path
import subprocess,zipfile
root=Path(__file__).resolve().parents[3];out=Path('/workspace/scratch/boya-probe');out.mkdir(exist_ok=True)
libs=list(Path('/workspace/kasaba-test-server/libraries').rglob('*.jar'))+[root/'kasaba-hub/target/KasabaHub-1.6.2.jar']
subprocess.run(['/workspace/jdk-21.0.9+10/bin/javac','-proc:none','--release','21','-encoding','UTF-8','-cp',':'.join(map(str,libs)),'-d',str(out),str(Path(__file__).with_name('BoyaProbe.java'))],check=True)
with zipfile.ZipFile(out/'KasabaBoyaProbe.jar','w',zipfile.ZIP_DEFLATED) as z:
 z.writestr('plugin.yml',"name: KasabaBoyaProbe\nversion: 1.0\nmain: tr.kasaba.test.BoyaProbe\napi-version: '1.21.10'\ndepend: [KasabaBoya]\n")
 for p in out.rglob('*.class'):z.write(p,str(p.relative_to(out)))
print('Loopback probe built; do not distribute.')
