'use client';

import { useEffect, useState } from 'react';
import { getLocalJSON, setLocalJSON } from '../_utils';
import { getNotificationSetting, updateNotificationSetting } from '../../../api/user';

const LS_KEY = 'myPage:notificationSettings';

export default function SettingCard() {
  const [loaded, setLoaded] = useState(false);
  const [settings, setSettings] = useState({
    announcement: true,
  });

  useEffect(() => {
    const saved = getLocalJSON(LS_KEY, null);
    if (saved) setSettings((prev) => ({ ...prev, announcement: saved?.announcement ?? prev.announcement }));
    setLoaded(true);

    // 서버 알림 수신 설정(단일 boolean)과 announcement 토글을 동기화
    (async () => {
      try {
        if (typeof window === 'undefined') return;
        const accessToken = localStorage.getItem('accessToken');
        if (!accessToken) return;

        const res = await getNotificationSetting();
        const enabled = !!res?.notificationEnabled;
        setSettings((prev) => {
          const next = { ...prev, announcement: enabled };
          setLocalJSON(LS_KEY, next);
          return next;
        });
      } catch {
        // 조회 실패 시엔 로컬값 유지
      }
    })();
  }, []);

  const toggleAnnouncement = async () => {
    const nextEnabled = !settings.announcement;

    // UI는 즉시 반영
    setSettings((prev) => {
      const next = { ...prev, announcement: nextEnabled };
      setLocalJSON(LS_KEY, next);
      return next;
    });

    try {
      await updateNotificationSetting(nextEnabled);
      // 헤더 등에서 SSE on/off를 할 수 있게 이벤트로 브로드캐스트
      if (typeof window !== 'undefined') {
        window.dispatchEvent(
          new CustomEvent('notificationSetting:changed', {
            detail: { notificationEnabled: nextEnabled },
          })
        );
      }
    } catch (e) {
      // 실패하면 롤백
      setSettings((prev) => {
        const rollback = { ...prev, announcement: !nextEnabled };
        setLocalJSON(LS_KEY, rollback);
        return rollback;
      });
      alert(e?.message ?? '알림 설정 변경에 실패했습니다.');
    }
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
              onChange={toggleAnnouncement}
              className="h-4 w-4"
            />
          </label>

          <div className="text-xs text-gray-500 pt-1">
            공지사항 알림은 서버 설정과 연동됩니다.
          </div>
        </div>
      )}
    </div>
  );
}

