package gov.nasa.gsfc.seadas.earthdatacloud.matchup;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * A Harmony job, from {@code /jobs/{id}} (with links) or {@code /jobs/status} (without).
 *
 * @param jobId    the job's UUID
 * @param status   Harmony's status, e.g. {@code running}, {@code successful}
 * @param progress percent done
 * @param message  Harmony's message, or null from {@code /jobs/status}
 * @param links    result and paging links; empty from {@code /jobs/status}
 */
public record HarmonyJob(String jobId, String status, int progress, String message, List<Link> links) {

    /** Statuses a job leaves only when a user acts (harmony-py's {@code TERMINAL_JOB_STATUSES}). */
    public static final Set<String> TERMINAL =
            Set.of("successful", "failed", "canceled", "complete_with_errors", "paused");

    /** A job link: {@code rel} is {@code data} for an output file. */
    public record Link(URI href, String rel, String title, String type) {
    }

    public HarmonyJob {
        links = links == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(links));
    }

    public boolean terminal() {
        return TERMINAL.contains(status);
    }

    public boolean successful() {
        return "successful".equals(status);
    }

    /** The job's output files. */
    public List<Link> dataLinks() {
        return links.stream().filter(link -> "data".equals(link.rel())).toList();
    }

    /** The next page of links, or null. */
    Link nextPage() {
        return links.stream().filter(link -> "next".equals(link.rel())).findFirst().orElse(null);
    }

    static HarmonyJob fromJson(JSONObject json) {
        List<Link> links = new ArrayList<>();
        JSONArray array = json.optJSONArray("links");
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                JSONObject link = array.getJSONObject(i);
                String href = link.optString("href", null);
                if (href != null) {
                    links.add(new Link(URI.create(href), link.optString("rel", null),
                            link.optString("title", null), link.optString("type", null)));
                }
            }
        }
        return new HarmonyJob(json.getString("jobID"), json.getString("status"), json.optInt("progress", 0),
                json.optString("message", null), links);
    }

    HarmonyJob withMoreLinks(List<Link> more) {
        List<Link> all = new ArrayList<>(links.stream().filter(l -> !"next".equals(l.rel())).toList());
        all.addAll(more);
        return new HarmonyJob(jobId, status, progress, message, all);
    }
}
