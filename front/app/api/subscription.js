/**
 * 구독 관련 API
 * 구독 시작, 자동결제 관리, 구독 정보 조회 기능을 제공합니다.
 */

import { get, post } from './api';

const SUBSCRIPTION_ENDPOINTS = {
  billingIssue: '/subscriptions/billing/issue',
  billingSuccess: '/subscriptions/billing/success',
  billingFail: '/subscriptions/billing/fail',
  start: '/subscriptions/start',
  me: '/subscriptions/me',
  phoneAuthSend: '/subscriptions/phone-auth/send',
  phoneAuthVerify: '/subscriptions/phone-auth/verify',
  autoPayCancel: '/subscriptions/auto-pay/cancel',
  autoPayReactivate: '/subscriptions/auto-pay/reactivate',
};

/**
 * 빌링키 발급 요청
 *
 * @param {object} [request] - 빌링키 발급 요청 정보 (선택사항)
 * @returns {Promise<{billingKey: string, customerKey: string}>} 빌링키 정보
 *
 * 사용 예시:
 * const result = await issueBillingKey();
 */
export async function issueBillingKey(request) {
  return post(SUBSCRIPTION_ENDPOINTS.billingIssue, request);
}

/**
 * 빌링키 등록 성공 콜백
 *
 * @param {string} customerKey - 고객 키
 * @param {string} authKey - 인증 키
 * @returns {Promise<void>}
 *
 * 사용 예시:
 * await billingSuccess('CUSTOMER_123', 'AUTH_KEY');
 */
export async function billingSuccess(customerKey, authKey) {
  const params = new URLSearchParams();
  params.append('customerKey', customerKey);
  params.append('authKey', authKey);

  return get(`${SUBSCRIPTION_ENDPOINTS.billingSuccess}?${params.toString()}`);
}

/**
 * 빌링키 등록 실패 콜백
 *
 * @param {string} [customerKey] - 고객 키 (선택사항)
 * @returns {Promise<void>}
 *
 * 사용 예시:
 * await billingFail('CUSTOMER_123');
 */
export async function billingFail(customerKey) {
  const params = new URLSearchParams();
  if (customerKey) params.append('customerKey', customerKey);

  const queryString = params.toString();
  return get(
    `${SUBSCRIPTION_ENDPOINTS.billingFail}${queryString ? `?${queryString}` : ''}`
  );
}

/**
 * 구독 시작
 *
 * @returns {Promise<void>}
 *
 * 사용 예시:
 * await startSubscription();
 */
export async function startSubscription() {
  return post(SUBSCRIPTION_ENDPOINTS.start);
}

/**
 * 자동결제 취소
 *
 * @returns {Promise<void>}
 *
 * 사용 예시:
 * await cancelAutoPay();
 */
export async function cancelAutoPay() {
  return post(SUBSCRIPTION_ENDPOINTS.autoPayCancel);
}

/**
 * 자동결제 재활성화
 *
 * @returns {Promise<void>}
 *
 * 사용 예시:
 * await reactivateAutoPay();
 */
export async function reactivateAutoPay() {
  return post(SUBSCRIPTION_ENDPOINTS.autoPayReactivate);
}

/**
 * 내 구독 정보 조회
 *
 * @returns {Promise<object>} 구독 정보
 *
 * 사용 예시:
 * const subscription = await getMySubscription();
 */
export async function getMySubscription() {
  return get(SUBSCRIPTION_ENDPOINTS.me);
}

/**
 * 휴대폰 인증 요청
 *
 * @param {string} phoneNumber - 휴대폰 번호
 * @returns {Promise<{requestId: string, expiresInSeconds: number}>}
 */
export async function sendPhoneAuth(phoneNumber) {
  return post(SUBSCRIPTION_ENDPOINTS.phoneAuthSend, { phoneNumber });
}

/**
 * 휴대폰 인증 검증
 *
 * @param {object} payload - 인증 검증 정보
 * @param {string} payload.phoneNumber - 휴대폰 번호
 * @param {string} payload.requestId - 요청 ID
 * @param {string} payload.code - 인증 코드
 * @returns {Promise<{verified: boolean, verificationToken: string | null}>}
 *
 * 백엔드 응답이 { verified, verificationToken } 이 형태라서 success가 아니라 verified임
 */
export async function verifyPhoneAuth(payload) {
  return post(SUBSCRIPTION_ENDPOINTS.phoneAuthVerify, payload);
}

// 기본 export
export default {
  issueBillingKey,
  billingSuccess,
  billingFail,
  startSubscription,
  cancelAutoPay,
  reactivateAutoPay,
  getMySubscription,
  sendPhoneAuth,
  verifyPhoneAuth,
};