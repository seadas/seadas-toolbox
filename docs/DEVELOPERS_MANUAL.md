# SeaDAS Toolbox — Developer's Manual

*Applies to the `SEADAS-12.0.0-SNAP-14.0.0` branch (SeaDAS Toolbox 12.0.0 on SNAP 14.0.0).*

---

## Table of contents

1. [What this software is](#1-what-this-software-is)
2. [Repository layout](#2-repository-layout)
3. [Toolchain and prerequisites](#3-toolchain-and-prerequisites)
4. [Building](#4-building)
5. [Running and debugging in the IDE](#5-running-and-debugging-in-the-ide)
6. [Versioning and branching](#6-versioning-and-branching)
7. [Module reference](#7-module-reference)
8. [Architecture: `seadas-processing`](#8-architecture-seadas-processing)
9. [Cookbook: common development tasks](#9-cookbook-common-development-tasks)
10. [The other modules](#10-the-other-modules)
11. [Help and documentation](#11-help-and-documentation)
12. [Preferences reference](#12-preferences-reference)
13. [Testing](#13-testing)
14. [Packaging, distribution and release](#14-packaging-distribution-and-release)
15. [Developer scripts (`bin/`)](#15-developer-scripts-bin)
16. [Conventions, gotchas and known rough edges](#16-conventions-gotchas-and-known-rough-edges)

---

## 1. What this software is

The SeaDAS Toolbox is **not a standalone application**. It is a set of NetBeans
plugin modules (NBMs) that extend ESA's SNAP desktop application with NASA OBPG
(Ocean Biology Processing Group) capability. The build produces a NetBeans
*cluster* named `seadas`, which the SNAP application loads at startup alongside
its own clusters.

Three separate things get confused frequently, so it is worth naming them:

| Name | What it is |
| --- | --- |
| **SNAP** | ESA's Java desktop platform (`snap-engine` + `snap-desktop`), built on the NetBeans Platform. Provides the product model, GPF, the image views, the Options dialog, JavaHelp integration. |
| **SeaDAS Application Platform** | A NASA-branded build of SNAP. Lives in the `snap-engine` / `snap-desktop` forks on `SEADAS-*` branches — branding, colour schemes, default preferences. |
| **SeaDAS Toolbox** | *This repository.* The NASA-specific plugin modules that add OCSSW processing, Earthdata Cloud access, masks and overlays on top of that platform. |

The single most important external dependency is **OCSSW** — the OBPG's
command-line science processing software (`l2gen`, `l2bin`, `l3mapgen`, the MODIS
and VIIRS geolocation/calibration chains, and so on). OCSSW is written in C and
Python and is installed separately by the user. The toolbox is, to a first
approximation, a Swing front-end and job manager for OCSSW.

Language and licence: Java 17 (`maven.compiler.release`), Maven multi-module,
GPL-3.

---

## 2. Repository layout

```
seadas-toolbox/
├── pom.xml                          root reactor + all shared plugin config
├── seadas-processing/               OCSSW GUI front-end — the largest module
├── seadas-earthdata-cloud-toolbox/  CMR / Harmony / OB.DAAC data access
├── seadas-watermask-operator/       GPF operator: land / water / coastline masks
├── seadas-bathymetry-operator/      GPF operator: bathymetry + elevation bands
├── seadas-contour-operator/         contour vector overlays
├── seadas-image-animator/           band / image sequence animation
├── seadas-metadata-tools/           metadata dump TopComponent
├── seadas-kit/                      packaging: aggregates NBMs into the cluster
│
├── keystore/                        NBM signing keystore (shared, checked in)
├── bin/                             developer utility scripts (Python, shell)
├── docs/                            developer notes, release notes, this manual
├── src/                             assembly descriptor only (seadas-zip.xml)
│
├── seadas-installer/                IzPack installer (NOT in the reactor)
├── seadas-ocsswrest/                remote-OCSSW REST server (NOT in the reactor)
├── seadas-writeimage-operator/      parked (NOT in the reactor)
├── obdaac/  CMR/                    parked data/scratch (NOT in the reactor)
│
├── CHANGELOG.txt  README.md  LICENSE.html  VERSION.txt
```

**Reactor membership matters.** Only the eight modules listed in the root
`pom.xml` `<modules>` block are built by `mvn install` at the top level. Anything
else in the tree — including `seadas-ocsswrest` and `seadas-installer` — must be
built explicitly from its own directory. Treat the parked directories as
reference material unless you are deliberately working on them.

---

## 3. Toolchain and prerequisites

| Requirement | Version | Notes |
| --- | --- | --- |
| JDK | 21 recommended | `jvm.version=21` for `source`/`target`, but `maven.compiler.release=17` is what actually governs the bytecode. Build with a JDK ≥ 17; the SeaDAS distribution ships a JRE 21. |
| Maven | 3.8+ | `MAVEN_HOME` on `PATH`. |
| Git | any | |
| Python 3 | for `bin/` scripts and some Earthdata helpers | Only needed for doc regeneration and a few cloud features. |
| OCSSW | matching tag | Needed to *run* anything in `seadas-processing`; not needed to compile. |

Artifacts are resolved from four repositories declared in the root POM: the SNAP
Nexus (`nexus.snap-ci.ovh`), the NASA SeaDAS repository
(`seadas.gsfc.nasa.gov/mvn-seadas`), Maven Central, and Unidata (for netCDF).

---

## 4. Building

### 4.1 The full stack from source

The toolbox compiles against SNAP artifacts that are normally built from sibling
checkouts on matching `SEADAS-*` branches. The conventional layout is:

```
${seadas}/
├── snap-engine/
├── snap-desktop/
├── optical-toolbox/
└── seadas-toolbox/
```

Build strictly in dependency order — engine, then desktop, then optical-toolbox,
then this repository:

```bash
cd ${seadas}/snap-engine       && git checkout <SEADAS-branch> && mvn install -Dmaven.test.skip=true
cd ${seadas}/snap-desktop      && git checkout <SEADAS-branch> && mvn install -Dmaven.test.skip=true
cd ${seadas}/optical-toolbox   && git checkout <SEADAS-branch> && mvn install -Dmaven.test.skip=true
cd ${seadas}/seadas-toolbox    && git checkout SEADAS-12.0.0-SNAP-14.0.0 && mvn install -Dmaven.test.skip=true
```

If you only need to work on the toolbox and the matching SNAP artifacts are
already published, the first three steps can be skipped — Maven will resolve
`org.esa.snap:*:14.0.0` from the SNAP Nexus.

### 4.2 Everyday commands

```bash
mvn install -Dmaven.test.skip=true              # normal full build
mvn install                                     # with tests
mvn install -pl seadas-processing -am           # one module plus its dependencies
mvn install -pl seadas-processing               # one module alone (deps already installed)
mvn test -pl seadas-processing                  # tests for one module
mvn test -pl seadas-processing -Dtest=ProcessorModelTest
mvn test -pl seadas-processing -Dtest=ProcessorModelTest#someTest
mvn install -Dmaven.test.skip=true -o           # offline, once everything is cached
```

Because `seadas-kit` collects the other modules' NBM output with an Ant `copy`
task at `package` time, **you must build `seadas-kit` after any module whose
changes you want to see in the cluster.** `mvn install -pl seadas-processing`
alone updates the module's own `target/`, but the cluster under
`seadas-kit/target/netbeans_clusters/seadas` will still hold the previous jar.
Either run a full `mvn install` or add `-pl seadas-processing,seadas-kit`.

### 4.3 What the build produces

Each module is packaged as `nbm` and, via the shared `nbm-maven-plugin`
configuration in the root POM, lands in cluster `seadas` signed with
`keystore/seadas.ks` (alias `snap`, password `snap-123` — this is a
distribution-signing convenience key, not a secret). `seadas-kit` additionally
runs the `cluster` and `autoupdate` goals, producing:

```
seadas-kit/target/netbeans_clusters/seadas/
├── modules/                 the module jars
├── modules/docs/            JavaHelp doc jars
├── modules/ext/             third-party jars
├── config/Modules/          per-module NetBeans config XML
└── update_tracking/
```

plus an auto-update catalogue for the update centre.

### 4.4 Root POM: what is inherited

Everything shared lives in the root `pom.xml`. Worth knowing:

- **`dependencyManagement`** pins every SNAP/Ceres artifact to `${snap.version}`,
  NetBeans APIs to `${netbeans.version}` (`RELEASE210`), Jersey to
  `${jersey.version}` (2.27), netCDF, JUnit 4.13.2, Mockito, jimfs. Child modules
  declare dependencies **without versions**.
- **`maven-compiler-plugin`** sets `release` 17 and adds `--add-opens` /
  `--add-exports` flags that NetBeans reflection needs.
- **`maven-resources-plugin`** filters `src/main/nbm/manifest.mf` into
  `target/nbm/manifest.mf` during `process-sources`; the NBM plugin then reads it
  via `sourceManifestFile`. This is how `${seadas.nbmSpecVersion}` gets
  substituted into the module manifest.
- **`build-helper-maven-plugin`** derives `seadas.nbmSpecVersion` from
  `${seadas-toolbox.version}` by stripping `-SNAPSHOT`, so NetBeans
  specification versions stay clean and comparable even in snapshot builds.
- **`nbm-maven-plugin`** (in `pluginManagement`): cluster and branding token
  `seadas`, GPL-3, `requiresRestart=true`, `useOSGiDependencies=false`.

---

## 5. Running and debugging in the IDE

The toolbox cannot be launched on its own. You launch SNAP's NetBeans bootstrap
and point it at your freshly built cluster and class trees.

**Run configuration (IntelliJ IDEA):**

- **Main class:** `org.esa.snap.nbexec.Launcher`
- **Use classpath of module:** `snap-main`
- **Working directory:** `${seadas}/snap-desktop/snap-application/target/snap/`
- **Program arguments:**
  ```
  --userdir  "${seadas}/seadas-toolbox/target/userdir"
  --clusters "${seadas}/seadas-toolbox/seadas-kit/target/netbeans_clusters/seadas:${seadas}/optical-toolbox/opttbx-kit/target/netbeans_clusters/opttbx"
  --patches  "${seadas}/snap-engine/$/target/classes:${seadas}/seadas-toolbox/$/target/classes:${seadas}/optical-toolbox/$/target/classes"
  ```
- **VM parameters (optional):**
  `-Dsun.awt.nopixfmt=true -Dsun.java2d.noddraw=true -Dsun.java2d.dpiaware=false`

The `--patches` argument is what makes iteration bearable: the literal `$`
is a wildcard over module directories, and the `target/classes` trees take
precedence over the jars inside the cluster. **Recompiling a module (without
repackaging the NBM) is enough for the next launch to pick up your change.** The
cluster itself only needs rebuilding when you touch resources that are packaged
differently — layer files, help sets, manifests.

Import the project by pointing IntelliJ at the `${seadas}` parent directory and
importing as Maven with "search for projects recursively"; do **not** enable
"create module groups for multi-module Maven projects".

---

## 6. Versioning and branching

Two independent version numbers are tracked, and they are **kept in sync by
hand**:

- `${seadas-toolbox.version}` and its per-module twins — the SeaDAS Toolbox
  release, currently `12.0.0`.
- `${snap.version}` — the SNAP platform being built against, currently `14.0.0`.

Branches are named `SEADAS-<toolbox version>-SNAP-<snap version>`, e.g. the
current `SEADAS-12.0.0-SNAP-14.0.0`. Topic branches append a suffix
(`…-SNAP-13.0.0-FixZoomInIssue`) or use a bare descriptive name
(`image-preview-fallback`, `subset-progress-ux`).

The root POM carries a version property per submodule — `seadas-processing.version`,
`seadas-contour-operator.version`, and so on — each with a commented-out
`-SNAPSHOT` twin sitting right next to it:

```xml
<seadas-processing.version>12.0.0</seadas-processing.version>   <!-- <seadas-processing.version>12.0.0-SNAPSHOT</seadas-processing.version>-->
```

Release cuts flip these by hand, together with the `<version>` elements in the
root POM and in each module POM (the child POMs repeat both their own version
and the parent's). There is no `versions:set` automation in place; if you change
one, grep for the old string and change all of them.

Tags are annotated and follow `SEADAS-<version>[-qualifier][-date]`, e.g.
`SEADAS-11.0.0`, `SEADAS-11.0.0-RC5`, `SEADAS-12.0.0-SNAP-13.0.0-2026-04-29`,
`SEADAS-10.0.0-no-JRE`. `docs/GitNotes` records the convention of stamping a tag
with an explicit date:

```bash
GIT_COMMITTER_DATE="2026-04-29 12:00" git tag -a SEADAS-12.0.0 \
  -m "SeaDAS Toolbox 12.0.0, released in the SeaDAS 12.0.0 platform (SNAP 14.0.0)"
```

Tags are cut in parallel across `snap-engine`, `snap-desktop`, `optical-toolbox`
and `seadas-toolbox` so a release is reproducible from four checkouts.

---

## 7. Module reference

| Module | NetBeans code base | Role |
| --- | --- | --- |
| `seadas-processing` | `gov.nasa.gsfc.seadas.processing` | GUI front-end to the OCSSW command-line processors. ~195 classes; by far the largest and most intricate module. |
| `seadas-earthdata-cloud-toolbox` | `gov.nasa.gsfc.seadas.earthdata.cloud.toolbox` | NASA Earthdata / CMR search, Harmony subsetting, OB.DAAC browser and download. |
| `seadas-watermask-operator` | `gov.nasa.gsfc.seadas.watermask` | GPF operator producing land/water/coastline masks from SRTM and MODIS auxdata. |
| `seadas-bathymetry-operator` | `gov.nasa.gsfc.seadas.bathymetry` | GPF operator producing bathymetry / elevation / topography bands and depth masks. |
| `seadas-contour-operator` | `gov.nasa.gsfc.seadas.contour` | JAI-based contour generation rendered as a vector overlay. |
| `seadas-image-animator` | `gov.nasa.gsfc.seadas.imageanimator` | Animates a band or image sequence in the SNAP image view; GIF export. |
| `seadas-metadata-tools` | `gov.nasa.gsfc.seadas.metadata` | Metadata dump TopComponent, installed via a NetBeans `Installer`/`StartupHook`. |
| `seadas-kit` | `gov.nasa.gsfc.seadas` | Packaging only. Aggregates every module's NBM into the `seadas` cluster, owns the top-level menu folder structure and the About box. |

`seadas-kit` contains almost no logic — three classes for the About box — but it
is load-bearing twice over: its `pom.xml` dependency list determines what ships,
and its `layer.xml` determines the menu skeleton that every module's actions hang
off. **A module missing from either list builds fine and then silently fails to
appear in the application.**

---

## 8. Architecture: `seadas-processing`

This is the part that cannot be understood from one file. Read this section
before touching it.

```
gov.nasa.gsfc.seadas.processing
├── ocssw/          the OCSSW abstraction: where/how programs actually run
├── core/           the model: parameters, processor state, command building
├── common/         generic Swing UI, actions base class, file selectors, utils
├── ui/             one thin *Action class per OCSSW program (menu entry points)
├── l2gen/          the hand-written l2gen subsystem (productData + userInterface)
├── processor/      the multilevel processor (MLP) chain runner
├── preferences/    Tools → Options → SeaDAS panels
├── help/           actions that open web pages and video tutorials
├── docs/           JavaHelp registration + HTML help sources (resources)
└── utilities/      spreadsheet widget, file compare, misc
```

### 8.1 The OCSSW abstraction

Every path to OCSSW goes through the abstract class
`ocssw/OCSSW`. There are three concrete implementations:

| Implementation | Used when `seadas.ocssw.location` is | How it runs programs |
| --- | --- | --- |
| `OCSSWLocal` | `local` | Spawns a local process via the `ocssw_runner` script. |
| `OCSSWVM` | `virtualMachine`, `docker` | Same protocol as remote, against `localhost`. |
| `OCSSWRemote` | `remoteServer` | Jersey REST client against a `seadas-ocsswrest` server; uploads inputs, polls, downloads outputs. |

The instance is chosen by the factory:

```java
// OCSSW.java
public static OCSSW getOCSSWInstance() {
    String ocsswLocation = OCSSWInfo.getInstance().getOcsswLocation();
    if (OCSSW_LOCATION_LOCAL.equals(ocsswLocation))            return new OCSSWLocal();
    else if (OCSSW_LOCATION_VIRTUAL_MACHINE.equals(...)
          || OCSSW_LOCATION_DOCKER.equals(...))                return new OCSSWVM();
    else if (OCSSW_LOCATION_REMOTE_SERVER.equals(...))         return new OCSSWRemote();
    return new OCSSWLocal();
}
```

> **Rule:** new processor code must go through the `OCSSW` interface. Never
> `ProcessBuilder` or `Runtime.exec` directly, never build a path under the OCSSW
> root by hand. Anything that shells out directly works in local mode and breaks
> silently in remote and VM modes — which is exactly the configuration the
> smallest number of developers test.

The abstract surface worth knowing:

```java
abstract Process     execute(ProcessorModel processorModel);
abstract Process     executeSimple(ProcessorModel processorModel);
abstract InputStream executeAndGetStdout(ProcessorModel processorModel);
abstract Process     execute(ParamList paramList);
abstract Process     execute(String[] commandArray);
abstract String      executeUpdateLuts(ProcessorModel processorModel);

abstract void        getOutputFiles(ProcessorModel processorModel);
abstract boolean     getIntermediateOutputFiles(ProcessorModel processorModel);
abstract void        findFileInfo(String fileName, FileInfoFinder fileInfoFinder);

abstract String      getOfileName(String ifileName);                       // + 3 overloads
abstract boolean     isMissionDirExist(String missionName);
abstract String[]    getMissionSuites(String missionName, String programName);
abstract HashMap<String,String> computePixelsFromLonLat(ProcessorModel pm);
abstract InputStream getProductXMLFile(L2genData.Source source);
abstract ProcessObserver getOCSSWProcessObserver(Process p, String name, ProgressMonitor pm);
abstract void        setCommandArrayPrefix();
abstract void        setCommandArraySuffix();
```

Several of these delegate real work back to OCSSW itself: output filenames come
from running the `get_output_name` program and parsing the `Output Name:` token;
file type and mission come from `obpg_file_type`. This is deliberate — the naming
rules live in OCSSW and are not duplicated in Java.

**`OCSSWInfo`** is the singleton holding the installation state: OCSSW root,
derived `bin`/`share` paths, the `ocssw_runner` script path, session/client id,
server-up flag, log directory, and all the preference key constants. It is
initialised from SNAP preferences and re-read by `updateOCSSWInfo()` when the
user changes the configuration. `detectOcssw()` / `initializeLocalOCSSW()` decide
whether a usable installation is present; if not, the processor menu entries are
disabled until `install_ocssw` has run.

**Command construction.** `ProcessorModel.setCommandArrayPrefix()` builds:

```
<ocsswRoot>/bin/ocssw_runner  --ocsswroot  <ocsswRoot>  <programName>   [ … options … ]
```

so every program is invoked through OCSSW's own environment-setup wrapper rather
than directly.

**Remote mode** talks to `seadas-ocsswrest` over a Jersey `WebTarget` rooted at
`http://<host>:<port>/ocsswws/`. The endpoint vocabulary (from `OCSSWRemote`)
covers job lifecycle (`jobs`, `newJobId`, `processStatus`), file staging
(`uploadClientFile`, `uploadParFile`, `downloadFile`, `fileServices`), execution
(`executeOcsswProgram`, `executeOcsswProgramSimple`,
`executeOcsswProgramAndGetStdout`, `executeParFile`, `executeUpdateLutsProgram`)
and metadata (`getOfileName`, `getFileInfo`, `missionSuites`, `ocsswTags`,
`productXmlFile`). The server side lives in the parked `seadas-ocsswrest`
directory; if you change the protocol you must change both sides.

### 8.2 XML-driven processor UIs

Most OCSSW programs get their entire user interface generated from a declarative
XML file. No bespoke Swing code is written per program.

**Where the descriptors live:**
`seadas-processing/src/main/resources/gov/nasa/gsfc/seadas/processing/core/*.xml`
— one per program (`l2gen.xml`, `l2bin.xml`, `l3bin.xml`, `l3mapgen.xml`,
`extractor.xml`, `modis_L1A.xml`, `geolocate_viirs.xml`, …), validated against
`ParamInfo-1.0.xsd` in the same directory.

**Shape of a descriptor:**

```xml
<paramInfo>
  <programMetaData>
    <hasParFile>true</hasParFile>
    <parFileOptionName>par</parFileOptionName>
    <progressRegex>krow: +(\d+) out of +(\d+)</progressRegex>
    <subPanel0Title> Product &amp; Suite Parameters </subPanel0Title>
    <subPanel1Title> Geospatial Parameters </subPanel1Title>
    <numColumns>12</numColumns>
    <columnWidth>10</columnWidth>
    <primaryOptions>
      <primaryOption>ifile</primaryOption>
      <primaryOption>ofile</primaryOption>
    </primaryOptions>
  </programMetaData>
  <options>
    <option type="string">
      <name>-resolution</name>
      <value>9km</value>
      <default>9km</default>
      <source>default</source>
      <order>10</order>
      <aliases><alias>--resolution</alias></aliases>
      <validValues>
        <validValue><value>1km</value><description>…</description></validValue>
      </validValues>
      <description>output resolution</description>
    </option>
  </options>
</paramInfo>
```

Key metadata elements:

| Element | Effect |
| --- | --- |
| `hasParFile` / `parFileOptionName` | Whether the program accepts a parameter file and under which flag. Drives the "Save/Load parfile" UI (`ParFileUI`, `ParFileManager`). |
| `progressRegex` | Regex applied to the program's stdout to drive the progress bar. Two capture groups: *current* and *total*. |
| `primaryOptions` | Which options are the primary input/output — these get the dedicated `L2genPrimaryIOFilesSelector` at the top of the dialog rather than a generic row. |
| `subPanelNTitle` | Titles of up to five grouping panels; each option's `subPanelIndex` places it. |
| `numColumns`, `columnWidth` | Layout hints for the generated form. |
| `hasProgramDependency` | Declares that this program's parameters depend on another program's descriptor. |

Option `type` (the XML attribute) maps onto `ParamInfo.Type`:
`BOOLEAN, STRING, INT, FLOAT, IFILE, OFILE, HELP, DIR, FLAGS, BUTTON`.
`ParamInfo` also records how the value is rendered on the command line via
`usedAs`: `option` (default, `-name value`), `option_minusminus`, `flag`,
`flag_minusminus`, or `argument` (bare positional).

**The parsing and model chain:**

```
*.xml
  │  ParamUtils.computeParamList(xmlFileName)          ← DOM parse, also merges user preferences
  ▼
ArrayList<ParamInfo>  ──►  ParamList  ──►  ProcessorModel
                                             │  holds current values, fires PropertyChangeEvents,
                                             │  validates completeness, builds the command array
                                             ▼
                                        ParamUIFactory / ProgramUIFactory   (generated Swing form)
```

- **`ParamUtils`** is the XML reader. Besides parsing options it applies user
  preferences (favourites, projections, defaults from the Options panels) on top
  of the XML defaults, and exposes small helpers for the metadata elements
  (`getPrimaryOptions`, `getParFileOptionName`, `getProgressRegex`, …).
- **`ParamInfo`** is one option: name, value, type, default (plus
  `defaultValueOriginal` so an overridden default can be restored), description,
  source, order, `colSpan`, `subPanelIndex`, valid values, aliases, bit-flag flag.
- **`ParamList`** is the ordered collection with par-string serialisation
  (`getParamString` / `setParamString`) — this is what parfile import/export and
  the "show defaults" toggle operate on.
- **`ProcessorModel`** (~3,200 lines) is the centre of gravity. It owns the
  `ParamList`, resolves input/output filenames through OCSSW, tracks
  `isReadyToRun`, exposes a `SwingPropertyChangeSupport` bus keyed by parameter
  name, and assembles `cmdArrayPrefix + options + cmdArraySuffix`.

**Program-specific behaviour goes in a `ProcessorModel` subclass**, selected by
the factory:

```java
ProcessorModel.valueOf(programName, xmlFileName, ocssw)
```

which dispatches on `ProcessorTypeInfo.getProcessorID(programName)` — a
`String → ProcessorID` map — and returns one of the private static subclasses:

| `ProcessorID` | Subclass | Why it needs custom behaviour |
| --- | --- | --- |
| `EXTRACTOR` | `Extractor_Processor` | Pixel/line vs lat/lon extraction geometry; per-mission extractor programs. |
| `MODIS_L1B` | `Modis_L1B_Processor` | Multiple output files, non-standard output naming. |
| `LONLAT2PIXLINE` | `LonLat2Pixels_Processor` | Runs a secondary program to convert coordinates. |
| `L2BIN` | `L2Bin_Processor` | Resolution/suite interdependencies. |
| `L3BIN` | `L3Bin_Processor` | |
| `L3MAPGEN` | `L3MAPGEN_Processor` | Projection, RGB, and geospatial parameter coupling. |
| `MAPGEN` | `MAPGEN_Processor` | |
| `L3BINDUMP` | `L3BinDump_Processor` | Output is a text dump, not a product. |
| `OCSSW_INSTALLER` | `OCSSWInstaller_Processor` | Installs OCSSW itself; post-run reconfiguration. |
| everything else | plain `ProcessorModel` | |

There are also `L2BinAquarius_Processor` and `SMIGEN_Processor` classes present
but currently unreachable — their `ProcessorID` entries are commented out.

### 8.3 Actions, menus and the execution pipeline

Each OCSSW program has one thin action class in `ui/`, all extending
`common/CallCloProgramAction`. They are wired *entirely* through NetBeans
annotations — no registration code:

```java
@ActionID(category = "Processing", id = "gov.nasa.gsfc.seadas.processing.ui.L3MapGenAction")
@ActionRegistration(displayName = "#CTL_ L3MapGenAction_Name", popupText = "#CTL_ L3MapGenAction_Name")
@ActionReference(path = "Menu/SeaDAS-Toolbox/SeaDAS Processors", position = 360)
@NbBundle.Messages({
        "CTL_L3MapGenAction_Name=l3mapgen...",
        "CTL_L3MapGenAction_ProgramName=l3mapgen",
        "CTL_L3MapGenAction_DialogTitle=l3mapgen",
        "CTL_L3MapGenAction_XMLFileName=l3mapgen.xml",
        "CTL_L3MapGenAction_Description=Creates a L3 mapped file from an input L3 bin file"
})
public class L3MapGenAction extends CallCloProgramAction implements ContextAwareAction, LookupListener {
    public L3MapGenAction(Lookup lkp) {
        …
        setProgramName(Bundle.CTL_L3MapGenAction_ProgramName());
        setDialogTitle(Bundle.CTL_L3MapGenAction_DialogTitle());
        setXmlFileName(Bundle.CTL_L3MapGenAction_XMLFileName());
    }
}
```

The `@NbBundle.Messages` block is doing double duty: it supplies display strings
*and* it is the binding between the action, its OCSSW program name and its XML
descriptor, read back through the annotation-processor-generated `Bundle` class.
The `Menu/...` folders those `@ActionReference` paths point into are declared in
`seadas-kit/src/main/resources/gov/nasa/gsfc/seadas/layer.xml`.

**What happens when the menu item is clicked** (`CallCloProgramAction.actionPerformed`):

1. `initializeOcsswClient()` — obtain the `OCSSW` instance; if none, open
   `OCSSWInfoGUI` so the user can configure one.
2. If not local and the server is down, show the "server down" message and stop.
3. `getProgramUI(appContext)` returns a `CloProgramUI`. This is the **one place
   that hard-codes exceptions to the generic factory**:
   - name contains `extract` → `ExtractorUI`
   - `modis_GEO` / `modis_L1B` → `ModisGEO_L1B_UI`
   - `install_ocssw` → `OCSSWInstallerFormLocal` or `…FormRemote`
   - `update_luts` → `UpdateLutsUI`
   - otherwise → `ProgramUIFactory` (the generic XML-driven form)
4. Wrap the panel in a SNAP `ModalDialog` (`OK`/`Apply`/`Cancel`/`Help`), rename
   OK to **Run**, and bind its enabled state to
   `processorModel.isReadyToRun()` through the model's property-change bus.
5. On OK: `executeProgram(processorModel)` runs the job in a
   `ProgressMonitorSwingWorker`:
   - `ocssw.execute(processorModel)` → a `Process`
   - `ocssw.getOCSSWProcessObserver(...)` → a `ProcessObserver` with two reader
     threads (stdout, stderr)
   - handlers attached: a `ProgressHandler` driven by the descriptor's
     `progressRegex`, and a `ConsoleHandler` accumulating the log
   - `processObserver.startAndWait()`
   - exit code 0 → write `OCSSW_LOG_<program>.txt` into the log directory;
     non-zero → throw, surfacing as an error dialog
6. In `done()`: fetch output files (`ocssw.getOutputFiles`), optionally open the
   product in SNAP, show the completion dialog, and run the model's
   *secondary processor* if one was set (this is how programs that chain a second
   OCSSW call are handled).

`update_luts` bypasses this pipeline entirely — it is executed synchronously via
`ocssw.executeUpdateLuts(...)` and its output shown in an information dialog.

The `CloProgramUI` interface every processor panel implements is small:

```java
public interface CloProgramUI {
    JPanel         getParamPanel();
    ProcessorModel getProcessorModel();
    File           getSelectedSourceProduct();
    boolean        isOpenOutputInApp();
    String         getParamString();
    void           setParamString(String paramString);
}
```

`ProgramUIFactory` is its generic implementation: it builds the model via
`ProcessorModel.valueOf`, adds the primary I/O selector
(`L2genPrimaryIOFilesSelector`), the parfile panel (`ParFileUI`) and the
generated parameter form (`ParamUIFactory`, ~1,850 lines — this is where option
types become widgets, sub-panels are laid out, and valid-value lists become
combo boxes).

### 8.4 The l2gen subsystem

`l2gen` is the flagship OCSSW processor and it does **not** use the generic XML
path. It has a hand-written, much richer UI:

```
core/L2genData.java              (~2,000 lines) the model
core/L2genReader.java            parses l2gen's own parameter/product XML
core/L2genProductsParamInfo.java product-selection parameter
core/L2genParamCategoryInfo.java parameter categorisation
l2gen/productData/*              product / algorithm / wavelength model
l2gen/userInterface/*            33 classes of bespoke Swing
```

What makes it different:

- **Wavelength awareness.** Products are offered per-wavelength, with the
  wavelength set derived from the input file's mission. `L2genWavelengthInfo`,
  the wavelength limiter panel, and the visible/IR wave-type distinction all
  exist for this.
- **Categorised parameters.** `paramCategoryInfo.xml` and
  `productCategoryInfo.xml` (under `resources/.../l2gen/userInterface/`) group
  parameters and products into the tabbed/tree UI rather than flat panels.
- **Descriptors come from OCSSW at runtime.** `OCSSW.getProductXMLFile(Source)`
  reads `${ocsswRoot}/share/common/` — so the available products track the
  installed OCSSW tag, not the toolbox build. The bundled XML under `resources/`
  is the fallback (`Source.RESOURCES`).
- **Ancillary file retrieval.** `L2genGetAncillaryFilesSpecifier` /
  `L2genGetAncillarySplitButton` drive OCSSW's `getanc`.
- **Its own parfile import/export**, defaults indicator, and suite combo box.
- Three modes share the machinery: `L2genAction` (l2gen), `L3genAction` (l3gen)
  and `L2genAquariusAction` (l2gen_aquarius), selected via `L2genData.Mode`.

Treat `l2gen` as a subsystem with its own idioms. Changes to the generic XML path
do not affect it, and vice versa.

### 8.5 The multilevel processor

`processor/` implements OCSSW's `multilevel_processor` (MLP) — a chain runner
that takes a list of input files and a sequence of processing levels.
`MultilevelProcessorModel` / `MultlevelProcessorForm` / `MultilevelProcessorRow`
build a table of per-level configurations and serialise it into an MLP parameter
file (`OCSSW.MLP_PAR_FILE_NAME`). In remote mode the par file is uploaded
(`uploadMLPParFile`) and outputs are retrieved with `getMLPOutputFilesList` /
`downloadMLPOutputFile`.

### 8.6 Preferences panels

`preferences/OCSSW_*Controller` classes extend SNAP's `DefaultConfigController`
and register with:

```java
@OptionsPanelController.SubRegistration(location = "SeaDAS",
        displayName = "#Options_DisplayName_OCSSW_L3mapgen",
        keywords    = "#Options_Keywords_OCSSW_L3mapgen",
        id = "L3mapgen_preferences", position = 10)
```

The container itself (`Tools → Options → SeaDAS`) is declared once in
`preferences/package-info.java` via
`@OptionsPanelController.ContainerRegistration(id = "SeaDAS", …)`.

Panels are built with Ceres `Property` / `PropertyContainer` / `BindingContext`
and persist through SNAP's `PropertyMap` / `Config.instance().preferences()`.
Each panel defines its own property-key constants; `SeadasToolboxDefaults` only
holds the root prefix `seadas.toolbox`. There is no central key registry — see
§12 for the collected list.

Current panels: L3mapgen, L2gen, L2bin, L3bin, Extractors, OCSSW Installer.
`seadas-watermask-operator` and `seadas-bathymetry-operator` register their own
(`Landmask_Controller`, `Bathymetry_Controller`), as does the Earthdata module
(`Earthdata_Cloud_Controller`).

---

## 9. Cookbook: common development tasks

### 9.1 Add support for a new OCSSW program

For a program that fits the generic form, this is a three-file change and no
bespoke UI code.

1. **Write the parameter descriptor.**
   `seadas-processing/src/main/resources/gov/nasa/gsfc/seadas/processing/core/<program>.xml`,
   conforming to `ParamInfo-1.0.xsd`. The fastest starting point is to run the
   program with `--dump_options_xmlfile`, then hand-edit: set `primaryOptions`,
   add `progressRegex` if the program reports progress, and assign
   `subPanelIndex`/`order` for layout.

2. **Register the program name.** Add it to the `processorHashMap` in
   `core/ProcessorTypeInfo` — mapping to an existing `ProcessorID` if it behaves
   like one that already exists, or to a new enum constant if it needs custom
   logic.

3. **Write the action.** Copy an existing `ui/*Action.java` (`L3MapGenAction` is
   a clean template), change the class name, the `@ActionID` id, the
   `@ActionReference` path and `position`, and the five `CTL_*` bundle keys —
   `_Name`, `_ProgramName`, `_DialogTitle`, `_XMLFileName`, `_Description`.

4. **Make sure the menu folder exists.** If the `@ActionReference` path names a
   folder that is not declared in `seadas-kit`'s `layer.xml`, the item will not
   appear. Add the folder (with a `position`) if needed.

5. **If it needs custom behaviour** — filename derivation, coupled parameters, a
   secondary program — add a `private static class <Name>_Processor extends
   ProcessorModel` inside `ProcessorModel` and a `case` in
   `ProcessorModel.valueOf`. Do not put program-specific logic in the generic
   `ProcessorModel` body or in the UI factory.

6. **Add the help page.** `resources/.../docs/processors/Process<Name>.html`, plus
   entries in `map.jhm` and `toc.xml` (see §11).

Rebuild `seadas-processing` and `seadas-kit`, relaunch, and verify the dialog
opens, the parameters are laid out sensibly, Run is enabled once ifile/ofile are
set, and the progress bar moves.

### 9.2 Add a new user-facing entry point

Any new entry point needs **three things wired together**:

1. an `@ActionReference(path = "Menu/…")` on the action class,
2. a folder for that path declared in `seadas-kit/src/main/resources/gov/nasa/gsfc/seadas/layer.xml`
   with a `position` attribute,
3. for OCSSW programs, a parameter XML under `core/`.

Miss any one and the build still succeeds.

The existing top-level menu skeleton:

```
Menu/
├── SeaDAS-Toolbox            (position 350)
│   ├── SeaDAS Processors     (1000)
│   │   ├── Tools    (10)
│   │   ├── HawkEye  (20)
│   │   ├── MODIS    (30)
│   │   └── VIIRS    (40)
│   └── General Tools         (2000)
├── Earthdata-Cloud           (800)
├── Video-Tutorials           (1100)
│   └── Overview / Installation / General Tools / Troubleshooting /
│       Science Processors (Installation) / Science Processors / Earthdata-Cloud
└── Help/SeaDAS               (1500)
    ├── Configuration (20)
    └── Data Access   (100)
```

### 9.3 Add a new Maven module

1. Create the directory with a POM whose `<parent>` is `gov.nasa.gsfc.seadas:seadas`,
   `<packaging>nbm</packaging>`, and dependencies declared **without versions**
   (they come from the root `dependencyManagement`).
2. Add a version property for it in the root POM (with the commented `-SNAPSHOT`
   twin, matching the existing style).
3. Add `<module>…</module>` to the root `<modules>` list.
4. Add a `<dependency>` on it in `seadas-kit/pom.xml` — **in the same order as
   the root `<modules>` list**, as the comment there asks.
5. Create `src/main/nbm/manifest.mf` with at least:
   ```
   Manifest-Version: 1.0
   AutoUpdate-Show-In-Client: false
   AutoUpdate-Essential-Module: false
   OpenIDE-Module-Display-Category: SeaDAS Toolbox
   OpenIDE-Module-Specification-Version: ${seadas.nbmSpecVersion}
   OpenIDE-Module-Layer: layer.xml          # only if you have one
   OpenIDE-Module-Long-Description: …
   ```
6. Declare the `build-helper-maven-plugin` and `maven-jar-plugin`
   (`useDefaultManifestFile`) executions in the module POM, as the existing
   modules do.

Steps 3 and 4 are both required. A module in the reactor but not in the kit
builds and is never shipped.

### 9.4 Add a preferences panel

Create a controller extending `DefaultConfigController`, annotate it with
`@OptionsPanelController.SubRegistration(location = "SeaDAS", id = "…", position = …)`
and an `@NbBundle.Messages` block for the display name and keywords. Build the
form in `createPanel(BindingContext)`, define `Preference`-annotated properties,
and read/write through `SnapApp.getDefault().getPreferences()` /
`Config.instance().preferences()`. Namespace your keys under `seadas.` and keep
them as `public static final String` constants in the controller.

### 9.5 Add a GPF operator

Follow the pattern in `seadas-watermask-operator` / `seadas-bathymetry-operator`:
an `@OperatorMetadata`-annotated `Operator` subclass with `@SourceProduct`,
`@TargetProduct` and `@Parameter` fields, a `Spi` inner class, and — for a menu
entry — either a dedicated `*Action` class or a `DefaultOperatorAction` entry in
the module's `layer.xml`. Auxdata that must be downloaded on demand goes through
a `ResourceInstallationUtils`-style installer with a progress dialog, as both
mask operators do.

---

## 10. The other modules

### 10.1 `seadas-earthdata-cloud-toolbox`

NASA Earthdata Cloud access. The interesting pieces:

- **`data/`** — metadata fetchers hitting `cmr.earthdata.nasa.gov`:
  `CmrGranuleMetadataFetcher`, `CmrVariableMetadataFetcher`,
  `OBDAACMetadataFetcher`, `FileVariableMetadataFetcher`, behind a
  `VariableMetadataFetcher` interface, with a `VariableCache` /
  `JsonVariableCache` layer keyed by `VariableCacheKey`.
- **`src/main/resources/json-files/*.json`** — per-mission collection descriptors
  (MODISA, VIIRS, PACE, …) checked into the repo and periodically refreshed from
  CMR. Commits titled "update the json-files with latest in CMR" are routine
  maintenance, not feature work.
- **`auth/WebPageFetcherWithJWT`** — Earthdata Login (EDL) token handling.
- **`action/HarmonySubsetTask`** + **`ui/HarmonySubsetServiceDialog`** — Harmony
  subsetting requests; `HarmonySearchServiceDialog` for search.
- **`ui/OBDAACDataBrowser`** — the OB.DAAC browse/download UI;
  `util/FileDownloadManager` handles the transfers.
- **`util/PythonScriptRunner`** invokes the helper scripts in
  `src/main/resources/scripts/`.
- Module-level notes: `HARMONY_SUBSET_SERVICE_GUIDE.md`,
  `DOWNLOAD_REFACTORING_SUMMARY.md`.

Note this module has no `src/main/nbm/manifest.mf`; it supplies
`src/main/resources/META-INF/MANIFEST.MF` directly and declares
`publicPackages` in its POM.

### 10.2 `seadas-watermask-operator`

`WatermaskOp` (GPF) + `WatermaskClassifier` produce land, water and coastline
masks. Source data is SRTM-derived PNG tiles (`PNGSourceImage`, `SRTMOpImage`)
with MODIS-derived north/south polar coverage (`ModisNorthImage`,
`ModisSouthImage`). Resolution files are installed on demand
(`InstallResolutionFileDialog`, `FileInstallRunnable`). `LandMasksDialog` is the
user-facing dialog; `Landmask_Controller` the Options panel. The `util/` package
holds the offline tools used to *generate* the auxdata (shapefile rasterisation,
MODIS mosaicking) — these are not part of the runtime path.

### 10.3 `seadas-bathymetry-operator`

Same shape as the watermask module: `BathymetryOp` + `BathymetryReader` produce
bathymetry, elevation and topography bands plus a depth-range mask.
`EarthBox`/`MotherEarthBox` handle the tiling geometry. `BathymetryDialog` and
`Bathymetry_Controller` are the UI and preferences.

### 10.4 `seadas-contour-operator`

A JAI-based contour generator: `ContourDescriptor` / `ContourRIF` /
`ContourOpImage` / `Segments` compute the contour segments; `Java2DConverter`
turns them into geometry; `ShowVectorContourOverlayAction` and `ContourDialog`
add the result to the product as a vector data node so SNAP renders it as an
overlay. `ContourLineFigureStyle` controls appearance.

### 10.5 `seadas-image-animator`

`ImageAnimatorDialog` plus `AngularAnimationTopComponent` /
`SpectrumAnimationTopComponent` animate a selected set of bands in the SNAP image
view. `GifSequenceWriter` exports the sequence. `JCheckBoxTree` is a local
widget for band selection.

### 10.6 `seadas-metadata-tools`

The smallest module: `MetadataDumpTopComponent` renders a product's metadata
tree as text, `MetadataTreeBuilder` builds it, `MetadataSelectionWatcher` keeps
it in sync with the selected node. Registered through a NetBeans
`OpenIDE-Module-Install` hook (`Installer`, `StartupHook`) rather than a layer.

---

## 11. Help and documentation

The toolbox uses **JavaHelp, not Javadoc**, for user documentation.

Sources live under
`seadas-processing/src/main/resources/gov/nasa/gsfc/seadas/processing/docs/`:

```
help.hs        the HelpSet descriptor
map.jhm        help-ID → HTML file mapping
toc.xml        table of contents
toc.html       generated from toc.xml + map.jhm
processors/    one HTML page per processor (Process*.html)
Topics/        general topics
OcsswCookbook/ GptCookbook/ EarthdataCloud/ FieldMeasurements/ Drafts/
images/  style.css  style_new.css
```

Registration happens in `docs/package-info.java`:

```java
@HelpSetRegistration(helpSet = "help.hs", position = 4410)
package gov.nasa.gsfc.seadas.processing.docs;
```

The other modules (`contour`, `watermask`, `bathymetry`, `image-animator`)
register a `helpset.xml` through their own `layer.xml` under
`Services/javahelp` instead.

### Generated help sections — do not hand-edit

The command-line help blocks inside the processor HTML pages are **generated**,
delimited by:

```html
<!--AUTOMATED CODE HELP START-->
   … generated from `<program> --help` against an installed OCSSW …
<!--AUTOMATED CODE HELP END-->
```

Regenerate them with an installed OCSSW on the path:

```bash
bin/updateCommandLineHelpInSeaDASGUI.py  <path-to>/processing/docs/processors
bin/updateExtractorsHelpInSeaDASGUI.py   <path-to>/processing/docs/processors
```

Then rebuild `toc.html`:

```bash
bin/convertXML2HTML.py
```

Editing between the markers by hand guarantees your change is lost at the next
regeneration.

---

## 12. Preferences reference

Preferences are stored through SNAP's `Config.instance("seadas").preferences()`
and are surfaced in `Tools → Options → SeaDAS`. There is no central constants
class — keys are defined next to the code that uses them, principally in
`OCSSWInfo` and `OCSSWConfigData`.

**OCSSW configuration**

| Key | Meaning |
| --- | --- |
| `seadas.ocssw.location` | `local` \| `virtualMachine` \| `remoteServer` \| `docker` — selects the `OCSSW` implementation. |
| `seadas.ocssw.root` | OCSSW installation directory. `bin/`, `share/` and `ocssw_runner` are derived from it. |
| `seadas.ocssw.tag` | Installed OCSSW tag. |
| `seadas.ocssw.version` | |
| `seadas.ocssw.serverAddress` | Remote/VM server host. |
| `seadas.ocssw.port` | Remote/VM server port (client default `6400`, context path `ocsswws`). |
| `seadas.ocssw.sharedDir` | Shared directory between client and server. |
| `seadas.ocssw.processInputStreamPort` | Port for streaming process stdout back to the client. |
| `seadas.ocssw.processErrorStreamPort` | Same for stderr. |
| `seadas.ocssw.deleteFilesOnServer` / `…keepFilesOnServer` | Server-side cleanup policy. |
| `seadas.ocssw.debug` | Extra diagnostic output. |
| `seadas.client.id` | Client identity used to scope server-side jobs. |
| `seadas.log.dir` | Where `OCSSW_LOG_<program>.txt` files are written. |
| `seadas.version` | |
| `seadas.toolbox` | Root prefix for toolbox preferences (`SeadasToolboxDefaults`). |

**Help/web links** (each maps a menu item to a URL, all under `seadas.`):
`homepage.url`, `tutorials`, `presentations`, `oceanColorForum`, `installation`,
`installationDetails`, `systemRequirements`, `clientServer`,
`dataAccessDirect`, `dataAccessFileSearch`, `showWebOpendap`, `showWebSeabass`,
`showWebOcSubscriptions`, `showWebEarthExplorer`,
`showWebObpgAlgorithmDescriptions`, `video01`…`video09`,
`playlist01`…`playlist08`.

**Processor defaults** are namespaced per panel by the individual
`OCSSW_*Controller` classes (favourites, projections, naming schemes, geospatial
defaults). Look in the controller for the exact keys.

**Remote OCSSW endpoints** (`http://<host>:<port>/ocsswws/…`):
`jobs`, `newJobId`, `processStatus`, `ocsswSetClientId`, `ocsswSetProgramName`,
`uploadClientFile`, `uploadParFile`, `uploadMLPParFile`,
`uploadNextLevelNameParams`, `executeOcsswProgram`, `executeOcsswProgramSimple`,
`executeOcsswProgramOnDemand`, `executeOcsswProgramAndGetStdout`,
`executeParFile`, `executeUpdateLutsProgram`, `getOfileName`, `getFileInfo`,
`getFileCharSet`, `getSensorInfoFileContent`, `missionSuites`,
`isMissionDirExist`, `ocsswTags`, `productXmlFile`, `convertLonLat2Pixels`,
`getConvertedPixels`, `downloadFile`, `downloadAncFileList`,
`getMLPOutputFilesList`, `downloadMLPOutputFile`, `fileServices/fileVerification`.

**Branding-related properties** (in the SNAP forks, not here — see
`docs/BrandingNotes`): `snap-engine/etc/seadas.properties`,
`snap-engine/etc/seadas.auxdata.properties`, `snap-desktop/etc/seadas.conf`.
SeaDAS overrides SNAP's colour-manipulation defaults there
(`seadas.color.manipulation.*`), and those files need re-checking each release
when SNAP changes its own.

---

## 13. Testing

**Coverage is thin — a few dozen JUnit 4 tests in total, and most modules have
none.** Present tests:

```
seadas-processing/src/test/…  ProcessorModelTest, CallCloProgramActionTest,
                              RSClientTest, ProcessObserverTest, OCSSWInfoGUITest
seadas-watermask-operator     WatermaskClassifierTest, WatermaskOpTest,
                              LandMaskRasterCreatorTest, ShapeFileRasterizerTest
seadas-bathymetry-operator    BathymetryOpTest, LandMaskRasterCreatorTest,
                              ShapeFileRasterizerTest
seadas-earthdata-cloud-toolbox ImagePreviewHelperTest
seadas-ocsswrest (parked)     OCSSWServicesTest, OCSSWFileServicesTest, …
seadas-writeimage-operator (parked) WriteImageOpTest
```

Note that `seadas-processing`'s test tree contains a directory literally named
`gov.nasa.gsfc.seadas.processing` (dots, not slashes) alongside the correct
`gov/nasa/gsfc/seadas/processing` tree — three test classes live under the
malformed path and are not compiled as the package they declare.

**Do not assume `mvn test` validates a change.** For anything touching the UI or
OCSSW, the real verification loop is:

1. `mvn install -Dmaven.test.skip=true` (or `-pl <module>,seadas-kit`)
2. launch SNAP from the IDE with `--patches` pointing at your `target/classes`
3. exercise the dialog end-to-end against a real OCSSW installation
4. check `${seadas.log.dir}/OCSSW_LOG_<program>.txt` for the executed command

For changes to the `OCSSW` abstraction, test in **at least two modes** (local and
one of remote/VM). Local-only testing is the most common source of regressions in
that layer.

---

## 14. Packaging, distribution and release

### 14.1 The NBM / cluster path

`mvn install` at the root produces, for each module, a signed `.nbm` and its
contribution to the cluster; `seadas-kit` then assembles
`seadas-kit/target/netbeans_clusters/seadas` and runs the `autoupdate` goal to
generate the update catalogue. `layer.xml` in `seadas-kit` also registers the
update centre itself:

```xml
<folder name="Services"><folder name="AutoupdateType">
  <file name="seadas_toolbox_update_center.instance">
    <attr name="displayName" stringvalue="SeaDAS Toolbox"/>
    <attr name="url" urlvalue="https://oceandata.sci.gsfc.nasa.gov/SeaDAS/updatecenter/9.x/seadas-toolbox/updates.xml.gz"/>
  </file>
</folder></folder>
```

`bin/GenAUCatalog.class` regenerates `updates.xml` / `updates.xml.gz` from a
directory of NBMs; `bin/copy_nbm` gathers the NBMs from the source tree.

Version reporting at runtime is handled by
`common/SeadasToolboxVersion`, which compares the installed module's
specification version against
`https://oceandata.sci.gsfc.nasa.gov/seadas-downloads/SEADAS_TOOLBOX_VERSION.txt`
and prompts the user when a newer release exists.

### 14.2 The IzPack installer

`seadas-installer/` (outside the reactor) builds the full SeaDAS application
installers — SNAP platform + optical toolbox + seadas cluster, with or without
a bundled JRE.

```
seadas-installer/
├── izpack-installer/     the installer assembly
│   ├── pom.xml           one Maven profile per platform (mac, linux, win), plus nojre
│   ├── build-all.sh      builds all six installers
│   ├── src/main/izpack/  descriptors and packs/
│   ├── src/main/xslt/    without-bundled-jre.xsl (derives the no-JRE descriptors)
│   └── dist/             output (do not delete — rebuilds overwrite)
├── izpack-panels/        custom IzPack panels
└── docs/IzPack-Packaging-Instruction.md
```

Build:

```bash
cd seadas-installer/izpack-installer
./build-all.sh                    # all six: mac, linux, win and their -nojre twins
./build-all.sh linux win          # only the named platforms
./build-all.sh linux-nojre        # = mvn clean package -P linux,nojre
OUTDIR=/tmp/installers ./build-all.sh
```

Each platform is selected by a Maven profile — the descriptor is chosen by
profile, not by copying a file over `install.xml`. `mvn clean package` wipes
`target/`, so `build-all.sh` moves each artifact into `OUTDIR` (default
`./dist`) before the next platform starts. Roughly 2–3 minutes and ~900 MB per
platform. Nothing is deleted up front: a failed build leaves the last good
installer in place. The Windows profile additionally wraps the jar with
launch4j (`l4j-gui`) to produce an `.exe`.

**Installers without a bundled JRE.** Adding the `nojre` profile to a platform
(`-P linux,nojre`) produces `seadas-installer-<platform>-nojre.jar` (and `.exe`
on Windows), about 50 MB smaller. There is no hand-maintained
`*-without-bundled-jre.xml`: the descriptor is generated into `target/staging`
from `install-for-<os>.xml` by `src/main/xslt/without-bundled-jre.xsl`, so a
fix to a bundled descriptor reaches its no-JRE twin automatically. The
stylesheet changes only the following. It drops the pack installing from
`packs/jre/` and adds a `JDKPathPanel` after the licence panel. It presets
`jdkPath` once to the running JVM's home (`${JAVA_HOME}`) and sets `jdkhome` to
`${jdkPath}`, the panel's result; the name is case-sensitive. It drops
`JDKPathPanel.maxVersion` and registers `resources/nojre-langpack-eng.xml`,
which replaces IzPack's panel text: that text assumes a maximum version and
links to java.sun.com. The preset is needed because IzPack 5.2.4 guesses the
JDK as the *parent* of the running JVM's home. That was right when `java.home`
was `<jdk>/jre` (Java 8 and older); on Java 9+ the guess always fails and the
panel always prompts. The build fails if the result still references
`packs/jre/`, lacks the panel, or does not set `jdkhome` from `jdkPath`.

The user needs a full **JDK 21+**, not a JRE: IzPack's `JDKPathPanel` rejects any
folder without `bin/javac`. The panel skips itself when the JVM running the
installer is a JDK 21+; otherwise it asks for one. The Windows launch4j wrapper sets only a minimum JVM
version (21), so both `.exe` installers start on any Java 21 or newer.

The installer copies three cluster trees into `src/main/izpack/packs/`:
`snap-desktop/snap-application/target/snap/`,
`optical-toolbox/opttbx-kit/target/netbeans_clusters/opttbx`, and
`seadas-toolbox/seadas-kit/target/netbeans_clusters/seadas` — so **the full stack
must be built and installed before packaging**. These trees are Java and
platform-independent, so all six installers share them.

The installed `bin/` and `etc/` do **not** come from the SNAP tree: every
descriptor excludes `bin/*` and `etc/*` from `packs/snap` and installs them
from `src/main/izpack/packs/files/<os>/` instead (`unix`, `macosx`, `winx64`).
That is where SeaDAS's launcher configuration lives (`seadas.clusters`,
`seadas.conf`, `snap.conf` with `extra_clusters`, `snap.properties`), so
branding or cluster changes go there. `seadas-installer/docs/IzPack-Packaging-Instruction.md`
describes an older setup that edited `packs/snap/etc/` directly; the pom no
longer does that, since those files were never installed.

Build with `clean` (`build-all.sh` always does). The copy into `target/staging`
only replaces files whose source is newer and never removes files deleted
upstream, so a `mvn package` over an old `target/` can pick up leftovers.

### 14.3 Release checklist (as practised)

1. Confirm the SNAP fork branches (`snap-engine`, `snap-desktop`,
   `optical-toolbox`) are at the intended state and build clean.
2. Flip `${seadas-toolbox.version}` and every per-module version property in the
   root POM off `-SNAPSHOT`, and the `<version>` elements in the root and child
   POMs to match.
3. Regenerate the command-line help sections (§11) against the OCSSW tag being
   shipped.
4. Full clean build of all four repositories.
5. Build the installers (`build-all.sh`) and smoke-test each platform.
6. Tag all four repositories with a matching annotated tag and an explicit
   `GIT_COMMITTER_DATE`.
7. Publish the NBMs to the update centre and the installers to the download site.
8. Update `CHANGELOG.txt` and `docs/release-notes/`.

> Merging to `master` is the maintainer's job. Development happens on the
> `SEADAS-<toolbox>-SNAP-<snap>` release branch.

---

## 15. Developer scripts (`bin/`)

| Script | Purpose |
| --- | --- |
| `updateCommandLineHelpInSeaDASGUI.py <docs/processors>` | Regenerates the `<!--AUTOMATED CODE HELP …-->` blocks in the processor help pages from an installed OCSSW. |
| `updateExtractorsHelpInSeaDASGUI.py <docs/processors>` | Same, for the extractor pages. |
| `convertXML2HTML.py` | Builds `toc.html` from `toc.xml` + `map.jhm`, copying the HTML and images. (`convertXML2HTML20221209.py` is the previous version.) |
| `copy_nbm` | Copies all NBMs from the source tree into a target directory. |
| `GenAUCatalog.class` | Generates `updates.xml` / `updates.xml.gz` for the update centre. |
| `workflow*.sh`, `workflow_l2gen.sh`, `workflow_l2bin.sh`, … | Reference OCSSW command-line workflows, useful for reproducing a GUI run on the shell. |
| `l2gen_custom.par` | Sample l2gen parameter file. |
| `blue_marble.py`, `blue_marble_part1.py` | Auxdata generation helpers. |

`docs/` also holds durable notes worth reading before a release:
`GitNotes` (branch/tag recipes), `DevelopmentNotes` (where branding, splash,
about box, and the `seadas.*` config files live in the SNAP forks),
`BrandingNotes` (SNAP-vs-SeaDAS default property values), `dev/versions.md`,
`dev/config`, `release-notes/`.

---

## 16. Conventions, gotchas and known rough edges

**Conventions to match**

- All Java lives under `gov.nasa.gsfc.seadas.*`; each module owns exactly one
  subpackage.
- Actions are registered by annotation, never by hand-written layer entries
  (except the folder skeleton in `seadas-kit`).
- Program-specific behaviour goes in a `ProcessorModel` subclass, not in the
  generic model or the UI factory.
- Everything that touches OCSSW goes through the `OCSSW` abstraction.
- Module dependencies are declared without versions; add the version to the root
  `dependencyManagement` instead.
- `CLAUDE.md` and `.claude/` are in `.gitignore` and intentionally untracked.

**Gotchas**

- **Three-place wiring.** A new menu entry needs the `@ActionReference` path, the
  folder in `seadas-kit`'s `layer.xml`, and (for processors) the param XML.
  Missing pieces fail silently.
- **Two-place module wiring.** Root `<modules>` *and* `seadas-kit` dependency
  list.
- **Version properties are duplicated** across the root POM properties, the root
  `<version>`, and every child POM's `<version>` and `<parent><version>`. They
  are maintained by hand.
- **`seadas-kit` must be rebuilt** after any module change for the cluster to
  reflect it — but the IDE `--patches` mechanism makes this unnecessary for pure
  Java changes during development.
- **`seadas-processing/src/main/resources/.../processing/layer.xml` is dead
  weight.** It registers a `helpset.xml` that does not exist, and the module's
  manifest has no `OpenIDE-Module-Layer` line, so the layer is never loaded. The
  help set is registered by `docs/package-info.java` instead. Do not add entries
  to that layer file expecting them to take effect.
- **`VERSION.txt` at the repo root contains the literal string
  `${project.version}`** — it is a filtering template that is only substituted
  when copied by the assembly, so it is not a source of truth for the version.
- **Dead code paths in `ProcessorTypeInfo`**: `SMIGEN`, `L2BIN_AQUARIUS`,
  `L1MAPGEN`, `L2MAPGEN`, `NEXT_LEVEL_NAME_PY`, `OBPG_FILE_TYPE_PY` and
  `MULTILEVEL_PROCESSOR_PY` are commented out, but their XML descriptors and (for
  some) their `ProcessorModel` subclasses are still present. Do not assume a
  descriptor file implies a reachable feature.
- **The malformed test source directory** noted in §13.
- **The NBM signing keystore is checked in** (`keystore/seadas.ks`, alias `snap`,
  password `snap-123`, also spelled out in the root POM). It exists so unsigned
  modules do not trip NetBeans' verification, not as a security boundary.
