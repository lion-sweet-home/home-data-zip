/**
 * Base API Request 유틸리티
 * 모든 API 요청의 기본 설정과 공통 로직을 처리합니다.
 */

// API Base URL 설정 (환경 변수 또는 기본값)
const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080/api';

/**
 * 기본 요청 옵션
 */
const defaultOptions = {
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000, // 30초
};

/**
 * 타임아웃을 포함한 fetch 래퍼 (?)
 */
function fetchWithTimeout(url, options, timeout = 30000) {
  return Promise.race([
    fetch(url, options),
    new Promise((_, reject) =>
      setTimeout(() => reject(new Error('Request timeout')), timeout)
    ),
  ]);
}

/**
 * JWT 토큰 디코딩 (payload 추출)
 */
function decodeJWT(token) {
  if (!token) return null;
  try {
    const parts = token.split('.');
    if (parts.length !== 3) return null;
    const payload = parts[1];
    const decoded = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')));
    return decoded;
  } catch (error) {
    console.error('JWT decode error:', error);
    return null;
  }
}

/**
 * Access Token의 만료 시간 확인
 * @returns {number|null} 만료 시간 (Unix timestamp, 초 단위) 또는 null
 */
function getTokenExpiration(token) {
  const decoded = decodeJWT(token);
  if (!decoded || !decoded.exp) return null;
  return decoded.exp;
}

/**
 * Access Token이 만료되었는지 확인
 * @param {number} bufferSeconds - 만료 전 버퍼 시간 (초, 기본값: 60초)
 * @returns {boolean} 만료되었거나 곧 만료될 예정이면 true
 */
function isTokenExpired(bufferSeconds = 60) {
  const token = getAuthToken();
  if (!token) return true;
  
  const exp = getTokenExpiration(token);
  if (!exp) return true;
  
  // 현재 시간 + 버퍼 시간과 만료 시간 비교
  const now = Math.floor(Date.now() / 1000);
  return exp <= (now + bufferSeconds);
}

/**
 * 로컬 스토리지에서 인증 토큰 가져오기
 */
function getAuthToken() {
  if (typeof window === 'undefined') return null;
  return localStorage.getItem('accessToken');
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
 * 쿠키에서 리프레시 토큰 가져오기
 */
function getRefreshToken() {
  if (typeof window === 'undefined') return null;
  return getCookie('refreshToken');
}

/**
 * 토큰 저장
 */
function setTokens(accessToken, refreshToken) {
  if (typeof window === 'undefined') return;
  if (accessToken) {
    localStorage.setItem('accessToken', accessToken);
  }
  if (refreshToken) {
    localStorage.setItem('refreshToken', refreshToken);
  }
}

/**
 * 쿠키 삭제
 */
function deleteCookie(name) {
  if (typeof document === 'undefined') return;
  document.cookie = `${name}=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;`;
}

/**
 * 토큰 제거
 */
function clearTokens() {
  if (typeof window === 'undefined') return;
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
  // 쿠키에서도 refreshToken 제거
  deleteCookie('refreshToken');
}

/**
 * Reissue API 호출 (토큰 재발급)
 * 순환 참조 방지를 위해 직접 fetch 사용
 * refreshToken은 쿠키로 자동 전송되므로 body에 포함하지 않음
 */
async function reissueToken() {
  if (typeof window === 'undefined') return null;
  
  const refreshToken = getRefreshToken();
  if (!refreshToken) {
    return null;
  }

  try {
    const url = `${API_BASE_URL}/auth/refresh`;
    // 쿠키는 자동으로 전송되므로 credentials: 'include' 옵션 사용
    const response = await fetchWithTimeout(
      url,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include', // 쿠키 자동 전송
      },
      defaultOptions.timeout
    );

    if (response.ok) {
      const data = await response.json();
      // 백엔드 응답은 AccessToken 필드 사용 (대문자 A)
      const newAccessToken = data.AccessToken || data.token || data.accessToken;
      
      if (newAccessToken) {
        setTokens(newAccessToken, null); // refreshToken은 쿠키로 관리되므로 null
        return newAccessToken;
      }
    }
    
    return null;
  } catch (error) {
    console.error('Token reissue failed:', error);
    return null;
  }
}

/**
 * Access Token 자동 갱신 (필요한 경우)
 * @returns {Promise<string|null>} 유효한 Access Token 또는 null
 */
async function ensureValidToken() {
  // 토큰이 없으면 null 반환
  const token = getAuthToken();
  if (!token) return null;

  // 토큰이 만료되지 않았으면 그대로 반환
  if (!isTokenExpired()) {
    return token;
  }

  // 토큰이 만료되었거나 곧 만료될 예정이면 재발급 시도
  console.log('Access Token이 만료되었거나 곧 만료됩니다. 재발급 시도...');
  const newToken = await reissueToken();
  
  if (newToken) {
    console.log('Access Token 재발급 성공');
    return newToken;
  } else {
    console.log('Access Token 재발급 실패');
    // 재발급 실패 시 토큰 제거
    clearTokens();
    return null;
  }
}

/**
 * 요청 헤더 생성
 */
async function createHeaders(customHeaders = {}) {
  const headers = {
    ...defaultOptions.headers,
    ...customHeaders,
  };

  // 유효한 인증 토큰 가져오기 (필요시 자동 갱신)
  const token = await ensureValidToken();
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  return headers;
}

/**
 * 응답 처리 및 에러 핸들링
 */
async function handleResponse(response, skipReissue = false) {
  // 응답이 없거나 에러인 경우
  if (!response) {
    throw new Error('Network error: No response received');
  }

  // HTTP 상태 코드 확인
  if (!response.ok) {
    let errorMessage = `HTTP error! status: ${response.status}`;
    let errorData = null;
    
    try {
      errorData = await response.json();
      // 백엔드 ErrorResponse 형식 (code, message)
      if (errorData.message) {
        errorMessage = errorData.message;
      } else if (errorData.error) {
        errorMessage = errorData.error;
      }
      // Spring Boot validation 에러 형식 (errors 배열)
      if (errorData.errors && Array.isArray(errorData.errors) && errorData.errors.length > 0) {
        // 첫 번째 validation 에러 메시지 사용
        errorMessage = errorData.errors[0].message || errorMessage;
      }
    } catch {
      // JSON 파싱 실패 시 텍스트로 시도
      try {
        const errorText = await response.text();
        errorMessage = errorText || errorMessage;
      } catch {
        // 텍스트도 실패하면 기본 메시지 사용
      }
    }

    const error = new Error(errorMessage);
    error.status = response.status;
    error.statusText = response.statusText;
    error.data = errorData; // 원본 에러 데이터 저장
    error.skipReissue = skipReissue; // reissue 요청 자체는 재시도하지 않음
    throw error;
  }

  // Content-Type 확인하여 적절한 파싱
  const contentType = response.headers.get('content-type');
  if (contentType && contentType.includes('application/json')) {
    return await response.json();
  }
  
  return await response.text();
}

/**
 * Base Request 함수
 * 
 * @param {string} endpoint - API 엔드포인트 (예: '/auth/login')
 * @param {object} options - fetch 옵션
 * @param {string} options.method - HTTP 메서드 (GET, POST, PUT, DELETE 등)
 * @param {object} options.body - 요청 본문
 * @param {object} options.headers - 추가 헤더
 * @param {number} options.timeout - 타임아웃 (밀리초)
 * @param {boolean} options.skipReissue - reissue 재시도 건너뛰기 (내부용)
 * @returns {Promise} API 응답
 */
export async function request(endpoint, options = {}) {
  const {
    method = 'GET',
    body,
    headers: customHeaders = {},
    timeout = defaultOptions.timeout,
    skipReissue = false,
    ...restOptions
  } = options;

  // URL 구성
  const url = endpoint.startsWith('http') 
    ? endpoint 
    : `${API_BASE_URL}${endpoint.startsWith('/') ? endpoint : `/${endpoint}`}`;

  // 요청 옵션 구성
  const requestOptions = {
    method,
    headers: await createHeaders(customHeaders),
    credentials: 'include', // 쿠키 자동 전송
    ...restOptions,
  };

  // body가 있으면 JSON으로 변환
  if (body) {
    if (body instanceof FormData) {
      // FormData인 경우 Content-Type을 설정하지 않음 (브라우저가 자동 설정)
      delete requestOptions.headers['Content-Type'];
      requestOptions.body = body;
    } else {
      requestOptions.body = JSON.stringify(body);
    }
  }

  try {
    const response = await fetchWithTimeout(url, requestOptions, timeout);
    return await handleResponse(response, skipReissue);
  } catch (error) {
    // 401 에러이고 reissue를 건너뛰지 않는 경우 토큰 재발급 시도
    if (error.status === 401 && !skipReissue && !endpoint.includes('/auth/refresh')) {
      const newToken = await reissueToken();
      
      if (newToken) {
        // 토큰 재발급 성공 시 원래 요청 재시도
        try {
          // 새로운 토큰으로 헤더 재생성
          const retryHeaders = await createHeaders(customHeaders);
          const retryOptions = {
            ...requestOptions,
            headers: retryHeaders,
          };
          
          const retryResponse = await fetchWithTimeout(url, retryOptions, timeout);
          return await handleResponse(retryResponse, true); // 재시도는 skipReissue로 표시
        } catch (retryError) {
          // 재시도도 실패하면 에러 throw
          throw retryError;
        }
      } else {
        // 토큰 재발급 실패 시 토큰 제거 및 로그인 페이지로 리다이렉트
        clearTokens();
        if (typeof window !== 'undefined') {
          window.location.href = '/auth/login';
        }
        throw new Error('인증이 만료되었습니다. 다시 로그인해주세요.');
      }
    }

    // 네트워크 에러 또는 타임아웃 에러 처리
    if (error.message === 'Request timeout') {
      throw new Error('Request timeout: 서버 응답이 너무 오래 걸립니다.');
    }
    
    if (error.name === 'TypeError' && error.message.includes('fetch')) {
      throw new Error('Network error: 서버에 연결할 수 없습니다.');
    }

    throw error;
  }
}

/**
 * GET 요청
 */
export async function get(endpoint, options = {}) {
  return request(endpoint, { ...options, method: 'GET' });
}

/**
 * POST 요청
 */
export async function post(endpoint, body, options = {}) {
  return request(endpoint, { ...options, method: 'POST', body });
}

/**
 * PUT 요청
 */
export async function put(endpoint, body, options = {}) {
  return request(endpoint, { ...options, method: 'PUT', body });
}

/**
 * PATCH 요청
 */
export async function patch(endpoint, body, options = {}) {
  return request(endpoint, { ...options, method: 'PATCH', body });
}

/**
 * DELETE 요청
 */
export async function del(endpoint, options = {}) {
  return request(endpoint, { ...options, method: 'DELETE' });
}

/**
 * 파일 업로드 요청
 */
export async function upload(endpoint, formData, options = {}) {
  return request(endpoint, {
    ...options,
    method: 'POST',
    body: formData,
  });
}

// 기본 export
export default {
  request,
  get,
  post,
  put,
  patch,
  delete: del,
  upload,
};
