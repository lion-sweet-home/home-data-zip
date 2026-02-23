/**
 * 매물 관련 API
 * 매물 등록, 조회, 검색 기능을 제공합니다.
 */

import { get, post, del, upload } from './api';

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
 * @param {string} [params.sido] - 시/도
 * @param {string} [params.gugun] - 구/군
 * @param {string} [params.dong] - 동
 * @param {string} [params.apartmentName] - 아파트명
 * @param {string} [params.tradeType] - 거래 유형 ('SALE' | 'RENT')
 * @param {string} [params.rentType] - 전월세 유형 ('CHARTER' | 'MONTHLY')
 * @param {number} [params.limit=50] - 최대 개수
 * @returns {Promise<Array>} 매물 목록
 */
export async function searchListings(params = {}) {
  const queryParams = new URLSearchParams();
  
  if (params.sido) queryParams.append('sido', params.sido);
  if (params.gugun) queryParams.append('gugun', params.gugun);
  if (params.dong) queryParams.append('dong', params.dong);
  if (params.apartmentName) queryParams.append('apartmentName', params.apartmentName);
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
 * @param {string} [params.sido] - 시/도
 * @param {string} [params.gugun] - 구/군
 * @param {string} [params.dong] - 동
 * @param {string} [params.apartmentName] - 아파트명
 * @param {number} [params.limit=50] - 최대 개수
 * @returns {Promise<Array>} 매매 매물 목록
 */
export async function getSaleListings(params = {}) {
  const queryParams = new URLSearchParams();
  
  if (params.sido) queryParams.append('sido', params.sido);
  if (params.gugun) queryParams.append('gugun', params.gugun);
  if (params.dong) queryParams.append('dong', params.dong);
  if (params.apartmentName) queryParams.append('apartmentName', params.apartmentName);
  if (params.limit) queryParams.append('limit', params.limit);
  
  const queryString = queryParams.toString();
  return get(`/listings/sale${queryString ? `?${queryString}` : ''}`);
}

/**
 * 전월세 매물만 조회
 * 
 * @param {object} params - 검색 파라미터
 * @param {string} [params.sido] - 시/도
 * @param {string} [params.gugun] - 구/군
 * @param {string} [params.dong] - 동
 * @param {string} [params.apartmentName] - 아파트명
 * @param {string} [params.rentType] - 전월세 유형 ('CHARTER' | 'MONTHLY')
 * @param {number} [params.limit=50] - 최대 개수
 * @returns {Promise<Array>} 전월세 매물 목록
 */
export async function getRentListings(params = {}) {
  const queryParams = new URLSearchParams();
  
  if (params.sido) queryParams.append('sido', params.sido);
  if (params.gugun) queryParams.append('gugun', params.gugun);
  if (params.dong) queryParams.append('dong', params.dong);
  if (params.apartmentName) queryParams.append('apartmentName', params.apartmentName);
  if (params.rentType) queryParams.append('rentType', params.rentType);
  if (params.limit) queryParams.append('limit', params.limit);
  
  const queryString = queryParams.toString();
  return get(`/listings/rent${queryString ? `?${queryString}` : ''}`);
}

/**
 * 매물 상세 조회 (이미지 포함)
 * @param {number} listingId - 매물 ID
 * @returns {Promise<{id: number, title: string, images: Array}>}
 */
export async function getListingDetail(listingId) {
  return get(`/listings/${listingId}`);
}

/**
 * 매물 삭제 (본인 매물만)
 * @param {number} listingId - 매물 ID
 * @returns {Promise<{listingId: number, deleted: boolean}>}
 */
export async function deleteListing(listingId) {
  return del(`/listings/${listingId}`);
}

/**
 * 매물 이미지 임시 업로드 (등록 전 업로드, 등록 시 imageTempKeys로 전달)
 * @param {File} file
 * @returns {Promise<{key: string, url: string}>}
 */
export async function uploadListingImageTemp(file) {
  const formData = new FormData();
  formData.append('file', file);
  return upload('/listings/images/temp', formData);
}

// 기본 export
export default {
  createListing,
  getMyListings,
  searchListings,
  getSaleListings,
  getRentListings,
  getListingDetail,
  deleteListing,
  uploadListingImageTemp,
};
