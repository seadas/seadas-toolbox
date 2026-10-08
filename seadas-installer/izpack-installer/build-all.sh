#!/bin/bash
#
# Build the SeaDAS IzPack installers.
#
#   ./build-all.sh                 build every installer, with and without JRE
#   ./build-all.sh linux win       build only the named platforms
#   ./build-all.sh linux-nojre     Linux installer that uses the machine's JDK
#   OUTDIR=/tmp/installers ./build-all.sh
#
# 'win' and 'win-nojre' also build the Windows installers for users,
# seadas_<version>_windows64_installer.exe and ..._windows64_installer_no_bundled_jre.exe:
# the Maven build lays out the installed SeaDAS folder and the Inno Setup
# script (see pom.xml and ../windows-installer-files), and this script
# compiles it with Inno Setup's ISCC, run under Wine in Docker, so Docker is
# required for both; a run without arguments skips them when Docker is not
# available.  INNO_IMAGE overrides the image.
#
# 'linux' and 'linux-nojre' likewise also build the Linux installers for users,
# seadas_<version>_linux64_installer.sh and ..._linux64_installer_no_bundled_jre.sh:
# self-extracting archives made with makeself, which is required for both and
# skipped the same way when missing.
#
# Platforms: mac, linux, win, and mac-nojre, linux-nojre, win-nojre for the
# installers without a bundled JRE.  Each is selected by Maven profiles (see
# pom.xml): 'linux' is -P linux, 'linux-nojre' is -P linux,nojre.  Nothing is
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

# Inno Setup 6 under Wine, pinned so rebuilds use the same compiler
INNO_IMAGE="${INNO_IMAGE:-amake/innosetup@sha256:e003376ba818547275fe10c95e2a29be0f2d12d45e9eb8f205b6672dc5685bb1}"

if [ $# -gt 0 ]; then
    PLATFORMS="$*"
else
    PLATFORMS="mac linux win mac-nojre linux-nojre win-nojre"
fi

for p in $PLATFORMS; do
    case "$p" in
        mac|linux|win|mac-nojre|linux-nojre|win-nojre) ;;
        *)
            echo "build-all.sh: unknown platform '$p' (expected mac, linux, win, or one of those with -nojre)" >&2
            exit 2
            ;;
    esac
done

# The Windows installers need Docker.  Without it, a default run (no platforms
# named) skips them with a warning; asking for win or win-nojre by name fails.
case " $PLATFORMS " in
    *" win "*|*" win-nojre "*)
        if ! docker info >/dev/null 2>&1; then
            if [ $# -gt 0 ]; then
                echo "build-all.sh: win and win-nojre need Docker to run Inno Setup, and 'docker info' failed" >&2
                exit 1
            fi
            echo "build-all.sh: WARNING: 'docker info' failed, so the Windows installers (win, win-nojre) are skipped; they need Docker to run Inno Setup" >&2
            PLATFORMS="$(echo $PLATFORMS | tr ' ' '\n' | grep -v '^win' | tr '\n' ' ')"
        fi
        ;;
esac

# The Linux installers for users are made with makeself, handled the same way.
MAKESELF="$(command -v makeself || command -v makeself.sh || true)"
case " $PLATFORMS " in
    *" linux "*|*" linux-nojre "*)
        if [ -z "$MAKESELF" ]; then
            if [ $# -gt 0 ]; then
                echo "build-all.sh: linux and linux-nojre need makeself (https://makeself.io/, e.g. 'apt install makeself')" >&2
                exit 1
            fi
            echo "build-all.sh: WARNING: makeself not found, so the Linux installers (linux, linux-nojre) are skipped; install it from https://makeself.io/" >&2
            PLATFORMS="$(echo $PLATFORMS | tr ' ' '\n' | grep -v '^linux' | tr '\n' ' ')"
        fi
        ;;
esac

# The SeaDAS version, for the names of the installers for users
VERSION="$(awk '/<artifactId>seadas<\/artifactId>/{f=1} f && /<version>/{match($0, /<version>[^<]*<\/version>/); print substr($0, RSTART + 9, RLENGTH - 19); exit}' ../../pom.xml)"
if [ -z "$VERSION" ]; then
    echo "build-all.sh: no SeaDAS version in ../../pom.xml" >&2
    exit 1
fi

# Wraps a Linux IzPack jar in a self-extracting seadas_<version>_linux64_installer*.sh.
# The bundled-JRE one carries the Java 21 JRE from packs/jre to run the installer;
# the no-JRE one runs it on the machine's Java (see ../linux-installer-files).
make_linux_sh() {
    local p="$1" jar="$2" stage=target/linux-sh out
    if [ "$p" = linux ]; then
        out="seadas_${VERSION}_linux64_installer.sh"
    else
        out="seadas_${VERSION}_linux64_installer_no_bundled_jre.sh"
    fi
    rm -rf "$stage"
    mkdir -p "$stage"
    ln "$jar" "$stage/seadas-installer.jar" 2>/dev/null || cp "$jar" "$stage/seadas-installer.jar"
    cp ../linux-installer-files/start-installer.sh "$stage/"
    chmod +x "$stage/start-installer.sh"
    if [ "$p" = linux ]; then
        local jres=(src/main/izpack/packs/jre/OpenJDK21U-jre_x64_linux_*.tar.gz)
        if [ ${#jres[@]} -ne 1 ] || [ ! -f "${jres[0]}" ]; then
            echo "build-all.sh: expected one Linux JRE archive in src/main/izpack/packs/jre, found: ${jres[*]}" >&2
            exit 1
        fi
        mkdir "$stage/jre"
        tar -xzf "${jres[0]}" -C "$stage/jre" --strip-components=1
    fi
    # --nox11: never try to open an xterm; the installer opens its own window.
    "$MAKESELF" --nox11 "$stage" "$OUTDIR/$out" "SeaDAS $VERSION installer" ./start-installer.sh
    rm -rf "$stage"
}

# The installers take the SeaDAS modules from seadas-kit's cluster, which only
# 'mvn install' in seadas-toolbox refreshes.  Refuse to package a module whose
# sources are newer than its jar there.  SKIP_CLUSTER_CHECK=1 skips this.
if [ "${SKIP_CLUSTER_CHECK:-0}" != 1 ]; then
    TOOLBOX_DIR=../..
    KIT_MODULES="$TOOLBOX_DIR/seadas-kit/target/netbeans_clusters/seadas/modules"
    stale=""
    for m in $(sed -n 's#.*<module>\(.*\)</module>.*#\1#p' "$TOOLBOX_DIR/pom.xml"); do
        jar="$KIT_MODULES/gov-nasa-gsfc-seadas-$m.jar"
        if [ ! -f "$jar" ] || [ -n "$(find "$TOOLBOX_DIR/$m/src" "$TOOLBOX_DIR/$m/pom.xml" -newer "$jar" -type f -print -quit)" ]; then
            stale="$stale $m"
        fi
    done
    if [ -n "$stale" ]; then
        echo "build-all.sh: seadas-kit's cluster is older than the sources of:$stale" >&2
        echo "Run 'mvn install -Dmaven.test.skip=true' in seadas-toolbox first (or set SKIP_CLUSTER_CHECK=1)." >&2
        exit 1
    fi
fi

mkdir -p "$OUTDIR"
echo "Building:         $PLATFORMS"
echo "Output directory: $OUTDIR"

for p in $PLATFORMS; do
    echo
    echo "=================== $p ==================="

    # linux -> linux, linux-nojre -> linux,nojre
    mvn clean package -P "${p/-/,}"

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

    if [ "${p%-nojre}" = win ]; then
        echo
        echo "=================== $p: Inno Setup ==================="
        # The compiler only reads and writes /work.  No network, so a missing
        # or broken docker0 bridge on the host cannot fail the build.
        docker run --rm --network none -v "$PWD/target/windows:/work" "$INNO_IMAGE" seadas-windows.iss
        # Written by the container's user: copy it rather than move it, so
        # the copy in OUTDIR belongs to whoever runs this script.
        for f in target/windows/out/*.exe; do
            cp "$f" "$OUTDIR"/
            rm -f "$f"
        done
    fi

    if [ "${p%-nojre}" = linux ]; then
        echo
        echo "=================== $p: makeself ==================="
        if [ "$p" = linux ]; then
            make_linux_sh "$p" "$OUTDIR/seadas-installer-linux-x64.jar"
        else
            make_linux_sh "$p" "$OUTDIR/seadas-installer-linux-x64-nojre.jar"
        fi
    fi
done

echo
echo "=================== installers ==================="
ls -lh "$OUTDIR"
