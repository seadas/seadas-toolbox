# ocssw-run: the SeaDAS OCSSW server (seadas-ocsswrest) that SeaDAS talks to when
# Tools > Options > SeaDAS > OCSSW location is "docker".
#
# Build with build.sh, which builds the server jar and stages the build context.
# Replaces the hand-made seadas/ocssw-run:base image plus the thin Dockerfile on top of it.

# Same Ubuntu release as the seadas/ocssw-run:base image the 2.0 image was built on.
FROM ubuntu:20.04

# python3 and python3-requests are what OCSSW's install_ocssw and ocssw_runner need;
# the OCSSW binaries bring their own libraries.
RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
       openjdk-11-jre-headless \
       python3 \
       python3-requests \
       ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# The server resolves ocsswroot, the shared directory and its job database (ocssw.db)
# against the home directory and the working directory, so everything lives in /root.
# entrypoint.sh starts the server as root, or with HOST_UID/HOST_GID as that user,
# whose home is then /root too.
WORKDIR /root
COPY seadas-ocsswserver.jar ocsswserver.config entrypoint.sh /root/
RUN mkdir -p /root/ocssw /root/seadasClientServerShared

# seadasClientServerShared: bind-mount the client's shared directory here. SeaDAS copies
#   input files into it and reads outputs from it. A .netrc placed in it is copied to
#   /root/.netrc when the server starts.
# ocssw: OCSSW itself. Keep it in a named volume (SeaDAS's OCSSW installer fills it) or
#   bind-mount an existing Linux OCSSW installation.
VOLUME /root/seadasClientServerShared
VOLUME /root/ocssw

# 6400: REST API; 6402/6403: stdout/stderr of running OCSSW programs.
EXPOSE 6400 6402 6403

CMD ["/bin/bash", "/root/entrypoint.sh"]

ARG VERSION=unknown
LABEL "gov.nasa.gsfc.seadas"="NASA OEL SeaDAS"
LABEL version="${VERSION}"
LABEL description="SeaDAS OCSSW server (ocssw-run) image."
LABEL org.opencontainers.image.authors="aynur.abdurazik@nasa.gov"
