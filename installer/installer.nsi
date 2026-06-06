; NSIS installer for World Downloader Manager (WinUI 3, unpackaged self-contained)
; Built in CI; see .github/workflows/desktop-release.yml

Unicode true
!include "MUI2.nsh"

!ifndef APP_VERSION
  !define APP_VERSION "1.0.0"
!endif
!ifndef SRC_DIR
  !define SRC_DIR "..\desktop\bin\Release\net8.0-windows10.0.19041.0\win-x64\publish"
!endif
!ifndef OUT_FILE
  !define OUT_FILE "WorldDownloaderManager-Setup.exe"
!endif

!define APP_NAME    "World Downloader Manager"
!define APP_EXE     "WorldDownloaderManager.exe"
!define PUBLISHER   "cafepromenade"
!define UNINST_KEY  "Software\Microsoft\Windows\CurrentVersion\Uninstall\WorldDownloaderManager"

Name "${APP_NAME}"
OutFile "${OUT_FILE}"
InstallDir "$PROGRAMFILES64\${APP_NAME}"
InstallDirRegKey HKLM "Software\WorldDownloaderManager" "InstallDir"
RequestExecutionLevel admin
ShowInstDetails show
ShowUninstDetails show

VIProductVersion "${APP_VERSION}.0"
VIAddVersionKey "ProductName" "${APP_NAME}"
VIAddVersionKey "CompanyName" "${PUBLISHER}"
VIAddVersionKey "FileVersion" "${APP_VERSION}"
VIAddVersionKey "FileDescription" "${APP_NAME} installer"
VIAddVersionKey "LegalCopyright" "${PUBLISHER}"

!define MUI_ABORTWARNING
!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!define MUI_FINISHPAGE_RUN "$INSTDIR\${APP_EXE}"
!define MUI_FINISHPAGE_RUN_TEXT "Launch ${APP_NAME}"
!insertmacro MUI_PAGE_FINISH

!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

!insertmacro MUI_LANGUAGE "English"

Section "Install"
  SetOutPath "$INSTDIR"
  File /r "${SRC_DIR}\*.*"

  WriteRegStr HKLM "Software\WorldDownloaderManager" "InstallDir" "$INSTDIR"

  CreateDirectory "$SMPROGRAMS\${APP_NAME}"
  CreateShortcut "$SMPROGRAMS\${APP_NAME}\${APP_NAME}.lnk" "$INSTDIR\${APP_EXE}"
  CreateShortcut "$DESKTOP\${APP_NAME}.lnk" "$INSTDIR\${APP_EXE}"

  WriteUninstaller "$INSTDIR\Uninstall.exe"

  WriteRegStr   HKLM "${UNINST_KEY}" "DisplayName"     "${APP_NAME}"
  WriteRegStr   HKLM "${UNINST_KEY}" "DisplayVersion"  "${APP_VERSION}"
  WriteRegStr   HKLM "${UNINST_KEY}" "Publisher"       "${PUBLISHER}"
  WriteRegStr   HKLM "${UNINST_KEY}" "DisplayIcon"     "$INSTDIR\${APP_EXE}"
  WriteRegStr   HKLM "${UNINST_KEY}" "UninstallString" "$INSTDIR\Uninstall.exe"
  WriteRegStr   HKLM "${UNINST_KEY}" "InstallLocation" "$INSTDIR"
  WriteRegDWORD HKLM "${UNINST_KEY}" "NoModify" 1
  WriteRegDWORD HKLM "${UNINST_KEY}" "NoRepair" 1
SectionEnd

Section "Uninstall"
  Delete "$SMPROGRAMS\${APP_NAME}\${APP_NAME}.lnk"
  RMDir  "$SMPROGRAMS\${APP_NAME}"
  Delete "$DESKTOP\${APP_NAME}.lnk"

  RMDir /r "$INSTDIR"

  DeleteRegKey HKLM "${UNINST_KEY}"
  DeleteRegKey HKLM "Software\WorldDownloaderManager"
SectionEnd
