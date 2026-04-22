#!/bin/bash

KEEP_BRANCH="development"

echo "Switching to $KEEP_BRANCH..."
git checkout $KEEP_BRANCH || exit 1

echo "Deleting all LOCAL branches except $KEEP_BRANCH..."
git branch | grep -v "$KEEP_BRANCH" | while read branch; do
  echo "Deleting local branch: $branch"
  git branch -D "$branch"
done

echo "Fetching latest remote branches..."
git fetch --prune

echo "Deleting all REMOTE branches except $KEEP_BRANCH..."
git branch -r | grep -v "$KEEP_BRANCH" | grep -v "HEAD" | while read remote; do
  branch_name=${remote#origin/}
  echo "Deleting remote branch: $branch_name"
  git push origin --delete "$branch_name"
done

echo "✅ Done. Only '$KEEP_BRANCH' remains (local + remote)."
