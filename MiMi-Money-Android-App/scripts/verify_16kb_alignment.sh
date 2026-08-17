#!/usr/bin/env bash
set -euo pipefail

project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
apk="${1:-$project_root/app/build/outputs/apk/debug/app-debug.apk}"
aab="${2:-$project_root/app/build/outputs/bundle/release/app-release.aab}"

sdk_dir="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [[ -z "$sdk_dir" && -f "$project_root/local.properties" ]]; then
  sdk_dir="$(sed -n 's/^sdk\.dir=//p' "$project_root/local.properties" | head -n 1)"
fi
if [[ -z "$sdk_dir" ]]; then
  echo "Android SDK not found. Set ANDROID_SDK_ROOT or sdk.dir." >&2
  exit 2
fi

readelf_bin="$(find "$sdk_dir/ndk" -type f -name llvm-readelf -print 2>/dev/null | sort -V | tail -n 1)"
zipalign_bin="$(find "$sdk_dir/build-tools" -type f -name zipalign -print 2>/dev/null | sort -V | tail -n 1)"
if [[ -z "$readelf_bin" || -z "$zipalign_bin" ]]; then
  echo "llvm-readelf or zipalign is missing from the Android SDK." >&2
  exit 2
fi
if [[ ! -f "$apk" || ! -f "$aab" ]]; then
  echo "Build the debug APK and release AAB before running this check." >&2
  exit 2
fi

work_dir="$(mktemp -d "${TMPDIR:-/tmp}/mimi-16kb.XXXXXX")"
trap 'rm -rf "$work_dir"' EXIT
unzip -q "$aab" 'base/lib/arm64-v8a/*.so' 'base/lib/x86_64/*.so' -d "$work_dir"

status=0
while IFS= read -r library; do
  alignments="$("$readelf_bin" -lW "$library" | awk '/ LOAD / {print $NF}' | sort -u | tr '\n' ',')"
  printf '%s: %s\n' "${library#"$work_dir/"}" "$alignments"
  if [[ "$alignments" == *"0x1000"* ]]; then
    status=1
  fi
done < <(find "$work_dir/base/lib/arm64-v8a" "$work_dir/base/lib/x86_64" -type f -name '*.so' | sort)

"$zipalign_bin" -c -P 16 4 "$apk"
if [[ "$status" -ne 0 ]]; then
  echo "Found a 4 KB-aligned 64-bit native library." >&2
  exit "$status"
fi
echo "16 KB ELF and APK zip alignment verified."
