# 검색 페이지 구조 문서

## 개요

검색 기능은 두 개의 주요 페이지로 구성되어 있습니다:
- `/search` - 검색 조건 입력 페이지
- `/search/map` - 지도 및 마커 표시 페이지

---

## 1. `/search` 페이지 (`front/app/search/page.js`)

### 1.1 역할
검색 조건을 입력하고, 동을 선택하지 않은 경우 동 목록 모달을 표시하여 사용자가 동을 선택할 수 있게 합니다.

### 1.2 주요 기능

#### 1.2.1 거래 유형 선택
- **매매**: 매매 거래 검색
- **전/월세**: 전세/월세 거래 검색
- `tradeType` state로 관리 (기본값: '매매')
- **중요**: `tradeType`은 쿼리 스트링에 포함되지 않으며, 컴포넌트 내부에서만 사용

#### 1.2.2 검색 조건 타입 선택
- **지역 검색** (`region`): 시/도, 구/군, 동 선택
- **지하철역 검색** (`subway`): 역명 또는 호선으로 검색
- 라디오 버튼으로 선택

#### 1.2.3 지역 검색 기능
- **시/도 선택** (필수, `*` 표시)
  - API: `GET /api/regions/sido`
  - `getSidoList()` 호출
- **구/군 선택** (필수, `*` 표시)
  - API: `GET /api/regions/gugun?sido={sido}`
  - `getGugunList(selectedSido)` 호출
  - 시/도 선택 시 자동 로드
- **동 선택** (선택사항)
  - API: `GET /api/regions/dong?sido={sido}&gugun={gugun}`
  - `getDongList(selectedSido, selectedGugun)` 호출
  - 구/군 선택 시 자동 로드
  - null 값은 필터링하여 표시

#### 1.2.4 가격 범위 필터
- **매매**: 매매가 범위 (최소/최대, 만원 단위)
- **전/월세**: 
  - 보증금 범위 (최소/최대, 만원 단위)
  - 월세 범위 (최소/최대, 만원 단위)

#### 1.2.5 학교 필터
- 체크박스로 선택: 초등학교, 중학교, 고등학교
- 반경 선택: 0.5km, 1.0km, 2.0km, 3.0km, 5.0km, 10.0km
- API: `GET /api/schools?sido={sido}&gugun={gugun}&schoolLevel={level}`
- `searchSchoolsByRegion()` 호출
- 선택된 학교 목록 표시

#### 1.2.6 지하철역 검색 기능
- 검색 타입 선택: 역명 또는 호선
- 검색 키워드 입력
- API: `GET /api/subway/stations?stationName={name}` 또는 `?lineName={line}`
- `searchSubwayStations()` 호출
- 검색 결과에서 역 선택
- 반경 선택: 0.5km ~ 10.0km

#### 1.2.7 동 목록 모달
- **트리거**: 동을 선택하지 않고 "검색하기" 버튼 클릭
- **매매인 경우**:
  - API: `GET /api/regions/dong/rank?sido={sido}&gugun={gugun}&periodMonths=6`
  - `getDongRank()` 호출
  - 동별 거래량 표시
- **전월세인 경우**:
  - API: 
    - `GET /api/apartments/month-avg/jeonse-count?si={sido}&gu={gugun}&period=6`
    - `GET /api/apartments/month-avg/wolse-count?si={sido}&gu={gugun}&period=6`
  - `getJeonseCount()`, `getWolseCount()` 병렬 호출
  - 동별 전세/월세 거래량 표시
- **동 선택 시**: `/search/map`으로 이동하며 선택한 동 정보 전달

### 1.3 검색 버튼 동작

#### 1.3.1 지역 검색
```javascript
handleSearch() {
  // 1. 시/도, 구/군 필수 체크
  if (!selectedSido || !selectedGugun) {
    alert('시/도와 구/군을 선택해주세요.');
    return;
  }

  // 2. 동 미선택 시 모달 표시
  if (!selectedDong) {
    // 동 목록 API 호출 및 모달 표시
    setShowDongModal(true);
    return;
  }

  // 3. 동 선택 시 바로 /search/map으로 이동
  router.push(`/search/map?${params.toString()}`);
}
```

#### 1.3.2 지하철역 검색
```javascript
handleSearch() {
  // 1. 역 선택 필수 체크
  if (!selectedSubwayStation) {
    alert('지하철역을 선택해주세요.');
    return;
  }

  // 2. /search/map으로 이동
  router.push(`/search/map?${params.toString()}`);
}
```

### 1.4 쿼리 스트링 파라미터
- `sido`: 시/도명 (필수)
- `gugun`: 구/군명 (필수)
- `dong`: 동명 (선택)
- `priceMin`, `priceMax`: 매매가 범위 (매매인 경우)
- `depositMin`, `depositMax`: 보증금 범위 (전월세인 경우)
- `monthlyRentMin`, `monthlyRentMax`: 월세 범위 (전월세인 경우)
- `schoolTypes`: 학교 타입 (쉼표로 구분)
- `schoolRadius`: 학교 반경
- `subwayStationId`: 지하철역 ID
- `subwayStationName`: 지하철역명
- `subwayRadius`: 지하철역 반경

**주의**: `tradeType`은 쿼리 스트링에 포함되지 않음

---

## 2. `/search/map` 페이지 (`front/app/search/map/page.js`)

### 2.1 역할
URL 파라미터를 받아 지도에 마커를 표시하고, 마커 클릭 시 사이드 패널을 표시합니다.

### 2.2 주요 컴포넌트

#### 2.2.1 Filter 컴포넌트 (`components/filter.js`)
- 상단에 위치
- 검색 조건 재설정 가능
- `onSearch` 콜백으로 검색 실행
- `tradeType`을 내부 state로 관리 (쿼리 스트링에 포함하지 않음)

#### 2.2.2 Map 컴포넌트 (`components/map.js`)
- Kakao Maps API 사용
- 아파트 마커 표시
- 버스 정류장 마커 표시 (선택적)
- 마커 클릭 시 `onMarkerClick` 콜백 호출
- 지도 클릭 시 `onMapClick` 콜백 호출

#### 2.2.3 SidePanner 컴포넌트 (`components/sidepanner.js`)
- 좌측 30% 영역에 표시
- 마커 클릭 시 활성화
- 아파트 상세 정보 표시:
  - 월별 거래량 (그래프)
  - 최근 거래내역 (5건)
  - 인근 지하철역 (가까운 3개)
  - 인근 버스 정류장 (반경 500m)
  - 인근 병원 (동 단위)

### 2.3 초기 로드 동작

```javascript
useEffect(() => {
  const sido = searchParams.get('sido');
  const gugun = searchParams.get('gugun');
  
  if (sido && gugun) {
    // URL 파라미터가 있으면 검색 실행
    handleSearch({
      tradeType: '매매', // 기본값 (Filter에서 실제 값이 전달됨)
      searchConditionType: 'region',
      sido,
      gugun,
      dong: searchParams.get('dong'),
      // ... 기타 파라미터
    });
  }
}, [searchParams]);
```

### 2.4 마커 조회 로직

#### 2.4.1 지역 검색 - 매매
```javascript
if (tradeType === '매매') {
  const markerRequest = {
    sido: searchParams.sido,
    gugun: searchParams.gugun,
    dong: searchParams.dong,
    minAmount: searchParams.priceMin ? Number(searchParams.priceMin) * 10000 : undefined,
    maxAmount: searchParams.priceMax ? Number(searchParams.priceMax) * 10000 : undefined,
    periodMonths: searchParams.period || 6,
  };
  const response = await getSaleMarkers(markerRequest);
  // API: GET /api/apartment/trade-sale/markers
}
```

#### 2.4.2 지역 검색 - 전월세
```javascript
else {
  const markerRequest = {
    sido: searchParams.sido,
    gugun: searchParams.gugun,
    dong: searchParams.dong,
    minDeposit: searchParams.depositMin ? Number(searchParams.depositMin) * 10000 : undefined,
    maxDeposit: searchParams.depositMax ? Number(searchParams.depositMax) * 10000 : undefined,
    minMonthlyRent: searchParams.monthlyRentMin ? Number(searchParams.monthlyRentMin) * 10000 : undefined,
    maxMonthlyRent: searchParams.monthlyRentMax ? Number(searchParams.monthlyRentMax) * 10000 : undefined,
  };
  const response = await getRentMarkers(markerRequest);
  // API: GET /api/rent
}
```

### 2.5 마커 클릭 동작

```javascript
handleMarkerClick(markerData) {
  // 1. apartmentId 추출
  const apartmentId = markerData.apartmentData.aptId;
  
  // 2. 아파트 정보 설정
  setSelectedApartment({
    id: apartmentId,
    name: apartment.aptNm,
    address: apartment.roadAddress,
    sido: currentSearchParams?.sido,
    gugun: currentSearchParams?.gugun,
    dong: currentSearchParams?.dong,
    latitude: apartment.latitude,
    longitude: apartment.longitude,
  });
  
  // 3. 사이드 패널 표시
  setShowSidePanner(true);
}
```

### 2.6 사이드 패널 데이터 로드

`SidePanner` 컴포넌트 내부에서 `useEffect`로 다음 API들을 병렬 호출:

```javascript
useEffect(() => {
  if (!apartmentId) return;

  const promises = [
    getMonthlyTradeVolume(apartmentId, selectedPeriod),  // GET /apartments/month-avg/{aptId}/total-rent
    getRecentTrades(apartmentId),                        // GET /rent/{aptId}
    getNearbySubways(apartmentId),                      // GET /apartments/{apartmentId}/subways
    getNearbyBusStations(apartmentId, 500, 50),         // GET /apartments/{apartmentId}/bus-stations
    getHospitalCount({ sido, gugun, dong }),            // GET /api/hospitals/count
    getHospitalStats({ sido, gugun, dong })            // GET /api/hospitals/stats
  ];

  const results = await Promise.all(promises);
  // 결과를 각각의 state에 저장
}, [apartmentId, selectedPeriod, apartmentInfo]);
```

---

## 3. 데이터 흐름

### 3.1 검색 흐름

```
[사용자] 
  ↓
[/search 페이지]
  - 검색 조건 입력
  - 동 미선택 시 모달 표시
  ↓
[동 선택 또는 동이 이미 선택된 경우]
  ↓
[router.push('/search/map?params')]
  ↓
[/search/map 페이지]
  - URL 파라미터 읽기
  - handleSearch() 호출
  ↓
[마커 API 호출]
  - 매매: /api/apartment/trade-sale/markers
  - 전월세: /api/rent
  ↓
[지도에 마커 표시]
```

### 3.2 마커 클릭 흐름

```
[사용자 마커 클릭]
  ↓
[handleMarkerClick()]
  - apartmentId 추출
  - selectedApartment 설정
  - showSidePanner = true
  ↓
[SidePanner 컴포넌트 렌더링]
  ↓
[useEffect 트리거]
  ↓
[병렬 API 호출]
  - 월별 거래량
  - 최근 거래내역
  - 인근 지하철역
  - 인근 버스 정류장
  - 병원 정보
  ↓
[사이드 패널에 데이터 표시]
```

---

## 4. 주요 API 엔드포인트

### 4.1 지역 관련
- `GET /api/regions/sido` - 시도 목록
- `GET /api/regions/gugun?sido={sido}` - 구군 목록
- `GET /api/regions/dong?sido={sido}&gugun={gugun}` - 동 목록
- `GET /api/regions/dong/rank?sido={sido}&gugun={gugun}&periodMonths={period}` - 동 랭킹 (매매)

### 4.2 마커 관련
- `GET /api/apartment/trade-sale/markers?sido={sido}&gugun={gugun}&dong={dong}&minAmount={min}&maxAmount={max}&periodMonths={period}` - 매매 마커
- `GET /api/rent?sido={sido}&gugun={gugun}&dong={dong}&minDeposit={min}&maxDeposit={max}&minMonthlyRent={min}&maxMonthlyRent={max}` - 전월세 마커

### 4.3 전월세 동 목록
- `GET /api/apartments/month-avg/jeonse-count?si={sido}&gu={gugun}&period={period}` - 전세 개수
- `GET /api/apartments/month-avg/wolse-count?si={sido}&gu={gugun}&period={period}` - 월세 개수

### 4.4 사이드 패널 관련
- `GET /apartments/month-avg/{aptId}/total-rent?period={period}` - 월별 거래량
- `GET /rent/{aptId}` - 최근 거래내역 (5건)
- `GET /apartments/{apartmentId}/subways` - 인근 지하철역 (top 3)
- `GET /apartments/{apartmentId}/bus-stations?radiusMeters={radius}&limit={limit}` - 인근 버스 정류장
- `GET /api/hospitals/count?sido={sido}&gugun={gugun}&dong={dong}` - 병원 개수
- `GET /api/hospitals/stats?sido={sido}&gugun={gugun}&dong={dong}` - 병원 통계

---

## 5. State 관리

### 5.1 `/search/page.js`
- `tradeType`: '매매' | '전월세'
- `searchConditionType`: 'region' | 'subway'
- `selectedSido`, `selectedGugun`, `selectedDong`: 지역 선택
- `priceMin`, `priceMax`: 매매가 범위
- `depositMin`, `depositMax`, `monthlyRentMin`, `monthlyRentMax`: 전월세 가격 범위
- `schoolTypes`: 학교 타입 체크박스 상태
- `showDongModal`: 동 목록 모달 표시 여부
- `dongModalList`: 동 목록 모달 데이터

### 5.2 `/search/map/page.js`
- `selectedApartment`: 선택된 아파트 정보
- `showSidePanner`: 사이드 패널 표시 여부
- `apartments`: 지도 마커 데이터 배열
- `mapCenter`: 지도 중심 좌표
- `currentSearchParams`: 현재 검색 파라미터 (마커 클릭 시 사용)

---

## 6. 주요 특징

### 6.1 tradeType 관리
- `tradeType`은 쿼리 스트링에 포함되지 않음
- 각 컴포넌트에서 내부 state로 관리
- API 호출 시에만 사용

### 6.2 동 선택 플로우
- 동을 선택하지 않은 경우: 모달로 동 목록 표시 → 동 선택 → `/search/map` 이동
- 동을 선택한 경우: 바로 `/search/map` 이동

### 6.3 가격 단위 변환
- 프론트엔드: 만원 단위 입력
- API 호출: 원 단위로 변환 (× 10000)

### 6.4 마커 데이터 구조
```javascript
{
  lat: number,           // 위도
  lng: number,           // 경도
  title: string,         // 아파트명
  info: string,         // 주소
  apartmentId: number,  // 아파트 ID
  apartmentData: object  // 전체 아파트 데이터
}
```

---

## 7. 파일 구조

```
front/app/search/
├── page.js                    # 검색 조건 입력 페이지
└── map/
    ├── page.js                # 지도 표시 페이지
    └── components/
        ├── filter.js          # 필터 컴포넌트
        ├── map.js             # 지도 컴포넌트
        └── sidepanner.js      # 사이드 패널 컴포넌트
```

---

## 8. 향후 개선 사항

- [ ] 지하철역 검색도 마커 API로 통일
- [ ] 학교 필터를 마커 조회에 반영
- [ ] 지도 중심을 모든 마커를 포함하도록 자동 조정
- [ ] 매물 상세 페이지 라우팅 구현
