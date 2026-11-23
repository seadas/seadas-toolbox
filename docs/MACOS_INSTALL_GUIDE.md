# Installing SeaDAS on macOS

Last updated: November 23, 2026 (SeaDAS 12.1.0)

SeaDAS for macOS comes as a disk image (.dmg) that installs SeaDAS.app in your Applications folder. The installer is signed by NASA and notarized by Apple, so macOS opens it without security workarounds. SeaDAS includes its own Java, so you don't need to install Java, Xcode or Homebrew first. The processing tools (OCSSW) also need Python 3, which SeaDAS helps you set up when you install them.

## Requirements

| Item | Requirement |
| --- | --- |
| Mac | Apple silicon (M1 or later). Intel Macs aren't supported. |
| macOS | 12 Monterey or later |
| Disk space | About 1 GB for the download and 1.2 GB for SeaDAS. OCSSW (processing tools) needs more, depending on the missions you install. |
| Administrator password | Only to install for all users in /Applications. Installing only for yourself needs none. |
| Python 3 and `requests` | Only for OCSSW. SeaDAS offers to install them when you install OCSSW. |
| NASA Earthdata Login | Only for OCSSW processing and Earthdata downloads. A free account, saved in a `.netrc` file (see [Earthdata Login](#earthdata-login)). |

To check your Mac, choose Apple menu > About This Mac: Chip shows Apple M1, M2 or later.

## Install SeaDAS

1. Download `seadas_<version>_mac_aarch64_installer.dmg` from the SeaDAS download page: [download link].
2. Double-click the .dmg in your Downloads folder. A window opens with **Install SeaDAS**.
3. Double-click **Install SeaDAS**. macOS asks: “‘Install SeaDAS’ is an app downloaded from the Internet. Are you sure you want to open it?” Click **Open**.
4. Read the welcome page and accept the license.
5. Choose where to install SeaDAS:
    - **For all users**: installs in /Applications. If your account can't write there, macOS asks for an administrator name and password.
    - **Only for me**: installs in the Applications folder in your home folder (~/Applications). No password needed. Choose this if you don't have administrator rights.
6. If you have an older SeaDAS in the SeaDAS folder in your home folder, the same page offers to remove its program files. See [Upgrading from an older SeaDAS](#upgrading-from-an-older-seadas).
7. Click **Next** to install. Copying takes a minute or two.
8. When the last page says “SeaDAS is installed”, click **Done**, then eject the disk image (drag it to the Trash, or click the eject button next to it in Finder).

You can delete the .dmg after installing.

## Start SeaDAS

Open **SeaDAS** from Launchpad, or from the Applications folder you installed it in. To keep it in the Dock, right-click its Dock icon while it runs and choose Options > Keep in Dock.

If you installed only for yourself, SeaDAS is in ~/Applications, not /Applications. Launchpad and Spotlight find it in both places.

## Upgrading from an older SeaDAS

Install the new version the same way; you don't need to uninstall first. Quit SeaDAS before you start the installer.

- **An older SeaDAS.app** in the same Applications folder is replaced.
- **An older install in the SeaDAS folder in your home folder** (~/SeaDAS, used by SeaDAS 12.0.0 and earlier): if you tick the option to remove it, the installer removes its program files only. Your OCSSW folder (~/SeaDAS/ocssw) and any of your own files there are kept, and the new SeaDAS uses the same OCSSW.
- **Your settings and preferences** in ~/.seadas are kept. The installer clears only its module cache (~/.seadas/var/cache), so the new version starts cleanly.

## Install the processing tools (OCSSW)

SeaDAS opens and displays data without OCSSW. To process data (for example with l2gen), install OCSSW from within SeaDAS: choose **SeaDAS-Toolbox > Install/Update SeaDAS Processors**, pick the missions you need, and click **Run**.

OCSSW needs Python 3 with the `requests` library. A new Mac doesn't have them, so the first time you choose Install/Update SeaDAS Processors, SeaDAS checks and offers to install what's missing:

1. **Apple's Command Line Tools**, which provide Python 3. Click **Install** in SeaDAS, then click **Install** in Apple's window that opens. The download takes several minutes; SeaDAS waits and continues when it's done.
2. **The `requests` library.** Click **Install**. SeaDAS installs it for your account only, so no administrator password is needed.

Then the OCSSW installer opens as usual. If you already have Python 3 with `requests` (for example from Homebrew or conda), SeaDAS skips these steps.

- The default location is ~/SeaDAS/ocssw. If you used OCSSW with an older SeaDAS, the new SeaDAS finds it there.
- OCSSW needs internet access to download its files, and again when processing to fetch ancillary data. These downloads use your Earthdata Login: set it up before you process data (see [Earthdata Login](#earthdata-login)).
- To install the Python parts yourself instead, run `xcode-select --install` in Terminal, wait for it to finish, then run `python3 -m pip install --user requests`.

## Earthdata Login

The processors download ancillary data (such as the attitude and ephemeris files modis_GEO needs, and the weather and ozone data l2gen needs) with your NASA Earthdata Login. They read it from a file named `.netrc` in your home folder. Logging in to a NASA website in Safari doesn't set this up.

1. Create a free account at [urs.earthdata.nasa.gov](https://urs.earthdata.nasa.gov) if you don't have one.
2. Open Terminal (in Applications > Utilities) and run:

   ```
   touch ~/.netrc && chmod 600 ~/.netrc && open -e ~/.netrc
   ```

   This creates the file, makes it readable only by you, and opens it in TextEdit.
3. Type one line, with your own user name and password, then save and close the file:

   ```
   machine urs.earthdata.nasa.gov login YOUR_USERNAME password YOUR_PASSWORD
   ```

When you change your Earthdata password, change it in this file too.

## Uninstall SeaDAS

Quit SeaDAS, then drag **SeaDAS.app** from /Applications or ~/Applications to the Trash. Removing it from /Applications may ask for an administrator password.

Your data is kept, so a later reinstall picks it up. To remove it too, delete these folders:

| Folder | Holds |
| --- | --- |
| ~/SeaDAS/ocssw | OCSSW processing tools and their data |
| ~/.seadas | SeaDAS settings, preferences and cache |
| ~/Library/Logs/SeaDAS | Installer logs |

Folders starting with a dot are hidden in Finder: press Command-Shift-. (period) to show them.

## Troubleshooting

| Problem | What to do |
| --- | --- |
| “Apple could not verify … is free of malware” | This installer shouldn't show it. Check that you downloaded the .dmg from the SeaDAS download page, not an older copy, and report it to us. |
| “No administrator password was given, so SeaDAS wasn't installed” | The password prompt was cancelled. Run Install SeaDAS again and enter an administrator password, or choose **Only for me**. |
| “SeaDAS is running from … Quit SeaDAS and run the installer again” | Quit SeaDAS (SeaDAS menu > Quit SeaDAS), then run Install SeaDAS again. |
| macOS asks to allow Install SeaDAS to update or delete other apps (App Management) | Click **Allow**, or allow it in System Settings > Privacy & Security > App Management. It's needed to replace an older SeaDAS.app. [To be confirmed in testing.] |
| SeaDAS doesn't appear in Launchpad | Open it from ~/Applications or /Applications in Finder. |
| A processor fails with “Authentication Failure” or “Failed to identify/retrieve attitude/ephemeris” | Your Earthdata Login is missing or out of date. Check ~/.netrc (see [Earthdata Login](#earthdata-login)). |

When you ask for help, include these logs:

- Installer: ~/Library/Logs/SeaDAS/install.log and ~/Library/Logs/SeaDAS/installer.log
- SeaDAS: ~/.seadas/var/log/messages.log

Ask on the [Earthdata Forum](https://forum.earthdata.nasa.gov/).
