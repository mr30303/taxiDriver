# Project Overview (프로젝트 개요)
- Android app for corporate taxi drivers (법인 택시 기사용 Android 앱)
- Core problems (핵심 문제):
  - Complex salary calculation (quota-based system) (복잡한 급여 계산: 할당제)
  - Lack of reliable restroom information during driving (운행 중 신뢰할 화장실 정보 부족)

# Tech Stack (FIXED) (기술 스택, 고정)
- Android
- Kotlin
- Jetpack Compose
- MVVM
- Firebase Auth / Firestore
- Naver Map Android SDK
- VS Code (main), Android Studio (support) (VS Code 메인, Android Studio 보조)

# Core Features (MVP ONLY) (핵심 기능, MVP 한정)
1. Authentication (Email) (이메일 인증)
2. Salary Calculator (급여 계산기)
3. Toilet Map & Sharing (화장실 지도 및 공유)
4. Comment system (댓글 시스템)

# Screen Structure (화면 구조)
- AppStart (login check) (앱 시작/로그인 체크)
- Login / SignUp (로그인 / 회원가입)
- Main (hub) (메인 허브)
- SalarySetting (급여 설정)
- DailySalesInput (일 매출 입력)
- SalaryResult (급여 결과)
- ToiletMap (화장실 지도)
- ToiletDetail (BottomSheet) (화장실 상세/바텀시트)
- AddToilet (화장실 추가)
- Comment (댓글)

# Data Model (Backbone) (데이터 모델)
User
- userId (사용자 ID)
- email (이메일)

SalarySetting (1:1) (급여 설정)
- monthlyTarget (월 목표)
- baseSalary (기본급)
- bonusRatio (보너스 비율)

DailySales (1:N) (일 매출)
- userId (사용자 ID)
- date (YYYY-MM-DD) (날짜)
- amount (금액)
- tollFee (통행료)

Toilet (1:N) (화장실)
- lat (위도)
- lng (경도)
- type (open/public/private) (유형)
- description (설명)
- createdBy (등록자)
- likeCount (좋아요 수)
- dislikeCount (싫어요 수)

Comment (1:N) (댓글)
- toiletId (화장실 ID)
- userId (사용자 ID)
- content (내용)

# Constraints (제약)
- MVP scope only (MVP 범위)
- No over-engineering (과한 설계 금지)
- Single-module app (단일 모듈)
- UI simplicity prioritized (UI 단순성 우선)