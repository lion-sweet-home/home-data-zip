/**
 * 채팅 관련 API
 * 채팅방 목록 조회, 채팅방 생성/입장, 메시지 조회 등의 기능을 제공합니다.
 */

import { get, post, patch, getApiBaseUrl } from './api';
import { EventSourcePolyfill } from 'event-source-polyfill';

/**
 * SSE 연결 (채팅 이벤트 수신)
 * - unreadCount
 * - roomListUpdate
 *
 * @returns {EventSourcePolyfill}
 */
export function subscribeChatEvents() {
  const accessToken = typeof window !== 'undefined' ? localStorage.getItem('accessToken') : null;
  if (!accessToken) {
    throw new Error('Access token이 없습니다. 로그인이 필요합니다.');
  }

  return new EventSourcePolyfill(`${getApiBaseUrl()}/sse/chat`, {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
    withCredentials: true,
  });
}

/**
 * 채팅방 목록 조회
 * 
 * @returns {Promise<Array<ChatRoomListResponse>>} 채팅방 목록
 * 
 * 사용 예시:
 * const rooms = await getChatRooms();
 */
export async function getChatRooms() {
  return get('/chat/rooms');
}

/**
 * 채팅방 생성 또는 입장
 * 
 * @param {number} listingId - 매물 ID
 * @returns {Promise<{roomId: number}>} 생성된 또는 입장한 채팅방 ID
 * 
 * 사용 예시:
 * const { roomId } = await createOrJoinRoom(123);
 */
export async function createOrJoinRoom(listingId) {
  return post('/chat/room', { listingId });
}

/**
 * 채팅방 상세 정보 및 메시지 조회
 * 
 * @param {number} roomId - 채팅방 ID
 * @param {Object} options - 페이지네이션 옵션
 * @param {number} options.page - 페이지 번호 (0부터 시작)
 * @param {number} options.size - 페이지 크기 (기본값: 20)
 * @param {string} options.sort - 정렬 필드 (기본값: 'createdAt')
 * @param {string} options.direction - 정렬 방향 (기본값: 'DESC')
 * @returns {Promise<ChatRoomDetailResponse>} 채팅방 상세 정보 및 메시지 목록
 * 
 * 사용 예시:
 * const detail = await getChatRoomDetail(123, { page: 0, size: 20 });
 */
export async function getChatRoomDetail(roomId, options = {}) {
  const { page = 0, size = 20, sort = 'createdAt', direction = 'DESC' } = options;
  
  const params = new URLSearchParams({
    page: page.toString(),
    size: size.toString(),
    sort: `${sort},${direction}`,
  });

  return get(`/chat/room/${roomId}?${params.toString()}`);
}

/**
 * 채팅방 나가기
 * 
 * @param {number} roomId - 채팅방 ID
 * @returns {Promise<void>}
 * 
 * 사용 예시:
 * await exitChatRoom(123);
 */
export async function exitChatRoom(roomId) {
  return patch(`/chat/room/${roomId}/exit`);
}

// 기본 export
export default {
  getChatRooms,
  createOrJoinRoom,
  getChatRoomDetail,
  exitChatRoom,
  subscribeChatEvents,
};

