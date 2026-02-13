/**
 * 학교 관련 API
 * 학교 정보를 조회합니다.
 */

import { get } from './api';

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


// 기본 export
export default {
  searchSchoolsByRegion,
  getApartmentsNearSchool,
};
