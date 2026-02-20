/**
 * SSE (Server-Sent Events) 연결 관리 유틸리티
 *
 * A안: 채팅 SSE / 공지 SSE를 분리한다.
 * - chat SSE: unreadCount, roomListUpdate
 * - notification SSE: notification (named event)
 */

import { EventSourcePolyfill } from 'event-source-polyfill';
import { subscribeChatEvents } from '../api/chat';
import { subscribeNotifications } from '../api/notification';

// event-source-polyfill 내부에서 찍는 타임아웃/재연결 에러는 콘솔에 안 나오게 필터
(function suppressSSETimeoutConsoleError() {
  if (typeof console === 'undefined' || !console.error) return;
  const original = console.error;
  function getMessage(arg) {
    if (arg == null) return '';
    if (typeof arg === 'string') return arg;
    if (typeof arg === 'object' && arg.message != null) return String(arg.message);
    return '';
  }
  console.error = function (...args) {
    const combined = args.map(getMessage).join(' ');
    if (
      combined.includes('No activity within') ||
      combined.includes('Reconnecting')
    ) {
      return;
    }
    original.apply(console, args);
  };
})();

// 채널별 EventSource 인스턴스
let chatEventSource = null;
let notificationEventSource = null;

let chatReconnectAttempts = 0;
let chatReconnectTimer = null;

let notificationReconnectAttempts = 0;
let notificationReconnectTimer = null;

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
 * SSE 연결 재시도 (chat)
 */
function attemptChatReconnect() {
  if (chatReconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
    console.error('Chat SSE 재연결 시도 횟수 초과');
    return;
  }

  chatReconnectAttempts++;
  console.log(`Chat SSE 재연결 시도 ${chatReconnectAttempts}/${MAX_RECONNECT_ATTEMPTS}`);

  chatReconnectTimer = setTimeout(() => {
    connectChatSSE();
  }, RECONNECT_DELAY);
}

/**
 * SSE 연결 재시도 (notification)
 */
function attemptNotificationReconnect() {
  if (notificationReconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
    console.error('Notification SSE 재연결 시도 횟수 초과');
    return;
  }

  notificationReconnectAttempts++;
  console.log(
    `Notification SSE 재연결 시도 ${notificationReconnectAttempts}/${MAX_RECONNECT_ATTEMPTS}`
  );

  notificationReconnectTimer = setTimeout(() => {
    connectNotificationSSE();
  }, RECONNECT_DELAY);
}

/**
 * Chat SSE 연결 초기화
 */
export function connectChatSSE() {
  if (chatEventSource && chatEventSource.readyState !== EventSourcePolyfill.CLOSED) {
    return;
  }

  if (typeof window === 'undefined') return;
  const accessToken = localStorage.getItem('accessToken');
  if (!accessToken) return;

  try {
    if (chatEventSource) {
      chatEventSource.close();
      chatEventSource = null;
    }

    chatReconnectAttempts = 0;
    if (chatReconnectTimer) {
      clearTimeout(chatReconnectTimer);
      chatReconnectTimer = null;
    }

    chatEventSource = subscribeChatEvents();

    chatEventSource.onopen = () => {
      chatReconnectAttempts = 0;
    };

    chatEventSource.addEventListener('unreadCount', (event) => {
      try {
        const count = parseInt(event.data, 10);
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

    chatEventSource.addEventListener('roomListUpdate', () => {
      roomListUpdateCallbacks.forEach((callback) => {
        try {
          callback();
        } catch (error) {
          console.error('RoomListUpdate callback error:', error);
        }
      });
    });

    chatEventSource.onerror = (error) => {
      const willReconnect =
        chatEventSource &&
        chatEventSource.readyState === EventSourcePolyfill.CLOSED &&
        !chatReconnectTimer;
      if (willReconnect) {
        attemptChatReconnect();
        console.log('Chat SSE 재연결 중...');
      } else if (!chatReconnectTimer) {
        console.error('Chat SSE 연결 오류:', error);
      }
    };
  } catch (error) {
    console.error('Chat SSE 연결 생성 실패:', error);
    chatEventSource = null;
  }
}

/**
 * Notification SSE 연결 초기화
 */
export function connectNotificationSSE() {
  if (notificationEventSource && notificationEventSource.readyState !== EventSourcePolyfill.CLOSED) {
    return;
  }

  if (typeof window === 'undefined') return;
  const accessToken = localStorage.getItem('accessToken');
  if (!accessToken) return;

  try {
    if (notificationEventSource) {
      notificationEventSource.close();
      notificationEventSource = null;
    }

    notificationReconnectAttempts = 0;
    if (notificationReconnectTimer) {
      clearTimeout(notificationReconnectTimer);
      notificationReconnectTimer = null;
    }

    notificationEventSource = subscribeNotifications();

    notificationEventSource.onopen = () => {
      notificationReconnectAttempts = 0;
    };

    // named event: notification
    notificationEventSource.addEventListener('notification', (event) => {
      try {
        const notification = JSON.parse(event.data);
        notifyCallbacks(notification);
      } catch (error) {
        console.error('알림 데이터 파싱 오류:', error);
      }
    });

    // fallback (혹시 기본 message로 오는 경우)
    notificationEventSource.onmessage = (event) => {
      try {
        const notification = JSON.parse(event.data);
        notifyCallbacks(notification);
      } catch (error) {
        console.error('알림 데이터 파싱 오류:', error);
      }
    };

    notificationEventSource.onerror = (error) => {
      const willReconnect =
        notificationEventSource &&
        notificationEventSource.readyState === EventSourcePolyfill.CLOSED &&
        !notificationReconnectTimer;
      if (willReconnect) {
        attemptNotificationReconnect();
        console.log('Notification SSE 재연결 중...');
      } else if (!notificationReconnectTimer) {
        console.error('Notification SSE 연결 오류:', error);
      }
    };
  } catch (error) {
    console.error('Notification SSE 연결 생성 실패:', error);
    notificationEventSource = null;
  }
}

export function disconnectChatSSE() {
  if (chatReconnectTimer) {
    clearTimeout(chatReconnectTimer);
    chatReconnectTimer = null;
  }
  chatReconnectAttempts = 0;

  if (chatEventSource) {
    try {
      chatEventSource.close();
    } catch (error) {
      console.error('Chat SSE 연결 해제 오류:', error);
    }
    chatEventSource = null;
  }
}

export function disconnectNotificationSSE() {
  if (notificationReconnectTimer) {
    clearTimeout(notificationReconnectTimer);
    notificationReconnectTimer = null;
  }
  notificationReconnectAttempts = 0;

  if (notificationEventSource) {
    try {
      notificationEventSource.close();
    } catch (error) {
      console.error('Notification SSE 연결 해제 오류:', error);
    }
    notificationEventSource = null;
  }
}

export function disconnectAllSSE() {
  disconnectChatSSE();
  disconnectNotificationSSE();

  notificationCallbacks.clear();
  unreadCountCallbacks.clear();
  roomListUpdateCallbacks.clear();
}

/**
 * (하위 호환) 기존 함수명 유지
 * - connectSSE: chat SSE 연결
 * - disconnectSSE: 모든 SSE 해제
 */
export function connectSSE() {
  connectChatSSE();
}

export function disconnectSSE() {
  disconnectAllSSE();
}

export function isChatSSEConnected() {
  return chatEventSource !== null && chatEventSource.readyState === EventSourcePolyfill.OPEN;
}

export function isNotificationSSEConnected() {
  return (
    notificationEventSource !== null &&
    notificationEventSource.readyState === EventSourcePolyfill.OPEN
  );
}

export function getChatSSEState() {
  if (!chatEventSource) return 'CLOSED';
  const states = {
    [EventSourcePolyfill.CONNECTING]: 'CONNECTING',
    [EventSourcePolyfill.OPEN]: 'OPEN',
    [EventSourcePolyfill.CLOSED]: 'CLOSED',
  };
  return states[chatEventSource.readyState] || 'UNKNOWN';
}

export function getNotificationSSEState() {
  if (!notificationEventSource) return 'CLOSED';
  const states = {
    [EventSourcePolyfill.CONNECTING]: 'CONNECTING',
    [EventSourcePolyfill.OPEN]: 'OPEN',
    [EventSourcePolyfill.CLOSED]: 'CLOSED',
  };
  return states[notificationEventSource.readyState] || 'UNKNOWN';
}

// 기본 export
export default {
  connectChatSSE,
  disconnectChatSSE,
  connectNotificationSSE,
  disconnectNotificationSSE,
  disconnectAllSSE,
  connectSSE,
  disconnectSSE,
  onNotification,
  offNotification,
  onUnreadCount,
  offUnreadCount,
  onRoomListUpdate,
  offRoomListUpdate,
  isChatSSEConnected,
  isNotificationSSEConnected,
  getChatSSEState,
  getNotificationSSEState,
};

