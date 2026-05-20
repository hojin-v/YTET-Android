<div align="center">

# YTET Android

YouTube Extractor Toolkit for Android

YouTube URL 하나로 오디오와 영상을 Android 기기에 저장하는 모바일 추출 도구입니다.

[![CI](https://github.com/hojin-v/YTET-Android/actions/workflows/ci.yml/badge.svg)](https://github.com/hojin-v/YTET-Android/actions/workflows/ci.yml)
[![Release](https://github.com/hojin-v/YTET-Android/actions/workflows/release.yml/badge.svg)](https://github.com/hojin-v/YTET-Android/actions/workflows/release.yml)
[![Latest Release](https://img.shields.io/github/v/release/hojin-v/YTET-Android?label=release)](https://github.com/hojin-v/YTET-Android/releases)
![Platform](https://img.shields.io/badge/platform-Android-3DDC84)
![Java](https://img.shields.io/badge/java-17-ED8B00)

[Download](https://github.com/hojin-v/YTET-Android/releases) · [Features](#features) · [Tech Stack](#tech-stack) · [Caution](#caution)

</div>

## Overview

YTET Android는 YouTube 링크를 오디오 파일이나 영상 파일로 저장합니다.

Android 저장소 권한 모델에 맞춰 사용자가 저장 폴더를 직접 선택하고, 추출 작업은 foreground service에서 진행됩니다. 추출 엔진은 프로젝트 내부의 `YtDlpProcessEngine`이 담당하며, Android용 `yt-dlp`와 `ffmpeg` 실행 파일을 앱 assets에서 복사한 뒤 직접 실행합니다.

| Mode | Best For | Output |
| --- | --- | --- |
| Audio | 음악, 강의, 플레이리스트 정리 | `M4A (AAC)`, `Original Opus`, `MP3` |
| Video | 모바일 보관, 고화질 저장 | 최고 품질 `MKV` 또는 호환 우선 `MP4` |
| Subtitles | 선택형 한국어/영어 등록 자막 | 영상 내 자막 트랙 + `.srt` |
| Multi Audio | 선택형 다국어 오디오 | 원본 오디오 + 한국어 오디오 트랙 |

## Features

| Feature | Description |
| --- | --- |
| URL 기반 추출 | YouTube URL을 넣고 저장 폴더를 고르면 추출을 시작합니다. |
| Android 폴더 선택 | Storage Access Framework로 사용자가 지정한 폴더에 결과를 저장합니다. |
| 백그라운드 진행 | foreground service와 알림으로 추출 진행 상태를 표시합니다. |
| 자동 파일명 | 오디오는 `artist - title`, 영상은 `channel - title` 형식으로 저장합니다. |
| 저용량 영상 옵션 | 1080p, 720p, 480p 이하 품질로 저장할 수 있습니다. |
| 선택형 자막 | `자막 포함` 선택 시 등록된 한국어/영어 자막을 저장하고 영상에도 삽입합니다. |
| 선택형 다중 오디오 | `다중 오디오 포함` 선택 시 원본/한국어 오디오 트랙을 함께 보관합니다. |

## Quick Start

1. [Releases](https://github.com/hojin-v/YTET-Android/releases)에서 최신 `YTET-Android-버전-debug.apk` 또는 ZIP을 받습니다.
2. APK를 Android 기기에 설치합니다.
3. YouTube URL을 입력합니다.
4. `음원` 또는 `영상`을 선택합니다.
5. 저장 폴더와 포맷 또는 품질을 고릅니다.
6. 영상일 경우 `자막 포함`, `다중 오디오 포함`을 필요에 맞게 선택합니다.
7. `추출`을 누릅니다.

> 현재 저장소는 `io.github.junkfood02.youtubedl-android` 같은 GPL Android wrapper에 의존하지 않습니다. 실제 추출이 가능한 APK를 배포하려면 `app/src/main/assets/runtime/<ABI>/`에 Android 호환 `yt-dlp`, `ffmpeg` 런타임을 포함해야 합니다.

## Audio

| Format | When to Use |
| --- | --- |
| `M4A (AAC)` | 기본 추천 포맷 |
| `Original Opus` | YouTube 원본 오디오에 가깝고 용량 효율이 좋은 포맷 |
| `MP3` | 오래된 기기나 앱과의 호환성이 필요한 경우 |

오디오 추출 결과에는 가능한 경우 다음 정보가 포함됩니다.

- 제목
- 아티스트
- 원본 URL
- 커버 이미지
- 가사 또는 자막 기반 텍스트 정보

메타데이터 기록은 `yt-dlp`와 `ffmpeg` 후처리에 위임합니다.

## Video

| Quality Option | Container | Tags |
| --- | --- | --- |
| 원본 최고품질 | `MKV` | 4K/8K, AV1/VP9, Opus, 파일 용량 큼 |
| 1080p MP4 | `MP4` | 호환 우선, H.264/AAC, 최대 1080p |
| 720p MP4 | `MP4` | 저용량, 모바일 보관, 작은 글자 약함 |
| 480p MP4 | `MP4` | 최소용량, 확인용, 큰 화면 약함 |

YouTube의 4K 이상 영상은 보통 H.264 MP4가 아니라 VP9 또는 AV1 같은 고효율 코덱으로 제공됩니다. 그래서 최고 품질 결과는 `.mkv`가 될 수 있습니다.

호환성이 더 중요하면 1080p 이하 MP4 옵션을 권장합니다.

## Subtitles & Audio Tracks

영상 모드에서 `자막 포함`을 선택하면 업로더가 등록한 한국어와 영어 자막만 저장합니다.

저장 가능한 자막이 있으면 영상 파일 안에 자막 트랙을 넣고, 플레이어 호환을 위해 같은 이름의 `.srt` 파일도 함께 저장합니다.

자동 생성 자막만 있는 영상은 기본적으로 자막을 저장하지 않습니다.

영상 모드에서 `다중 오디오 포함`을 선택하면 여러 오디오 언어가 제공되는 영상의 원본 오디오를 유지합니다. 원본 오디오가 한국어가 아니고 YouTube가 한국어 오디오 트랙을 제공하면, 한국어 오디오도 함께 추가합니다.

## Runtime Assets

직접 구현 방식은 APK 안에 어떤 런타임을 넣을지 프로젝트가 직접 결정합니다.

```text
app/src/main/assets/runtime/arm64-v8a/yt-dlp
app/src/main/assets/runtime/arm64-v8a/ffmpeg
app/src/main/assets/runtime/armeabi-v7a/yt-dlp
app/src/main/assets/runtime/armeabi-v7a/ffmpeg
app/src/main/assets/runtime/x86_64/yt-dlp
app/src/main/assets/runtime/x86_64/ffmpeg
```

`yt-dlp`는 Android에서 실행 가능한 단일 실행 파일이거나, 내장 Python/yt-dlp 런타임을 호출하는 shell wrapper일 수 있습니다. 앱은 현재 기기의 ABI에 맞는 파일을 내부 저장소로 복사하고 실행 권한을 부여한 뒤 `ProcessBuilder`로 실행합니다.

## Output Rules

```text
audio:    artist - title.ext
video:    channel - title.ext
subtitle: channel - title.ko.srt
subtitle: channel - title.en.srt
```

Android 결과 화면은 저장소로 복사된 파일과 요청 조건만 표시합니다. Windows 앱처럼 화질/코덱/오디오/자막 스트림을 재검증해 표시하는 단계는 아직 포팅하지 않았으며, 확인되지 않은 값은 기본값처럼 표시하지 않습니다.

## How It Works

```mermaid
flowchart LR
    A[YouTube URL] --> B[Android foreground service]
    B --> C[Install runtime assets]
    C --> D{Mode}
    D -->|Audio| E[Run yt-dlp audio command]
    E --> F[FFmpeg metadata and format post-processing]
    F --> G[Save through Storage Access Framework]
    D -->|Video| H[Run yt-dlp video command]
    H --> I[FFmpeg merge and optional subtitle muxing]
    I --> G
```

## Tech Stack

| Layer | Technology | Role |
| --- | --- | --- |
| App | Android SDK, Java | Native mobile UI and foreground service |
| Storage | Storage Access Framework | User-selected output folders |
| Extraction | Direct `ProcessBuilder` runtime execution | Runs Android-compatible `yt-dlp` |
| Media Processing | Android-compatible FFmpeg binary | Audio conversion, video merge, subtitle muxing |
| Runtime Packaging | Project-managed assets | Avoids GPL Android wrapper dependency |
| Automation | GitHub Actions | CI, Android build, release publishing |

## Build From Source

```bash
./gradlew assembleDebug
```

For CLI builds without Android Studio, set `ANDROID_HOME` or `sdk.dir` so Gradle can find Android SDK Platform 36 and Build Tools 36.0.0.

```bash
scripts/verify_static.sh
./gradlew clean :app:assembleDebug
```

## Release Pipeline

```mermaid
flowchart TD
    A[Push to main] --> B[CI]
    B --> C[Android debug build]
    C --> D[Nightly prerelease]
    E[Push v* tag] --> F[CI]
    F --> G[Android debug build]
    G --> H[Versioned GitHub Release]
```

Every release build uploads:

- `YTET-Android-버전-debug.apk`
- `YTET-Android-버전-android-debug.zip`

## Caution

- 권한이 있는 콘텐츠에만 사용하세요.
- YouTube 서비스 약관과 지역 법규를 확인해야 합니다.
- 영상 제공 품질, 자막, 다중 오디오 여부는 YouTube와 업로더 설정에 따라 달라집니다.
- Android 호환 `yt-dlp`, `ffmpeg` 런타임을 포함하지 않은 APK는 추출 시작 시 런타임 누락 오류를 표시합니다.
- 런타임 바이너리를 배포할 경우 각 바이너리의 라이선스와 소스 제공 의무를 확인해야 합니다.
- 처음 설치하는 APK는 Android 보안 경고가 표시될 수 있습니다. 신뢰할 수 있는 출처에서 받은 파일인지 확인한 뒤 설치하세요.
