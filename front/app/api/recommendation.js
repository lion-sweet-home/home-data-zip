/**
 * 맞춤 추천 API
 * UserPreference 기반 아파트 추천 (백엔드 UserSearchLogRepository, ApartmentRecommendationRepository 연동)
 *
 * 동작:
 * - 비로그인 또는 UserLog가 없으면 빈 배열 [] 반환.
 * - 로그인 + 최근 30일 평수 클릭 로그가 쌓인 경우에만 추천 목록 반환.
 * - UI에서는 list.length > 0 일 때만 추천 섹션을 렌더링할 것.
 */

import { get } from './api';

/**
 * 추천 아파트 목록 조회
 * - 비로그인 or UserLog 없음 → [] (추천 섹션 숨김)
 * - 로그인 + UserLog 있음 → 맞춤 추천 목록
 *
 * @returns {Promise<Array<{
 *   id: number,
 *   aptName: string,
 *   roadAddress: string,
 *   jibunAddress: string,
 *   buildYear: number,
 *   latitude: number,
 *   longitude: number,
 *   regionName: string,
 *   recommendArea: number,
 *   recommendPrice: number
 * }>>}
 */
export async function getRecommendations() {
  return get('/v1/recommendations');
}
