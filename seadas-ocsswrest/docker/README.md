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

Publish with `docker push seadas/ocssw-run:<tag>`.

## Run

```bash
docker run -d --name ocssw \
  -p 6400:6400 -p 6402:6402 -p 6403:6403 \
  -v "$HOME/seadasClientServerShared:/root/seadasClientServerShared" \
  -v ocssw:/root/ocssw \
  seadas/ocssw-run:12.0.0
```

- **Shared directory.** The host side must be the directory set as *OCSSW Shared Dir*
  in SeaDAS (default `~/seadasClientServerShared`). SeaDAS copies input files into it,
  including the band files next to a Landsat `*_MTL.txt`, and OCSSW writes outputs there.
- **OCSSW.** `ocssw` above is a named volume, so OCSSW survives container restarts and
  image upgrades. Install OCSSW into it from SeaDAS (OCSSW Manager), or instead
  bind-mount an existing Linux OCSSW installation at `/root/ocssw`.
- **Earthdata Login.** Put a `.netrc` in the shared directory; the server copies it to
  `/root/.netrc` when it starts. Never bake credentials into the image. If there is
  none, the server logs a `NoSuchFileException` for it and carries on.
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
