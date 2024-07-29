SeaDAS Toolbox (seadas-toolbox)
==========================

A toolbox for the OBPG processing code.

[![Build Status](https://travis-ci.org/senbox-org/s3tbx.svg?branch=master)](https://travis-ci.org/senbox-org/s3tbx)
[![Coverity Scan Status](https://scan.coverity.com/projects/7247/badge.svg)](https://scan.coverity.com/projects/senbox-org-s3tbx)

Building seadas-toolbox from the source
------------------------------

Download and install the required build tools
* Install OpenJDK 11 and set JAVA_HOME accordingly.
* Install Maven and set MAVEN_HOME accordingly.
* Install git

Add $JAVA_HOME/bin, $MAVEN_HOME/bin to your PATH.

Clone the following SeaDAS git repositories into a directory referred to here as [SEADAS]

    cd ${snap}
    git clone https://github.com/seadas/seadas-toolbox.git
    git clone https://github.com/senbox-org/optical-toolbox.git
    git clone https://github.com/senbox-org/snap-desktop.git
    git clone https://github.com/senbox-org/snap-engine.git

Checkout and build the corresponding branches for your desired release.  See SeaDAS Release Tags (below) for other SeaDAS 8 versions.

SNAP-Engine:

    cd [SEADAS]/snap-engine
    git checkout SEADAS-9.0.2-SNAP-11.0.0-04-30-24
    mvn install -Dmaven.test.skip=true
    *NOTE if mvn fails then try: 'mvn install'
    * Also you could try this:  1. mvn clean -U compile install   
                                2. mvn install  -Dmaven.test.skip=true

SNAP-Desktop:

    cd [SEADAS]/snap-desktop
    git checkout  SEADAS-9.0.2-SNAP-11.0.0-04-30-24
    mvn install -Dmaven.test.skip=true

Sentinel-3 Toolbox:

    cd [SEADAS]/optical-toolbox
    git checkout  SEADAS-9.0.2-SNAP-11.0.0-04-30-24
    mvn install -Dmaven.test.skip=true

SeaDAS Toolbox:

    cd [SEADAS]/seadas-toolbox
    git checkout master
    mvn install -Dmaven.test.skip=true



Setting up IntelliJ IDEA
------------------------


1. In IntelliJ IDEA, select "Import Project" and select the ${snap} directory. (Some versions: select "New -> Project From Existing Sources", then navigate upwards in the file selector to select the ${snap} directory, then select "Open")
2. Select "Import project from external model" -> "Maven"
3. Ensure the "Root directory" is ${snap}. (Note: put your actual path).
   Select "Search for projects recursively"; Ensure **not** to enable the option *Create module groups for multi-module Maven projects*. Everything can be default values.

4. Set the used SDK for the main project. SeaDAS-8.3.10 is tested with OpenJDK 11.0.9.

5. Use the following configuration to run SNAP in the IDE:

   **Main class:** `org.esa.snap.nbexec.Launcher`

   **VM parameters:** `-Dsun.awt.nopixfmt=true -Dsun.java2d.noddraw=true -Dsun.java2d.dpiaware=false`

   All VM parameters are optional

   **Program arguments:**    
   `--userdir "${snap}/seadas-toolbox/target/userdir"`
   `--clusters "${snap}/seadas-toolbox/seadas-kit/target/netbeans_clusters/seadas:${snap}/optical-toolbox/opttbx-kit/target/netbeans_clusters/opttbx"`
   `--patches "${snap}/snap-engine/$/target/classes:${snap}/seadas-toolbox/$/target/classes:${snap}/optical-toolbox/$/target/classes"`

   **Working directory:** `${snap}/snap-desktop/snap-application/target/snap/`

   **Use classpath of module:** `snap-main`



SeaDAS Release Tags
------------------------
SeaDAS Release: 9.0.1

https://github.com/seadas/seadas-toolbox/releases/tag/SEADAS-9.0.1
https://github.com/senbox-org/snap-desktop/releases/tag/SEADAS-9.0.1
https://github.com/senbox-org/snap-engine/releases/tag/SEADAS-9.0.1
https://github.com/senbox-org/optical-toolbox/releases/tag/SEADAS-9.0.1



