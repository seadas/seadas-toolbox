#!/bin/bash
# Builds the ocssw-run Docker image from this checkout:
#   1. builds seadas-ocsswrest/target/seadas-ocsswserver.jar with Maven,
#   2. stages the Docker build context in seadas-ocsswrest/target/docker,
#   3. runs docker build.
#
# Usage: build.sh [image-tag]        (default tag: seadas/ocssw-run:<seadas-ocsswrest version>)
#   SKIP_MAVEN=1 build.sh ...        reuse an existing target/seadas-ocsswserver.jar
set -euo pipefail

DOCKER_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODULE_DIR="$(dirname "$DOCKER_DIR")"
STAGE_DIR="$MODULE_DIR/target/docker"

# The module's own <version>, i.e. the first one after its <artifactId> (the parent's comes before it).
VERSION="$(awk '/<artifactId>seadas-ocsswrest<\/artifactId>/{f=1} f && /<version>/{match($0, /<version>[^<]*<\/version>/); print substr($0, RSTART + 9, RLENGTH - 19); exit}' "$MODULE_DIR/pom.xml")"
IMAGE="${1:-seadas/ocssw-run:$VERSION}"

if [ "${SKIP_MAVEN:-0}" != "1" ]; then
    mvn -q -f "$MODULE_DIR/pom.xml" package -DskipTests
fi

JAR="$MODULE_DIR/target/seadas-ocsswserver.jar"
[ -f "$JAR" ] || { echo "Missing $JAR" >&2; exit 1; }

rm -rf "$STAGE_DIR"
mkdir -p "$STAGE_DIR"
cp "$DOCKER_DIR/Dockerfile" "$DOCKER_DIR/entrypoint.sh" "$DOCKER_DIR/ocsswserver.config" "$JAR" "$STAGE_DIR/"

docker build --build-arg VERSION="$VERSION" -t "$IMAGE" "$STAGE_DIR"
echo "Built $IMAGE"
