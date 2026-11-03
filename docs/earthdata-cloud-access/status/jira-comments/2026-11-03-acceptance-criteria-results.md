**Remote open prototype: results against the acceptance criteria**

All acceptance criteria are met. SeaDAS opens PACE OCI files directly from Earthdata Cloud, and pixels match the downloaded files exactly.

| Acceptance criterion | Status | Evidence |
| --- | --- | --- |
| Open a PACE OCI L2 AOP granule directly from Earthdata Cloud | Met | `PACE_OCI.20260901T235307.L2.OC_AOP.V3_2.nc` opens in 6–7 s. L2 BGC and L1B also work. |
| Earthdata Login works with the existing SeaDAS authentication | Met | Uses the Earthdata Login SeaDAS already has (`~/.netrc`); nothing new to set up. |
| Open Remotely available in the Earthdata Cloud Data Browser | Met | **Open Remotely** button next to Download. The browser now stays open beside SeaDAS, so you don't have to close it to view bands. |
| 2-D and 3-D bands display correctly | Met | 2-D (`aot_865`, geometry) and 3-D (`Rrs` and L1B wavelengths) bands display, and every band checked matches the local file pixel for pixel. |
| Reads only the needed parts of the file | Met | Opening L2 AOP fetches under 1 MB of the 345 MB file. One `Rrs` wavelength fetches about 50 MB. |
| Performance compared with full download | Met | See the table below. |
| Known limitations documented | Met | On the status page (link below) and in seadas-toolbox `docs/earthdata-cloud-access/`. |
| Recommendation for production integration | Met | See below. |

**Performance** (one workstation, about 10 MB/s to the archive):

| File | Full download | Open remotely | First band shown |
| --- | --- | --- | --- |
| L2 AOP, 345 MB | 31 s | 6–7 s | `aot_865` under 1 s; first `Rrs` about 6 s |
| L1B, 1.9 GB | 171 s | about 7.5 s | blue or red band about 25 s; SWIR 6 s |

**Main limitations:**
- **L1B blue and red bands:** they are stored in blocks of 40 wavelengths, so the first band from a block costs about 25 s. Work that uses most wavelengths is still faster with a full download.
- **Not tested yet:** analysis tools (Band Maths, statistics, processing), L1C and L3, other data centers, and sessions longer than the download link's lifetime.
- **Only from the data browser:** opening any other Earthdata link (for example from File > Open) isn't supported.
- **No reuse after closing:** fetched data is kept in memory only, so reopening a file fetches it again.
- **Change to SNAP's own code:** the snap-engine part changes SNAP itself, so SeaDAS must either carry it as a patch or get ESA to accept it.

**Recommendation:** proceed with production integration. Remote open cuts the time to first image from 31 s to about 7 s for L2 and from 171 s to under 30 s for L1B, with identical pixels. Before release:
1. Test the analysis tools on a remotely opened file.
2. Test a session longer than the download link's lifetime.
3. Decide how to handle the snap-engine change: carry it as a SeaDAS patch or propose it to ESA.
4. Keep Download as the choice for small files and for work that uses most L1B wavelengths.

**Branches** (for building and review): the work is on new branches based on the new cycle's `SEADAS-13.0.0-SNAP-14.0.1`. This also fixes the earlier build failure. The old branches built as 14.0.0-SNAPSHOT, so seadas-toolbox couldn't find the new snap-engine classes.

| Repo | Branch |
| --- | --- |
| snap-engine | `SEADAS-13.0.0-SNAP-14.0.1-earthdata-remote` |
| snap-desktop | `SEADAS-13.0.0-SNAP-14.0.1` (no changes) |
| optical-toolbox | `SEADAS-13.0.0-SNAP-14.0.1-earthdata-remote` |
| seadas-toolbox | `SEADAS-13.0.0-SNAP-14.0.1-earthdata-remote` |

To build, run `mvn install -Dmaven.test.skip=true` in the usual order: snap-engine, snap-desktop, optical-toolbox, seadas-toolbox. No extra flags are needed. None of these branches has a PR yet. The old PRs (#642, #171, #7) still point at the `SEADAS-12.0.0` branches and should be closed when new ones are opened.

Full status and open items: https://claude.ai/artifact/UcaxyUpB23uGYC2nwWDCXt
