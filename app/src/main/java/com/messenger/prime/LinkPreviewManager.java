package com.messenger.prime;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.LruCache;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

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

    private final LruCache<String, LinkPreviewData> cache = new LruCache<>(200);
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .followRedirects(true)
            .build();

    public static synchronized LinkPreviewManager getInstance() {
        if (instance == null) {
            instance = new LinkPreviewManager();
        }
        return instance;
    }

    private LinkPreviewManager() {}

    public void fetchPreview(String rawUrl, Callback callback) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) return;

        final String formattedUrl = rawUrl.startsWith("http") ? rawUrl : "https://" + rawUrl;

        // Check LRU Cache first for instant, lag-free loading
        LinkPreviewData cached = cache.get(formattedUrl);
        if (cached != null) {
            if (callback != null) {
                callback.onDataLoaded(cached);
            }
            return;
        }

        // Return fallback immediately while fetching OpenGraph asynchronously
        if (callback != null) {
            LinkPreviewData fallback = createFallbackData(formattedUrl);
            callback.onDataLoaded(fallback);
        }

        // Fetch OpenGraph in background thread safely
        executor.execute(() -> {
            LinkPreviewData data = parseOpenGraph(formattedUrl);
            if (data != null) {
                cache.put(formattedUrl, data);
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onDataLoaded(data);
                    }
                });
            }
        });
    }

    private LinkPreviewData parseOpenGraph(String urlString) {
        try {
            URL url = new URL(urlString);
            String host = url.getHost().toLowerCase(Locale.ROOT);

            // YouTube special oEmbed handler
            if (host.contains("youtube.com") || host.contains("youtu.be")) {
                return parseYouTubePreview(urlString);
            }

            Request request = new Request.Builder()
                    .url(urlString)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    return createFallbackData(urlString);
                }

                String html = response.body().string();
                Document doc = Jsoup.parse(html, urlString);

                LinkPreviewData data = new LinkPreviewData();
                data.url = urlString;
                data.domain = url.getHost();

                // Extract OpenGraph / Twitter / Standard Meta Tags with Jsoup
                data.title = getMetaTagContent(doc, "og:title", "twitter:title");
                if (data.title == null || data.title.isEmpty()) {
                    data.title = doc.title();
                }
                if (data.title == null || data.title.isEmpty()) {
                    data.title = url.getHost();
                }

                data.description = getMetaTagContent(doc, "og:description", "twitter:description", "description");

                data.imageUrl = getMetaTagContent(doc, "og:image", "og:image:secure_url", "twitter:image", "twitter:image:src");
                if (data.imageUrl != null && !data.imageUrl.isEmpty()) {
                    if (data.imageUrl.startsWith("//")) {
                        data.imageUrl = "https:" + data.imageUrl;
                    } else if (data.imageUrl.startsWith("/")) {
                        data.imageUrl = "https://" + url.getHost() + data.imageUrl;
                    }
                }

                return data;
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to parse OpenGraph via Jsoup for " + urlString + ": " + e.getMessage());
            return createFallbackData(urlString);
        }
    }

    private String getMetaTagContent(Document doc, String... keys) {
        for (String key : keys) {
            Elements elements = doc.select("meta[property='" + key + "'], meta[name='" + key + "']");
            for (Element element : elements) {
                String content = element.attr("content");
                if (content != null && !content.trim().isEmpty()) {
                    return content.trim();
                }
            }
        }
        return null;
    }

    private LinkPreviewData parseYouTubePreview(String urlString) {
        try {
            String oembedUrl = "https://www.youtube.com/oembed?url=" + java.net.URLEncoder.encode(urlString, "UTF-8") + "&format=json";
            Request request = new Request.Builder().url(oembedUrl).build();
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonStr = response.body().string();
                    org.json.JSONObject json = new org.json.JSONObject(jsonStr);
                    LinkPreviewData data = new LinkPreviewData();
                    data.url = urlString;
                    data.domain = "youtube.com";
                    data.title = json.optString("title", "YouTube Video");
                    data.description = json.optString("author_name", "");
                    data.imageUrl = json.optString("thumbnail_url", "");
                    return data;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to parse YouTube oEmbed for " + urlString + ": " + e.getMessage());
        }
        return createFallbackData(urlString);
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
}
