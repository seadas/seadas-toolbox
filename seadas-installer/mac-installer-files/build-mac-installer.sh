#!/bin/bash
# Builds "Install SeaDAS.app", the app on the macOS .dmg that installs SeaDAS.app in
# /Applications or ~/Applications.
#
# Usage:
#   build-mac-installer.sh [--app <SeaDAS.app>] [--jar <installer jar>] [--out <dir>] [--version <x.y.z>]
#
#   --app      SeaDAS.app from build-mac-app.sh (default: ../izpack-installer/dist/SeaDAS.app)
#   --jar      the installer from 'mvn package -P mac-app' in izpack-installer
#              (default: ../izpack-installer/dist/seadas-installer-macos-app.jar, else target/)
#   --out      where "Install SeaDAS.app" is written (default: the folder of --app)
#   --version  app version (default: the version in the root pom.xml)
#
# Layout:
#   Install SeaDAS.app/Contents/MacOS/Install SeaDAS        native launcher (app/launcher.c)
#   Install SeaDAS.app/Contents/Resources/start-installer.sh  runs the installer jar on the
#                                                           Java of the SeaDAS.app below
#   Install SeaDAS.app/Contents/Resources/seadas-installer.jar
#   Install SeaDAS.app/Contents/Helpers/SeaDAS.app          the app that gets installed
#
# Then sign, make the .dmg and notarize, on a Mac:
#   VOLNAME="SeaDAS <version>" sign-and-notarize.sh all "<out>/Install SeaDAS.app" \
#       <out>/seadas_<version>_mac_aarch64_installer.dmg
#
# On a Mac the launchers are compiled (Xcode command line tools: xcode-select --install); a
# SeaDAS.app built elsewhere gets its compiled launcher here too. Elsewhere script stand-ins
# are used, for layout checks only.

set -u -o pipefail

here=$(cd "$(dirname "$0")" && pwd)
dist="$here/../izpack-installer/dist"
APP="$dist/SeaDAS.app"
JAR=""
OUT=""
VERSION=""

die() { echo "build-mac-installer.sh: $*" >&2; exit 1; }
log() { echo "==> $*"; }

while [ $# -gt 0 ]; do
    case "$1" in
        --app)      APP=$2; shift 2 ;;
        --jar)      JAR=$2; shift 2 ;;
        --out)      OUT=$2; shift 2 ;;
        --version)  VERSION=$2; shift 2 ;;
        -h|--help)  sed -n '2,28p' "$0"; exit 0 ;;
        *)          die "unknown option $1 (see --help)" ;;
    esac
done

if [ -z "$VERSION" ]; then
    VERSION="$(awk '/<artifactId>seadas<\/artifactId>/{f=1} f && /<version>/{match($0, /<version>[^<]*<\/version>/); print substr($0, RSTART + 9, RLENGTH - 19); exit}' "$here/../../pom.xml")"
    VERSION=${VERSION%-SNAPSHOT}
fi
[ -n "$VERSION" ] || die "could not read the version from pom.xml; pass --version"

[ -d "$APP/Contents/app" ] && [ -d "$APP/Contents/runtime" ] || die "not a SeaDAS.app from build-mac-app.sh: $APP"
APP=$(cd "$APP" && pwd)
if [ -z "$JAR" ]; then
    for j in "$dist/seadas-installer-macos-app.jar" "$here/../izpack-installer/target/seadas-installer-macos-app.jar"; do
        [ -f "$j" ] && { JAR=$j; break; }
    done
fi
[ -n "$JAR" ] && [ -f "$JAR" ] || die "no installer jar; run 'mvn package -P mac-app' in izpack-installer, or pass --jar"
OUT=${OUT:-$(dirname "$APP")}
mkdir -p "$OUT" || die "can't create $OUT"
OUT=$(cd "$OUT" && pwd)

on_mac=false
if [ "$(uname -s)" = Darwin ] && command -v cc >/dev/null 2>&1; then
    on_mac=true
fi

# compile <output> [cc flags]: the native launcher, or a script stand-in off a Mac.
compile() {
    local out=$1 script=$2
    shift 2
    if [ "$on_mac" = true ]; then
        cc -O2 -arch arm64 -mmacosx-version-min=12.0 "$@" -o "$out" "$here/app/launcher.c" \
            || die "compiling launcher.c failed"
    else
        printf '#!/bin/sh\nexec /bin/sh "$(cd "$(dirname "$0")" && pwd)/%s" "$@"\n' "$script" > "$out"
    fi
    chmod 755 "$out"
}

# 1. SeaDAS.app built on another system has a script stand-in as its launcher.
if [ "$on_mac" = true ] && ! file "$APP/Contents/MacOS/SeaDAS" | grep -q Mach-O; then
    log "compiling the launcher of $APP"
    chmod u+w "$APP/Contents/MacOS" "$APP/Contents/MacOS/SeaDAS"
    compile "$APP/Contents/MacOS/SeaDAS" ../app/bin/seadas
    chmod go-w "$APP/Contents/MacOS/SeaDAS"
fi
[ "$on_mac" = true ] || log "WARNING: not on a Mac with a compiler; using script launchers (for layout checks only, not for release)"

# 2. The installer app.
inst="$OUT/Install SeaDAS.app"
log "building $inst (SeaDAS $VERSION)"
rm -rf "$inst"
mkdir -p "$inst/Contents/MacOS" "$inst/Contents/Resources" "$inst/Contents/Helpers" || exit 1

log "copying $(basename "$APP") ($(du -sh "$APP" | cut -f1))"
if command -v ditto >/dev/null 2>&1; then
    ditto "$APP" "$inst/Contents/Helpers/SeaDAS.app" || die "copying SeaDAS.app failed"
else
    cp -a "$APP" "$inst/Contents/Helpers/SeaDAS.app" || die "copying SeaDAS.app failed"
fi

cp "$JAR" "$inst/Contents/Resources/seadas-installer.jar" || exit 1
cp "$here/installer/start-installer.sh" "$inst/Contents/Resources/" || exit 1
chmod 755 "$inst/Contents/Resources/start-installer.sh"
cp "$APP/Contents/Resources/SeaDAS.icns" "$inst/Contents/Resources/SeaDAS.icns" || die "no SeaDAS.icns in $APP"
sed "s|@VERSION@|$VERSION|g" "$here/installer/Info.plist.in" > "$inst/Contents/Info.plist"
printf 'APPL????' > "$inst/Contents/PkgInfo"
compile "$inst/Contents/MacOS/Install SeaDAS" ../Resources/start-installer.sh \
    -DNAME='"Install SeaDAS"' -DSCRIPT='"../Resources/start-installer.sh"'

chmod -R go-w,a+rX "$inst"

log "done: $inst ($(du -sh "$inst" | cut -f1))"
echo "Next, on a Mac: VOLNAME=\"SeaDAS $VERSION\" $here/sign-and-notarize.sh all \"$inst\" \"$OUT/seadas_${VERSION}_mac_aarch64_installer.dmg\""
