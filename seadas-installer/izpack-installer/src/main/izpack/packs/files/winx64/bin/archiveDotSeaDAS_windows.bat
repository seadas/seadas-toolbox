@echo off
setlocal

REM Define paths with explicit quotes for safety
set "SEADAS9_DIR=%USERPROFILE%\.seadas9"
set "SEADAS8_DIR=%USERPROFILE%\.seadas8"
set "SEADAS_ARCHIVE=%USERPROFILE%\.seadas_archive"

REM Directly perform the task that was in the GET_RECORDS label
echo Creating clean %SEADAS9_DIR% configuration directory ...

echo.

cd /d "%USERPROFILE%" || exit /b

REM Check if .seadas9 directory exists
if exist "%SEADAS9_DIR%" (
    echo Found .seadas9 directory.

    REM Check if .seadas_archive exists
    if exist "%SEADAS_ARCHIVE%" (
        echo Removing existing .seadas9 archive
        rd /s /q "%SEADAS_ARCHIVE%\.seadas9"
    ) else (
        echo Creating .seadas_archive directory
        mkdir "%SEADAS_ARCHIVE%"
    )

    echo Archiving .seadas9 directory
    move /y "%SEADAS9_DIR%" "%SEADAS_ARCHIVE%\.seadas9" >nul 2>&1
    echo Previous %SEADAS9_DIR% has been archived in %SEADAS_ARCHIVE%\.seadas9

    echo Creating new .seadas9 directory
    mkdir "%SEADAS9_DIR%"
    mkdir "%SEADAS9_DIR%\auxdata"

    echo Copying files from archive to new .seadas9
    copy /y "%SEADAS_ARCHIVE%\.seadas9\auxdata\color_palettes" "%SEADAS9_DIR%\auxdata" >nul 2>&1
    copy /y "%SEADAS_ARCHIVE%\.seadas9\auxdata\color_schemes" "%SEADAS9_DIR%\auxdata" >nul 2>&1
    copy /y "%SEADAS_ARCHIVE%\.seadas9\auxdata\rgb_profiles" "%SEADAS9_DIR%\auxdata" >nul 2>&1
    copy /y "%SEADAS_ARCHIVE%\.seadas9\auxdata\regions" "%SEADAS9_DIR%\auxdata" >nul 2>&1
    copy /y "%SEADAS_ARCHIVE%\.seadas9\graphs" "%SEADAS9_DIR%" >nul 2>&1
    echo Retained user custom files in %SEADAS9_DIR%
) else (
    echo No .seadas9 directory found to archive.
)

REM Check if .seadas8 directory exists
if exist "%SEADAS8_DIR%" (
    echo Found .seadas8 directory.

    REM Check if .seadas_archive exists
    if exist "%SEADAS_ARCHIVE%" (
        echo Removing existing .seadas8 archive
        rd /s /q "%SEADAS_ARCHIVE%\.seadas8"
    ) else (
        echo Creating .seadas_archive directory
        mkdir "%SEADAS_ARCHIVE%"
    )

    echo Archiving .seadas8 directory
    move /y "%SEADAS8_DIR%" "%SEADAS_ARCHIVE%\.seadas8" >nul 2>&1
    echo Previous %SEADAS8_DIR% has been archived in %SEADAS_ARCHIVE%\.seadas8

    REM Transfer archived .seadas8 files to .seadas9 only if .seadas9 doesn't exist
    if not exist "%SEADAS9_DIR%" (
        echo .seadas9 does not exist, creating it.
        mkdir "%SEADAS9_DIR%"
        mkdir "%SEADAS9_DIR%\auxdata"

        echo Transferring files from .seadas8 to .seadas9
        copy /y "%SEADAS_ARCHIVE%\.seadas8\auxdata\color_palettes" "%SEADAS9_DIR%\auxdata" >nul 2>&1
        copy /y "%SEADAS_ARCHIVE%\.seadas8\auxdata\color_schemes" "%SEADAS9_DIR%\auxdata" >nul 2>&1
        copy /y "%SEADAS_ARCHIVE%\.seadas8\auxdata\rgb_profiles" "%SEADAS9_DIR%\auxdata" >nul 2>&1
        copy /y "%SEADAS_ARCHIVE%\.seadas8\graphs" "%SEADAS9_DIR%" >nul 2>&1
        echo Transferred user custom files from %SEADAS8_DIR% to %SEADAS9_DIR%
    ) else (
        echo .seadas9 already exists, no transfer needed.
    )
) else (
    echo No .seadas8 directory found to archive.
)

endlocal
