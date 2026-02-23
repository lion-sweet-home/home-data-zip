'use client';

/**
 * 알림 툴바: 왼쪽(선택 읽음, 선택 삭제) / 오른쪽(모두 읽음, 읽은 알림 전체 삭제)
 * 선택 읽음은 선택된 항목 중 미읽음이 있을 때만 표시
 */
export default function NotificationToolbar({
  selectedCount,
  selectedUnreadCount,
  onMarkSelectedRead,
  onDeleteSelected,
  onMarkAllRead,
  onDeleteAllRead,
  loading,
}) {
  const hasSelectedUnread = selectedUnreadCount > 0;
  return (
    <div className="flex flex-wrap items-center justify-between gap-2 py-3 px-4 border-t border-gray-200 bg-gray-50 rounded-b-lg">
      <div className="flex flex-wrap items-center gap-2">
        {hasSelectedUnread && (
          <button
            type="button"
            onClick={onMarkSelectedRead}
            disabled={loading}
            className="px-3 py-1.5 text-sm rounded-lg bg-blue-100 text-blue-700 hover:bg-blue-200 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            선택 읽음 ({selectedUnreadCount}건)
          </button>
        )}
        {selectedCount > 0 && (
          <button
            type="button"
            onClick={onDeleteSelected}
            disabled={loading}
            className="px-3 py-1.5 text-sm rounded-lg bg-red-100 text-red-700 hover:bg-red-200 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            선택 삭제 ({selectedCount}건)
          </button>
        )}
      </div>
      <div className="flex flex-wrap items-center gap-2">
        <button
          type="button"
          onClick={onMarkAllRead}
          disabled={loading}
          className="px-3 py-1.5 text-sm rounded-lg bg-blue-100 text-blue-700 hover:bg-blue-200 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          모두 읽음
        </button>
        <button
          type="button"
          onClick={onDeleteAllRead}
          disabled={loading}
          className="px-3 py-1.5 text-sm rounded-lg bg-gray-200 text-gray-700 hover:bg-gray-300 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          읽은 알림 전체 삭제
        </button>
      </div>
    </div>
  );
}
