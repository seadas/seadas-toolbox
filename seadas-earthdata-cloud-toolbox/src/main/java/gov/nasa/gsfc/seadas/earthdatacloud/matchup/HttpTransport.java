package gov.nasa.gsfc.seadas.earthdatacloud.matchup;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;

/**
 * The HTTP calls {@link HarmonyJobClient} makes, behind an interface so tests
 * can stand in for Harmony. Redirects are never followed here: the client
 * decides, because the token must not go to the storage a download redirects to.
 */
public interface HttpTransport {

    /** A response; close it to release the connection. */
    final class Response implements Closeable {
        private final int status;
        private final Map<String, String> headers;
        private final InputStream body;

        /** @param headers response headers, keys in lower case */
        public Response(int status, Map<String, String> headers, InputStream body) {
            this.status = status;
            this.headers = headers;
            this.body = body;
        }

        public int status() {
            return status;
        }

        /** A header by lower-case name, or null. */
        public String header(String name) {
            return headers.get(name.toLowerCase(Locale.ROOT));
        }

        public InputStream body() {
            return body;
        }

        public String text() throws IOException {
            try (InputStream in = body) {
                return in == null ? "" : new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        }

        @Override
        public void close() throws IOException {
            if (body != null) {
                body.close();
            }
        }
    }

    /**
     * Sends a request.
     *
     * @param method  GET or POST
     * @param headers request headers
     * @param body    a POST body, or null
     */
    Response send(String method, URI uri, Map<String, String> headers, String body)
            throws IOException, InterruptedException;

    /** The JDK's HTTP client: 30 s to connect, 5 min for a response, no redirects. */
    static HttpTransport jdk() {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        return (method, uri, headers, body) -> {
            HttpRequest.Builder request = HttpRequest.newBuilder(uri).timeout(Duration.ofMinutes(5));
            headers.forEach(request::header);
            request.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
            HttpResponse<InputStream> response = client.send(request.build(), HttpResponse.BodyHandlers.ofInputStream());
            Map<String, String> lower = new java.util.HashMap<>();
            response.headers().map().forEach((name, values) -> {
                if (!values.isEmpty()) {
                    lower.put(name.toLowerCase(Locale.ROOT), values.get(0));
                }
            });
            return new Response(response.statusCode(), lower, response.body());
        };
    }
}
