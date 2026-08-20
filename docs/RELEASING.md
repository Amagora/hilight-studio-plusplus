# Releasing HiLight Studio

GitHub releases are experimental prereleases. The current public release line uses an installable debug build, signed with the maintainer machine's Android debug certificate. It is not a production-signed release.

Keep signing files and APKs out of Git. Android only installs an update over an existing app when both APKs use the same application ID and signing identity.

1. Update `versionCode` and `versionName` in `app/build.gradle.kts`.
2. Add the user-visible changes to `CHANGELOG.md`.
3. Run the release checks:

   ```bash
   ./gradlew --no-daemon :app:testDebugUnitTest :app:build :app:lint
   ```

4. Build the developer APK:

   ```bash
   ./gradlew --no-daemon :app:assembleDebug
   ```

5. Copy `app/build/outputs/apk/debug/app-debug.apk` outside the repository and name it `HiLight-Studio-v<version>-experimental.apk`.
6. Verify the certificate and record the SHA-256 digest:

   ```bash
   apksigner verify --verbose --print-certs HiLight-Studio-v<version>-experimental.apk
   shasum -a 256 HiLight-Studio-v<version>-experimental.apk
   ```

7. Create an annotated `v<version>-experimental` tag, push `main` and the tag, then create a GitHub prerelease with the APK attached.
8. Copy the matching changelog entry into the release notes. Include the SHA-256 digest and state that the APK is debug-signed.

Do not upload an unsigned APK, signing material, or an APK signed by a different identity without explaining that existing users must uninstall before installing it.
