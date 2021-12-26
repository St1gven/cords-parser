import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CordParser {

    private static final Pattern hrefPattern = Pattern.compile("href=\"(.*?)\"");
    private static final Pattern cordPattern = Pattern.compile("\\d{2}°\\d{2}'\\d{2}\\.\\d\"N \\d{2}°\\d{2}'\\d{2}\\.\\d\"E");

    public DMS convertCordToDMS(double cord) {
        double deg = Math.floor(cord);
        cord *= 3600.0;
        double min = Math.floor(cord % 3600.0 / 60.0);
        double sec = cord % 60.0;
        return new DMS(deg, min, sec);
    }

    public double convertDMStoCord(double deg, double min, double sec) {
        return deg + (min / 60.0) + (sec / 3600.0);
    }

    public double convertDMStoCord(DMS dms) {
        return convertDMStoCord(dms.deg, dms.min, dms.sec);
    }

    public static class DMS {
        double deg;
        double min;
        double sec;

        DMS(String cord) {

            String[] degSplitted = cord.split("°");
            this.deg = Double.parseDouble(degSplitted[0]);
            String[] minSplitted = degSplitted[1].split("'");
            this.min = Double.parseDouble(minSplitted[0]);
            this.sec = Double.parseDouble(minSplitted[1].split("\"")[0]);
        }

        DMS(double deg, double min, double sec) {
            this.deg = deg;
            this.min = min;
            this.sec = sec;
        }
    }

    public String parseLink(String url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setInstanceFollowRedirects(false);

            while (!url.isEmpty() && connection.getResponseCode() / 100 == 3) {
                url = connection.getHeaderField("location");
                connection = (HttpURLConnection) new URL(url).openConnection();
            }
        } catch (IOException e) {
            return null;
        }

        String link = URLDecoder.decode(url, Charset.defaultCharset());
        if (link.contains("/maps/place/")) {
            try {
                String cords = link.split("/maps/place/")[1].split("/")[0];

                if (cordPattern.matcher(cords).matches()) {
                    String[] sCords = cords.split(" ");
                    DMS cord1 = new DMS(sCords[0]);
                    DMS cord2 = new DMS(sCords[1]);
                    return String.format("%f,%f", convertDMStoCord(cord1), convertDMStoCord(cord2));
                } else {
                    return cords;
                }


            } catch (ArrayIndexOutOfBoundsException e) {
                System.out.println("Wrong link format");
                return null;
            }
        } else {
            return null;
        }
    }

    public List<String> paresPage(String page) {

        Matcher m = hrefPattern.matcher(page);

        ExecutorService executor = Executors.newCachedThreadPool();
        List<Future<String>> futures = new ArrayList<>();
        while (m.find()) {
            String url = m.group(1);
            if (url.startsWith("http")) {
                futures.add(executor.submit(() -> parseLink(url)));
            }
        }
        executor.shutdown();
        return futures.stream().map(future -> {
            try {
                return future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

}
