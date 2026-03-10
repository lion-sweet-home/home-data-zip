# HomeDataZip

부동산 실거래 데이터와 생활 인프라 정보를 통합해 검색/분석/매물 관리/구독 기능을 제공하는 플랫폼

## 📋 목차

- [프로젝트 소개](#프로젝트-소개)
- [프로젝트 개요](#프로젝트-개요)
- [팀원 & 역할](#팀원--역할)
- [기술 스택](#기술-스택)
- [설치 및 실행 방법](#설치-및-실행-방법)
- [주요 기능](#주요-기능)
- [DB/ERD](#dberd)
- [API 명세서](#api-명세서)
- [코드 스타일 및 브랜치 전략](#코드-스타일-및-브랜치-전략)

---

## 프로젝트 소개

HomeDataZip은 아파트 매매/전월세 실거래 데이터와 지역 생활 인프라(학교, 병원, 지하철, 버스)를 결합해 사용자가 지역을 빠르게 비교하고 의사결정할 수 있도록 돕는 서비스입니다.

인증 이후에는 즐겨찾기, 매물 등록/조회, 실시간 채팅, 알림, 구독/결제 기능까지 하나의 흐름으로 사용할 수 있습니다.

### 주요 특징

- 🔐 **인증/인가**: JWT 기반 로그인, 토큰 재발급, Google OAuth 지원
- 🗺️ **지도 기반 검색**: 지역/지하철 중심 필터링 + 마커 시각화
- 🏘️ **매물 기능**: 매물 등록, 내 매물 조회, 상세 조회, 삭제
- ⭐ **관심매물**: 즐겨찾기 등록/해제 및 목록 관리
- 💳 **구독/결제**: Toss Payments 빌링키 발급/해지, 구독 시작/자동결제 관리
- 💬 **실시간 기능**: WebSocket(STOMP) 채팅 + SSE 알림
- 🛠️ **데이터 동기화**: 공공데이터 수집용 Batch/Quartz 스케줄링

---

## 프로젝트 개요

### 프로젝트 구조

```text
home-data-zip/
├── back/homedatazip/                  # Spring Boot 백엔드
│   ├── src/main/java/org/example/homedatazip/
│   │   ├── auth/                      # 인증/보안
│   │   ├── user/                      # 사용자/관리자
│   │   ├── listing/                   # 매물
│   │   ├── subscription/              # 구독
│   │   ├── payment/                   # 결제
│   │   ├── chat/                      # 채팅
│   │   ├── notification/              # 알림(SSE)
│   │   ├── apartment/, tradeSale/, tradeRent/
│   │   └── global/                    # 공통 설정/배치/외부 연동
│   └── src/main/resources/
│       └── application-dev.yml
│
├── front/                             # Next.js 프론트엔드 (App Router)
│   ├── app/
│   │   ├── auth/, search/, subscription/, my_page/, chat/
│   │   ├── notification/, admin/
│   │   └── api/                       # API 호출 유틸/도메인 모듈
│   ├── package.json
│   └── next.config.mjs
│
├── docker-compose.yml                 # mysql/redis/backend/frontend 통합 실행
└── .env.example                       # 환경변수 템플릿
```

### 아키텍처

- **Backend**: Spring Boot 기반 REST API + 실시간(WebSocket, SSE)
- **Frontend**: Next.js App Router 기반 UI
- **Database**: MySQL 8.0
- **Cache**: Redis 7
- **External**: Toss Payments, Google OAuth, Kakao API, 공공데이터 API
- **Infrastructure**: Docker Compose 기반 멀티 컨테이너 실행

---

## 팀원 & 역할


| 이름 | 역할 | 담당 |
|------|------|------|
| 정범규 | Backend | 회원가입, Region 및 아파트 실거래 데이터, AI 챗봇, 사용자 기반 추천 |
| 김서영 | Backend | 구독 및 결제, 학교 데이터 |
| 김재현 | Backend & Infra/DevOps | 회원 검색 및 삭제, 지오코더, 채팅, Docker/배포/환경변수 |
| 김재훈 | Backend & Frontend | 관리자 페이지, 병원 데이터, 매물 검색, UI/UX |
| 전주현 | Backend & Frontend | 공지, 알림, 지하철 데이터, 아파트와 지하철&학교 거리, 관심매물 등록, UI/UX |
| 최민혁 | Backend & Frontend | JWT, 마이페이지, 전월세 데이터, UI/UX |
| 허보미 | Backend | 구독 및 결제, 버스 데이터, 매물 CRUD, 휴대폰 인증구현 |

---

## 기술 스택

### Backend
- **Framework**: Spring Boot 3.5.9
- **Language**: Java 21
- **Database**: MySQL 8.0
- **Cache**: Redis 7
- **Security**: Spring Security + JWT + OAuth2
- **Build Tool**: Gradle
- **ORM/Query**: Spring Data JPA + QueryDSL
- **Batch/Scheduler**: Spring Batch + Quartz
- **Realtime**: WebSocket(STOMP), SSE

### Frontend
- **Framework**: Next.js 16.1.6 (App Router)
- **UI Library**: React 19.2.3
- **Language**: JavaScript (ES6+)
- **Styling**: Tailwind CSS 4
- **HTTP**: Fetch 기반 공통 API 유틸
- **Realtime**: event-source-polyfill, @stomp/stompjs, sockjs-client
- **3rd Party**: Kakao Maps API, Toss Payments SDK

### Infrastructure
- **Containerization**: Docker, Docker Compose
- **Storage**: AWS S3 (이미지)
- **Deployment**: GitHub Actions 기반 배포 파이프라인(`.github/workflows/deploy.yml`)

---

## 설치 및 실행 방법

### 사전 요구사항

- Docker, Docker Compose
- (선택) 로컬 개별 실행 시 Java 21, Node.js 20+

### 1) 저장소 클론

```bash
git clone https://github.com/lion-sweet-home/home-data-zip.git
cd home-data-zip
```

> 참고: 기본 개발 브랜치가 `dev`인 경우 아래 명령으로 전환 후 진행하세요.

```bash
git checkout dev
```

### 2) 환경 변수 설정

프로젝트 루트에서 `.env.example`을 복사한 뒤 `.env` 파일을 생성하고, 아래 형식에 맞춰 값을 채웁니다.

**루트 `.env` (백엔드/ Docker Compose용)**

```bash
# DB
MYSQL_ROOT_PASSWORD=your_root_password
MYSQL_DATABASE=home_db
MYSQL_USER=your_user
MYSQL_PASSWORD=your_db_password
MYSQL_PORT=3306

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# JWT (256비트 이상 권장)
JWT_ACCESS_SECRET=your_access_secret
JWT_REFRESH_SECRET=your_refresh_secret
JWT_ACCESS_EXP_SEC=3600000
JWT_REFRESH_EXP_SEC=1209600000

# OAuth (Google)
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret

# Toss Payments (테스트 키 사용 가능)
PAYMENT_TOSS_SECRET_KEY=test_sk_xxxxxxxxxxxxx
PAYMENT_TOSS_CLIENT_KEY=test_ck_xxxxxxxxxxxxx

# 메일 (SMTP)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email@example.com
MAIL_PASSWORD=your_app_password

# 외부 API 키 (아래 참고 사이트에서 발급)
DATA_GO_KR_SERVICE_KEY=
HOSPITAL_SERVICE_KEY=
RENT_API_SERVICE_KEY=
KAKAO_API_KEY=
SEOUL_OPENAPI_KEY=
SEOUL_OPENAPI_SERVICE=
SCHOOL_OPENAPI_KEY=
SUBWAY_OPENAPI_URL=

# AWS S3 (이미지 저장)
AWS_ACCESS_KEY_ID=
AWS_SECRET_ACCESS_KEY=
AWS_REGION=ap-northeast-2
AWS_S3_BUCKET=

# 프론트엔드 접근용 백엔드 URL (Docker 시 backend 서비스명 사용)
NEXT_PUBLIC_BACKEND_URL=http://localhost:8080
```

**프론트엔드 `front/.env.local`**

```bash
# API Base URL
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api

# Toss Payments 클라이언트 키 (결제 위젯)
NEXT_PUBLIC_TOSS_CLIENT_KEY=test_ck_xxxxxxxxxxxxx

# Kakao 지도 API (JavaScript 키)
NEXT_PUBLIC_KAKAO_MAP_API_KEY=your_kakao_javascript_key
```

#### 외부 API 키 발급 및 참고 사이트

| 용도 | 환경변수 예시 | 참고 사이트 |
|------|----------------|-------------|
| 공공데이터포털 | `DATA_GO_KR_SERVICE_KEY` | [공공데이터포털](https://www.data.go.kr/) → 회원가입 후 활용신청·인증키 발급 |
| 공공데이터 전국 법정동 | `DATA_GO_KR_SERVICE_KEY` | [공공데이터포털 - 국토교통부_전국 법정동](https://www.data.go.kr/data/15063424/fileData.do)
| 국토부 매매 API | `DATA_GO_KR_SERVICE_KEY` | [공공데이터포털 - 아파트 매매 실거래사 상세 자료](https://www.data.go.kr/data/15126468/openapi.do)
| 국토부 전월세 API | `RENT_API_SERVICE_KEY` | [공공데이터포털 - 아파트 전월세 실거래가 자료](https://www.data.go.kr/data/15126474/openapi.do) |
| 공공데이터 병원 정보 | `HOSPITAL_SERVICE_KEY` | [공공데이터포털 - 국립중앙의료원_전국 병・의원 찾기 서비스](https://www.data.go.kr/data/15000736/openapi.do) |
| Kakao 지도/주소/로그인 | `KAKAO_API_KEY`, `NEXT_PUBLIC_KAKAO_MAP_API_KEY` | [Kakao Developers](https://developers.kakao.com/) → 앱 생성 후 REST API 키 / JavaScript 키 발급 |
| 서울시 열린데이터 (버스·지하철 등) | `SEOUL_OPENAPI_KEY`, `SUBWAY_OPENAPI_URL` | [서울 열린데이터 광장](https://data.seoul.go.kr/dataList/OA-15067/S/1/datasetView.do?utm_source=chatgpt.com) / [서울시 교통빅데이터플랫폼](https://t-data.seoul.go.kr/dataprovide/trafficdataviewopenapi.do?data_id=1036) |
| 학교정보 API | `SCHOOL_OPENAPI_KEY` | [공공데이터포털 - 전국초중등학교위치표준데이터](https://www.data.go.kr/data/15021148/standard.do#/tab_layer_open) |
| Toss Payments | `PAYMENT_TOSS_*`, `NEXT_PUBLIC_TOSS_CLIENT_KEY` | [Toss Payments 개발자센터](https://developers.tosspayments.com/) → 테스트 키/클라이언트 키 발급 |
| Google OAuth | `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | [Google Cloud Console](https://console.cloud.google.com/) → API 및 서비스 > 사용자 인증 정보 |
| AWS S3 | `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_S3_BUCKET` | [AWS 콘솔](https://console.aws.amazon.com/) → IAM 사용자 키, S3 버킷 생성 |
| SOLAPI (SMS) | `SOLAPI_API_KEY`, `SOLAPI_API_SECRET` | [SOLAPI](https://solapi.com/) → API 키 발급 |

### 3) 전체 서비스 실행 (권장)

```bash
docker-compose up -d --build
```

접속 URL:
- 프론트: `http://localhost:3000`
- 백엔드: `http://localhost:8080`

종료:
```bash
docker-compose down
```

### 4) 개별 실행 (선택)

백엔드:
```bash
cd back/homedatazip
./gradlew bootRun
```

프론트:
```bash
cd front
npm install
npm run dev
```

---

## 주요 기능

### 인증/회원
- 회원가입(이메일 중복/닉네임 중복/이메일 인증 관련 API)
- 로그인/로그아웃/토큰 재발급
- Google OAuth 로그인
- 비밀번호 찾기/재설정

### 검색/탐색
- 지역(시도/구군/동) 기반 검색
- 지하철역 기반 검색
- 매매/전월세 필터 검색
- 지도 마커 + 사이드 패널 상세 정보

### 인프라 정보 조회
- 인근 학교/병원/버스정류장/지하철역 조회
- 월별/조건별 거래 데이터 조회

### 매물/마이페이지
- 매물 등록, 내 매물 조회, 상세 조회, 삭제
- 관심매물 등록/해제/목록
- 마이페이지 정보/알림 설정/비밀번호 변경

### 구독/결제
- 휴대폰 인증(구독 전 플로우)
- 빌링키 발급/해지
- 구독 시작, 자동결제 취소/재활성화
- 결제 내역 조회

### 실시간/관리자
- SSE 알림 스트림
- 채팅방 생성/조회/나가기 및 실시간 메시지
- 관리자 통계/정산/유저 관리/배치 실행 API

---

## DB/ERD

### 주요 엔티티

- **User**: 사용자 계정/권한/프로필/전화번호 인증
- **Subscription**: 구독 상태, 기간, 빌링키
- **PaymentLog**: 결제 로그(결제 상태, 금액, 승인 정보)
- **Listing**: 사용자 매물(매매/전월세)
- **Apartment**: 아파트 마스터 데이터
- **Region**: 시도/구군/동 지역 정보
- **Favorite**: 사용자-관심매물 관계
- **ChatRoom / ChatMessage**: 채팅 도메인
- **School / Hospital / Subway / BusStation**: 생활 인프라 데이터
- **TradeSale / TradeRent**: 매매/전월세 실거래 데이터

### 주요 관계 요약

- User ↔ Subscription: 1:1
- Subscription ↔ PaymentLog: 1:N
- User ↔ Listing: 1:N
- Region ↔ Apartment: 1:N
- Apartment ↔ TradeSale/TradeRent: 1:N
- User ↔ Favorite ↔ Listing: N:M (중간 엔티티 Favorite)
- User ↔ ChatRoom, Listing ↔ ChatRoom: 1:N

---

## API 명세서

아래는 주요 엔드포인트 요약입니다.

### 인증

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/auth/login` | 로그인 |
| POST | `/api/auth/refresh` | 토큰 재발급 |
| POST | `/api/auth/logout` | 로그아웃 |

### 사용자/마이페이지

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/users/register` | 회원가입 |
| GET | `/api/users/me` | 내 정보 조회 |
| PATCH | `/api/users/change-password` | 비밀번호 변경 |
| PUT | `/api/users/notification-setting` | 알림 설정 변경 |
| GET | `/api/users/notifications` | 사용자 알림 조회 |

### 검색/지역/아파트

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/regions/sido` | 시도 목록 |
| GET | `/api/regions/gugun` | 구군 목록 |
| GET | `/api/regions/dong` | 동 목록 |
| GET | `/api/apartment/trade-sale/markers` | 매매 마커 조회 |
| GET | `/api/rent` | 전월세 마커/목록 조회 |
| GET | `/api/apartments/{apartmentId}/subways` | 인근 지하철역 |
| GET | `/api/apartments/{apartmentId}/bus-stations` | 인근 버스정류장 |

### 매물/관심목록

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/listings/create` | 매물 등록 |
| GET | `/api/listings` | 매물 조회 |
| GET | `/api/listings/{listingId}` | 매물 상세 |
| DELETE | `/api/listings/{listingId}` | 매물 삭제 |
| POST | `/api/users/me/favorites/{listingId}` | 관심매물 등록 |
| DELETE | `/api/users/me/favorites/{listingId}` | 관심매물 해제 |

### 구독/결제

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/subscriptions/billing/issue` | 빌링키 발급 시작 |
| POST | `/api/subscriptions/billing/revoke` | 빌링키 해지 |
| POST | `/api/subscriptions/start` | 구독 시작 |
| POST | `/api/subscriptions/auto-pay/cancel` | 자동결제 취소 |
| POST | `/api/subscriptions/auto-pay/reactivate` | 자동결제 재활성화 |
| GET | `/api/subscriptions/me` | 내 구독 정보 |
| POST | `/api/payments/prepare` | 결제 준비 |
| POST | `/api/payments/confirm` | 결제 승인 |
| GET | `/api/payments/me` | 결제 내역 조회 |

### 채팅/알림

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/chat/rooms` | 채팅방 목록 |
| POST | `/api/chat/room` | 채팅방 생성/입장 |
| GET | `/api/sse/notifications` | 알림 SSE 연결 |
| GET | `/api/sse/chat` | 채팅 SSE 연결 |

### 관리자

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/admin/monthly-income` | 월 수익 통계 |
| GET | `/api/admin/users/list` | 사용자 목록 |
| POST | `/api/admin/settlement/process` | 정산 처리 |

---

## 코드 스타일 및 브랜치 전략

### 코드 스타일

#### 백엔드 (Java / Spring Boot)

**네이밍 컨벤션**
- 클래스명: `PascalCase` (예: `UserController`, `ListingService`)
- 메서드명: `camelCase` (예: `getUserById`, `createListing`)
- 상수: `UPPER_SNAKE_CASE` (예: `MAX_RETRY_COUNT`)
- 패키지명: 소문자, 점으로 구분 (예: `org.example.homedatazip.user.controller`)

**파일 구조**
```
org.example.homedatazip.{domain}/
├── controller/     # REST API 엔드포인트
├── service/        # 비즈니스 로직
├── repository/     # 데이터 접근 계층
├── entity/         # JPA 엔티티
└── dto/            # 데이터 전송 객체
```

**주요 규칙**
- Lombok 사용: `@Getter`, `@Setter`, `@Builder` 등 적극 활용
- 불변 객체: DTO는 `@Builder` 패턴 사용, 엔티티는 setter 최소화
- Null 안전성: `Optional` 사용, `@Nullable`/`@NonNull` 어노테이션 활용
- 의존성 주입: 생성자 주입 사용 (필드 주입 지양)

#### 프론트엔드 (JavaScript / Next.js App Router)

**네이밍 컨벤션**
- 컴포넌트: `PascalCase` (예: `UserProfile`, `SubscriptionDetailModal`)
- 함수/변수: `camelCase` (예: `getUserData`, `isLoading`)
- 상수: `UPPER_SNAKE_CASE` (예: `API_BASE_URL`)
- 파일명: 컴포넌트는 `PascalCase.js` 또는 폴더 내 `page.js`, 유틸/API는 `camelCase.js`

**파일 구조**
```
front/app/
├── api/            # API 클라이언트 (auth.js, user.js, subscription.js 등)
├── auth/, search/, subscription/, my_page/, chat/  # 기능별 페이지
│   ├── page.js
│   └── components/ # 해당 기능 전용 컴포넌트
├── layout.js       # 루트 레이아웃
└── globals.css     # 전역 스타일
```

---


### 브랜치 전략

**브랜치 구조**
- `main`: 운영(프로덕션) 브랜치
- `dev`: 개발 브랜치 (`main`에서 분기)
- 개인 브랜치(기능/픽스): `dev`에서 분기 (예: `feature/auth-login`, `fix/payment-timeout`)

**브랜치 생성**

원격 최신 상태를 반영한 뒤 `dev`에서 개인 브랜치를 만들고, 최초 푸시 시 업스트림을 등록합니다.

```bash
git fetch
git checkout dev                    # dev로 이동
git pull origin dev                 # 원격 dev 최신 반영
git checkout -b branchName          # 개인 브랜치 생성 및 이동

# 새 브랜치를 원격에 올리면서 업스트림 설정 (-u == --set-upstream)
git push -u origin branchName
# 또는
git push --set-upstream origin branchName
```

**커밋 정책**

푸시 전에는 아래 순서로 진행하여 원격 `dev`와 동기화한 뒤 올립니다.

```bash
git add .
git commit -m "message"
git pull --rebase origin dev
git push origin branchName
```

- `git push` 시 `(origin branchName)`은 업스트림을 `-u`로 등록했다면 생략 가능합니다.

**`--rebase` 옵션**
- 원격 `dev`의 최신 커밋을 먼저 반영한 뒤,
- 로컬에서 만든(아직 push 안 된) 커밋들을 그 위에 다시 적용(rebase)하여
- 커밋 히스토리를 깔끔하게 유지합니다. (merge commit을 만들지 않음)

**운영 규칙 요약**
- 개인 브랜치(기능/픽스)는 항상 `dev`에서 생성
- 기능 개발이 끝나면 해당 브랜치를 `dev`로만 병합
- `fix/*`는 상황에 따라 `dev` 또는 `main`에서 분기
- `fix/*`를 `main`에 병합한 경우 반드시 `dev`로 백머지하여 동기화 유지
