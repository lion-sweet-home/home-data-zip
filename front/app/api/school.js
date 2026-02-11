/**
 * 학교 관련 API
 * 학교 정보를 조회합니다.
 */

import { get, post } from './api';

/**
 * 지역으로 학교 목록 조회
 * 
 * @param {object} params - 조회 파라미터
 * @param {string} params.sido - 시도명 (필수)
 * @param {string} params.gugun - 구/군명 (필수)
 * @param {string} [params.dong] - 동명 (선택)
 * @param {Array<string>} [params.schoolLevel] - 학교급 (선택: '초등학교', '중학교', '고등학교')
 * @returns {Promise<Array>} 학교 목록
 * 
 * 사용 예시:
 * const schools = await searchSchoolsByRegion({
 *   sido: '서울특별시',
 *   gugun: '강남구',
 *   dong: '역삼동',
 *   schoolLevel: ['초등학교', '중학교']
 * });
 */
export async function searchSchoolsByRegion(params) {
  const queryParams = new URLSearchParams();
  
  queryParams.append('sido', params.sido);
  queryParams.append('gugun', params.gugun);
  if (params.dong) queryParams.append('dong', params.dong);
  if (params.schoolLevel && Array.isArray(params.schoolLevel)) {
    params.schoolLevel.forEach(level => queryParams.append('schoolLevel', level));
  }
  
  return get(`/schools?${queryParams.toString()}`);
}

/**
 * 학교 반경 내 아파트 검색
 * 
 * @param {number} schoolId - 학교 ID
 * @param {number} distanceKm - 반경 (km)
 * @returns {Promise<Array>} 반경 내 아파트 목록
 * 
 * 사용 예시:
 * const apartments = await getApartmentsNearSchool(1, 2.0);
 */
export async function getApartmentsNearSchool(schoolId, distanceKm) {
  const queryParams = new URLSearchParams();
  queryParams.append('distanceKm', distanceKm);
  return get(`/schools/${schoolId}/apartments?${queryParams.toString()}`);
}

/**
 * 특정 학교 상세 정보 조회
 * 
 * @param {string|number} id - 학교 ID
 * @returns {Promise<object>} 학교 상세 정보
 * 
 * 사용 예시:
 * const detail = await getSchoolDetail(123);
 */
export async function getSchoolDetail(id) {
  return get(`/school/${id}`);
}

/**
 * 좌표 기반 근처 학교 조회
 * 
 * @param {object} params - 조회 파라미터
 * @param {number} params.latitude - 위도
 * @param {number} params.longitude - 경도
 * @param {number} [params.radius] - 반경 (미터, 기본값: 2000)
 * @param {string} [params.schoolType] - 학교 유형 필터
 * @returns {Promise<Array>} 근처 학교 목록 (거리순 정렬)
 * 
 * 사용 예시:
 * const nearbySchools = await getNearbySchools({
 *   latitude: 37.5665,
 *   longitude: 126.9780,
 *   radius: 1000,
 *   schoolType: '초등학교'
 * });
 */
export async function getNearbySchools(params) {
  const queryParams = new URLSearchParams();
  
  queryParams.append('latitude', params.latitude);
  queryParams.append('longitude', params.longitude);
  if (params.radius) queryParams.append('radius', params.radius);
  if (params.schoolType) queryParams.append('schoolType', params.schoolType);
  
  const queryString = queryParams.toString();
  return get(`/school/nearby?${queryString}`);
}

/**
 * 학교 검색
 * 
 * @param {object} searchParams - 검색 조건
 * @param {string} [searchParams.keyword] - 검색 키워드
 * @param {string} [searchParams.schoolType] - 학교 유형
 * @param {string} [searchParams.lawdCd] - 법정동 코드
 * @param {number} [searchParams.latitude] - 위도 (거리순 정렬용)
 * @param {number} [searchParams.longitude] - 경도 (거리순 정렬용)
 * @returns {Promise<{items: Array, totalCount: number}>} 검색 결과
 * 
 * 사용 예시:
 * const results = await searchSchools({
 *   keyword: '서울초등학교',
 *   schoolType: '초등학교',
 *   lawdCd: '11680'
 * });
 */
export async function searchSchools(searchParams) {
  return post('/school/search', searchParams);
}

/**
 * 학교 필터링 조회
 * 
 * @param {object} filters - 필터 조건
 * @param {string[]} [filters.schoolTypes] - 학교 유형 배열
 * @param {string[]} [filters.lawdCds] - 법정동 코드 배열
 * @param {string[]} [filters.sidos] - 시도명 배열
 * @param {string[]} [filters.sigungus] - 시군구명 배열
 * @returns {Promise<{items: Array, totalCount: number}>} 필터링된 결과
 * 
 * 사용 예시:
 * const filtered = await filterSchools({
 *   schoolTypes: ['초등학교', '중학교'],
 *   lawdCds: ['11680', '11681']
 * });
 */
export async function filterSchools(filters) {
  return post('/school/filter', filters);
}

/**
 * 특정 지역의 학교 통계 조회
 * 
 * @param {string} lawdCd - 법정동 코드
 * @returns {Promise<{elementary: number, middle: number, high: number}>} 학교 유형별 개수
 * 
 * 사용 예시:
 * const stats = await getSchoolStats('11680');
 */
export async function getSchoolStats(lawdCd) {
  return get(`/school/stats/${lawdCd}`);
}

// 기본 export
export default {
  searchSchoolsByRegion,
  getApartmentsNearSchool,
  getSchoolDetail,
  getNearbySchools,
  searchSchools,
  filterSchools,
  getSchoolStats,
};
