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
// Chat SSE 재연결 시 호출할 콜백 (상태 동기화용)
const chatReconnectedCallbacks = new Set();
// Notification SSE 재연결 시 호출할 콜백 (상태 동기화용)
const notificationReconnectedCallbacks = new Set();
// 채팅방 상세 갱신 콜백 함수들
const roomDetailUpdateCallbacks = new Set();

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
 * Chat SSE 재연결 시 콜백 등록 (연결/재연결 성공 시 1회 호출, 상태 동기화용)
 */
export function onChatReconnected(callback) {
  if (typeof callback === 'function') {
    chatReconnectedCallbacks.add(callback);
  }
}

/**
 * Chat SSE 재연결 콜백 제거
 */
export function offChatReconnected(callback) {
  chatReconnectedCallbacks.delete(callback);
}

/**
 * Notification SSE 재연결 시 콜백 등록 (연결/재연결 성공 시 1회 호출, 상태 동기화용)
 */
export function onNotificationReconnected(callback) {
  if (typeof callback === 'function') {
    notificationReconnectedCallbacks.add(callback);
  }
}

/**
 * Notification SSE 재연결 콜백 제거
 */
export function offNotificationReconnected(callback) {
  notificationReconnectedCallbacks.delete(callback);
}

/**
 * 채팅방 상세 갱신 콜백 등록
 * @param {Function} callback - 채팅방 상세 갱신 신호 수신 시 호출될 콜백 함수
 */
export function onRoomDetailUpdate(callback) {
  if (typeof callback === 'function') {
    roomDetailUpdateCallbacks.add(callback);
  }
}

/**
 * 채팅방 상세 갱신 콜백 제거
 * @param {Function} callback - 제거할 콜백 함수
 */
export function offRoomDetailUpdate(callback) {
  roomDetailUpdateCallbacks.delete(callback);
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
  if (chatReconnectAttempts >= MAX_RECONNECT_ATTEMPTS) return;

  chatReconnectAttempts++;
  chatReconnectTimer = setTimeout(() => {
    connectChatSSE();
  }, RECONNECT_DELAY);
}

/**
 * SSE 연결 재시도 (notification)
 */
function attemptNotificationReconnect() {
  if (notificationReconnectAttempts >= MAX_RECONNECT_ATTEMPTS) return;

  notificationReconnectAttempts++;
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
      chatReconnectedCallbacks.forEach((cb) => {
        try {
          cb();
        } catch (e) {
          // no-op
        }
      });
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

    chatEventSource.addEventListener('roomDetailUpdate', () => {
      roomDetailUpdateCallbacks.forEach((callback) => {
        try {
          callback();
        } catch (error) {
          console.error('RoomDetailUpdate callback error:', error);
        }
      });
    });

    chatEventSource.onerror = (error) => {
      // 연결이 끊어진 경우 재연결 시도
      if (chatEventSource && chatEventSource.readyState === EventSourcePolyfill.CLOSED) {
        if (!chatReconnectTimer) attemptChatReconnect();
      }
      console.error('Chat SSE 연결 오류:', error);
    chatEventSource.onerror = () => {
      if (chatEventSource) {
        try {
          chatEventSource.close();
        } catch (_) {}
        chatEventSource = null;
      }
      if (!chatReconnectTimer) attemptChatReconnect();
    };
  } catch (error) {
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
      notificationReconnectedCallbacks.forEach((cb) => {
        try {
          cb();
        } catch (e) {
          // no-op
        }
      });
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

    notificationEventSource.onerror = () => {
      if (notificationEventSource) {
        try {
          notificationEventSource.close();
        } catch (_) {}
        notificationEventSource = null;
      }
      if (!notificationReconnectTimer) attemptNotificationReconnect();
    };
  } catch (error) {
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
  chatReconnectedCallbacks.clear();
  notificationReconnectedCallbacks.clear();
  roomDetailUpdateCallbacks.clear();
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
  onChatReconnected,
  offChatReconnected,
  onNotificationReconnected,
  offNotificationReconnected,
  onRoomDetailUpdate,
  offRoomDetailUpdate,
  isChatSSEConnected,
  isNotificationSSEConnected,
  getChatSSEState,
  getNotificationSSEState,
};

