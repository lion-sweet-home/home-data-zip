/**
 * 아파트 매매 관련 API
 * 아파트 매매 거래 정보를 조회합니다.
 */

import { get, post } from './api';

/**
 * 아파트 매매 목록 조회
 * 
 * @param {object} params - 조회 파라미터
 * @param {string} params.lawdCd - 법정동 코드 (5자리)
 * @param {string} params.dealYmd - 거래년월 (YYYYMM 형식)
 * @param {number} params.pageNo - 페이지 번호 (기본값: 1)
 * @param {number} params.numOfRows - 페이지당 항목 수 (기본값: 10)
 * @returns {Promise<{items: Array, totalCount: number}>} 아파트 매매 목록
 * 
 * 사용 예시:
 * const data = await getApartmentSaleList({
 *   lawdCd: '11680',
 *   dealYmd: '202401',
 *   pageNo: 1,
 *   numOfRows: 20
 * });
 */
export async function getApartmentSaleList(params) {
  const queryParams = new URLSearchParams();
  
  if (params.lawdCd) queryParams.append('lawdCd', params.lawdCd);
  if (params.dealYmd) queryParams.append('dealYmd', params.dealYmd);
  if (params.pageNo) queryParams.append('pageNo', params.pageNo);
  if (params.numOfRows) queryParams.append('numOfRows', params.numOfRows);
  
  const queryString = queryParams.toString();
  return get(`/apartment/sale${queryString ? `?${queryString}` : ''}`);
}

/**
 * 특정 아파트 매매 상세 정보 조회
 * 
 * @param {string|number} id - 아파트 매매 ID
 * @returns {Promise<object>} 아파트 매매 상세 정보
 * 
 * 사용 예시:
 * const detail = await getApartmentSaleDetail(123);
 */
export async function getApartmentSaleDetail(id) {
  return get(`/apartment/sale/${id}`);
}

/**
 * 아파트 매매 검색
 * 
 * @param {object} searchParams - 검색 조건
 * @param {string} [searchParams.keyword] - 검색 키워드 (아파트명, 주소 등)
 * @param {string} [searchParams.lawdCd] - 법정동 코드
 * @param {number} [searchParams.minPrice] - 최소 가격
 * @param {number} [searchParams.maxPrice] - 최대 가격
 * @param {number} [searchParams.minArea] - 최소 면적 (제곱미터)
 * @param {number} [searchParams.maxArea] - 최대 면적 (제곱미터)
 * @returns {Promise<{items: Array, totalCount: number}>} 검색 결과
 * 
 * 사용 예시:
 * const results = await searchApartmentSale({
 *   keyword: '반포',
 *   minPrice: 500000000,
 *   maxPrice: 1000000000,
 *   minArea: 30,
 *   maxArea: 100
 * });
 */
export async function searchApartmentSale(searchParams) {
  return post('/apartment/sale/search', searchParams);
}

/**
 * 아파트 매매 필터링 조회
 * 
 * @param {object} filters - 필터 조건
 * @param {string[]} [filters.lawdCds] - 법정동 코드 배열
 * @param {number} [filters.priceRange] - 가격 범위 [min, max]
 * @param {number} [filters.areaRange] - 면적 범위 [min, max] (제곱미터)
 * @param {number} [filters.floorRange] - 층 범위 [min, max]
 * @param {number} [filters.buildYearRange] - 건축년도 범위 [min, max]
 * @returns {Promise<{items: Array, totalCount: number}>} 필터링된 결과
 * 
 * 사용 예시:
 * const filtered = await filterApartmentSale({
 *   lawdCds: ['11680', '11681'],
 *   priceRange: [500000000, 1000000000],
 *   areaRange: [30, 100],
 *   buildYearRange: [2010, 2024]
 * });
 */
export async function filterApartmentSale(filters) {
  return post('/apartment/sale/filter', filters);
}

/**
 * 아파트 매매 가격 통계 조회
 * 
 * @param {object} params - 통계 조회 파라미터
 * @param {string} params.lawdCd - 법정동 코드
 * @param {string} [params.dealYmd] - 거래년월 (YYYYMM 형식, 없으면 전체)
 * @returns {Promise<{avgPrice: number, minPrice: number, maxPrice: number, count: number}>} 가격 통계
 * 
 * 사용 예시:
 * const stats = await getApartmentSaleStats({
 *   lawdCd: '11680',
 *   dealYmd: '202401'
 * });
 */
export async function getApartmentSaleStats(params) {
  const queryParams = new URLSearchParams();
  
  if (params.lawdCd) queryParams.append('lawdCd', params.lawdCd);
  if (params.dealYmd) queryParams.append('dealYmd', params.dealYmd);
  
  const queryString = queryParams.toString();
  return get(`/apartment/sale/stats${queryString ? `?${queryString}` : ''}`);
}

// 기본 export
export default {
  getApartmentSaleList,
  getApartmentSaleDetail,
  searchApartmentSale,
  filterApartmentSale,
  getApartmentSaleStats,
};
