# Codex handoff

이 문서는 새 환경의 Codex 세션이 기존 대화 맥락을 최대한 이어받기 위한 작업 인수인계 문서입니다.
GitHub에 올라간 소스만으로는 알기 어려운 의사결정, 로컬 파일, 릴리즈 규칙, 검증 방법을 함께 기록합니다.

## 현재 프로젝트 상태

- 저장소: `hojin-v/YTET-Android`
- 로컬 경로: `/home/hojin/Projects/youtube-audio-extractor-android`
- 앱 이름: `YTET`
- 플랫폼: Android native Java app
- 최신 기준 태그: `v1.3.5`
- 최신 기준 커밋: `e5ed9ef Polish mini player and sleep timer controls`
- 하단 탭: `홈`, `내 음악`, `추출기`
- 주요 목표: 외부 라디오/스트리밍이 아니라 디바이스에 저장된 음악을 제대로 스캔, 분류, 표시, 재생하는 앱

## 사용자 선호와 제품 방향

- 스트리밍은 외부 URL이나 라디오가 아니라 로컬 파일 재생을 뜻한다.
- 추천 스테이션은 사용자가 가진 음악만 기반으로 해야 한다.
- `밤에 어울리는 음악`, `차분한 힙합`처럼 보유 여부를 보장할 수 없는 테마 분류는 피한다.
- 홈 추천은 기본적으로 `Download/YTET/Music/` 경로의 음악만 반영한다.
- 내 음악은 상단 드롭다운에서 `보관함`과 `기기 파일`을 전환한다.
  - `보관함`: YTET 기본 음악 경로만 표시
  - `기기 파일`: MediaStore가 감지한 전체 음악 표시
- 설정 탭은 제거된 상태다. 재스캔은 내 음악의 pull-to-refresh로 처리한다.
- 사용자에게 보이는 UI는 Spotify, YouTube Music 같은 상용 앱의 밀도와 동작감을 참고한다.
- 텍스트 버튼보다 아이콘을 선호한다. 상태 차이는 배경보다 아이콘 색상으로 표현한다.
- 임시방편으로 대충 처리하지 말고, 새 기능이 기존 재생/스캔/전환 동작을 깨지 않는지 확인한다.

## 주요 구현 요약

### 재생

- `PlaybackService`가 foreground media playback을 담당한다.
- 앱을 나가거나 화면을 잠가도 재생이 유지되어야 한다.
- 알림바와 잠금화면에서 재생/일시정지, 이전, 다음, 셔플, 반복 상태가 표시되어야 한다.
- 동작 불가능한 이전/다음/셔플은 버튼을 숨기지 않고 흐린 색으로 표시한다.
- 미니 플레이어와 전체 플레이어는 커버 이미지 색감 기반 배경을 사용한다.
- 미니 플레이어에는 하단 재생바가 있다.
- 미니 플레이어에서 재생 버튼을 제외한 영역을 좌우로 스와이프하면 이전/다음 곡으로 이동한다.
  - 이전/다음 곡이 없으면 약간 당겨지는 rubber-band 느낌만 주고 실제 이동하지 않는다.
- 긴 제목, 아티스트, 앨범 텍스트는 줄바꿈하지 않고 한 줄 marquee로 부드럽게 이동한다.
- marquee 비네팅은 텍스트가 실제로 영역을 넘어갈 때만 보여야 한다.

### 전체 플레이어

- 전체 화면 플레이어는 아래로 드래그하면 Spotify처럼 내려가며 닫힌다.
- 드래그 중에는 플레이어의 상단 좌우 모서리가 라운딩 처리된다.
- 드래그 중에도 재생바와 재생시간은 멈추지 않아야 한다.
- 알림바 영역은 뒤 화면이 그대로 비치지 않도록 플레이어 배경 계열의 더 어두운 색으로 덮는다.
- 슬립 타이머는 시간/분 2개 다이얼로 설정한다.
  - 시간: `00`부터 `99`
  - 분: `00`부터 `59`
  - 가운데 `:` 표시
  - 오른쪽 체크 버튼으로 확정
  - 타이머 버튼을 다시 누르면 설정을 취소하고 기존 값으로 복귀
- 타이머가 켜져도 커버, 제목, 아티스트, 앨범 영역 위치가 튀지 않도록 고정한다.

### 내 음악

- `DeviceMusicLibrary`와 Android `MediaStore`가 라이브러리 스캔을 담당한다.
- `RecyclerView` 기반으로 전환되어 많은 곡에서도 전체 View를 매번 다시 그리지 않도록 한다.
- 전체/앨범/아티스트/재생목록 필터가 있다.
- 정렬은 최신순, 오래된순, 이름순, 많이 재생한순, 적게 재생한순을 제공한다.
- 필터 행은 좌우 스크롤이 가능해야 하며, 필터 탭을 누르면 선택 항목이 중앙에 오도록 부드럽게 이동한다.
- pull-to-refresh는 문구가 아니라 회전 화살표 모션으로만 표현한다.
  - 필터 행을 좌우 스크롤할 때 refresh가 끼어들면 안 된다.
  - refresh 제스처 중 롱프레스 메뉴가 뜨면 안 된다.
- 곡 롱프레스 메뉴에는 순서대로 다음 항목이 표시된다.
  - `다음 곡으로 재생`
  - `현재 재생목록에 추가`
  - `재생목록에 저장`
- `앨범에 추가` 기능은 제거된 상태다.
  - Android Storage Access Framework에서 임의 위치 파일 이동이 까다롭고, 앨범 분류가 폴더 기준과 메타데이터 기준을 함께 써 혼동이 생길 수 있어 제거했다.

### 앨범/아티스트/재생목록

- 앨범을 누르면 바로 재생하지 않고 상세 페이지로 이동한다.
- 앨범 상세 페이지 안의 재생/셔플 버튼으로 재생을 시작한다.
- 아티스트도 바로 재생하지 않고 상세 페이지로 이동한다.
- 아티스트 상세 페이지는 앨범이 2개 이상이면 `전체`보다 `앨범` 단위 표시를 우선한다.
- 앨범 상세 페이지에서 OS 뒤로가기 동작은 앱 종료가 아니라 상세 페이지 닫기여야 한다.
- 상세 페이지의 재생/셔플 버튼은 텍스트 중심이 어색해지지 않도록 아이콘 위치를 세밀하게 조정했다.

### 메타데이터와 검색 보정

- 추출 결과의 표시 이름과 실제 파일명은 다를 수 있다.
- MusicBrainz 검색 보정이 성공하면 제목, 아티스트, 앨범, 트랙 번호 등을 보정한다.
- 여러 아티스트가 있을 때는 표시용 아티스트와 분류용 대표 아티스트를 구분한다.
  - 곡 표시/플레이어/상세 수록곡 표시: 전체 아티스트 표시
  - 앨범/아티스트 분류: 대표 아티스트 사용
- `w`, `w.`, `with` 계열은 `with.`로 정규화한다.
- `ft`, `ft.`, `feat` 계열은 `feat.`로 정규화한다.
- 쉼표로 구분된 아티스트도 대표 아티스트 분류가 깨지지 않는지 확인해야 한다.
- 검색 보정이 성공하면 파일명도 `artist - title`로 바꾸는 방향이 논의되었고 일부 흐름에 반영되어 있다.
- 검색 실패 시에는 원본 YouTube 제목/업로더 기반 값을 유지한다.
- 사용자가 점 3개 메뉴에서 제목, 아티스트, 앨범 정보를 수정할 수 있다.

### 추출기

- YouTube URL 입력은 기존 복사/붙여넣기와 Android 공유 인텐트 둘 다 지원한다.
- YouTube 앱/브라우저에서 공유한 URL은 YTET 추출기 URL 입력창에 자동 입력되어야 한다.
- URL 입력창에서 좌우 드래그로 긴 URL을 확인할 수 있어야 하며, 이때 화면 전체 상하 스크롤이 끼어들면 안 된다.
- 추출은 foreground service로 진행된다.
- 추출 중 취소 버튼을 제공한다.
- 플레이리스트 추출 중 일부 항목이 실패해도 전체 작업을 바로 실패로 끝내지 않고 다음 항목으로 진행한다.
- 결과에서는 실패 항목을 성공 항목보다 먼저 보여줘야 한다.
- 전체가 대부분 실패했는데도 성공처럼 보이는 `부분 성공` UX를 조심해야 한다.
- 플레이리스트 URL은 전체 플레이리스트 추출 토글이 켜져 있으면 순서대로 처리한다.
- 단일 긴 플레이리스트 영상은 분할하지 않고 하나의 파일로 취급한다.
- 제목에 `playlist`, `[playlist]`, `playlist |` 같은 불필요 표현이 있으면 정리하고 MusicBrainz 검색 대상에서 제외한다.

### 업데이트

- 앱 실행 시 GitHub Releases에서 정식 릴리즈 업데이트를 감지하면 팝업을 띄운다.
- nightly/prerelease는 업데이트 대상에서 제외한다.
- 사용자 안내 문구는 간단하게 `새로운 업데이트가 있습니다.`를 사용한다.
- `릴리즈정보` 문구는 제거된 상태다.
- 현재 버전과 새 버전 표기는 둘 다 `v1.3.0` 형식으로 맞춘다.
- 업데이트 버튼을 누르면 `UpdateDownloadService` foreground service가 다운로드 진행률을 표시한다.
- 앱을 홈으로 보내거나 백그라운드로 내려도 업데이트 다운로드 완료 시 설치 요청이 이어져야 한다.
- Android 정책상 앱이 조용히 업데이트를 설치할 수는 없고, 최종 설치는 사용자가 승인해야 한다.

## 기본 경로와 저장소

- 기본 음악 경로: `Download/YTET/Music/`
- 기본 영상 경로: `Download/YTET/Video/`
- 플레이리스트 추출 시 기본 음악 경로 아래에 하위 폴더를 만든다.
- Android Storage Access Framework URI는 `content://com.android.externalstorage...%3ADownload...`처럼 보일 수 있다.
  - 표시용 `Download/YTET/Music`와 SAF URI는 같은 경로를 가리킬 수 있다.
  - 실제 저장/스캔 가능 여부는 SAF 권한과 tree URI 보존 상태를 함께 확인해야 한다.

## 빌드와 검증

자주 쓰는 검증:

```bash
scripts/verify_static.sh
git diff --check
./gradlew :app:compileDebugJavaWithJavac --warning-mode all
```

WSL에서 Gradle이 Android SDK를 못 찾을 때는 `local.properties`를 임시로 다음처럼 바꿔 검증한 뒤 되돌린다.

```properties
sdk.dir=/mnt/c/Users/ghwls/AppData/Local/Android/Sdk
```

검증 후 `local.properties`는 다시 원래 값으로 되돌린다.

```properties
sdk.dir=C\:\\Users\\ghwls\\AppData\\Local\\Android\\Sdk
```

`local.properties`는 `.gitignore` 대상이므로 GitHub에 올리지 않는다.

## 릴리즈 태그 규칙

`태그푸쉬`, `최신 태그 푸쉬`, `vX.Y.Z 태그푸쉬` 요청이 오면 반드시 `AGENTS.md`의 규칙을 따른다.

1. working tree 상태를 확인한다.
2. 이전 릴리즈 태그 이후 변경사항을 확인한다.
3. 사용자에게 보이는 변경사항, 수정, 검증 결과 중심으로 한국어 패치노트를 작성한다.
4. lightweight tag가 아니라 annotated tag를 만든다.
5. 브랜치와 태그를 push한다.
6. GitHub Release 도구가 가능하면 같은 패치노트로 Release를 생성/갱신한다.

권장 명령 형태:

```bash
git tag -a vX.Y.Z -F /tmp/ytet-vX.Y.Z-notes.md
git push origin main
git push origin vX.Y.Z
```

태그 메시지에 `Automated YTET Android release build...` 같은 자동 문구만 들어가면 안 된다.

## 서명과 배포

GitHub Actions release build는 signed release APK를 만들기 위해 다음 secrets를 사용한다.

- `YTET_SIGNING_KEYSTORE_BASE64`
- `YTET_SIGNING_STORE_PASSWORD`
- `YTET_SIGNING_KEY_ALIAS`
- `YTET_SIGNING_KEY_PASSWORD`

로컬의 `ytet-release.jks`는 `.gitignore` 대상이므로 GitHub에 올라가지 않는다.
다른 환경으로 옮길 때 업데이트 서명 연속성이 필요하면 이 파일과 비밀번호를 안전하게 별도로 옮겨야 한다.

서명 키가 달라지면 기존 설치 앱 위에 업데이트 설치가 실패할 수 있다.

## GitHub에 없는 로컬 항목

다음은 현재 `.gitignore` 또는 미추적 상태 때문에 GitHub clone만으로는 복원되지 않을 수 있다.

- `ytet-release.jks`: release signing keystore, 민감 파일
- `local.properties`: 로컬 Android SDK 경로
- `.gradle/`: Gradle cache
- `.idea/`: Android Studio IDE 설정
- `app/build/`, `build/`, `dist/`: 빌드 산출물
- `__pycache__/`, `*.pyc`: Python cache
- `스크린샷/`: 사용자가 비교용으로 제공한 Spotify/YTET 스크린샷, 현재 미추적

반대로 `AGENTS.md`와 `legacy/`는 `.gitignore`에 들어있지만 이미 tracked 상태라 GitHub에 남아 있다.
GitHub에서 제거하려면 별도 요청 후 `git rm --cached AGENTS.md`와 `git rm -r --cached legacy/`가 필요하다.

## 다른 환경으로 옮기는 방법

가장 안정적인 방식:

1. GitHub에서 저장소를 clone한다.
2. `git fetch --tags`로 태그를 모두 가져온다.
3. 새 환경의 Android SDK에 맞춰 `local.properties`를 새로 만든다.
4. release 서명이 필요하면 `ytet-release.jks`와 비밀번호를 안전한 방식으로 복사한다.
5. 비교 스크린샷이 필요하면 `스크린샷/` 폴더를 별도로 복사한다.
6. 새 Codex 세션이 이 문서와 `AGENTS.md`, `README.md`, `docs/PORTING.md`를 먼저 읽게 한다.

로컬 작업 상태까지 완전히 옮기려면 `.git` 디렉터리를 포함한 압축본을 별도로 만들어 옮긴다.
다만 `app/build/` 같은 빌드 캐시는 크고 재생성 가능하므로 보통 제외해도 된다.

## 새 Codex 세션 시작 프롬프트 예시

```text
이 저장소는 YTET Android 앱입니다.
먼저 AGENTS.md, README.md, docs/PORTING.md, docs/CODEX_HANDOFF.md를 읽고 현재 작업 맥락을 이어받아 주세요.
릴리즈 태그 요청 시에는 반드시 annotated tag에 한국어 패치노트를 넣어야 합니다.
로컬 음악 중심 앱이며 외부 라디오/테마 추천은 넣지 않습니다.
기존 사용자 변경사항은 되돌리지 말고, 기능 수정 후 가능한 검증을 실행해 주세요.
```

## 다음 세션에서 특히 조심할 점

- `git reset --hard`, `git checkout --` 등으로 사용자 작업을 되돌리지 않는다.
- `스크린샷/`은 미추적이므로 정리/삭제하지 않는다.
- 앱 UI는 실제 기기에서 status bar, navigation bar, gesture navigation 차이가 크게 보일 수 있다.
- 하단 탭과 navigation bar는 edge-to-edge/insets 처리가 민감하다.
- 많은 음악 파일에서 RecyclerView 재사용이 깨지면 다시 버벅일 수 있다.
- 업데이트 설치는 Android 권한과 signing key 영향을 크게 받는다.
- release tag push 요청은 커밋 push와 annotated tag push를 모두 포함하는 것으로 처리한다.
