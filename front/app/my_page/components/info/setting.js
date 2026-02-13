'use client';

import { useEffect, useState } from 'react';
import { getLocalJSON, setLocalJSON } from '../_utils';

const LS_KEY = 'myPage:notificationSettings';

export default function SettingCard() {
  const [loaded, setLoaded] = useState(false);
  const [settings, setSettings] = useState({
    announcement: true,
    priceChange: true,
    chatMessage: true,
  });

  useEffect(() => {
    const saved = getLocalJSON(LS_KEY, null);
    if (saved) setSettings((prev) => ({ ...prev, ...saved }));
    setLoaded(true);
  }, []);

  const toggle = (key) => {
    setSettings((prev) => {
      const next = { ...prev, [key]: !prev[key] };
      setLocalJSON(LS_KEY, next);
      // TODO: 알림 설정 서버 연동
      return next;
    });
  };

  return (
    <div className="bg-white border border-gray-200 rounded-2xl p-5 shadow-sm">
      <div className="flex items-center gap-2 mb-4">
        <div className="w-9 h-9 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden>
            <path
              d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 7h18s-3 0-3-7"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
            <path
              d="M13.73 21a2 2 0 0 1-3.46 0"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        </div>
        <h2 className="text-base font-bold text-gray-900">알림 설정</h2>
      </div>

      {!loaded ? (
        <div className="h-24 rounded-xl bg-gray-50 border border-gray-100 animate-pulse" />
      ) : (
        <div className="space-y-3">
          <label className="flex items-center justify-between gap-3 text-sm">
            <span className="text-gray-800">공지사항 알림</span>
            <input
              type="checkbox"
              checked={!!settings.announcement}
              onChange={() => toggle('announcement')}
              className="h-4 w-4"
            />
          </label>

          <label className="flex items-center justify-between gap-3 text-sm">
            <span className="text-gray-800">실거래가 변동 알림</span>
            <input
              type="checkbox"
              checked={!!settings.priceChange}
              onChange={() => toggle('priceChange')}
              className="h-4 w-4"
            />
          </label>

          <label className="flex items-center justify-between gap-3 text-sm">
            <span className="text-gray-800">채팅 메시지 알림</span>
            <input
              type="checkbox"
              checked={!!settings.chatMessage}
              onChange={() => toggle('chatMessage')}
              className="h-4 w-4"
            />
          </label>

          <div className="text-xs text-gray-500 pt-1">
            현재는 로컬 저장소에만 반영됩니다. (TODO: 서버 연동)
          </div>
        </div>
      )}
    </div>
  );
}

