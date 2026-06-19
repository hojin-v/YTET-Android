package com.ytet.android.update;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UpdateChecker {
    private static final String RELEASES_API_URL = "https://api.github.com/repos/hojin-v/YTET-Android/releases";
    private static final Pattern STABLE_TAG_PATTERN = Pattern.compile("^v(\\d+)\\.(\\d+)\\.(\\d+)$");
    private static final Pattern CURRENT_VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)");
    private static final Pattern STABLE_APK_ASSET_PATTERN = Pattern.compile("^ytet-android-v\\d+\\.\\d+\\.\\d+\\.apk$");
    private static final Pattern UNSTABLE_MARKER_PATTERN = Pattern.compile(
            "(^|[^a-z0-9])(nightly|alpha|beta|rc|dev|preview)([^a-z0-9]|$)",
            Pattern.CASE_INSENSITIVE
    );
    private static final int CONNECT_TIMEOUT_MS = 8000;
    private static final int READ_TIMEOUT_MS = 8000;

    public UpdateInfo checkForStableUpdate(String currentVersionName) throws IOException, JSONException {
        HttpURLConnection connection = (HttpURLConnection) new URL(RELEASES_API_URL).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setRequestProperty("User-Agent", "YTET-Android-Updater");

        int status = connection.getResponseCode();
        InputStream stream = status >= 200 && status < 300
                ? connection.getInputStream()
                : connection.getErrorStream();
        String body = readBody(stream);
        connection.disconnect();

        if (status < 200 || status >= 300) {
            throw new IOException("GitHub releases request failed: " + status);
        }
        return latestStableUpdateFromJson(body, currentVersionName);
    }

    public static UpdateInfo latestStableUpdateFromJson(String releasesJson, String currentVersionName) throws JSONException {
        JSONArray releases = new JSONArray(releasesJson == null ? "[]" : releasesJson);
        UpdateInfo latestUpdate = null;
        for (int index = 0; index < releases.length(); index++) {
            JSONObject release = releases.getJSONObject(index);
            String tagName = release.optString("tag_name", "");
            String releaseName = release.optString("name", "");
            if (!isStableRelease(tagName, releaseName, release.optBoolean("draft"), release.optBoolean("prerelease"))) {
                continue;
            }
            if (compareStableTagToCurrentVersion(tagName, currentVersionName) <= 0) {
                continue;
            }
            JSONObject asset = findApkAsset(release.optJSONArray("assets"));
            if (asset == null) {
                continue;
            }
            UpdateInfo update = new UpdateInfo(
                    tagName,
                    versionNameFromTag(tagName),
                    releaseName.isEmpty() ? tagName : releaseName,
                    release.optString("html_url", ""),
                    asset.optString("name", "YTET.apk"),
                    asset.optString("browser_download_url", "")
            );
            if (latestUpdate == null || compareStableTagToCurrentVersion(tagName, latestUpdate.versionName()) > 0) {
                latestUpdate = update;
            }
        }
        return latestUpdate;
    }

    static boolean isStableRelease(String tagName, String releaseName, boolean draft, boolean prerelease) {
        if (draft || prerelease) {
            return false;
        }
        if (!STABLE_TAG_PATTERN.matcher(tagName == null ? "" : tagName.trim()).matches()) {
            return false;
        }
        String combined = ((tagName == null ? "" : tagName) + " " + (releaseName == null ? "" : releaseName))
                .toLowerCase();
        return !UNSTABLE_MARKER_PATTERN.matcher(combined).find();
    }

    static boolean isApkAssetName(String assetName) {
        String lower = assetName == null ? "" : assetName.toLowerCase();
        return STABLE_APK_ASSET_PATTERN.matcher(lower).matches()
                && !UNSTABLE_MARKER_PATTERN.matcher(lower).find();
    }

    public static int compareStableTagToCurrentVersion(String tagName, String currentVersionName) {
        int[] release = parseStableTag(tagName);
        int[] current = parseCurrentVersion(currentVersionName);
        if (release == null || current == null) {
            return 0;
        }
        for (int index = 0; index < release.length; index++) {
            if (release[index] != current[index]) {
                return Integer.compare(release[index], current[index]);
            }
        }
        return 0;
    }

    private static JSONObject findApkAsset(JSONArray assets) throws JSONException {
        if (assets == null) {
            return null;
        }
        for (int index = 0; index < assets.length(); index++) {
            JSONObject asset = assets.getJSONObject(index);
            String name = asset.optString("name", "");
            String url = asset.optString("browser_download_url", "");
            if (isApkAssetName(name) && url.startsWith("https://")) {
                return asset;
            }
        }
        return null;
    }

    private static String versionNameFromTag(String tagName) {
        return tagName == null ? "" : tagName.replaceFirst("^v", "");
    }

    private static int[] parseStableTag(String tagName) {
        Matcher matcher = STABLE_TAG_PATTERN.matcher(tagName == null ? "" : tagName.trim());
        if (!matcher.matches()) {
            return null;
        }
        return new int[]{
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)),
                Integer.parseInt(matcher.group(3))
        };
    }

    private static int[] parseCurrentVersion(String currentVersionName) {
        Matcher matcher = CURRENT_VERSION_PATTERN.matcher(currentVersionName == null ? "" : currentVersionName.trim());
        if (!matcher.find()) {
            return null;
        }
        return new int[]{
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)),
                Integer.parseInt(matcher.group(3))
        };
    }

    private static String readBody(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        }
        return body.toString();
    }
}
