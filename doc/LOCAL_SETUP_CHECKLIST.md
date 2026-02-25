# Local Setup Checklist (Windows 11 + VSCode) (로컬 환경 체크리스트)

This checklist is for this PC and the planned implementation flow. (현재 PC 기준 점검 항목)
Each item includes a short explanation for beginners. (뭔지 모르는 분을 위한 짧은 설명 포함)

## 1) JDK 17 (Java Development Kit)
- What: Required to build Android/Kotlin with Gradle. (설명: 안드로이드/코틀린 빌드에 필요한 자바 도구)
- [ ] `java -version` shows 17.x (현재: openjdk version "21" 2023-09-19)
- [ ] `JAVA_HOME` points to JDK 17 (현재: C:\Program Files\Java\jdk-21)
- Result: Missing (JDK 17 미설치, JDK 21만 확인됨)
- Command:
  - `java -version`
  - `echo $env:JAVA_HOME`

## 2) Android SDK / Tools
- What: Android build and device tools. (설명: 안드로이드 빌드/실행 도구 모음)
- [x] Android SDK installed (SDK 존재: `C:\Users\jwkim\AppData\Local\Android\Sdk`)
- [x] Build Tools installed (build-tools 폴더 있음)
- [x] Platform Tools installed (platform-tools 폴더 있음)
- [x] `ANDROID_SDK_ROOT` set (환경 변수 설정 완료)
- [x] `adb` on PATH (PATH에 추가 완료, `adb version` 정상)
- Result: OK (SDK와 환경 변수 설정 완료)
- Command:
  - `echo $env:ANDROID_SDK_ROOT`
  - `Get-ChildItem $env:ANDROID_SDK_ROOT`
  - `where.exe adb` (PowerShell에서 `where`는 별칭이라 `where.exe` 사용)
  - `adb version`

## 3) Emulator or Device
- What: Needed to run and test the app. (설명: 앱 실행/테스트에 필요)
- [x] Emulator installed and AVD exists (AVD 1개 감지)
- [x] `adb devices` shows a device (현재: `emulator-5554 device`)
- Result: OK (에뮬레이터 정상 연결)
- Command:
  - `emulator -list-avds`
  - `adb devices`

## 4) Gradle / Project Build
- What: Build system used by Android. (설명: 안드로이드 빌드 시스템)
- [x] Gradle wrapper exists (gradlew/gradlew.bat 생성됨, Gradle 8.7)
- [x] Debug build succeeds (`.\gradlew assembleDebug` 성공)
- Result: OK (기본 빌드 성공)
- Command:
  - `.\gradlew tasks`
  - `.\gradlew assembleDebug`

## 5) Firebase (Auth + Firestore)
- What: Login and data storage. (설명: 로그인/데이터 저장)
- [ ] Firebase project created (확인 필요)
- [ ] Auth (Email) enabled (확인 필요)
- [ ] Firestore enabled (확인 필요)
- [ ] `app/google-services.json` downloaded (현재 없음)
- Result: Not sure (Firebase 설정 필요/미확인)
- Notes:
  - Package name must match `google-services.json` (패키지명 일치 필요)

## 6) Naver Map SDK
- What: Map view and location features. (설명: 지도 기능)
- [ ] Naver Map API key issued (확인 필요)
- [ ] API key ready for `AndroidManifest.xml` (확인 필요)
- Result: Not sure (키 발급/설정 필요)

## 7) VSCode Setup (Optional but Recommended) (선택)
- What: Helps coding and running Gradle. (설명: 개발 편의)
- [ ] Kotlin extension installed (확인 필요)
- [ ] Gradle extension installed (확인 필요)
- [ ] Java extension pack installed (확인 필요)
- Suggested extensions (추천 확장):
  - Kotlin Language (`fwcd.kotlin`)
  - Gradle for Java (`vscjava.vscode-gradle`)
  - Extension Pack for Java (`vscjava.vscode-java-pack`)

## 8) Windows 11 Environment Variables (환경 변수 설정 위치)
- Settings path: Settings > System > About > Advanced system settings > Environment Variables
- Shortcut: `Win + R` then `sysdm.cpl` > Advanced > Environment Variables

## 9) Quick Status Snapshot (빠른 상태 체크)
- JDK 17: Missing (JDK 21 installed)
- ANDROID_SDK_ROOT: OK (`C:\Users\jwkim\AppData\Local\Android\Sdk`)
- Android SDK tools: OK (build-tools/platform-tools/emulator 있음)
- ADB devices: OK (`emulator-5554 device`)
- Project Gradle wrapper: OK (gradlew present)
- Firebase: Not sure (`app/google-services.json` missing)
- Naver Map key: Not sure
