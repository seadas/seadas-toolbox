##### Before Start Packaging

###### Build the project

`mvn clean install` 
    <entries>
      <fileEntry mountPoint="611" file="./images/SNAP_icon_48.jpg" />
      <fileEntry mountPoint="646" file="../snap-desktop/snap-application/target/snap/etc/snap.clusters" />
      <dirEntry mountPoint="646" file="../snap-engine/etc" fileMode="666" overrideFileMode="true" subDirectory="etc" />
      <fileEntry mountPoint="1017" file="../snap-desktop/snap-main/target/snap-main.jar" />
      <dirEntry mountPoint="58" file="../snap-desktop/snap-application/target/snap/snap" entryMode="subdir" subDirectory="snap" />
      <dirEntry mountPoint="58" file="../snap-desktop/snap-application/target/snap/platform" entryMode="subdir" subDirectory="platform" />
      <dirEntry mountPoint="58" file="../snap-desktop/snap-application/target/snap/ide" entryMode="subdir" subDirectory="ide" />
      <dirEntry mountPoint="58" file="../s1tbx/s1tbx-kit/target/netbeans_clusters/s1tbx" entryMode="subdir" subDirectory="s1tbx" />
      <dirEntry mountPoint="58" file="../s2tbx/s2tbx-kit/target/netbeans_clusters/s2tbx" entryMode="subdir" subDirectory="s2tbx" />
      <dirEntry mountPoint="58" file="../s3tbx/s3tbx-kit/target/netbeans_clusters/s3tbx" entryMode="subdir" subDirectory="s3tbx" />
      <dirEntry mountPoint="58" file="../s1tbx/s1tbx-kit/target/netbeans_clusters/rstb" entryMode="subdir" subDirectory="rstb" />
      <dirEntry mountPoint="58" file="../smos-box/smos-kit/target/netbeans_clusters/smos" entryMode="subdir" subDirectory="smos" />
      <dirEntry mountPoint="58" file="../probavbox/probavbox-kit/target/netbeans_clusters/probavbox" entryMode="subdir" subDirectory="probavbox" />
      <fileEntry mountPoint="58" file="./LICENSE.txt" />
      <fileEntry mountPoint="58" file="../snap-desktop/snap-application/target/snap/THIRDPARTY_LICENSES.txt" />
      <fileEntry mountPoint="58" file="./VERSION.txt" />
      <dirEntry mountPoint="636" file="./files/macosx" subDirectory="macosx" />
      <dirEntry mountPoint="643" file="./files/winx32" subDirectory="winx32" />
      <dirEntry mountPoint="644" file="./files/winx64" subDirectory="winx64" />
      <dirEntry mountPoint="645" file="./files/unix" subDirectory="unix" />
    </entries>
    
 <component name="SNAP" id="76" customizedId="SNAP" hideHelpButton="true" changeable="false">
        <description>Sentinels Application Platform.</description>
        <include>
          <entry location="bin" />
          <entry location="etc" />
          <entry location="snap" />
          <entry location="platform" />
          <entry location="ide" />
          <entry location="LICENSE.txt" />
          <entry location="THIRDPARTY_LICENSES.txt" />
          <entry location="VERSION.txt" />
          <entry filesetId="634" />
          <entry filesetId="637" />
          <entry filesetId="639" />
          <entry filesetId="641" />
        </include>