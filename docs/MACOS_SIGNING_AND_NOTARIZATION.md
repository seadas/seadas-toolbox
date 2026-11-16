# SeaDAS macOS Signing and Notarization Manual

Last updated: November 16, 2026 (SeaDAS 12.0.0)

## Overview

The SeaDAS macOS release is a `.dmg` that Apple has notarized, so it opens without the "Apple could not verify…" warning. Producing it takes about an hour on a Mac with the NASA Developer ID certificate, most of it waiting for Apple.

- **Signing** puts the NASA Developer ID signature on every native file: the launchers, the bundled Java, and the native libraries inside SeaDAS's jars. It runs on your Mac with `codesign`.
- **Notarization** uploads the signed `.dmg` to Apple, which scans it and either accepts or rejects it. On acceptance, a ticket is stapled to the `.dmg`, so it opens even without a network connection.
- **What users get:** `seadas_<version>_mac_aarch64_installer.dmg`, holding **Install SeaDAS.app**. That app installs SeaDAS.app in `/Applications` or `~/Applications`.

The scripts live in [`seadas-installer/mac-installer-files/`](../seadas-installer/mac-installer-files/). The build steps are also summarized in [`seadas-installer/README.md`](../seadas-installer/README.md), section 5.

The work splits across three places:

1. **Build machine (Linux or Mac):** builds SeaDAS.app and the installer jar.
2. **Signing Mac (Apple silicon):** builds Install SeaDAS.app, signs everything, makes the `.dmg`.
3. **Apple's notary service:** checks the upload, then the Mac staples the ticket.

## One-time setup

Each person who signs needs four things: an Apple Account on NASA's developer team, the certificate in their Mac's keychain, an app-specific password, and a saved notarization login. Do this once per person and Mac.

1. **Join the developer team.** Ask the NASA certificate contact to add your Apple Account to team **82A95CK2HC** with the **Developer** role or higher. Accept the email invitation by signing in with that Apple Account. A personal Apple Developer membership on another team does not work: notarization needs an account on the same team as the certificate.
2. **Get the certificate.** The certificate contact provides `Developer ID Application: NASA (82A95CK2HC)` as a `.p12` file plus its password. It expires on 2029-12-07. You don't need the "Developer ID Installer" certificate.
3. **Import it.** Double-click the `.p12` and enter its password into the **login** keychain.
4. **Add Apple's intermediate certificate.** Download **Developer ID - G2** (`DeveloperIDG2CA.cer`) from [apple.com/certificateauthority](https://www.apple.com/certificateauthority/) and double-click it. Without it, the certificate imports but isn't valid.
5. **Check.** In Terminal, run `security find-identity -v -p codesigning`. It should print `1 valid identities found` with the NASA line.
6. **Install Apple's command line tools:** `xcode-select --install`.
7. **Make an app-specific password.** Go to [account.apple.com](https://account.apple.com), then Sign-In and Security, then App-Specific Passwords. Add one named "SeaDAS notary" and copy it.
8. **Save the notarization login:**

   ```bash
   xcrun notarytool store-credentials seadas-notary --apple-id <your Apple Account email> --team-id 82A95CK2HC
   ```

   Paste the app-specific password when asked. It should end with "Credentials validated". `./sign-and-notarize.sh setup` does the same but asks every question; press Enter at the first one, "Path to App Store Connect API private key".

Never email, paste or commit the `.p12` file or its password.

## Build the inputs

The signing Mac needs three things from a normal SeaDAS build: **SeaDAS.app**, the installer jar **seadas-installer-macos-app.jar**, and the **mac-installer-files** folder. You can build them on Linux or on the Mac.

1. Build snap-engine, snap-desktop, optical-toolbox and seadas-toolbox with `mvn install -Dmaven.test.skip=true`, in that order and on matching branches (README section 1).
2. Build the Mac packs and the installer jar:

   ```bash
   cd <seadas>/seadas-toolbox/seadas-installer/izpack-installer
   ./build-all.sh mac mac-app
   ```

3. Build SeaDAS.app from those packs:

   ```bash
   ../mac-installer-files/build-mac-app.sh     # writes dist/SeaDAS.app
   ```

   It takes the version from the root `pom.xml`; `--version` overrides it.
4. If you built on Linux, copy the three items to the Mac with `tar` and `scp`, which keep permissions and links:

   ```bash
   cd <seadas>/seadas-toolbox/seadas-installer
   tar -czf seadas-mac-inputs.tar.gz -C izpack-installer/dist SeaDAS.app seadas-installer-macos-app.jar \
       -C .. mac-installer-files
   ```

   On the Mac, unpack it in Terminal with `tar -xzf`. Don't open it in Finder: macOS then marks the files as downloaded and blocks the unsigned app.

A SeaDAS.app built on Linux has a script stand-in for its launcher. That's fine: the next step compiles the real launcher on the Mac.

## Sign, notarize and staple on the Mac

Run these in Terminal, in the folder holding the three inputs. Replace `12.0.0` with the release version. Use a steady network connection: the upload is about 1 GB.

1. **Build Install SeaDAS.app** (about a minute). This also compiles the launchers:

   ```bash
   ./mac-installer-files/build-mac-installer.sh --app SeaDAS.app --jar seadas-installer-macos-app.jar --out . --version 12.0.0
   ```

2. **Sign everything and make the .dmg** (several minutes):

   ```bash
   VOLNAME="SeaDAS 12.0.0" ./mac-installer-files/sign-and-notarize.sh dmg "Install SeaDAS.app" seadas_12.0.0_mac_aarch64_installer.dmg
   ```

   - macOS asks for the **login** keychain password, which is normally your Mac login password. Click **Always Allow**, or it asks again for each of several hundred files.
   - The script signs native code inside jars first, then programs and libraries, then the app bundles. Then it checks that every native file is signed for notarization.
   - Success ends with `signed seadas_12.0.0_mac_aarch64_installer.dmg`. If the check finds unsigned files, the script stops and lists them, so nothing is uploaded. See [Troubleshooting](#troubleshooting).

3. **Notarize and staple** (upload, then usually 5–15 minutes at Apple, sometimes an hour or more):

   ```bash
   ./mac-installer-files/sign-and-notarize.sh notarize seadas_12.0.0_mac_aarch64_installer.dmg
   ```

   The script prints nothing while it waits. Success shows `accepted` and a stapling message. It also saves Apple's report next to the `.dmg` as `<name>.notary-log.json`.

4. **Check the result:**

   ```bash
   ./mac-installer-files/sign-and-notarize.sh check seadas_12.0.0_mac_aarch64_installer.dmg
   ```

   Look for the NASA authority, `accepted` from Gatekeeper (`source=Notarized Developer ID`), and "The validate action worked!".

`sign-and-notarize.sh all` runs steps 2 to 4 in one go. Separate steps are safer: if the upload fails, you rerun only step 3, without signing again. Don't rebuild the `.dmg` after step 3: the notarization applies only to that exact file.

## Test the .dmg like a user

Gatekeeper only checks files that were downloaded, so mark the `.dmg` as downloaded before testing. Alternatively, upload it to Google Drive and download it in Safari.

```bash
xattr -w com.apple.quarantine "0081;$(printf %x $(date +%s));Safari;" seadas_12.0.0_mac_aarch64_installer.dmg
```

- [ ] Double-click the `.dmg`, then **Install SeaDAS**. At most, macOS asks "downloaded from the internet, open it?". It never says "Apple could not verify…".
- [ ] Choose **Only for me**. SeaDAS.app goes to `~/Applications`, with no password asked.
- [ ] Run it again and choose **For all users**. SeaDAS.app goes to `/Applications`. A password is asked only if your account can't write there.
- [ ] With an old folder install in `~/SeaDAS`, it offers to remove the program files and keeps `ocssw` and other user files.
- [ ] SeaDAS starts from Applications or Launchpad with no warning, and shows its own Dock icon.

If something fails, collect `~/Library/Logs/SeaDAS/install.log` and `~/Library/Logs/SeaDAS/installer.log`.

## Troubleshooting

Most of these came up during the first SeaDAS 12.0.0 notarization; the rest are messages the script can print.

| What you see | Cause | Fix |
| --- | --- | --- |
| `1 identity imported` but `0 valid identities found` | Apple's intermediate certificate is missing | Import `DeveloperIDG2CA.cer` (setup step 4) |
| `found 2 "Developer ID Application" certificates` | More than one certificate in the keychain | Set `SIGN_ID="Developer ID Application: NASA (82A95CK2HC)"` before the command |
| Keychain password asked again and again | "Allow" was clicked instead of "Always Allow" | Enter the password once more and click Always Allow |
| Setup asks for "Path to App Store Connect API private key" | Its first question is for API keys | Press Enter, or use the `store-credentials` command with `--apple-id` (setup step 8) |
| Credentials not valid, or not allowed to notarize | Not on team 82A95CK2HC, role too low, or a wrong app-specific password | Check the team invitation and role; make a new app-specific password |
| `The Internet connection appears to be offline` while waiting | The network or VPN dropped after the upload | Apple keeps working. Run `xcrun notarytool history --keychain-profile seadas-notary` for the id, `xcrun notarytool wait <id> --keychain-profile seadas-notary`, then `xcrun stapler staple <dmg>` and `check` |
| `status: Invalid` | Apple found unsigned or wrongly signed files | Read `<name>.notary-log.json`; each issue names the file. Usually native code inside a jar that the script missed. Fix the script, then sign and upload again |
| The check step stops with "not signed for notarization" | Same as Invalid, caught before the upload | The list names each file; fix the script before going further |
| `can't list the files in <jar>` | `unzip` failed on that jar | Check that the jar is intact; rebuild the inputs |
| `has its own jar signature and N unsigned native files` | A third-party jar is jar-signed and holds unsigned Mac code | It can't be repacked without breaking its signature. Get a version without the jar signature, or one whose native files are already Developer ID signed |
| Stapling fails with `Record not found` (error 65) | The `.dmg` was rebuilt after the accepted upload, so Apple has no ticket for this file | Compare `shasum -a 256 <dmg>` with the `sha256` in `xcrun notarytool log <id> --keychain-profile seadas-notary`; if they differ, run the `notarize` step again |
| Gatekeeper still warns on the user's Mac | Ticket not stapled, or the `.dmg` was changed after notarizing | Run `check`; staple again, or rebuild |

`sign-and-notarize.sh verify <.dmg|.app|folder>` lists the native files Apple would reject, without signing or uploading anything. Use it to check a `.dmg` you are unsure about.

## Reference

**Accounts and IDs**

| Item | Value |
| --- | --- |
| Apple developer team | NASA, team ID `82A95CK2HC` |
| Signing certificate | `Developer ID Application: NASA (82A95CK2HC)`, expires 2029-12-07 |
| Intermediate certificate | Developer ID - G2 (`DeveloperIDG2CA.cer`) |
| Notarization login (keychain profile) | `seadas-notary` |
| Installer app bundle ID | `gov.nasa.gsfc.seadas.installer` |

**Script commands** (`sign-and-notarize.sh`, macOS only)

| Command | What it does |
| --- | --- |
| `setup` | Saves the notarization login in the keychain |
| `dmg <src> <out.dmg>` | Copies `<src>`, signs everything in it, checks it, builds and signs `<out.dmg>` |
| `notarize <file>` | Uploads a `.dmg`, `.pkg` or `.zip`, waits, and staples the ticket |
| `check <file>` | Shows the signature, the Gatekeeper verdict and the ticket |
| `verify <.dmg/.app/dir>` | Lists native files, including those inside jars, that Apple would reject |
| `all <src> <out.dmg>` | `dmg`, then `notarize`, then `check` |
| `sign <dir/.app/.jar>` | Signs native code in place |

**Settings** (environment variables): `SIGN_ID` picks the certificate when there are several; `NOTARY_PROFILE` overrides `seadas-notary`; `VOLNAME` sets the `.dmg` volume name, such as `SeaDAS 12.0.0`.

**Files** (in `seadas-installer/`)

| File | Purpose |
| --- | --- |
| `mac-installer-files/sign-and-notarize.sh` | Signs, checks, makes the `.dmg`, notarizes |
| `mac-installer-files/build-mac-app.sh` | Builds SeaDAS.app from the IzPack packs |
| `mac-installer-files/build-mac-installer.sh` | Builds Install SeaDAS.app around SeaDAS.app |
| `mac-installer-files/app/`, `mac-installer-files/installer/` | Launcher source and Info.plist templates |
| `izpack-installer/src/main/izpack/install-for-mac-app.xml` | Installer pages (`build-all.sh mac-app`) |
| `izpack-installer/src/main/izpack/packs/files/macosx-app/install-seadas-app.sh` | Copies SeaDAS.app into place and cleans up |
