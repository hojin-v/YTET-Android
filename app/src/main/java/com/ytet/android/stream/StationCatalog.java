package com.ytet.android.stream;

import java.util.Arrays;
import java.util.List;

public final class StationCatalog {
    private StationCatalog() {
    }

    public static List<MusicStation> defaultStations() {
        return Arrays.asList(
                new MusicStation(
                        "bonobo-night-drive",
                        "Bonobo Night Drive",
                        "아티스트 믹스",
                        "Bonobo 감성의 다운템포",
                        "느린 그루브와 전자음 위주로 홈 첫 화면에서 바로 틀 수 있는 믹스입니다.",
                        "https://somafm.com/m3u/groovesalad.m3u",
                        0xFFB84D5A
                ),
                new MusicStation(
                        "nujabes-late-room",
                        "Nujabes Late Room",
                        "아티스트 믹스",
                        "재즈 힙합과 밤의 비트",
                        "차분한 힙합 질감과 라운지 비트를 묶은 추천 스테이션입니다.",
                        "https://somafm.com/m3u/beatblender.m3u",
                        0xFFD08A2E
                ),
                new MusicStation(
                        "downtempo-focus",
                        "Downtempo Focus",
                        "장르 믹스",
                        "Ambient / Downtempo",
                        "집중할 때 흐름을 깨지 않는 부드러운 장르 믹스입니다.",
                        "https://somafm.com/m3u/groovesalad.m3u",
                        0xFF48A078
                ),
                new MusicStation(
                        "deep-house-pulse",
                        "Deep House Pulse",
                        "장르 믹스",
                        "Deep House / Chill",
                        "늦은 밤에도 부담 없이 이어지는 하우스 기반 스트리밍입니다.",
                        "https://somafm.com/m3u/beatblender.m3u",
                        0xFF7D5CDB
                ),
                new MusicStation(
                        "ambient-orbit",
                        "Ambient Orbit",
                        "장르 믹스",
                        "Deep Ambient",
                        "긴 호흡의 전자음과 공간감 있는 사운드 중심의 믹스입니다.",
                        "https://somafm.com/m3u/deepspaceone.m3u",
                        0xFF3F7AD8
                )
        );
    }
}
