/**
 * 구독 관련 API
 * - 빌링키 발급/삭제(해지)
 * - 구독 시작
 * - 자동결제 취소/재활성화
 * - 내 구독 정보 조회
 * - 휴대폰 인증
 */

import { get, post } from "./api";

const SUBSCRIPTION_ENDPOINTS = {
  // billing
  billingIssue: "/subscriptions/billing/issue",
  billingRevoke: "/subscriptions/billing/revoke",

  // Toss redirect callback (프론트에서 직접 호출 거의 X)
  billingSuccess: "/subscriptions/billing/success",
  billingFail: "/subscriptions/billing/fail",

  // subscription
  start: "/subscriptions/start",
  me: "/subscriptions/me",

  // phone auth
  phoneAuthSend: "/subscriptions/phone-auth/send",
  phoneAuthVerify: "/subscriptions/phone-auth/verify",

  // auto-pay
  autoPayCancel: "/subscriptions/auto-pay/cancel",
  autoPayReactivate: "/subscriptions/auto-pay/reactivate",
};

// 응답이 { data: ... } 이거나, 그냥 객체거나 섞여서 오는 경우 대비
function unwrap(res) {
  return res?.data ?? res;
}

/** 빌링키 발급(카드등록 시작용 정보 받기) */
export async function issueBillingKey(request) {
  const res = await post(SUBSCRIPTION_ENDPOINTS.billingIssue, request);
  return unwrap(res);
}

/**
 * (거의 사용 X) 빌링키 등록 성공 콜백 수동 호출용
 * ⚠️ 보통은 Toss가 백엔드로 redirect 치고, 백엔드가 프론트로 다시 redirect 함
 */
export async function billingSuccess(customerKey, authKey) {
  const params = new URLSearchParams();
  params.append("customerKey", customerKey);
  params.append("authKey", authKey);

  const res = await get(
    `${SUBSCRIPTION_ENDPOINTS.billingSuccess}?${params.toString()}`
  );
  return unwrap(res);
}

/** (거의 사용 X) 빌링키 등록 실패 콜백 수동 호출용 */
export async function billingFail(customerKey) {
  const params = new URLSearchParams();
  if (customerKey) params.append("customerKey", customerKey);

  const queryString = params.toString();
  const res = await get(
    `${SUBSCRIPTION_ENDPOINTS.billingFail}${
      queryString ? `?${queryString}` : ""
    }`
  );
  return unwrap(res);
}

/** ✅ 카드 삭제(빌링키 해지) */
export async function revokeBillingKey() {
  const res = await post(SUBSCRIPTION_ENDPOINTS.billingRevoke);
  return unwrap(res);
}

/** 구독 시작(첫 결제 포함) */
export async function startSubscription() {
  const res = await post(SUBSCRIPTION_ENDPOINTS.start);
  return unwrap(res);
}

/** 자동결제 취소 */
export async function cancelAutoPay() {
  const res = await post(SUBSCRIPTION_ENDPOINTS.autoPayCancel);
  return unwrap(res);
}

/** 자동결제 재활성화(재구독) */
export async function reactivateAutoPay() {
  const res = await post(SUBSCRIPTION_ENDPOINTS.autoPayReactivate);
  return unwrap(res);
}

/** 내 구독 정보 조회 */
export async function getMySubscription() {
  const res = await get(SUBSCRIPTION_ENDPOINTS.me);
  return unwrap(res);
}

/** 휴대폰 인증번호 발송 */
export async function sendPhoneAuth(phoneNumber) {
  const res = await post(SUBSCRIPTION_ENDPOINTS.phoneAuthSend, { phoneNumber });
  return unwrap(res);
}

/** 휴대폰 인증번호 검증 */
export async function verifyPhoneAuth(payload) {
  const res = await post(SUBSCRIPTION_ENDPOINTS.phoneAuthVerify, payload);
  return unwrap(res);
}

export default {
  issueBillingKey,
  billingSuccess,
  billingFail,
  revokeBillingKey,
  startSubscription,
  cancelAutoPay,
  reactivateAutoPay,
  getMySubscription,
  sendPhoneAuth,
  verifyPhoneAuth,
};