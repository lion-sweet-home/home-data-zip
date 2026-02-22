'use client';

import { useEffect, useMemo, useState, useCallback } from 'react';
import { decodeAccessTokenPayload, getLocalJSON, setLocalJSON } from '../_utils';
import { getMyPageInfo, editMyPage } from '../../../api/user';
import { checkNickname as checkNicknameApi } from '../../../api/auth';

const LS_KEY = 'myPage:userInfoDraft';

export default function UserInfoCard() {
  const [userId, setUserId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [email, setEmail] = useState('');
  const [nickname, setNickname] = useState('');
  const [originalNickname, setOriginalNickname] = useState('');

  const [editing, setEditing] = useState(false);
  const [editNickname, setEditNickname] = useState('');
  const [password, setPassword] = useState('');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const [nicknameChecked, setNicknameChecked] = useState(false);
  const [nicknameAvailable, setNicknameAvailable] = useState(false);
  const [checkingNickname, setCheckingNickname] = useState(false);
  const [nicknameMsg, setNicknameMsg] = useState('');

  useEffect(() => {
    const payload = decodeAccessTokenPayload();
    const draft = getLocalJSON(LS_KEY, null);
    const id = payload?.userId ?? payload?.user_id ?? null;

    if (id) {
      setUserId(id);
      getMyPageInfo(id)
        .then((data) => {
          const em = data.email ?? payload?.email ?? '';
          const nn = data.Nickname ?? data.nickname ?? payload?.nickname ?? '';
          setEmail(em);
          setNickname(nn);
          setOriginalNickname(nn);
        })
        .catch(() => {
          const em = draft?.email ?? payload?.email ?? '';
          const nn = draft?.nickname ?? payload?.nickname ?? '';
          setEmail(em);
          setNickname(nn);
          setOriginalNickname(nn);
        })
        .finally(() => setLoading(false));
    } else {
      const em = draft?.email ?? payload?.email ?? '';
      const nn = draft?.nickname ?? payload?.nickname ?? '';
      setEmail(em);
      setNickname(nn);
      setOriginalNickname(nn);
      setLoading(false);
    }
  }, []);

  const isNicknameChanged = editing && editNickname.trim() !== '' && editNickname.trim() !== originalNickname;

  const startEditing = () => {
    setEditing(true);
    setEditNickname(nickname);
    setPassword('');
    setError('');
    setNicknameChecked(false);
    setNicknameAvailable(false);
    setNicknameMsg('');
  };

  const cancelEditing = () => {
    setEditing(false);
    setEditNickname('');
    setPassword('');
    setError('');
    setNicknameChecked(false);
    setNicknameAvailable(false);
    setNicknameMsg('');
  };

  const handleEditNicknameChange = (e) => {
    setEditNickname(e.target.value);
    setNicknameChecked(false);
    setNicknameAvailable(false);
    setNicknameMsg('');
  };

  const onCheckNickname = useCallback(async () => {
    const trimmed = editNickname.trim();
    if (!trimmed) {
      setNicknameMsg('닉네임을 입력해주세요.');
      return;
    }
    if (trimmed.length < 2 || trimmed.length > 30) {
      setNicknameMsg('닉네임은 2~30자여야 합니다.');
      return;
    }
    if (trimmed === originalNickname) {
      setNicknameChecked(true);
      setNicknameAvailable(true);
      setNicknameMsg('현재 사용 중인 닉네임입니다.');
      return;
    }

    setCheckingNickname(true);
    setNicknameMsg('');
    try {
      const isDuplicate = await checkNicknameApi(trimmed);
      if (isDuplicate) {
        setNicknameAvailable(false);
        setNicknameMsg('이미 사용 중인 닉네임입니다.');
      } else {
        setNicknameAvailable(true);
        setNicknameMsg('사용 가능한 닉네임입니다.');
      }
      setNicknameChecked(true);
    } catch {
      setNicknameMsg('중복 확인에 실패했습니다. 다시 시도해주세요.');
    } finally {
      setCheckingNickname(false);
    }
  }, [editNickname, originalNickname]);

  const canSave = useMemo(() => {
    if (saving || !editNickname.trim() || !password.trim()) return false;
    if (isNicknameChanged && (!nicknameChecked || !nicknameAvailable)) return false;
    return true;
  }, [saving, editNickname, password, isNicknameChanged, nicknameChecked, nicknameAvailable]);

  const onSave = async () => {
    setError('');
    if (!password.trim()) {
      setError('비밀번호 확인이 필요합니다.');
      return;
    }
    if (!userId) {
      setError('로그인 정보를 확인할 수 없습니다. 다시 로그인해주세요.');
      return;
    }
    if (isNicknameChanged && (!nicknameChecked || !nicknameAvailable)) {
      setError('닉네임 중복 확인을 해주세요.');
      return;
    }

    setSaving(true);
    try {
      const result = await editMyPage(userId, editNickname.trim(), password.trim());
      const updatedNickname = result.Nickname ?? result.nickname ?? editNickname.trim();
      setNickname(updatedNickname);
      setOriginalNickname(updatedNickname);
      setLocalJSON(LS_KEY, { email, nickname: updatedNickname });
      alert('정보가 수정되었습니다.');
      setEditing(false);
      setPassword('');
    } catch (err) {
      setError(err?.message ?? '정보 수정에 실패했습니다.');
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
      ) : !editing ? (
        <div className="space-y-3">
          <div>
            <label className="block text-xs font-semibold text-gray-600">이메일</label>
            <div className="mt-1 w-full px-3 py-2 rounded-lg border border-gray-200 bg-gray-50 text-sm text-gray-700">
              {email || '-'}
            </div>
          </div>
          <div>
            <label className="block text-xs font-semibold text-gray-600">닉네임</label>
            <div className="mt-1 w-full px-3 py-2 rounded-lg border border-gray-200 bg-gray-50 text-sm text-gray-700">
              {nickname || '-'}
            </div>
          </div>

          <button
            type="button"
            onClick={startEditing}
            className="w-full mt-2 px-4 py-2.5 rounded-lg text-sm font-semibold bg-blue-600 text-white hover:bg-blue-700"
          >
            정보 수정
          </button>
        </div>
      ) : (
        <div className="space-y-3">
          <div>
            <label className="block text-xs font-semibold text-gray-600">이메일</label>
            <div className="mt-1 w-full px-3 py-2 rounded-lg border border-gray-200 bg-gray-50 text-sm text-gray-700">
              {email || '-'}
            </div>
          </div>
          <div>
            <label className="block text-xs font-semibold text-gray-600">닉네임</label>
            <div className="flex gap-2 mt-1">
              <input
                value={editNickname}
                onChange={handleEditNicknameChange}
                placeholder="변경할 닉네임"
                className="flex-1 px-3 py-2 rounded-lg border border-gray-200 text-sm"
              />
              <button
                type="button"
                onClick={onCheckNickname}
                disabled={checkingNickname || !editNickname.trim()}
                className={`shrink-0 px-3 py-2 rounded-lg text-xs font-semibold ${
                  checkingNickname || !editNickname.trim()
                    ? 'bg-gray-200 text-gray-500 cursor-not-allowed'
                    : 'bg-gray-100 text-gray-700 hover:bg-gray-200 border border-gray-300'
                }`}
              >
                {checkingNickname ? '확인 중…' : '중복 확인'}
              </button>
            </div>
            {nicknameMsg && (
              <div className={`text-xs mt-1 ${nicknameAvailable ? 'text-green-600' : 'text-red-500'}`}>
                {nicknameMsg}
              </div>
            )}
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
              닉네임 변경을 저장하려면 비밀번호 확인이 필요합니다.
            </div>
          </div>

          {error && <div className="text-sm text-red-600">{error}</div>}

          <div className="flex gap-2 mt-2">
            <button
              type="button"
              onClick={cancelEditing}
              className="flex-1 px-4 py-2.5 rounded-lg text-sm font-semibold bg-gray-100 text-gray-700 hover:bg-gray-200 border border-gray-300"
            >
              취소
            </button>
            <button
              type="button"
              onClick={onSave}
              disabled={!canSave}
              className={`flex-1 px-4 py-2.5 rounded-lg text-sm font-semibold ${
                canSave ? 'bg-blue-600 text-white hover:bg-blue-700' : 'bg-gray-200 text-gray-500 cursor-not-allowed'
              }`}
            >
              {saving ? '저장 중…' : '저장'}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

