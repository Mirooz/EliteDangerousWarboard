#ifndef AppVersion
  #define AppVersion "1.2.0-SNAPSHOT"
#endif

[Setup]
AppName=Elite Warboard
AppVersion={#AppVersion}
DefaultDirName={localappdata}\EliteWarboard
DisableProgramGroupPage=yes
OutputBaseFilename=EliteWarboard-Setup
ArchitecturesInstallIn64BitMode=x64
Compression=lzma
SolidCompression=yes
OutputDir=executable
UninstallDisplayIcon={app}\Elite Warboard.exe
AppPublisher=Mirooz
[Files]
Source: "executable\Elite Warboard\*"; DestDir: "{app}"; Flags: recursesubdirs createallsubdirs

[Icons]
Name: "{autoprograms}\Elite Warboard"; Filename: "{app}\Elite Warboard.exe"
Name: "{userdesktop}\Elite Warboard"; Filename: "{app}\Elite Warboard.exe"

[Run]
Filename: "{app}\Elite Warboard.exe"; Description: "Launch Elite Warboard"; Flags: nowait postinstall skipifsilent
