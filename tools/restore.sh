#!/usr/bin/env bash
# Re-enable every package listed in a debloat file, restore the stock launcher as home and reset
# animation scales. Pass the same list you used with tools/disable.sh.
#
# Usage: TV_HOST=192.168.1.50 tools/restore.sh examples/xiaomi-mi-tv-4a/debloat.txt [stock.launcher.package]
set -euo pipefail
cd "$(dirname "$0")/.."
LIST="${1:?usage: restore.sh <debloat list> [stock launcher package]}"
STOCK="${2:-com.google.android.tvlauncher}"
PY=tools/venv/bin/python
[ -x "$PY" ] || PY=tools/venv/Scripts/python.exe
export MSYS_NO_PATHCONV=1
# Entries are pasted into shell commands that run on the TV, so accept plain package names only.
PKGS=$(tr -d '\r' < "$LIST" | grep -vE '^\s*(#|$)' | sed -E 's/\s+#.*$//' | awk '{print $1}' || true)
BAD=$(printf '%s\n' "$PKGS" | grep -vE '^[A-Za-z0-9_]+(\.[A-Za-z0-9_]+)*$' | grep -v '^$' || true)
[ -z "$BAD" ] || { echo "refusing to run: not a package name in $LIST:"; printf '%s\n' "$BAD" | sed 's/^/  /'; exit 1; } >&2
case "$STOCK" in *[!A-Za-z0-9_.]*|"") echo "refusing to run: not a package name: $STOCK" >&2; exit 1 ;; esac
{
  [ -z "$PKGS" ] || printf '%s\n' "$PKGS" | awk '{print "pm enable " $1}'
  echo "pm enable $STOCK"
  # resolve the stock HOME component on the TV (Android 9 needs pkg/activity, not just pkg)
  echo "cmd package set-home-activity --user 0 \$(cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME $STOCK | tail -n 1)"
  echo "settings put global window_animation_scale 1.0"
  echo "settings put global transition_animation_scale 1.0"
  echo "settings put global animator_duration_scale 1.0"
  echo "pm list packages -d"
} | "$PY" -I tools/tv.py --batch
