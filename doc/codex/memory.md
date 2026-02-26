이 파일은 프로젝트의 장기 기억이다.
항상 최신 상태로 유지해야 한다.
# 프로젝트 메모

## 현재 상태
- 핵심 기능 통합이 완료되었고 앱 빌드는 성공 상태입니다.
- 댓글 흐름(등록/수정/삭제/조회)이 구현되어 있으며 소유자 검증이 적용되어 있습니다.
- `AuthViewModel`, `ToiletViewModel`, `CommentViewModel` 간 인증 상태 동기화가 보강되었습니다.
- 커뮤니티 화면은 화장실 기준 요약 카드로 표시되며, 카드 탭 시 지도 포커스가 동작합니다.
- 메인 화면의 프로필 아이콘에 서브메뉴와 로그아웃 기능이 연결되어 있습니다.
- 런처/앱 아이콘은 45도 택시 스타일로 변경되었고 런처 배경은 흰색입니다.
- 서명된 릴리즈 산출물이 생성된 상태입니다.

## 이번 작업 범위에서 반영된 내용
- Firestore 댓글 권한 동작을 점검하고 관련 코드 경로를 보강했습니다.
- 댓글 소유자 검증을 UI 로직과 저장소 로직 양쪽에 적용했습니다.
- 화장실 숨기기에서 권한 거부 시 로컬 상태로 처리하는 폴백을 유지합니다.
- 지도 데이터 로딩은 bounds 기반 청크 로딩 + 캐시/디바운스 전략을 사용합니다.
- 이번 사이클 중 Firestore `comments` 데이터는 1회 전체 삭제되었습니다.

## 빌드/산출물 메모
- 디버그 빌드: `./gradlew.bat :app:assembleDebug` 성공
- 릴리즈 빌드: `./gradlew.bat :app:assembleRelease` 성공
- 현재 산출물:
  - `app/build/outputs/apk/debug/app-debug.apk`
  - `app/build/outputs/apk/release/app-release-unsigned.apk`
  - `app/build/outputs/apk/release/TaxiDriverNote_v1.0_signed.txt` (`.txt` 확장자지만 APK 바이너리)
- 릴리즈 서명에 사용한 키스토어:
  - `app/release-keystore.jks`
  - alias: `taxi_driver_release`

## 다음 세션 시작 포인트
- 최종 공유 파일 확장자가 `.apk`인지 확인 (`.txt`면 이름 변경 필요)
- 서명된 릴리즈 파일로 설치/업데이트 스모크 테스트 수행
- UI 문자열의 인코딩 깨짐(모지바케) 정리
- Gradle `signingConfigs`로 릴리즈 서명 자동화 여부 결정

## 알려진 리스크
- 변경/미추적 파일이 많아 워킹트리가 더러운 상태입니다.
- 일부 UI 파일에 인코딩 깨진 문자열이 남아 있을 수 있습니다.
- 일부 컴포저블에서 deprecated 아이콘 API 경고가 남아 있습니다.
