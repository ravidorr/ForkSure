#!/usr/bin/env bash
set -euo pipefail

# Installs a pre-push git hook that prevents direct pushes to the main branch.
# This is a local safety net; your repo already has server-side protections,
# but this avoids accidental pushes from this machine.
#
# Usage:
#   bash scripts/install_hooks.sh

REPO_ROOT=$(git rev-parse --show-toplevel 2>/dev/null || true)
if [[ -z "${REPO_ROOT}" ]]; then
  echo "Not inside a git repository." >&2
  exit 1
fi

HOOK_DIR="${REPO_ROOT}/.git/hooks"
HOOK_PATH="${HOOK_DIR}/pre-push"

mkdir -p "${HOOK_DIR}"

cat >"${HOOK_PATH}" <<'HOOK'
#!/usr/bin/env bash
# pre-push: block direct pushes to main
set -euo pipefail

# Read refs from stdin: local_ref local_sha remote_ref remote_sha
while read -r local_ref local_sha remote_ref remote_sha; do
  # Normalize ref names if needed
  if [[ "${local_ref}" == "refs/heads/main" ]] || [[ "${remote_ref}" == "refs/heads/main" ]]; then
    echo "[pre-push] Direct pushes to main are blocked. Please open a PR." >&2
    echo "[pre-push] Tip: create a branch and push it, then run: gh pr create --fill --base main" >&2
    exit 1
  fi
done

exit 0
HOOK

chmod +x "${HOOK_PATH}"

echo "Installed pre-push hook at ${HOOK_PATH}"
