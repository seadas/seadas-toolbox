# SeaDAS cloud access prototype

Throwaway prototypes from the Earthdata Cloud investigation (2026-10-26).
Write-up: [../INVESTIGATION.md](../INVESTIGATION.md)

They test reading PACE OCI NetCDF files over HTTPS byte-range requests with
SeaDAS's own netCDF-Java 5.3.3 jar, without downloading the whole file.

| File | What it tests |
| --- | --- |
| `EdlRaf.java` | netCDF-Java's stock `RemoteRandomAccessFile`. Its read cache overflows the stack in 5.3.3; with the cache off it is very slow. |
| `BlockRaf.java` | Custom `RandomAccessFile` with an aligned LRU block cache and parallel block prefetch. One file handle. |
| `ParRaf.java` | Same as `BlockRaf`, but several file handles share one cache and read tiles in parallel (what SNAP would do without `synchronized` reads). |
| `Inspect.java` | Opens a remote file and prints variable shapes and `_ChunkSizes`. Needs `BlockRaf`. |

## Run

Needs `~/.netrc` with Earthdata Login credentials.

```bash
JAR=~/.m2/repository/edu/ucar/netcdfAll/5.3.3/netcdfAll-5.3.3.jar
mkdir -p classes && javac -cp $JAR -d classes *.java

# Log in once and get the signed URL (valid for a limited time; don't share it)
FILE=PACE_OCI.20260901T235307.L2.OC_AOP.V3_2.nc
URL="https://obdaac-tea.earthdatacloud.nasa.gov/ob-cumulus-prod-public/$FILE"
curl -sS -L --netrc -c cj -b cj -r 0-0 -o /dev/null "$URL"   # login handshake, stores session cookie
SIG=$(curl -sS --netrc -c cj -b cj -r 0-0 -o /dev/null -w "%{redirect_url}" "$URL")
# SIG should start with https://...cloudfront.net/

java -cp classes:$JAR BlockRaf "$SIG" 262144       # block size in bytes
java -cp classes:$JAR ParRaf   "$SIG" 262144 7     # block size, threads
java -cp classes:$JAR EdlRaf   "$SIG" 65536 0      # buffer size, cache bytes (0 = off)
java -cp classes:$JAR Inspect  "$SIG"
```

The test reads in `main()` use variable names from the L2 AOP product
(`aot_865`, `Rrs`, lat/lon); change them for other products.
