# 백엔드 코드 리뷰 및 피드백

> 작성일: 2026-02-02  
> 리뷰 대상: `back/homedatazip/src/main/java/org/example/homedatazip/`

---

## 1. 현재 기능이 돌아는가지만 사실 아슬아슬 위험한 코드

### 1.1 보안 관련

#### 🔴 **SecurityConfig - CSRF 비활성화 및 과도한 permitAll**
**위치**: `global/config/SecurityConfig.java`

**문제점**:
- CSRF가 완전히 비활성화되어 있음 (`csrf(AbstractHttpConfigurer::disable)`)
- 많은 엔드포인트가 `permitAll()`로 설정되어 있어 인증 없이 접근 가능
- `/api/listings/**` 전체가 permitAll (TODO 주석에 "추후 create,me는 seller만 가능"이라고 되어 있음)
- `/api/subscriptions/billing/**` 전체가 permitAll (TODO 주석에 "추후 로그인한 사람에 한해 가능"이라고 되어 있음)

**위험도**: 🔴 **매우 높음**
- CSRF 공격에 취약
- 인증이 필요한 API가 무단 접근 가능
- 운영 환경에서 심각한 보안 취약점

**권장사항**:
```java
// CSRF는 REST API의 경우 쿠키 기반 인증이 아닌 이상 비활성화 가능하지만,
// JWT를 사용하는 경우에도 일부 엔드포인트는 CSRF 보호 필요
// permitAll을 최소화하고 필요한 엔드포인트만 허용
.requestMatchers("/api/listings/create", "/api/listings/me").hasRole("SELLER")
.requestMatchers("/api/subscriptions/**").authenticated()
```

---

#### 🔴 **AuthService - Secure 쿠키 하드코딩**
**위치**: `auth/service/AuthService.java:125`

**문제점**:
```java
boolean secure = false;     //운영할 떈 true 바꾸기
```
- 운영 환경에서도 `false`로 설정되어 있을 가능성
- HTTPS 환경에서 쿠키가 암호화되지 않아 탈취 위험

**위험도**: 🔴 **높음**

**권장사항**:
```java
// 환경 변수나 프로파일로 관리
boolean secure = !"dev".equals(activeProfile);
// 또는
@Value("${app.cookie.secure:true}")
private boolean cookieSecure;
```

---

#### 🟡 **JwtTokenizer - 예외 처리가 너무 넓음**
**위치**: `global/jwt/util/JwtTokenizer.java:53-60, 62-69`

**문제점**:
```java
public boolean validateAccessToken(String token) {
    try{
        parseAccessToken(token);
        return true;
    } catch(Exception e){  // 너무 넓은 예외 처리
        return false;
    }
}
```
- 모든 예외를 무시하고 `false`만 반환
- 디버깅이 어려움
- 어떤 종류의 예외인지 알 수 없음

**위험도**: 🟡 **중간**

**권장사항**:
```java
public boolean validateAccessToken(String token) {
    try {
        parseAccessToken(token);
        return true;
    } catch (ExpiredJwtException e) {
        log.debug("Token expired: {}", e.getMessage());
        return false;
    } catch (JwtException e) {
        log.warn("Invalid token: {}", e.getMessage());
        return false;
    } catch (Exception e) {
        log.error("Unexpected error validating token", e);
        return false;
    }
}
```

---

### 1.2 예외 처리 관련

#### 🔴 **GlobalExceptionHandler - Exception을 너무 넓게 catch**
**위치**: `global/exception/common/GlobalExceptionHandler.java:22-32`

**문제점**:
```java
@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponse> handleException(Exception e) {
    log.error("Unhandled exception", e);
    return ResponseEntity
            .internalServerError()
            .body(ErrorResponse.of(
                    "INTERNAL_SERVER_ERROR",
                    "서버 오류가 발생햇습니다."  // 오타도 있음
            ));
}
```
- 모든 예외를 500으로 처리
- 클라이언트가 실제 원인을 알 수 없음
- 오타: "발생햇습니다" → "발생했습니다"

**위험도**: 🔴 **높음**

**권장사항**:
```java
// 구체적인 예외 타입별로 처리
@ExceptionHandler(IllegalArgumentException.class)
public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
    return ResponseEntity.badRequest()
            .body(ErrorResponse.of("INVALID_ARGUMENT", e.getMessage()));
}

@ExceptionHandler(DataAccessException.class)
public ResponseEntity<ErrorResponse> handleDataAccess(DataAccessException e) {
    log.error("Database error", e);
    return ResponseEntity.status(503)
            .body(ErrorResponse.of("DATABASE_ERROR", "데이터베이스 오류가 발생했습니다."));
}

// 마지막에만 일반 Exception 처리
@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponse> handleException(Exception e) {
    log.error("Unhandled exception", e);
    return ResponseEntity.internalServerError()
            .body(ErrorResponse.of("INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다."));
}
```

---

#### 🟡 **ChatService - RuntimeException 사용**
**위치**: `chat/service/ChatService.java:67-71`

**문제점**:
```java
Listing listing = listingRepository.findById(listingId)
    .orElseThrow(() -> {
        log.error("매물이 존재하지 않습니다. listingId={}", listingId);
        return new RuntimeException("매물이 존재하지 않습니다.");  // TODO 주석 있음
    });
```
- `RuntimeException` 대신 `BusinessException` 사용해야 함
- TODO 주석이 있지만 아직 수정되지 않음

**위험도**: 🟡 **중간**

**권장사항**:
```java
Listing listing = listingRepository.findById(listingId)
    .orElseThrow(() -> new BusinessException(ListingErrorCode.LISTING_NOT_FOUND));
```

---

### 1.3 트랜잭션 관련

#### 🟡 **ApartmentService - 트랜잭션 전파 문제 가능성**
**위치**: `apartment/service/ApartmentService.java:38-66`

**문제점**:
- `getOrCreateApartmentsFromTradeSale` 메서드가 `@Transactional`이지만
- 내부에서 `apartmentSaveService.saveAndGetApartment`를 호출하는데 이는 `REQUIRES_NEW`
- 루프 내에서 외부 API 호출(`geoService.convertCoordinateInfo`)이 있어 실패 시 롤백 범위가 불명확

**위험도**: 🟡 **중간**

**권장사항**:
- 배치 처리 시에는 각 항목별로 독립적인 트랜잭션 처리 고려
- 실패한 항목만 스킵하고 나머지는 계속 처리하는 로직 명확화

---

## 2. 이정도는 괜찮지만 사실 더 좋은 방법이 있는 코드

### 2.1 트랜잭션 최적화

#### 📝 **@Transactional(readOnly = true) 누락**
**위치**: 여러 Service 클래스

**문제점**:
- 조회 메서드에 `readOnly = true`가 누락된 경우가 많음
- 예: `ListingQueryService`, `ChatService.getRooms()` 등

**개선사항**:
```java
@Transactional(readOnly = true)  // 추가
public List<ChatRoomListResponse> getRooms(Long userId) {
    // ...
}
```

**효과**:
- 읽기 전용 트랜잭션으로 성능 향상
- 불필요한 쓰기 락 방지

---

### 2.2 Pagination 일관성

#### 📝 **Pagination 구현이 일관되지 않음**
**위치**: `listing/repository/ListingRepository.java`, `chat/repository/ChatMessageRepository.java`

**문제점**:
- `ListingRepository`는 `Pageable`을 받지만 `List` 반환
- `ChatMessageRepository`는 `Slice` 반환 (더 나은 방법)
- 일부는 limit만 받아서 처리

**개선사항**:
```java
// 일관된 Pagination 전략 수립
// 1. Slice 사용 (무한 스크롤에 적합)
Slice<Listing> searchActive(Long regionId, Long apartmentId, TradeType tradeType, Pageable pageable);

// 2. 또는 Page 사용 (총 개수 필요 시)
Page<Listing> searchActive(Long regionId, Long apartmentId, TradeType tradeType, Pageable pageable);
```

---

### 2.3 Validation 개선

#### 📝 **DTO Validation이 일부만 적용**
**위치**: `listing/dto/ListingCreateRequest.java`

**문제점**:
- `@NotNull`, `@Min` 등 기본 validation만 있음
- 비즈니스 로직 validation은 Service 레이어에서 처리 (`validateCreate` 메서드)
- DTO 레벨에서 더 많은 validation 가능

**개선사항**:
```java
// Custom Validator 사용
@ValidTradeType  // 커스텀 validator
TradeType tradeType;

// 또는 그룹 validation
@NotNull(groups = SaleGroup.class)
Long salePrice;

@NotNull(groups = RentGroup.class)
Long deposit;
```

---

### 2.4 외부 API 호출 개선

#### 📝 **Retry 로직이 일관되지 않음**
**위치**: `global/batch/busstation/tasklet/BusStationGeocodeTasklet.java`, `global/geocode/service/GeoService.java`

**문제점**:
- 일부는 수동 retry 구현 (`BusStationGeocodeTasklet`)
- 일부는 Spring Retry 미사용
- `BackOffPolicyConfig`가 있지만 활용되지 않음

**개선사항**:
```java
// Spring Retry 활용
@Retryable(
    value = {WebClientResponseException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 1000, multiplier = 2)
)
public Region convertAddressInfo(double lat, double lon) {
    // ...
}
```

---

#### 📝 **외부 API 호출 패턴 중복 - 공통화 방안**
**위치**: 여러 Reader 클래스 (`BusStationApiReader`, `HospitalApiReader`, `SchoolApiReader`, `RegionApiReader` 등)

**현재 상황**:
- 각 Reader마다 비슷한 패턴이 반복됨:
  - 페이지네이션 처리 (startIndex, pageNo 등)
  - 버퍼링/Iterator 패턴
  - API 호출 및 에러 처리
  - 종료 조건 체크
  - Rate limiting

**문제점**:
- 코드 중복이 많지만, 각 API의 응답 구조가 달라서 공통화가 어려워 보임
- 하지만 **페이지네이션 로직과 버퍼링 로직은 공통화 가능**

**공통화 방안**:

**1. 제네릭 기반 공통 Reader 추상 클래스**

```java
// 공통 Reader 추상 클래스
public abstract class AbstractPagedApiReader<T> implements ItemReader<T> {
    
    protected int currentPage = 1;
    protected final int pageSize;
    protected List<T> buffer = new ArrayList<>();
    protected boolean isEnd = false;
    
    protected AbstractPagedApiReader(int pageSize) {
        this.pageSize = pageSize;
    }
    
    @Override
    public T read() {
        if (isEnd && buffer.isEmpty()) return null;
        if (buffer.isEmpty()) {
            fetchNextPage();
        }
        return buffer.isEmpty() ? null : buffer.remove(0);
    }
    
    // 각 구현체에서 구현
    protected abstract ApiResponse<T> fetchPage(int page, int pageSize);
    protected abstract List<T> extractItems(ApiResponse<T> response);
    protected abstract boolean isLastPage(ApiResponse<T> response);
    
    private void fetchNextPage() {
        try {
            ApiResponse<T> response = fetchPage(currentPage, pageSize);
            if (response == null || isLastPage(response)) {
                isEnd = true;
                return;
            }
            
            List<T> items = extractItems(response);
            if (items == null || items.isEmpty()) {
                isEnd = true;
                return;
            }
            
            buffer.addAll(items);
            currentPage++;
            
            // Rate limiting
            Thread.sleep(getDelayMs());
            
        } catch (Exception e) {
            log.error("API 호출 실패: page={}", currentPage, e);
            isEnd = true;
        }
    }
    
    protected long getDelayMs() {
        return 100; // 기본값, 오버라이드 가능
    }
}
```

**2. 각 Reader는 추상 클래스 상속**

```java
@Component
@StepScope
public class HospitalApiReader extends AbstractPagedApiReader<HospitalApiResponse.HospitalItem> {
    
    private final HospitalApiClient client;
    
    public HospitalApiReader(HospitalApiClient client) {
        super(1000); // pageSize
        this.client = client;
    }
    
    @Override
    protected ApiResponse<HospitalApiResponse.HospitalItem> fetchPage(int page, int pageSize) {
        return client.fetchHospital(page, pageSize);
    }
    
    @Override
    protected List<HospitalApiResponse.HospitalItem> extractItems(ApiResponse response) {
        return ((HospitalApiResponse) response).getItems();
    }
    
    @Override
    protected boolean isLastPage(ApiResponse response) {
        HospitalApiResponse hospitalResponse = (HospitalApiResponse) response;
        return currentPage * pageSize >= hospitalResponse.getTotalCount();
    }
}
```

**3. API Client 인터페이스 추상화 (선택사항)**

```java
// 공통 API Client 인터페이스
public interface PagedApiClient<T> {
    ApiResponse<T> fetch(int page, int pageSize);
    int getTotalCount();
}

// 구현 예시
@Component
public class HospitalApiClient implements PagedApiClient<HospitalApiResponse.HospitalItem> {
    // 기존 코드...
}
```

**4. Response 추상화 (더 고급)**

```java
// 공통 Response 인터페이스
public interface PagedApiResponse<T> {
    List<T> getItems();
    int getTotalCount();
    boolean isSuccess();
    String getErrorMessage();
}

// 각 API Response가 이 인터페이스 구현
public record HospitalApiResponse(...) implements PagedApiResponse<HospitalItem> {
    // ...
}
```

**장점**:
- ✅ 페이지네이션 로직 중복 제거
- ✅ 버퍼링 로직 중복 제거
- ✅ Rate limiting 공통 처리
- ✅ 에러 처리 일관성
- ✅ 각 API의 특수성은 추상 메서드로 처리

**단점**:
- ⚠️ 초기 구현 비용
- ⚠️ 모든 API가 같은 패턴을 따르지 않을 수 있음 (예: StationApiReader는 한 번에 모든 데이터 로드)

**권장사항**:
- 단계적으로 적용: 먼저 비슷한 패턴의 Reader들부터 공통화
- 완전한 공통화보다는 **공통 유틸리티 클래스**로 시작하는 것도 좋은 방법

---

### 2.5 미완성 메서드

#### 📝 **ListingService.update() 미완성**
**위치**: `listing/service/ListingService.java:67-70`

**문제점**:
```java
// 수정 (추후 작성)
@Transactional
public void update(Long userId, Long listingId /*, ListingUpdateRequest req */) {
    // 비어있음
}
```

**개선사항**:
- 미완성 메서드는 제거하거나 추상 메서드로 표시
- 또는 `@Deprecated` + TODO 주석으로 명확히 표시

---

## 3. 해당 서비스에 더 있었으면 하는 기능

### 3.1 보안 강화

#### 🔵 **API Rate Limiting**
- 현재 외부 API 호출에만 rate limiting이 있음
- 사용자 API 호출에 대한 rate limiting 필요
- Spring Cloud Gateway 또는 Bucket4j 활용

**예시**:
```java
@RateLimiter(name = "default")
@GetMapping("/api/listings")
public ResponseEntity<List<ListingSearchResponse>> search(...) {
    // ...
}
```

---

#### 🔵 **API Key 관리**
- 외부 API 키가 하드코딩되어 있거나 환경 변수로만 관리
- API Key Rotation 기능
- 키별 사용량 모니터링

---

### 3.2 성능 최적화

#### 🔵 **Caching 전략**
- 현재 캐싱이 거의 없음
- 자주 조회되는 데이터에 캐싱 적용:
  - 지역 정보 (Region)
  - 아파트 기본 정보
  - 학교/지하철역 정보

**예시**:
```java
@Cacheable(value = "regions", key = "#sido + '_' + #gugun + '_' + #dong")
public Region findRegion(String sido, String gugun, String dong) {
    // ...
}
```

---

#### 🔵 **Database Connection Pool 최적화**
- `application.yaml`에 connection pool 설정이 보이지 않음
- HikariCP 설정 추가 필요

---

### 3.3 모니터링 및 로깅

#### 🔵 **구조화된 로깅**
- 현재 로그가 일관되지 않음
- JSON 로깅 도입 (Logback JSON Encoder)
- 로그 레벨 관리

---

#### 🔵 **메트릭 수집**
- Micrometer + Prometheus 연동
- 주요 비즈니스 메트릭:
  - API 응답 시간
  - 에러율
  - 배치 작업 성공/실패율
  - 외부 API 호출 횟수

---

#### 🔵 **분산 추적 (Distributed Tracing)**
- Spring Cloud Sleuth 또는 Zipkin
- 마이크로서비스 간 호출 추적

---

### 3.4 테스트 코드

#### 🔵 **단위 테스트 부족**
- 현재 테스트 코드가 거의 없음
- Service 레이어 단위 테스트 추가
- Repository 테스트 (TestContainers 활용)

---

#### 🔵 **통합 테스트**
- API 통합 테스트
- @SpringBootTest 활용

---

### 3.5 기능 개선

#### 🔵 **검색 기능 강화**
- 현재 기본적인 검색만 지원
- Elasticsearch 도입 고려:
  - 아파트명, 주소 풀텍스트 검색
  - 가격 범위, 면적 등 복합 검색

---

#### 🔵 **이미지 업로드 기능**
- 현재 이미지 관련 기능이 보이지 않음
- 매물 이미지 업로드 기능 필요
- S3 또는 로컬 스토리지 연동

---

#### 🔵 **알림 기능 확장**
- 현재 SSE 기반 알림만 있음
- 이메일 알림 추가
- 푸시 알림 (FCM) 추가

---

#### 🔵 **통계 및 분석**
- 사용자 행동 분석
- 인기 매물 통계
- 지역별 거래 추이

---

## 4. 전체적으로 피드백

### 4.1 아키텍처

#### ✅ **잘된 점**
- 계층 구조가 명확함 (Controller → Service → Repository)
- DTO 패턴 적절히 사용
- Exception 처리 구조가 잘 설계됨 (ErrorCode, BusinessException)
- 배치 처리 구조가 체계적임

#### ⚠️ **개선 필요**
- 일부 Service가 너무 많은 책임을 가짐 (예: `ApartmentService`)
- 도메인별로 더 명확한 경계 필요
- 일부 비즈니스 로직이 Entity에 있는 것이 좋지만, 일관성 필요

---

### 4.2 코드 품질

#### ✅ **잘된 점**
- Lombok 적절히 활용
- Record 타입 적극 사용 (DTO)
- 네이밍이 대체로 명확함

#### ⚠️ **개선 필요**
- 주석이 부족함 (특히 복잡한 비즈니스 로직)
- 매직 넘버/문자열이 일부 있음
- 코드 중복이 일부 있음 (예: 외부 API 호출 패턴)

---

### 4.3 보안

#### ⚠️ **주요 개선 사항**
1. **인증/인가 강화**
   - 현재 많은 엔드포인트가 `permitAll()`
   - 역할 기반 접근 제어 (RBAC) 더 세밀하게 적용 필요

2. **입력 검증**
   - DTO validation은 있지만, 일부는 Service 레이어에서만 검증
   - Controller 레벨에서 `@Valid` 적용 확인 필요

3. **SQL Injection 방지**
   - JPA 사용으로 대부분 방지되지만, Native Query 사용 시 주의 필요

4. **XSS 방지**
   - 프론트엔드에서 처리하는 것으로 보이지만, 백엔드에서도 검증 필요

---

### 4.4 성능

#### ⚠️ **개선 필요**
1. **N+1 문제**
   - `@EntityGraph` 또는 `fetch join` 활용 확인 필요
   - 예: `ChatService.getRooms()`에서 연관 엔티티 조회 시

2. **배치 처리 최적화**
   - 대용량 데이터 처리 시 메모리 관리
   - 청크 크기 조정

3. **캐싱 전략**
   - 거의 없음 → 추가 필요

---

### 4.5 유지보수성

#### ✅ **잘된 점**
- 패키지 구조가 도메인별로 잘 나뉘어 있음
- 공통 기능이 `global` 패키지에 잘 정리됨

#### ⚠️ **개선 필요**
1. **문서화**
   - API 문서 (Swagger/OpenAPI) 추가 필요
   - README에 아키텍처 설명 추가

2. **설정 관리**
   - 하드코딩된 값들을 설정 파일로 이동
   - 프로파일별 설정 분리

3. **에러 메시지**
   - 오타 수정 ("발생햇습니다" → "발생했습니다")
   - 에러 메시지 일관성

---

### 4.6 테스트

#### ⚠️ **시급한 개선**
- **테스트 코드가 거의 없음**
- 단위 테스트, 통합 테스트 추가 필요
- 테스트 커버리지 목표 설정 (예: 70% 이상)

---

## 5. 우선순위별 개선 권장사항

### 🔴 **즉시 수정 (Critical)**
1. SecurityConfig의 과도한 `permitAll()` 제거
2. `AuthService`의 `secure = false` 환경 변수화
3. `GlobalExceptionHandler`의 오타 수정 및 예외 처리 개선
4. `ChatService`의 `RuntimeException` → `BusinessException` 변경

### 🟡 **단기 개선 (High Priority)**
1. `@Transactional(readOnly = true)` 추가
2. Pagination 일관성 확보
3. 테스트 코드 작성 시작
4. API 문서화 (Swagger)

### 🔵 **중기 개선 (Medium Priority)**
1. Caching 전략 수립 및 적용
2. Rate Limiting 구현
3. 로깅 구조화
4. 메트릭 수집 도입

### 🟢 **장기 개선 (Low Priority)**
1. Elasticsearch 도입 검토
2. 이미지 업로드 기능
3. 알림 기능 확장
4. 통계/분석 기능

---

## 6. 결론

전체적으로 **아키텍처는 잘 설계**되어 있고, **코드 구조도 깔끔**합니다. 다만 **보안**과 **예외 처리** 부분에서 개선이 필요하며, **테스트 코드**가 거의 없는 것이 가장 큰 약점입니다.

**강점**:
- ✅ 명확한 계층 구조
- ✅ 적절한 DTO 패턴 사용
- ✅ 체계적인 배치 처리
- ✅ Exception 처리 구조

**약점**:
- ❌ 보안 설정이 느슨함
- ❌ 테스트 코드 부족
- ❌ 일부 미완성 코드
- ❌ 성능 최적화 여지

**종합 평가**: ⭐⭐⭐⭐ (4/5)
- 기능적으로는 잘 동작하지만, 운영 환경을 고려한 보안과 안정성 개선이 필요합니다.

---

*이 문서는 코드베이스 분석을 기반으로 작성되었으며, 실제 운영 환경에 맞게 우선순위를 조정하여 적용하시기 바랍니다.*
