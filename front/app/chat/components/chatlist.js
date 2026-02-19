'use client';

import { useEffect, useState } from 'react';
import { getChatRooms } from '../../api/chat';
import { onRoomListUpdate, offRoomListUpdate } from '../../utils/sseManager';
import { formatChatTime } from '../../utils/chatUtils';
import { useRouter } from 'next/navigation';

export default function ChatList({ selectedRoomId, onRoomSelect }) {
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
    if (onRoomSelect) {
      onRoomSelect(roomId);
    }
    router.push(`/chat/${roomId}`);
  };

  return (
    <div className="w-full h-full border-r border-gray-200 bg-white flex flex-col">
      <div className="p-4 border-b border-gray-200">
        <h1 className="text-xl font-bold text-gray-900">채팅</h1>
      </div>
      
      <div className="flex-1 overflow-y-auto relative">
        {initialLoading ? (
          <div className="p-4 text-center text-sm text-gray-500">로딩 중...</div>
        ) : rooms.length === 0 ? (
          <div className="p-8 text-center text-sm text-gray-500">
            채팅이 없습니다.
          </div>
        ) : (
          <div className="divide-y divide-gray-200">
              {rooms.map((room) => (
              <button
                key={room.roomId}
                type="button"
                onClick={() => handleRoomClick(room.roomId)}
                className={`w-full text-left px-4 py-3 hover:bg-gray-50 transition-colors ${
                  selectedRoomId === room.roomId ? 'bg-blue-50' : ''
                }`}
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0 flex-1">
                    <div className="text-sm font-semibold text-gray-900 truncate">
                      {room.listingName}
                    </div>
                    <div className="text-sm text-gray-600 mt-1 truncate">
                      {room.lastMessage || '메시지가 없습니다.'}
                    </div>
                  </div>
                  <div className="text-right shrink-0">
                    <div className="text-xs text-gray-500">
                      {room.lastMessageTime ? formatChatTime(room.lastMessageTime) : ''}
                    </div>
                    {room.unReadCount > 0 && (
                      <div className="mt-1 inline-flex items-center justify-center min-w-[18px] h-[18px] px-1 rounded-full bg-red-600 text-white text-xs font-bold">
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
    </div>
  );
}

