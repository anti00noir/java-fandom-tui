package com.fandomtui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Client for interacting with Fandom's MediaWiki API.
 * Uses the standard /api.php endpoint which is stable across all MediaWiki instances.
 */
public class FandomApiClient {
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private String wikiBaseUrl;
    
    // MediaWiki API action parameters
    private static final String API_PATH = "/api.php";
    private static final String ACTION_QUERY = "query";
    private static final String ACTION_PARSE = "parse";
    
    public FandomApiClient() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Sets the base URL for the Fandom wiki.
     * Removes trailing slashes and ensures proper URL format.
     */
    public void setWikiBaseUrl(String wikiUrl) {
        // Clean up the URL
        wikiUrl = wikiUrl.trim();
        if (!wikiUrl.startsWith("https://") && !wikiUrl.startsWith("http://")) {
            wikiUrl = "https://" + wikiUrl;
        }
        if (wikiUrl.endsWith("/")) {
            wikiUrl = wikiUrl.substring(0, wikiUrl.length() - 1);
        }
        this.wikiBaseUrl = wikiUrl;
    }
    
    public String getWikiBaseUrl() {
        return wikiBaseUrl;
    }
    
    /**
     * Searches for articles using the MediaWiki opensearch API.
     * Returns a list of SearchResult objects containing title and snippet.
     * 
     * API Documentation: https://www.mediawiki.org/wiki/API:Opensearch
     */
    public List<SearchResult> search(String query) throws IOException {
        if (wikiBaseUrl == null) {
            throw new IllegalStateException("Wiki URL not set");
        }
        
        // Build the API URL for opensearch
        HttpUrl url = HttpUrl.parse(wikiBaseUrl + API_PATH).newBuilder()
                .addQueryParameter("action", "opensearch")
                .addQueryParameter("search", query)
                .addQueryParameter("limit", "20")
                .addQueryParameter("namespace", "0")  // Only main namespace
                .addQueryParameter("format", "json")
                .addQueryParameter("redirects", "resolve")
                .build();
        
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "FandomTUI/1.0")
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("API request failed: " + response.code());
            }
            
            String responseBody = response.body().string();
            return parseSearchResults(responseBody);
        }
    }
    
    /**
     * Parses the opensearch API response format:
     * [query, [titles], [descriptions], [urls]]
     */
    private List<SearchResult> parseSearchResults(String json) throws IOException {
        JsonNode root = objectMapper.readTree(json);
        List<SearchResult> results = new ArrayList<>();
        
        // Opensearch returns an array where:
        // index 0 = search query
        // index 1 = array of titles
        // index 2 = array of descriptions (snippets)
        // index 3 = array of URLs
        if (root.isArray() && root.size() >= 4) {
            JsonNode titles = root.get(1);
            JsonNode snippets = root.get(2);
            JsonNode urls = root.get(3);
            
            for (int i = 0; i < titles.size(); i++) {
                String title = titles.get(i).asText();
                String snippet = snippets.get(i).asText();
                String url = urls.get(i).asText();
                results.add(new SearchResult(title, snippet, url));
            }
        }
        
        return results;
    }
    
    /**
     * Fetches article content in wikitext format using the parse API.
     * 
     * API Documentation: https://www.mediawiki.org/wiki/API:Parse
     */
    public ArticleContent getArticleContent(String pageTitle) throws IOException {
        if (wikiBaseUrl == null) {
            throw new IllegalStateException("Wiki URL not set");
        }
        
        // Build the API URL for parsing a page
        HttpUrl url = HttpUrl.parse(wikiBaseUrl + API_PATH).newBuilder()
                .addQueryParameter("action", "parse")
                .addQueryParameter("page", pageTitle)
                .addQueryParameter("prop", "wikitext")
                .addQueryParameter("format", "json")
                .addQueryParameter("redirects", "1")  // Follow redirects
                .build();
        
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "FandomTUI/1.0")
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("API request failed: " + response.code());
            }
            
            String responseBody = response.body().string();
            return parseArticleContent(pageTitle, responseBody);
        }
    }
    
    /**
     * Parses the parse API response to extract wikitext content.
     */
    private ArticleContent parseArticleContent(String pageTitle, String json) throws IOException {
        JsonNode root = objectMapper.readTree(json);
        
        // Check for errors
        if (root.has("error")) {
            String errorInfo = root.get("error").get("info").asText();
            throw new IOException("API Error: " + errorInfo);
        }
        
        JsonNode parse = root.get("parse");
        if (parse == null) {
            throw new IOException("Invalid response: no parse data");
        }
        
        // Extract the actual page title (in case of redirects)
        String actualTitle = parse.get("title").asText();
        
        // Get the wikitext content
        String wikitext = parse.get("wikitext").get("*").asText();
        
        return new ArticleContent(actualTitle, wikitext);
    }
    
    /**
     * Data class for search results.
     */
    public record SearchResult(String title, String snippet, String url) {
        @Override
        public String toString() {
            return title + (snippet.isEmpty() ? "" : " - " + snippet);
        }
    }
    
    /**
     * Data class for article content.
     */
    public record ArticleContent(String title, String wikitext) {}
}