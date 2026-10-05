#!/bin/bash
# Starts the SeaDAS OCSSW server. Override the heap with, e.g.,
#   docker run -e OCSSW_SERVER_JAVA_OPTS=-Xmx8G ...
# (this only limits the Java server; OCSSW programs such as l2gen run as separate processes).
#
# With HOST_UID (and HOST_GID) set, the server and every OCSSW program it runs
# run as that user instead of root, so on Linux the files they write into the
# bind-mounted OCSSW and shared directories belong to the user on the host.
# start_ocssw_docker sets them on Linux; Docker Desktop (macOS, Windows) already
# maps file ownership to the user.

cd /root || exit 1
JAVA_OPTS=${OCSSW_SERVER_JAVA_OPTS:--Xmx4G}

if [ -n "${HOST_UID:-}" ] && [ "$HOST_UID" != 0 ] && [ "$(id -u)" = 0 ]; then
    HOST_GID=${HOST_GID:-$HOST_UID}
    # The ids need an account: Java takes user.home from it.
    getent group "$HOST_GID" >/dev/null || groupadd -o -g "$HOST_GID" ocssw
    getent passwd "$HOST_UID" >/dev/null || useradd -o -u "$HOST_UID" -g "$HOST_GID" -d /root -M -s /bin/bash ocssw
    # /root holds the server's own files (job database, intermediate files, .netrc).
    # Hand those over, and whatever earlier containers, running as root, left in
    # the mounted directories; files owned by anyone else are left alone.
    find /root -user 0 -exec chown -h "$HOST_UID:$HOST_GID" {} +
    exec setpriv --reuid="$HOST_UID" --regid="$HOST_GID" --clear-groups \
        env HOME=/root java $JAVA_OPTS -Duser.home=/root -jar seadas-ocsswserver.jar ocsswserver.config
fi

exec java $JAVA_OPTS -jar seadas-ocsswserver.jar ocsswserver.config
