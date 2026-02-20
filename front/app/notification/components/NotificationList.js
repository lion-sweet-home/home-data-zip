'use client';

import { useEffect, useRef } from 'react';

/**
 * 알림 목록: 체크박스 + 항목(제목, 날짜, 읽음 상태) + 개별 읽음/삭제
 */
function formatDate(value) {
  if (!value) return '';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return '';
  return d.toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export default function NotificationList({
  items,
  selectedIds,
  onToggleSelect,
  onSelectAll,
  onMarkRead,
  onDelete,
  loading,
}) {
  const allSelected =
    items.length > 0 && items.every((it) => selectedIds.has(it?.id));
  const someSelected = selectedIds.size > 0;
  const selectAllRef = useRef(null);

  useEffect(() => {
    const el = selectAllRef.current;
    if (!el) return;
    el.indeterminate = someSelected && !allSelected;
  }, [someSelected, allSelected]);

  if (loading) {
    return (
      <div className="py-12 text-center text-gray-500">
        알림을 불러오는 중…
      </div>
    );
  }

  if (!items?.length) {
    return (
      <div className="py-12 text-center text-gray-500">
        알림이 없습니다.
      </div>
    );
  }

  return (
    <div className="divide-y divide-gray-100">
      {/* 헤더: 전체 선택 체크박스 */}
      <div className="flex items-center gap-3 px-4 py-2 bg-gray-50 border-b border-gray-100">
        <label className="flex items-center gap-2 cursor-pointer select-none">
          <input
            ref={selectAllRef}
            type="checkbox"
            checked={allSelected}
            onChange={(e) => onSelectAll(e.target.checked)}
            className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
          />
          <span className="text-sm text-gray-600">전체 선택</span>
        </label>
      </div>

      {items.map((item) => {
        const id = item?.id;
        const isRead = !!item?.readAt;
        const isSelected = selectedIds.has(id);

        return (
          <div
            key={id}
            className={`flex items-start gap-3 px-4 py-3 hover:bg-gray-50/50 transition-colors ${
              isRead ? 'bg-white' : 'bg-blue-50/30'
            }`}
          >
            <label className="flex items-center pt-1 shrink-0 cursor-pointer">
              <input
                type="checkbox"
                checked={isSelected}
                onChange={() => onToggleSelect(id)}
                className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
              />
            </label>
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2 flex-wrap">
                <span
                  className={`font-medium ${isRead ? 'text-gray-700' : 'text-gray-900'}`}
                >
                  {item?.title ?? '(제목 없음)'}
                </span>
                {!isRead && (
                  <span className="text-xs px-1.5 py-0.5 rounded bg-blue-100 text-blue-700">
                    새 알림
                  </span>
                )}
              </div>
              {item?.message && (
                <p className="mt-1 text-sm text-gray-600 line-clamp-2">
                  {item.message}
                </p>
              )}
              <p className="mt-1 text-xs text-gray-400">
                {formatDate(item?.createdAt)}
              </p>
            </div>
            <div className="flex items-center gap-1 shrink-0">
              {!isRead && (
                <button
                  type="button"
                  onClick={() => onMarkRead(id)}
                  className="text-xs px-2 py-1 rounded text-blue-600 hover:bg-blue-100"
                >
                  읽음
                </button>
              )}
              <button
                type="button"
                onClick={() => onDelete(id)}
                className="text-xs px-2 py-1 rounded text-red-600 hover:bg-red-100"
              >
                삭제
              </button>
            </div>
          </div>
        );
      })}
    </div>
  );
}
