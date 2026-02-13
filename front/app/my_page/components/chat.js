'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { formatDate, getLocalJSON, setLocalJSON } from './_utils';

const LS_KEY = 'myPage:chatMock';

const DEFAULT_ITEMS = [
  {
    id: 1,
    name: '김중개사',
    lastMessage: '해당 매물 방문 가능하십니까?',
    updatedAt: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(),
    unreadCount: 2,
  },
  {
    id: 2,
    name: '이중개사',
    lastMessage: '가격 조정 가능합니다.',
    updatedAt: new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString(),
    unreadCount: 0,
  },
  {
    id: 3,
    name: '박중개사',
    lastMessage: '주차 가능 여부 문의 주셔서 확인 중입니다.',
    updatedAt: new Date(Date.now() - 3 * 24 * 60 * 60 * 1000).toISOString(),
    unreadCount: 1,
  },
  {
    id: 4,
    name: '최중개사',
    lastMessage: '전세도 가능하고 월세 조건도 함께 안내드릴게요.',
    updatedAt: new Date(Date.now() - 5 * 24 * 60 * 60 * 1000).toISOString(),
    unreadCount: 0,
  },
  {
    id: 5,
    name: '정중개사',
    lastMessage: '계약 일정은 다음 주 중으로 조율 가능하십니다.',
    updatedAt: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString(),
    unreadCount: 0,
  },
  {
    id: 6,
    name: '한중개사',
    lastMessage: '해당 동/호수는 현재 다른 분이 문의 중이라 빠른 확인 권장드립니다.',
    updatedAt: new Date(Date.now() - 9 * 24 * 60 * 60 * 1000).toISOString(),
    unreadCount: 3,
  },
  {
    id: 7,
    name: '오중개사',
    lastMessage: '관리비는 평균 18~22만원 선입니다. 세부 내역 공유드릴까요?',
    updatedAt: new Date(Date.now() - 12 * 24 * 60 * 60 * 1000).toISOString(),
    unreadCount: 0,
  },
  {
    id: 8,
    name: '류중개사',
    lastMessage: '내부 사진이 도착하는대로 바로 전달드리겠습니다.',
    updatedAt: new Date(Date.now() - 15 * 24 * 60 * 60 * 1000).toISOString(),
    unreadCount: 0,
  },
];

export default function ChatCard() {
  const [items, setItems] = useState([]);
  const [loaded, setLoaded] = useState(false);
  const router = useRouter();

  useEffect(() => {
    const saved = getLocalJSON(LS_KEY, null);
    if (Array.isArray(saved) && saved.length > 0) {
      setItems(saved);
    } else {
      setItems(DEFAULT_ITEMS);
      setLocalJSON(LS_KEY, DEFAULT_ITEMS);
    }
    setLoaded(true);
  }, []);

  const onDetail = (id) => {
    router.push(`/my_page/chat/${id}`);
  };

  return (
    <div className="bg-white border border-gray-200 rounded-2xl p-5 shadow-sm">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <div className="w-9 h-9 rounded-xl bg-gray-50 text-gray-700 flex items-center justify-center">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden>
              <path
                d="M21 15a4 4 0 0 1-4 4H8l-5 3V7a4 4 0 0 1 4-4h10a4 4 0 0 1 4 4v8Z"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
            </svg>
          </div>
          <h2 className="text-base font-bold text-gray-900">1:1 채팅</h2>
        </div>
        <Link
          href="/chat"
          className="text-sm font-semibold text-gray-600 hover:text-gray-900"
        >
          상세보기 →
        </Link>
      </div>

      {!loaded ? (
        <div className="h-56 rounded-xl bg-gray-50 border border-gray-100 animate-pulse" />
      ) : items.length === 0 ? (
        <div className="py-10 text-center text-sm text-gray-500">채팅이 없습니다.</div>
      ) : (
        <div className="divide-y max-h-[420px] overflow-y-auto pr-1">
          {items.slice(0, 8).map((it) => (
            <button
              key={it.id}
              type="button"
              onClick={() => onDetail(it.id)}
              className="w-full text-left py-3 hover:bg-gray-50 transition-colors"
            >
              <div className="flex items-start justify-between gap-3">
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
      )}
    </div>
  );
}

