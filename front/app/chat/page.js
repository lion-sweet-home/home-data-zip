'use client';

import Link from 'next/link';
import { useMemo } from 'react';
import { useRouter } from 'next/navigation';
import { formatDate, getLocalJSON } from '../my_page/components/_utils';

const LS_KEY = 'myPage:chatMock';

export default function ChatPage() {
  const router = useRouter();

  const items = useMemo(() => {
    const list = getLocalJSON(LS_KEY, []);
    return Array.isArray(list) ? list : [];
  }, []);

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-6xl mx-auto px-6 md:px-10 py-8">
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-3">
            <Link href="/" className="text-sm font-medium text-blue-600 hover:text-blue-700">
              ← 메인으로
            </Link>
            <h1 className="text-2xl font-bold text-gray-900">1:1 채팅</h1>
          </div>
          <Link href="/my_page" className="text-sm font-semibold text-gray-700 hover:text-gray-900">
            마이페이지
          </Link>
        </div>

        {items.length === 0 ? (
          <div className="bg-white border border-gray-200 rounded-2xl p-8 text-center text-sm text-gray-600">
            채팅이 없습니다.
          </div>
        ) : (
          <div className="bg-white border border-gray-200 rounded-2xl overflow-hidden">
            <div className="divide-y">
              {items.map((it) => (
                <button
                  key={it.id}
                  type="button"
                  onClick={() => router.push(`/my_page/chat/${it.id}`)}
                  className="w-full text-left px-5 py-4 hover:bg-gray-50 transition-colors"
                >
                  <div className="flex items-start justify-between gap-4">
                    <div className="min-w-0">
                      <div className="text-sm font-bold text-gray-900 truncate">{it.name}</div>
                      <div className="text-sm text-gray-600 mt-1 whitespace-normal break-words">
                        {it.lastMessage}
                      </div>
                    </div>
                    <div className="text-right shrink-0">
                      <div className="text-xs text-gray-500">{formatDate(it.updatedAt)}</div>
                      {it.unreadCount ? (
                        <div className="mt-1 inline-flex items-center justify-center min-w-5 h-5 px-1.5 rounded-full bg-red-600 text-white text-xs font-bold">
                          {it.unreadCount}
                        </div>
                      ) : null}
                    </div>
                  </div>
                </button>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

