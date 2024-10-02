@echo off
setlocal

REM Define paths
set SEADAS9_DIR=%USERPROFILE%\.seadas9
set SEADAS8_DIR=%USERPROFILE%\.seadas8
set SEADAS_ARCHIVE=%USERPROFILE%\.seadas_archive

REM Function equivalents using labels

:GET_RECORDS
echo Creating clean %SEADAS9_DIR% configuration directory ...
goto :eof

:ARCHIVE_SEADAS9_MSG
echo Previous %SEADAS9_DIR% has been archived in %SEADAS_ARCHIVE%\.seadas9
goto :eof

:ARCHIVE_SEADAS8_MSG
echo Previous %SEADAS8_DIR% has been archived in %SEADAS_ARCHIVE%\.seadas8
goto :eof

:RETAIN_SEADAS9_MSG
echo Retained user custom files in %SEADAS9_DIR%
goto :eof

:TRANSFER_SEADAS8_MSG
echo Transferred user custom files from %SEADAS8_DIR% to %SEADAS9_DIR%
goto :eof

REM Call the GET_RECORDS label to print the message
call :GET_RECORDS

echo.

cd %USERPROFILE%

REM Check if .seadas9 directory exists
if exist "%SEADAS9_DIR%" (

    REM Check if .seadas_archive exists
    if exist "%SEADAS_ARCHIVE%" (
        rd /s /q "%SEADAS_ARCHIVE%\.seadas9"
    ) else (
        mkdir "%SEADAS_ARCHIVE%"
    )

    move /y "%SEADAS9_DIR%" "%SEADAS_ARCHIVE%\.seadas9" >nul 2>&1
    call :ARCHIVE_SEADAS9_MSG

    mkdir "%SEADAS9_DIR%"
    mkdir "%SEADAS9_DIR%\auxdata"

    copy /y "%SEADAS_ARCHIVE%\.seadas9\auxdata\color_palettes" "%SEADAS9_DIR%\auxdata" >nul 2>&1
    copy /y "%SEADAS_ARCHIVE%\.seadas9\auxdata\color_schemes" "%SEADAS9_DIR%\auxdata" >nul 2>&1
    copy /y "%SEADAS_ARCHIVE%\.seadas9\auxdata\rgb_profiles" "%SEADAS9_DIR%\auxdata" >nul 2>&1
    copy /y "%SEADAS_ARCHIVE%\.seadas9\graphs" "%SEADAS9_DIR%" >nul 2>&1
    call :RETAIN_SEADAS9_MSG
)

REM Check if .seadas8 directory exists
if exist "%SEADAS8_DIR%" (

    REM Check if .seadas_archive exists
    if exist "%SEADAS_ARCHIVE%" (
        rd /s /q "%SEADAS_ARCHIVE%\.seadas8"
    ) else (
        mkdir "%SEADAS_ARCHIVE%"
    )

    move /y "%SEADAS8_DIR%" "%SEADAS_ARCHIVE%\.seadas8" >nul 2>&1
    call :ARCHIVE_SEADAS8_MSG

    REM Transfer archived .seadas8 files to .seadas9 only if .seadas9 doesn't exist
    if not exist "%SEADAS9_DIR%" (
        mkdir "%SEADAS9_DIR%"
        mkdir "%SEADAS9_DIR%\auxdata"

        copy /y "%SEADAS_ARCHIVE%\.seadas8\auxdata\color_palettes" "%SEADAS9_DIR%\auxdata" >nul 2>&1
        copy /y "%SEADAS_ARCHIVE%\.seadas8\auxdata\color_schemes" "%SEADAS9_DIR%\auxdata" >nul 2>&1
        copy /y "%SEADAS_ARCHIVE%\.seadas8\auxdata\rgb_profiles" "%SEADAS9_DIR%\auxdata" >nul 2>&1
        copy /y "%SEADAS_ARCHIVE%\.seadas8\graphs" "%SEADAS9_DIR%" >nul 2>&1
        call :TRANSFER_SEADAS8_MSG
    )
)

endlocal
