'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { getChatRooms } from '../../api/chat';
import { onRoomListUpdate, offRoomListUpdate } from '../../utils/sseManager';
import { formatChatTime } from '../../utils/chatUtils';

export default function ChatCard() {
  const [rooms, setRooms] = useState([]);
  const [initialLoading, setInitialLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const router = useRouter();

  // 채팅방 목록 로드
  const loadRooms = async (isRefresh = false) => {
    try {
      if (isRefresh) {
        setRefreshing(true);
      } else {
        setInitialLoading(true);
      }
      const data = await getChatRooms();
      setRooms(data || []);
    } catch (error) {
      console.error('채팅방 목록 로드 실패:', error);
      // 에러 발생 시에도 기존 데이터 유지
      if (rooms.length === 0) {
        setRooms([]);
      }
    } finally {
      setInitialLoading(false);
      setRefreshing(false);
    }
  };

  // 초기 로드
  useEffect(() => {
    loadRooms();
  }, []);

  // SSE roomListUpdate 이벤트 구독
  useEffect(() => {
    const handleRoomListUpdate = () => {
      loadRooms(true); // 새로고침이므로 true 전달
    };

    onRoomListUpdate(handleRoomListUpdate);

    return () => {
      offRoomListUpdate(handleRoomListUpdate);
    };
  }, []);

  // 채팅방 클릭 핸들러
  const handleRoomClick = (roomId) => {
    router.push(`/chat/${roomId}`);
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

      {initialLoading ? (
        <div className="h-56 rounded-xl bg-gray-50 border border-gray-100 animate-pulse" />
      ) : rooms.length === 0 ? (
        <div className="py-10 text-center text-sm text-gray-500">채팅이 없습니다.</div>
      ) : (
        <div className="divide-y max-h-[420px] overflow-y-auto pr-1">
          {rooms.slice(0, 8).map((room) => (
            <button
              key={room.roomId}
              type="button"
              onClick={() => handleRoomClick(room.roomId)}
              className="w-full text-left py-3 hover:bg-gray-50 transition-colors"
            >
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0 flex-1">
                  <div className="text-sm font-bold text-gray-900 truncate">
                    {room.listingName}
                  </div>
                  <div className="text-sm text-gray-600 mt-1 whitespace-normal break-words">
                    {room.lastMessage || '메시지가 없습니다.'}
                  </div>
                </div>
                <div className="text-right shrink-0">
                  <div className="text-xs text-gray-500">
                    {room.lastMessageTime ? formatChatTime(room.lastMessageTime) : ''}
                  </div>
                  {room.unReadCount > 0 && (
                    <div className="mt-1 inline-flex items-center justify-center min-w-5 h-5 px-1.5 rounded-full bg-red-600 text-white text-xs font-bold">
                      {room.unReadCount > 99 ? '99+' : room.unReadCount}
                    </div>
                  )}
                </div>
              </div>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
