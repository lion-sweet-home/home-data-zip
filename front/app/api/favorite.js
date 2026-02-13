/**
 * 관심 매물 관련 API
 * 관심 매물 등록, 해제, 조회 기능을 제공합니다.
 */

import { get, post, del } from './api';

/**
 * 관심 매물 등록
 * 
 * @param {number} listingId - 매물 ID
 * @returns {Promise<void>}
 * 
 * 사용 예시:
 * await addFavorite(123);
 */
export async function addFavorite(listingId) {
  return post(`/users/me/favorites/${listingId}`);
}

/**
 * 관심 매물 해제
 * 
 * @param {number} listingId - 매물 ID
 * @returns {Promise<void>}
 * 
 * 사용 예시:
 * await removeFavorite(123);
 */
export async function removeFavorite(listingId) {
  return del(`/users/me/favorites/${listingId}`);
}

/**
 * 내 관심 매물 목록 조회
 * 관심 등록 시점 최신순으로 정렬됩니다.
 * 
 * @returns {Promise<Array>} 관심 매물 목록
 * 
 * 사용 예시:
 * const favorites = await getMyFavorites();
 */
export async function getMyFavorites() {
  return get('/users/me/favorites');
}

// 기본 export
export default {
  addFavorite,
  removeFavorite,
  getMyFavorites,
};
