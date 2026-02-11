/**
 * 지하철역 관련 API
 * 지하철역 정보를 조회합니다.
 */

import { get, post } from './api';

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

/**
 * 특정 지하철역 상세 정보 조회
 * 
 * @param {string|number} id - 지하철역 ID
 * @returns {Promise<object>} 지하철역 상세 정보
 * 
 * 사용 예시:
 * const detail = await getSubwayStationDetail(123);
 */
export async function getSubwayStationDetail(id) {
  return get(`/subway/stations/${id}`);
}

/**
 * 좌표 기반 근처 지하철역 조회
 * 
 * @param {object} params - 조회 파라미터
 * @param {number} params.latitude - 위도
 * @param {number} params.longitude - 경도
 * @param {number} [params.radius] - 반경 (미터, 기본값: 1000)
 * @returns {Promise<Array>} 근처 지하철역 목록 (거리순 정렬)
 * 
 * 사용 예시:
 * const nearbyStations = await getNearbySubwayStations({
 *   latitude: 37.5665,
 *   longitude: 126.9780,
 *   radius: 2000
 * });
 */
export async function getNearbySubwayStations(params) {
  const queryParams = new URLSearchParams();
  
  queryParams.append('latitude', params.latitude);
  queryParams.append('longitude', params.longitude);
  if (params.radius) queryParams.append('radius', params.radius);
  
  const queryString = queryParams.toString();
  return get(`/subway/stations/nearby?${queryString}`);
}

/**
 * 특정 역의 환승 정보 조회
 * 
 * @param {string|number} stationId - 지하철역 ID
 * @returns {Promise<Array>} 환승 가능한 노선 목록
 * 
 * 사용 예시:
 * const transfers = await getTransferInfo(123);
 */
export async function getTransferInfo(stationId) {
  return get(`/subway/stations/${stationId}/transfers`);
}

/**
 * 노선별 지하철역 목록 조회
 * 
 * @param {string} line - 노선명
 * @returns {Promise<Array>} 해당 노선의 지하철역 목록 (순서대로)
 * 
 * 사용 예시:
 * const stations = await getStationsByLine('2호선');
 */
export async function getStationsByLine(line) {
  const queryParams = new URLSearchParams();
  queryParams.append('line', line);
  return get(`/subway/stations/line?${queryParams.toString()}`);
}

/**
 * 두 지하철역 간 경로 조회
 * 
 * @param {object} params - 경로 조회 파라미터
 * @param {string|number} params.fromStationId - 출발역 ID
 * @param {string|number} params.toStationId - 도착역 ID
 * @returns {Promise<{path: Array, transferCount: number, totalTime: number}>} 경로 정보
 * 
 * 사용 예시:
 * const route = await getRoute({
 *   fromStationId: 123,
 *   toStationId: 456
 * });
 */
export async function getRoute(params) {
  return post('/subway/route', params);
}

// 기본 export
export default {
  searchSubwayStations,
  getApartmentsNearSubway,
  getSubwayStationDetail,
  getNearbySubwayStations,
  getTransferInfo,
  getStationsByLine,
  getRoute,
};
