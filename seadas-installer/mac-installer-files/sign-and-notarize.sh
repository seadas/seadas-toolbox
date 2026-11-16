#!/bin/bash
# Signs and notarizes the SeaDAS macOS installer for distribution outside the App Store,
# so Gatekeeper opens it without the "Apple could not verify ..." warning.
# Runs on macOS only: codesign, notarytool, stapler and hdiutil are Apple tools.
#
# Usage:
#   sign-and-notarize.sh setup                    save the notarization login in the keychain (once per Mac)
#   sign-and-notarize.sh all <src> <out.dmg>      dmg + notarize + check (the usual command)
#   sign-and-notarize.sh dmg <src> <out.dmg>      copy <src>, sign everything in it, build and sign <out.dmg>
#   sign-and-notarize.sh notarize <file>          notarize a .dmg, .pkg or .zip and staple the ticket
#   sign-and-notarize.sh check <file>             show the signature, Gatekeeper and ticket status
#   sign-and-notarize.sh verify <.dmg|.app|dir>   list native files (also inside jars) the notary would reject
#   sign-and-notarize.sh sign <dir|.app|.jar>     sign all native code in place
#
# <src> is a folder holding the installer files, an .app, or an existing .dmg.
# An existing .dmg is copied, not changed: its files are re-signed and put in a new <out.dmg>.
#
# What gets signed, innermost first:
#   1. Mach-O libraries and executables inside .jar/.zip files (also nested ones). Apple's
#      notary service looks inside jars and rejects the upload if any of them is unsigned.
#   2. Mach-O executables and libraries, such as the bundled JRE's.
#   3. Bundles (.app, .framework, and folders with Contents/Info.plist, such as the JRE), deepest first.
# Everything is signed with the hardened runtime and a secure timestamp, which notarization requires.
# Executables and apps also get the entitlements Java needs (JIT, and loading libraries signed by others).
#
# Settings (environment variables):
#   SIGN_ID         codesign identity; default: the only "Developer ID Application" certificate in the keychain
#   NOTARY_PROFILE  notarytool keychain profile name; default: seadas-notary
#   VOLNAME         volume name of the new .dmg; default: the source .dmg's, else the output file name

set -u -o pipefail

PROFILE=${NOTARY_PROFILE:-seadas-notary}
FAILED=0
BAD=0
SIGNED=0
MOUNTED=
TMPROOT=

die() { echo "error: $*" >&2; exit 1; }
log() { echo "==> $*"; }

abspath() {
    local d
    d=$(cd "$(dirname "$1")" && pwd) || return 1
    echo "$d/$(basename "$1")"
}

cleanup() {
    [ -n "$MOUNTED" ] && hdiutil detach -quiet -force "$MOUNTED" 2>/dev/null
    [ -n "$TMPROOT" ] && rm -rf "$TMPROOT"
}

sign_identity() {
    if [ -n "${SIGN_ID:-}" ]; then
        echo "$SIGN_ID"
        return
    fi
    local ids n
    ids=$(security find-identity -v -p codesigning | sed -n 's/.*"\(Developer ID Application:[^"]*\)".*/\1/p' | sort -u)
    n=$(printf '%s\n' "$ids" | grep -c . || true)
    [ "$n" -eq 1 ] || die "found $n \"Developer ID Application\" certificates; set SIGN_ID to the one to use${ids:+:$'\n'}$ids"
    echo "$ids"
}

write_entitlements() {
    cat > "$ENT" <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>com.apple.security.cs.allow-jit</key><true/>
    <key>com.apple.security.cs.allow-unsigned-executable-memory</key><true/>
    <key>com.apple.security.cs.disable-library-validation</key><true/>
</dict>
</plist>
EOF
}

is_macho()     { file -b "$1" 2>/dev/null | grep -q 'Mach-O'; }
is_macho_exe() { file -b "$1" 2>/dev/null | grep -q 'Mach-O.*executable'; }

# cs <path> [entitlements]: sign with the hardened runtime and a timestamp; count and show failures.
cs() {
    local args=(--force --timestamp --options runtime --sign "$IDENTITY")
    [ -n "${2:-}" ] && args+=(--entitlements "$2")
    if codesign "${args[@]}" "$1" 2>"$TMPROOT/cs.err"; then
        SIGNED=$((SIGNED + 1))
        return 0
    fi
    echo "   FAILED: $1"
    sed 's/^/      /' "$TMPROOT/cs.err"
    FAILED=$((FAILED + 1))
    return 1
}

sign_file() {
    if is_macho_exe "$1"; then cs "$1" "$ENT"; else cs "$1"; fi
}

# has_entries <archive> <regex>: whether any entry name in a jar or zip matches. A listing
# that fails stops the script: hiding it would make the archive look empty, and its native
# code would go unsigned. The whole listing is read first, not piped into 'grep -q', which
# can kill unzip with SIGPIPE under pipefail.
has_entries() {
    local list
    list=$(unzip -Z1 "$1" 2>&1) || die "can't list the files in $1: $list"
    grep -qE "$2" <<<"$list"
}

# Entries that may hold native code: libraries, nested archives, and files without an
# extension, such as bin/opj_compress in SNAP's openjpeg jar, which may be executables.
NATIVE_RE='\.(dylib|jnilib|so(\.[0-9.]+)?|jar|zip)$|(^|/)[^./]+$'

# extract_natives <archive> <dir>: unpack the entries matching NATIVE_RE into <dir>.
# Returns 1 if there are none, 2 if unpacking failed.
extract_natives() {
    local list e names=()
    list=$(unzip -Z1 "$1" 2>&1) || die "can't list the files in $1: $list"
    while IFS= read -r e; do
        # unzip treats names as patterns; put [ ] * ? in brackets to match them literally.
        names+=("$(printf '%s' "$e" | sed 's/[][*?]/[&]/g')")
    done < <(grep -E "$NATIVE_RE" <<<"$list")
    [ ${#names[@]} -gt 0 ] || return 1
    unzip -q -o "$1" "${names[@]}" -d "$2" || return 2
}

# sign_archive <absolute path>: sign the Mach-O libraries inside a jar or zip (recursing into
# nested ones) and put them back. Returns 0 if the archive was changed.
sign_archive() {
    local arc=$1 work changed name rc
    if has_entries "$arc" '^META-INF/[^/]+\.(SF|RSA|DSA|EC)$'; then
        # Re-packing would break the jar's own signature. Fine if no native code in it
        # needs signing; otherwise there is nothing this script can do.
        local bad=$BAD
        BAD=0
        check_archive "$arc" "$arc" >/dev/null
        if [ "$BAD" -gt 0 ]; then
            echo "   FAILED: $arc has its own jar signature and $BAD unsigned native files; it can't be re-packed"
            FAILED=$((FAILED + 1))
        fi
        BAD=$bad
        return 1
    fi
    work=$(mktemp -d "$TMPROOT/arc.XXXXXX")
    changed="$work.changed"
    : > "$changed"
    extract_natives "$arc" "$work"
    rc=$?
    [ $rc -ne 1 ] || return 1
    [ $rc -eq 0 ] || { echo "   FAILED to unpack: $arc"; FAILED=$((FAILED + 1)); return 1; }

    while IFS= read -r -d '' f; do
        name=${f#"$work"/}
        case "$f" in
            *.jar|*.zip)
                sign_archive "$f" && echo "$name" >> "$changed" ;;
            *)
                if is_macho "$f"; then
                    sign_file "$f" && echo "$name" >> "$changed"
                fi ;;
        esac
    done < <(find "$work" -type f -print0)

    [ -s "$changed" ] || return 1
    chmod u+w "$arc"
    if (cd "$work" && zip -q "$arc" -@ < "$changed"); then
        echo "   updated $(wc -l < "$changed" | tr -d ' ') entries in $arc"
        return 0
    fi
    echo "   FAILED to update: $arc"
    FAILED=$((FAILED + 1))
    return 1
}

# check_macho <file> <label>: count and show a Mach-O file that Apple's notary service would
# reject: no Developer ID signature, no secure timestamp, or an executable without the
# hardened runtime.
check_macho() {
    local info why=
    info=$(codesign -dv --verbose=2 "$1" 2>&1)
    grep -q '^Authority=Developer ID Application' <<<"$info" || why="no Developer ID signature"
    [ -n "$why" ] || grep -q '^Timestamp=' <<<"$info" || why="no secure timestamp"
    if [ -z "$why" ] && is_macho_exe "$1"; then
        grep -q 'flags=.*runtime' <<<"$info" || why="no hardened runtime"
    fi
    [ -z "$why" ] && return 0
    BAD=$((BAD + 1))
    [ "$BAD" -le 30 ] && echo "   $why: $2"
    return 1
}

# check_archive <absolute path> <label>: check_macho every Mach-O inside a jar or zip,
# recursing into nested ones.
check_archive() {
    local arc=$1 label=$2 work f rc
    work=$(mktemp -d "$TMPROOT/chk.XXXXXX")
    extract_natives "$arc" "$work"
    rc=$?
    [ $rc -ne 1 ] || return 0
    if [ $rc -ne 0 ]; then
        BAD=$((BAD + 1))
        echo "   can't unpack: $label"
        return
    fi
    while IFS= read -r -d '' f; do
        case "$f" in
            *.jar|*.zip) check_archive "$f" "$label/${f#"$work"/}" ;;
            *)           is_macho "$f" && check_macho "$f" "$label/${f#"$work"/}" ;;
        esac
    done < <(find "$work" -type f -print0)
    rm -rf "$work"
}

# check_tree <dir>: before uploading, find what the notary service would reject, including
# native code inside jars, so a bad build fails here instead of after a 1 GB upload.
check_tree() {
    local root=$1 f
    BAD=0
    log "checking every native file is signed for notarization"
    while IFS= read -r -d '' f; do
        check_archive "$f" "${f#"$root"/}"
    done < <(find "$root" -type f \( -name '*.jar' -o -name '*.zip' \) -print0)
    while IFS= read -r -d '' f; do
        is_macho "$f" && check_macho "$f" "${f#"$root"/}"
    done < <(find "$root" -type f \( -perm -100 -o -name '*.dylib' -o -name '*.jnilib' -o -name '*.so' -o -name '*.so.*' \) -print0)
    [ "$BAD" -le 30 ] || echo "   ... and $((BAD - 30)) more"
    [ "$BAD" -eq 0 ]
}

list_bundles() {
    {
        find "$1" -type d \( -name '*.app' -o -name '*.framework' -o -name '*.bundle' -o -name '*.plugin' -o -name '*.xpc' \)
        find "$1" -type f -path '*/Contents/Info.plist' | sed 's#/Contents/Info\.plist$##'
    } | sort -u | awk -F/ '{ print NF "\t" $0 }' | sort -rn | cut -f2-
}

sign_tree() {
    local root=$1 f b
    log "signing native code in jars under $root"
    while IFS= read -r -d '' f; do
        sign_archive "$f"
    done < <(find "$root" -type f \( -name '*.jar' -o -name '*.zip' \) -print0)

    log "signing executables and libraries"
    while IFS= read -r -d '' f; do
        is_macho "$f" && sign_file "$f"
    done < <(find "$root" -type f \( -perm -100 -o -name '*.dylib' -o -name '*.jnilib' -o -name '*.so' -o -name '*.so.*' \) -print0)

    log "signing bundles"
    while IFS= read -r b; do
        [ -n "$b" ] || continue
        case "$b" in
            *.framework) cs "$b" ;;
            *)           cs "$b" "$ENT" ;;
        esac
    done < <(list_bundles "$root")
}

cmd_sign() {
    local target
    target=$(abspath "$1") || die "not found: $1"
    [ -e "$target" ] || die "not found: $1"
    case "$target" in
        *.jar|*.zip) sign_archive "$target" ;;
        *)           sign_tree "$target" ;;
    esac
    log "signed $SIGNED items, $FAILED failed"
    [ "$FAILED" -eq 0 ]
}

cmd_dmg() {
    local src out stage vol srcvol=
    src=$(abspath "$1") || die "not found: $1"
    [ -e "$src" ] || die "not found: $1"
    out=$(abspath "$2") || die "output folder not found: $2"
    case "$out" in *.dmg) ;; *) die "output must end in .dmg: $2" ;; esac
    [ "$src" != "$out" ] || die "output must differ from the source .dmg"

    stage=$(mktemp -d "$TMPROOT/stage.XXXXXX")
    case "$src" in
        *.dmg)
            log "copying the contents of $src"
            MOUNTED=$(hdiutil attach -nobrowse -readonly -noautoopen "$src" | awk -F'\t' '/\/Volumes\// { print $NF }' | tail -1)
            [ -d "$MOUNTED" ] || die "could not mount $src"
            srcvol=$(basename "$MOUNTED")
            ditto "$MOUNTED" "$stage" || die "copy failed"
            hdiutil detach -quiet "$MOUNTED"
            MOUNTED=
            rm -rf "$stage/.fseventsd" "$stage/.Trashes"
            ;;
        *.app) ditto "$src" "$stage/$(basename "$src")" || die "copy failed" ;;
        *)     [ -d "$src" ] || die "source must be a folder, .app or .dmg: $1"
               ditto "$src" "$stage" || die "copy failed" ;;
    esac
    xattr -dr com.apple.quarantine "$stage" 2>/dev/null

    sign_tree "$stage"
    log "signed $SIGNED items, $FAILED failed"
    [ "$FAILED" -eq 0 ] || die "$FAILED items failed to sign, so the .dmg was not built"
    check_tree "$stage" || die "$BAD native files are not signed for notarization (see above), so the .dmg was not built"

    vol=${VOLNAME:-${srcvol:-$(basename "$out" .dmg)}}
    log "building $out (volume \"$vol\")"
    hdiutil create -quiet -volname "$vol" -srcfolder "$stage" -fs HFS+ -format UDZO -ov "$out" || die "hdiutil create failed"
    codesign --force --timestamp --sign "$IDENTITY" "$out" || die "signing the .dmg failed"
    log "signed $out"
}

cmd_notarize() {
    local f res id status logfile
    f=$(abspath "$1") || die "not found: $1"
    [ -f "$f" ] || die "not found: $1"
    case "$f" in
        *.dmg|*.pkg|*.zip) ;;
        *) die "notarize takes a .dmg, .pkg or .zip (zip an .app with: ditto -c -k --keepParent X.app X.zip)" ;;
    esac
    log "uploading to Apple's notary service and waiting (usually a few minutes)"
    res=$(xcrun notarytool submit "$f" --keychain-profile "$PROFILE" --wait --output-format plist) \
        || { echo "$res"; die "notarytool submit failed (run '$0 setup' first if the profile is missing)"; }
    id=$(printf '%s' "$res" | plutil -extract id raw -o - -)
    status=$(printf '%s' "$res" | plutil -extract status raw -o - -)
    logfile="${f%.*}.notary-log.json"
    xcrun notarytool log "$id" --keychain-profile "$PROFILE" "$logfile" >/dev/null 2>&1
    if [ "$status" != Accepted ]; then
        echo "status: $status (submission $id)"
        [ -f "$logfile" ] && grep -E '"(path|message)"' "$logfile" | head -60
        die "notarization was rejected; full log: $logfile"
    fi
    log "accepted (submission $id, log: $logfile)"
    case "$f" in
        *.zip) echo "   a .zip can't be stapled; staple the .app inside it instead" ;;
        *)     xcrun stapler staple "$f" || die "stapling failed" ;;
    esac
}

cmd_check() {
    local f=$1
    [ -e "$f" ] || die "not found: $f"
    echo "--- signature"
    codesign -dv --verbose=2 "$f" 2>&1 | grep -E 'not signed|Authority|TeamIdentifier|Timestamp|Runtime'
    echo "--- Gatekeeper"
    case "$f" in
        *.dmg) spctl -a -vv -t open --context context:primary-signature "$f" ;;
        *.pkg) spctl -a -vv -t install "$f" ;;
        *)     codesign --verify --deep --strict "$f" && spctl -a -vv -t exec "$f" ;;
    esac
    echo "--- notarization ticket"
    xcrun stapler validate "$f"
    true
}

cmd_verify() {
    local target
    target=$(abspath "$1") || die "not found: $1"
    [ -e "$target" ] || die "not found: $1"
    case "$target" in
        *.dmg)
            MOUNTED=$(hdiutil attach -nobrowse -readonly -noautoopen "$target" | awk -F'\t' '/\/Volumes\// { print $NF }' | tail -1)
            [ -d "$MOUNTED" ] || die "could not mount $target"
            target=$MOUNTED ;;
    esac
    if check_tree "$target"; then
        log "all native files are signed for notarization"
    else
        die "$BAD native files would be rejected"
    fi
}

main() {
    [ $# -ge 1 ] || { sed -n '6,14p' "$0"; exit 1; }
    [ "$(uname -s)" = Darwin ] || die "this script runs on macOS only"
    local cmd=$1
    shift
    TMPROOT=$(mktemp -d "${TMPDIR:-/tmp}/seadas-sign.XXXXXX") || die "could not make a temp folder"
    trap cleanup EXIT
    ENT="$TMPROOT/java.entitlements"

    case "$cmd" in
        setup)
            echo "You'll be asked for the Apple ID, the team ID and an app-specific password"
            echo "(make one at https://account.apple.com > Sign-In and Security > App-Specific Passwords)."
            xcrun notarytool store-credentials "$PROFILE" ;;
        check)
            [ $# -eq 1 ] || die "usage: $0 check <file>"
            cmd_check "$1" ;;
        verify)
            [ $# -eq 1 ] || die "usage: $0 verify <.dmg|.app|dir>"
            cmd_verify "$1" ;;
        notarize)
            [ $# -eq 1 ] || die "usage: $0 notarize <file>"
            cmd_notarize "$1" ;;
        sign|dmg|all)
            IDENTITY=$(sign_identity) || exit 1
            log "signing as: $IDENTITY"
            write_entitlements
            case "$cmd" in
                sign) [ $# -eq 1 ] || die "usage: $0 sign <dir|.app|.jar>"
                      cmd_sign "$1" ;;
                dmg)  [ $# -eq 2 ] || die "usage: $0 dmg <src> <out.dmg>"
                      cmd_dmg "$1" "$2" ;;
                all)  [ $# -eq 2 ] || die "usage: $0 all <src> <out.dmg>"
                      cmd_dmg "$1" "$2" && cmd_notarize "$2" && cmd_check "$2" ;;
            esac ;;
        *)
            die "unknown command '$cmd' (setup, all, dmg, notarize, check, verify, sign)" ;;
    esac
}

main "$@"
