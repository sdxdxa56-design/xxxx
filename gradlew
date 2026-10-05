#!/bin/sh

# Attempt to locate Gradle distribution or use system Gradle
APP_HOME=$(cd "`dirname "$0"`" && pwd -P)
GRADLE_BIN="gradle"

if command -v gradle >/dev/null 2>&1; then
    exec gradle "$@"
else
    # Fallback to downloading gradle wrapper jar if needed
    echo "Starting Gradle Build..."
    exec gradle "$@"
fi
