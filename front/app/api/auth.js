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
 * @returns {Promise<{AccessToken: string}>} AccessToken (refreshToken은 쿠키로 설정됨)
 * 
 * 사용 예시:
 * const response = await login('user@example.com', 'password123');
 * localStorage.setItem('accessToken', response.AccessToken);
 */
export async function login(email, password) {
  const response = await post('/auth/login', {
    email,
    password,
  });
  
  // AccessToken 저장 (백엔드 LoginResponse는 accessToken 필드, 호환을 위해 둘 다 확인)
  const token = response.AccessToken ?? response.accessToken;
  if (token) {
    if (typeof window !== 'undefined') {
      localStorage.setItem('accessToken', token);
      // Header 등 전역 UI 즉시 갱신용 이벤트
      window.dispatchEvent(new Event('auth:changed'));
    }
  }
  
  return response;
}

/**
 * 로그아웃 API
 * 인증이 필요합니다 (Authorization 헤더에 AccessToken 포함)
 * 
 * @returns {Promise<void>}
 * 
 * 사용 예시:
 * await logout();
 * localStorage.removeItem('accessToken');
 */
export async function logout() {
  try {
    await post('/auth/logout');
  } finally {
    // 로그아웃 성공/실패와 관계없이 로컬 스토리지 정리
    if (typeof window !== 'undefined') {
      localStorage.removeItem('accessToken');
      // Header 등 전역 UI 즉시 갱신용 이벤트
      window.dispatchEvent(new Event('auth:changed'));
    }
  }
}

/**
 * 닉네임 중복 확인 API
 * 
 * @param {string} nickname - 확인할 닉네임
 * @returns {Promise<boolean>} true면 중복(사용 불가), false면 사용 가능
 * 
 * 사용 예시:
 * const isDuplicate = await checkNickname('홍길동');
 * if (!isDuplicate) {
 *   console.log('사용 가능한 닉네임입니다.');
 * }
 */
export async function checkNickname(nickname) {
  return post('/users/check-nickname', { nickname });
}

/**
 * 이메일 중복 확인 API
 * 
 * @param {string} email - 확인할 이메일
 * @returns {Promise<boolean>} true면 중복(사용 불가), false면 사용 가능
 * 
 * 사용 예시:
 * const isDuplicate = await checkEmail('user@example.com');
 * if (!isDuplicate) {
 *   console.log('사용 가능한 이메일입니다.');
 * }
 */
export async function checkEmail(email) {
  return post('/users/check-email', { email });
}

/**
 * 이메일 인증 코드 발송 API
 * 
 * @param {string} email - 인증 코드를 받을 이메일
 * @returns {Promise<void>}
 * 
 * 사용 예시:
 * await sendEmailVerification('user@example.com');
 */
export async function sendEmailVerification(email) {
  return post('/users/email-verification', { email });
}

/**
 * 이메일 인증 코드 확인 API
 * 
 * @param {string} email - 이메일
 * @param {string} authCode - 인증 코드
 * @returns {Promise<void>}
 * 
 * 사용 예시:
 * await verifyEmailCode('user@example.com', '123456');
 */
export async function verifyEmailCode(email, authCode) {
  return post('/users/verify-email-code', { email, authCode });
}

/**
 * 회원가입 API (register)
 * 
 * @param {object} userData - 회원가입 정보
 * @param {string} userData.nickname - 닉네임 (2~30자)
 * @param {string} userData.email - 이메일
 * @param {string} userData.password - 비밀번호 (8~50자, 영문+특수문자 포함)
 * @param {string} userData.authCode - 이메일 인증 코드
 * @returns {Promise<void>}
 * 
 * 사용 예시:
 * await register({
 *   nickname: '홍길동',
 *   email: 'newuser@example.com',
 *   password: 'password123!',
 *   authCode: '123456'
 * });
 */
export async function register(userData) {
  return post('/users/register', userData);
}

/**
 * 현재 로그인한 사용자 정보 조회
 * 
 * @returns {Promise<{user: object}>} 사용자 정보
 * 
 * 사용 예시:
 * const user = await getCurrentUser();
 */
// export async function getCurrentUser() {
//   return get('/auth/me');
// }

/**
 * 토큰 갱신 API (deprecated - reissue 사용 권장)
 * refreshToken은 쿠키로 자동 전송됩니다.
 * 
 * @returns {Promise<{AccessToken: string}>} 새로운 AccessToken
 * 
 * 사용 예시:
 * const response = await refreshToken();
 * localStorage.setItem('accessToken', response.AccessToken);
 */
export async function refreshToken() {
  const response = await post('/auth/refresh', {});
  const token = response.AccessToken ?? response.accessToken;
  if (token && typeof window !== 'undefined') {
    localStorage.setItem('accessToken', token);
    window.dispatchEvent(new Event('auth:changed'));
  }
  return response;
}

/**
 * 쿠키에서 특정 쿠키 값 가져오기
 */
// function getCookie(name) {
//   if (typeof document === 'undefined') return null;
  
//   const value = `; ${document.cookie}`;
//   const parts = value.split(`; ${name}=`);
//   if (parts.length === 2) {
//     return parts.pop().split(';').shift();
//   }
//   return null;
// }

/**
 * 토큰 재발급 API (reissue)
 * 리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급받습니다.
 * refreshToken은 쿠키로 자동 전송됩니다.
 * 
 * @returns {Promise<{AccessToken: string}>} 새로운 AccessToken
 * 
 * 사용 예시:
 * const response = await reissue();
 * localStorage.setItem('accessToken', response.AccessToken);
 */
export async function reissue() {
  // refreshToken은 쿠키로 자동 전송되므로 body에 포함하지 않음
  const response = await post('/auth/refresh', {});
  const token = response.AccessToken ?? response.accessToken;
  if (token && typeof window !== 'undefined') {
    localStorage.setItem('accessToken', token);
    window.dispatchEvent(new Event('auth:changed'));
  }
  return response;
}


// 기본 export
export default {
  login,
  logout,
  checkNickname,
  checkEmail,
  sendEmailVerification,
  verifyEmailCode,
  register,
  refreshToken,
  reissue,
};
