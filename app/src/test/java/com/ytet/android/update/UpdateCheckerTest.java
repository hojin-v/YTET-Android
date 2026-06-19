package com.ytet.android.update;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class UpdateCheckerTest {
    @Test
    public void acceptsOnlyFormalVersionTags() {
        assertTrue(UpdateChecker.isStableRelease("v1.2.3", "YTET Android v1.2.3", false, false));
        assertFalse(UpdateChecker.isStableRelease("nightly", "YTET Android Nightly", false, true));
        assertFalse(UpdateChecker.isStableRelease("v1.2.3-beta", "YTET beta", false, false));
        assertFalse(UpdateChecker.isStableRelease("v1.2.3", "YTET Android RC", false, false));
        assertFalse(UpdateChecker.isStableRelease("v1.2.3", "YTET Android v1.2.3", true, false));
        assertTrue(UpdateChecker.isStableRelease("v1.2.4", "YTET Android March release", false, false));
    }

    @Test
    public void comparesStableTagsAgainstAndroidVersionNames() {
        assertTrue(UpdateChecker.compareStableTagToCurrentVersion("v0.1.4", "0.1.3-android") > 0);
        assertEquals(0, UpdateChecker.compareStableTagToCurrentVersion("v0.1.3", "0.1.3-android"));
        assertTrue(UpdateChecker.compareStableTagToCurrentVersion("v0.1.2", "0.1.3-android") < 0);
    }

    @Test
    public void acceptsOnlyYtetApkAssets() {
        assertTrue(UpdateChecker.isApkAssetName("YTET-Android-v0.1.4.apk"));
        assertFalse(UpdateChecker.isApkAssetName("YTET-Android-v0.1.4-debug.apk"));
        assertFalse(UpdateChecker.isApkAssetName("app-release.apk"));
        assertFalse(UpdateChecker.isApkAssetName("YTET-Android-v0.1.4-android-debug.zip"));
        assertFalse(UpdateChecker.isApkAssetName("YTET-Android-nightly-debug.apk"));
        assertFalse(UpdateChecker.isApkAssetName("YTET-Android-v0.1.4-beta-debug.apk"));
        assertFalse(UpdateChecker.isApkAssetName("source-code.zip"));
    }

    @Test
    public void doesNotTreatMalformedVersionsAsUpdates() {
        assertEquals(0, UpdateChecker.compareStableTagToCurrentVersion("nightly", "0.1.3-android"));
        assertEquals(0, UpdateChecker.compareStableTagToCurrentVersion("v0.1", "0.1.3-android"));
        assertEquals(0, UpdateChecker.compareStableTagToCurrentVersion("v0.1.4", "dev"));
    }
}
