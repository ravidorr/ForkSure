#!/usr/bin/env bash
set -euo pipefail

# Verify that all native libraries in the given Android App Bundle (AAB)
# declare PT_LOAD segment alignment >= 16 KB (0x4000), which implies 16 KB
# page-size support.
#
# Usage:
#   bash scripts/verify_16kb.sh [PATH_TO_AAB]
#
# Defaults to app/build/outputs/bundle/release/app-release.aab if not provided.

AAB_PATH=${1:-app/build/outputs/bundle/release/app-release.aab}

if [[ ! -f "$AAB_PATH" ]]; then
  echo "[verify_16kb] AAB not found at '$AAB_PATH'. Building release bundle..." >&2
  ./gradlew --no-daemon :app:bundleRelease
fi

if [[ ! -f "$AAB_PATH" ]]; then
  echo "[verify_16kb] ERROR: AAB still not found at '$AAB_PATH' after build." >&2
  exit 2
fi

echo "[verify_16kb] Using AAB: $AAB_PATH"

# Ensure tools exist
if ! command -v unzip >/dev/null 2>&1; then
  echo "[verify_16kb] ERROR: 'unzip' not found in PATH." >&2
  exit 2
fi
if ! command -v readelf >/dev/null 2>&1; then
  echo "[verify_16kb] ERROR: 'readelf' not found in PATH. Install binutils." >&2
  exit 2
fi

WORKDIR=$(mktemp -d)
trap 'rm -rf "$WORKDIR"' EXIT

unzip -q "$AAB_PATH" -d "$WORKDIR"

mapfile -t SO_FILES < <(find "$WORKDIR" -type f -name '*.so' | sort)
if [[ ${#SO_FILES[@]} -eq 0 ]]; then
  echo "[verify_16kb] No .so files found in AAB (no native code)." >&2
  exit 0
fi

echo "[verify_16kb] Scanning ${#SO_FILES[@]} native libraries..."

non_compliant=0
for so in "${SO_FILES[@]}"; do
  echo "== $so =="
  # Extract the 'Align' value of PT_LOAD rows from readelf -l output.
  # Use -W to prevent line wrapping that breaks field parsing on CI
  aligns=$(readelf -W -l "$so" | awk '/^  LOAD/ {print $NF}')
  if [[ -z "$aligns" ]]; then
    echo "  RESULT: UNKNOWN (no LOAD rows found)"
    non_compliant=$((non_compliant+1))
    echo
    continue
  fi
  all_ge_16k=1
  while IFS= read -r a; do
    [[ -z "$a" ]] && continue
    ah=$(echo "$a" | tr 'A-Z' 'a-z')
    # Convert hex to decimal; default to 0 on unexpected format
    if [[ "$ah" == 0x* ]] || [[ "$ah" == 0X* ]]; then
      val=$((16#${ah#0x}))
    else
      # Some readelf builds may print decimal; fall back to parsing as decimal
      # Remove any leading zeros/spaces
      val=$(echo "$ah" | sed 's/^0*//')
      [[ -z "$val" ]] && val=0
    fi
    echo "  LOAD Align=$ah ($val)"
    if (( val < 16384 )); then
      all_ge_16k=0
    fi
  done <<< "$aligns"

  if (( all_ge_16k == 1 )); then
    echo "  RESULT: COMPLIANT (all LOAD Align >= 0x4000)"
  else
    echo "  RESULT: NON-COMPLIANT (one or more LOAD Align < 0x4000)"
    non_compliant=$((non_compliant+1))
  fi
  echo
done

if (( non_compliant > 0 )); then
  echo "[verify_16kb] One or more native libraries are NOT 16 KB aligned." >&2
  exit 1
fi

echo "[verify_16kb] All native libraries are 16 KB aligned (>= 0x4000)."
