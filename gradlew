#!/bin/sh
set -e
DIR="$(cd "$(dirname "$0")" && pwd)"
exec "$DIR/gradle/wrapper/gradle-wrapper.jar" "$@" 2>/dev/null || {
    # Fallback to standard gradle if wrapper jar isn't bundled yet
    gradle assembleDebug "$@"
}
