/**
 * 결제 관련 API
 * 결제 준비, 결제 확인, 결제 내역 조회 기능을 제공합니다.
 */

import { get, post } from './api';

/**
 * 결제 준비
 * 
 * @param {object} [request] - 결제 준비 요청 정보 (선택사항)
 * @returns {Promise<{orderId: string, amount: number, orderName: string}>} 결제 준비 정보
 * 
 * 사용 예시:
 * const prepareResult = await preparePayment();
 */
export async function preparePayment(request) {
  return post('/payments/prepare', request);
}

/**
 * 결제 확인
 * 
 * @param {object} request - 결제 확인 요청 정보
 * @param {string} request.orderId - 주문 ID
 * @param {string} request.paymentKey - 결제 키
 * @param {number} request.amount - 결제 금액
 * @returns {Promise<{paymentKey: string, orderId: string, amount: number}>} 결제 확인 결과
 * 
 * 사용 예시:
 * const confirmResult = await confirmPayment({
 *   orderId: 'ORDER_123',
 *   paymentKey: 'PAYMENT_KEY',
 *   amount: 10000
 * });
 */
export async function confirmPayment(request) {
  return post('/payments/confirm', request);
}

/**
 * 내 결제 내역 조회
 * 
 * @returns {Promise<{payments: Array, totalCount: number}>} 결제 내역 목록
 * 
 * 사용 예시:
 * const payments = await getMyPayments();
 */
export async function getMyPayments() {
  return get('/payments/me');
}

/**
 * 최근 결제 내역 조회
 * 
 * @returns {Promise<object>} 최근 결제 내역
 * 
 * 사용 예시:
 * const latestPayment = await getMyLatestPayment();
 */
export async function getMyLatestPayment() {
  return get('/payments/me/latest');
}

// 기본 export
export default {
  preparePayment,
  confirmPayment,
  getMyPayments,
  getMyLatestPayment,
};
