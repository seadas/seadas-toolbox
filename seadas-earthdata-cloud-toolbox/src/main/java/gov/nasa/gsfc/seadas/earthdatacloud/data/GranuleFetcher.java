package gov.nasa.gsfc.seadas.earthdatacloud.data;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import javax.swing.table.DefaultTableModel;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class GranuleFetcher {

    public static class Granule {
        public String fileName;
        public String downloadUrl;

        public Granule(String fileName, String downloadUrl) {
            this.fileName = fileName;
            this.downloadUrl = downloadUrl;
        }
    }

    public Map<String, String> fetchGranules(
            String product,
            int maxResults,
            String startDate,
            String endDate,
            String minLat,
            String maxLat,
            String minLon,
            String maxLon,
            DefaultTableModel tableModel) {

        Map<String, String> fileLinkMap = new HashMap<>();
        int pageSize = 2000;
        int page = 1;
        int totalFetched = 0;

        try {
            while (totalFetched < maxResults) {
                StringBuilder urlBuilder = new StringBuilder("https://cmr.earthdata.nasa.gov/search/granules.json?provider=OB_CLOUD");
                urlBuilder.append("&short_name=").append(product);
                urlBuilder.append("&page_size=").append(pageSize);
                urlBuilder.append("&page_num=").append(page);

                if (startDate != null) {
                    urlBuilder.append("&temporal=").append(startDate).append(",");
                    if (endDate != null) {
                        urlBuilder.append(endDate);
                    }
                }

                if (!minLat.isEmpty() && !maxLat.isEmpty() && !minLon.isEmpty() && !maxLon.isEmpty()) {
                    urlBuilder.append("&bounding_box=").append(minLon).append(",")
                            .append(minLat).append(",")
                            .append(maxLon).append(",")
                            .append(maxLat);
                }

                URL url = new URL(urlBuilder.toString());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                JSONObject json = new JSONObject(new JSONTokener(in));
                JSONArray entries = json.getJSONObject("feed").getJSONArray("entry");

                for (int i = 0; i < entries.length(); i++) {
                    JSONObject entry = entries.getJSONObject(i);
                    String title = entry.getString("title");
                    JSONArray links = entry.getJSONArray("links");

                    for (int j = 0; j < links.length(); j++) {
                        JSONObject link = links.getJSONObject(j);
                        if (link.has("href") && link.getString("href").endsWith(".nc")) {
                            String href = link.getString("href");
                            tableModel.addRow(new Object[]{title});
                            fileLinkMap.put(title, href);
                            break;
                        }
                    }

                    totalFetched++;
                    if (totalFetched >= maxResults) break;
                }

                if (entries.length() < pageSize) break;
                page++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return fileLinkMap;
    }

    public List<Granule> fetchGranules(String shortName,
                                       Date startDate,
                                       Date endDate,
                                       String minLat,
                                       String maxLat,
                                       String minLon,
                                       String maxLon,
                                       String dayNightFlag,
                                       int maxResults) throws Exception {

        List<Granule> granules = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        int page = 1;
        int pageSize = 2000;
        int totalFetched = 0;

        while (totalFetched < maxResults) {
            StringBuilder urlBuilder = new StringBuilder("https://cmr.earthdata.nasa.gov/search/granules.json?provider=OB_CLOUD");
            urlBuilder.append("&short_name=").append(URLEncoder.encode(shortName, StandardCharsets.UTF_8));
            urlBuilder.append("&page_size=").append(pageSize);
            urlBuilder.append("&page_num=").append(page);

            if (startDate != null) {
                urlBuilder.append("&temporal=").append(sdf.format(startDate)).append("T00:00:00Z,");
                urlBuilder.append(endDate != null ? sdf.format(endDate) + "T23:59:59Z" : "");
            }

            if (dayNightFlag != null && !dayNightFlag.isEmpty()) {
                urlBuilder.append("&day_night_flag=").append(dayNightFlag);
            }

            if (!minLat.isEmpty() && !maxLat.isEmpty() && !minLon.isEmpty() && !maxLon.isEmpty()) {
                urlBuilder.append("&bounding_box=")
                        .append(minLon).append(",")
                        .append(minLat).append(",")
                        .append(maxLon).append(",")
                        .append(maxLat);
            }

            HttpURLConnection conn = (HttpURLConnection) new URL(urlBuilder.toString()).openConnection();
            conn.setRequestProperty("Accept", "application/json");

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            JSONObject json = new JSONObject(new JSONTokener(reader));
            reader.close();

            JSONArray entries = json.getJSONObject("feed").optJSONArray("entry");
            if (entries == null || entries.isEmpty()) break;

            for (int i = 0; i < entries.length() && totalFetched < maxResults; i++) {
                JSONObject entry = entries.getJSONObject(i);
                String title = entry.optString("title");
                JSONArray links = entry.optJSONArray("links");
                if (links == null) continue;

                for (int j = 0; j < links.length(); j++) {
                    JSONObject link = links.getJSONObject(j);
                    String href = link.optString("href");
                    if (href != null && href.endsWith(".nc")) {
                        granules.add(new Granule(title, href));
                        totalFetched++;
                        break;
                    }
                }
            }
            page++;
        }

        return granules;
    }
}
