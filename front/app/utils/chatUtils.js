/**
 * 채팅 관련 유틸리티 함수
 */

/**
 * 채팅 메시지 시간 포맷팅
 * @param {string|Date} dateTime - 날짜/시간 문자열 또는 Date 객체
 * @returns {string} 포맷된 시간 문자열
 */
export function formatChatTime(dateTime) {
  if (!dateTime) return '';

  const date = new Date(dateTime);
  if (Number.isNaN(date.getTime())) return '';

  const now = new Date();
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const messageDate = new Date(date.getFullYear(), date.getMonth(), date.getDate());
  const diffDays = Math.floor((today - messageDate) / (1000 * 60 * 60 * 24));

  const hours = date.getHours();
  const minutes = date.getMinutes();
  const ampm = hours >= 12 ? '오후' : '오전';
  const displayHours = hours % 12 || 12;
  const timeStr = `${ampm} ${displayHours}:${String(minutes).padStart(2, '0')}`;

  if (diffDays === 0) {
    // 오늘
    return timeStr;
  } else if (diffDays === 1) {
    // 어제
    return `어제 ${timeStr}`;
  } else if (diffDays < 7) {
    // 일주일 이내
    const dayNames = ['일', '월', '화', '수', '목', '금', '토'];
    return `${dayNames[date.getDay()]}요일 ${timeStr}`;
  } else {
    // 그 이전
    const year = date.getFullYear();
    const month = date.getMonth() + 1;
    const day = date.getDate();
    return `${year}.${String(month).padStart(2, '0')}.${String(day).padStart(2, '0')} ${timeStr}`;
  }
}

/**
 * JWT 토큰에서 사용자 닉네임 가져오기
 * @returns {string|null} 사용자 닉네임 또는 null
 */
export function getCurrentUserNickname() {
  if (typeof window === 'undefined') return null;
  
  const token = localStorage.getItem('accessToken');
  if (!token) return null;

  try {
    const parts = token.split('.');
    if (parts.length !== 3) return null;
    const payload = JSON.parse(atob(parts[1].replace(/-/g, '+').replace(/_/g, '/')));
    return payload.nickname || null;
  } catch (error) {
    console.error('토큰 디코딩 오류:', error);
    return null;
  }
}

/**
 * JWT 토큰에서 사용자 이메일 가져오기
 * @returns {string|null} 사용자 이메일 또는 null
 */
export function getCurrentUserEmail() {
  if (typeof window === 'undefined') return null;
  
  const token = localStorage.getItem('accessToken');
  if (!token) return null;

  try {
    const parts = token.split('.');
    if (parts.length !== 3) return null;
    const payload = JSON.parse(atob(parts[1].replace(/-/g, '+').replace(/_/g, '/')));
    return payload.email || null;
  } catch (error) {
    console.error('토큰 디코딩 오류:', error);
    return null;
  }
}

