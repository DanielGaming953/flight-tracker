#!/usr/bin/env bash
# Creates a GitHub repo for the Flight Tracker and pushes the local commit.
#
# Usage (run from this project folder):
#   1. Install GitHub CLI:   https://cli.github.com
#   2. One-time login:       gh auth login
#   3. Run this script:      ./push-to-github.sh
#
# After running, fix your commit author if you want a different identity:
#   git config user.name  "Your Name"
#   git config user.email "you@example.com"
#   git commit --amend --reset-author --no-edit
set -euo pipefail

REPO_NAME="flight-tracker"
VISIBILITY="public"

gh repo create "$REPO_NAME" --"$VISIBILITY" --source=. --remote=origin --push
echo
echo "Done! Your repo is live at: https://github.com/$(gh api user --jq .login)/$REPO_NAME"
