/**
 * 지하철역 관련 API
 * 지하철역 정보를 조회합니다.
 */

import { get } from './api';

/**
 * 지하철역 검색 (역명 또는 호선으로 검색)
 * 
 * @param {object} params - 검색 파라미터
 * @param {string} [params.stationName] - 역명 (예: '강남')
 * @param {string} [params.lineName] - 호선명 (예: '2호선')
 * @returns {Promise<Array>} 지하철역 목록
 * 
 * 사용 예시:
 * const stations = await searchSubwayStations({
 *   stationName: '강남',
 *   lineName: '2호선'
 * });
 */
export async function searchSubwayStations(params = {}) {
  const queryParams = new URLSearchParams();
  
  if (params.stationName) queryParams.append('stationName', params.stationName);
  if (params.lineName) queryParams.append('lineName', params.lineName);
  
  const queryString = queryParams.toString();
  return get(`/subway/stations${queryString ? `?${queryString}` : ''}`);
}

/**
 * 지하철역 반경 내 아파트 검색
 * 
 * @param {number} stationId - 지하철역 ID
 * @param {number} distanceKm - 반경 (km)
 * @returns {Promise<Array>} 반경 내 아파트 목록
 * 
 * 사용 예시:
 * const apartments = await getApartmentsNearSubway(1, 2.0);
 */
export async function getApartmentsNearSubway(stationId, distanceKm) {
  const queryParams = new URLSearchParams();
  queryParams.append('distanceKm', distanceKm);
  return get(`/subway/stations/${stationId}/apartments?${queryParams.toString()}`);
}


// 기본 export
export default {
  searchSubwayStations,
  getApartmentsNearSubway,
};
