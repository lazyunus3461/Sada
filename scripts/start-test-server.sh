#!/usr/bin/env bash
set -euo pipefail
cd /workspace/kasaba-test-server
# Bu dizin yalnızca yerel test içindir; gerçek sunucu ayarlarını değiştirmez.
grep -qx 'server-ip=127.0.0.1' server.properties
grep -qx 'server-port=25575' server.properties
grep -qx 'eula=true' eula.txt
python3 - <<'PY'
import os,urllib.parse,pathlib
p=urllib.parse.urlparse(os.environ.get('HTTPS_PROXY',os.environ.get('https_proxy','')))
args=['-Djavax.net.ssl.trustStore=/workspace/kasaba-cacerts']
if p.hostname:
    args += ['-Dhttps.proxyHost='+p.hostname,'-Dhttps.proxyPort='+str(p.port or 80),'-Dhttp.proxyHost='+p.hostname,'-Dhttp.proxyPort='+str(p.port or 80),'-Dhttp.nonProxyHosts=localhost|127.*']
pathlib.Path('jvm-proxy.args').write_text('\n'.join(args)+'\n')
PY
exec java @jvm-proxy.args -Dterminal.jline=false -Dterminal.ansi=false -XX:ActiveProcessorCount=2 -Xms256M -Xmx1536M -jar paper.jar --nogui
