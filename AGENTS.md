# Project Rules

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
