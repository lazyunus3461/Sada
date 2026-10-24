from pathlib import Path
import subprocess,zipfile
root=Path('/workspace/Sada');out=Path('/workspace/scratch/followup-probes');out.mkdir(parents=True,exist_ok=True)
libs=list(Path('/workspace/kasaba-test-server/libraries').rglob('*.jar'))
libs.extend((root/p).resolve() for p in ['kasaba-hub/target/KasabaHub-1.6.0.jar','kasaba-roles/target/KasabaRoles-1.3.1.jar','kasaba-akce/target/KasabaAkce-1.1.0.jar'])
cp=':'.join(map(str,libs))
for module,java,depends in [('scripts/integration/followup','FollowupProbe','KasabaYaris, KasabaHapis, KasabaIspanya, KasabaVampirKoylu'),('kasaba-sosyal/scripts/integration','NetworkProbe','KasabaSosyal'),('kasaba-renk/scripts/integration','RenkProbe','KasabaRenk')]:
    dest=out/java;dest.mkdir(exist_ok=True)
    subprocess.run(['/workspace/jdk-21.0.9+10/bin/javac','-proc:none','--release','21','-encoding','UTF-8','-cp',cp,'-d',str(dest),str(root/module/(java+'.java'))],check=True)
    with zipfile.ZipFile(out/(java+'.jar'),'w',zipfile.ZIP_DEFLATED) as z:
        z.writestr('plugin.yml',f"name: Kasaba{java}\nversion: 1.0\nmain: tr.kasaba.test.{java}\napi-version: '1.21.10'\ndepend: [{depends}]\n")
        for p in dest.rglob('*.class'):z.write(p,str(p.relative_to(dest)))
print('Loopback probes built; do not distribute.')
