/**
 * SSE (Server-Sent Events) 연결 관리 유틸리티
 * 로그인 시 알림 구독, 로그아웃 시 연결 해제를 담당합니다.
 */

import { subscribeNotifications } from '../api/notification';
import { EventSourcePolyfill } from 'event-source-polyfill';

// 전역 EventSource 인스턴스
let eventSource = null;
let reconnectAttempts = 0;
let reconnectTimer = null;
const MAX_RECONNECT_ATTEMPTS = 5;
const RECONNECT_DELAY = 3000; // 3초

// 알림 수신 콜백 함수들
const notificationCallbacks = new Set();
// 읽지 않은 메시지 개수 콜백 함수들
const unreadCountCallbacks = new Set();
// 채팅방 목록 갱신 콜백 함수들
const roomListUpdateCallbacks = new Set();

/**
 * 알림 수신 콜백 등록
 * @param {Function} callback - 알림 수신 시 호출될 콜백 함수
 */
export function onNotification(callback) {
  if (typeof callback === 'function') {
    notificationCallbacks.add(callback);
  }
}

/**
 * 알림 수신 콜백 제거
 * @param {Function} callback - 제거할 콜백 함수
 */
export function offNotification(callback) {
  notificationCallbacks.delete(callback);
}

/**
 * 읽지 않은 메시지 개수 콜백 등록
 * @param {Function} callback - 읽지 않은 메시지 개수 수신 시 호출될 콜백 함수
 */
export function onUnreadCount(callback) {
  if (typeof callback === 'function') {
    unreadCountCallbacks.add(callback);
  }
}

/**
 * 읽지 않은 메시지 개수 콜백 제거
 * @param {Function} callback - 제거할 콜백 함수
 */
export function offUnreadCount(callback) {
  unreadCountCallbacks.delete(callback);
}

/**
 * 채팅방 목록 갱신 콜백 등록
 * @param {Function} callback - 채팅방 목록 갱신 신호 수신 시 호출될 콜백 함수
 */
export function onRoomListUpdate(callback) {
  if (typeof callback === 'function') {
    roomListUpdateCallbacks.add(callback);
  }
}

/**
 * 채팅방 목록 갱신 콜백 제거
 * @param {Function} callback - 제거할 콜백 함수
 */
export function offRoomListUpdate(callback) {
  roomListUpdateCallbacks.delete(callback);
}

/**
 * 등록된 모든 콜백에 알림 전달
 * @param {Object} notification - 알림 데이터
 */
function notifyCallbacks(notification) {
  notificationCallbacks.forEach((callback) => {
    try {
      callback(notification);
    } catch (error) {
      console.error('Notification callback error:', error);
    }
  });
}

/**
 * SSE 연결 재시도
 */
function attemptReconnect() {
  if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
    console.error('SSE 재연결 시도 횟수 초과');
    return;
  }

  reconnectAttempts++;
  console.log(`SSE 재연결 시도 ${reconnectAttempts}/${MAX_RECONNECT_ATTEMPTS}`);

  reconnectTimer = setTimeout(() => {
    connectSSE();
  }, RECONNECT_DELAY);
}

/**
 * SSE 연결 초기화
 */
export function connectSSE() {
  // 이미 연결되어 있으면 중복 연결 방지
  // EventSourcePolyfill도 EventSource와 동일한 readyState 상수를 사용
  if (eventSource && eventSource.readyState !== EventSourcePolyfill.CLOSED) {
    console.log('SSE가 이미 연결되어 있습니다.');
    return;
  }

  // 로그인 상태 확인
  if (typeof window === 'undefined') {
    return;
  }

  const accessToken = localStorage.getItem('accessToken');
  if (!accessToken) {
    console.log('로그인되지 않아 SSE 연결을 시작할 수 없습니다.');
    return;
  }

  try {
    // 기존 연결이 있으면 먼저 정리
    if (eventSource) {
      eventSource.close();
      eventSource = null;
    }

    // 재연결 시도 횟수 초기화
    reconnectAttempts = 0;
    // 재연결 타이머 초기화
    if (reconnectTimer) {
      clearTimeout(reconnectTimer);
      reconnectTimer = null;
    }

    // SSE 연결 생성
    eventSource = subscribeNotifications();

    // 연결 성공 시
    eventSource.onopen = () => {
      console.log('SSE 연결 성공');
      reconnectAttempts = 0; // 성공 시 재시도 횟수 초기화
    };

    // 메시지 수신 시 (기본 이벤트 - notification 이벤트)
    eventSource.onmessage = (event) => {
      try {
        const notification = JSON.parse(event.data);
        console.log('알림 수신:', notification);
        notifyCallbacks(notification);
      } catch (error) {
        console.error('알림 데이터 파싱 오류:', error);
      }
    };

    // 읽지 않은 메시지 개수 이벤트 구독
    eventSource.addEventListener('unreadCount', (event) => {
      try {
        const count = parseInt(event.data, 10);
        console.log('읽지 않은 메시지 개수:', count);
        unreadCountCallbacks.forEach((callback) => {
          try {
            callback(count);
          } catch (error) {
            console.error('UnreadCount callback error:', error);
          }
        });
      } catch (error) {
        console.error('읽지 않은 메시지 개수 파싱 오류:', error);
      }
    });

    // 채팅방 목록 갱신 이벤트 구독
    eventSource.addEventListener('roomListUpdate', (event) => {
      try {
        console.log('채팅방 목록 갱신 신호 수신');
        roomListUpdateCallbacks.forEach((callback) => {
          try {
            callback();
          } catch (error) {
            console.error('RoomListUpdate callback error:', error);
          }
        });
      } catch (error) {
        console.error('채팅방 목록 갱신 신호 처리 오류:', error);
      }
    });

    // 에러 발생 시
    eventSource.onerror = (error) => {
      console.error('SSE 연결 오류:', error);
      
      // 연결이 끊어진 경우 재연결 시도
      if (eventSource && eventSource.readyState === EventSourcePolyfill.CLOSED) {
        // 재연결 타이머가 없을 때만 재연결 시도
        if (!reconnectTimer) {
          attemptReconnect();
        }
      }
    };
  } catch (error) {
    console.error('SSE 연결 생성 실패:', error);
    eventSource = null;
  }
}

/**
 * SSE 연결 해제
 */
export function disconnectSSE() {
  // 재연결 타이머 취소
  if (reconnectTimer) {
    clearTimeout(reconnectTimer);
    reconnectTimer = null;
  }

  // 재연결 시도 횟수 초기화
  reconnectAttempts = 0;

  // EventSource 연결 종료
  if (eventSource) {
    try {
      eventSource.close();
      console.log('SSE 연결 해제');
    } catch (error) {
      console.error('SSE 연결 해제 오류:', error);
    }
    eventSource = null;
  }

  // 모든 콜백 제거
  notificationCallbacks.clear();
  unreadCountCallbacks.clear();
  roomListUpdateCallbacks.clear();
}

/**
 * SSE 연결 상태 확인
 * @returns {boolean} 연결되어 있으면 true
 */
export function isSSEConnected() {
  return eventSource !== null && eventSource.readyState === EventSourcePolyfill.OPEN;
}

/**
 * SSE 연결 상태 가져오기
 * @returns {string} 연결 상태 ('CONNECTING', 'OPEN', 'CLOSED')
 */
export function getSSEState() {
  if (!eventSource) return 'CLOSED';
  
  const states = {
    [EventSourcePolyfill.CONNECTING]: 'CONNECTING',
    [EventSourcePolyfill.OPEN]: 'OPEN',
    [EventSourcePolyfill.CLOSED]: 'CLOSED',
  };
  
  return states[eventSource.readyState] || 'UNKNOWN';
}

// 기본 export
export default {
  connectSSE,
  disconnectSSE,
  onNotification,
  offNotification,
  onUnreadCount,
  offUnreadCount,
  onRoomListUpdate,
  offRoomListUpdate,
  isSSEConnected,
  getSSEState,
};

