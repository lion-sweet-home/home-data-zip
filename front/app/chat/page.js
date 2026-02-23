'use client';

import { useEffect, useState } from 'react';
import { usePathname, useRouter } from 'next/navigation';
import ChatList from './components/chatlist';
import ChatRoomDetail from './components/ChatRoomDetail';
import { getChatRooms } from '../api/chat';

export default function ChatPage() {
  const pathname = usePathname();
  const router = useRouter();
  const [selectedRoomId, setSelectedRoomId] = useState(null);

  // URL에서 roomId 추출 (/chat/123 -> 123)
  useEffect(() => {
    const pathParts = pathname.split('/');
    if (pathParts.length === 3 && pathParts[1] === 'chat' && pathParts[2]) {
      setSelectedRoomId(pathParts[2]);
    } else {
      setSelectedRoomId(null);
    }
  }, [pathname]);

  // 채팅방 목록 갱신 함수
  const loadRooms = async () => {
    try {
      await getChatRooms();
    } catch (error) {
      console.error('채팅방 목록 갱신 실패:', error);
    }
  };

  // 채팅방 선택 핸들러
  const handleRoomSelect = (roomId) => {
    setSelectedRoomId(roomId);
    router.push(`/chat/${roomId}`);
  };

  // 채팅방 닫기 핸들러
  const handleClose = () => {
    setSelectedRoomId(null);
    router.push('/chat');
  };

  return (
    <div className="flex h-screen bg-gray-50">
      {/* 왼쪽: 채팅 목록 (30%) */}
      <div className="w-[30%] flex-shrink-0">
        <ChatList 
          selectedRoomId={selectedRoomId}
          onRoomSelect={handleRoomSelect}
        />
      </div>

      {/* 오른쪽: 채팅방 (70%) */}
      <div className="flex-1 w-[70%] h-full overflow-hidden">
      {selectedRoomId ? (
          <ChatRoomDetail 
            roomId={selectedRoomId} 
            onClose={handleClose}
            onRoomListUpdate={loadRooms}
          />
      ) : (
          <div className="h-full flex items-center justify-center">
          <div className="text-center text-gray-500">
            <p className="text-lg">채팅방을 선택하세요</p>
              <p className="text-sm mt-2">채팅방을 클릭하면 대화를 시작할 수 있습니다.</p>
            </div>
          </div>
        )}
        </div>
    </div>
  );
}
