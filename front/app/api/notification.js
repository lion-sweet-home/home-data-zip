/**
 * 알림 관련 API
 * 알림 조회, 읽음 처리, 삭제 기능을 제공합니다.
 */

import { get, put, del } from './api';
import { EventSourcePolyfill } from 'event-source-polyfill';

/**
 * SSE 연결 (알림 수신)
 * Server-Sent Events를 통해 실시간 알림을 수신합니다.
 * EventSourcePolyfill을 사용하여 Authorization 헤더를 설정할 수 있습니다.
 * 
 * @returns {EventSourcePolyfill} EventSourcePolyfill 객체
 * 
 * 사용 예시:
 * const eventSource = subscribeNotifications();
 * eventSource.onmessage = (event) => {
 *   console.log('알림:', JSON.parse(event.data));
 * };
 */
export function subscribeNotifications() {
  const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080/api';
  
  // localStorage에서 accessToken 가져오기
  const accessToken = typeof window !== 'undefined' 
    ? localStorage.getItem('accessToken') 
    : null;

  if (!accessToken) {
    throw new Error('Access token이 없습니다. 로그인이 필요합니다.');
  }

  // EventSourcePolyfill을 사용하여 Authorization 헤더 설정
  return new EventSourcePolyfill(
    `${API_BASE_URL}/sse/notifications`,
    {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
      withCredentials: true, // 쿠키(refreshToken)도 함께 전송
    }
  );
}

/**
 * 전체 알림 목록 조회
 * 
 * @returns {Promise<Array>} 알림 목록
 * 
 * 사용 예시:
 * const notifications = await getAllNotifications();
 */
export async function getAllNotifications() {
  return get('/users/notifications');
}

/**
 * 읽음 알림 목록 조회
 * 
 * @returns {Promise<Array>} 읽음 알림 목록
 * 
 * 사용 예시:
 * const readNotifications = await getReadNotifications();
 */
export async function getReadNotifications() {
  return get('/users/notifications/read');
}

/**
 * 미읽음 알림 목록 조회
 * 
 * @returns {Promise<Array>} 미읽음 알림 목록
 * 
 * 사용 예시:
 * const unreadNotifications = await getUnreadNotifications();
 */
export async function getUnreadNotifications() {
  return get('/users/notifications/unread');
}

/**
 * 미읽음 알림 개수 조회
 * 
 * @returns {Promise<{count: number}>} 미읽음 알림 개수
 * 
 * 사용 예시:
 * const { count } = await getUnreadCount();
 */
export async function getUnreadCount() {
  return get('/users/notifications/unread-count');
}

/**
 * 알림 읽음 처리
 * 
 * @param {number} userNotificationId - 사용자 알림 ID
 * @returns {Promise<void>}
 * 
 * 사용 예시:
 * await markAsRead(123);
 */
export async function markAsRead(userNotificationId) {
  return put(`/users/notifications/${userNotificationId}/read`);
}

/**
 * 전체 알림 읽음 처리
 * 
 * @returns {Promise<void>}
 * 
 * 사용 예시:
 * await markAllAsRead();
 */
export async function markAllAsRead() {
  return put('/users/notifications/read-all');
}

/**
 * 알림 삭제
 * 
 * @param {number} userNotificationId - 사용자 알림 ID
 * @returns {Promise<void>}
 * 
 * 사용 예시:
 * await deleteNotification(123);
 */
export async function deleteNotification(userNotificationId) {
  return del(`/users/notifications/${userNotificationId}`);
}

/**
 * 읽은 알림 전체 삭제
 * 
 * @returns {Promise<void>}
 * 
 * 사용 예시:
 * await deleteAllReadNotifications();
 */
export async function deleteAllReadNotifications() {
  return del('/users/notifications/read-all');
}

// 기본 export
export default {
  subscribeNotifications,
  getAllNotifications,
  getReadNotifications,
  getUnreadNotifications,
  getUnreadCount,
  markAsRead,
  markAllAsRead,
  deleteNotification,
  deleteAllReadNotifications,
};








