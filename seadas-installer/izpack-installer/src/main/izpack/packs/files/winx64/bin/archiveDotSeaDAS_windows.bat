@echo off
setlocal

REM Function equivalents using labels

:GET_RECORDS
echo Creating clean %USERPROFILE%\.seadas9 configuration directory ...
goto :eof

:ARCHIVE_SEADAS9_MSG
echo Previous %USERPROFILE%\.seadas9 has been archived in %USERPROFILE%\.seadas_archive\.seadas9
goto :eof

:ARCHIVE_SEADAS8_MSG
echo Previous %USERPROFILE%\.seadas8 has been archived in %USERPROFILE%\.seadas_archive\.seadas8
goto :eof

:RETAIN_SEADAS9_MSG
echo Retained user custom files in %USERPROFILE%\.seadas9
goto :eof

:TRANSFER_SEADAS8_MSG
echo Transferred user custom files from %USERPROFILE%\.seadas8 to %USERPROFILE%\.seadas9
goto :eof

REM Call the GET_RECORDS label to print the message
call :GET_RECORDS

echo.

cd %USERPROFILE%

REM Check if .seadas9 directory exists
if exist ".seadas9" (

    REM Check if .seadas_archive exists
    if exist ".seadas_archive" (
        rd /s /q ".seadas_archive\.seadas9"
    ) else (
        mkdir ".seadas_archive"
    )

    move /y ".seadas9" ".seadas_archive\.seadas9" >nul 2>&1
    call :ARCHIVE_SEADAS9_MSG

    mkdir ".seadas9"
    mkdir ".seadas9\auxdata"

    copy /y ".seadas_archive\.seadas9\auxdata\color_palettes" ".seadas9\auxdata" >nul 2>&1
    copy /y ".seadas_archive\.seadas9\auxdata\color_schemes" ".seadas9\auxdata" >nul 2>&1
    copy /y ".seadas_archive\.seadas9\auxdata\rgb_profiles" ".seadas9\auxdata" >nul 2>&1
    copy /y ".seadas_archive\.seadas9\graphs" ".seadas9" >nul 2>&1
    call :RETAIN_SEADAS9_MSG
)

REM Check if .seadas8 directory exists
if exist ".seadas8" (

    REM Check if .seadas_archive exists
    if exist ".seadas_archive" (
        rd /s /q ".seadas_archive\.seadas8"
    ) else (
        mkdir ".seadas_archive"
    )

    move /y ".seadas8" ".seadas_archive\.seadas8" >nul 2>&1
    call :ARCHIVE_SEADAS8_MSG

    REM Transfer archived .seadas8 files to .seadas9 only if .seadas9 doesn't exist
    if not exist ".seadas9" (
        mkdir ".seadas9"
        mkdir ".seadas9\auxdata"

        copy /y ".seadas_archive\.seadas8\auxdata\color_palettes" ".seadas9\auxdata" >nul 2>&1
        copy /y ".seadas_archive\.seadas8\auxdata\color_schemes" ".seadas9\auxdata" >nul 2>&1
        copy /y ".seadas_archive\.seadas8\auxdata\rgb_profiles" ".seadas9\auxdata" >nul 2>&1
        copy /y ".seadas_archive\.seadas8\graphs" ".seadas9" >nul 2>&1
        call :TRANSFER_SEADAS8_MSG
    )
)

endlocal
