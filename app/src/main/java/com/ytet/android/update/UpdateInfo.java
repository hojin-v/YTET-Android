package com.ytet.android.update;

public final class UpdateInfo {
    private final String tagName;
    private final String versionName;
    private final String releaseName;
    private final String releaseUrl;
    private final String apkName;
    private final String apkUrl;

    public UpdateInfo(
            String tagName,
            String versionName,
            String releaseName,
            String releaseUrl,
            String apkName,
            String apkUrl
    ) {
        this.tagName = clean(tagName);
        this.versionName = clean(versionName);
        this.releaseName = clean(releaseName);
        this.releaseUrl = clean(releaseUrl);
        this.apkName = clean(apkName);
        this.apkUrl = clean(apkUrl);
    }

    public String tagName() {
        return tagName;
    }

    public String versionName() {
        return versionName;
    }

    public String releaseName() {
        return releaseName;
    }

    public String releaseUrl() {
        return releaseUrl;
    }

    public String apkName() {
        return apkName;
    }

    public String apkUrl() {
        return apkUrl;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
