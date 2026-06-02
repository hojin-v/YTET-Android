package com.ytet.android.stream;

import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class StationCatalogTest {
    @Test
    public void includesArtistAndGenreMixesWithStablePlaylistUrls() {
        List<MusicStation> stations = StationCatalog.defaultStations();

        assertTrue(stations.stream().anyMatch(MusicStation::isArtistMix));
        assertTrue(stations.stream().anyMatch(MusicStation::isGenreMix));
        assertTrue(stations.stream().allMatch(station -> station.streamUrl().startsWith("https://somafm.com/m3u/")));
        assertTrue(stations.stream().allMatch(station -> station.streamUrl().endsWith(".m3u")));
    }

    @Test
    public void stationIdsAreUnique() {
        List<MusicStation> stations = StationCatalog.defaultStations();
        Set<String> ids = new HashSet<>();

        for (MusicStation station : stations) {
            assertFalse(ids.contains(station.id()));
            ids.add(station.id());
        }
    }
}
