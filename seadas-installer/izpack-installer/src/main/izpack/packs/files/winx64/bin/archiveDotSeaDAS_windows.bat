@echo off
setlocal

REM Define paths with explicit quotes for safety
set "SEADAS_DIR=%USERPROFILE%\.seadas"
set "SEADAS_AUXDATA_DIR_=%SEADAS_DIR%\.auxdata"
set "SEADAS8_DIR=%USERPROFILE%\.seadas8"
set "SEADAS9_DIR=%USERPROFILE%\.seadas9"
set "SEADAS_ARCHIVE=%USERPROFILE%\.seadas_archive"
set "SEADAS_ARCHIVE_SEADAS7_DIR=%SEADAS_ARCHIVE%\.seadas7"
set "SEADAS_ARCHIVE_SEADAS8_DIR=%SEADAS_ARCHIVE%\.seadas8"
set "SEADAS_ARCHIVE_SEADAS9_DIR=%SEADAS_ARCHIVE%\.seadas9"




REM Directly perform the task that was in the GET_RECORDS label
echo Assessing any previous SeaDAS version configuration directories ...

echo.

cd /d "%USERPROFILE%" || exit /b


REM Check if .seadas_archive exists
if exist "%SEADAS_ARCHIVE%" (
    REM Do nothing
) else (
    REM Creating .seadas_archive directory
    mkdir "%SEADAS_ARCHIVE%"
)



REM Check if .seadas9 directory exists
if exist "%SEADAS_DIR%" (
    REM Found .seadas directory.
    if exist "%SEADAS_AUXDATA_DIR_%" (
        REM SeaDAS 10 or later has been previously run, so leave in tact and archive any other versions
        echo Retaining previous SeaDAS version configuration directory %SEADAS_DIR%

        if exist "%SEADAS9_DIR%" ()
            if exist "%SEADAS_ARCHIVE_SEADAS9_DIR%" (
                REM Removing existing .seadas9 archive
                rd /s /q "%SEADAS_ARCHIVE_SEADAS9_DIR%"
            )

            if exist "%SEADAS9_DIR%\seadas-bathymetry-operator" ()
                rd /s /q "%SEADAS8_DIR%\seadas-bathymetry-operator"
            )

            if exist "%SEADAS9_DIR%\seadas-watermask-operator" ()
                rd /s /q "%SEADAS8_DIR%\seadas-watermask-operator"
            )

            move /y "%SEADAS9_DIR%" "%SEADAS_ARCHIVE_SEADAS9_DIR%" >nul 2>&1
            echo Previous %SEADAS9_DIR% has been archived in %SEADAS_ARCHIVE_SEADAS9_DIR%
        )

    ) else (
        if exist "%SEADAS_ARCHIVE_SEADAS7_DIR%" (
            REM Removing existing .seadas7 archive
            rd /s /q "%SEADAS_ARCHIVE_SEADAS7_DIR%"
        )
        move /y "%SEADAS_DIR%" "%SEADAS_ARCHIVE_SEADAS7_DIR%" >nul 2>&1
        echo Existing %SEADAS_DIR% has been archived in %SEADAS_ARCHIVE_SEADAS7_DIR%


        if exist "%SEADAS_SEADAS9_DIR%" (
            move /y "%SEADAS9_DIR%" "%SEADAS_DIR%" >nul 2>&1
            echo Existing "%SEADAS_SEADAS9_DIR%" configuration directory has been moved to "%SEADAS_DIR%"
        )
    )

) else (
    REM # SeaDAS 10 or later not previously run, copy seadas9 if available

    if exist "%SEADAS_SEADAS9_DIR%" (
        move /y "%SEADAS9_DIR%" "%SEADAS_DIR%" >nul 2>&1
        echo Existing "%SEADAS_SEADAS9_DIR%" directory has been moved to "%SEADAS_DIR%"
    )
)


if exist "%SEADAS8_DIR%" ()
    if exist "%SEADAS_ARCHIVE_SEADAS8_DIR%" (
        REM Removing existing .seadas8 archive
        rd /s /q "%SEADAS_ARCHIVE_SEADAS8_DIR%"
    )

    if exist "%SEADAS8_DIR%\seadas-bathymetry-operator" ()
        rd /s /q "%SEADAS8_DIR%\seadas-bathymetry-operator"
    )

    if exist "%SEADAS8_DIR%\seadas-watermask-operator" ()
        rd /s /q "%SEADAS8_DIR%\seadas-watermask-operator"
    )

    move /y "%SEADAS8_DIR%" "%SEADAS_ARCHIVE_SEADAS8_DIR%" >nul 2>&1
    echo Previous %SEADAS8_DIR% has been archived in %SEADAS_ARCHIVE_SEADAS8_DIR%
)


if exist "%SEADAS_DIR%\config\Preferences\org\netbeans" ()
    rd /s /q "%SEADAS_DIR%\config\Preferences\org\netbeans"
)

if exist "%SEADAS_DIR%\config\Toolbars" ()
    rd /s /q "%SEADAS_DIR%\config\Toolbars"
)

if exist "%SEADAS_DIR%\config\Windows2Local" ()
    rd /s /q "%SEADAS_DIR%\config\Windows2Local"
)

if exist "%SEADAS_DIR%\var" ()
    rd /s /q "%SEADAS_DIR%\var"
)



endlocal
