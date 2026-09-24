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
<snap>/
├── snap-engine/
├── snap-desktop/
├── optical-toolbox/
└── seadas-toolbox/        (this repository)
```

Build and install them in this order:

```bash
cd <snap>/snap-engine      && mvn install -Dmaven.test.skip=true
cd <snap>/snap-desktop     && mvn install -Dmaven.test.skip=true
cd <snap>/optical-toolbox  && mvn install -Dmaven.test.skip=true
cd <snap>/seadas-toolbox   && mvn install -Dmaven.test.skip=true
```

The installer build picks up:

- `snap-desktop/snap-application/target/snap/`
- `optical-toolbox/opttbx-kit/target/netbeans_clusters/opttbx`
- `seadas-toolbox/seadas-kit/target/netbeans_clusters/seadas`

The bundled JREs are checked in under
`izpack-installer/src/main/izpack/packs/jre/`.

### 2. Build the installers

```bash
cd <snap>/seadas-toolbox/seadas-installer/izpack-installer

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
```

A rebuild overwrites the previous installer for that platform. A failed build
leaves the last good one in place, so there is no need to clear `dist/`.

To build a single installer with Maven directly, pick the platform profile,
and add `nojre` for the version without a JRE:

```bash
mvn clean package -P linux             # -> target/seadas-installer-linux-x64.jar
mvn clean package -P linux,nojre       # -> target/seadas-installer-linux-x64-nojre.jar
```

A platform profile is required; there is deliberately no default. The
no-JRE descriptors are generated at build time from `install-for-<os>.xml`, so
make descriptor changes there only. See `docs/DEVELOPERS_MANUAL.md` §14.2 at
the repository root for how that works.

## How to Run

Every installer needs **Java 21 or newer** on the machine to start.

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
