'use client';

import { useState, useRef, useCallback } from 'react';
import { askAi } from '../api/aiChat';

function generateSessionId() {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID();
  }
  return `session-${Date.now()}-${Math.random().toString(36).slice(2, 11)}`;
}

export default function AiChatSection() {
  const [sessionId] = useState(() => generateSessionId());
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [messages, setMessages] = useState([]);
  const [open, setOpen] = useState(false);
  const inputRef = useRef(null);
  const listRef = useRef(null);

  const handleSubmit = useCallback(
    async (e) => {
      e.preventDefault();
      const text = (input || '').trim();
      if (!text || loading) return;

      setError(null);
      setLoading(true);
      setInput('');
      setMessages((prev) => [...prev, { role: 'user', content: text }]);

      try {
        const answer = await askAi(sessionId, text);
        setMessages((prev) => [...prev, { role: 'assistant', content: answer }]);
        if (listRef.current) listRef.current.scrollTop = listRef.current.scrollHeight;
      } catch (err) {
        const isAuthError = err?.status === 401 || /인증|로그인|unauthorized/i.test(err?.message || '');
        const msg = isAuthError
          ? '로그인 후 이용 가능합니다.'
          : err?.message || '전송에 실패했습니다.';
        setError(msg);
        setMessages((prev) => prev.slice(0, -1));
        setInput(text);
        if (inputRef.current) inputRef.current.focus();
      } finally {
        setLoading(false);
      }
    },
    [input, loading, sessionId]
  );

  return (
    <>
      {/* 열기/닫기 탭 */}
      <button
        type="button"
        onClick={() => setOpen((o) => !o)}
        className="fixed bottom-6 right-6 z-50 flex items-center gap-2 rounded-full bg-blue-600 px-4 py-2.5 text-sm font-medium text-white shadow-lg hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
        aria-label={open ? '챗봇 닫기' : 'AI 챗봇 열기'}
      >
        <span className="text-base">💬</span>
        <span>AI 챗봇</span>
      </button>

      {/* 사이드 패널 */}
      {open && (
        <div className="fixed bottom-20 right-6 z-50 w-[300px] rounded-2xl border border-gray-200 bg-white shadow-xl">
          <div className="flex items-center justify-between border-b border-gray-200 px-3 py-2 bg-gray-50 rounded-t-2xl">
            <span className="text-sm font-semibold text-gray-900">AI 부동산 챗봇</span>
            <button
              type="button"
              onClick={() => setOpen(false)}
              className="rounded p-1 text-gray-500 hover:bg-gray-200 hover:text-gray-700"
              aria-label="닫기"
            >
              ✕
            </button>
          </div>

          <div ref={listRef} className="h-48 overflow-y-auto p-2 space-y-2 bg-gray-50/50">
            {messages.length === 0 && (
              <p className="text-xs text-gray-500 px-2 py-4 text-center">
                매물·지역 질문을 입력하세요.
              </p>
            )}
            {messages.map((m, idx) => (
              <div
                key={`msg-${idx}`}
                className={`flex ${m.role === 'user' ? 'justify-end' : 'justify-start'}`}
              >
                <div
                  className={`max-w-[90%] rounded-lg px-2.5 py-1.5 text-xs ${
                    m.role === 'user'
                      ? 'bg-blue-600 text-white'
                      : 'bg-white text-gray-800 border border-gray-200'
                  }`}
                >
                  {m.content}
                </div>
              </div>
            ))}
          </div>

          {error && (
            <div className="px-2 py-1 text-xs text-red-600 bg-red-50">{error}</div>
          )}

          <form onSubmit={handleSubmit} className="p-2 border-t border-gray-200">
            <div className="flex gap-1.5">
              <input
                ref={inputRef}
                type="text"
                value={input}
                onChange={(e) => setInput(e.target.value)}
                placeholder="질문 입력..."
                className="flex-1 min-w-0 rounded-lg border border-gray-300 px-2.5 py-2 text-xs placeholder:text-gray-400 focus:outline-none focus:ring-1 focus:ring-blue-500"
                disabled={loading}
              />
              <button
                type="submit"
                disabled={loading || !input.trim()}
                className="rounded-lg bg-blue-600 px-3 py-2 text-xs font-medium text-white hover:bg-blue-700 disabled:opacity-50 shrink-0"
              >
                {loading ? '…' : '전송'}
              </button>
            </div>
          </form>
        </div>
      )}
    </>
  );
}
