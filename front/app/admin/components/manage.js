'use client';

import { useEffect, useMemo, useState } from 'react';
import { deleteAdminUser, getAdminUsersList } from '../../api/admin';

function safeDate(d) {
  if (!d) return null;
  const date = new Date(d);
  return Number.isNaN(date.getTime()) ? null : date;
}

function formatDate(d) {
  const date = safeDate(d);
  if (!date) return '-';
  return date.toISOString().slice(0, 10);
}

function normalizeRoles(roles) {
  if (!roles) return [];
  if (Array.isArray(roles)) {
    if (roles.length > 0 && typeof roles[0] === 'string') return roles;
    if (roles.length > 0 && roles[0]?.roleType) return roles.map((r) => r.roleType);
    if (roles.length > 0 && roles[0]?.role?.roleType) return roles.map((r) => r.role.roleType);
  }
  return [];
}

function getUserId(row) {
  return row?.userId ?? row?.id ?? row?.memberId ?? null;
}

function RoleBadge({ isPremium }) {
  return isPremium ? (
    <span className="px-2 py-1 rounded-full text-xs font-semibold bg-purple-50 text-purple-600 border border-purple-100">
      프리미엄
    </span>
  ) : (
    <span className="px-2 py-1 rounded-full text-xs font-semibold bg-gray-50 text-gray-600 border border-gray-100">
      무료
    </span>
  );
}

function UserRow({ user, onDelete }) {
  const roles = normalizeRoles(user?.roles);
  const isPremium = roles.includes('SELLER') || roles.includes('PREMIUM');
  const userId = getUserId(user);

  return (
    <div className="py-4 border-b border-gray-100 last:border-b-0">
      <div className="flex items-start justify-between gap-3">
        <div>
          <div className="flex items-center gap-2">
            <div className="text-sm font-bold text-gray-900">{user?.nickname ?? '—'}</div>
            <RoleBadge isPremium={isPremium} />
          </div>
          <div className="text-sm text-gray-600 mt-1">{user?.email ?? '—'}</div>
          <div className="text-xs text-gray-500 mt-2">가입일: {formatDate(user?.createdAt)}</div>
        </div>

        <div className="flex items-center gap-2">
          <button className="px-3 py-1.5 rounded-lg bg-gray-100 text-gray-600 text-xs font-semibold cursor-not-allowed" disabled>
            수정
          </button>
          <button
            onClick={() => onDelete(userId)}
            className={`px-3 py-1.5 rounded-lg text-xs font-semibold ${
              userId
                ? 'bg-red-50 text-red-600 hover:bg-red-100'
                : 'bg-gray-100 text-gray-400 cursor-not-allowed'
            }`}
            disabled={!userId}
            title={!userId ? '목록 응답에 userId가 없어 삭제할 수 없습니다(백엔드 DTO 확인 필요).' : '삭제'}
          >
            삭제
          </button>
        </div>
      </div>
    </div>
  );
}

export default function ManageUsers() {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(6);
  const [showAll, setShowAll] = useState(false);
  const [keyword, setKeyword] = useState('');
  const [data, setData] = useState({
    content: [],
    totalElements: 0,
    totalPages: 0,
    number: 0,
    size: 0,
  });

  async function load() {
    setLoading(true);
    setError(null);
    try {
      const res = await getAdminUsersList({ page, size });
      // Spring Page 응답: {content, totalElements, totalPages, number, size, ...}
      setData({
        content: Array.isArray(res?.content) ? res.content : [],
        totalElements: Number(res?.totalElements ?? 0),
        totalPages: Number(res?.totalPages ?? 0),
        number: Number(res?.number ?? page),
        size: Number(res?.size ?? size),
      });
    } catch (e) {
      setError(e?.message ?? '회원 목록을 불러오지 못했습니다.');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, size]);

  const filtered = useMemo(() => {
    const k = keyword.trim().toLowerCase();
    if (!k) return data.content;
    return data.content.filter((u) => {
      const nick = String(u?.nickname ?? '').toLowerCase();
      const email = String(u?.email ?? '').toLowerCase();
      return nick.includes(k) || email.includes(k);
    });
  }, [data.content, keyword]);

  const totalLabel = useMemo(() => {
    const n = Number.isFinite(data.totalElements) ? data.totalElements : 0;
    return n.toLocaleString('ko-KR');
  }, [data.totalElements]);

  const visibleUsers = useMemo(() => {
    return showAll ? filtered : filtered.slice(0, 3);
  }, [filtered, showAll]);

  const handleDelete = async (userId) => {
    if (!userId) return;
    const ok = window.confirm('정말 삭제하시겠습니까?');
    if (!ok) return;

    try {
      await deleteAdminUser(userId);
      await load();
    } catch (e) {
      alert(e?.message ?? '삭제에 실패했습니다.');
    }
  };

  return (
    <div className="bg-white border border-gray-200 rounded-2xl p-5 shadow-sm">
      <div className="flex items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <span className="text-base font-bold text-gray-900">회원 관리</span>
          <span className="text-xs text-gray-500">총 {totalLabel}명</span>
        </div>
        <button
          onClick={() => {
            setPage(0);
            setShowAll((v) => !v);
            if (!showAll && size < 20) setSize(20);
            if (showAll) setSize(6);
          }}
          className="px-3 py-2 rounded-lg bg-blue-600 text-white text-sm font-semibold hover:bg-blue-700"
        >
          {showAll ? '접기' : '전체 보기'}
        </button>
      </div>

      <div className="mt-4 flex items-center gap-2">
        <input
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          placeholder="닉네임 또는 이메일 검색"
          className="w-full px-3 py-2 rounded-lg border border-gray-200 text-sm"
        />
        <select
          value={size}
          onChange={(e) => {
            setPage(0);
            setSize(Number(e.target.value));
          }}
          className="px-3 py-2 rounded-lg border border-gray-200 text-sm bg-white"
          title="페이지 크기"
        >
          {[6, 10, 20].map((n) => (
            <option key={n} value={n}>
              {n}개
            </option>
          ))}
        </select>
      </div>

      {error ? (
        <div className="mt-4 bg-red-50 border border-red-100 text-red-700 rounded-xl px-4 py-3 text-sm">
          {error}
        </div>
      ) : null}

      <div className="mt-2">
        {loading ? (
          <div className="mt-4 space-y-3">
            {Array.from({ length: 3 }).map((_, i) => (
              <div
                key={i}
                className="h-16 rounded-xl bg-gray-50 border border-gray-100 animate-pulse"
              />
            ))}
          </div>
        ) : (
          <div className={`mt-2 ${showAll ? 'max-h-[360px] overflow-y-auto pr-1' : ''}`}>
            {visibleUsers.length === 0 ? (
              <div className="py-10 text-center text-sm text-gray-500">표시할 회원이 없습니다.</div>
            ) : (
              visibleUsers.map((u, idx) => (
                <UserRow key={getUserId(u) ?? `${u?.email ?? 'user'}-${idx}`} user={u} onDelete={handleDelete} />
              ))
            )}
          </div>
        )}
      </div>

      <div className="mt-4 flex items-center justify-between">
        <button
          onClick={() => setPage((p) => Math.max(0, p - 1))}
          disabled={page <= 0 || loading}
          className={`px-3 py-2 rounded-lg text-sm font-semibold ${
            page <= 0 || loading
              ? 'bg-gray-100 text-gray-400 cursor-not-allowed'
              : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
          }`}
        >
          이전
        </button>
        <div className="text-xs text-gray-500">
          {data.totalPages ? `${page + 1} / ${data.totalPages}` : `${page + 1}`}
        </div>
        <button
          onClick={() => setPage((p) => p + 1)}
          disabled={loading || (data.totalPages ? page + 1 >= data.totalPages : false)}
          className={`px-3 py-2 rounded-lg text-sm font-semibold ${
            loading || (data.totalPages ? page + 1 >= data.totalPages : false)
              ? 'bg-gray-100 text-gray-400 cursor-not-allowed'
              : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
          }`}
        >
          다음
        </button>
      </div>
    </div>
  );
}
