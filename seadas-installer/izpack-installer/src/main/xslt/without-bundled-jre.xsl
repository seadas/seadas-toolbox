<?xml version="1.0" encoding="UTF-8"?>
<!--
    Derives a "without bundled JRE" IzPack descriptor from a per-OS
    install-for-*.xml.  Applied by the 'nojre' profile in pom.xml; the result
    only ever exists in target/staging.

    The bundled and non-bundled installers differ only in the points below;
    everything else is copied through unchanged, so the two can never drift
    apart the way hand-maintained *-without-bundled-jre.xml copies did:

      1. the JRE pack (the one installing from packs/jre/) is dropped;
      2. a JDKPathPanel is added after the licence panel.  It skips itself when
         jdkPath already holds a valid JDK, otherwise asks for one.  IzPack's
         validity check requires bin/javac, so this needs a full JDK;
      3. jdkPath is preset, once, to the home of the JVM running the installer.
         IzPack 5.2.4's own guess is the *parent* of that home, which only
         worked when java.home was <jdk>/jre (Java 8 and older); on Java 9+ it
         never finds a JDK, so without this the panel always prompts;
      4. the launchers' jdkhome (substituted into etc/snap.conf and
         etc/seadas.conf) points at ${jdkPath}, the panel's result, instead of
         the bundled JRE.  The variable name is case-sensitive;
      5. JDKPathPanel.maxVersion is dropped, so any Java 21 or newer passes,
         and resources/nojre-langpack-eng.xml replaces IzPack's panel text,
         which assumed a maximum version and pointed at java.sun.com.
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

    <!-- 3. offer the running JVM's home (checkonce: never overwrite the path
            the user chose), and 4. launch SeaDAS with the chosen JDK -->
    <xsl:template match="dynamicvariables/variable[@name = 'jdkhome']">
        <variable name="jdkPath" value="${{JAVA_HOME}}" checkonce="true"/>
        <xsl:text>&#10;        </xsl:text>
        <variable name="jdkhome" value="${{jdkPath}}"/>
    </xsl:template>

    <!-- 5. no upper Java version limit, and panel text that does not expect one -->
    <xsl:template match="variables/variable[@name = 'JDKPathPanel.maxVersion']"/>

    <xsl:template match="resources">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
            <res id="CustomLangPack.xml_eng" src="resources/nojre-langpack-eng.xml"/>
            <xsl:text>&#10;    </xsl:text>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
