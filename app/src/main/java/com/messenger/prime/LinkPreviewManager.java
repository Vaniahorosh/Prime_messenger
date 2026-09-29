package com.messenger.prime;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.LruCache;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinkPreviewManager {

    private static final String TAG = "LinkPreviewManager";
    private static LinkPreviewManager instance;

    public static class LinkPreviewData {
        public String url;
        public String domain;
        public String title;
        public String description;
        public String imageUrl;
    }

    public interface Callback {
        void onDataLoaded(LinkPreviewData data);
    }

    private final LruCache<String, LinkPreviewData> cache = new LruCache<>(100);
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static synchronized LinkPreviewManager getInstance() {
        if (instance == null) {
            instance = new LinkPreviewManager();
        }
        return instance;
    }

    private LinkPreviewManager() {}

    public void fetchPreview(String rawUrl, Callback callback) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) return;

        String formattedUrl = rawUrl.trim();
        if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
            formattedUrl = "https://" + formattedUrl;
        }

        final String targetUrl = formattedUrl;

        LinkPreviewData cached = cache.get(targetUrl);
        if (cached != null) {
            if (callback != null) {
                callback.onDataLoaded(cached);
            }
            return;
        }

        executor.execute(() -> {
            LinkPreviewData data = parseOpenGraph(targetUrl);
            if (data != null) {
                cache.put(targetUrl, data);
            }
            if (callback != null) {
                mainHandler.post(() -> callback.onDataLoaded(data));
            }
        });
    }

    private LinkPreviewData parseOpenGraph(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:109.0) Gecko/109.0 Firefox/115.0");
            conn.setInstanceFollowRedirects(true);

            int code = conn.getResponseCode();
            if (code != 200) return createFallbackData(urlString);

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder html = new StringBuilder();
            String line;
            int maxChars = 150000;
            while ((line = reader.readLine()) != null && html.length() < maxChars) {
                html.append(line).append("\n");
            }
            reader.close();

            String pageSource = html.toString();
            LinkPreviewData data = new LinkPreviewData();
            data.url = urlString;
            data.domain = url.getHost();

            data.title = extractMetaProperty(pageSource, "og:title");
            if (data.title == null || data.title.isEmpty()) {
                data.title = extractTagContent(pageSource, "title");
            }
            if (data.title == null || data.title.isEmpty()) {
                data.title = url.getHost();
            }

            data.description = extractMetaProperty(pageSource, "og:description");
            if (data.description == null || data.description.isEmpty()) {
                data.description = extractMetaName(pageSource, "description");
            }

            data.imageUrl = extractMetaProperty(pageSource, "og:image");
            if (data.imageUrl == null || data.imageUrl.isEmpty()) {
                data.imageUrl = extractMetaProperty(pageSource, "twitter:image");
            }

            if (data.imageUrl != null && !data.imageUrl.isEmpty()) {
                if (data.imageUrl.startsWith("//")) {
                    data.imageUrl = "https:" + data.imageUrl;
                } else if (data.imageUrl.startsWith("/")) {
                    data.imageUrl = "https://" + url.getHost() + data.imageUrl;
                }
            }

            return data;
        } catch (Exception e) {
            Log.w(TAG, "Failed to parse OpenGraph for " + urlString + ": " + e.getMessage());
            return createFallbackData(urlString);
        }
    }

    private LinkPreviewData createFallbackData(String urlString) {
        LinkPreviewData data = new LinkPreviewData();
        data.url = urlString;
        try {
            data.domain = new URL(urlString).getHost();
        } catch (Exception e) {
            data.domain = urlString;
        }
        data.title = data.domain;
        return data;
    }

    private String extractMetaProperty(String html, String property) {
        try {
            Pattern p = Pattern.compile("<meta\\s+[^>]*property=[\"']" + Pattern.quote(property) + "[\"']\\s+[^>]*content=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(html);
            if (m.find()) return m.group(1).trim();

            Pattern p2 = Pattern.compile("<meta\\s+[^>]*content=[\"']([^\"']+)[\"']\\s+[^>]*property=[\"']" + Pattern.quote(property) + "[\"']", Pattern.CASE_INSENSITIVE);
            Matcher m2 = p2.matcher(html);
            if (m2.find()) return m2.group(1).trim();
        } catch (Exception ignored) {}
        return null;
    }

    private String extractMetaName(String html, String name) {
        try {
            Pattern p = Pattern.compile("<meta\\s+[^>]*name=[\"']" + Pattern.quote(name) + "[\"']\\s+[^>]*content=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(html);
            if (m.find()) return m.group(1).trim();

            Pattern p2 = Pattern.compile("<meta\\s+[^>]*content=[\"']([^\"']+)[\"']\\s+[^>]*name=[\"']" + Pattern.quote(name) + "[\"']", Pattern.CASE_INSENSITIVE);
            Matcher m2 = p2.matcher(html);
            if (m2.find()) return m2.group(1).trim();
        } catch (Exception ignored) {}
        return null;
    }

    private String extractTagContent(String html, String tag) {
        try {
            Pattern p = Pattern.compile("<" + Pattern.quote(tag) + "[^>]*>(.*?)</" + Pattern.quote(tag) + ">", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Matcher m = p.matcher(html);
            if (m.find()) {
                return m.group(1).replaceAll("<[^>]+>", "").trim();
            }
        } catch (Exception ignored) {}
        return null;
    }
}
