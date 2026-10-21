#!/usr/bin/env bash
set -euo pipefail
cd /workspace/Sada
if [ ! -x /workspace/jdk-21.0.9+10/bin/javac ] || [ ! -f /workspace/kasaba-maven-settings.xml ]; then
  bash scripts/cloud-build.sh
fi
export JAVA_HOME=/workspace/jdk-21.0.9+10
export MAVEN_OPTS="${MAVEN_OPTS:-} -Djavax.net.ssl.trustStore=/workspace/kasaba-cacerts"
KASABA_MAVEN=(/workspace/apache-maven-3.9.11/bin/mvn -s /workspace/kasaba-maven-settings.xml -Dmaven.repo.local=/workspace/.m2/repository -B -ntp)
"${KASABA_MAVEN[@]}" -f kasaba-roles/pom.xml install
"${KASABA_MAVEN[@]}" -f kasaba-warp/pom.xml install
"${KASABA_MAVEN[@]}" -f kasaba-clan/pom.xml verify
