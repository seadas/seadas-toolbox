# SeaDAS Remote Open: Status

Nov 3, 2026 · Aynur Abdurazik

> Copy of the live status page saved on Nov 3, 2026. The live page is https://claude.ai/artifact/UcaxyUpB23uGYC2nwWDCXt and may be newer.

SeaDAS can now open PACE OCI Level-1B and Level-2 files straight from Earthdata Cloud and display them without downloading the file. That meets "streaming data from EDC for visualization" for PACE L1B and L2. It only partly meets the analysis part, and the wider requirement: analysis tools, other products and other data centers are untested, and none of the code is in a release yet.

## What it does

An "Open remotely" button next to Download in the OB.DAAC data browser opens a file from NASA's cloud archive, using the Earthdata Login SeaDAS already has. SeaDAS then downloads only the parts of the file a view or tool asks for: the header when the file opens, and each band's data the first time it is shown or used.

The data browser stays open beside SeaDAS, so you can view an opened file and go back to open more. It keeps its last search until SeaDAS closes.

Measured on `PACE_OCI.20260901T235307.L2.OC_AOP.V3_2.nc` (345 MB) from one workstation, about 10 MB/s to the archive:

| Step | Time remotely | Data fetched |
| --- | --- | --- |
| Download the whole file (today's workflow) | 31 s | 345 MB |
| Open the file | 6–7 s | under 1 MB |
| Show a 2-D band such as `aot_865` | under 1 s | a few MB |
| Show one `Rrs` wavelength, first time | about 6 s | about 50 MB |
| Show another `Rrs` wavelength | about 5 s | about 50 MB |

Remote reads gave the same pixels as the local file for every band checked: 6 AOP bands, 3 BGC bands, and latitude/longitude in both. About 3 s of the open time is NASA's login redirect, which SeaDAS cannot shorten.

Measured on `PACE_OCI.20260901T234807.L1B.V3.nc` (1.9 GB), same workstation and connection:

| Step | Time remotely | Data fetched |
| --- | --- | --- |
| Download the whole file (today's workflow) | 171 s | 1.9 GB |
| Open the file | about 7.5 s | 8 MB |
| Show a blue or red band, first time | about 25 s | about 256 MB |
| Show another band in the same block of 40 wavelengths | instant | nothing more |
| Show a SWIR band, first time | 6 s | 61 MB |

Remote L1B pixels match the local file. L1B stores blue and red wavelengths in blocks of 40, so the first band shown from a block fetches the whole block, an eighth of the file. Work that touches most wavelengths is still faster with a full download.

## Against the requirement

Visualization of PACE L1B and L2 from the cloud works today; the rest of "streaming from EDC for visualization and analysis" is untested or not built yet.

| Part of the requirement | Status | Notes |
| --- | --- | --- |
| Show remote data in image views | Met | Zooming and panning fetch data as needed |
| Analysis: Band Maths, Pixel Info, statistics, processing tools | Untested | They read through the same reader as image views, so they should work; each fetches every band it touches over the network |
| PACE OCI L2 (AOP, BGC) | Met | Pixels match the local files |
| PACE OCI L1B | Met | Opens in about 7.5 s instead of a 171 s download; pixels match the local file |
| PACE OCI L1C, L3 | Untested | L3 files are small enough to download |
| MODIS and VIIRS opened remotely | Untested | Opening them locally is unchanged: identical output on 4 test files |
| Open any Earthdata link, e.g. from File > Open | Not met | Only the OB.DAAC data browser button opens remotely |
| Other data centers, e.g. PO.DAAC | Untested | Only OB.DAAC's archive was tried |
| Reopen a file without fetching it again | Not met | Fetched data is kept in memory only, for the current session |
| Sessions longer than the download link's lifetime | Untested | NASA's login hands out a time-limited link |
| Available in a SeaDAS release | Not met | See Code status |

All timings come from one network. Slower or higher-latency connections will be slower, and small files such as L2 BGC (23 MB) are quicker to download in full.

## Code status

Phase 1 and Phase 2 now sit together on one branch per repository, built on the new cycle's `SEADAS-13.0.0-SNAP-14.0.1` branches; none has a PR yet. With snap-desktop on `SEADAS-13.0.0-SNAP-14.0.1`, they build with the team's standard 14.0.1 setup (full build checked on Nov 2).

| Repository | Branch | Where it is |
| --- | --- | --- |
| snap-engine | `SEADAS-13.0.0-SNAP-14.0.1-earthdata-remote` | Pushed, no PR |
| optical-toolbox | `SEADAS-13.0.0-SNAP-14.0.1-earthdata-remote` | Pushed, no PR (includes the L1B read fix) |
| seadas-toolbox | `SEADAS-13.0.0-SNAP-14.0.1-earthdata-remote` | Pushed, no PR (includes the data browser change) |

The earlier Phase 1 PRs are still open against the old `SEADAS-12.0.0` branches: [snap-engine #642](https://github.com/senbox-org/snap-engine/pull/642), [optical-toolbox #171](https://github.com/senbox-org/optical-toolbox/pull/171) and [seadas-toolbox #7](https://github.com/seadas/seadas-toolbox/pull/7).

The snap-engine change sits in SNAP's own code, so SeaDAS will either carry it as a patch or need ESA to accept it upstream. The investigation behind this work is in seadas-toolbox at `docs/earthdata-cloud-access/` on branch `SEADAS-earthdata-cloud-docs-14.0.1`.

## To close the gaps

The first four items would let SeaDAS meet the requirement in full; the rest are decisions and checks.

- [ ] Test Band Maths, Pixel Info, statistics and one processing tool on a remote file
- [ ] Test PACE L1C and L3 opened remotely
- [ ] Add a way to open any Earthdata link, not just from the OB.DAAC data browser
- [ ] Open PRs from the new-cycle branches, close the old Phase 1 PRs, and merge into a SeaDAS release
- [ ] Decide whether to carry the snap-engine change as a SeaDAS patch or propose it to ESA
- [ ] Repeat the timings on a home connection and from an AWS us-west-2 machine
- [ ] Test a session longer than the download link's lifetime
- [ ] Optional: keep fetched data on disk so reopening a file is instant
