<?xml version="1.0" encoding="UTF-8"?>
<!--
    Derives a "without bundled JRE" IzPack descriptor from a per-OS
    install-for-*.xml.  Applied by the 'nojre' profile in pom.xml; the result
    only ever exists in target/staging.

    The bundled and non-bundled installers differ in exactly three ways, and
    everything else is copied through unchanged, so the two can never drift
    apart the way hand-maintained *-without-bundled-jre.xml copies did:

      1. the JRE pack (the one installing from packs/jre/) is dropped;
      2. a JDKPathPanel is added after the licence panel.  It skips itself when
         the JVM running the installer is valid, otherwise asks for a JDK home.
         IzPack's validity check requires bin/javac, so this needs a full JDK;
      3. the launchers' jdkhome (substituted into etc/snap.conf and
         etc/seadas.conf) points at the panel's result, ${jdkPath}, instead of
         the bundled JRE.  The variable name is case-sensitive.

    JDKPathPanel.maxVersion is also dropped, so any Java 21 or newer passes.
-->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:output method="xml" encoding="UTF-8"/>

    <!-- identity: copy everything, comments included -->
    <xsl:template match="@* | node()">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:copy>
    </xsl:template>

    <!-- 1. no bundled JRE -->
    <xsl:template match="packs/pack[file[starts-with(@src, 'packs/jre/')]]"/>

    <!-- 2. ask for a JDK when the one running the installer does not qualify -->
    <xsl:template match="panels/panel[@classname = 'HTMLLicencePanel']">
        <xsl:copy-of select="."/>
        <xsl:text>&#10;        </xsl:text>
        <panel classname="JDKPathPanel" id="jdkpath"/>
    </xsl:template>

    <!-- 3. launch SeaDAS with the JDK chosen on that panel -->
    <xsl:template match="dynamicvariables/variable[@name = 'jdkhome']/@value">
        <xsl:attribute name="value">${jdkPath}</xsl:attribute>
    </xsl:template>

    <xsl:template match="variables/variable[@name = 'JDKPathPanel.maxVersion']"/>

</xsl:stylesheet>
