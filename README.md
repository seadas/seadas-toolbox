SeaDAS Toolbox (seadas-toolbox)
===============================

A toolbox for the OBPG processing code. It adds NASA Ocean Biology Processing
Group tools (OCSSW processors, Earthdata search and download, land/water mask,
bathymetry, contours and more) to the ESA SNAP desktop application as a set of
NetBeans modules.

For architecture, conventions and day-to-day development, see the
[developer's manual](docs/DEVELOPERS_MANUAL.md).

Building seadas-toolbox from the source
---------------------------------------

### Prerequisites

* **JDK 21**, with `JAVA_HOME` set accordingly. The SNAP build enforces Java 21.
* **Maven 3**, with `MAVEN_HOME` set accordingly.
* **git**

Add `$JAVA_HOME/bin` and `$MAVEN_HOME/bin` to your `PATH`.

### Clone the four repositories

The toolbox builds against three SeaDAS forks of SNAP. Clone all four side by
side into one directory, referred to here as `${seadas}`:

    cd ${seadas}
    git clone https://github.com/senbox-org/snap-engine.git
    git clone https://github.com/senbox-org/snap-desktop.git
    git clone https://github.com/senbox-org/optical-toolbox.git
    git clone https://github.com/seadas/seadas-toolbox.git

### Choose a version

All four repositories must be on matching versions. The `master` branch of the
three SNAP forks is ESA's SNAP, not SeaDAS, so always check out a `SEADAS-…` tag
or branch there.

**Release build**: check out the same release tag in all four repositories,
for example:

    SEADAS-12.0.0-RC2

**Development build** (SeaDAS 12.0.0 on SNAP 14.0.0): check out this branch
in all four repositories:

    SEADAS-12.0.0-SNAP-14.0.0

### Build

Build and install the repositories in this order: snap-engine, snap-desktop,
optical-toolbox, then seadas-toolbox. Replace `<version>` with the tag or
branch chosen above.

    cd ${seadas}/snap-engine
    git checkout <version>
    mvn install -Dmaven.test.skip=true

    cd ${seadas}/snap-desktop
    git checkout <version>
    mvn install -Dmaven.test.skip=true

    cd ${seadas}/optical-toolbox
    git checkout <version>
    mvn install -Dmaven.test.skip=true

    cd ${seadas}/seadas-toolbox
    git checkout <version>
    mvn install -Dmaven.test.skip=true

If a build fails on missing or stale dependencies, retry with
`mvn clean -U install -Dmaven.test.skip=true`.

The toolbox is packaged as a NetBeans cluster in
`seadas-kit/target/netbeans_clusters/seadas`.

Setting up IntelliJ IDEA
------------------------

1. In IntelliJ IDEA, select "Import Project" and select the `${seadas}` directory.
   (Some versions: select "New -> Project From Existing Sources", then navigate
   upwards in the file selector to select the `${seadas}` directory, then select
   "Open".)
2. Select "Import project from external model" -> "Maven".
3. Ensure the "Root directory" is `${seadas}` (your actual path). Select
   "Search for projects recursively". Do **not** enable *Create module groups
   for multi-module Maven projects*. Everything else can keep its default value.
4. Set the project SDK to JDK 21.
5. Use the following configuration to run SeaDAS in the IDE:

   **Main class:** `org.esa.snap.nbexec.Launcher`

   **VM parameters:** `-Dsun.awt.nopixfmt=true -Dsun.java2d.noddraw=true -Dsun.java2d.dpiaware=false`

   All VM parameters are optional.

   **Program arguments:**
   `--userdir "${seadas}/seadas-toolbox/target/userdir"`
   `--clusters "${seadas}/seadas-toolbox/seadas-kit/target/netbeans_clusters/seadas:${seadas}/optical-toolbox/opttbx-kit/target/netbeans_clusters/opttbx"`
   `--patches "${seadas}/snap-engine/$/target/classes:${seadas}/seadas-toolbox/$/target/classes:${seadas}/optical-toolbox/$/target/classes"`

   **Working directory:** `${seadas}/snap-desktop/snap-application/target/snap/`

   **Use classpath of module:** `snap-main`

   With `--patches`, recompiling a module is enough for the next launch to pick
   up a Java change.

Building the installers
-----------------------

The SeaDAS application installers (bundled with Java, or using a JDK 21+ already
on the machine) are built from `seadas-installer/` after the build above. See
[seadas-installer/README.md](seadas-installer/README.md).

SeaDAS Release Tags
-------------------

Each tag exists in all four repositories.

SeaDAS Release: 11.0.0

  https://github.com/seadas/seadas-toolbox/releases/tag/SEADAS-11.0.0

  https://github.com/senbox-org/snap-desktop/releases/tag/SEADAS-11.0.0

  https://github.com/senbox-org/snap-engine/releases/tag/SEADAS-11.0.0

  https://github.com/senbox-org/optical-toolbox/releases/tag/SEADAS-11.0.0

SeaDAS 12.0.0 release candidate: `SEADAS-12.0.0-RC2`

  https://github.com/seadas/seadas-toolbox/releases/tag/SEADAS-12.0.0-RC2

  https://github.com/senbox-org/snap-desktop/releases/tag/SEADAS-12.0.0-RC2

  https://github.com/senbox-org/snap-engine/releases/tag/SEADAS-12.0.0-RC2

  https://github.com/senbox-org/optical-toolbox/releases/tag/SEADAS-12.0.0-RC2
