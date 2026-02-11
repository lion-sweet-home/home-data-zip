/**
 * 지역(법정동) 관련 API
 * 법정동 코드 및 지역 정보를 조회합니다.
 */

import { get, post } from './api';

/**
 * 지역(법정동) 목록 조회
 * 
 * @param {object} params - 조회 파라미터
 * @param {string} [params.sido] - 시도명 (예: '서울특별시')
 * @param {string} [params.sigungu] - 시군구명 (예: '강남구')
 * @param {string} [params.dong] - 동명 (예: '역삼동')
 * @param {number} [params.pageNo] - 페이지 번호
 * @param {number} [params.numOfRows] - 페이지당 항목 수
 * @returns {Promise<{items: Array, totalCount: number}>} 지역 목록
 * 
 * 사용 예시:
 * const data = await getRegionList({
 *   sido: '서울특별시',
 *   sigungu: '강남구'
 * });
 */
export async function getRegionList(params = {}) {
  const queryParams = new URLSearchParams();
  
  if (params.sido) queryParams.append('sido', params.sido);
  if (params.sigungu) queryParams.append('sigungu', params.sigungu);
  if (params.dong) queryParams.append('dong', params.dong);
  if (params.pageNo) queryParams.append('pageNo', params.pageNo);
  if (params.numOfRows) queryParams.append('numOfRows', params.numOfRows);
  
  const queryString = queryParams.toString();
  return get(`/region${queryString ? `?${queryString}` : ''}`);
}

/**
 * 법정동 코드로 지역 정보 조회
 * 
 * @param {string} lawdCd - 법정동 코드 (5자리 또는 10자리)
 * @returns {Promise<object>} 지역 정보
 * 
 * 사용 예시:
 * const region = await getRegionByCode('1168010100');
 */
export async function getRegionByCode(lawdCd) {
  return get(`/region/${lawdCd}`);
}

/**
 * 지역 검색 (자동완성용)
 * 
 * @param {object} searchParams - 검색 조건
 * @param {string} searchParams.keyword - 검색 키워드 (시도, 시군구, 동명)
 * @returns {Promise<Array>} 검색 결과 (자동완성 목록)
 * 
 * 사용 예시:
 * const suggestions = await searchRegion({
 *   keyword: '강남'
 * });
 */
export async function searchRegion(searchParams) {
  return post('/region/search', searchParams);
}

/**
 * 시도 목록 조회
 * 
 * @returns {Promise<Array<string>>} 시도 목록
 * 
 * 사용 예시:
 * const sidos = await getSidoList();
 */
export async function getSidoList() {
  return get('/regions/sido');
}

/**
 * 구/군 목록 조회
 * 
 * @param {string} sido - 시도명
 * @returns {Promise<Array<string>>} 해당 시도의 구/군 목록
 * 
 * 사용 예시:
 * const guguns = await getGugunList('서울특별시');
 */
export async function getGugunList(sido) {
  const queryParams = new URLSearchParams();
  queryParams.append('sido', sido);
  return get(`/regions/gugun?${queryParams.toString()}`);
}

/**
 * 동 목록 조회
 * 
 * @param {string} sido - 시도명
 * @param {string} gugun - 구/군명
 * @returns {Promise<Array<string>>} 해당 구/군의 동 목록
 * 
 * 사용 예시:
 * const dongs = await getDongList('서울특별시', '강남구');
 */
export async function getDongList(sido, gugun) {
  const queryParams = new URLSearchParams();
  queryParams.append('sido', sido);
  queryParams.append('gugun', gugun);
  return get(`/regions/dong?${queryParams.toString()}`);
}

/**
 * 좌표로 법정동 코드 조회 (역지오코딩)
 * 
 * @param {object} params - 조회 파라미터
 * @param {number} params.latitude - 위도
 * @param {number} params.longitude - 경도
 * @returns {Promise<object>} 해당 좌표의 법정동 정보
 * 
 * 사용 예시:
 * const region = await getRegionByCoordinates({
 *   latitude: 37.5665,
 *   longitude: 126.9780
 * });
 */
export async function getRegionByCoordinates(params) {
  const queryParams = new URLSearchParams();
  queryParams.append('latitude', params.latitude);
  queryParams.append('longitude', params.longitude);
  return get(`/region/coordinates?${queryParams.toString()}`);
}

// 기본 export
export default {
  getRegionList,
  getRegionByCode,
  searchRegion,
  getSidoList,
  getGugunList,
  getDongList,
  getRegionByCoordinates,
};
