# Implementation Plan (MVP) (구현 계획)

This plan follows the fixed spec and orders work by dependency. (확정된 스펙을 따르고 의존성 기준으로 정렬)

## 1) Implementation Steps (구현 단계)
Status labels: [진행중] / [완료]
1. Project and SDK setup (프로젝트 및 SDK 설정) [완료] (Gradle 8.7, 빌드 성공)
2. App shell and navigation (앱 골격 및 내비게이션) [완료]
3. Core data models and repository interfaces (핵심 데이터 모델 및 레포지토리 인터페이스) [완료]
4. Authentication flow and userId contract (인증 플로우 및 userId 계약 고정) [완료]
5. Salary calculator (급여 계산기) [완료]
6. Toilet map and sharing (화장실 지도 및 공유)
7. Comment system (댓글 시스템)

## 2) Key Files by Step (단계별 주요 파일)
Status labels: [진행중] / [완료]
1. Project and SDK setup (프로젝트 및 SDK 설정) [완료]
   - settings.gradle.kts [완료]
   - build.gradle.kts [완료]
   - app/build.gradle.kts [완료]
   - app/google-services.json [완료]
   - app/src/main/AndroidManifest.xml [완료]
   - app/src/main/res/values/strings.xml [완료]
   - app/src/main/res/values/themes.xml [완료]
2. App shell and navigation (앱 골격 및 내비게이션) [완료]
   - MainActivity.kt [완료]
   - AppNavGraph.kt [완료]
   - Routes.kt [완료]
   - AppStartScreen.kt [완료]
   - MainScreen.kt [완료]
   - Theme.kt [완료]
3. Core data models and repository interfaces (핵심 데이터 모델 및 레포지토리 인터페이스) [완료]
   - User.kt [완료]
   - SalarySetting.kt [완료]
   - DailySales.kt [완료]
   - Toilet.kt [완료]
   - Comment.kt [완료]
   - AuthRepository.kt (interface) [완료]
   - FirestoreRepository.kt (interface) [완료]
4. Authentication flow and userId contract (인증 플로우 및 userId 계약 고정) [완료]
   - LoginScreen.kt [완료]
   - SignUpScreen.kt [완료]
   - AuthViewModel.kt (userId 제공 구조 고정) [완료]
   - AuthRepositoryImpl.kt [완료]
5. Salary calculator (급여 계산기) [완료]
   - FirestoreRepositoryImpl.kt (Step 4 이후 구체화) [완료]
   - SalarySettingScreen.kt [완료]
   - DailySalesInputScreen.kt [완료]
   - SalaryResultScreen.kt [완료]
   - SalaryViewModel.kt [완료]
6. Toilet map and sharing (화장실 지도 및 공유)
   - ToiletMapScreen.kt
   - ToiletDetailSheet.kt
   - AddToiletScreen.kt
   - ToiletViewModel.kt
7. Comment system (댓글 시스템)
   - CommentScreen.kt
   - CommentViewModel.kt

## 3) Prerequisites (선행 조건)
- External setup: Firebase project with Auth/Firestore enabled and Naver Map API key (외부 설정: Firebase 프로젝트에서 Auth/Firestore 활성화 및 Naver Map API 키)
- Step dependencies: (단계 의존성)
  - Step 2 depends on Step 1 (2단계는 1단계 필요)
  - Step 3 depends on Step 1 (3단계는 1단계 필요)
  - Step 4 depends on Steps 2 and 3 (4단계는 2, 3단계 필요)
  - Step 5 depends on Steps 3 and 4 (5단계는 3, 4단계 필요)
  - Step 6 depends on Steps 1, 3, and 4 (6단계는 1, 3, 4단계 필요)
  - Step 7 depends on Steps 4 and 6 (7단계는 4, 6단계 필요)
