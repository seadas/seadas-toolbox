# seadas-installer

Builds the SeaDAS application installers with IzPack: the SNAP platform, the
optical toolbox and the SeaDAS toolbox cluster, for macOS (Apple Silicon),
Linux (x64) and Windows (x64). Each platform comes in two versions:

| Installer | Java |
| --- | --- |
| `seadas-installer-<platform>` | bundles a Java 21 JRE |
| `seadas-installer-<platform>-nojre` | uses a JDK 21+ already on the machine; about 50 MB smaller |

This directory is not part of the root Maven reactor; build it on its own, as
below.

## How to package

### 1. Build the full stack first

The installer copies build output from the sibling checkouts, so all four
repositories must sit side by side, on matching branches:

```
<seadas>/
├── snap-engine/
├── snap-desktop/
├── optical-toolbox/
└── seadas-toolbox/        (this repository)
```

Build and install them in this order:

```bash
cd <seadas>/snap-engine      && mvn install -Dmaven.test.skip=true
cd <seadas>/snap-desktop     && mvn install -Dmaven.test.skip=true
cd <seadas>/optical-toolbox  && mvn install -Dmaven.test.skip=true
cd <seadas>/seadas-toolbox   && mvn install -Dmaven.test.skip=true
```

The installer build picks up:

- `snap-desktop/snap-application/target/snap/`
- `optical-toolbox/opttbx-kit/target/netbeans_clusters/opttbx`
- `seadas-toolbox/seadas-kit/target/netbeans_clusters/seadas`

The bundled JREs are checked in under
`izpack-installer/src/main/izpack/packs/jre/`.

### 2. Build the installers

```bash
cd <seadas>/seadas-toolbox/seadas-installer/izpack-installer

./build-all.sh                    # all six installers
./build-all.sh linux win          # only the named platforms
./build-all.sh linux-nojre        # one installer without a bundled JRE
OUTDIR=/tmp/installers ./build-all.sh
```

Platforms are `mac`, `linux` and `win`, and `mac-nojre`, `linux-nojre` and
`win-nojre`. Each takes roughly 2–3 minutes and ~900 MB.

The installers are written to `izpack-installer/dist/` (or `OUTDIR`):

```
seadas-installer-linux-x64.jar            seadas-installer-linux-x64-nojre.jar
seadas-installer-macos-aarch64.jar        seadas-installer-macos-aarch64-nojre.jar
seadas-installer-windows-x64.jar / .exe   seadas-installer-windows-x64-nojre.jar / .exe
seadas_<version>_windows64_installer.exe  seadas_<version>_windows64_nojre_installer.exe
                                          (the Windows installers for users; see below)
seadas_<version>_linux64_installer.sh     seadas_<version>_linux64_installer_no_bundled_jre.sh
                                          (the Linux installers for users; see below)
```

A rebuild overwrites the previous installer for that platform. A failed build
leaves the last good one in place, so there is no need to clear `dist/`.

To build a single installer with Maven directly, pick the platform profile,
and add `nojre` for the version without a JRE:

```bash
mvn clean package -P linux             # -> target/seadas-installer-linux-x64.jar
mvn clean package -P linux,nojre       # -> target/seadas-installer-linux-x64-nojre.jar
```

A direct Maven build leaves the installer in `izpack-installer/target/`, **not**
in `dist/`; only `build-all.sh` moves installers into `dist/`. The next
`mvn clean` deletes `target/`, so copy the installer out if you want to keep it.

### 3. The Windows installers for users

`win` also produces `seadas_<version>_windows64_installer.exe`, and
`win-nojre` produces `seadas_<version>_windows64_nojre_installer.exe`. Both
are [Inno Setup](https://jrsoftware.org/isinfo.php) installers. These are the
Windows installers to publish, and they need no Java to start. They are built
entirely on Linux, with no manual Windows steps:

1. `mvn package -P win` lays out the installed SeaDAS folder in
   `target/windows/SeaDAS/`, with the same files the IzPack installer would
   install, and the bundled JRE unpacked (not for `nojre`). It copies the Inno Setup script and
   the files it uses from `seadas-installer/windows-installer-files/` next to
   it, and writes `target/windows/build.iss` with the SeaDAS version (from the
   root `pom.xml`), the JRE folder name, and `NoJre` for `nojre`.
2. `build-all.sh` compiles `target/windows/seadas-windows.iss` with Inno
   Setup's ISCC, running under Wine in Docker (image `amake/innosetup`, pinned
   by digest; override it with `INNO_IMAGE`).

So `win` and `win-nojre` need **Docker**, and the user running `build-all.sh`
must be able to run `docker`. The ISCC step adds about 2.5 minutes. A direct
`mvn package -P win` (or `-P win,nojre`) does step 1 only.

The no-JRE Inno installer adds a page that asks where Java is installed. Any
Java 21 or newer works, a JDK or a JRE, unlike the IzPack `-nojre` installers,
which need a JDK. The page is preset with the first Java 21+ it finds in
`JAVA_HOME` or the registry entries of the Oracle and Eclipse Adoptium
installers. For a silent install (`/VERYSILENT`), pass `/JAVAHOME=<folder>`
if none of those has one.

The Java location, and the install folder, are only known at install time, so
the Inno installer fills in `jdkhome` in `etc\seadas.conf` and
`etc\snap.conf` itself. Change the
installer's wizard text, license, icons or shortcuts in
`windows-installer-files/`. Keep the script's `AppId` unchanged, so a new
version installs over the old one. The installer is not code-signed yet (the
`SignTool` line is commented out). Test it on a Windows machine before
publishing, since it is built under Wine.

### 4. The Linux installers for users

`linux` also produces `seadas_<version>_linux64_installer.sh`, and
`linux-nojre` produces `seadas_<version>_linux64_installer_no_bundled_jre.sh`.
These are the Linux installers to publish: self-extracting archives made with
[makeself](https://makeself.io/), so `build-all.sh` needs `makeself` on the
PATH for both (`apt install makeself`). Without it, a run with no arguments
skips them with a warning, and asking for `linux` or `linux-nojre` fails.

Each archive holds the IzPack jar and
`seadas-installer/linux-installer-files/start-installer.sh`, which makeself
runs after unpacking it to `$TMPDIR` (about 1 GB):

* The bundled-JRE archive also holds the Java 21 JRE from `packs/jre/`, and
  the installer runs on it.
* The no-JRE archive holds no Java. The installer runs on the machine's Java:
  `JAVA_HOME`, else `java` on the PATH. With no Java, or one older than 21, it
  stops with a message saying what it found. The installer then offers that
  Java's folder for SeaDAS to use, and asks for another if it is not a JDK.

Arguments after `--` go to the installer, for example on a machine without a
display:

```bash
sh seadas_<version>_linux64_installer.sh -- -console
```

`sh <file> --check` verifies an archive, and `--list` shows what is in it.
The makeself step adds about 20 seconds. These replace the old hand-made
`.sh` installers, which started the installer with a Java 8 JRE.

A platform profile is required; there is deliberately no default. The
no-JRE descriptors are generated at build time from `install-for-<os>.xml`, so
make descriptor changes there only. See `docs/DEVELOPERS_MANUAL.md` §14.2 at
the repository root for how that works.

## How to Run

The installers for users start without any Java on the machine, except the
no-JRE ones, which need Java 21 or newer:

```bash
sh seadas_<version>_linux64_installer.sh                  # Linux
sh seadas_<version>_linux64_installer_no_bundled_jre.sh   # Linux, Java 21+ via JAVA_HOME or PATH
```

On Windows, run `seadas_<version>_windows64_installer.exe` (see above).

The IzPack jars themselves need **Java 21 or newer** to start:

```bash
java -jar seadas-installer-linux-x64.jar          # Linux
java -jar seadas-installer-macos-aarch64.jar      # macOS
```

On Windows, double-click `seadas-installer-windows-x64.exe`, or run the `.jar`
with `java -jar`.

The installer offers `SeaDAS` in the user's home folder as the install
location on every platform. That default comes from the `INSTALL_PATH`
variable in `install-for-<os>.xml`.

The `-nojre` installers need a full **JDK 21 or newer**, not just a JRE: the
installer only accepts a folder containing `bin/javac`. If the JDK running the
installer qualifies, it is used without asking. Otherwise the installer asks
for the JDK location, for example:

- `/usr/lib/jvm/jdk-21` (Linux)
- `/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home` (macOS)
- `C:\Program Files\Eclipse Adoptium\jdk-21` (Windows)
