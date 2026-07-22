package gov.nasa.gsfc.seadas.earthdatacloud.action;

import gov.nasa.gsfc.seadas.earthdatacloud.auth.WebPageFetcherWithJWT;
import gov.nasa.gsfc.seadas.earthdatacloud.data.CmrGranuleMetadataFetcher;
import gov.nasa.gsfc.seadas.earthdatacloud.ui.HarmonySubsetServiceDialog;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.Locale;
import java.util.Scanner;

/**
 * Clean Harmony subset task:
 *  - builds OGC Coverages URL correctly (granuleid lowercase)
 *  - detects OPS vs UAT consistently
 *  - uses dialog meta first, only calls CMR when needed
 *  - token is optional; only required if server demands it
 *  - supports async Harmony jobs + downloads NetCDF link
 */
public class HarmonySubsetTask extends SwingWorker<JSONObject, Void> {

    private final JSONObject subsetParameters;
    private final JProgressBar progressBar;
    private final JButton subsetButton;
    private final JButton cancelButton;
    private final HarmonySubsetServiceDialog dialog;
    private final boolean needsL2Flags;

    private static final int CONNECT_TIMEOUT_MS = 30_000;
    private static final int READ_TIMEOUT_MS    = 5 * 60_000;
    private final Path requestedOutputPath;
    public HarmonySubsetTask(JSONObject subsetParameters,
                             JProgressBar progressBar,
                             JButton subsetButton,
                             JButton cancelButton,
                             boolean needsL2Flags,
                             HarmonySubsetServiceDialog dialog) {
        this.subsetParameters = subsetParameters;
        this.progressBar = progressBar;
        this.subsetButton = subsetButton;
        this.cancelButton = cancelButton;
        this.needsL2Flags = needsL2Flags;
        this.dialog = dialog;

        String outputFile = subsetParameters.optString("outputFile", null);
        this.requestedOutputPath = (outputFile == null || outputFile.isBlank())
                ? null
                : Paths.get(outputFile);
    }
    // ------------------------ Environment ------------------------

    private static final class Env {
        final String harmonyBase;
        final String cmrBase;
        Env(String harmonyBase, String cmrBase) {
            this.harmonyBase = harmonyBase;
            this.cmrBase = cmrBase;
        }
    }

    private Env detectEnv(String sourceUrl) {
        return new Env(
                "https://harmony.earthdata.nasa.gov",
                "https://cmr.earthdata.nasa.gov"
        );
    }

    // ------------------------ Main worker ------------------------

    @Override
    protected JSONObject doInBackground() throws Exception {
        updateUiStart();

        String sourceUrl = subsetParameters.optString("url", null);
        if (sourceUrl == null || sourceUrl.isBlank()) {
            throw new IllegalArgumentException("Missing 'url' in subset parameters");
        }

        Env env = detectEnv(sourceUrl);
        status("Env: Harmony=" + env.harmonyBase + " | CMR=" + env.cmrBase);

        // Token is OPTIONAL. We'll attach it if we have it.
        String token = tryGetToken();

        // Resolve granuleId + collectionId (concept ids)
        ResolvedIds ids = resolveIds(env, sourceUrl, token);

        // Build OGC URL
        String ogcUrl = buildOgcUrl(env, ids.collectionConceptId, ids.granuleConceptId);
        status("Harmony URL: " + ogcUrl);

        // Execute OGC request (sync or async job JSON)
        JSONObject result = executeOgc(env, ogcUrl, token);

        // If async job: poll then download final NetCDF link(s) to temp folder
        if (result.has("jobID")) {
            String jobId = result.getString("jobID");
            status("Harmony job created: " + jobId + " — polling...");
            setPhase("Harmony job submitted; polling...", 30);
            JSONObject finalJob = pollJob(env, jobId, token, Duration.ofMinutes(10));
            String status = finalJob.optString("status", "");
            status("Job status: " + status);
            setPhase("Job complete; preparing downloads...", 85);
            if ("failed".equalsIgnoreCase(status)) {
                throw new IOException("Harmony job failed: " + truncate(finalJob.toString(), 400));
            }

            // Download NetCDF links (best effort)
            int downloaded = downloadJobNetcdfAssets(finalJob, token);
            finalJob.put("downloadedCount", downloaded);
            return finalJob;
        }

        // If sync binary: executeOgc already saved and returns {"savedFile": "..."}
        return result;
    }

    @Override
    protected void done() {
        try {
            if (isCancelled()) {
                status("Subset request cancelled.");
                if (dialog != null) {
                    SwingUtilities.invokeLater(dialog::onSubsetCancelled);
                }
                return;
            }

            JSONObject result = get();
            if (result != null) {
                if (result.has("savedFile")) {
                    status("Saved subset: " + result.getString("savedFile"));
                } else if (result.has("downloadedCount")) {
                    status("Downloaded " + result.getInt("downloadedCount") + " NetCDF file(s).");
                } else {
                    status("Subset complete.");
                }
            } else {
                status("Subset finished (no result).");
            }

            if (dialog != null) {
                SwingUtilities.invokeLater(dialog::onSubsetSucceeded);
            }

        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            String msg = cause.getMessage() != null ? cause.getMessage() : cause.toString();

            status("ERROR: " + msg);
            JOptionPane.showMessageDialog(dialog, msg, "Subset Error", JOptionPane.ERROR_MESSAGE);

            if (dialog != null) {
                SwingUtilities.invokeLater(dialog::onSubsetFailed);
            }
        } finally {
            updateUiEnd();
        }
    }

    // ------------------------ ID resolution ------------------------

    private static final class ResolvedIds {
        final String granuleConceptId;     // G...-OB_CLOUD
        final String collectionConceptId;  // C...-OB_CLOUD
        ResolvedIds(String g, String c) { this.granuleConceptId = g; this.collectionConceptId = c; }
    }

    private ResolvedIds resolveIds(Env env, String sourceUrl, String token) throws Exception {
        // 1) Prefer dialog meta / setter
        String granuleId = subsetParameters.optString("granuleId", null);
        if ((granuleId == null || granuleId.isBlank()) && dialog != null) {
            CmrGranuleMetadataFetcher.GranuleMeta meta = dialog.getMeta();
            if (meta != null && meta.granuleId != null && !meta.granuleId.isBlank()) {
                granuleId = meta.granuleId;
            }
        }

        // 2) If still missing, resolve via CMR by readable granule name (filename)
        if (granuleId == null || granuleId.isBlank()) {
            String fileName = fileNameFromUrl(sourceUrl);
            status("Resolving granuleId from CMR using filename: " + fileName);
            granuleId = cmrResolveGranuleConceptId(env, fileName, token);
            if (granuleId == null) {
                throw new IOException("Could not resolve granuleId for filename: " + fileName);
            }
        }

        // 3) Resolve collection concept-id from CMR using granule concept-id
        String collectionId = subsetParameters.optString("collectionId", null);
        if (collectionId == null || collectionId.isBlank()) {
            status("Resolving collection concept-id from CMR for granule: " + granuleId);
            collectionId = cmrResolveCollectionConceptId(env, granuleId, token);
            if (collectionId == null) {
                throw new IOException("Could not resolve collectionId for granuleId: " + granuleId);
            }
        }

        status("Resolved: granuleId=" + granuleId + " | collectionId=" + collectionId);
        setPhase("Resolved IDs; building Harmony request...", 10);
        return new ResolvedIds(granuleId, collectionId);
    }

    private String cmrResolveGranuleConceptId(Env env, String readableGranuleName, String token) throws Exception {
        // Try OB_CLOUD then POCLOUD like you’ve been doing elsewhere
        for (String provider : new String[]{"OB_CLOUD", "POCLOUD"}) {
            String url = env.cmrBase + "/search/granules.json?readable_granule_name="
                    + urlEncode(readableGranuleName) + "&provider=" + provider;
            JSONObject json = httpGetJson(url, token);
            JSONArray entry = json.optJSONObject("feed") != null ? json.optJSONObject("feed").optJSONArray("entry") : null;
            if (entry != null && entry.length() > 0) {
                return entry.getJSONObject(0).optString("id", null); // concept-id
            }
        }
        return null;
    }

    private String cmrResolveCollectionConceptId(Env env, String granuleConceptId, String token) throws Exception {
        String url = env.cmrBase + "/search/granules/" + urlEncode(granuleConceptId) + ".json";
        JSONObject json = httpGetJson(url, token);
        JSONObject entry = json.optJSONObject("feed") != null
                ? (json.optJSONObject("feed").optJSONArray("entry") != null && json.optJSONObject("feed").optJSONArray("entry").length() > 0
                ? json.optJSONObject("feed").optJSONArray("entry").getJSONObject(0)
                : null)
                : null;
        if (entry == null) return null;
        return entry.optString("collection_concept_id", null);
    }

    // ------------------------ Harmony OGC ------------------------

    private String buildOgcUrl(Env env, String collectionConceptId, String granuleConceptId) {
        JSONArray vars = subsetParameters.optJSONArray("variables");
        boolean allVariablesSelected = subsetParameters.optBoolean("allVariablesSelected", false);

        boolean variableSubsetRequested =
                !allVariablesSelected && vars != null && vars.length() > 0;

        // if level2 file then force inclusion of l2_flags
        boolean forceAddL2Flags = false;
        if (needsL2Flags) {
            forceAddL2Flags = true;
        }

        StringBuilder url = new StringBuilder();
        url.append(env.harmonyBase)
                .append("/")
                .append(collectionConceptId)
                .append("/ogc-api-coverages/1.0.0/collections/");


        // Variable subset: use full Harmony variable paths from file metadata, e.g.
        // geophysical_data/chlor_a
        if (variableSubsetRequested) {
            boolean l2_flags_found = false;

            StringBuilder variableCsv = new StringBuilder();
            for (int i = 0; i < vars.length(); i++) {
                String var = vars.optString(i, "").trim();
                if (var.isEmpty()) {
                    continue;
                }
                if (variableCsv.length() > 0) {
                    variableCsv.append(",");
                }
                variableCsv.append(var);
                if (var.contains("l2_flags")) {
                    l2_flags_found = true;
                }
            }

            if (variableCsv.length() > 0) {
                if (needsL2Flags && forceAddL2Flags && !l2_flags_found) {
                    variableCsv.append(",").append("geophysical_data/").append("l2_flags");
                }

//                url.append("&variable=").append(urlEncode(variableCsv.toString()));
                url.append(urlEncode(variableCsv.toString()));
            }
        } else {
            url.append("all");
        }


        // Harmony wants parameter_vars + variable=... for variable subsetting
        String varSegment = variableSubsetRequested ? "parameter_vars" : "all";

        url.append("/coverage/rangeset");


        // Keep granuleid lowercase to match the working behavior you observed
        url.append("?granuleid=")
                .append(urlEncode(granuleConceptId));



        // Spatial subset
        String latMin = subsetParameters.optString("latMin", null);
        String latMax = subsetParameters.optString("latMax", null);
        String lonMin = subsetParameters.optString("lonMin", null);
        String lonMax = subsetParameters.optString("lonMax", null);

        if (latMin != null && latMax != null && !latMin.isBlank() && !latMax.isBlank()) {
            url.append("&subset=lat(")
                    .append(latMin)
                    .append(":")
                    .append(latMax)
                    .append(")");
        }

        if (lonMin != null && lonMax != null && !lonMin.isBlank() && !lonMax.isBlank()) {
            url.append("&subset=lon(")
                    .append(lonMin)
                    .append(":")
                    .append(lonMax)
                    .append(")");
        }

        url.append("&skipPreview=true");
        url.append("&pixelSubset=true");

        return url.toString();
    }

    private JSONObject executeOgc(Env env, String ogcUrl, String token) throws Exception {
        setPhase("Submitting Harmony request...", 5);
        HttpURLConnection conn = (HttpURLConnection) new URL(ogcUrl).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setInstanceFollowRedirects(true);
        if (token != null) conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setRequestProperty("Accept", "*/*");

        int code = conn.getResponseCode();
        String ctype = conn.getContentType();
        status("OGC GET status=" + code + " contentType=" + ctype);

        if (code == 401 || code == 403) {
            String body = slurp(conn.getErrorStream());
            throw new IllegalStateException(
                    "Harmony request failed in environment " + env.harmonyBase +
                            " with HTTP " + code + ". Response body: " + truncate(body, 600)
            );
        }

        if (code >= 400) {
            String err = slurp(conn.getErrorStream());
            throw new IOException("Harmony request failed: HTTP " + code + " — " + truncate(err, 500));
        }

        // Determine JSON vs binary by content-type and stream peek
        InputStream raw = conn.getInputStream();
        BufferedInputStream bis = new BufferedInputStream(raw);
        bis.mark(1);
        int first = bis.read();
        bis.reset();

        boolean isJson = (ctype != null && ctype.toLowerCase(Locale.ROOT).contains("application/json")) || first == '{';

        if (isJson) {
            String body = slurp(bis);
            setPhase("Harmony response received...", 20);
            return new JSONObject(body);
        }

        long contentLength = conn.getContentLengthLong();
        status("Downloading subset result...");
        if (contentLength > 0) {
            setPhase("Downloading subset...", 70);
        } else {
            setPhaseIndeterminate("Downloading subset...");
        }
        Path out = saveBinaryResponse(bis, "harmony_subset_", ".nc", contentLength);
        setPhase("Finalizing...", 95);

        JSONObject result = new JSONObject();
        result.put("savedFile", out.toAbsolutePath().toString());
        result.put("contentType", ctype);
        return result;
    }

    private String getUserTokenFallback() {
        String prop = System.getProperty("earthdata.userToken");
        if (isLikelyUserToken(prop)) return prop.trim();

        String env = System.getenv("EARTHDATA_USER_TOKEN");
        if (isLikelyUserToken(env)) return env.trim();

        try {
            if (!java.awt.GraphicsEnvironment.isHeadless()) {
                String pasted = JOptionPane.showInputDialog(
                        dialog,
                        "Harmony returned 401.\n" +
                                "Paste an Earthdata Login *User Token* (URS → Profile → User Tokens).\n" +
                                "Tip: set EARTHDATA_USER_TOKEN or -Dearthdata.userToken to avoid this prompt."
                );
                if (isLikelyUserToken(pasted)) return pasted.trim();
            }
        } catch (Throwable ignored) {}

        return null;
    }
    private static boolean isLikelyUserToken(String s) {
        // User Tokens are typically long and NOT JWT-like (not starting with eyJ)
        return s != null && s.trim().length() >= 20 && !s.trim().startsWith("eyJ");
    }

    private JSONObject pollJob(Env env, String jobId, String token, Duration timeout) throws Exception {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        setPhaseIndeterminate("Polling Harmony job...");

        while (System.currentTimeMillis() < deadline) {
            if (isCancelled()) throw new InterruptedException("Cancelled");
            Thread.sleep(5_000);

            String jobUrl = env.harmonyBase + "/jobs/" + urlEncode(jobId);
            JSONObject json = httpGetJson(jobUrl, token);
            String st = json.optString("status", "");
            status("Job " + jobId + " status: " + st);
            setPhaseIndeterminate("Polling Harmony job (" + st + ")...");

            if ("successful".equalsIgnoreCase(st) ||
                    "partial_success".equalsIgnoreCase(st) ||
                    "failed".equalsIgnoreCase(st)) {
                return json;
            }
        }

        throw new IOException("Timed out waiting for Harmony job: " + jobId);
    }

    private int downloadJobNetcdfAssets(JSONObject jobJson, String token) throws Exception {
        JSONArray links = jobJson.optJSONArray("links");
        if (links == null || links.length() == 0) {
            status("No links found in Harmony job result.");
            return 0;
        }

        int netcdfCount = 0;
        for (int i = 0; i < links.length(); i++) {
            JSONObject link = links.getJSONObject(i);
            String href = link.optString("href", null);
            if (href != null && href.toLowerCase(Locale.ROOT).contains(".nc")) {
                netcdfCount++;
            }
        }

        int downloaded = 0;
        for (int i = 0; i < links.length(); i++) {
            if (isCancelled()) throw new InterruptedException("Cancelled");

            JSONObject link = links.getJSONObject(i);
            String href = link.optString("href", null);
            if (href == null) continue;
            if (!href.toLowerCase(Locale.ROOT).contains(".nc")) continue;

            downloaded++;
            Path out = saveRemoteFile(href, token, downloaded, netcdfCount);
            status("Downloaded: " + out.toAbsolutePath());
            setPhase("Downloaded " + downloaded + " of " + netcdfCount, downloadProgress(downloaded, netcdfCount));
        }

        return downloaded;
    }

    private static int downloadProgress(int filesCompleted, int totalFiles) {
        if (totalFiles <= 0) {
            return 95;
        }
        return Math.min(95, 85 + (int) Math.round(10.0 * filesCompleted / totalFiles));
    }

    // ------------------------ HTTP helpers ------------------------

    private JSONObject httpGetJson(String url, String token) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setInstanceFollowRedirects(true);
        if (token != null) conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setRequestProperty("Accept", "application/json");

        int code = conn.getResponseCode();
        if (code == 401 || code == 403) {
            // CMR is usually public; Harmony jobs may require auth. Report cleanly.
            throw new IllegalStateException("HTTP " + code + " from " + url + " (auth required).");
        }
        if (code >= 400) {
            String err = slurp(conn.getErrorStream());
            throw new IOException("HTTP " + code + " from " + url + " — " + truncate(err, 400));
        }

        String body = slurp(conn.getInputStream());
        return new JSONObject(body);
    }

    private Path saveRemoteFile(String href, String token, int downloadIndex, int totalDownloads) throws Exception {
        String current = href;
        for (int hop = 0; hop < 6; hop++) {
            HttpURLConnection conn = (HttpURLConnection) new URL(current).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setInstanceFollowRedirects(false);
            if (token != null) conn.setRequestProperty("Authorization", "Bearer " + token);

            int code = conn.getResponseCode();
            if (code == HttpURLConnection.HTTP_MOVED_PERM ||
                    code == HttpURLConnection.HTTP_MOVED_TEMP ||
                    code == 303) {
                String loc = conn.getHeaderField("Location");
                if (loc == null) throw new IOException("Redirect without Location header: " + current);
                current = loc;
                continue;
            }

            if (code == 401 || code == 403) {
                throw new IllegalStateException("Download unauthorized (HTTP " + code + "). Token missing/invalid.");
            }
            if (code >= 400) {
                String err = slurp(conn.getErrorStream());
                throw new IOException("Download failed HTTP " + code + " — " + truncate(err, 400));
            }

            String name = extractFileName(current);
            long contentLength = conn.getContentLengthLong();
            try (InputStream is = conn.getInputStream()) {
                Path out = resolveDownloadPath(name, downloadIndex, totalDownloads);
                String label = "Downloading " + downloadIndex + " of " + totalDownloads + " (" + name + ")";
                if (contentLength > 0) {
                    setPhase(label, downloadProgress(downloadIndex - 1, totalDownloads));
                } else {
                    setPhaseIndeterminate(label);
                }

                int sliceStart = downloadProgress(downloadIndex - 1, totalDownloads);
                int sliceEnd = downloadProgress(downloadIndex, totalDownloads);
                copyDownloadStream(is, out, contentLength, sliceStart, sliceEnd);
                return out;
            }
        }

        throw new IOException("Too many redirects: " + href);
    }

    private void copyDownloadStream(InputStream is, Path out, long contentLength, int sliceStart, int sliceEnd)
            throws IOException {
        try (OutputStream os = Files.newOutputStream(out, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
            byte[] buffer = new byte[8192];
            long totalRead = 0;
            int read;
            while ((read = is.read(buffer)) != -1) {
                if (isCancelled()) {
                    throw new InterruptedIOException("Cancelled");
                }
                os.write(buffer, 0, read);
                totalRead += read;
                if (contentLength > 0) {
                    int p = sliceStart + (int) Math.round((sliceEnd - sliceStart) * (totalRead / (double) contentLength));
                    setProgress(Math.min(sliceEnd, p));
                }
            }
        }
    }

    private Path resolveDownloadPath(String suggestedRemoteName, int downloadIndex, int totalDownloads) throws IOException {
        if (requestedOutputPath == null) {
            return Files.createTempFile(stripExt(suggestedRemoteName) + "_", ".nc");
        }

        Path fileNamePath = requestedOutputPath.getFileName();
        String requestedName = fileNamePath != null ? fileNamePath.toString() : "";

        // Better: determine this from UI/state, not filesystem existence
        boolean treatAsDirectory = requestedName.isEmpty() || !requestedName.contains(".");

        Path directory;
        String baseFileName;

        if (treatAsDirectory) {
            directory = requestedOutputPath;
            baseFileName = suggestedRemoteName;
        } else {
            directory = requestedOutputPath.getParent();
            baseFileName = requestedName;
        }

        if (directory != null) {
            Files.createDirectories(directory);
        }

        if (totalDownloads <= 1) {
            return (directory != null) ? directory.resolve(baseFileName) : Paths.get(baseFileName);
        }

        String stem = stripExt(baseFileName);
        String suffix = baseFileName.toLowerCase(Locale.ROOT).endsWith(".nc") ? ".nc" : "";
        if (suffix.isEmpty() && suggestedRemoteName.toLowerCase(Locale.ROOT).endsWith(".nc")) {
            suffix = ".nc";
        }

        return (directory != null)
                ? directory.resolve(stem + "_" + downloadIndex + suffix)
                : Paths.get(stem + "_" + downloadIndex + suffix);
    }

    // ------------------------ Token handling ------------------------

    private String tryGetToken() {
        try {
            String tok = WebPageFetcherWithJWT.getAccessToken("urs.earthdata.nasa.gov");
            if (tok != null && tok.trim().length() >= 20) {
                status("Using Earthdata token (masked): " + tok.substring(0, 6) + "..." + tok.substring(tok.length() - 6));
                return tok.trim();
            }
        } catch (Exception e) {
            // token is optional; do not fail here
            status("No Earthdata token found (will try unauthenticated).");
        }
        return null;
    }

    // ------------------------ UI helpers ------------------------

    private void updateUiStart() {
        setPhaseIndeterminate("Starting...");
        status("=== HarmonySubsetTask started ===");
    }

    private void updateUiEnd() {
        setPhase("Done", 100);
    }

    private void setPhase(String label, int progress) {
        setPhaseInternal(label, progress, false);
    }

    private void setPhaseIndeterminate(String label) {
        setPhaseInternal(label, -1, true);
    }

    private void setPhaseInternal(String label, int progress, boolean indeterminate) {
        if (progress >= 0) {
            setProgress(progress);
        }
        if (progressBar != null) {
            SwingUtilities.invokeLater(() -> {
                progressBar.setIndeterminate(indeterminate);
                if (label != null) {
                    progressBar.setString(label);
                }
            });
        }
    }

    private void status(String msg) {
        System.out.println(msg);
        if (dialog != null) {
            SwingUtilities.invokeLater(() -> dialog.updateStatus(msg));
        }
    }

    private Path saveBinaryResponse(InputStream is, String prefix, String suffix, long contentLength) throws IOException {
        Path out = chooseOutputPathForWrite(prefix, suffix);

        try (OutputStream os = Files.newOutputStream(out)) {
            byte[] buffer = new byte[8192];
            long totalRead = 0;
            int read;

            while ((read = is.read(buffer)) != -1) {
                if (isCancelled()) {
                    throw new InterruptedIOException("Cancelled");
                }
                os.write(buffer, 0, read);
                totalRead += read;

                if (contentLength > 0) {
                    int p = 70 + (int) ((25.0 * totalRead) / contentLength);
                    setProgress(Math.min(95, p));
                }
            }
        }

        return out;
    }

    private Path chooseOutputPathForWrite(String defaultPrefix, String suffix) throws IOException {
        if (requestedOutputPath != null) {
            Path parent = requestedOutputPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            return requestedOutputPath;
        }

        return Files.createTempFile(defaultPrefix, suffix);
    }

    private static String fileNameFromUrl(String url) {
        int idx = url.lastIndexOf('/');
        return (idx >= 0) ? url.substring(idx + 1) : url;
    }

    private static String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String slurp(InputStream is) throws IOException {
        if (is == null) return "";
        try (Scanner sc = new Scanner(is, StandardCharsets.UTF_8)) {
            sc.useDelimiter("\\A");
            return sc.hasNext() ? sc.next() : "";
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    private static String extractFileName(String url) {
        // best-effort: endswith .nc or use last path segment
        int q = url.indexOf('?');
        String base = (q >= 0) ? url.substring(0, q) : url;
        int slash = base.lastIndexOf('/');
        String name = (slash >= 0) ? base.substring(slash + 1) : base;
        if (!name.toLowerCase(Locale.ROOT).endsWith(".nc")) name = name + ".nc";
        return name;
    }

    private static String stripExt(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}