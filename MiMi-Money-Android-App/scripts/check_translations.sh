#!/usr/bin/env bash
set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
ROOT="$PROJECT_ROOT/app/src/main/res"
BASE="$ROOT/values/strings.xml"
OUTDIR="$PROJECT_ROOT/app/build"
OUTFILE="$OUTDIR/translation-report.txt"
DBGFILE="$OUTDIR/translation-debug.txt"
mkdir -p "$OUTDIR"
{
  echo "Starting check_translations.sh"
  echo "ROOT=$ROOT"
  echo "BASE=$BASE"
  # Extract base keys (strings only, skip translatable=false)
  awk 'BEGIN{FS="name=\""} /<string/ && $0 !~ /translatable=\"false\"/{ split($2,a,"\""); print a[1] }' "$BASE" | sort -u > /tmp/base_keys.txt
  echo "Base keys extracted: $(wc -l < /tmp/base_keys.txt)"
  : > "$OUTFILE"
  for LOCALE in zh sw pt; do
    echo "Processing locale $LOCALE"
    DIR="$ROOT/values-$LOCALE"
    TMP="/tmp/${LOCALE}_keys.txt"
    : > "$TMP"
    for f in "$DIR"/*.xml; do
      echo "  reading $f"
      awk 'BEGIN{FS="name=\""} /<string/{ split($2,a,"\""); if (a[1] != "") print a[1] }' "$f" >> "$TMP" || true
    done
    sort -u -o "$TMP" "$TMP"
    echo "  locale keys: $(wc -l < "$TMP")"
    comm -23 /tmp/base_keys.txt "$TMP" > "/tmp/${LOCALE}_missing.txt" || true
    echo "=== Missing in $LOCALE ($(( $(wc -l < "/tmp/${LOCALE}_missing.txt") ))) ===" >> "$OUTFILE"
    cat "/tmp/${LOCALE}_missing.txt" >> "$OUTFILE"
    echo >> "$OUTFILE"
  done
  # Also include counts summary
  printf "Base keys: %s\n" "$(wc -l < /tmp/base_keys.txt)" >> "$OUTFILE"
  for LOCALE in zh sw pt; do
    printf "Locale %s keys: %s\n" "$LOCALE" "$(wc -l < "/tmp/${LOCALE}_keys.txt")" >> "$OUTFILE"
    printf "Locale %s missing: %s\n" "$LOCALE" "$(wc -l < "/tmp/${LOCALE}_missing.txt")" >> "$OUTFILE"
  done
  echo "Wrote $OUTFILE"
} > "$DBGFILE" 2>&1

# Also mirror OUTFILE into debug for convenience
cat "$OUTFILE" >> "$DBGFILE" || true
