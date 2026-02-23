/**
 * 사용자 행동 로그 API
 * 평수 클릭 등 추천/분석용 로그를 전송합니다.
 */

import { post } from './api';

/**
 * 아파트 상세 페이지에서 평수(면적) 선택 시 클릭 로그 전송
 * @param {object} body
 * @param {number} body.aptId - 아파트 ID
 * @param {number} body.area - 면적 (㎡)
 * @param {number|null} [body.price] - 매매가 (매매일 때)
 * @param {number|null} [body.monthlyRent] - 보증금/월세 (전월세일 때, 전세는 보증금, 월세는 월세료)
 * @param {boolean} body.isRent - 전월세 여부 (true: 전월세, false: 매매)
 * @returns {Promise<void>}
 */
export async function logPyeongClick(body) {
  await post('/logs/click', body);
}
