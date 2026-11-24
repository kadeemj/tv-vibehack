#!/usr/bin/env bash
# Disable every package listed in a debloat file on the TV (one package per line, # comments allowed).
# Packages are disabled for user 0, never uninstalled. Undo with tools/restore.sh <same file>.
#
# Usage: TV_HOST=192.168.1.50 tools/disable.sh examples/xiaomi-mi-tv-4a/debloat.txt
set -euo pipefail
cd "$(dirname "$0")/.."
LIST="${1:?usage: disable.sh <debloat list>}"
PY=tools/venv/bin/python
[ -x "$PY" ] || PY=tools/venv/Scripts/python.exe
export MSYS_NO_PATHCONV=1
[ -x "$PY" ] || { echo "run: python3 -m venv tools/venv && tools/venv/bin/pip install -r tools/requirements.txt"; exit 1; }
# Entries are pasted into shell commands that run on the TV, so accept plain package names only.
PKGS=$(tr -d '\r' < "$LIST" | grep -vE '^\s*(#|$)' | sed -E 's/\s+#.*$//' | awk '{print $1}' || true)
BAD=$(printf '%s\n' "$PKGS" | grep -vE '^[A-Za-z0-9_]+(\.[A-Za-z0-9_]+)*$' | grep -v '^$' || true)
[ -z "$BAD" ] || { echo "refusing to run: not a package name in $LIST:"; printf '%s\n' "$BAD" | sed 's/^/  /'; exit 1; } >&2
{
  [ -z "$PKGS" ] || printf '%s\n' "$PKGS" | awk '{print "pm disable-user --user 0 " $1}'
  echo "pm list packages -d"
} | "$PY" -I tools/tv.py --batch
