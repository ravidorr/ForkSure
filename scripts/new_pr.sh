#!/usr/bin/env bash
set -euo pipefail

# new_pr.sh: Push current branch and open a GitHub Pull Request.
# - If on main, it will create a new branch from main first.
# - Uses gh CLI to create a PR; requires 'gh auth status' to be logged in.
#
# Usage:
#   bash scripts/new_pr.sh "Title of the PR" [--fill]
#   bash scripts/new_pr.sh --fill            # use last commit message as title/body
#
# Options:
#   --base <branch>    Base branch (default: main)
#   --fill             Use commit message(s) for PR title/body
#   --body <text>      PR body text
#
BASE="main"
FILL=0
TITLE=""
BODY=""

# Parse args
while [[ $# -gt 0 ]]; do
  case "$1" in
    --base)
      BASE="$2"; shift 2;;
    --fill)
      FILL=1; shift;;
    --body)
      BODY="$2"; shift 2;;
    *)
      if [[ -z "$TITLE" ]]; then
        TITLE="$1"; shift
      else
        echo "Unknown arg: $1" >&2; exit 2
      fi
      ;;
  esac
done

# Ensure gh
if ! command -v gh >/dev/null 2>&1; then
  echo "GitHub CLI (gh) not found. Install from https://cli.github.com/" >&2
  exit 2
fi

# Ensure repo state
git rev-parse --is-inside-work-tree >/dev/null

CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
if [[ "$CURRENT_BRANCH" == "HEAD" ]]; then
  echo "You are in a detached HEAD state. Please checkout a branch." >&2
  exit 2
fi

# If on main, create a new branch
if [[ "$CURRENT_BRANCH" == "$BASE" ]]; then
  SAFE_NAME=$(date +"feat/%Y%m%d-%H%M%S")
  echo "On $BASE; creating new branch: $SAFE_NAME"
  git fetch origin "$BASE"
  git checkout -b "$SAFE_NAME" "origin/$BASE"
  CURRENT_BRANCH="$SAFE_NAME"
fi

# Push current branch
git push -u origin "$CURRENT_BRANCH"

echo "Opening PR: head=$CURRENT_BRANCH base=$BASE"
if [[ $FILL -eq 1 ]]; then
  gh pr create --base "$BASE" --head "$CURRENT_BRANCH" --fill
else
  if [[ -z "$TITLE" ]]; then
    TITLE=$(git log -1 --pretty=%s)
  fi
  if [[ -z "$BODY" ]]; then
    BODY="Automated PR for branch $CURRENT_BRANCH"
  fi
  gh pr create --base "$BASE" --head "$CURRENT_BRANCH" --title "$TITLE" --body "$BODY"
fi
