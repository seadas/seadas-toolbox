@echo off
setlocal

set "SEADAS_DIR=%USERPROFILE%\.seadas"
set "SEADAS_AUXDATA_DIR=%SEADAS_DIR%\.auxdata"
set "SEADAS_ARCHIVE=%USERPROFILE%\.seadas_archive"
set "SEADAS_ARCHIVE_SEADAS_DIR=%SEADAS_ARCHIVE%\.seadas"

echo Assessing any previous SeaDAS configuration directories ...
echo.

cd /d "%USERPROFILE%" || exit /b 1

REM Create archive directory if needed
if not exist "%SEADAS_ARCHIVE%" (
    mkdir "%SEADAS_ARCHIVE%"
)

REM Check whether .seadas exists
if exist "%SEADAS_DIR%" (
    REM If .auxdata exists, treat this as a current SeaDAS configuration and retain it
    if exist "%SEADAS_AUXDATA_DIR%" (
        echo Retaining existing SeaDAS configuration directory "%SEADAS_DIR%"
    ) else (
        REM Archive old or incompatible .seadas directory
        if exist "%SEADAS_ARCHIVE_SEADAS_DIR%" (
            rd /s /q "%SEADAS_ARCHIVE_SEADAS_DIR%"
        )

        move /y "%SEADAS_DIR%" "%SEADAS_ARCHIVE_SEADAS_DIR%" >nul 2>&1
        echo Existing "%SEADAS_DIR%" has been archived in "%SEADAS_ARCHIVE_SEADAS_DIR%"
    )
)

REM Clean stale UI/runtime caches from retained .seadas
if exist "%SEADAS_DIR%\config\Preferences\org\netbeans" (
    rd /s /q "%SEADAS_DIR%\config\Preferences\org\netbeans"
)

if exist "%SEADAS_DIR%\config\Toolbars" (
    rd /s /q "%SEADAS_DIR%\config\Toolbars"
)

if exist "%SEADAS_DIR%\config\Windows2Local" (
    rd /s /q "%SEADAS_DIR%\config\Windows2Local"
)

if exist "%SEADAS_DIR%\var" (
    rd /s /q "%SEADAS_DIR%\var"
)

endlocal
exit /b 0