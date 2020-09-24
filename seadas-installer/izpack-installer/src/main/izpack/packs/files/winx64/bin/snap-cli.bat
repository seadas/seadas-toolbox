@echo off

echo.
@echo Welcome to the SNAP command-line interface.
@echo The following command-line tools are available:
@echo   gpt          - Graph Processing Tool
@echo   pconvert     - Data product conversion and quicklook generation
@echo   snap64       - SNAP Desktop launcher
@echo   snappy-conf  - Configuration tool for the SNAP-Python interface
@echo Typing the name of each tool will output its usage information.
echo.

cd /d %~dp0

prompt $G$S
