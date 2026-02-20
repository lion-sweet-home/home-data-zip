'use client';

import { useCallback, useEffect, useState } from 'react';
import Link from 'next/link';
import {
  getAllNotifications,
  getReadNotifications,
  getUnreadNotifications,
  markAllAsRead,
  markAsRead,
  deleteNotification,
  deleteAllReadNotifications,
} from '../api/notification';
import NotificationTabs from './components/NotificationTabs';
import NotificationList from './components/NotificationList';
import NotificationToolbar from './components/NotificationToolbar';

export default function NotificationPage() {
  const [activeTab, setActiveTab] = useState('all');
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [selectedIds, setSelectedIds] = useState(new Set());

  const fetchList = useCallback(async () => {
    setLoading(true);
    try {
      let list = [];
      if (activeTab === 'all') {
        list = await getAllNotifications();
      } else if (activeTab === 'unread') {
        list = await getUnreadNotifications();
      } else {
        list = await getReadNotifications();
      }
      setItems(Array.isArray(list) ? list : []);
    } catch (e) {
      setItems([]);
      console.error(e);
    } finally {
      setLoading(false);
    }
  }, [activeTab]);

  useEffect(() => {
    fetchList();
  }, [fetchList]);

  const handleTabChange = (tab) => {
    setActiveTab(tab);
    setSelectedIds(new Set());
  };

  const handleToggleSelect = (id) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const handleSelectAll = (checked) => {
    if (checked) {
      setSelectedIds(new Set(items.map((it) => it?.id).filter(Boolean)));
    } else {
      setSelectedIds(new Set());
    }
  };

  const handleMarkRead = async (userNotificationId) => {
    try {
      await markAsRead(userNotificationId);
      setItems((prev) => {
        if (activeTab === 'unread') {
          // 미읽음 탭에서는 읽음 처리한 항목을 목록에서 바로 제거
          return prev.filter((it) => it?.id !== userNotificationId);
        }
        return prev.map((it) =>
          it?.id === userNotificationId ? { ...it, readAt: new Date().toISOString() } : it
        );
      });
      setSelectedIds((prev) => {
        const next = new Set(prev);
        next.delete(userNotificationId);
        return next;
      });
      if (typeof window !== 'undefined') window.dispatchEvent(new Event('notification:updated'));
    } catch (e) {
      alert(e?.message ?? '읽음 처리에 실패했습니다.');
    }
  };

  const handleDeleteOne = async (userNotificationId) => {
    const ok = window.confirm('해당 공지를 삭제하겠습니까?');
    if (!ok) return;
    try {
      await deleteNotification(userNotificationId);
      setItems((prev) => prev.filter((it) => it?.id !== userNotificationId));
      setSelectedIds((prev) => {
        const next = new Set(prev);
        next.delete(userNotificationId);
        return next;
      });
      if (typeof window !== 'undefined') window.dispatchEvent(new Event('notification:updated'));
    } catch (e) {
      alert(e?.message ?? '삭제에 실패했습니다.');
    }
  };

  const handleMarkAllRead = async () => {
    setActionLoading(true);
    try {
      await markAllAsRead();
      await fetchList();
      setSelectedIds(new Set());
      if (typeof window !== 'undefined') window.dispatchEvent(new Event('notification:updated'));
    } catch (e) {
      alert(e?.message ?? '전체 읽음 처리에 실패했습니다.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleDeleteSelected = async () => {
    const ids = Array.from(selectedIds);
    if (ids.length === 0) return;
    const ok = window.confirm(`선택한 ${ids.length}건의 공지를 삭제하겠습니까?`);
    if (!ok) return;
    setActionLoading(true);
    try {
      await Promise.all(ids.map((id) => deleteNotification(id)));
      setItems((prev) => prev.filter((it) => !selectedIds.has(it?.id)));
      setSelectedIds(new Set());
      if (typeof window !== 'undefined') window.dispatchEvent(new Event('notification:updated'));
    } catch (e) {
      alert(e?.message ?? '선택 삭제에 실패했습니다.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleDeleteAllRead = async () => {
    const ok = window.confirm('읽은 알림을 모두 삭제하겠습니까?');
    if (!ok) return;
    setActionLoading(true);
    try {
      await deleteAllReadNotifications();
      await fetchList();
      setSelectedIds(new Set());
      if (typeof window !== 'undefined') window.dispatchEvent(new Event('notification:updated'));
    } catch (e) {
      alert(e?.message ?? '읽은 알림 전체 삭제에 실패했습니다.');
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-100">
      <div className="max-w-2xl mx-auto px-4 py-8">
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-xl font-semibold text-gray-800">공지 알림</h1>
          <Link
            href="/"
            className="text-sm text-gray-600 hover:text-gray-900"
          >
            홈으로
          </Link>
        </div>

        <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
          <NotificationTabs activeTab={activeTab} onTabChange={handleTabChange} />
          <div className="max-h-[60vh] overflow-y-auto">
            <NotificationList
              items={items}
              selectedIds={selectedIds}
              onToggleSelect={handleToggleSelect}
              onSelectAll={handleSelectAll}
              onMarkRead={handleMarkRead}
              onDelete={handleDeleteOne}
              loading={loading}
            />
          </div>
          <NotificationToolbar
            selectedCount={selectedIds.size}
            onMarkAllRead={handleMarkAllRead}
            onDeleteSelected={handleDeleteSelected}
            onDeleteAllRead={handleDeleteAllRead}
            loading={actionLoading}
          />
        </div>
      </div>
    </div>
  );
}
