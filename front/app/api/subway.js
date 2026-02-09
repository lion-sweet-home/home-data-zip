/**
 * 지하철역 관련 API
 * 지하철역 정보를 조회합니다.
 */

import { get, post } from './api';

/**
 * 지하철역 목록 조회
 * 
 * @param {object} params - 조회 파라미터
 * @param {number} params.pageNo - 페이지 번호 (기본값: 1)
 * @param {number} params.numOfRows - 페이지당 항목 수 (기본값: 10)
 * @param {string} [params.keyword] - 검색 키워드 (역명, 주소 등)
 * @param {string} [params.line] - 노선명 (예: '1호선', '2호선', '분당선' 등)
 * @returns {Promise<{items: Array, totalCount: number}>} 지하철역 목록
 * 
 * 사용 예시:
 * const data = await getSubwayStationList({
 *   pageNo: 1,
 *   numOfRows: 20,
 *   keyword: '강남',
 *   line: '2호선'
 * });
 */
export async function getSubwayStationList(params = {}) {
  const queryParams = new URLSearchParams();
  
  if (params.pageNo) queryParams.append('pageNo', params.pageNo);
  if (params.numOfRows) queryParams.append('numOfRows', params.numOfRows);
  if (params.keyword) queryParams.append('keyword', params.keyword);
  if (params.line) queryParams.append('line', params.line);
  
  const queryString = queryParams.toString();
  return get(`/subway/stations${queryString ? `?${queryString}` : ''}`);
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
 * 지하철역 검색
 * 
 * @param {object} searchParams - 검색 조건
 * @param {string} searchParams.keyword - 검색 키워드
 * @param {string} [searchParams.line] - 노선명
 * @param {number} [searchParams.latitude] - 위도 (거리순 정렬용)
 * @param {number} [searchParams.longitude] - 경도 (거리순 정렬용)
 * @returns {Promise<{items: Array, totalCount: number}>} 검색 결과
 * 
 * 사용 예시:
 * const results = await searchSubwayStations({
 *   keyword: '강남',
 *   line: '2호선',
 *   latitude: 37.5665,
 *   longitude: 126.9780
 * });
 */
export async function searchSubwayStations(searchParams) {
  return post('/subway/stations/search', searchParams);
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
  getSubwayStationList,
  getSubwayStationDetail,
  getNearbySubwayStations,
  searchSubwayStations,
  getTransferInfo,
  getStationsByLine,
  getRoute,
};
