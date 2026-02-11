/**
 * 버스 정류장 관련 API
 * 버스 정류장 정보를 조회합니다.
 */

import { get } from './api';

/**
 * 좌표 기준 근처 버스 정류장 조회
 * 
 * @param {object} params - 조회 파라미터
 * @param {number} params.lat - 위도
 * @param {number} params.lon - 경도
 * @param {number} [params.radiusMeters=500] - 반경 (미터, 기본값: 500)
 * @param {number} [params.limit=50] - 최대 개수 (기본값: 50)
 * @returns {Promise<Array>} 근처 버스 정류장 목록
 * 
 * 사용 예시:
 * const nearbyStations = await getNearbyBusStations({
 *   lat: 37.5665,
 *   lon: 126.9780,
 *   radiusMeters: 1000,
 *   limit: 20
 * });
 */
export async function getNearbyBusStations(params) {
  const queryParams = new URLSearchParams();
  
  queryParams.append('lat', params.lat);
  queryParams.append('lon', params.lon);
  if (params.radiusMeters) queryParams.append('radiusMeters', params.radiusMeters);
  if (params.limit) queryParams.append('limit', params.limit);
  
  return get(`/bus-stations/nearby?${queryParams.toString()}`);
}

// 기본 export
export default {
  getNearbyBusStations,
};
