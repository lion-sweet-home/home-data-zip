/**
 * 구독 관련 API
 * 구독 시작, 자동결제 관리, 구독 정보 조회 기능을 제공합니다.
 */

import { get, post } from './api';

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
  return post('/subscriptions/billing/issue', request);
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
  
  return get(`/subscriptions/billing/success?${params.toString()}`);
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
  return get(`/subscriptions/billing/fail${queryString ? `?${queryString}` : ''}`);
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
  return post('/subscriptions/start');
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
  return post('/subscriptions/auto-pay/cancel');
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
  return post('/subscriptions/auto-pay/reactivate');
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
  return get('/subscriptions/me');
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
};
