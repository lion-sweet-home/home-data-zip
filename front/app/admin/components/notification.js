'use client';

import { useEffect, useMemo, useState } from 'react';
import {
  createAdminNotification,
  deleteAdminNotification,
  getAdminNotifications,
  updateAdminNotification,
} from '../../api/admin';

function formatDateTime(d) {
  if (!d) return '-';
  const date = new Date(d);
  if (Number.isNaN(date.getTime())) return '-';
  return date.toISOString().slice(0, 10);
}

function Modal({ open, title, children, onClose }) {
  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center px-4">
      <div className="absolute inset-0 bg-black/40" onClick={onClose} />
      <div className="relative w-full max-w-lg bg-white rounded-2xl border border-gray-200 shadow-lg p-5">
        <div className="flex items-center justify-between">
          <h3 className="text-base font-bold text-gray-900">{title}</h3>
          <button
            onClick={onClose}
            className="px-2 py-1 rounded-lg text-gray-600 hover:bg-gray-100"
            aria-label="닫기"
          >
            ✕
          </button>
        </div>
        <div className="mt-4">{children}</div>
      </div>
    </div>
  );
}

export default function AdminNotifications() {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [items, setItems] = useState([]);

  const [open, setOpen] = useState(false);
  const [mode, setMode] = useState('create'); // create | edit
  const [editingId, setEditingId] = useState(null);
  const [title, setTitle] = useState('');
  const [message, setMessage] = useState('');
  const [saving, setSaving] = useState(false);

  const resetForm = () => {
    setTitle('');
    setMessage('');
    setEditingId(null);
    setMode('create');
  };

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await getAdminNotifications();
      setItems(Array.isArray(res) ? res : []);
    } catch (e) {
      setError(e?.message ?? '공지사항을 불러오지 못했습니다.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const list = useMemo(() => {
    // 최신순(생성일)
    return [...items].sort((a, b) => {
      const da = new Date(a?.createdAt ?? 0).getTime();
      const db = new Date(b?.createdAt ?? 0).getTime();
      return db - da;
    });
  }, [items]);

  const openCreate = () => {
    resetForm();
    setMode('create');
    setOpen(true);
  };

  const openEdit = (it) => {
    setMode('edit');
    setEditingId(it?.id ?? it?.notificationId ?? null);
    setTitle(it?.title ?? '');
    setMessage(it?.message ?? '');
    setOpen(true);
  };

  const onSubmit = async (e) => {
    e.preventDefault();
    if (!title.trim() || !message.trim()) return;

    setSaving(true);
    try {
      if (mode === 'create') {
        await createAdminNotification({ title: title.trim(), message: message.trim() });
      } else {
        await updateAdminNotification(editingId, { title: title.trim(), message: message.trim() });
      }
      setOpen(false);
      resetForm();
      await load();
    } catch (e2) {
      alert(e2?.message ?? '저장에 실패했습니다.');
    } finally {
      setSaving(false);
    }
  };

  const onDelete = async (id) => {
    const ok = window.confirm('삭제하시겠습니까?');
    if (!ok) return;
    try {
      await deleteAdminNotification(id);
      await load();
    } catch (e) {
      alert(e?.message ?? '삭제에 실패했습니다.');
    }
  };

  return (
    <div className="bg-white border border-gray-200 rounded-2xl p-5 shadow-sm">
      <div className="flex items-center justify-between gap-3">
        <div>
          <h2 className="text-base font-bold text-gray-900">공지사항 관리</h2>
          <p className="text-sm text-gray-600 mt-1">공지 등록/수정/삭제</p>
        </div>
        <button
          onClick={openCreate}
          className="px-3 py-2 rounded-lg bg-amber-600 text-white text-sm font-semibold hover:bg-amber-700"
        >
          공지사항 작성
        </button>
      </div>

      {error ? (
        <div className="mt-4 bg-red-50 border border-red-100 text-red-700 rounded-xl px-4 py-3 text-sm">
          {error}
        </div>
      ) : null}

      {/* 목록이 길어져도 카드 높이가 과도하게 늘지 않도록 내부 스크롤 적용 */}
      <div className="mt-4 max-h-[520px] overflow-y-auto pr-1">
        {loading ? (
          <div className="space-y-3">
            {Array.from({ length: 2 }).map((_, i) => (
              <div
                key={i}
                className="h-20 rounded-xl bg-gray-50 border border-gray-100 animate-pulse"
              />
            ))}
          </div>
        ) : list.length === 0 ? (
          <div className="py-10 text-center text-sm text-gray-500">공지사항이 없습니다.</div>
        ) : (
          <div className="space-y-3">
            {list.map((it) => {
              const id = it?.id ?? it?.notificationId;
              return (
                <div key={id} className="border border-gray-200 rounded-xl p-4">
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <div className="text-sm font-bold text-gray-900 truncate">{it?.title}</div>
                      <div className="text-xs text-gray-500 mt-1">{formatDateTime(it?.createdAt)}</div>
                      <div className="text-sm text-gray-700 mt-2 whitespace-pre-wrap break-words">
                        {it?.message}
                      </div>
                    </div>
                    <div className="flex items-center gap-2 shrink-0">
                      <button
                        onClick={() => openEdit(it)}
                        className="px-3 py-1.5 rounded-lg bg-gray-100 text-gray-700 text-xs font-semibold hover:bg-gray-200"
                      >
                        수정
                      </button>
                      <button
                        onClick={() => onDelete(id)}
                        className="px-3 py-1.5 rounded-lg bg-red-50 text-red-600 text-xs font-semibold hover:bg-red-100"
                      >
                        삭제
                      </button>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      <Modal
        open={open}
        title={mode === 'create' ? '공지사항 작성' : '공지사항 수정'}
        onClose={() => {
          if (saving) return;
          setOpen(false);
          resetForm();
        }}
      >
        <form onSubmit={onSubmit} className="space-y-3">
          <div>
            <label className="block text-sm font-medium text-gray-700">제목</label>
            <input
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              className="mt-1 w-full px-3 py-2 rounded-lg border border-gray-200 text-sm"
              placeholder="제목을 입력하세요"
              maxLength={100}
              required
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700">내용</label>
            <textarea
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              className="mt-1 w-full px-3 py-2 rounded-lg border border-gray-200 text-sm min-h-[120px]"
              placeholder="내용을 입력하세요"
              required
            />
          </div>
          <div className="flex items-center justify-end gap-2 pt-2">
            <button
              type="button"
              onClick={() => {
                if (saving) return;
                setOpen(false);
                resetForm();
              }}
              className="px-4 py-2 rounded-lg bg-gray-100 text-gray-700 text-sm font-semibold hover:bg-gray-200"
            >
              취소
            </button>
            <button
              type="submit"
              disabled={saving || !title.trim() || !message.trim()}
              className={`px-4 py-2 rounded-lg text-sm font-semibold ${
                saving || !title.trim() || !message.trim()
                  ? 'bg-gray-200 text-gray-500 cursor-not-allowed'
                  : 'bg-blue-600 text-white hover:bg-blue-700'
              }`}
            >
              {saving ? '저장 중…' : '저장'}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
