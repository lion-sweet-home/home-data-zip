"use client";

import { useEffect, useState, useRef, useCallback } from "react";
import { getChatRoomDetail, exitChatRoom } from "../../api/chat";
import { formatChatTime } from "../../utils/chatUtils";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import MessageInput from "./MessageInput";
import { useRouter } from "next/navigation";
import {
  onRoomDetailUpdate,
  offRoomDetailUpdate,
} from "../../utils/sseManager";
import { getBackendUrl } from "@/app/api/api";

export default function ChatRoomDetail({ roomId, onClose, onRoomListUpdate }) {
  const [roomDetail, setRoomDetail] = useState(null);
  const [messages, setMessages] = useState([]);
  const [loading, setLoading] = useState(true);
  const [hasMore, setHasMore] = useState(true);
  const [currentPage, setCurrentPage] = useState(0);
  const [wsConnected, setWsConnected] = useState(false);
  const [shouldRestoreScroll, setShouldRestoreScroll] = useState(false);
  const [savedScrollHeight, setSavedScrollHeight] = useState(0);
  const [savedScrollTop, setSavedScrollTop] = useState(0);
  const [exiting, setExiting] = useState(false);

  const messagesEndRef = useRef(null);
  const messagesContainerRef = useRef(null);
  const stompClientRef = useRef(null);
  const roomDetailRef = useRef(null);
  const router = useRouter();

  // 스크롤을 맨 아래로
  const scrollToBottom = (useSmooth = false) => {
    const container = messagesContainerRef.current;
    if (container) {
      // requestAnimationFrame을 사용하여 DOM 업데이트 후 스크롤
      requestAnimationFrame(() => {
        if (container) {
          container.scrollTop = container.scrollHeight;
        }
      });
    } else if (messagesEndRef.current) {
      // fallback: scrollIntoView 사용
      messagesEndRef.current.scrollIntoView({
        behavior: useSmooth ? "smooth" : "auto",
      });
    }
  };

  // 채팅방 상세 정보 및 메시지 로드
  const loadRoomDetail = useCallback(
    async (page = 0) => {
      try {
        setLoading(true);
        const data = await getChatRoomDetail(roomId, { page, size: 20 });

        const loadedMessages = data.messages?.content || [];
        console.log("로드된 메시지들:", loadedMessages.length, "개");
        console.log("전체 응답 데이터:", JSON.stringify(data, null, 2));
        console.log("messages 객체:", data.messages);
        console.log("hasNext 값:", data.messages?.hasNext);
        console.log("hasNext 타입:", typeof data.messages?.hasNext);

        // 백엔드에서 DESC로 가져오므로 최신 메시지가 첫 번째
        // 채팅 UI는 오래된 메시지가 위에, 최신 메시지가 아래에 있어야 하므로 역순으로 정렬
        const sortedMessages = [...loadedMessages].reverse();

        // Slice의 hasNext 확인 (다양한 필드명 시도)
        const hasNext =
          data.messages?.hasNext ??
          data.messages?.has_next ??
          data.messages?.next ??
          loadedMessages.length >= 20; // size=20이면 20개 이상이면 다음 페이지가 있을 수 있음

        if (page === 0) {
          // 첫 로드: 역순으로 정렬된 메시지 설정
          setRoomDetail(data);
          roomDetailRef.current = data; // ref에도 저장
          setMessages(sortedMessages);
          console.log(
            "첫 로드 - hasNext:",
            hasNext,
            "메시지 개수:",
            sortedMessages.length,
            "로드된 개수:",
            loadedMessages.length,
          );
          setHasMore(hasNext);
          setCurrentPage(0);

          // 채팅방 목록 갱신 (읽음 처리 후)
          if (onRoomListUpdate) {
            setTimeout(() => {
              onRoomListUpdate();
            }, 300);
          }
        } else {
          // 추가 로드 (이전 메시지): 역순으로 정렬된 메시지를 앞에 추가
          setMessages((prev) => [...sortedMessages, ...prev]);
          console.log(
            "추가 로드 - hasNext:",
            hasNext,
            "로드된 메시지 개수:",
            sortedMessages.length,
          );
          setHasMore(hasNext);
        }
      } catch (error) {
        console.error("채팅방 상세 로드 실패:", error);
      } finally {
        setLoading(false);
      }
    },
    [roomId],
  );

  // 초기 로드
  useEffect(() => {
    if (roomId) {
      loadRoomDetail(0);
    }
  }, [roomId, loadRoomDetail]);

  // SSE roomDetailUpdate 이벤트 구독
  useEffect(() => {
    const handleRoomDetailUpdate = () => {
      // roomId가 있을 때만 메시지 재요청
      if (roomId) {
        loadRoomDetail(0);
      }
    };

    onRoomDetailUpdate(handleRoomDetailUpdate);

    return () => {
      offRoomDetailUpdate(handleRoomDetailUpdate);
    };
  }, [roomId, loadRoomDetail]);

  // 메시지 로드 후 스크롤 처리
  useEffect(() => {
    const container = messagesContainerRef.current;
    if (!container) return;

    if (currentPage === 0 && messages.length > 0) {
      // 첫 로드 시 맨 아래로 스크롤
      // 여러 번 시도하여 메시지가 완전히 렌더링된 후 스크롤
      setTimeout(() => scrollToBottom(false), 100);
      setTimeout(() => scrollToBottom(false), 300);
      setTimeout(() => scrollToBottom(false), 500);
    } else if (shouldRestoreScroll && messages.length > 0) {
      // 이전 메시지 로드 후 스크롤 위치 복원
      setTimeout(() => {
        if (container) {
          const newScrollHeight = container.scrollHeight;
          const scrollDifference = newScrollHeight - savedScrollHeight;
          container.scrollTop = savedScrollTop + scrollDifference;
          setShouldRestoreScroll(false);
        }
      }, 100);
    }
  }, [
    messages.length,
    currentPage,
    shouldRestoreScroll,
    savedScrollHeight,
    savedScrollTop,
  ]);

  // WebSocket 연결
  useEffect(() => {
    if (!roomId) return;

    const accessToken = localStorage.getItem("accessToken");

    // SockJS와 STOMP 클라이언트 생성
    const socket = new SockJS(`${getBackendUrl()}/ws-stomp`);
    const client = new Client({
      webSocketFactory: () => socket,
      connectHeaders: {
        Authorization: `Bearer ${accessToken}`,
      },
      debug: (str) => {
        console.log("STOMP:", str);
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        console.log("WebSocket 연결 성공");
        setWsConnected(true);

        // 채팅방 입장 메시지 전송
        try {
          const enterMessage = {
            type: "ENTER",
            roomId: roomId,
            content: "", // 백엔드에서 자동으로 생성
          };
          client.publish({
            destination: "/pub/chat/message",
            body: JSON.stringify(enterMessage),
          });
          console.log("채팅방 입장 메시지 전송 완료");
        } catch (error) {
          console.error("입장 메시지 전송 실패:", error);
        }

        // 채팅방 구독
        client.subscribe(`/sub/chat/room/${roomId}`, (message) => {
          try {
            const data = JSON.parse(message.body);

            // READ_ALL 이벤트 처리: 상대방이 방에 들어왔을 때 내가 보낸 메시지들을 읽음 처리
            if (data.type === "READ_ALL") {
              console.log("상대방이 방에 들어옴:", data.readerNickname);
              setMessages((prev) => {
                const currentRoomDetail = roomDetailRef.current;
                if (!currentRoomDetail) return prev;

                return prev.map((msg) => {
                  // 내가 보낸 메시지이고 아직 읽지 않은 경우 읽음 처리
                  const isMyMsg =
                    msg.senderNickname !== currentRoomDetail.opponentNickname;
                  if (isMyMsg && msg.isRead === false) {
                    return { ...msg, isRead: true };
                  }
                  return msg;
                });
              });
              return;
            }

            // 일반 메시지 수신
            const newMessage = data;
            console.log("새 메시지 수신:", {
              ...newMessage,
              isRead: newMessage.isRead,
              isReadType: typeof newMessage.isRead,
              senderNickname: newMessage.senderNickname,
            });

            setMessages((prev) => {
              // 중복 방지: messageId가 있으면 messageId로, 없으면 createdAt과 content로 비교
              const isDuplicate = newMessage.messageId
                ? prev.some((msg) => msg.messageId === newMessage.messageId)
                : prev.some(
                    (msg) =>
                      !msg.messageId &&
                      msg.createdAt === newMessage.createdAt &&
                      msg.content === newMessage.content &&
                      msg.senderId === newMessage.senderId,
                  );

              if (isDuplicate) {
                return prev;
              }
              return [...prev, newMessage];
            });

            setTimeout(() => scrollToBottom(false), 100);
          } catch (error) {
            console.error("메시지 파싱 오류:", error);
          }
        });
      },
      onStompError: (frame) => {
        console.error("STOMP 에러:", frame);
        setWsConnected(false);
      },
      onDisconnect: () => {
        console.log("WebSocket 연결 해제");
        setWsConnected(false);
      },
      onWebSocketError: (event) => {
        console.error("WebSocket 에러:", event);
        setWsConnected(false);
      },
    });

    client.activate();
    stompClientRef.current = client;

    return () => {
      if (client) {
        client.deactivate();
      }
    };
  }, [roomId]);

  // 메시지 전송 후 스크롤 처리
  const handleMessageSent = () => {
    setTimeout(scrollToBottom, 100);
  };

  // 채팅방 나가기
  const handleExitRoom = async () => {
    // 한 명이 이미 나간 상태인지 확인
    const isSomeoneExited = roomDetail?.buyerExited || roomDetail?.sellerExited;

    const confirmMessage = isSomeoneExited
      ? "나가면 채팅방이 삭제됩니다. 그래도 나가시겠습니까?"
      : "정말 채팅방을 나가시겠습니까?";

    if (!confirm(confirmMessage)) {
      return;
    }

    try {
      setExiting(true);

      // 채팅방 퇴장 메시지 전송
      if (stompClientRef.current && stompClientRef.current.connected) {
        try {
          const leaveMessage = {
            type: "LEAVE",
            roomId: roomId,
            content: "", // 백엔드에서 자동으로 생성
          };
          stompClientRef.current.publish({
            destination: "/pub/chat/message",
            body: JSON.stringify(leaveMessage),
          });
          console.log("채팅방 퇴장 메시지 전송 완료");

          // 메시지 전송 후 약간의 지연을 주어 메시지가 전송되도록 함
          await new Promise((resolve) => setTimeout(resolve, 200));
        } catch (error) {
          console.error("퇴장 메시지 전송 실패:", error);
        }
      }

      // 채팅방 나가기 API 호출
      await exitChatRoom(roomId);

      // WebSocket 연결 해제
      if (stompClientRef.current) {
        stompClientRef.current.deactivate();
      }

      // 채팅방 목록 갱신
      if (onRoomListUpdate) {
        onRoomListUpdate();
      }

      // 채팅 목록 페이지로 이동
      router.push("/chat");
    } catch (error) {
      console.error("채팅방 나가기 실패:", error);
      alert("채팅방 나가기에 실패했습니다. 다시 시도해주세요.");
      setExiting(false);
    }
  };

  // 이전 메시지 더 불러오기
  const loadMoreMessages = useCallback(() => {
    console.log("loadMoreMessages 호출:", { hasMore, loading, currentPage });

    if (!hasMore || loading) {
      console.log("로드 중단:", { hasMore, loading });
      return;
    }

    // 현재 스크롤 위치와 높이 저장
    const container = messagesContainerRef.current;
    if (!container) {
      console.log("컨테이너 없음");
      return;
    }

    setSavedScrollHeight(container.scrollHeight);
    setSavedScrollTop(container.scrollTop);
    setShouldRestoreScroll(true);

    const nextPage = currentPage + 1;
    console.log("다음 페이지 로드:", nextPage);
    setCurrentPage(nextPage);
    loadRoomDetail(nextPage);
  }, [hasMore, loading, currentPage, loadRoomDetail]);

  // 메시지가 내 메시지인지 확인
  // opponentNickname과 비교: 상대방 닉네임과 다르면 내 메시지
  const isMyMessage = (message) => {
    if (
      !roomDetail ||
      !message.senderNickname ||
      !roomDetail.opponentNickname
    ) {
      return false;
    }

    // roomDetail.opponentNickname은 상대방 닉네임이므로,
    // senderNickname이 opponentNickname과 다르면 내 메시지
    return message.senderNickname !== roomDetail.opponentNickname;
  };

  if (loading && !roomDetail) {
    return (
      <div className="flex-1 flex items-center justify-center">
        <div className="text-gray-500">로딩 중...</div>
      </div>
    );
  }

  if (!roomDetail) {
    return (
      <div className="flex-1 flex items-center justify-center">
        <div className="text-gray-500">채팅방을 찾을 수 없습니다.</div>
      </div>
    );
  }

  return (
    <div className="w-full h-full flex flex-col bg-white overflow-hidden">
      {/* 헤더 */}
      <div className="flex-shrink-0 px-4 py-3 border-b border-gray-200 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div>
            <h2 className="text-lg font-semibold text-gray-900">
              {roomDetail.listingName}
            </h2>
            <p className="text-sm text-gray-500">
              {roomDetail.opponentNickname}
            </p>
          </div>
          {/* WebSocket 연결 상태 표시 */}
          <div className="flex items-center gap-2">
            <div
              className={`w-2 h-2 rounded-full ${wsConnected ? "bg-green-500" : "bg-red-500"}`}
              title={wsConnected ? "연결됨" : "연결 안됨"}
            ></div>
            <span className="text-xs text-gray-500">
              {wsConnected ? "연결됨" : "연결 중..."}
            </span>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={handleExitRoom}
            disabled={exiting}
            className="px-3 py-1.5 text-sm text-red-600 hover:text-red-700 hover:bg-red-50 rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            aria-label="나가기"
          >
            {exiting ? "나가는 중..." : "나가기"}
          </button>
          {onClose && (
            <button
              onClick={onClose}
              className="p-2 text-gray-500 hover:text-gray-700"
              aria-label="닫기"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                className="h-6 w-6"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M6 18L18 6M6 6l12 12"
                />
              </svg>
            </button>
          )}
        </div>
      </div>

      {/* 메시지 영역 */}
      <div
        ref={messagesContainerRef}
        className="flex-1 overflow-y-auto p-4 space-y-4 min-h-0"
        onScroll={(e) => {
          const container = e.target;
          const scrollTop = container.scrollTop;
          const scrollHeight = container.scrollHeight;
          const clientHeight = container.clientHeight;

          // 스크롤이 맨 위에 가까워지면 이전 메시지 자동 로드 (50px 여유)
          if (scrollTop <= 50 && hasMore && !loading) {
            console.log("스크롤 감지 - 이전 메시지 로드 시작", {
              scrollTop,
              hasMore,
              loading,
              currentPage,
            });
            loadMoreMessages();
          }
        }}
      >
        {hasMore && (
          <div className="text-center py-2">
            <button
              onClick={loadMoreMessages}
              className="text-xs text-blue-600 hover:text-blue-700 font-medium px-3 py-1.5 rounded-full bg-blue-50 hover:bg-blue-100 transition-colors inline-block"
              disabled={loading}
            >
              {loading ? "로딩 중..." : "↑ 이전 메시지 더보기"}
            </button>
          </div>
        )}

        {messages
          .filter((message) => {
            // 시스템 메시지(ENTER/LEAVE)인데 content가 비어있으면 필터링하여 제거
            const isSystemMessage =
              message.type === "ENTER" || message.type === "LEAVE";
            if (
              isSystemMessage &&
              (!message.content || message.content.trim() === "")
            ) {
              return false;
            }
            return true;
          })
          .map((message, index) => {
            const isMine = isMyMessage(message);
            const isSystemMessage =
              message.type === "ENTER" || message.type === "LEAVE";

            // 고유한 key 생성: messageId가 있으면 사용, 없으면 index와 createdAt 조합
            const uniqueKey = message.messageId
              ? `msg-${message.messageId}`
              : `msg-${index}-${message.createdAt || Date.now()}`;

            // 디버깅: 메시지 정보 로그
            if (index === 0) {
              console.log("메시지 샘플:", {
                messageId: message.messageId,
                senderNickname: message.senderNickname,
                isRead: message.isRead,
                isMine,
                opponentNickname: roomDetail?.opponentNickname,
                type: message.type,
              });
            }

            // 시스템 메시지 (ENTER/LEAVE)는 가운데 공지 스타일로 표시
            if (isSystemMessage) {
              return (
                <div
                  key={uniqueKey}
                  className="flex justify-center items-center py-2"
                >
                  <div className="px-4 py-1.5 bg-gray-100 text-gray-600 text-xs rounded-full">
                    {message.content}
                  </div>
                </div>
              );
            }

            // 일반 메시지
            return (
              <div
                key={uniqueKey}
                className={`flex ${isMine ? "justify-end" : "justify-start"}`}
              >
                <div
                  className={`flex items-end gap-2 max-w-[70%] ${isMine ? "flex-row-reverse" : "flex-row"}`}
                >
                  {/* 메시지 버블 */}
                  <div
                    className={`px-4 py-2 rounded-2xl ${
                      isMine
                        ? "bg-blue-600 text-white"
                        : "bg-gray-200 text-gray-900"
                    }`}
                  >
                    <div className="text-sm whitespace-pre-wrap break-words">
                      {message.content}
                    </div>
                  </div>

                  {/* 시간 및 읽음 표시 */}
                  <div
                    className={`flex flex-col items-end gap-1 ${isMine ? "" : "items-start"}`}
                  >
                    {/* 내 메시지이고 상대방이 읽지 않았을 때만 표시 - 시간 위에 */}
                    {isMine && message.isRead === false && (
                      <span
                        className="text-xs font-bold text-red-600"
                        title="안 읽음"
                      >
                        1
                      </span>
                    )}
                    <div className="text-xs text-gray-500">
                      {formatChatTime(message.createdAt)}
                    </div>
                  </div>
                </div>
              </div>
            );
          })}
        <div ref={messagesEndRef} />
      </div>

      {/* 입력 영역 */}
      <MessageInput
        roomId={roomId}
        stompClientRef={stompClientRef}
        onMessageSent={handleMessageSent}
      />
    </div>
  );
}
