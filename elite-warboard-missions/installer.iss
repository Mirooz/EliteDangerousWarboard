#ifndef AppVersion
  #define AppVersion "1.2.0-SNAPSHOT"
#endif

[Setup]
AppName=Elite Warboard
AppVersion={#AppVersion}
DefaultDirName={autopf}\Elite Warboard
DisableProgramGroupPage=yes
OutputBaseFilename=EliteWarboard-Setup
Compression=lzma
SolidCompression=yes
OutputDir=executable
UninstallDisplayIcon={app}\Elite Warboard.exe
AppPublisher=Mirooz
[Files]
Source: "executable\Elite Warboard\*"; DestDir: "{app}"; Flags: recursesubdirs

[Icons]
Name: "{autoprograms}\Elite Warboard"; Filename: "{app}\Elite Warboard.exe"
Name: "{userdesktop}\Elite Warboard"; Filename: "{app}\Elite Warboard.exe"

[Run]
Filename: "{app}\Elite Warboard.exe"; Description: "Launch Elite Warboard"; Flags: nowait postinstall skipifsilent
