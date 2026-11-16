#!/bin/bash
# Builds SeaDAS.app, a self-contained macOS application (Apple silicon), from the same files
# the IzPack installer packs: izpack-installer/src/main/izpack/packs, filled by the izpack build.
#
# Usage:
#   build-mac-app.sh [--packs <dir>] [--out <dir>] [--version <x.y.z>] [--max-heap <size>]
#
#   --packs     the IzPack packs folder (default: ../izpack-installer/src/main/izpack/packs)
#   --out       where SeaDAS.app is written (default: ../izpack-installer/dist)
#   --version   app version (default: the version in the root pom.xml)
#   --max-heap  Java maximum heap (default: 12G, as the IzPack installer sets it)
#
# Layout of the app:
#   SeaDAS.app/Contents/Info.plist
#   SeaDAS.app/Contents/MacOS/SeaDAS          native launcher (launcher.c), runs app/bin/seadas
#   SeaDAS.app/Contents/Resources/SeaDAS.icns
#   SeaDAS.app/Contents/app/                  what the folder install puts in ~/SeaDAS:
#                                             bin, etc, platform, snap, ide, optical-toolbox, seadas-toolbox
#   SeaDAS.app/Contents/runtime/              the bundled Java runtime (a bundle of its own)
#
# On a Mac the launcher is compiled with the Xcode command line tools (xcode-select --install).
# Elsewhere a shell-script stand-in is used, which is fine for checking the layout but not for
# a release: sign and notarize on a Mac with sign-and-notarize.sh.

set -u -o pipefail

here=$(cd "$(dirname "$0")" && pwd)
PACKS="$here/../izpack-installer/src/main/izpack/packs"
OUT="$here/../izpack-installer/dist"
VERSION=""
MAX_HEAP="12G"

die() { echo "build-mac-app.sh: $*" >&2; exit 1; }
log() { echo "==> $*"; }

while [ $# -gt 0 ]; do
    case "$1" in
        --packs)    PACKS=$2; shift 2 ;;
        --out)      OUT=$2; shift 2 ;;
        --version)  VERSION=$2; shift 2 ;;
        --max-heap) MAX_HEAP=$2; shift 2 ;;
        -h|--help)  sed -n '2,13p' "$0"; exit 0 ;;
        *)          die "unknown option $1 (see --help)" ;;
    esac
done

if [ -z "$VERSION" ]; then
    VERSION="$(awk '/<artifactId>seadas<\/artifactId>/{f=1} f && /<version>/{match($0, /<version>[^<]*<\/version>/); print substr($0, RSTART + 9, RLENGTH - 19); exit}' "$here/../../pom.xml")"
    VERSION=${VERSION%-SNAPSHOT}
fi
[ -n "$VERSION" ] || die "could not read the version from pom.xml; pass --version"

PACKS=$(cd "$PACKS" 2>/dev/null && pwd) || die "packs folder not found: $PACKS"
for d in snap/platform snap/snap opttbx/modules seadas/modules files/macosx/etc; do
    [ -d "$PACKS/$d" ] || die "missing $PACKS/$d; build the IzPack packs first (izpack-installer/build-all.sh mac)"
done
jres=("$PACKS"/jre/OpenJDK21U-jre_aarch64_mac_*.tar.gz)
[ -f "${jres[0]}" ] || die "no macOS aarch64 JRE in $PACKS/jre"
[ ${#jres[@]} -eq 1 ] || die "more than one macOS aarch64 JRE in $PACKS/jre"

mkdir -p "$OUT" || die "can't create $OUT"
OUT=$(cd "$OUT" && pwd)
app="$OUT/SeaDAS.app"
contents="$app/Contents"
home="$contents/app"

log "building $app (SeaDAS $VERSION)"
rm -rf "$app"
mkdir -p "$contents/MacOS" "$contents/Resources" "$home/bin" "$home/etc" "$contents/runtime" || exit 1

# 1. SNAP (platform, snap, ide, ...) without its own bin and etc, as the IzPack Mac pack does.
log "copying SNAP"
(cd "$PACKS/snap" && tar -cf - --exclude=./bin --exclude=./etc .) | (cd "$home" && tar -xf -) || die "copying SNAP failed"

# 2. The Mac bin and etc files. The old seadas.app wrapper, the folder-install launcher and the
#    installer's .seadas archive script are not part of the app.
log "copying the Mac launcher and settings"
for f in "$PACKS"/files/macosx/bin/*; do
    case "$(basename "$f")" in
        seadas|seadas.app|archiveDotSeaDAS.sh) ;;
        *) cp -p "$f" "$home/bin/" ;;
    esac
done
[ -f "$PACKS/files/unix/bin/start_ocssw_docker" ] && cp -p "$PACKS/files/unix/bin/start_ocssw_docker" "$home/bin/"
cp -p "$PACKS"/files/macosx/etc/* "$home/etc/"
cp "$here/app/seadas" "$home/bin/seadas"
chmod 755 "$home/bin/seadas" "$home/bin/gpt" "$home/bin/pconvert" "$home/bin/gpt-mac.sh" "$home/bin/start_ocssw_docker" 2>/dev/null

# Fill in what IzPack fills in at install time (the "parsable" files).
for conf in "$home/etc/seadas.conf" "$home/etc/snap.conf"; do
    [ -f "$conf" ] || continue
    sed -e "s|\${installer:maxHeapSize}|$MAX_HEAP|g" \
        -e 's|^jdkhome=.*|jdkhome="../runtime/Contents/Home"|' \
        -e 's|^extra_clusters=.*|extra_clusters=""|' \
        "$conf" > "$conf.tmp" && mv "$conf.tmp" "$conf"
done
if grep -n '\${installer:\|\${jdkhome}\|\${extra_clusters}' "$home"/etc/*.conf; then
    die "placeholders left in etc/*.conf (above)"
fi

# 3. The toolboxes, as their own clusters.
log "copying the optical toolbox and the SeaDAS toolbox"
mkdir -p "$home/optical-toolbox" "$home/seadas-toolbox"
(cd "$PACKS/opttbx" && tar -cf - .) | (cd "$home/optical-toolbox" && tar -xf -) || die "copying opttbx failed"
(cd "$PACKS/seadas" && tar -cf - .) | (cd "$home/seadas-toolbox" && tar -xf -) || die "copying seadas failed"

# 4. Java. The Adoptium archive holds jdk-<version>-jre/Contents/...; it becomes Contents/runtime.
log "unpacking $(basename "${jres[0]}")"
tar -xzf "${jres[0]}" -C "$contents/runtime" --strip-components=1 || die "unpacking the JRE failed"
[ -x "$contents/runtime/Contents/Home/bin/java" ] || die "no Contents/Home/bin/java in the JRE archive"

# 5. Bundle files.
sed "s|@VERSION@|$VERSION|g" "$here/app/Info.plist.in" > "$contents/Info.plist"
printf 'APPL????' > "$contents/PkgInfo"
cp "$PACKS/files/macosx/bin/seadas.app/Contents/Resources/app.icns" "$contents/Resources/SeaDAS.icns" \
    || die "app icon not found"

# 6. The main executable.
if [ "$(uname -s)" = Darwin ] && command -v cc >/dev/null 2>&1; then
    log "compiling the launcher"
    cc -O2 -arch arm64 -mmacosx-version-min=12.0 -o "$contents/MacOS/SeaDAS" "$here/app/launcher.c" \
        || die "compiling launcher.c failed"
else
    log "WARNING: not on a Mac with a compiler; using a script launcher (for layout checks only, not for release)"
    cat > "$contents/MacOS/SeaDAS" <<'EOF'
#!/bin/sh
exec /bin/sh "$(cd "$(dirname "$0")/../app/bin" && pwd)/seadas" "$@"
EOF
fi
chmod 755 "$contents/MacOS/SeaDAS"

# 7. Nobody but the owner may change the app; everyone may read and run it.
chmod -R go-w,a+rX "$app"

log "done: $app ($(du -sh "$app" | cut -f1))"
