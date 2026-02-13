# Next.js App Router 구조 가이드

## 개요

이 프로젝트는 Next.js 16의 **App Router**를 사용합니다. App Router는 `app` 디렉토리를 기반으로 파일 시스템 기반 라우팅을 제공합니다.

## 디렉토리 구조

```
front/
└── app/
    ├── layout.js          # 루트 레이아웃 (모든 페이지에 적용)
    ├── page.js            # 루트 페이지 (/)
    ├── globals.css        # 전역 스타일
    │
    ├── admin/             # /admin 경로
    │   ├── page.js
    │   └── components/
    │
    ├── apartment/         # /apartment 경로
    │   ├── page.js
    │   └── components/
    │
    ├── auth/              # /auth 경로
    │   ├── login/         # /auth/login 경로
    │   │   └── page.js
    │   └── logout/        # /auth/logout 경로
    │       └── page.js
    │
    ├── chat/              # /chat 경로
    │   ├── page.js        # /chat 페이지
    │   ├── [id]/          # 동적 라우트: /chat/:id
    │   │   └── page.js
    │   └── components/
    │
    ├── favorite/          # /favorite 경로
    │   └── page.js
    │
    ├── listing/           # /listing 경로
    │   ├── page.js
    │   └── components/
    │
    ├── my_page/           # /my_page 경로
    │   ├── page.js
    │   └── components/
    │
    ├── search/            # /search 경로
    │   ├── page.js
    │   └── components/
    │
    ├── subscription/      # /subscription 경로
    │   └── page.js
    │
    └── api/               # API 라우트
        ├── apartment_rent.js
        ├── apartment_sale.js
        ├── api.js
        ├── auth.js
        ├── bus.js
        ├── hospital.js
        ├── region.js
        ├── school.js
        ├── subway.js
```

## 라우팅 규칙

### 1. 기본 라우팅

- `app/page.js` → `/` (홈 페이지)
- `app/admin/page.js` → `/admin`
- `app/apartment/page.js` → `/apartment`
- `app/favorite/page.js` → `/favorite`

### 2. 중첩 라우팅

폴더 구조가 URL 경로를 결정합니다:

- `app/auth/login/page.js` → `/auth/login`
- `app/auth/logout/page.js` → `/auth/logout`

### 3. 동적 라우팅

대괄호 `[]`를 사용하여 동적 세그먼트를 생성합니다:

- `app/chat/[id]/page.js` → `/chat/1`, `/chat/2`, `/chat/abc` 등

동적 라우트에서 파라미터 접근:

```javascript
// app/chat/[id]/page.js
export default function ChatPage({ params }) {
  const { id } = params;
  return <div>Chat ID: {id}</div>;
}
```

### 4. 레이아웃 (Layout)

- `app/layout.js`: 루트 레이아웃으로 모든 페이지에 적용됩니다.
- 각 폴더에 `layout.js`를 추가하면 해당 경로와 하위 경로에만 적용됩니다.

예시:
```
app/
├── layout.js              # 모든 페이지에 적용
└── admin/
    ├── layout.js          # /admin과 하위 경로에만 적용
    └── page.js
```

### 5. API 라우트

`app/api/` 디렉토리의 파일은 API 엔드포인트가 됩니다:

- `app/api/auth.js` → `/api/auth`
- `app/api/apartment_rent.js` → `/api/apartment_rent`

API 라우트 예시:

```javascript
// app/api/auth.js
export async function GET(request) {
  return Response.json({ message: 'GET 요청' });
}

export async function POST(request) {
  const body = await request.json();
  return Response.json({ message: 'POST 요청', data: body });
}
```

## 컴포넌트 구조

각 페이지 디렉토리 내에 `components` 폴더를 두어 해당 페이지에서만 사용하는 컴포넌트를 관리합니다:

```
app/
└── admin/
    ├── page.js
    └── components/
        ├── manage.js
        ├── notification.js
        └── settlement.js
```

## 페이지 파일 규칙

### 필수 파일

- **`page.js`**: 페이지 컴포넌트를 내보내는 파일 (필수)
- **`layout.js`**: 레이아웃 컴포넌트를 내보내는 파일 (선택)

### 페이지 컴포넌트 예시

```javascript
// app/admin/page.js
export default function AdminPage() {
  return (
    <div>
      <h1>관리자 페이지</h1>
    </div>
  );
}
```

### 레이아웃 컴포넌트 예시

```javascript
// app/admin/layout.js
export default function AdminLayout({ children }) {
  return (
    <div>
      <nav>관리자 네비게이션</nav>
      {children}
    </div>
  );
}
```

## 네비게이션

### Link 컴포넌트 사용

```javascript
import Link from 'next/link';

export default function Navigation() {
  return (
    <nav>
      <Link href="/">홈</Link>
      <Link href="/admin">관리자</Link>
      <Link href="/chat/123">채팅 123</Link>
    </nav>
  );
}
```

### useRouter 훅 사용

```javascript
'use client'; // 클라이언트 컴포넌트에서만 사용 가능

import { useRouter } from 'next/navigation';

export default function MyComponent() {
  const router = useRouter();
  
  const handleClick = () => {
    router.push('/admin');
  };
  
  return <button onClick={handleClick}>이동</button>;
}
```

## 주의사항

1. **파일명 규칙**: 
   - 페이지는 반드시 `page.js`로 명명
   - 레이아웃은 `layout.js`로 명명

2. **클라이언트 컴포넌트**:
   - 인터랙티브한 기능(useState, useEffect, 이벤트 핸들러)이 필요하면 파일 상단에 `'use client'` 추가

3. **서버 컴포넌트** (기본값):
   - 기본적으로 모든 컴포넌트는 서버 컴포넌트입니다
   - 서버에서 렌더링되므로 브라우저 API 사용 불가

4. **동적 라우트**:
   - `[id]` 형식으로 동적 세그먼트 생성
   - 여러 동적 세그먼트: `[category]/[id]` → `/category/123`

5. **API 라우트**:
   - `app/api/` 내의 파일은 API 엔드포인트가 됩니다
   - HTTP 메서드(GET, POST, PUT, DELETE 등)를 export하여 처리

## 현재 프로젝트 라우트 목록

- `/` - 홈 페이지
- `/admin` - 관리자 페이지
- `/apartment` - 아파트 페이지
- `/auth/login` - 로그인 페이지
- `/auth/logout` - 로그아웃 페이지
- `/chat` - 채팅 목록
- `/chat/[id]` - 특정 채팅방
- `/favorite` - 즐겨찾기
- `/listing` - 목록 페이지
- `/my_page` - 마이페이지
- `/search` - 검색 페이지
- `/subscription` - 구독 페이지

## 참고 자료

- [Next.js App Router 공식 문서](https://nextjs.org/docs/app)
- [Next.js 라우팅 가이드](https://nextjs.org/docs/app/building-your-application/routing)
