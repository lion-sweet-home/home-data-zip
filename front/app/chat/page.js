'use client';

import { useEffect, useState } from 'react';
import { getChatRooms } from '../api/chat';
import { onRoomListUpdate, offRoomListUpdate } from '../utils/sseManager';
import { formatChatTime } from '../utils/chatUtils';
import ChatRoomDetail from './components/ChatRoomDetail';

export default function ChatPage() {
  const [rooms, setRooms] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedRoomId, setSelectedRoomId] = useState(null);

  // 채팅방 목록 로드
  const loadRooms = async () => {
    try {
      setLoading(true);
      const data = await getChatRooms();
      setRooms(data || []);
    } catch (error) {
      console.error('채팅방 목록 로드 실패:', error);
      setRooms([]);
    } finally {
      setLoading(false);
    }
  };

  // 초기 로드
  useEffect(() => {
    loadRooms();
  }, []);

  // SSE roomListUpdate 이벤트 구독
  useEffect(() => {
    const handleRoomListUpdate = () => {
      loadRooms();
    };

    onRoomListUpdate(handleRoomListUpdate);

    return () => {
      offRoomListUpdate(handleRoomListUpdate);
    };
  }, []);

  // 채팅방 더블클릭 핸들러
  const handleRoomDoubleClick = (roomId) => {
    setSelectedRoomId(roomId);
    // 채팅방 상세를 열 때 목록 갱신 (SSE 이벤트가 늦게 올 수 있으므로 즉시 갱신)
    setTimeout(() => {
      loadRooms();
    }, 500); // 읽음 처리 후 목록 갱신을 위해 약간의 지연
  };

  return (
    <div className="flex h-screen bg-gray-50">
      {/* 왼쪽: 채팅방 목록 */}
      <div className="w-96 border-r border-gray-200 bg-white flex flex-col">
        <div className="p-4 border-b border-gray-200">
          <h1 className="text-xl font-bold text-gray-900">채팅</h1>
        </div>
        
        <div className="flex-1 overflow-y-auto">
          {loading ? (
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
                  onDoubleClick={() => handleRoomDoubleClick(room.roomId)}
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

      {/* 오른쪽: 채팅방 상세 (더블클릭 시 표시) */}
      {selectedRoomId ? (
        <div className="flex-1 flex flex-col">
          <ChatRoomDetail 
            roomId={selectedRoomId} 
            onClose={() => setSelectedRoomId(null)}
            onRoomListUpdate={loadRooms}
          />
        </div>
      ) : (
        <div className="flex-1 flex items-center justify-center">
          <div className="text-center text-gray-500">
            <p className="text-lg">채팅방을 선택하세요</p>
            <p className="text-sm mt-2">채팅방을 더블클릭하면 대화를 시작할 수 있습니다.</p>
          </div>
        </div>
      )}
    </div>
  );
}
