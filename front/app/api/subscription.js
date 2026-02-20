/**
 * 구독 관련 API
 * 구독 시작, 자동결제 관리, 구독 정보 조회, 휴대폰 인증, 빌링키 발급 기능을 제공합니다.
 */

import { get, post } from './api';

const SUBSCRIPTION_ENDPOINTS = {
  billingIssue: '/subscriptions/billing/issue',
  // 아래 2개는 Toss 리다이렉트로 백엔드가 직접 받는 콜백 성격이라
  // 프론트에서 직접 호출할 일은 거의 없음(테스트/수동호출용으로만 남김)
  billingSuccess: '/subscriptions/billing/success',
  billingFail: '/subscriptions/billing/fail',

  start: '/subscriptions/start',
  me: '/subscriptions/me',

  phoneAuthSend: '/subscriptions/phone-auth/send',
  phoneAuthVerify: '/subscriptions/phone-auth/verify',

  autoPayCancel: '/subscriptions/auto-pay/cancel',
  autoPayReactivate: '/subscriptions/auto-pay/reactivate',
};

// 응답이 {data: ...} 로 오거나, 그냥 객체로 오거나 섞일 때 대비
function unwrap(res) {
  return res?.data ?? res;
}

/**
 * 빌링키 발급 요청
 * @param {object} [request]
 * @returns {Promise<object>} BillingKeyIssueResponse
 */
export async function issueBillingKey(request) {
  const res = await post(SUBSCRIPTION_ENDPOINTS.billingIssue, request);
  return unwrap(res);
}

/**
 * (대부분 프론트에서 직접 호출 X) 빌링키 등록 성공 콜백 수동 호출용
 */
export async function billingSuccess(customerKey, authKey) {
  const params = new URLSearchParams();
  params.append('customerKey', customerKey);
  params.append('authKey', authKey);

  const res = await get(`${SUBSCRIPTION_ENDPOINTS.billingSuccess}?${params.toString()}`);
  return unwrap(res);
}

/**
 * (대부분 프론트에서 직접 호출 X) 빌링키 등록 실패 콜백 수동 호출용
 */
export async function billingFail(customerKey) {
  const params = new URLSearchParams();
  if (customerKey) params.append('customerKey', customerKey);

  const queryString = params.toString();
  const res = await get(
    `${SUBSCRIPTION_ENDPOINTS.billingFail}${queryString ? `?${queryString}` : ''}`
  );
  return unwrap(res);
}

/**
 * 구독 시작
 * @returns {Promise<void|object>}
 */
export async function startSubscription() {
  const res = await post(SUBSCRIPTION_ENDPOINTS.start);
  return unwrap(res);
}

/**
 * 자동결제 취소
 * @returns {Promise<void|object>}
 */
export async function cancelAutoPay() {
  const res = await post(SUBSCRIPTION_ENDPOINTS.autoPayCancel);
  return unwrap(res);
}

/**
 * 자동결제 재활성화
 * @returns {Promise<void|object>}
 */
export async function reactivateAutoPay() {
  const res = await post(SUBSCRIPTION_ENDPOINTS.autoPayReactivate);
  return unwrap(res);
}

/**
 * 내 구독 정보 조회
 * @returns {Promise<object>} SubscriptionMeResponse
 */
export async function getMySubscription() {
  const res = await get(SUBSCRIPTION_ENDPOINTS.me);
  return unwrap(res);
}

/**
 * 휴대폰 인증 요청
 * @param {string} phoneNumber
 * @returns {Promise<{requestId: string, expiresInSeconds: number}>}
 */
export async function sendPhoneAuth(phoneNumber) {
  const res = await post(SUBSCRIPTION_ENDPOINTS.phoneAuthSend, { phoneNumber });
  return unwrap(res);
}

/**
 * 휴대폰 인증 검증
 * @param {{phoneNumber: string, requestId: string, code: string}} payload
 * @returns {Promise<{verified: boolean, verificationToken: string|null}>}
 */
export async function verifyPhoneAuth(payload) {
  const res = await post(SUBSCRIPTION_ENDPOINTS.phoneAuthVerify, payload);
  return unwrap(res);
}

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