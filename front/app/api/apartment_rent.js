/**
 * 아파트 전세/월세 관련 API
 * 아파트 전세 및 월세 거래 정보를 조회합니다.
 */

import { get, post } from './api';

/**
 * 아파트 전세/월세 목록 조회
 * 
 * @param {object} params - 조회 파라미터
 * @param {string} params.lawdCd - 법정동 코드 (5자리)
 * @param {string} params.dealYmd - 거래년월 (YYYYMM 형식)
 * @param {number} params.pageNo - 페이지 번호 (기본값: 1)
 * @param {number} params.numOfRows - 페이지당 항목 수 (기본값: 10)
 * @param {string} [params.rentType] - 전세/월세 구분 ('전세' | '월세')
 * @returns {Promise<{items: Array, totalCount: number}>} 아파트 전세/월세 목록
 * 
 * 사용 예시:
 * const data = await getApartmentRentList({
 *   lawdCd: '11680',
 *   dealYmd: '202401',
 *   pageNo: 1,
 *   numOfRows: 20
 * });
 */
export async function getApartmentRentList(params) {
  const queryParams = new URLSearchParams();
  
  if (params.lawdCd) queryParams.append('lawdCd', params.lawdCd);
  if (params.dealYmd) queryParams.append('dealYmd', params.dealYmd);
  if (params.pageNo) queryParams.append('pageNo', params.pageNo);
  if (params.numOfRows) queryParams.append('numOfRows', params.numOfRows);
  if (params.rentType) queryParams.append('rentType', params.rentType);
  
  const queryString = queryParams.toString();
  return get(`/apartment/rent${queryString ? `?${queryString}` : ''}`);
}

/**
 * 특정 아파트 전세/월세 상세 정보 조회
 * 
 * @param {string|number} id - 아파트 전세/월세 ID
 * @returns {Promise<object>} 아파트 전세/월세 상세 정보
 * 
 * 사용 예시:
 * const detail = await getApartmentRentDetail(123);
 */
export async function getApartmentRentDetail(id) {
  return get(`/apartment/rent/${id}`);
}

/**
 * 아파트 전세/월세 검색
 * 
 * @param {object} searchParams - 검색 조건
 * @param {string} [searchParams.keyword] - 검색 키워드 (아파트명, 주소 등)
 * @param {string} [searchParams.lawdCd] - 법정동 코드
 * @param {number} [searchParams.minDeposit] - 최소 보증금
 * @param {number} [searchParams.maxDeposit] - 최대 보증금
 * @param {number} [searchParams.minMonthlyRent] - 최소 월세
 * @param {number} [searchParams.maxMonthlyRent] - 최대 월세
 * @param {string} [searchParams.rentType] - 전세/월세 구분
 * @returns {Promise<{items: Array, totalCount: number}>} 검색 결과
 * 
 * 사용 예시:
 * const results = await searchApartmentRent({
 *   keyword: '반포',
 *   minDeposit: 100000000,
 *   maxDeposit: 500000000,
 *   rentType: '전세'
 * });
 */
export async function searchApartmentRent(searchParams) {
  return post('/apartment/rent/search', searchParams);
}

/**
 * 아파트 전세/월세 필터링 조회
 * 
 * @param {object} filters - 필터 조건
 * @param {string[]} [filters.lawdCds] - 법정동 코드 배열
 * @param {string} [filters.rentType] - 전세/월세 구분
 * @param {number} [filters.depositRange] - 보증금 범위 [min, max]
 * @param {number} [filters.monthlyRentRange] - 월세 범위 [min, max]
 * @param {number} [filters.areaRange] - 면적 범위 [min, max] (제곱미터)
 * @param {number} [filters.floorRange] - 층 범위 [min, max]
 * @returns {Promise<{items: Array, totalCount: number}>} 필터링된 결과
 * 
 * 사용 예시:
 * const filtered = await filterApartmentRent({
 *   lawdCds: ['11680', '11681'],
 *   rentType: '전세',
 *   depositRange: [100000000, 500000000],
 *   areaRange: [30, 100]
 * });
 */
export async function filterApartmentRent(filters) {
  return post('/apartment/rent/filter', filters);
}

// 기본 export
export default {
  getApartmentRentList,
  getApartmentRentDetail,
  searchApartmentRent,
  filterApartmentRent,
};
