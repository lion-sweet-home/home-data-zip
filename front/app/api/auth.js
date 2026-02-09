/**
 * 인증 관련 API
 * 로그인, 로그아웃, 회원가입, 토큰 갱신 등의 인증 기능을 처리합니다.
 */

import { post, get, del } from './api';

/**
 * 로그인 API
 * 
 * @param {string} email - 사용자 이메일
 * @param {string} password - 사용자 비밀번호
 * @returns {Promise<{token: string, user: object}>} 토큰과 사용자 정보
 * 
 * 사용 예시:
 * const response = await login('user@example.com', 'password123');
 * localStorage.setItem('token', response.token);
 */
export async function login(email, password) {
  return post('/auth/login', {
    email,
    password,
  });
}

/**
 * 로그아웃 API
 * 
 * @returns {Promise<void>}
 * 
 * 사용 예시:
 * await logout();
 * localStorage.removeItem('token');
 */
export async function logout() {
  return post('/auth/logout');
}

/**
 * 회원가입 API
 * 
 * @param {object} userData - 회원가입 정보
 * @param {string} userData.email - 이메일
 * @param {string} userData.password - 비밀번호
 * @param {string} userData.name - 이름
 * @returns {Promise<{user: object}>} 생성된 사용자 정보
 * 
 * 사용 예시:
 * const user = await signup({
 *   email: 'newuser@example.com',
 *   password: 'password123',
 *   name: '홍길동'
 * });
 */
export async function signup(userData) {
  return post('/auth/signup', userData);
}

/**
 * 현재 로그인한 사용자 정보 조회
 * 
 * @returns {Promise<{user: object}>} 사용자 정보
 * 
 * 사용 예시:
 * const user = await getCurrentUser();
 */
export async function getCurrentUser() {
  return get('/auth/me');
}

/**
 * 토큰 갱신 API
 * 
 * @param {string} refreshToken - 리프레시 토큰
 * @returns {Promise<{token: string, refreshToken: string}>} 새로운 토큰
 * 
 * 사용 예시:
 * const tokens = await refreshToken(refreshToken);
 * localStorage.setItem('token', tokens.token);
 */
export async function refreshToken(refreshToken) {
  return post('/auth/refresh', { refreshToken });
}

/**
 * 쿠키에서 특정 쿠키 값 가져오기
 */
function getCookie(name) {
  if (typeof document === 'undefined') return null;
  
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) {
    return parts.pop().split(';').shift();
  }
  return null;
}

/**
 * 토큰 재발급 API (reissue)
 * 리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급받습니다.
 * refreshToken은 쿠키로 자동 전송됩니다.
 * 
 * @returns {Promise<{token: string, refreshToken: string}>} 새로운 토큰
 * 
 * 사용 예시:
 * const tokens = await reissue();
 * localStorage.setItem('token', tokens.token);
 */
export async function reissue() {
  // refreshToken은 쿠키로 자동 전송되므로 body에 포함하지 않음
  // credentials: 'include' 옵션이 api.js에서 자동으로 설정됨
  return post('/auth/reissue', {});
}

/**
 * 비밀번호 변경 API
 * 
 * @param {string} currentPassword - 현재 비밀번호
 * @param {string} newPassword - 새 비밀번호
 * @returns {Promise<void>}
 * 
 * 사용 예시:
 * await changePassword('oldPassword', 'newPassword');
 */
export async function changePassword(currentPassword, newPassword) {
  return post('/auth/change-password', {
    currentPassword,
    newPassword,
  });
}

// 기본 export
export default {
  login,
  logout,
  signup,
  getCurrentUser,
  refreshToken,
  reissue,
  changePassword,
};
