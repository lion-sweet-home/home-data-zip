/**
 * 아파트 관련 API
 * 아파트 정보 및 주변 시설 정보를 조회합니다.
 */

import { get } from './api';

/**
 * 아파트 기준 가까운 지하철역 top 3 조회
 * 
 * @param {number} apartmentId - 아파트 ID
 * @returns {Promise<Array>} 가까운 지하철역 목록 (최대 3개)
 * 
 * 사용 예시:
 * const subways = await getNearbySubways(123);
 */
export async function getNearbySubways(apartmentId) {
  return get(`/apartments/${apartmentId}/subways`);
}

/**
 * 아파트 기준 반경 내 버스 정류장 조회
 * 
 * @param {number} apartmentId - 아파트 ID
 * @param {number} [radiusMeters=500] - 반경 (미터, 기본값: 500)
 * @param {number} [limit=50] - 최대 개수 (기본값: 50)
 * @returns {Promise<{count: number, items: Array}>} 버스 정류장 목록
 * 
 * 사용 예시:
 * const busStations = await getNearbyBusStations(123, 500, 50);
 */
export async function getNearbyBusStations(apartmentId, radiusMeters = 500, limit = 50) {
  return get(`/apartments/${apartmentId}/bus-stations?radiusMeters=${radiusMeters}&limit=${limit}`);
}

/**
 * 아파트 월별 거래량 조회
 * 
 * @param {number} aptId - 아파트 ID
 * @param {number} [period] - 기간 (개월 수, 6, 12, 24, 36, 48)
 * @returns {Promise<Array>} 월별 거래량 데이터
 * 
 * 사용 예시:
 * const monthlyData = await getMonthlyTradeVolume(123, 12);
 */
export async function getMonthlyTradeVolume(aptId, period) {
  const queryParams = new URLSearchParams();
  if (period) queryParams.append('period', period);
  
  const queryString = queryParams.toString();
  return get(`/apartments/month-avg/${aptId}/total-rent${queryString ? `?${queryString}` : ''}`);
}

/**
 * 아파트 최근 거래내역 조회 (최대 5건)
 * 
 * @param {number} aptId - 아파트 ID
 * @returns {Promise<Array>} 최근 거래내역 목록 (최대 5건)
 * 
 * 사용 예시:
 * const recentTrades = await getRecentTrades(123);
 */
export async function getRecentTrades(aptId) {
  return get(`/rent/${aptId}`);
}

// 기본 export
export default {
  getNearbySubways,
  getNearbyBusStations,
  getMonthlyTradeVolume,
  getRecentTrades,
};
