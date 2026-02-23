'use client';

import { useState } from 'react';

export default function MessageInput({ roomId, stompClientRef, onMessageSent }) {
  const [messageInput, setMessageInput] = useState('');
  const [sending, setSending] = useState(false);

  // 메시지 전송
  const handleSendMessage = async () => {
    if (!messageInput.trim() || sending) return;

    const content = messageInput.trim();
    setSending(true);

    try {
      if (!stompClientRef?.current) {
        console.error('WebSocket 클라이언트가 초기화되지 않았습니다.');
        alert('채팅 연결이 끊어졌습니다. 페이지를 새로고침해주세요.');
        setSending(false);
        return;
      }

      if (!stompClientRef.current.connected) {
        console.error('WebSocket이 연결되지 않았습니다.');
        alert('채팅 연결이 끊어졌습니다. 잠시 후 다시 시도해주세요.');
        setSending(false);
        return;
      }

      const messageBody = {
        type: 'TALK',
        roomId: roomId,
        content: content,
      };

      console.log('메시지 전송 시도:', messageBody);
      
      stompClientRef.current.publish({
        destination: '/pub/chat/message',
        body: JSON.stringify(messageBody),
      });

      console.log('메시지 전송 완료 (WebSocket publish 호출됨)');
      
      // 전송 성공 시 입력창 비우기
      setMessageInput('');
      
      // 메시지 전송 완료 콜백 호출
      if (onMessageSent) {
        onMessageSent();
      }
    } catch (error) {
      console.error('메시지 전송 실패:', error);
      alert('메시지 전송에 실패했습니다. 다시 시도해주세요.');
    } finally {
      setSending(false);
    }
  };

  return (
    <div className="flex-shrink-0 px-4 py-3 border-t border-gray-200 bg-white">
      <div className="flex gap-2">
        <input
          type="text"
          value={messageInput}
          onChange={(e) => setMessageInput(e.target.value)}
          onKeyPress={(e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
              e.preventDefault();
              handleSendMessage();
            }
          }}
          placeholder="메시지를 입력하세요..."
          className="flex-1 px-4 py-2 border border-gray-300 rounded-full focus:outline-none focus:ring-2 focus:ring-blue-500"
          disabled={sending}
        />
        <button
          onClick={handleSendMessage}
          disabled={!messageInput.trim() || sending}
          className="px-6 py-2 bg-blue-600 text-white rounded-full hover:bg-blue-700 disabled:bg-gray-300 disabled:cursor-not-allowed transition-colors"
        >
          전송
        </button>
      </div>
    </div>
  );
}

