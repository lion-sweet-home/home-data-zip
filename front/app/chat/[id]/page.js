'use client';

import { useParams, useRouter } from 'next/navigation';
import ChatList from '../components/chatlist';
import ChatRoomDetail from '../components/ChatRoomDetail';
import { useEffect, useState } from 'react';
import { getChatRooms } from '../../api/chat';

export default function ChatDetailPage() {
  const params = useParams();
  const router = useRouter();
  const roomId = params?.id;
  const [loadRooms, setLoadRooms] = useState(null);

  // 채팅방 목록 갱신 함수
  useEffect(() => {
    const refreshRooms = async () => {
      try {
        await getChatRooms();
      } catch (error) {
        console.error('채팅방 목록 갱신 실패:', error);
      }
    };
    setLoadRooms(() => refreshRooms);
  }, []);

  // 채팅방 선택 핸들러
  const handleRoomSelect = (selectedRoomId) => {
    router.push(`/chat/${selectedRoomId}`);
  };

  // 채팅방 닫기 핸들러
  const handleClose = () => {
    router.push('/chat');
  };

  if (!roomId) {
    return (
      <div className="flex h-screen bg-gray-50">
        <div className="w-[30%] flex-shrink-0">
          <ChatList 
            selectedRoomId={null}
            onRoomSelect={handleRoomSelect}
          />
        </div>
        <div className="flex-1 w-[70%] h-full overflow-hidden flex items-center justify-center">
          <div className="text-gray-500">채팅방을 찾을 수 없습니다.</div>
        </div>
      </div>
    );
  }

  return (
    <div className="flex h-screen bg-gray-50">
      {/* 왼쪽: 채팅 목록 (30%) */}
      <div className="w-[30%] flex-shrink-0">
        <ChatList 
          selectedRoomId={roomId}
          onRoomSelect={handleRoomSelect}
        />
      </div>

      {/* 오른쪽: 채팅방 (70%) */}
      <div className="flex-1 w-[70%] h-full overflow-hidden">
        <ChatRoomDetail 
          roomId={roomId} 
          onClose={handleClose}
          onRoomListUpdate={loadRooms}
        />
      </div>
    </div>
  );
}
