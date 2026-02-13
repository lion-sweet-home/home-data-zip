/**
 * 지역(법정동) 관련 API
 * 시도, 구군, 동 목록 및 동 랭킹을 조회합니다.
 */

import { get } from './api';

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
 * 동 랭킹 조회 (매매용)
 * 
 * @param {string} sido - 시도명
 * @param {string} gugun - 구/군명
 * @param {number} [periodMonths=6] - 기간 (개월 수)
 * @returns {Promise<Array>} 동별 거래량 랭킹 [{dong, tradeCount}]
 * 
 * 사용 예시:
 * const dongRank = await getDongRank('서울특별시', '강남구', 6);
 */
export async function getDongRank(sido, gugun, periodMonths = 6) {
  const params = new URLSearchParams();
  params.append('sido', sido);
  params.append('gugun', gugun);
  params.append('periodMonths', periodMonths);
  return get(`/regions/dong/rank?${params.toString()}`);
}

// 기본 export
export default {
  getSidoList,
  getGugunList,
  getDongList,
  getDongRank,
};
