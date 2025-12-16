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
Compression=lzma2/ultra64
SolidCompression=yes
OutputDir=executable
UninstallDisplayIcon={app}\Elite Warboard.exe
AppPublisher=Mirooz
WizardStyle=modern
[Files]
Source: "executable\Elite Warboard\*"; DestDir: "{app}"; Flags: recursesubdirs createallsubdirs

[Icons]
Name: "{autoprograms}\Elite Warboard"; Filename: "{app}\Elite Warboard.exe"
Name: "{userdesktop}\Elite Warboard"; Filename: "{app}\Elite Warboard.exe"

[Code]
const
  HWND_TOPMOST = -1;
  SWP_NOMOVE = $0002;
  SWP_NOSIZE = $0001;

function SetWindowPos(hWnd: LongInt; hWndInsertAfter: LongInt; X, Y, cx, cy: Integer; uFlags: LongInt): Boolean;
  external 'SetWindowPos@user32.dll stdcall';

function InitializeSetup(): Boolean;
var
  ResultCode: Integer;
begin
  Result := True;
  
  // Fermer l'application Elite Warboard si elle est en cours d'exécution
  // Utiliser taskkill pour forcer la fermeture
  if Exec('taskkill', '/F /IM "Elite Warboard.exe"', '', SW_HIDE, ewWaitUntilTerminated, ResultCode) then
  begin
    // Attendre un peu pour que le processus se termine
    Sleep(1000);
  end;
end;

procedure InitializeWizard;
begin
  // Forcer la fenêtre à être toujours au-dessus
  SetWindowPos(WizardForm.Handle, HWND_TOPMOST, 0, 0, 0, 0, SWP_NOMOVE or SWP_NOSIZE);
end;

[Run]
Filename: "{app}\Elite Warboard.exe"; Description: "Launch Elite Warboard"; Flags: nowait postinstall skipifsilent
