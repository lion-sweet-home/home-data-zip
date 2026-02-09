/**
 * 버스 정류장 관련 API
 * 버스 정류장 정보를 조회합니다.
 */

import { get, post } from './api';

/**
 * 버스 정류장 목록 조회
 * 
 * @param {object} params - 조회 파라미터
 * @param {number} params.pageNo - 페이지 번호 (기본값: 1)
 * @param {number} params.numOfRows - 페이지당 항목 수 (기본값: 10)
 * @param {string} [params.keyword] - 검색 키워드 (정류장명, 주소 등)
 * @returns {Promise<{items: Array, totalCount: number}>} 버스 정류장 목록
 * 
 * 사용 예시:
 * const data = await getBusStationList({
 *   pageNo: 1,
 *   numOfRows: 20,
 *   keyword: '강남역'
 * });
 */
export async function getBusStationList(params = {}) {
  const queryParams = new URLSearchParams();
  
  if (params.pageNo) queryParams.append('pageNo', params.pageNo);
  if (params.numOfRows) queryParams.append('numOfRows', params.numOfRows);
  if (params.keyword) queryParams.append('keyword', params.keyword);
  
  const queryString = queryParams.toString();
  return get(`/bus/stations${queryString ? `?${queryString}` : ''}`);
}

/**
 * 특정 버스 정류장 상세 정보 조회
 * 
 * @param {string|number} id - 버스 정류장 ID
 * @returns {Promise<object>} 버스 정류장 상세 정보
 * 
 * 사용 예시:
 * const detail = await getBusStationDetail(123);
 */
export async function getBusStationDetail(id) {
  return get(`/bus/stations/${id}`);
}

/**
 * 좌표 기반 근처 버스 정류장 조회
 * 
 * @param {object} params - 조회 파라미터
 * @param {number} params.latitude - 위도
 * @param {number} params.longitude - 경도
 * @param {number} [params.radius] - 반경 (미터, 기본값: 500)
 * @returns {Promise<Array>} 근처 버스 정류장 목록
 * 
 * 사용 예시:
 * const nearbyStations = await getNearbyBusStations({
 *   latitude: 37.5665,
 *   longitude: 126.9780,
 *   radius: 1000
 * });
 */
export async function getNearbyBusStations(params) {
  const queryParams = new URLSearchParams();
  
  queryParams.append('latitude', params.latitude);
  queryParams.append('longitude', params.longitude);
  if (params.radius) queryParams.append('radius', params.radius);
  
  const queryString = queryParams.toString();
  return get(`/bus/stations/nearby?${queryString}`);
}

/**
 * 버스 정류장 검색
 * 
 * @param {object} searchParams - 검색 조건
 * @param {string} searchParams.keyword - 검색 키워드
 * @param {number} [searchParams.latitude] - 위도 (거리순 정렬용)
 * @param {number} [searchParams.longitude] - 경도 (거리순 정렬용)
 * @returns {Promise<{items: Array, totalCount: number}>} 검색 결과
 * 
 * 사용 예시:
 * const results = await searchBusStations({
 *   keyword: '강남역',
 *   latitude: 37.5665,
 *   longitude: 126.9780
 * });
 */
export async function searchBusStations(searchParams) {
  return post('/bus/stations/search', searchParams);
}

/**
 * 특정 정류장의 버스 노선 조회
 * 
 * @param {string|number} stationId - 버스 정류장 ID
 * @returns {Promise<Array>} 해당 정류장을 지나는 버스 노선 목록
 * 
 * 사용 예시:
 * const routes = await getBusRoutesByStation(123);
 */
export async function getBusRoutesByStation(stationId) {
  return get(`/bus/stations/${stationId}/routes`);
}

// 기본 export
export default {
  getBusStationList,
  getBusStationDetail,
  getNearbyBusStations,
  searchBusStations,
  getBusRoutesByStation,
};
