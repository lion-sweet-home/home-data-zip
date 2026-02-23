/**
 * AI 챗봇 API (AiMessageController 연동)
 */

import { get, post } from './api';

/**
 * 질문 전송 및 답변 받기
 * @param {string} sessionId - 세션 ID (동일 세션일수록 대화 맥락 유지)
 * @param {string} content - 사용자 질문
 * @returns {Promise<string>} AI 답변 텍스트
 */
export async function askAi(sessionId, content) {
  return post('/ai-chat/ask', { sessionId, content });
}

/**
 * 세션 목록 조회
 * @returns {Promise<string[]>}
 */
export async function getAiSessionIds() {
  return get('/ai-chat/sessions');
}

/**
 * 특정 세션 대화 내역 조회
 * @param {string} sessionId
 * @returns {Promise<Array<{ id: number, role: string, content: string, sessionId: string }>>}
 */
export async function getAiSessionMessages(sessionId) {
  return get(`/ai-chat/sessions/${encodeURIComponent(sessionId)}`);
}
