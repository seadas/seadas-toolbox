#!/bin/bash
# Starts the SeaDAS OCSSW server. Override the heap with, e.g.,
#   docker run -e OCSSW_SERVER_JAVA_OPTS=-Xmx8G ...
# (this only limits the Java server; OCSSW programs such as l2gen run as separate processes).

cd /root || exit 1
exec java ${OCSSW_SERVER_JAVA_OPTS:--Xmx4G} -jar seadas-ocsswserver.jar ocsswserver.config
