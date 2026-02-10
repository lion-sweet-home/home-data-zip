/**
 * 병원 관련 API
 * 병원 정보를 조회합니다.
 */

import { get, post } from './api';

/**
 * 병원 목록 조회
 * 
 * @param {object} params - 조회 파라미터
 * @param {number} params.pageNo - 페이지 번호 (기본값: 1)
 * @param {number} params.numOfRows - 페이지당 항목 수 (기본값: 10)
 * @param {string} [params.keyword] - 검색 키워드 (병원명, 주소 등)
 * @param {string} [params.hospitalType] - 병원 유형 (종합병원, 병원, 의원 등)
 * @returns {Promise<{items: Array, totalCount: number}>} 병원 목록
 * 
 * 사용 예시:
 * const data = await getHospitalList({
 *   pageNo: 1,
 *   numOfRows: 20,
 *   keyword: '서울대',
 *   hospitalType: '종합병원'
 * });
 */
export async function getHospitalList(params = {}) {
  const queryParams = new URLSearchParams();
  
  if (params.pageNo) queryParams.append('pageNo', params.pageNo);
  if (params.numOfRows) queryParams.append('numOfRows', params.numOfRows);
  if (params.keyword) queryParams.append('keyword', params.keyword);
  if (params.hospitalType) queryParams.append('hospitalType', params.hospitalType);
  
  const queryString = queryParams.toString();
  return get(`/hospital${queryString ? `?${queryString}` : ''}`);
}

/**
 * 특정 병원 상세 정보 조회
 * 
 * @param {string|number} id - 병원 ID
 * @returns {Promise<object>} 병원 상세 정보
 * 
 * 사용 예시:
 * const detail = await getHospitalDetail(123);
 */
export async function getHospitalDetail(id) {
  return get(`/hospital/${id}`);
}

/**
 * 좌표 기반 근처 병원 조회
 * 
 * @param {object} params - 조회 파라미터
 * @param {number} params.latitude - 위도
 * @param {number} params.longitude - 경도
 * @param {number} [params.radius] - 반경 (미터, 기본값: 2000)
 * @param {string} [params.hospitalType] - 병원 유형 필터
 * @returns {Promise<Array>} 근처 병원 목록 (거리순 정렬)
 * 
 * 사용 예시:
 * const nearbyHospitals = await getNearbyHospitals({
 *   latitude: 37.5665,
 *   longitude: 126.9780,
 *   radius: 3000,
 *   hospitalType: '종합병원'
 * });
 */
export async function getNearbyHospitals(params) {
  const queryParams = new URLSearchParams();
  
  queryParams.append('latitude', params.latitude);
  queryParams.append('longitude', params.longitude);
  if (params.radius) queryParams.append('radius', params.radius);
  if (params.hospitalType) queryParams.append('hospitalType', params.hospitalType);
  
  const queryString = queryParams.toString();
  return get(`/hospital/nearby?${queryString}`);
}

/**
 * 병원 검색
 * 
 * @param {object} searchParams - 검색 조건
 * @param {string} [searchParams.keyword] - 검색 키워드
 * @param {string} [searchParams.hospitalType] - 병원 유형
 * @param {string} [searchParams.sido] - 시도명
 * @param {string} [searchParams.sigungu] - 시군구명
 * @param {number} [searchParams.latitude] - 위도 (거리순 정렬용)
 * @param {number} [searchParams.longitude] - 경도 (거리순 정렬용)
 * @returns {Promise<{items: Array, totalCount: number}>} 검색 결과
 * 
 * 사용 예시:
 * const results = await searchHospitals({
 *   keyword: '서울대',
 *   hospitalType: '종합병원',
 *   sido: '서울특별시'
 * });
 */
export async function searchHospitals(searchParams) {
  return post('/hospital/search', searchParams);
}

/**
 * 병원 필터링 조회
 * 
 * @param {object} filters - 필터 조건
 * @param {string[]} [filters.hospitalTypes] - 병원 유형 배열
 * @param {string[]} [filters.sidos] - 시도명 배열
 * @param {string[]} [filters.sigungus] - 시군구명 배열
 * @param {boolean} [filters.hasEmergency] - 응급실 보유 여부
 * @returns {Promise<{items: Array, totalCount: number}>} 필터링된 결과
 * 
 * 사용 예시:
 * const filtered = await filterHospitals({
 *   hospitalTypes: ['종합병원', '병원'],
 *   sidos: ['서울특별시', '경기도'],
 *   hasEmergency: true
 * });
 */
export async function filterHospitals(filters) {
  return post('/hospital/filter', filters);
}

// 기본 export
export default {
  getHospitalList,
  getHospitalDetail,
  getNearbyHospitals,
  searchHospitals,
  filterHospitals,
};
