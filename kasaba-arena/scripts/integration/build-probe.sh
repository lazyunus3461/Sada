#!/usr/bin/env bash
set -euo pipefail
repo_root="$(cd "$(dirname "$0")/../../.." && pwd)"
arena_build="$repo_root/kasaba-arena/target/probe"
mkdir -p "$arena_build"
arena_classpath="$(python3 - <<'PY'
from pathlib import Path
jars=list(Path('/workspace/kasaba-test-server/libraries').rglob('*.jar'))
jars.append(Path('/workspace/Sada/kasaba-arena/target/KasabaArena-1.2.1.jar'))
print(':'.join(map(str,jars)))
PY
)"
/workspace/jdk-21.0.9+10/bin/javac -proc:none -cp "$arena_classpath" -d "$arena_build" "$repo_root/kasaba-arena/scripts/integration/probe/ArenaProbe.java"
cat > "$arena_build/plugin.yml" <<'YAML'
name: KasabaArenaProbe
version: 1.0
main: tr.kasaba.test.ArenaProbe
api-version: '1.21.10'
depend: [KasabaArena]
YAML
/workspace/jdk-21.0.9+10/bin/jar cf "$arena_build/KasabaArenaProbe.jar" -C "$arena_build" plugin.yml -C "$arena_build" tr
