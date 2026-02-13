package org.example.homedatazip.global.batch.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;

import java.util.ArrayList;
import java.util.List;

/**
 * 페이지네이션 기반 외부 API Reader의 공통 로직을 제공하는 추상 클래스
 * 
 * 각 API의 응답 구조가 다르더라도, 페이지네이션과 버퍼링 로직은 공통화 가능
 * 
 * @param <T> API에서 읽어올 아이템 타입
 * @param <R> API 응답 타입
 */
@Slf4j
public abstract class AbstractPagedApiReader<T, R> implements ItemReader<T> {

    protected int currentPage = 1;
    protected final int pageSize;
    protected List<T> buffer = new ArrayList<>();
    protected boolean isEnd = false;
    protected int totalCount = -1; // -1이면 아직 모름

    protected AbstractPagedApiReader(int pageSize) {
        this.pageSize = pageSize;
    }

    @Override
    public T read() {
        if (isEnd && buffer.isEmpty()) {
            return null; // 종료 신호
        }
        
        if (buffer.isEmpty()) {
            fetchNextPage();
        }
        
        return buffer.isEmpty() ? null : buffer.remove(0);
    }

    /**
     * 다음 페이지를 가져와서 buffer에 추가
     */
    private void fetchNextPage() {
        try {
            // 종료 조건 체크
            if (totalCount > 0 && (currentPage - 1) * pageSize >= totalCount) {
                isEnd = true;
                return;
            }

            log.debug("Fetching page {} (pageSize={})", currentPage, pageSize);
            
            // API 호출
            R response = fetchPage(currentPage, pageSize);
            
            if (response == null) {
                log.warn("API 응답이 null입니다. 종료합니다.");
                isEnd = true;
                return;
            }

            // 응답 검증
            if (!isSuccess(response)) {
                String errorMsg = getErrorMessage(response);
                log.error("API 호출 실패: {}", errorMsg);
                isEnd = true;
                return;
            }

            // 총 개수 업데이트 (첫 호출 시)
            if (totalCount < 0) {
                totalCount = getTotalCount(response);
                if (totalCount > 0) {
                    log.info("전체 데이터 건수: {}", totalCount);
                }
            }

            // 아이템 추출
            List<T> items = extractItems(response);
            
            if (items == null || items.isEmpty()) {
                log.info("더 이상 데이터가 없습니다. 종료합니다.");
                isEnd = true;
                return;
            }

            buffer.addAll(items);
            log.info("페이지 {} 로드 완료: {}건 (누적: {})", 
                    currentPage, items.size(), (currentPage - 1) * pageSize + items.size());
            
            currentPage++;

            // Rate limiting (필요시)
            long delay = getDelayMs();
            if (delay > 0) {
                Thread.sleep(delay);
            }

            // 마지막 페이지 체크
            if (isLastPage(response)) {
                isEnd = true;
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("인터럽트 발생", e);
            isEnd = true;
        } catch (Exception e) {
            log.error("API 호출 중 예외 발생: page={}", currentPage, e);
            // 재시도 가능한 에러인지 체크
            if (shouldRetry(e)) {
                // 다음 페이지로 넘어가거나 재시도 로직
                log.warn("재시도 가능한 에러로 판단. 계속 진행합니다.");
            } else {
                isEnd = true;
            }
        }
    }

    /**
     * 특정 페이지의 데이터를 가져옴
     * 각 구현체에서 API 호출 로직 구현
     */
    protected abstract R fetchPage(int page, int pageSize) throws Exception;

    /**
     * 응답에서 실제 아이템 리스트를 추출
     */
    protected abstract List<T> extractItems(R response);

    /**
     * 응답이 성공인지 확인
     */
    protected abstract boolean isSuccess(R response);

    /**
     * 응답에서 에러 메시지 추출
     */
    protected abstract String getErrorMessage(R response);

    /**
     * 응답에서 전체 개수 추출 (선택적)
     */
    protected int getTotalCount(R response) {
        return -1; // 기본값: 알 수 없음
    }

    /**
     * 마지막 페이지인지 확인
     */
    protected boolean isLastPage(R response) {
        // 기본 구현: totalCount와 현재 페이지로 판단
        if (totalCount > 0) {
            return currentPage * pageSize >= totalCount;
        }
        // totalCount를 알 수 없으면 응답이 비어있으면 마지막 페이지로 간주
        List<T> items = extractItems(response);
        return items == null || items.isEmpty();
    }

    /**
     * Rate limiting을 위한 딜레이 (밀리초)
     * 오버라이드하여 각 API의 rate limit에 맞게 조정
     */
    protected long getDelayMs() {
        return 100; // 기본값: 100ms
    }

    /**
     * 예외 발생 시 재시도할지 여부
     * 오버라이드하여 특정 예외는 재시도, 특정 예외는 중단하도록 구현
     */
    protected boolean shouldRetry(Exception e) {
        // 기본값: 재시도 안 함
        return false;
    }
}
