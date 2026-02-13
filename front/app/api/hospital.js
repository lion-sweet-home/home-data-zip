/**
 * 병원 관련 API
 * 병원 정보를 조회합니다.
 */

import { get } from './api';


/**
 * 동 기준 병원 개수 조회
 * 
 * @param {object} params - 조회 파라미터
 * @param {string} params.sido - 시도명
 * @param {string} params.gugun - 구군명
 * @param {string} params.dong - 동명
 * @returns {Promise<number>} 병원 개수
 * 
 * 사용 예시:
 * const count = await getHospitalCount({
 *   sido: '서울특별시',
 *   gugun: '강남구',
 *   dong: '역삼동'
 * });
 */
export async function getHospitalCount(params) {
  const queryParams = new URLSearchParams();
  queryParams.append('sido', params.sido);
  queryParams.append('gugun', params.gugun);
  queryParams.append('dong', params.dong);
  
  return get(`/hospitals/count?${queryParams.toString()}`);
}

/**
 * 동 기준 병원 종류별 개수 조회
 * 
 * @param {object} params - 조회 파라미터
 * @param {string} params.sido - 시도명
 * @param {string} params.gugun - 구군명
 * @param {string} params.dong - 동명
 * @returns {Promise<object>} 병원 종류별 통계
 * 
 * 사용 예시:
 * const stats = await getHospitalStats({
 *   sido: '서울특별시',
 *   gugun: '강남구',
 *   dong: '역삼동'
 * });
 */
export async function getHospitalStats(params) {
  const queryParams = new URLSearchParams();
  queryParams.append('sido', params.sido);
  queryParams.append('gugun', params.gugun);
  queryParams.append('dong', params.dong);
  
  return get(`/hospitals/stats?${queryParams.toString()}`);
}

/**
 * 동 기준 병원 목록 조회 (마커용)
 * 
 * @param {object} params - 조회 파라미터
 * @param {string} params.sido - 시도명
 * @param {string} params.gugun - 구군명
 * @param {string} params.dong - 동명
 * @returns {Promise<Array>} 병원 목록
 * 
 * 사용 예시:
 * const hospitals = await getHospitalListByDong({
 *   sido: '서울특별시',
 *   gugun: '강남구',
 *   dong: '역삼동'
 * });
 */
export async function getHospitalListByDong(params) {
  const queryParams = new URLSearchParams();
  queryParams.append('sido', params.sido);
  queryParams.append('gugun', params.gugun);
  queryParams.append('dong', params.dong);
  
  return get(`/hospitals/list?${queryParams.toString()}`);
}

// 기본 export
export default {
  getHospitalCount,
  getHospitalStats,
  getHospitalListByDong,
};
