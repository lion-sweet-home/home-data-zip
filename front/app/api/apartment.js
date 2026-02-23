/**
 * 아파트 관련 API
 * 아파트 정보 및 주변 시설 정보를 조회합니다.
 */

import { get } from './api';

/**
 * 아파트 기준 가까운 지하철역 top 3 조회
 * 
 * @param {number} apartmentId - 아파트 ID
 * @returns {Promise<Array>} 가까운 지하철역 목록 (최대 3개)
 * 
 * 사용 예시:
 * const subways = await getNearbySubways(123);
 */
export async function getNearbySubways(apartmentId) {
  return get(`/apartments/${apartmentId}/subways`);
}

/**
 * 아파트 기준 가까운 학교 top 3 조회
 *
 * @param {number} apartmentId - 아파트 ID
 * @param {string[]} [schoolLevel] - 학교 레벨 필터 (예: ['초등학교','중학교','고등학교'])
 * @returns {Promise<Array>} 가까운 학교 목록 (최대 3개)
 */
export async function getNearbySchools(apartmentId, schoolLevel) {
  const params = new URLSearchParams();
  if (Array.isArray(schoolLevel) && schoolLevel.length > 0) {
    schoolLevel.forEach((level) => {
      if (level) params.append('schoolLevel', level);
    });
  }
  const qs = params.toString();
  return get(`/apartments/${apartmentId}/schools${qs ? `?${qs}` : ''}`);
}

/**
 * aptId로 지역(시/도, 구/군, 동) 조회
 *
 * @param {number} apartmentId
 * @returns {Promise<{sido: string|null, gugun: string|null, dong: string|null}>}
 */
export async function getApartmentRegion(apartmentId) {
  return get(`/apartments/${apartmentId}/region`);
}

/**
 * 아파트 기준 반경 내 버스 정류장 조회
 * 
 * @param {number} apartmentId - 아파트 ID
 * @param {number} [radiusMeters=500] - 반경 (미터, 기본값: 500)
 * @param {number} [limit=50] - 최대 개수 (기본값: 50)
 * @returns {Promise<{count: number, items: Array}>} 버스 정류장 목록
 * 
 * 사용 예시:
 * const busStations = await getNearbyBusStations(123, 500, 50);
 */
export async function getNearbyBusStations(apartmentId, radiusMeters = 500, limit = 50) {
  return get(`/apartments/${apartmentId}/bus-stations?radiusMeters=${radiusMeters}&limit=${limit}`);
}

/**
 * 아파트 월별 거래량 조회
 * 
 * @param {number} aptId - 아파트 ID
 * @param {number} [period] - 기간 (개월 수, 6, 12, 24, 36, 48)
 * @returns {Promise<Array>} 월별 거래량 데이터
 * 
 * 사용 예시:
 * const monthlyData = await getMonthlyTradeVolume(123, 12);
 */
export async function getMonthlyTradeVolume(aptId, period) {
  const queryParams = new URLSearchParams();
  if (period) queryParams.append('period', period);
  
  const queryString = queryParams.toString();
  return get(`/apartments/month-avg/${aptId}/total-rent${queryString ? `?${queryString}` : ''}`);
}

/**
 * 아파트 월별 거래량 조회 (면적 필터 적용)
 * 
 * @param {number} aptId - 아파트 ID
 * @param {number} areaKey - 면적 키 (areaKey)
 * @param {number} [period] - 기간 (개월 수)
 * @returns {Promise<Array>} 면적 필터 적용 월별 거래량 데이터
 * 
 * 사용 예시:
 * const monthlyData = await getMonthlyTradeVolumeByArea(123, 84, 12);
 */
export async function getMonthlyTradeVolumeByArea(aptId, areaKey, period) {
  const queryParams = new URLSearchParams();
  queryParams.append('areaKey', areaKey);
  if (period) queryParams.append('period', period);
  
  return get(`/apartments/month-avg/${aptId}/total-rent/area?${queryParams.toString()}`);
}

/**
 * 아파트 평수 목록 조회
 * 
 * @param {number} aptId - 아파트 ID
 * @returns {Promise<object>} 평수 목록 정보
 * 
 * 사용 예시:
 * const areaTypes = await getAptAreaTypes(123);
 */
export async function getAptAreaTypes(aptId) {
  return get(`/apartments/month-avg/${aptId}/detail/area-exclusive`);
}

/**
 * 면적별 최신 평균 거래가 조회 (6개월)
 * 
 * @param {number} aptId - 아파트 ID
 * @param {number} areaKey - 면적 키 (areaKey)
 * @returns {Promise<object>} 최신 평균 거래가
 * 
 * 사용 예시:
 * const recentAvg = await getRecentAreaAvg(123, 84);
 */
export async function getRecentAreaAvg(aptId, areaKey) {
  const params = new URLSearchParams();
  params.append('areaKey', areaKey);
  return get(`/apartments/month-avg/${aptId}/detail/area-exclusive/avg/recent?${params.toString()}`);
}

/**
 * 평수별, 월별 평균값 데이터 조회
 * 
 * @param {number} aptId - 아파트 ID
 * @param {number} areaKey - 면적 키 (areaKey)
 * @param {number} [period] - 기간 (개월 수)
 * @returns {Promise<Array>} 평수별, 월별 평균값 데이터
 * 
 * 사용 예시:
 * const areaAvg = await getAreaTypeAvg(123, 84, 12);
 */
export async function getAreaTypeAvg(aptId, areaKey, period) {
  const params = new URLSearchParams();
  params.append('areaKey', areaKey);
  if (period) params.append('period', period);
  return get(`/apartments/month-avg/${aptId}/detail/area-excluesive/avg?${params.toString()}`);
}

/**
 * 전세 거래량 Top 3 조회 (등락률 기준)
 * 
 * @returns {Promise<Array>} 전세 거래량 Top 3 지역 목록
 * 
 * 사용 예시:
 * const top3Jeonse = await getJeonseTop3();
 */
export async function getJeonseTop3() {
  return get('/apartments/month-avg/jeonse');
}

/**
 * 월세 거래량 Top 3 조회 (등락률 기준)
 * 
 * @returns {Promise<Array>} 월세 거래량 Top 3 지역 목록
 * 
 * 사용 예시:
 * const top3Wolse = await getWolseTop3();
 */
export async function getWolseTop3() {
  return get('/apartments/month-avg/wolse');
}

/**
 * 매매 거래량 Top 3 조회 (전월 거래량 기준)
 *
 * @returns {Promise<Array>} 매매 거래량 Top 3 목록
 */
export async function getSaleTop3() {
  return get('/apartments/month-avg/sale');
}

/**
 * 아파트명(키워드) + 지역 필터 검색
 * ApartmentController: GET /api/apartments/search?keyword=...&sido=...&gugun=...&dong=...
 *
 * @param {string} keyword
 * @param {string} [sido] - 시/도 (선택)
 * @param {string} [gugun] - 구/군 (선택)
 * @param {string} [dong] - 동 (선택)
 * @returns {Promise<Array|null>} AptSummaryResponse[]
 */
export async function searchApartmentsByName(keyword, sido, gugun, dong) {
  const params = new URLSearchParams();
  params.append('keyword', keyword);
  if (sido) params.append('sido', sido);
  if (gugun) params.append('gugun', gugun);
  if (dong) params.append('dong', dong);
  return get(`/apartments/search?${params.toString()}`);
}

/**
 * 아파트 최근 거래내역 조회 (최대 5건)
 * 
 * @param {number} aptId - 아파트 ID
 * @returns {Promise<Array>} 최근 거래내역 목록 (최대 5건)
 * 
 * 사용 예시:
 * const recentTrades = await getRecentTrades(123);
 */
export async function getRecentTrades(aptId) {
  return get(`/rent/${aptId}`);
}

/**
 * 전세 개수 조회 (구 단위)
 * 
 * @param {string} si - 시도명
 * @param {string} gu - 구/군명
 * @param {number} period - 기간 (개월 수)
 * @returns {Promise<Array>} 동별 전세 거래량 [{sido, gugun, dong, count}]
 * 
 * 사용 예시:
 * const jeonseCount = await getJeonseCount('서울특별시', '강남구', 6);
 */
export async function getJeonseCount(si, gu, period) {
  const params = new URLSearchParams();
  params.append('si', si);
  params.append('gu', gu);
  params.append('period', period);
  return get(`/apartments/month-avg/jeonse-count?${params.toString()}`);
}

/**
 * 월세 개수 조회 (구 단위)
 * 
 * @param {string} si - 시도명
 * @param {string} gu - 구/군명
 * @param {number} period - 기간 (개월 수)
 * @returns {Promise<Array>} 동별 월세 거래량 [{sido, gugun, count}]
 * 
 * 사용 예시:
 * const wolseCount = await getWolseCount('서울특별시', '강남구', 6);
 */
export async function getWolseCount(si, gu, period) {
  const params = new URLSearchParams();
  params.append('si', si);
  params.append('gu', gu);
  params.append('period', period);
  return get(`/apartments/month-avg/wolse-count?${params.toString()}`);
}


// 기본 export
export default {
  getNearbySubways,
  getNearbySchools,
  getApartmentRegion,
  getNearbyBusStations,
  getMonthlyTradeVolume,
  getMonthlyTradeVolumeByArea,
  getRecentTrades,
  getJeonseCount,
  getWolseCount,
  getAptAreaTypes,
  getRecentAreaAvg,
  getAreaTypeAvg,
  getJeonseTop3,
  getWolseTop3,
  getSaleTop3,
  searchApartmentsByName,
};
