#!/usr/bin/env bash
set -euo pipefail
cd /workspace/Sada
# Her görev zaten yalıtılmıştır; yeni Git worktree oluşturma.
TASK_MAVEN=/workspace/apache-maven-3.9.11
TASK_JDK=/workspace/jdk-21.0.9+10
TASK_TEMP=$(mktemp -d /tmp/kasaba-build.XXXXXX)
trap 'rm -rf "$TASK_TEMP"' EXIT
if [ ! -x "$TASK_MAVEN/bin/mvn" ]; then
  curl -fsSL https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11/apache-maven-3.9.11-bin.tar.gz -o "$TASK_TEMP/maven.tar.gz"
  curl -fsSL https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11/apache-maven-3.9.11-bin.tar.gz.sha512 -o "$TASK_TEMP/maven.sha512"
  python3 - "$TASK_TEMP" <<'PY'
import hashlib,pathlib,sys
p=pathlib.Path(sys.argv[1])
assert hashlib.sha512((p/'maven.tar.gz').read_bytes()).hexdigest()==(p/'maven.sha512').read_text().strip(), 'Maven checksum mismatch'
PY
  tar -xzf "$TASK_TEMP/maven.tar.gz" -C /workspace
fi
if [ ! -x "$TASK_JDK/bin/javac" ]; then
  TASK_JDK_URL=https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.9%2B10/OpenJDK21U-jdk_x64_linux_hotspot_21.0.9_10.tar.gz
  curl -fsSL "$TASK_JDK_URL" -o "$TASK_TEMP/jdk.tar.gz"
  curl -fsSL "$TASK_JDK_URL.sha256.txt" -o "$TASK_TEMP/jdk.sha256"
  python3 - "$TASK_TEMP" <<'PY'
import hashlib,pathlib,sys
p=pathlib.Path(sys.argv[1])
assert hashlib.sha256((p/'jdk.tar.gz').read_bytes()).hexdigest()==(p/'jdk.sha256').read_text().split()[0], 'JDK checksum mismatch'
PY
  tar -xzf "$TASK_TEMP/jdk.tar.gz" -C /workspace
fi
export JAVA_HOME="$TASK_JDK"
python3 - <<'PY'
import os,pathlib,shutil,subprocess,urllib.parse,xml.sax.saxutils as xml
jdk=pathlib.Path(os.environ['JAVA_HOME'])
trust=pathlib.Path('/workspace/kasaba-cacerts')
shutil.copyfile(jdk/'lib/security/cacerts',trust)
cert=os.environ.get('CODEX_PROXY_CERT') or os.environ.get('NODE_EXTRA_CA_CERTS')
if cert and pathlib.Path(cert).is_file():
    subprocess.run([str(jdk/'bin/keytool'),'-importcert','-noprompt','-alias','cloud-egress-proxy','-file',cert,'-keystore',str(trust),'-storepass','changeit'],check=True)
proxy=urllib.parse.urlparse(os.environ.get('HTTPS_PROXY',os.environ.get('https_proxy','')))
settings='<settings>'
if proxy.hostname:
    settings+='<proxies><proxy><id>cloud</id><active>true</active><protocol>http</protocol><host>'+xml.escape(proxy.hostname)+'</host><port>'+str(proxy.port or 80)+'</port></proxy></proxies>'
settings+='</settings>'
pathlib.Path('/workspace/kasaba-maven-settings.xml').write_text(settings)
PY
# Doğrulama korunur; güven deposuna sadece platformun sağladığı CA eklenir.
export MAVEN_OPTS="${MAVEN_OPTS:-} -Djavax.net.ssl.trustStore=/workspace/kasaba-cacerts"
"$TASK_MAVEN/bin/mvn" -s /workspace/kasaba-maven-settings.xml -Dmaven.repo.local=/workspace/.m2/repository -B -ntp verify
