#!/bin/sh
#
# Starts the SeaDAS IzPack installer inside the .sh installers for Linux and
# macOS (seadas_<version>_linux64_installer*.sh, seadas_<version>_mac*_installer*.sh).
# makeself runs this in the unpacked archive; arguments after '--' on the .sh
# command line are passed on to the installer, e.g. on a machine without a display:
#
#   sh seadas_<version>_linux64_installer.sh -- -console
#
# The installers with a bundled JRE carry a Java 21 JRE in ./jre to run on
# (./jre/bin/java on Linux, ./jre/Contents/Home/bin/java on macOS).  Those without
# use JAVA_HOME, else on macOS the newest Java 21+ that /usr/libexec/java_home
# knows, else 'java' on PATH; the installer then offers that Java's folder for
# SeaDAS to use.

if [ -x ./jre/bin/java ]; then
    JAVA=./jre/bin/java
elif [ -x ./jre/Contents/Home/bin/java ]; then
    JAVA=./jre/Contents/Home/bin/java
else
    JAVA=""
    if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
        JAVA="$JAVA_HOME/bin/java"
    elif [ -x /usr/libexec/java_home ] && home=$(/usr/libexec/java_home -v 21+ 2>/dev/null) && [ -x "$home/bin/java" ]; then
        JAVA="$home/bin/java"
    else
        JAVA=$(command -v java || true)
    fi

    # "21.0.8", "25", "25-ea" -> 21, 25, 25;  "1.8.0_271" -> 8
    version=""
    if [ -n "$JAVA" ]; then
        version=$("$JAVA" -version 2>&1 | sed -n 's/.* version "\([^"]*\)".*/\1/p' | head -n 1)
    fi
    major=$(echo "$version" | sed 's/^1\.//; s/[^0-9].*//')
    if [ -z "$major" ] || [ "$major" -lt 21 ]; then
        echo "The SeaDAS installer needs Java 21 or newer, and found ${version:-no Java} (${JAVA:-not on PATH})." >&2
        echo "Install a Java 21 JDK (for example from https://adoptium.net/) and set JAVA_HOME to it," >&2
        echo "or use the SeaDAS installer with a bundled JRE." >&2
        exit 1
    fi
fi

exec "$JAVA" -jar seadas-installer.jar "$@"
