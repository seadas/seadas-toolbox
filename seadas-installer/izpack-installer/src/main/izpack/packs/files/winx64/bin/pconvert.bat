@echo off
setlocal EnableDelayedExpansion
SET CURRENT_DIR=%~dp0
set SEADAS_HOME=%CURRENT_DIR:~0,-5%
set LOCAL_CLASSPATH=""
set JARS=""
set EXCLUDED1=%SEADAS_HOME%\snap\modules\org-esa-snap-snap-worldwind.jar
set EXCLUDED2=%SEADAS_HOME%\snap\modules\org-esa-snap-snap-python.jar

for %%j in (%SEADAS_HOME%\snap\modules\*.jar) do call :add_jar %%j

set LOCAL_CLASSPATH=%LOCAL_CLASSPATH%;%SEADAS_HOME%\snap\modules;%SEADAS_HOME%\snap\modules\*;
set LOCAL_CLASSPATH=%LOCAL_CLASSPATH%;%SEADAS_HOME%\optical-toolbox\modules;%SEADAS_HOME%\optical-toolbox\modules\*;
set LOCAL_CLASSPATH=%LOCAL_CLASSPATH%;%SEADAS_HOME%\seadas-toolbox\modules;%SEADAS_HOME%\seadas-toolbox\modules\*;
set LOCAL_CLASSPATH=%LOCAL_CLASSPATH%;%SEADAS_HOME%\snap\modules\ext\ncsa.hdf.lib-hdf\ncsa-hdf\jhdf5.jar
set LIBRARY_PATH=%SEADAS_HOME%\snap\modules\lib\amd64


"%SEADAS_HOME%\jdk-11.0.19+7-jre\bin\java.exe" ^
	-cp "%CLASSPATH%;%LOCAL_CLASSPATH%" ^
	-Djava.library.path="%LIBRARY_PATH%" ^
    -Xmx1024M ^
    "-Dsnap.mainClass=org.esa.snap.pconvert.PConvertMain" ^
    "-Dseadas.home=%SEADAS_HOME%" ^
	"-Dexe4j.moduleName=%SEADAS_HOME%\bin\pconvert" ^
	"-Djava.awt.headless=true" ^
    org.esa.snap.runtime.Launcher %*
endlocal
exit /B %ERRORLEVEL%

:add_jar
if !%EXCLUDED2%! neq !%1! set JARS=%JARS%;%1
exit /b
