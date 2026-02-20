'use client';

/**
 * 알림 툴바: 모두 읽음, 선택 삭제, 읽은 알림 전체 삭제
 */
export default function NotificationToolbar({
  selectedCount,
  onMarkAllRead,
  onDeleteSelected,
  onDeleteAllRead,
  loading,
}) {
  return (
    <div className="flex flex-wrap items-center gap-2 py-3 px-4 border-t border-gray-200 bg-gray-50 rounded-b-lg">
      <button
        type="button"
        onClick={onMarkAllRead}
        disabled={loading}
        className="px-3 py-1.5 text-sm rounded-lg bg-blue-100 text-blue-700 hover:bg-blue-200 disabled:opacity-50 disabled:cursor-not-allowed"
      >
        모두 읽음
      </button>
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
      <button
        type="button"
        onClick={onDeleteAllRead}
        disabled={loading}
        className="px-3 py-1.5 text-sm rounded-lg bg-gray-200 text-gray-700 hover:bg-gray-300 disabled:opacity-50 disabled:cursor-not-allowed"
      >
        읽은 알림 전체 삭제
      </button>
    </div>
  );
}
