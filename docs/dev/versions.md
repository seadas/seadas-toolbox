SeaDAS Versioning Notes
--------------------------------


SeaDAS 8.0.0
SeaDAS Toolbox 1.0.0

seadas-toolbox: /origin/master


s3tbx: tag=8.0.0
cd s3tbx
git checkout -b 8.0.0 8.0.0


snap-engine: branch = SEADAS-8.0.0-SNAP-8.0.1
cd snap-engine
git checkout --track origin/SEADAS-8.0.0-SNAP-8.0.1


snap-desktop: branch = SEADAS-8.0.0-SNAP-8.0.1
cd snap-desktop
git checkout --track origin/SEADAS-8.0.0-SNAP-8.0.1
