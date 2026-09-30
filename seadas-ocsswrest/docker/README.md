# ocssw-run Docker image

The `seadas/ocssw-run` image runs the SeaDAS OCSSW server (`seadas-ocsswrest`) that
SeaDAS uses when **Tools > Options > SeaDAS > OCSSW location** is set to `docker`.
SeaDAS talks to it on `localhost` (client class `OCSSWVM`).

| File | Purpose |
| --- | --- |
| `Dockerfile` | Ubuntu 20.04 + OpenJDK 11 JRE + python3/python3-requests + the server jar. |
| `entrypoint.sh` | Starts the server (`java -jar seadas-ocsswserver.jar ocsswserver.config`). |
| `ocsswserver.config` | Server settings: OCSSW in `/root/ocssw`, working directory `/root/seadasClientServerShared` shared with the client, ports 6400/6402/6403. |
| `build.sh` | Builds the jar with Maven, stages the build context in `../target/docker`, runs `docker build`. |

## Build

```bash
seadas-ocsswrest/docker/build.sh                        # tags seadas/ocssw-run:<module version>, e.g. 12.0.0
seadas-ocsswrest/docker/build.sh seadas/ocssw-run:test  # any other tag
SKIP_MAVEN=1 seadas-ocsswrest/docker/build.sh           # reuse target/seadas-ocsswserver.jar
```

`seadas-ocsswrest` is not in the root reactor, so a root `mvn install` does not build
the jar; `build.sh` runs `mvn package` on the module itself (the parent POM must be
resolvable, i.e. run it from a seadas-toolbox checkout).

On Windows, run it from WSL or Git Bash. The repository's `.gitattributes` keeps
`build.sh`, `entrypoint.sh` and the rest of this directory LF-only; a checkout made
before that file existed may still have CRLF endings, which make bash fail with
`/bin/bash^M: bad interpreter` and would leave a broken `entrypoint.sh` in the image.
Refresh it with `rm seadas-ocsswrest/docker/* && git checkout -- seadas-ocsswrest/docker`.

Publish with `docker push seadas/ocssw-run:<tag>`.

## Run

SeaDAS starts the container itself when OCSSW location is `docker`: at startup, after
switching to docker, and whenever a processor is run while the server is not answering.
It runs `bin/start_ocssw_docker` (Linux, macOS) or `bin\start_ocssw_docker.ps1`
(Windows) from the SeaDAS installation; the sources are in
`seadas-installer/izpack-installer/src/main/izpack/packs/files/{unix,winx64}/bin/`.
The script:

1. checks that Docker is installed and running (it starts Docker Desktop on macOS and Windows),
2. creates the OCSSW directory (*OCSSW Docker Dir*, default `~/ocssw-docker`) and the
   shared directory (*OCSSW Shared Dir*, default `~/seadasClientServerShared`),
3. copies `~/.netrc` (on Windows `.netrc` or `_netrc` in `%USERPROFILE%`) into the shared directory,
4. pulls `seadas/ocssw-run:<SeaDAS version>` if it is missing,
5. starts the container `seadas-ocssw`, recreating it when the image, directories or ports
   changed (they are recorded in a container label), and restarting it when `.netrc` changed,
6. waits until the server answers.

SeaDAS asks for the image matching its own version (the `seadas.ocssw.dockerImage`
preference overrides it), so **every SeaDAS release needs `seadas/ocssw-run:<version>`
pushed to Docker Hub**. The script can also be run by hand; `--help` lists its options.
What it does amounts to:

```bash
docker run -d --name seadas-ocssw --platform linux/amd64 \
  -p 6400:6400 -p 6402:6402 -p 6403:6403 \
  -v "$HOME/seadasClientServerShared:/root/seadasClientServerShared" \
  -v "$HOME/ocssw-docker:/root/ocssw" \
  seadas/ocssw-run:12.0.0
```

On Windows (PowerShell):

```powershell
docker run -d --name seadas-ocssw --platform linux/amd64 `
  -p 6400:6400 -p 6402:6402 -p 6403:6403 `
  -v "$env:USERPROFILE\seadasClientServerShared:/root/seadasClientServerShared" `
  -v "$env:USERPROFILE\ocssw-docker:/root/ocssw" `
  seadas/ocssw-run:12.0.0
```

**Both `-v` mounts are required.** A container started without them, e.g. a plain
`docker run seadas/ocssw-run:12.0.0` or the *Run* button in Docker Desktop, still
starts and answers on port 6400, but Docker gives both directories empty anonymous
volumes instead: the server never sees the client's shared directory (so no input
files and no `.netrc`), and OCSSW installed into it is lost when the container is
removed. Remove such a container (`docker rm -f seadas-ocssw`) and let SeaDAS or the
script recreate it.

- **Shared directory.** The host side must be the directory set as *OCSSW Shared Dir*
  in SeaDAS. SeaDAS copies input files into it, including the band files next to a
  Landsat `*_MTL.txt`, and OCSSW writes outputs there.
- **OCSSW.** The container keeps OCSSW in the *OCSSW Docker Dir*, so it survives container
  restarts and image upgrades. Install OCSSW into it from SeaDAS (OCSSW Manager). It is
  deliberately not `~/ocssw`, the default for a local (macOS or Linux) OCSSW.
- **Earthdata Login.** The server copies `.netrc` from the shared directory to
  `/root/.netrc` when it starts. Never bake credentials into the image. If there is
  none, the server logs `No .netrc in the shared directory` and carries on; check
  `docker logs seadas-ocssw` for `Copied .netrc from the shared directory`.
- **Apple Silicon.** The image is linux/amd64 only and runs under emulation.
- **Memory.** The server heap defaults to `-Xmx4G`; change it with
  `-e OCSSW_SERVER_JAVA_OPTS=-Xmx8G`. OCSSW programs run as separate processes and are
  not limited by it.

## History

Before this directory existed, the image was built from a separate, uncommitted
checkout: `seadas/ocssw-run:base` had been assembled by hand (`docker commit`, with a
bundled JRE and a partial OCSSW T2022.5 in `/root/ocssw`), and `seadas/ocssw-run:2.0`
only added a server jar from 2024-03-31 and `entrypoint.sh` on top. This Dockerfile
reproduces that image from source, except that the JRE comes from apt and `/root/ocssw`
starts empty.
