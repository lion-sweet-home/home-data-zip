'use client';

import { useEffect, useMemo, useState } from 'react';
import { decodeAccessTokenPayload, getLocalJSON, setLocalJSON } from '../_utils';

const LS_KEY = 'myPage:userInfoDraft';

export default function UserInfoCard() {
  const [loading, setLoading] = useState(true);
  const [email, setEmail] = useState('');
  const [nickname, setNickname] = useState('');
  const [password, setPassword] = useState('');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    const payload = decodeAccessTokenPayload();
    const draft = getLocalJSON(LS_KEY, null);

    setEmail(draft?.email ?? payload?.email ?? '');
    setNickname(draft?.nickname ?? payload?.nickname ?? '');
    setLoading(false);
  }, []);

  const canSave = useMemo(() => {
    return !!email && !!nickname && !saving;
  }, [email, nickname, saving]);

  const onSave = async () => {
    setError('');
    const pw = password.trim();
    if (!pw) {
      setError('정보 수정 시 비밀번호 확인이 필요합니다.');
      return;
    }

    setSaving(true);
    try {
      // TODO: 백엔드 마이페이지 수정 API 연결 (현재는 화면 구현 우선)
      // 비밀번호는 로컬에 저장하지 않음(보안)
      setLocalJSON(LS_KEY, { email, nickname });
      alert('정보 수정 (TODO: 서버 연동 예정)');
      setPassword('');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="bg-white border border-gray-200 rounded-2xl p-5 shadow-sm">
      <div className="flex items-center gap-2 mb-4">
        <div className="w-9 h-9 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden>
            <path
              d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
            <path
              d="M12 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        </div>
        <h2 className="text-base font-bold text-gray-900">회원 정보</h2>
      </div>

      {loading ? (
        <div className="h-32 rounded-xl bg-gray-50 border border-gray-100 animate-pulse" />
      ) : (
        <div className="space-y-3">
          <div>
            <label className="block text-xs font-semibold text-gray-600">이메일</label>
            <input
              value={email}
              readOnly
              className="mt-1 w-full px-3 py-2 rounded-lg border border-gray-200 bg-gray-50 text-sm text-gray-700"
            />
          </div>
          <div>
            <label className="block text-xs font-semibold text-gray-600">닉네임</label>
            <input
              value={nickname}
              onChange={(e) => setNickname(e.target.value)}
              placeholder="닉네임"
              className="mt-1 w-full px-3 py-2 rounded-lg border border-gray-200 text-sm"
            />
          </div>
          <div>
            <label className="block text-xs font-semibold text-gray-600">비밀번호 확인</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="현재 비밀번호"
              autoComplete="current-password"
              className="mt-1 w-full px-3 py-2 rounded-lg border border-gray-200 text-sm"
            />
            <div className="text-xs text-gray-500 mt-1">
              닉네임 변경을 저장하려면 비밀번호 확인이 필요합니다. 비밀번호는 저장되지 않습니다.
            </div>
          </div>

          {error ? <div className="text-sm text-red-600">{error}</div> : null}

          <button
            type="button"
            onClick={onSave}
            disabled={!canSave}
            className={`w-full mt-2 px-4 py-2.5 rounded-lg text-sm font-semibold ${
              canSave ? 'bg-blue-600 text-white hover:bg-blue-700' : 'bg-gray-200 text-gray-500 cursor-not-allowed'
            }`}
          >
            {saving ? '저장 중…' : '정보 수정'}
          </button>
        </div>
      )}
    </div>
  );
}

