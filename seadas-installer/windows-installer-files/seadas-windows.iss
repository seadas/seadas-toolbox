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
;   NoJre         only for 'mvn package -P win,nojre': SeaDAS\ has no JRE, and
;                 the installer asks for a Java 21+ already on the machine
;                 (preset from /JAVAHOME=<dir>, JAVA_HOME or the registry)
#include "build.iss"

#ifdef NoJre
  #define OutputSuffix "_nojre"
#else
  #define OutputSuffix ""
#endif

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
OutputBaseFilename=seadas_{#MyAppVersion}_windows64{#OutputSuffix}_installer
SetupIconFile=seadas_installer_icon.ico
UninstallDisplayIcon={app}\{#ShortcutIconFileName2}
Compression=lzma
SolidCompression=yes
WizardStyle=modern
DisableWelcomePage=no
DisableDirPage=no
; SeaDAS and its JRE are 64-bit: install under Program Files, not Program Files (x86)
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible

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
#ifdef NoJre
var
  JavaPage: TInputDirWizardPage;

// Major version of the Java in Dir, from its 'release' file
// (JAVA_VERSION="21.0.8"), or 0 if Dir holds no Java.
function JavaMajor(Dir: String): Integer;
var
  Lines: TArrayOfString;
  Line: String;
  I, P: Integer;
begin
  Result := 0;
  Dir := AddBackslash(Dir);
  if not FileExists(Dir + 'bin\java.exe') then
    Exit;
  if not LoadStringsFromFile(Dir + 'release', Lines) then
    Exit;
  for I := 0 to GetArrayLength(Lines) - 1 do
  begin
    Line := Trim(Lines[I]);
    if Pos('JAVA_VERSION="', Line) = 1 then
    begin
      P := Length('JAVA_VERSION="') + 1;
      while (P <= Length(Line)) and (Line[P] >= '0') and (Line[P] <= '9') do
      begin
        Result := Result * 10 + Ord(Line[P]) - Ord('0');
        P := P + 1;
      end;
      Exit;
    end;
  end;
end;

function IsJava21(Dir: String): Boolean;
begin
  Result := (Dir <> '') and (JavaMajor(Dir) >= 21);
end;

// First Java 21+ among JAVA_HOME and the registry entries of the Oracle and
// Eclipse Adoptium (Temurin) installers, or '' if there is none.
function FindJava: String;
var
  Keys: TArrayOfString;
  Version, Dir: String;
  I, K: Integer;
begin
  Result := GetEnv('JAVA_HOME');
  if IsJava21(Result) then
    Exit;
  if RegQueryStringValue(HKLM, 'SOFTWARE\JavaSoft\JDK', 'CurrentVersion', Version) and
     RegQueryStringValue(HKLM, 'SOFTWARE\JavaSoft\JDK\' + Version, 'JavaHome', Result) and
     IsJava21(Result) then
    Exit;
  for K := 0 to 1 do
  begin
    if K = 0 then
      Dir := 'SOFTWARE\Eclipse Adoptium\JDK'
    else
      Dir := 'SOFTWARE\Eclipse Adoptium\JRE';
    if RegGetSubkeyNames(HKLM, Dir, Keys) then
      for I := GetArrayLength(Keys) - 1 downto 0 do
        if RegQueryStringValue(HKLM, Dir + '\' + Keys[I] + '\hotspot\MSI', 'Path', Result) and
           IsJava21(Result) then
          Exit;
  end;
  Result := '';
end;

procedure InitializeWizard;
var
  Dir: String;
begin
  JavaPage := CreateInputDirPage(wpSelectDir,
    'Select Java', 'Which Java should SeaDAS use?',
    'This installer does not include Java. SeaDAS needs Java 21 or newer (a JDK or a JRE) ' +
    'already installed on this computer, for example from https://adoptium.net/.' + #13#10#13#10 +
    'Select the Java folder, the one that contains bin\java.exe, then click Next.',
    False, '');
  JavaPage.Add('');
  Dir := ExpandConstant('{param:JAVAHOME|}');
  if Dir = '' then
    Dir := FindJava;
  JavaPage.Values[0] := Dir;
end;

function JavaError: String;
begin
  Result := '';
  if not IsJava21(JavaPage.Values[0]) then
    Result := 'No Java 21 or newer was found in "' + JavaPage.Values[0] + '".' + #13#10 +
              'Select the folder that contains bin\java.exe of a Java 21 or newer.';
end;

function NextButtonClick(CurPageID: Integer): Boolean;
begin
  Result := True;
  if (CurPageID = JavaPage.ID) and (JavaError <> '') then
  begin
    MsgBox(JavaError, mbError, MB_OK);
    Result := False;
  end;
end;

// Also stops a silent install (/SILENT, /VERYSILENT) that found no Java.
function PrepareToInstall(var NeedsRestart: Boolean): String;
begin
  Result := JavaError;
end;

function JdkHome: String;
begin
  Result := RemoveBackslashUnlessRoot(JavaPage.Values[0]);
end;
#else
function JdkHome: String;
begin
  Result := ExpandConstant('{app}\{#JreDir}');
end;
#endif

// etc\seadas.conf and etc\snap.conf are staged with the line jdkhome=${jdkhome}
// (IzPack's placeholder).  Fill it in with the bundled JRE in the folder the
// user picked, or with the Java they selected; both are only known at install
// time.
procedure SetJdkHome(const FileName: String);
var
  Contents: AnsiString;
  Text: String;
begin
  if not LoadStringFromFile(FileName, Contents) then
    RaiseException('Cannot read ' + FileName);
  Text := String(Contents);
  if StringChangeEx(Text, '${jdkhome}', '"' + JdkHome + '"', True) = 0 then
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
