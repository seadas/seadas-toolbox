; Inno Setup script for the final Windows SeaDAS installer.
;
; Not compiled from this directory: 'mvn package -P win' (in ../izpack-installer)
; copies this script, the .rtf and .ico files next to it into
; target/windows/, lays out the installed SeaDAS folder as target/windows/SeaDAS,
; and writes target/windows/build.iss with the values below.  build-all.sh
; then runs ISCC on target/windows/seadas-windows.iss in Docker.
;
; build.iss defines:
;   MyAppVersion  SeaDAS version, from the seadas-toolbox root pom.xml
;   JreDir        name of the bundled JRE folder inside {app}
#include "build.iss"

#define MyAppName "SeaDAS"
#define MyAppPublisher "NASA"
#define MyAppURL "https://seadas.gsfc.nasa.gov"
#define MyAppExeName "seadas64.exe"
#define ArchiveDotSeaDASScriptName "archiveDotSeaDAS_windows.bat"
#define ShortcutIconFileName1 "seadas_installer_icon.ico"
#define ShortcutIconFileName2 "seadas_icon.ico"

[Setup]
; NOTE: The value of AppId uniquely identifies this application. Do not use the same AppId value in installers for other applications.
; Keep it unchanged between releases so a new installer upgrades the old one.
;SignTool=signtool  /n $qNASA OEL$q /t http://timestamp.comodoca.com/authenticode  /d $qSeaDAS Installer$q $f
AppId={{A27DCD92-CD65-440A-93C7-F1A52967E2E4}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
;AppVerName={#MyAppName} {#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}
AppUpdatesURL={#MyAppURL}
DefaultDirName={autopf}\{#MyAppName}
UsePreviousAppDir=no
DisableProgramGroupPage=yes
LicenseFile=seadas_license.rtf
InfoBeforeFile=seadas_installer_welcome_message.rtf
; Uncomment the following line to run in non administrative install mode (install for current user only.)
;PrivilegesRequired=lowest
PrivilegesRequiredOverridesAllowed=commandline
OutputDir=out
OutputBaseFilename=seadas_{#MyAppVersion}_windows64_installer
SetupIconFile=seadas_installer_icon.ico
UninstallDisplayIcon={app}\{#ShortcutIconFileName2}
Compression=lzma
SolidCompression=yes
WizardStyle=modern
DisableWelcomePage=no
DisableDirPage=no

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
Source: "SeaDAS\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "*.ico"; DestDir: "{app}"
; NOTE: Don't use "Flags: ignoreversion" on any shared system files

[Icons]
Name: "{autoprograms}\{#MyAppName}"; Filename: "{app}\bin\{#MyAppExeName}"; HotKey: "ctrl+alt+s"; IconFilename: "{app}\{#ShortcutIconFileName1}"
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\bin\{#MyAppExeName}"; Tasks: desktopicon; IconFilename: "{app}\{#ShortcutIconFileName2}"

[InstallDelete]
; Installing over an older SeaDAS must not leave its modules behind: NetBeans
; would load whatever jars are left in these clusters.
Type: filesandordirs; Name: "{app}\platform"
Type: filesandordirs; Name: "{app}\ide"
Type: filesandordirs; Name: "{app}\snap"
Type: filesandordirs; Name: "{app}\optical-toolbox"
Type: filesandordirs; Name: "{app}\seadas-toolbox"
Type: filesandordirs; Name: "{app}\{#JreDir}"

[Run]
; No nowait here: archiving must finish before SeaDAS starts and creates a new .seadas.
Filename: "{app}\bin\{#ArchiveDotSeaDASScriptName}"; Description: "Archive the .seadas directory."; Flags: postinstall skipifsilent runhidden
Filename: "{app}\bin\{#MyAppExeName}"; Description: "{cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}"; Flags: nowait postinstall skipifsilent

[Code]
// etc\seadas.conf and etc\snap.conf are staged with the line jdkhome=${jdkhome}
// (IzPack's placeholder).  Point it at the bundled JRE in the folder the user
// picked; that folder is only known at install time.
procedure SetJdkHome(const FileName: String);
var
  Contents: AnsiString;
  Text: String;
begin
  if not LoadStringFromFile(FileName, Contents) then
    RaiseException('Cannot read ' + FileName);
  Text := String(Contents);
  if StringChangeEx(Text, '${jdkhome}', '"' + ExpandConstant('{app}\{#JreDir}') + '"', True) = 0 then
    RaiseException('No jdkhome placeholder in ' + FileName);
  if not SaveStringToFile(FileName, AnsiString(Text), False) then
    RaiseException('Cannot write ' + FileName);
end;

procedure CurStepChanged(CurStep: TSetupStep);
begin
  if CurStep = ssPostInstall then
  begin
    SetJdkHome(ExpandConstant('{app}\etc\seadas.conf'));
    SetJdkHome(ExpandConstant('{app}\etc\snap.conf'));
  end;
end;
