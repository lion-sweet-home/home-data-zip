'use client';

/**
 * 알림 탭: 전체 / 미읽음 / 읽음
 */
export default function NotificationTabs({ activeTab, onTabChange }) {
  const tabs = [
    { key: 'all', label: '전체' },
    { key: 'unread', label: '미읽음' },
    { key: 'read', label: '읽음' },
  ];

  return (
    <div className="flex gap-1 border-b border-gray-200 bg-gray-50/80 rounded-t-lg overflow-hidden">
      {tabs.map((tab) => (
        <button
          key={tab.key}
          type="button"
          onClick={() => onTabChange(tab.key)}
          className={`flex-1 py-3 px-4 text-sm font-medium transition-colors ${
            activeTab === tab.key
              ? 'bg-white text-blue-600 border-b-2 border-blue-600 shadow-sm'
              : 'text-gray-600 hover:text-gray-900 hover:bg-gray-100'
          }`}
        >
          {tab.label}
        </button>
      ))}
    </div>
  );
}
