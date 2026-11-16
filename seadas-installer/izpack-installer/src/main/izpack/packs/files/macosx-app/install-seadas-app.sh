#!/bin/bash
#
# Installs SeaDAS.app for the macOS installer (install-for-mac-app.xml).
# The installer's ProcessPanel runs it after the user picked where SeaDAS goes:
#
#   install-seadas-app.sh <folder> <remove-old-install>
#
#   <folder>              system: /Applications (all users); user: ~/Applications (only this
#                         user); or an absolute folder
#   <remove-old-install>  true: remove the program files of an old folder install in
#                         ~/SeaDAS (SeaDAS 12.0.0 and older), keeping ocssw and any
#                         other files that aren't part of SeaDAS
#
# SeaDAS.app is copied from SEADAS_APP_SOURCE, set by "Install SeaDAS.app", which carries
# it in Contents/Resources. ditto copies it with its signature and extended attributes.
# If the folder isn't writable (a standard user and /Applications), macOS asks for an
# administrator name and password and only the copy runs as administrator.
#
# Internal: install-seadas-app.sh --copy <source app> <folder>  (the part that may run as root)
#
# Everything is also written to ~/Library/Logs/SeaDAS/install.log.

APP_NAME=SeaDAS.app

say() { echo "$*"; }
fail() { echo "ERROR: $*" >&2; exit 1; }

# The copy: replace <folder>/SeaDAS.app with <source app>. Runs as the user, or as root
# through the administrator prompt.
copy_app() {
    src=$1
    dir=$2
    dest="$dir/$APP_NAME"
    new="$dir/.$APP_NAME.installing.$$"
    old="$dir/.$APP_NAME.old.$$"

    mkdir -p "$dir" || fail "can't create $dir"
    rm -rf "$new"
    ditto "$src" "$new" || { rm -rf "$new"; fail "copying SeaDAS.app to $dir failed"; }

    if [ -e "$dest" ] || [ -L "$dest" ]; then
        mv "$dest" "$old" || { rm -rf "$new"; fail "can't replace $dest"; }
    fi
    if ! mv "$new" "$dest"; then
        [ -e "$old" ] && mv "$old" "$dest"
        rm -rf "$new"
        fail "can't move SeaDAS.app into $dir"
    fi
    rm -rf "$old"

    # As administrator, the app belongs to root like other apps in /Applications, so
    # users can run it but not change it.
    if [ "$(id -u)" = 0 ]; then
        chown -R root:wheel "$dest"
    fi
    chmod -R go-w "$dest"
}

if [ "${1:-}" = "--copy" ]; then
    [ $# -eq 3 ] || fail "usage: $0 --copy <source app> <folder>"
    copy_app "$2" "$3"
    exit 0
fi

[ $# -ge 1 ] || fail "usage: $0 <folder> [<remove-old-install>]"
case "$1" in
    system) DIR=/Applications ;;
    user)   DIR="$HOME/Applications" ;;
    *)      DIR=$1 ;;
esac
REMOVE_OLD=${2:-false}
SRC=${SEADAS_APP_SOURCE:-}
SELF=$(cd "$(dirname "$0")" && pwd)/$(basename "$0")

LOGDIR="$HOME/Library/Logs/SeaDAS"
mkdir -p "$LOGDIR" 2>/dev/null
LOG="$LOGDIR/install.log"
# Output goes to the installer window and to the log.
exec > >(tee -a "$LOG") 2>&1
echo
echo "=== $(date): installing SeaDAS.app in $DIR"

[ -n "$SRC" ] || fail "SEADAS_APP_SOURCE isn't set; start the installer with \"Install SeaDAS\""
[ -x "$SRC/Contents/MacOS/SeaDAS" ] || fail "no SeaDAS.app at $SRC"
case "$DIR" in
    /*) ;;
    *) fail "not an absolute folder: $DIR" ;;
esac

# 1. SeaDAS must not be running from the copy being replaced.
if pgrep -f "$DIR/$APP_NAME/Contents/" >/dev/null 2>&1; then
    fail "SeaDAS is running from $DIR/$APP_NAME. Quit SeaDAS and run the installer again."
fi

# 2. Copy the app, as administrator if the folder needs it.
say "Copying SeaDAS to $DIR ..."
# The folder must be writable, and an app already there must be the user's to replace
# (one installed as administrator belongs to root).
writable=false
if { [ -d "$DIR" ] && [ -w "$DIR" ]; } || { [ ! -e "$DIR" ] && [ -w "$(dirname "$DIR")" ]; }; then
    writable=true
fi
if [ -e "$DIR/$APP_NAME" ] && [ ! -O "$DIR/$APP_NAME" ]; then
    writable=false
fi
if [ "$writable" = true ]; then
    copy_app "$SRC" "$DIR"
else
    say "Installing in $DIR needs an administrator name and password."
    # osascript's errors go to a file: a cancelled prompt gets a plain message instead of
    # AppleScript's "execution error: User canceled. (-128)".
    errfile=$(mktemp "${TMPDIR:-/tmp}/seadas-install.XXXXXX") || fail "can't make a temporary file"
    if ! osascript - "$SELF" "$SRC" "$DIR" 2>"$errfile" <<'EOF'
on run argv
    set cmd to "/bin/bash " & quoted form of item 1 of argv & " --copy " & quoted form of item 2 of argv & " " & quoted form of item 3 of argv
    do shell script cmd with prompt "SeaDAS is being installed in " & item 3 of argv & "." with administrator privileges
end run
EOF
    then
        err=$(cat "$errfile")
        rm -f "$errfile"
        case "$err" in
            *"(-128)"*)
                fail "No administrator password was given, so SeaDAS wasn't installed in $DIR. To install without one, run the installer again and choose \"Only for me\"." ;;
            *)
                [ -n "$err" ] && echo "$err"
                fail "SeaDAS wasn't copied to $DIR (see above). To install without an administrator password, run the installer again and choose \"Only for me\"." ;;
        esac
    fi
    rm -f "$errfile"
fi
APP="$DIR/$APP_NAME"
[ -x "$APP/Contents/MacOS/SeaDAS" ] || fail "SeaDAS.app is missing from $DIR after the copy"
say "Installed $APP"

# 3. Check the signature, and let Finder, Spotlight and Launchpad know about the app.
if codesign --verify --deep --strict "$APP" 2>/dev/null; then
    say "Signature: valid"
else
    say "WARNING: SeaDAS.app is not signed, or its signature is broken (fine for a test build only)"
fi
LSREGISTER=/System/Library/Frameworks/CoreServices.framework/Frameworks/LaunchServices.framework/Support/lsregister
[ -x "$LSREGISTER" ] && "$LSREGISTER" -f "$APP" >/dev/null 2>&1

# 4. The old folder install in ~/SeaDAS. Only its program files are removed; ocssw and
#    anything else stay. It is recognized by its launcher app and settings.
OLD="$HOME/SeaDAS"
if [ "$REMOVE_OLD" = true ] && [ -d "$OLD/bin/seadas.app" ] && [ -f "$OLD/etc/seadas.conf" ]; then
    say "Removing the old SeaDAS program files from $OLD ..."
    for f in bin etc ide platform snap optical-toolbox seadas-toolbox jre jdk-*-jre \
             installinfo Uninstaller .installationinformation THIRDPARTY_LICENSES.txt VERSION.txt; do
        for p in "$OLD"/$f; do
            if [ -e "$p" ] || [ -L "$p" ]; then
                rm -rf "$p" && say "  removed $p"
            fi
        done
    done
    left=$(ls -A "$OLD" 2>/dev/null)
    if [ -n "$left" ]; then
        say "Kept in $OLD:"
        echo "$left" | sed 's/^/  /'
    else
        rmdir "$OLD" 2>/dev/null
    fi
fi

# 5. Start the new SeaDAS with a fresh module cache, so modules of an older version
#    that are still listed there aren't loaded (settings and preferences are kept).
if [ -d "$HOME/.seadas/var/cache" ]; then
    rm -rf "$HOME/.seadas/var/cache" && say "Cleared the SeaDAS module cache (~/.seadas/var/cache)"
fi

say "SeaDAS is installed. Open it from $DIR or Launchpad."
exit 0
