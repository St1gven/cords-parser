import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ENHelper {

    private final HttpClient httpClient;
    private final String domain;

    public ENHelper(String domain) {
        this.domain = domain;
        CookieManager cm = new CookieManager();
        cm.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        this.httpClient = HttpClient.newBuilder()
                .cookieHandler(cm)
                .build();
    }

    public void auth(String username, String password) throws IOException {
        Map<String, String> auth = new HashMap<>() {{
            put("Login", username);
            put("Password", password);
        }};
        try {
            httpClient.send(HttpRequest.newBuilder()
                    .POST(ofFormData(auth))
                    .uri(URI.create(String.format("http://%s/Login.aspx", domain)))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .build(), HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public String request(String path) throws IOException {
        try {
            HttpResponse<String> resp = httpClient.send(HttpRequest.newBuilder()
                            .GET()
                            .uri(URI.create(String.format("http://%s/%s", domain, path)))
                            .build()
                    , HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                return resp.body();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();

        }
        return null;
    }

     private static HttpRequest.BodyPublisher ofFormData(Map<String, String> data) {
        StringBuilder result = new StringBuilder();
        data.forEach( (key, value) -> {
            if (!result.isEmpty()) {
                result.append("&");
            }
            result.append(String.format("%s=%s", URLEncoder.encode(key, StandardCharsets.UTF_8), URLEncoder.encode(value, StandardCharsets.UTF_8)));
        });
        return HttpRequest.BodyPublishers.ofString(result.toString());
    }
}
