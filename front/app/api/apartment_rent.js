/**
 * 아파트 전월세 거래 관련 API
 * TradeRentGetController와 매칭
 * @see /api/rent
 */

import { get } from './api';

/**
 * 전월세 마커 조회
 * 
 * @param {object} request - 검색 조건
 * @param {string} [request.sido] - 시도명
 * @param {string} [request.gugun] - 구/군명
 * @param {string} [request.dong] - 동명
 * @param {number} [request.minDeposit] - 최소 보증금 (원)
 * @param {number} [request.maxDeposit] - 최대 보증금 (원)
 * @param {number} [request.minMonthlyRent] - 최소 월세 (원)
 * @param {number} [request.maxMonthlyRent] - 최대 월세 (원)
 * @returns {Promise<Array>} 마커 데이터 [{aptId, aptNm, latitude, longitude}]
 * 
 * 사용 예시:
 * const markers = await getRentMarkers({
 *   sido: '서울특별시',
 *   gugun: '강남구',
 *   dong: '역삼동',
 *   minDeposit: 100000000,
 *   maxDeposit: 500000000
 * });
 */
export async function getRentMarkers(request) {
  const params = new URLSearchParams();
  if (request.sido) params.append('sido', request.sido);
  if (request.gugun) params.append('gugun', request.gugun);
  if (request.dong) params.append('dong', request.dong);
  if (request.minDeposit != null) params.append('minDeposit', request.minDeposit);
  if (request.maxDeposit != null) params.append('maxDeposit', request.maxDeposit);
  if (request.minMonthlyRent != null) params.append('minMonthlyRent', request.minMonthlyRent);
  if (request.maxMonthlyRent != null) params.append('maxMonthlyRent', request.maxMonthlyRent);
  if (request.minExclusive != null) params.append('minExclusive', request.minExclusive);
  if (request.maxExclusive != null) params.append('maxExclusive', request.maxExclusive);
  return get(`/rent?${params.toString()}`);
}

/**
 * 전월세 그래프 점 데이터 조회
 * 
 * @param {number} aptId - 아파트 ID
 * @param {number} period - 기간 (개월 수)
 * @returns {Promise<Array>} 그래프 점 데이터
 * 
 * 사용 예시:
 * const dots = await getRentDots(123, 12);
 */
export async function getRentDots(aptId, period) {
  const params = new URLSearchParams();
  params.append('period', period);
  return get(`/rent/${aptId}/dots?${params.toString()}`);
}

/**
 * 아파트 최근 전월세 거래내역 조회 (최대 5건)
 * 
 * @param {number} aptId - 아파트 ID
 * @returns {Promise<Array>} 최근 거래내역 목록 (최대 5건)
 * 
 * 사용 예시:
 * const recentTrades = await getRecentRentTrades(123);
 */
export async function getRecentRentTrades(aptId) {
  return get(`/rent/${aptId}`);
}

/**
 * 평형별 전월세 거래기록 조회
 * 
 * @param {number} aptId - 아파트 ID
 * @param {number} areaKey - 면적 키 (areaKey)
 * @param {number} [period] - 기간 (개월 수, 선택사항)
 * @returns {Promise<object>} 평형별 거래기록
 * 
 * 사용 예시:
 * const detail = await getRentDetailByArea(123, 84, 12);
 */
export async function getRentDetailByArea(aptId, areaKey, period) {
  const params = new URLSearchParams();
  params.append('areaKey', areaKey);
  if (period) params.append('period', period);
  return get(`/rent/${aptId}/detail?${params.toString()}`);
}

// 기본 export
export default {
  getRentMarkers,
  getRentDots,
  getRecentRentTrades,
  getRentDetailByArea,
};
