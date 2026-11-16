#!/bin/sh
# Starts the SeaDAS installer in "Install SeaDAS.app" (Contents/Resources/start-installer.sh),
# run by its native launcher. The installer runs on the Java inside the SeaDAS.app it installs,
# Contents/Helpers/SeaDAS.app, and is told where that app is through SEADAS_APP_SOURCE.
# Its output goes to ~/Library/Logs/SeaDAS/installer.log.

contents=$(cd "$(dirname "$0")/.." && pwd)
app="$contents/Helpers/SeaDAS.app"
java="$app/Contents/runtime/Contents/Home/bin/java"

logdir="$HOME/Library/Logs/SeaDAS"
mkdir -p "$logdir" 2>/dev/null
log="$logdir/installer.log"
echo "=== $(date): $contents" >> "$log" 2>/dev/null

if [ ! -x "$java" ]; then
    echo "no Java at $java" >> "$log"
    osascript -e 'display alert "The SeaDAS installer is incomplete." message "Download the SeaDAS installer again." as critical' >/dev/null 2>&1
    exit 1
fi

SEADAS_APP_SOURCE="$app"
export SEADAS_APP_SOURCE
exec "$java" -Xdock:name="Install SeaDAS" -Xdock:icon="$contents/Resources/SeaDAS.icns" \
    -jar "$contents/Resources/seadas-installer.jar" "$@" >> "$log" 2>&1
