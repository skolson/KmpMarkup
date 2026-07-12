### 0.2.0 (2026-07)

- Gradle 9.6.1
- Gradle toolchain
- Kotlin 2.4.0
- kmp-io 0.3.1
- AGP 9.4.0-alpha04
- Android SDK 37
- kotlinx datetime 0.8.0
- kotlinx coroutines 1.11.0
- Refactored unit tests to get androidDeviceTest working. Still needs to use recent versions of androidSdk in libs.versions.toml to avoid desugaring issues with JUnit. 
- XML attribute parsing logic had to be repaired after upgrade to kmp-io 0.3.1 with the TextBuffer fixes. This version will no longer work with versions of kmp-io older than 0.3.1.
- OFX parser also needed workaround for original TextBuffer behavior removed, now works with and requires kmp-io 0.3.1 or later.   

### 0.1.0 (2025-08)

- Initial Release. No DTD or schema support.