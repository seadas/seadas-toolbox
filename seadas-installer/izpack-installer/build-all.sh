#!/bin/bash
#
# Build the SeaDAS IzPack installers.
#
#   ./build-all.sh                 build mac, linux and win
#   ./build-all.sh linux win       build only the named platforms
#   OUTDIR=/tmp/installers ./build-all.sh
#
# Each platform is selected by a Maven profile (see pom.xml); nothing is
# copied over install.xml.  'mvn clean package' wipes target/, so every
# artifact is moved into OUTDIR before the next platform starts.
#
# Artifacts are moved into OUTDIR, overwriting the previous build of the
# same platform, so repeated runs do not pile up.  Nothing is deleted up
# front: a build that fails leaves the last good installer in place, and
# platforms that are not being built are never touched.
#
# Roughly 2-3 minutes and ~900MB per platform.

set -eu

cd "$(dirname "$0")"

OUTDIR="${OUTDIR:-$PWD/dist}"

if [ $# -gt 0 ]; then
    PLATFORMS="$*"
else
    PLATFORMS="mac linux win"
fi

for p in $PLATFORMS; do
    case "$p" in
        mac|linux|win) ;;
        *)
            echo "build-all.sh: unknown platform '$p' (expected mac, linux or win)" >&2
            exit 2
            ;;
    esac
done

mkdir -p "$OUTDIR"
echo "Building:         $PLATFORMS"
echo "Output directory: $OUTDIR"

for p in $PLATFORMS; do
    echo
    echo "=================== $p ==================="

    mvn clean package -P "$p"

    if ! ls target/seadas-installer-*.jar >/dev/null 2>&1; then
        echo "build-all.sh: the $p build produced no installer jar" >&2
        exit 1
    fi

    # Move the artifacts out before the next build's 'clean' removes them.
    for f in target/seadas-installer-*.jar target/seadas-installer-*.exe; do
        if [ -e "$f" ]; then
            mv "$f" "$OUTDIR"/
        fi
    done
done

echo
echo "=================== installers ==================="
ls -lh "$OUTDIR"
