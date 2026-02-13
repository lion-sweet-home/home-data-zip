/**
 * 매물 관련 API
 * 매물 등록, 조회, 검색 기능을 제공합니다.
 */

import { get, post } from './api';

/**
 * 매물 등록
 * 
 * @param {object} request - 매물 등록 정보
 * @param {number} request.apartmentId - 아파트 ID
 * @param {string} request.tradeType - 거래 유형 ('SALE' | 'RENT')
 * @param {string} [request.rentType] - 전월세 유형 ('JEONSE' | 'WOLSE', tradeType이 'RENT'일 때 필수)
 * @param {number} request.price - 가격 (매매가 또는 보증금)
 * @param {number} [request.monthlyRent] - 월세 (rentType이 'WOLSE'일 때 필수)
 * @param {number} request.area - 면적 (제곱미터)
 * @param {number} request.floor - 층수
 * @param {string} [request.description] - 매물 설명
 * @returns {Promise<{listingId: number, status: string}>} 등록된 매물 정보
 * 
 * 사용 예시:
 * const result = await createListing({
 *   apartmentId: 123,
 *   tradeType: 'SALE',
 *   price: 500000000,
 *   area: 84.5,
 *   floor: 10
 * });
 */
export async function createListing(request) {
  return post('/listings/create', request);
}

/**
 * 내 매물 조회
 * 
 * @param {string} [status] - 매물 상태 필터 (선택사항)
 * @returns {Promise<Array>} 내 매물 목록
 * 
 * 사용 예시:
 * const myListings = await getMyListings();
 * const activeListings = await getMyListings('ACTIVE');
 */
export async function getMyListings(status) {
  const params = new URLSearchParams();
  if (status) params.append('status', status);
  
  const queryString = params.toString();
  return get(`/listings/me${queryString ? `?${queryString}` : ''}`);
}

/**
 * 전체 매물 검색
 * 
 * @param {object} params - 검색 파라미터
 * @param {number} [params.regionId] - 지역 ID
 * @param {number} [params.apartmentId] - 아파트 ID
 * @param {string} [params.tradeType] - 거래 유형 ('SALE' | 'RENT')
 * @param {string} [params.rentType] - 전월세 유형 ('JEONSE' | 'WOLSE')
 * @param {number} [params.limit=50] - 최대 개수
 * @returns {Promise<Array>} 매물 목록
 * 
 * 사용 예시:
 * const listings = await searchListings({
 *   regionId: 123,
 *   tradeType: 'SALE',
 *   limit: 20
 * });
 */
export async function searchListings(params = {}) {
  const queryParams = new URLSearchParams();
  
  if (params.regionId) queryParams.append('regionId', params.regionId);
  if (params.apartmentId) queryParams.append('apartmentId', params.apartmentId);
  if (params.tradeType) queryParams.append('tradeType', params.tradeType);
  if (params.rentType) queryParams.append('rentType', params.rentType);
  if (params.limit) queryParams.append('limit', params.limit);
  
  const queryString = queryParams.toString();
  return get(`/listings${queryString ? `?${queryString}` : ''}`);
}

/**
 * 매매 매물만 조회
 * 
 * @param {object} params - 검색 파라미터
 * @param {number} [params.regionId] - 지역 ID
 * @param {number} [params.apartmentId] - 아파트 ID
 * @param {number} [params.limit=50] - 최대 개수
 * @returns {Promise<Array>} 매매 매물 목록
 * 
 * 사용 예시:
 * const saleListings = await getSaleListings({
 *   regionId: 123,
 *   limit: 20
 * });
 */
export async function getSaleListings(params = {}) {
  const queryParams = new URLSearchParams();
  
  if (params.regionId) queryParams.append('regionId', params.regionId);
  if (params.apartmentId) queryParams.append('apartmentId', params.apartmentId);
  if (params.limit) queryParams.append('limit', params.limit);
  
  const queryString = queryParams.toString();
  return get(`/listings/sale${queryString ? `?${queryString}` : ''}`);
}

/**
 * 전월세 매물만 조회
 * 
 * @param {object} params - 검색 파라미터
 * @param {number} [params.regionId] - 지역 ID
 * @param {number} [params.apartmentId] - 아파트 ID
 * @param {string} [params.rentType] - 전월세 유형 ('JEONSE' | 'WOLSE')
 * @param {number} [params.limit=50] - 최대 개수
 * @returns {Promise<Array>} 전월세 매물 목록
 * 
 * 사용 예시:
 * const rentListings = await getRentListings({
 *   regionId: 123,
 *   rentType: 'JEONSE',
 *   limit: 20
 * });
 */
export async function getRentListings(params = {}) {
  const queryParams = new URLSearchParams();
  
  if (params.regionId) queryParams.append('regionId', params.regionId);
  if (params.apartmentId) queryParams.append('apartmentId', params.apartmentId);
  if (params.rentType) queryParams.append('rentType', params.rentType);
  if (params.limit) queryParams.append('limit', params.limit);
  
  const queryString = queryParams.toString();
  return get(`/listings/rent${queryString ? `?${queryString}` : ''}`);
}

// 기본 export
export default {
  createListing,
  getMyListings,
  searchListings,
  getSaleListings,
  getRentListings,
};
