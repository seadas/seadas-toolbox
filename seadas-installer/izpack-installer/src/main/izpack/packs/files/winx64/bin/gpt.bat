@echo off
setlocal EnableExtensions EnableDelayedExpansion

rem --------------------------------------------------
rem Resolve SeaDAS home
rem --------------------------------------------------
set "CURRENT_DIR=%~dp0"
set "SEADAS_HOME=%CURRENT_DIR:~0,-5%"

rem Normalize trailing backslash if present
if "%SEADAS_HOME:~-1%"=="\" set "SEADAS_HOME=%SEADAS_HOME:~0,-1%"

rem --------------------------------------------------
rem Choose Java
rem --------------------------------------------------
set "JAVA_EXE="

if exist "%SEADAS_HOME%\jdk-21.0.8+9-jre\bin\java.exe" (
    set "JAVA_EXE=%SEADAS_HOME%\jdk-21.0.8+9-jre\bin\java.exe"
) else if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" (
    set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
) else (
    for %%I in (java.exe) do set "JAVA_EXE=%%~$PATH:I"
)

if not defined JAVA_EXE (
    echo No suitable Java executable found.
    echo Set JAVA_HOME or bundle a JRE with SeaDAS.
    exit /b 83
)

rem --------------------------------------------------
rem Classpath
rem --------------------------------------------------
set "LOCAL_CLASSPATH="

call :append_cp "%SEADAS_HOME%\platform\lib"
call :append_cp "%SEADAS_HOME%\platform\lib\*"

call :append_cp "%SEADAS_HOME%\platform\modules"
call :append_cp "%SEADAS_HOME%\platform\modules\*"

call :append_cp "%SEADAS_HOME%\ide\modules"
call :append_cp "%SEADAS_HOME%\ide\modules\*"

call :append_cp "%SEADAS_HOME%\snap\modules"
call :append_cp "%SEADAS_HOME%\snap\modules\*"

call :append_cp "%SEADAS_HOME%\optical-toolbox\modules"
call :append_cp "%SEADAS_HOME%\optical-toolbox\modules\*"

call :append_cp "%SEADAS_HOME%\seadas-toolbox\modules"
call :append_cp "%SEADAS_HOME%\seadas-toolbox\modules\*"

if exist "%SEADAS_HOME%\snap\modules\ext\ncsa.hdf.lib-hdf\ncsa-hdf\jhdf5.jar" (
    call :append_cp "%SEADAS_HOME%\snap\modules\ext\ncsa.hdf.lib-hdf\ncsa-hdf\jhdf5.jar"
)

if exist "%SEADAS_HOME%\snap\modules\ext\org.esa.snap.ceres-core\org-slf4j\slf4j-api.jar" (
    call :append_cp "%SEADAS_HOME%\snap\modules\ext\org.esa.snap.ceres-core\org-slf4j\slf4j-api.jar"
)

if exist "%SEADAS_HOME%\snap\modules\ext\org.esa.snap.snap-netcdf\org-slf4j\slf4j-simple.jar" (
    call :append_cp "%SEADAS_HOME%\snap\modules\ext\org.esa.snap.snap-netcdf\org-slf4j\slf4j-simple.jar"
)

rem --------------------------------------------------
rem Native libraries
rem --------------------------------------------------
set "LIBRARY_PATH=%SEADAS_HOME%\snap\modules\lib\amd64"

if exist "%LIBRARY_PATH%" (
    set "PATH=%LIBRARY_PATH%;%PATH%"
)

rem --------------------------------------------------
rem NetBeans user dir
rem --------------------------------------------------
set "NETBEANS_USER_DIR=%SEADAS_HOME%\var\netbeans-user"
if not exist "%NETBEANS_USER_DIR%" mkdir "%NETBEANS_USER_DIR%" >nul 2>nul

rem --------------------------------------------------
rem Read simple vmoptions
rem One VM parameter per line, ignore blank lines and comments
rem --------------------------------------------------
set "VMOPTIONS_ARGS="
set "VMOPTIONS_FILE=%CURRENT_DIR%gpt.vmoptions"

if exist "%VMOPTIONS_FILE%" (
    for /f "usebackq delims=" %%L in ("%VMOPTIONS_FILE%") do (
        set "LINE=%%L"
        if defined LINE (
            if not "!LINE:~0,1!"=="#" (
                set "VMOPTIONS_ARGS=!VMOPTIONS_ARGS! !LINE!"
            )
        )
    )
)

echo ----------------------------------------
echo GPT Launcher Debug Info
echo SEADAS_HOME: %SEADAS_HOME%
echo JAVA_EXE: %JAVA_EXE%
echo VMOPTIONS_FILE: %VMOPTIONS_FILE%
"%JAVA_EXE%" -version 2>&1
echo ----------------------------------------

rem --------------------------------------------------
rem Launch GPT
rem --------------------------------------------------
"%JAVA_EXE%" %VMOPTIONS_ARGS% ^
  -cp "%CLASSPATH%;%LOCAL_CLASSPATH%" ^
  "-Djava.library.path=%LIBRARY_PATH%" ^
  "-Dnetbeans.user=%NETBEANS_USER_DIR%" ^
  "-Dsnap.mainClass=org.esa.snap.core.gpf.main.GPT" ^
  "-Dsnap.home=%SEADAS_HOME%" ^
  "-Dseadas.home=%SEADAS_HOME%" ^
  "-Dexe4j.moduleName=%SEADAS_HOME%\bin\gpt" ^
  "-Djava.awt.headless=true" ^
  org.esa.snap.runtime.Launcher %*

set "EXIT_CODE=%ERRORLEVEL%"
endlocal & exit /b %EXIT_CODE%

:append_cp
if "%~1"=="" exit /b 0
if defined LOCAL_CLASSPATH (
    set "LOCAL_CLASSPATH=%LOCAL_CLASSPATH%;%~1"
) else (
    set "LOCAL_CLASSPATH=%~1"
)
exit /b 0
