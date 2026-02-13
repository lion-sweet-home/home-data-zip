/**
 * 아파트 매매 거래 관련 API
 * TradeSaleController와 매칭
 * @see /api/apartment/trade-sale
 */

import { get } from './api';

/**
 * 매매 마커 조회
 * 
 * @param {object} request - 검색 조건
 * @param {string} request.sido - 시도명 (필수)
 * @param {string} request.gugun - 구/군명 (필수)
 * @param {string} [request.dong] - 동명
 * @param {string} [request.keyword] - 키워드
 * @param {number} [request.minAmount] - 최소 가격 (원)
 * @param {number} [request.maxAmount] - 최대 가격 (원)
 * @param {number} [request.periodMonths] - 기간 (개월 수)
 * @returns {Promise<Array>} 마커 데이터 [{aptId, aptNm, latitude, longitude}]
 * 
 * 사용 예시:
 * const markers = await getSaleMarkers({
 *   sido: '서울특별시',
 *   gugun: '강남구',
 *   dong: '역삼동',
 *   minAmount: 500000000,
 *   maxAmount: 1000000000
 * });
 */
export async function getSaleMarkers(request) {
  const params = new URLSearchParams();
  if (request.sido) params.append('sido', request.sido);
  if (request.gugun) params.append('gugun', request.gugun);
  if (request.dong) params.append('dong', request.dong);
  if (request.keyword) params.append('keyword', request.keyword);
  if (request.minAmount != null) params.append('minAmount', request.minAmount);
  if (request.maxAmount != null) params.append('maxAmount', request.maxAmount);
  if (request.periodMonths) params.append('periodMonths', request.periodMonths);
  return get(`/apartment/trade-sale/markers?${params.toString()}`);
}

/**
 * 아파트 매매 요약 정보 조회
 * 
 * @param {number} aptId - 아파트 ID
 * @param {number} [periodMonths=6] - 기간 (개월 수, 기본값: 6)
 * @returns {Promise<object>} 아파트 매매 요약 정보
 * 
 * 사용 예시:
 * const summary = await getAptSaleSummary(123, 12);
 */
export async function getAptSaleSummary(aptId, periodMonths = 6) {
  const params = new URLSearchParams();
  params.append('periodMonths', periodMonths);
  return get(`/apartment/trade-sale/${aptId}/summary?${params.toString()}`);
}

/**
 * 아파트 매매 상세 정보 조회
 * 
 * @param {number} aptId - 아파트 ID
 * @param {number} [periodMonths=6] - 기간 (개월 수, 기본값: 6)
 * @returns {Promise<object>} 아파트 매매 상세 정보
 * 
 * 사용 예시:
 * const detail = await getAptSaleDetail(123, 12);
 */
export async function getAptSaleDetail(aptId, periodMonths = 6) {
  const params = new URLSearchParams();
  params.append('periodMonths', periodMonths);
  return get(`/apartment/trade-sale/${aptId}/detail?${params.toString()}`);
}

/**
 * 아파트 매매 차트 데이터 조회
 * 
 * @param {number} aptId - 아파트 ID
 * @param {number} [periodMonths=6] - 기간 (개월 수, 기본값: 6)
 * @returns {Promise<object>} 아파트 매매 차트 데이터
 * 
 * 사용 예시:
 * const chart = await getAptSaleChart(123, 12);
 */
export async function getAptSaleChart(aptId, periodMonths = 6) {
  const params = new URLSearchParams();
  params.append('periodMonths', periodMonths);
  return get(`/apartment/trade-sale/${aptId}/chart?${params.toString()}`);
}

// 기본 export
export default {
  getSaleMarkers,
  getAptSaleSummary,
  getAptSaleDetail,
  getAptSaleChart,
};
