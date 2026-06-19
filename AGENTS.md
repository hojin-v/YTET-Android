# Project Rules

## Commit Messages

커밋을 만들 때는 커밋 메시지 본문을 한글로 작성하고, 현대적인 conventional commit 형식을 사용한다.

짧은 type prefix 뒤에 콜론을 붙인다. 예시는 다음과 같다.

- `feat:` 사용자에게 보이는 기능 추가
- `fix:` 버그 수정 또는 동작 보정
- `refactor:` 의도한 동작 변경 없이 코드 구조 개선
- `docs:` 문서만 변경
- `test:` 테스트 추가 또는 수정
- `build:` 빌드, 의존성, 패키징 변경
- `chore:` 다른 분류에 맞지 않는 유지보수 변경

제목은 짧고 구체적으로 작성한다. 구현 과정보다는 사용자에게 보이는 결과나 기술적 결과를 우선 설명한다.

예시:

- `fix: 슬립 타이머 기본값을 00:00으로 보정`
- `feat: 보관함 재생목록 저장 메뉴 추가`
- `docs: 릴리즈 태그 작성 규칙 정리`

## Release Tag Pushes

When the user asks to push a release tag, such as `태그푸쉬`, `최신 태그 푸쉬`, or `vX.Y.Z 태그푸쉬`, do not create a bare lightweight tag without notes.

Follow this release flow:

1. Confirm the working tree state and identify the commit that will receive the tag.
2. Review the relevant changes since the previous release tag.
3. Write concise Korean patch notes that summarize user-visible changes, fixes, and verification.
4. Create an annotated tag with those patch notes as the tag message.
   - Prefer: `git tag -a vX.Y.Z -F <patch-notes-file>`
   - Do not use a lightweight `git tag vX.Y.Z` for release tags.
5. Push the branch and the annotated tag.
6. If GitHub release tooling is available, create or update the GitHub Release for the tag using the same patch notes.

Patch notes should avoid internal-only implementation noise unless it affects behavior. Include verification results when they are relevant, for example static checks, Gradle compile, APK build, or emulator confirmation.
