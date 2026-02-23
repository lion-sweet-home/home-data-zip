'use client';

import Link from 'next/link';
import { useMemo } from 'react';
import { useParams } from 'next/navigation';
import { formatDate, getLocalJSON } from '../../components/_utils';

const LS_KEY = 'myPage:chatMock';

function Bubble({ me, text }) {
  return (
    <div className={`flex ${me ? 'justify-end' : 'justify-start'}`}>
      <div
        className={`max-w-[85%] px-4 py-2.5 rounded-2xl text-sm whitespace-pre-wrap break-words ${
          me ? 'bg-blue-600 text-white rounded-tr-md' : 'bg-gray-100 text-gray-800 rounded-tl-md'
        }`}
      >
        {text}
      </div>
    </div>
  );
}

export default function ChatDetailPage() {
  const params = useParams();
  const id = Number(params?.id);

  const room = useMemo(() => {
    const list = getLocalJSON(LS_KEY, []);
    if (!Array.isArray(list)) return null;
    return list.find((x) => Number(x?.id) === id) ?? null;
  }, [id]);

  const messages = useMemo(() => {
    if (!room) return [];
    // TODO: 실제 채팅 메시지 API 연결
    return [
      { me: false, text: room.lastMessage },
      { me: true, text: '네 확인했습니다. 가능한 일정/조건을 알려주세요.' },
      { me: false, text: '좋습니다. 세부 사항 정리해서 다시 연락드리겠습니다.' },
    ];
  }, [room]);

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-4xl mx-auto px-6 md:px-10 py-8">
        <div className="flex items-center justify-between mb-5">
          <div className="flex items-center gap-3">
            <Link href="/my_page" className="text-sm font-medium text-blue-600 hover:text-blue-700">
              ← 마이페이지
            </Link>
            <h1 className="text-xl font-bold text-gray-900">채팅 상세</h1>
          </div>
          {room ? (
            <div className="text-right">
              <div className="text-sm font-semibold text-gray-900">{room.name}</div>
              <div className="text-xs text-gray-500">{formatDate(room.updatedAt)}</div>
            </div>
          ) : null}
        </div>

        {!room ? (
          <div className="bg-white border border-gray-200 rounded-2xl p-6">
            <div className="text-sm text-gray-600">해당 채팅방을 찾을 수 없습니다.</div>
          </div>
        ) : (
          <div className="bg-white border border-gray-200 rounded-2xl shadow-sm overflow-hidden">
            <div className="px-5 py-4 border-b border-gray-200 bg-white">
              <div className="flex items-center justify-between">
                <div className="text-sm font-bold text-gray-900">{room.name}</div>
                {room.unreadCount ? (
                  <span className="text-xs font-semibold text-red-600 bg-red-50 border border-red-100 px-2 py-1 rounded-full">
                    미확인 {room.unreadCount}
                  </span>
                ) : (
                  <span className="text-xs text-gray-500">읽음</span>
                )}
              </div>
            </div>

            <div className="p-5 space-y-3 bg-gray-50">
              {messages.map((m, idx) => (
                <Bubble key={idx} me={m.me} text={m.text} />
              ))}
            </div>

            <div className="px-5 py-4 border-t border-gray-200 bg-white">
              <button
                type="button"
                onClick={() => alert('메시지 전송 (TODO)')}
                className="w-full px-4 py-3 rounded-xl bg-gray-900 text-white text-sm font-semibold hover:bg-black"
              >
                메시지 보내기 (TODO)
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

