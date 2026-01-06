#!/bin/bash

# Script to download all SQL files from pgmq GitHub repository
# Source: https://github.com/pgmq/pgmq/tree/main/pgmq-extension/sql

set -e

REPO_OWNER="pgmq"
REPO_NAME="pgmq"
REPO_PATH="pgmq-extension/sql"
BRANCH="main"

# Directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "Downloading SQL files from ${REPO_OWNER}/${REPO_NAME}/${REPO_PATH}..."
echo "Target directory: ${SCRIPT_DIR}"
echo ""

# GitHub API URL to list directory contents
API_URL="https://api.github.com/repos/${REPO_OWNER}/${REPO_NAME}/contents/${REPO_PATH}?ref=${BRANCH}"

# Fetch the list of files using GitHub API (excluding pgmq.sql)
echo "Fetching file list from GitHub API..."
FILES=$(curl -s "${API_URL}" | grep -o '"name": *"[^"]*\.sql"' | sed 's/"name": *"\([^"]*\)"/\1/')

if [ -z "$FILES" ]; then
    echo "Error: No SQL files found or unable to fetch from GitHub API"
    exit 1
fi

# Count total files
TOTAL=$(echo "$FILES" | wc -l | tr -d ' ')
echo "Found ${TOTAL} SQL files to download"
echo ""

# Download each SQL file
COUNT=0
for FILE in $FILES; do
    COUNT=$((COUNT + 1))
    RAW_URL="https://raw.githubusercontent.com/${REPO_OWNER}/${REPO_NAME}/${BRANCH}/${REPO_PATH}/${FILE}"
    echo "[${COUNT}/${TOTAL}] Downloading ${FILE}..."

    curl -s -o "${SCRIPT_DIR}/${FILE}" "${RAW_URL}"

    # shellcheck disable=SC2181
    if [ $? -eq 0 ]; then
        echo "  ✓ Successfully downloaded ${FILE}"
    else
        echo "  ✗ Failed to download ${FILE}"
    fi
done

echo ""
echo "Download complete! ${COUNT} files processed."
echo ""
echo "Downloaded files:"
ls -1 "${SCRIPT_DIR}"/*.sql 2>/dev/null || echo "No SQL files found"
