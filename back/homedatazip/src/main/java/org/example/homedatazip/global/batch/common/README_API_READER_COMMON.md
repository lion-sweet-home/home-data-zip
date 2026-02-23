# 외부 API Reader 공통화 가이드

## 개요

외부 API 호출 패턴에서 반복되는 코드(페이지네이션, 버퍼링, 에러 처리 등)를 공통화하기 위한 가이드입니다.

## 문제점

현재 각 Reader마다 비슷한 패턴이 반복됨:
- `BusStationApiReader`: startIndex/endIndex 기반 페이지네이션
- `HospitalApiReader`: pageNo/numOfRows 기반 페이지네이션
- `SchoolApiReader`: pageNo/numOfRows 기반 페이지네이션
- `RegionApiReader`: page/perPage 기반 페이지네이션

각각의 응답 구조는 다르지만, **페이지네이션 로직과 버퍼링 로직은 동일**합니다.

## 해결 방안

### 1. AbstractPagedApiReader 사용

`AbstractPagedApiReader<T, R>`를 상속받아 구현:

- `T`: 읽어올 아이템 타입 (예: `HospitalApiResponse.HospitalItem`)
- `R`: API 응답 타입 (예: `HospitalApiResponse`)

### 2. 구현 예시

#### HospitalApiReader 개선 예시

**Before (기존 코드)**:
```java
@Component
@StepScope
public class HospitalApiReader implements ItemReader<HospitalApiResponse.HospitalItem> {
    private Iterator<HospitalApiResponse.HospitalItem> iterator;
    private int currentPage = 1;
    private int totalCount = -1;
    private int processedCount = 0;
    private final int pageSize = 1000;

    @Override
    public synchronized HospitalApiResponse.HospitalItem read() {
        if (iterator == null || !iterator.hasNext()) {
            if (processedCount >= totalCount && totalCount != -1) {
                return null;
            }
            // ... 페이지네이션 로직 반복
        }
        // ...
    }
}
```

**After (공통화 후)**:
```java
@Component
@StepScope
public class HospitalApiReader extends AbstractPagedApiReader<HospitalApiResponse.HospitalItem, HospitalApiResponse> {
    
    private final HospitalApiClient client;
    
    public HospitalApiReader(HospitalApiClient client) {
        super(1000); // pageSize
        this.client = client;
    }
    
    @Override
    protected HospitalApiResponse fetchPage(int page, int pageSize) throws Exception {
        return client.fetchHospital(page, pageSize);
    }
    
    @Override
    protected List<HospitalApiResponse.HospitalItem> extractItems(HospitalApiResponse response) {
        return response.getItems();
    }
    
    @Override
    protected boolean isSuccess(HospitalApiResponse response) {
        return response.isSuccess();
    }
    
    @Override
    protected String getErrorMessage(HospitalApiResponse response) {
        return response.getHeader().getResultMsg();
    }
    
    @Override
    protected int getTotalCount(HospitalApiResponse response) {
        return response.getTotalCount();
    }
    
    @Override
    protected long getDelayMs() {
        return 100; // Hospital API rate limit
    }
}
```

#### BusStationApiReader 개선 예시

**Before**:
```java
public class BusStationApiReader implements ItemReader<Row> {
    private int startIndex = 1;
    private final int pageSize = 1000;
    private final List<Row> buffer = new ArrayList<>();
    private boolean isEnd = false;
    
    @Override
    public Row read() {
        if (isEnd && buffer.isEmpty()) return null;
        if (buffer.isEmpty()) fetch();
        return buffer.isEmpty() ? null : buffer.remove(0);
    }
    
    private void fetch() {
        // ... 페이지네이션 로직
    }
}
```

**After**:
```java
@Component
@StepScope
public class BusStationApiReader extends AbstractPagedApiReader<SeoulBusStopResponse.Row, SeoulBusStopResponse> {
    
    private final SeoulBusOpenApiClient client;
    private int startIndex = 1;
    
    public BusStationApiReader(SeoulBusOpenApiClient client) {
        super(1000);
        this.client = client;
    }
    
    @Override
    protected SeoulBusStopResponse fetchPage(int page, int pageSize) {
        int start = (page - 1) * pageSize + 1;
        int end = start + pageSize - 1;
        return client.fetch(start, end);
    }
    
    @Override
    protected List<SeoulBusStopResponse.Row> extractItems(SeoulBusStopResponse response) {
        if (response == null || response.busStopLocationXyInfo() == null) {
            return List.of();
        }
        return response.busStopLocationXyInfo().row();
    }
    
    @Override
    protected boolean isSuccess(SeoulBusStopResponse response) {
        if (response == null || response.busStopLocationXyInfo() == null) {
            return false;
        }
        var result = response.busStopLocationXyInfo().result();
        if (result == null) return true; // result가 없으면 성공으로 간주
        String code = result.code();
        return code == null || "INFO-000".equalsIgnoreCase(code);
    }
    
    @Override
    protected String getErrorMessage(SeoulBusStopResponse response) {
        if (response == null || response.busStopLocationXyInfo() == null) {
            return "Response is null";
        }
        var result = response.busStopLocationXyInfo().result();
        return result != null ? result.message() : "Unknown error";
    }
    
    @Override
    protected int getTotalCount(SeoulBusStopResponse response) {
        if (response == null || response.busStopLocationXyInfo() == null) {
            return -1;
        }
        return response.busStopLocationXyInfo().listTotalCount();
    }
}
```

## 장점

1. **코드 중복 제거**: 페이지네이션 로직이 한 곳에 집중
2. **일관성**: 모든 Reader가 동일한 패턴 사용
3. **유지보수성**: 공통 로직 수정 시 한 곳만 수정
4. **테스트 용이성**: 공통 로직은 추상 클래스에서 테스트

## 주의사항

1. **모든 API가 같은 패턴을 따르지 않을 수 있음**
   - 예: `StationApiReader`는 한 번에 모든 데이터를 로드
   - 이런 경우는 공통화하지 않고 그대로 유지

2. **점진적 적용**
   - 모든 Reader를 한 번에 변경하지 말고, 비슷한 패턴부터 적용
   - 기존 코드와 병행하여 안정성 확인 후 전환

3. **응답 구조가 너무 다를 경우**
   - 공통화보다는 각각 유지하는 것이 나을 수 있음
   - 과도한 추상화는 오히려 복잡도 증가

## 대안: 유틸리티 클래스 방식

완전한 추상화가 부담스러우면, 공통 유틸리티 메서드로 시작:

```java
public class ApiReaderUtils {
    public static <T> T readFromBuffer(List<T> buffer, Supplier<List<T>> fetcher) {
        if (buffer.isEmpty()) {
            List<T> items = fetcher.get();
            if (items == null || items.isEmpty()) {
                return null;
            }
            buffer.addAll(items);
        }
        return buffer.remove(0);
    }
}
```

이 방식은 더 가볍지만, 추상 클래스보다는 덜 강력합니다.
