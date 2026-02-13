'use client';

import { useMemo, useState } from 'react';

export default function PasswordCard() {
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [newPasswordConfirm, setNewPasswordConfirm] = useState('');
  const [saving, setSaving] = useState(false);

  const error = useMemo(() => {
    if (!newPassword && !newPasswordConfirm) return '';
    if (newPassword.length > 0 && newPassword.length < 8) return '새 비밀번호는 8자 이상이어야 합니다.';
    if (newPasswordConfirm && newPassword !== newPasswordConfirm) return '새 비밀번호가 일치하지 않습니다.';
    return '';
  }, [newPassword, newPasswordConfirm]);

  const canSave = useMemo(() => {
    return (
      !saving &&
      !!currentPassword &&
      !!newPassword &&
      !!newPasswordConfirm &&
      !error
    );
  }, [saving, currentPassword, newPassword, newPasswordConfirm, error]);

  const onSubmit = async (e) => {
    e.preventDefault();
    if (!canSave) return;

    setSaving(true);
    try {
      // TODO: 비밀번호 변경 API 연결
      alert('비밀번호 변경 (TODO: 서버 연동 예정)');
      setCurrentPassword('');
      setNewPassword('');
      setNewPasswordConfirm('');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="bg-white border border-gray-200 rounded-2xl p-5 shadow-sm">
      <div className="flex items-center gap-2 mb-4">
        <div className="w-9 h-9 rounded-xl bg-gray-50 text-gray-700 flex items-center justify-center">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden>
            <path
              d="M7 11V7a5 5 0 0 1 10 0v4"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
            <path
              d="M5 11h14v10H5V11Z"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        </div>
        <h2 className="text-base font-bold text-gray-900">비밀번호 변경</h2>
      </div>

      <form onSubmit={onSubmit} className="space-y-3">
        <div>
          <label className="block text-xs font-semibold text-gray-600">현재 비밀번호</label>
          <input
            type="password"
            value={currentPassword}
            onChange={(e) => setCurrentPassword(e.target.value)}
            className="mt-1 w-full px-3 py-2 rounded-lg border border-gray-200 text-sm"
            placeholder="현재 비밀번호"
            required
          />
        </div>
        <div>
          <label className="block text-xs font-semibold text-gray-600">새 비밀번호</label>
          <input
            type="password"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            className="mt-1 w-full px-3 py-2 rounded-lg border border-gray-200 text-sm"
            placeholder="새 비밀번호"
            required
          />
        </div>
        <div>
          <label className="block text-xs font-semibold text-gray-600">새 비밀번호 확인</label>
          <input
            type="password"
            value={newPasswordConfirm}
            onChange={(e) => setNewPasswordConfirm(e.target.value)}
            className="mt-1 w-full px-3 py-2 rounded-lg border border-gray-200 text-sm"
            placeholder="새 비밀번호 확인"
            required
          />
        </div>

        <div className="text-xs text-gray-500 flex items-start gap-2 pt-1">
          <span aria-hidden>🔒</span>
          <span>비밀번호 변경 시 이메일 인증이 필요합니다 (TODO)</span>
        </div>

        {error ? <div className="text-sm text-red-600">{error}</div> : null}

        <button
          type="submit"
          disabled={!canSave}
          className={`w-full mt-2 px-4 py-2.5 rounded-lg text-sm font-semibold ${
            canSave ? 'bg-gray-900 text-white hover:bg-black' : 'bg-gray-200 text-gray-500 cursor-not-allowed'
          }`}
        >
          {saving ? '변경 중…' : '비밀번호 변경'}
        </button>
      </form>
    </div>
  );
}

