# SeaDAS Cloud Data Access: Findings & Recommendation

Initial investigation, 2026-10-26 · Aynur Abdurazik

> This is the investigation as written before any implementation. Measurements are from the
> prototypes in [`prototype/`](prototype/). Phase 1 and Phase 2 have since been built; their
> results differ from the estimates here (for example, the first `Rrs` wavelength now loads in
> about 6 s instead of 43 s, using 32 KB blocks rather than 256 KB).

## Summary and recommendation

Recommendation: build an HTTPS range-reading path with Earthdata Login into SeaDAS's NetCDF opener, behind an "Open from Earthdata Cloud" action. Keep Harmony subsetting as the batch fallback. Prove it on PACE OCI L2 AOP. Opening the file remotely is technically sound and was measured end to end. The payoff depends on file size and chunk layout, so it needs a block cache and parallel reads to beat a plain download.

- **Feasible today:** a small prototype on SeaDAS's own netCDF-Java 5.3.3 jar opened a 345 MB PACE L2 file over HTTPS in 2.1 s (0.6 MB fetched). It loaded a 2-D band plus lat/lon in 8.2 s total, where today's full download takes 31 s.
- **Not free:** one wavelength of 3-D `Rrs` still pulls 117 MB (43 s with 7 parallel reads), because each chunk holds 40 wavelengths. Small products like L2 BGC (22.8 MB) download faster than any remote scheme.
- **Main blocker is in SNAP core:** `NetcdfFileOpener` has its HTTP branch commented out, and every reader requires a local `File`. The changes are small and localized; netCDF-Java already provides the extension points.
- **Much already exists:** SeaDAS already has Earthdata Login tokens, a CMR granule browser and a Harmony subset dialog (`seadas-earthdata-cloud-toolbox`).
- **Not viable now:** direct S3 works only inside AWS us-west-2. Cloud OPeNDAP is unavailable for PACE (no DMR++ files). There are no official Zarr references.
- **Harmony works but is slow:** an L2 AOP subset took 8 min 45 s and shrank the file 10×. Good for regional batch work, not for browsing.

## What SNAP and SeaDAS readers can do today

No SeaDAS or SNAP reader can open a remote file in place today. Every path ends in a local `java.io.File` and a disk-backed `RandomAccessFile`. The lower layers can stream, though. netCDF-Java already ships an HTTP range reader, and the readers already fetch data tile by tile.

**Blockers** (paths relative to the snap-14 workspace):

- **Readers accept only local files.** The PACE readers (`L1BPaceProductReaderPlugIn`, `L1CPaceProductReaderPlugIn`, `L2ProductReaderPlugIn`, `L3ProductReaderPlugIn` in `optical-toolbox/opttbx-seadas-reader`) take `String`/`File` input. `SeadasHelper.getInputFile` turns a string into `new File(...)`, and `checkInputObject` rejects anything that fails `isFile()`.
- **SNAP's NetCDF opener disables HTTP.** `snap-engine/snap-netcdf/.../NetcdfFileOpener.java` has its `HTTPRandomAccessFile` branch commented out ("Currently those protocols are not supported by BEAM", lines 258–271). It always builds a local `ucar.unidata.io.RandomAccessFile`. Its format sniffing also needs `Files.exists` (`ProductUtils.getProductInputStream`).
- **SNAP's virtual file system downloads the whole file.** `snap-virtual-file-system` has HTTP and S3 providers with range reads. But `FileUtils.getCachedFilePath` (snap-core, lines 592–618) copies the whole file to a local cache before netCDF opens it. Its HTTP provider supports only Basic auth, and its S3 provider cannot send the session token that Earthdata's temporary keys need.
- **`ProductIO.readProduct(String)` calls `new File(path)`.** No reader plugin declares `URL` or `URI` input.

**Building blocks already present:**

- **netCDF-Java 5.3.3 (`netcdfAll`) is bundled.** It includes `HTTPRandomAccessFile` and the DAP2 client, but not `cdm-s3`. HDF5 is read by the pure-Java `H5iosp`, so no native HDF5 build with the ROS3 driver is needed.
- **Band data is read lazily, one tile at a time.** `SeadasFileReader.readBandData` reads `Variable.read(section)` per tile. A `ProductCache` sizes tiles from `_ChunkSizes`, so tiles already line up with HDF5 chunks.
- **SeaDAS already has an Earthdata cloud toolbox** (`seadas-toolbox/seadas-earthdata-cloud-toolbox`):
  - Earthdata Login bearer tokens from `~/.netrc` (`WebPageFetcherWithJWT`).
  - A CMR granule browser for `OB_CLOUD` (`OBDAACDataBrowser`).
  - Full-file download (`FileDownloadManager`).
  - A Harmony subset dialog and job runner (`HarmonySubsetServiceDialog`, `HarmonySubsetTask`).

**Costs at open time** that grow remotely:

- **PACE L1B** reads the full `latitude`/`longitude` and wavelength arrays.
- **L2** reads full navigation lat/lon when it builds pixel geocoding.
- **All products** read the whole `scan_line_attributes` and `sensor_band_parameters` groups.
- **L3 bin is the worst case.** It reads all of `bin_num`, then calls `variable.read()` on the whole variable for every row.
- **Each file is opened twice:** once in `getDecodeQualification` and again in the reader, so the HDF5 header is parsed twice.
- **`readBandData` is `synchronized`**, so tile reads for one product run one at a time. Remotely, that means one network round trip after another.

## Ways to reach cloud-hosted NetCDF/HDF5

Only two routes work today for PACE OCI from a desktop outside AWS: HTTPS byte-range reads of the original file, and Harmony subsetting. Tested on 2026-10-26 against `PACE_OCI.20260901T235307.L2.OC_AOP.V3_2.nc` (345 MB).

| Method | Works for PACE OCI today? | Fits SNAP/SeaDAS readers | Notes |
| --- | --- | --- | --- |
| HTTPS byte-range on the archive URL (`obdaac-tea.earthdatacloud.nasa.gov/...`) | Yes, tested: HTTP 206 after Earthdata Login | Good: netCDF-Java opens `https://` with `HTTPRandomAccessFile` | Login redirects to a signed CloudFront URL; reuse it to skip the redirect on every read |
| Direct S3 (`s3://ob-cumulus-prod-public/...`) | Only inside AWS us-west-2 | netCDF-Java `cdm-s3` module | Temporary 1-hour keys from `/s3credentials`; refused from outside the region. Useful only for SeaDAS on cloud VMs |
| OPeNDAP / Hyrax in the cloud | No: the server returned 404 because no DMR++ sidecar exists for PACE granules | netCDF-Java `opendap`/DAP4 client | Would give server-side slicing, but OB.DAAC does not publish it for PACE |
| Zarr / kerchunk / VirtualiZarr references | No official ones; a community Icechunk store covers L3 chlorophyll only | SNAP has no Zarr-over-HTTP reader for these | Python ecosystem; out of scope for the Java desktop |
| Harmony subsetting (L2 Cloud Subsetter, OB.DAAC extraction tool) | Yes: L2 and L3M collections list a Harmony service in CMR | Output is a normal NetCDF-4 file the current readers open | Server-side; asynchronous jobs; covered in its own section |

The signed CloudFront URL handed out after login stayed valid for repeated range reads, so a client can authenticate once per file and then read directly.

## Authentication

Use Earthdata Login over HTTPS, with the bearer token SeaDAS already obtains. Direct S3 keys only help for SeaDAS running inside AWS us-west-2.

- **What we tested:** a range request to `obdaac-tea.earthdatacloud.nasa.gov` without credentials redirects to `urs.earthdata.nasa.gov` and ends in 401. With `~/.netrc` it returns 206 after 4 redirects (login handshake about 3.2 s). Requests that carried the session cookie needed one redirect.
- **Signed URL:** the final redirect points to a signed CloudFront URL (`Expires=` in the query). Range reads on that URL need no further auth and took about 0.4 s each. The client should cache it per granule, refresh it on 403, and never log it.
- **Bearer token:** `WebPageFetcherWithJWT` already gets an EDL token from `~/.netrc`, and `FileDownloadManager` sends it as `Authorization: Bearer`. The new remote reader should reuse that token, not build a second login path.
- **Redirect handling:** the client must follow cross-host redirects, keep cookies, and drop the `Authorization` header when it leaves the Earthdata domains. netCDF-Java's `HTTPRandomAccessFile` does not do this on its own. That is the main reason to write a small custom `RandomAccessFile` (see Implementation plan).
- **Direct S3:** `/s3credentials` returns 1-hour keys that only work from us-west-2 ([earthaccess docs](https://earthaccess.readthedocs.io/en/latest/api/), [NSIDC guide](https://nsidc.org/data/user-resources/help-center/nasa-earthdata-cloud-data-access-guide)). An OB.DAAC user outside AWS got 403 with those keys ([forum thread](https://forum.earthdata.nasa.gov/viewtopic.php?p=25409)). Using them would also need `cdm-s3` and session-token support.
- **Harmony:** uses the same EDL bearer token. `HarmonySubsetTask` already handles it.

## Performance and caching

Remote reading is fast for opening files and for 2-D bands. It is no faster than downloading for one wavelength of a 3-D variable, because each PACE chunk bundles 40 wavelengths. All figures were measured on 2026-10-26 from this workstation, which reaches CloudFront at about 10 MB/s with about 0.4 s per request. The test used a small Java prototype on SeaDAS's own netCDF-Java 5.3.3 jar.

| Scenario on the 345 MB L2 AOP granule | Data fetched | HTTP requests | Elapsed |
| --- | --- | --- | --- |
| Download the whole file (today's workflow) | 345 MB | 1 | 31 s |
| Remote open and header parse, 256 KB block cache | 0.6 MB | 4 | 2.1 s |
| Open, one 2-D band and lat/lon, 7 parallel handles | 11.9 MB | 53 | 8.2 s |
| Plus one Rrs wavelength, full scene, 7 parallel handles | 117 MB | 454 | 43 s |
| Same Rrs read, one handle (like today's `synchronized` reader) | 117 MB | 448 | 207 s |
| Open with netCDF-Java's stock remote reader, read cache off | 15 MB | 241 | 86 s (open alone) |

**What drives the numbers:**

- **Chunk layout.** L2 2-D variables (`chlor_a`, `aot_865`, lat/lon) are stored as 256 lines × full width, 7 chunks per scene, which suits remote reads. 3-D `Rrs` uses 32 × 256 × 40 chunks. Showing one wavelength reads 270 chunks (38.8 MB compressed of the 163 MB variable). L1B `rhot_*` uses 40 × 16 × 1272 chunks, the same 40-wavelength bundling.
- **Round trips, not bandwidth.** netCDF-Java's HDF5 reader fetches one chunk at a time. Parallel tile reads through several file handles that share one cache cut the Rrs read from 207 s to 43 s.
- **A block cache is required.** The HDF5 header parser re-reads the same bytes many times. With a 256 KB aligned block cache, open drops from 241 requests to 4. netCDF-Java 5.3.3's own remote read cache hit a `StackOverflowError` in `RemoteRandomAccessFile.readFromCache` on large reads, so SeaDAS would need its own cache.
- **File size.** Many PACE files are small: L2 BGC is 22.8 MB and L3m daily BGC at 0.1° is 10 MB. Downloading those takes 1–3 s here. Remote reading pays off for L1B (1.9 GB), L1C (534 MB) and L2 AOP (345 MB).

**Caching design:**

- **Memory:** an aligned LRU block cache of 256 KB blocks per open granule, capped at about 1 GB, shared by all file handles of one product.
- **Disk (optional):** keep fetched blocks under the SNAP cache folder so reopening a granule is instant. Offer "keep a full local copy" for heavy analysis.
- **Reader changes:** remove the double open in `getDecodeQualification` (cache the header or decide by file name). Allow parallel tile reads by using a pool of handles instead of the `synchronized` `readBandData`. Defer the full lat/lon read until geocoding is needed.

## Alternative: Harmony subsetting

Harmony works for PACE L2 and L3 and SeaDAS already calls it, but jobs take minutes, so it suits batch regional work, not interactive browsing. A test subset of a 5° × 7° box from the AOP granule ran 8 min 45 s. It returned a 34.8 MB NetCDF-4 file (from 345 MB) with the same group layout, which downloaded in 4.9 s.

| Harmony service (CMR id) | PACE collections | Subsetting offered | Output |
| --- | --- | --- | --- |
| PODAAC L2 Cloud Subsetter, l2ss-py (`S1962070864-POCLOUD`) | All L2 products (AOP, BGC, IOP, SFREFL, CLOUD, …) and NRT | Bounding box, shapefile/GeoJSON, variables, time | NetCDF-4, same structure as the input |
| OB.DAAC Data Extraction Tool (`S3144157208-OB_CLOUD`) | L3M, L3B, L4M and L2 IOP | Bounding box, variables, time | NetCDF-4 |
| none | L1A, L1B, L1C | — | Full download or remote read only |

- **Limit on variable names:** variable subsetting works only for variables registered in CMR (7 for L2 AOP). A request that named `navigation_data/latitude` was rejected ("Coverages were not found for the provided variables"). Spatial-only requests keep every variable, lat/lon included.
- **Existing code:** `HarmonySubsetServiceDialog` and `HarmonySubsetTask` already submit jobs, poll them and save the `.nc`. What's missing is opening the result in SeaDAS automatically, plus a status/queue view.
- **Untested:** whether the SeaDAS PACE readers accept the `_subsetted.nc` file. The global attributes (`title`, `processing_level`) are kept, so detection should still work. Verify this in the proof of concept.
- **Where it fits:** a region of interest over many granules, and products too large to stream. It is the fallback when remote reads are slow on a user's network.

## Proof-of-concept product

Use PACE OCI L2 AOP (`PACE_OCI_L2_AOP` v3.2, CMR `C4124887023-OB_CLOUD`) as the main test product, and L1B as the stretch case. AOP is large enough for remote reading to matter. It has both easy 2-D bands and hard 3-D `Rrs`, and Harmony supports it, so one granule can test both approaches.

| Product (CMR id) | Typical granule | Layout | Role in the proof of concept |
| --- | --- | --- | --- |
| L2 AOP (`C4124887023-OB_CLOUD`) | 345 MB | 2-D bands in 256 × 1272 chunks; `Rrs` 1710 × 1272 × 172 in 32 × 256 × 40 chunks | Main test: remote reading and Harmony |
| L1B (`C3392966952-OB_CLOUD`) | 1.9 GB | `rhot_blue`/`rhot_red` in 40 × 16 × 1272 chunks; geolocation in 512 × 1272 | Stretch: biggest potential payoff; no Harmony service |
| L2 BGC (`C4124887072-OB_CLOUD`) | 22.8 MB | All 2-D, 256 × 1272 chunks | Smoke test only; downloading is already fast |
| L3m BGC (`C4184125847-OB_CLOUD`) | 10 MB (daily, 0.1°) | Mapped grid | Out of scope; download |

The test granule was `PACE_OCI.20260901T235307.L2.OC_AOP.V3_2.nc`. Its HTTPS link is `https://obdaac-tea.earthdatacloud.nasa.gov/ob-cumulus-prod-public/<file>`, and the in-region S3 path is `s3://ob-cumulus-prod-public/<file>`.

## Proposed implementation plan

Start with Phase 1: a remote-open proof of concept on L2 AOP that reuses SeaDAS's existing login and data browser. Go on to the later phases only if its gate is met on real user networks.

| Phase | Work | Gate |
| --- | --- | --- |
| 1 · Remote open of PACE L2 AOP (proof of concept) | Earthdata HTTPS `RandomAccessFile`: EDL token, redirects, signed-URL reuse, 256 KB block cache. Opener and readers take `https://`. "Open remotely" button in the OB.DAAC data browser. | 2-D band + lat/lon in under 10 s; pixels match the local file |
| 2 · Parallel reads and lazy loading | Handle pool instead of `synchronized` `readBandData`. Lazy lat/lon geocoding; one open per file. Optional on-disk block cache. | `Rrs` band faster than download; MODIS/VIIRS regressions pass |
| 3 · Harmony for regions and batches | Auto-open subset results; job queue and status. Area of interest drawn on the map; L2 and L3M. Fallback when remote reads are slow. | Subset opens in the PACE reader; job status visible in SeaDAS |
| 4 · L1B, settings and upstream | L1B stretch case; cache size and thread preferences. Token refresh; timings on three networks. Propose a remote-file hook to ESA SNAP. | Ship in a SeaDAS release, or keep download-first |

**Code touched in Phase 1:**

1. **New `EarthdataHttpRandomAccessFile`** (extends `ucar.unidata.io.RandomAccessFile`). It gets the EDL bearer token from `WebPageFetcherWithJWT` and follows the redirect to the signed URL by hand. It holds an aligned LRU block cache, fetches several blocks in parallel when one read spans them, and re-authenticates on 403. The prototype classes `BlockRaf`/`ParRaf` from this investigation are a starting point.
2. **`snap-netcdf` `NetcdfFileOpener`:** route `https://` strings and URIs to the new class. Skip `FileUtils.getCachedFilePath` for them, and sniff the format from the file object instead of `Files.exists`.
3. **`opttbx-seadas-reader`:** `SeadasHelper.getInputFile`/`checkInputObject` and the PACE plugins accept a remote location and qualify it by file name plus header. `SeadasProductReader` passes the location through.
4. **`seadas-earthdata-cloud-toolbox`:** add an "Open remotely" action next to Download in `OBDAACDataBrowser` that calls `ProductIO.readProduct` with the HTTPS link.

Phase 2 changes the shared `SeadasFileReader` (handle pool, lazy geocoding). Phase 3 mostly wires up existing Harmony code. Effort is not sized here; size it after the Phase 1 gate.

## Risks and open questions

- **The user's network sets the speed.** All timings come from one link (about 10 MB/s, 0.4 s per request). High-latency links make remote reads much worse, while fast links make full downloads cheap. Repeat the measurements from a NASA network, from home broadband, and from a us-west-2 VM.
- **3-D variables over-read.** One `Rrs` or `rhot` wavelength costs about a quarter to a third of the variable. Workarounds: show a whole 40-band block at once, cache blocks on disk, or ask OB.DAAC whether a different chunking is possible in a future reprocessing.
- **SNAP core changes.** `NetcdfFileOpener` and `FileUtils.getCachedFilePath` live in snap-engine. SeaDAS either carries a patch or upstreams a pluggable "remote RandomAccessFile" hook to ESA. Decide early.
- **Products are single-threaded.** `SeadasFileReader.readBandData` is `synchronized`. Making it parallel-safe with a handle pool touches the shared reader base used by all SeaDAS products, so it needs regression testing on MODIS/VIIRS too.
- **Signed URLs expire.** The CloudFront URL carries an `Expires` time. A long session must re-authenticate on 403 without losing the cache.
- **Harmony turnaround** of about 9 minutes per job (one test) makes it unsuitable for interactive use. Ask whether the OB.DAAC extraction tool is faster for L2.
- **Open question:** will OB.DAAC publish DMR++ sidecars for PACE? That would enable cloud OPeNDAP, which does server-side slicing and would remove most of the client work.
- **Open question:** should SeaDAS target in-region (AWS us-west-2) users? If yes, add `cdm-s3` and session-token support for direct S3.

## Sources

- Measurements: run from this workstation on 2026-10-26 against the live OB.DAAC archive. The prototype Java classes (`BlockRaf`, `ParRaf`, `Inspect`) are in [`prototype/`](prototype/).
- Code: the snap-14 workspace (`snap-engine`, `optical-toolbox/opttbx-seadas-reader`, `seadas-toolbox/seadas-earthdata-cloud-toolbox`).
- [NASA CMR search API](https://cmr.earthdata.nasa.gov/search/): PACE collection, granule and Harmony service records.
- [netCDF-Java: reading remote files over HTTP](https://docs.unidata.ucar.edu/netcdf-java/5.1/userguide/read_over_http.html) and [dataset URLs (S3)](https://docs.unidata.ucar.edu/netcdf-java/5.3/userguide/dataset_urls.html)
- [earthaccess API (temporary S3 credentials)](https://earthaccess.readthedocs.io/en/latest/api/)
- [NSIDC: Earthdata Cloud data access guide](https://nsidc.org/data/user-resources/help-center/nasa-earthdata-cloud-data-access-guide)
- [Earthdata forum: 403 with OB.DAAC /s3credentials outside us-west-2](https://forum.earthdata.nasa.gov/viewtopic.php?p=25409)
- [l2ss-py (PODAAC L2 Cloud Subsetter)](https://pypi.org/project/l2ss-py/)
- [Community PACE L3 Icechunk store](https://source.coop/fish-pace/pace-oci)
