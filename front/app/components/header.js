'use client';

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { logout } from '../api/auth';
import { getNotificationSetting } from '../api/user';
import { getMySubscription } from '../api/subscription';
import {
  deleteNotification,
  getUnreadCount as getNotificationUnreadCount,
  getUnreadNotifications,
  markAllAsRead,
  markAsRead,
} from '../api/notification';
import {
  connectChatSSE,
  connectNotificationSSE,
  disconnectAllSSE,
  disconnectNotificationSSE,
  onNotification,
  offNotification,
  // onNotificationReconnected,
  // offNotificationReconnected,
  onUnreadCount,
  offUnreadCount,
} from '../utils/sseManager';

export default function Header() {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);
  const [subscription, setSubscription] = useState(null);
  const router = useRouter();

  // 알림(공지) 팝오버
  const [isNotifOpen, setIsNotifOpen] = useState(false);
  const [notifItems, setNotifItems] = useState([]);
  const [notifLoading, setNotifLoading] = useState(false);
  const [notifError, setNotifError] = useState('');
  const [notifUnreadCount, setNotifUnreadCount] = useState(0);
  const notifRef = useRef(null);

  const formatDate = (value) => {
    if (!value) return '';
    const d = new Date(value);
    if (Number.isNaN(d.getTime())) return '';
    return d.toISOString().slice(0, 10);
  };

  // Access Token으로만 로그인 상태 및 사용자 정보 확인
  const checkLoginStatus = useCallback(() => {
    if (typeof window === 'undefined') {
      setLoading(false);
      return;
    }

    const accessToken = localStorage.getItem('accessToken');

    // Access Token이 없으면 비로그인 상태
    if (!accessToken) {
      setIsLoggedIn(false);
      setUser(null);
      setLoading(false);
      return;
    }

    // JWT 토큰에서 사용자 정보 추출 (payload 디코딩)
    try {
      const parts = accessToken.split('.');
      if (parts.length === 3) {
        const payload = JSON.parse(atob(parts[1].replace(/-/g, '+').replace(/_/g, '/')));

        // 사용자 정보 설정 (이메일, 역할 등)
        const userInfo = {
          email: payload.email || null,
          roles: payload.roles || [],
        };

        setUser(userInfo);
        setIsLoggedIn(true);
      } else {
        setIsLoggedIn(false);
        setUser(null);
      }
    } catch (error) {
      console.error('Token decode error in Header:', error);
      setIsLoggedIn(false);
      setUser(null);
      // 유효하지 않은 토큰 제거
      if (typeof window !== 'undefined') {
        localStorage.removeItem('accessToken');
      }
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    checkLoginStatus();

    // 같은 탭: login/logout 시 dispatch하는 커스텀 이벤트
    const onAuthChanged = () => checkLoginStatus();
    // 다른 탭: localStorage 변경 이벤트
    const onStorage = (e) => {
      if (!e) return;
      if (e.key === 'accessToken' || e.key === null) checkLoginStatus();
    };
    // 탭 포커스 복귀 시 토큰 상태 재확인
    const onFocus = () => checkLoginStatus();

    window.addEventListener('auth:changed', onAuthChanged);
    window.addEventListener('storage', onStorage);
    window.addEventListener('focus', onFocus);
    document.addEventListener('visibilitychange', onFocus);

    return () => {
      window.removeEventListener('auth:changed', onAuthChanged);
      window.removeEventListener('storage', onStorage);
      window.removeEventListener('focus', onFocus);
      document.removeEventListener('visibilitychange', onFocus);
    };
  }, [checkLoginStatus]);

  // 로그인 상태에 따라 SSE 연결/해제
  useEffect(() => {
    let cancelled = false;

    async function syncNotificationSse() {
      try {
        const res = await getNotificationSetting();
        if (cancelled) return;
        if (res?.notificationEnabled) connectNotificationSSE();
        else disconnectNotificationSSE();
      } catch (e) {
        // 조회 실패 시(미로그인/만료 등) 알림 SSE는 끊어둔다
        if (!cancelled) disconnectNotificationSSE();
      }
    }

    if (isLoggedIn) {
      // 채팅 SSE는 로그인 시 항상 연결
      connectChatSSE();
      syncNotificationSse();
    } else {
      // 로그아웃 시 SSE 연결 해제
      disconnectAllSSE();
      setUnreadCount(0);
    }

    // 알림 설정 변경(마이페이지 토글) 이벤트 수신 → 알림 SSE on/off
    const onNotificationSettingChanged = (e) => {
      const enabled = e?.detail?.notificationEnabled;
      if (enabled) connectNotificationSSE();
      else disconnectNotificationSSE();
    };
    window.addEventListener('notificationSetting:changed', onNotificationSettingChanged);

    return () => {
      cancelled = true;
      window.removeEventListener('notificationSetting:changed', onNotificationSettingChanged);
      disconnectAllSSE();
    };
  }, [isLoggedIn]);

  // 읽지 않은 메시지 개수 구독
  useEffect(() => {
    if (!isLoggedIn) return;

    const handleUnreadCount = (count) => {
      setUnreadCount(count);
    };

    onUnreadCount(handleUnreadCount);

    return () => {
      offUnreadCount(handleUnreadCount);
    };
  }, [isLoggedIn]);

  // 공지 알림 SSE 구독 (헤더에서 뱃지/팝오버 갱신)
  useEffect(() => {
    if (!isLoggedIn) return;

    const handleNotification = (notification) => {
      // SSE payload는 UserNotificationResponse 형태(id/title/message/createdAt/readAt)
      setNotifItems((prev) => [notification, ...prev].slice(0, 20));
      setNotifUnreadCount((prev) => prev + 1);
    };

    onNotification(handleNotification);
    return () => offNotification(handleNotification);
  }, [isLoggedIn]);

  // 미읽음 개수(뱃지) 동기화 (초기 + 알림 페이지에서 읽음/삭제 시)
  const refreshNotifUnreadCount = useCallback(async () => {
    if (!isLoggedIn) return;
    try {
      const res = await getNotificationUnreadCount();
      setNotifUnreadCount(Number(res?.count ?? 0));
    } catch {
      // ignore
    }
  }, [isLoggedIn]);

  useEffect(() => {
    if (!isLoggedIn) {
      setNotifUnreadCount(0);
      setNotifItems([]);
      setNotifError('');
      setIsNotifOpen(false);
      setSubscription(null);
      return;
    }

    let cancelled = false;
    (async () => {
      try {
        const res = await getNotificationUnreadCount();
        if (cancelled) return;
        setNotifUnreadCount(Number(res?.count ?? 0));
      } catch {
        // ignore
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [isLoggedIn]);

  useEffect(() => {
    if (!isLoggedIn) return;
    let cancelled = false;

    (async () => {
      try {
        const res = await getMySubscription();
        if (!cancelled) setSubscription(res?.data ?? res ?? null);
      } catch {
        if (!cancelled) setSubscription(null);
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [isLoggedIn]);

  // 알림 페이지에서 읽음/삭제 시 헤더 뱃지 즉시 갱신
  useEffect(() => {
    const onNotificationUpdated = () => refreshNotifUnreadCount();
    window.addEventListener('notification:updated', onNotificationUpdated);
    return () => window.removeEventListener('notification:updated', onNotificationUpdated);
  }, [refreshNotifUnreadCount]);

  // Notification SSE 재연결 시 미읽음 개수 API로 동기화
  useEffect(() => {
    if (!isLoggedIn) return;
    const handleReconnected = () => refreshNotifUnreadCount();
    onNotificationReconnected(handleReconnected);
    return () => offNotificationReconnected(handleReconnected);
  }, [isLoggedIn, refreshNotifUnreadCount]);

  // 팝오버 열릴 때 미읽음 목록 로드
  const loadUnreadNotifications = useCallback(async () => {
    setNotifError('');
    setNotifLoading(true);
    try {
      const list = await getUnreadNotifications();
      const arr = Array.isArray(list) ? list : [];
      setNotifItems(arr);
      setNotifUnreadCount(arr.length);
    } catch (e) {
      setNotifError(e?.message ?? '알림을 불러오지 못했습니다.');
    } finally {
      setNotifLoading(false);
    }
  }, []);

  const notifTitle = useMemo(() => {
    if (notifUnreadCount > 0) return `공지 알림 (${notifUnreadCount})`;
    return '공지 알림';
  }, [notifUnreadCount]);

  // 바깥 클릭 / ESC로 팝오버 닫기
  useEffect(() => {
    if (!isNotifOpen) return;

    const onDown = (e) => {
      const el = notifRef.current;
      if (!el) return;
      if (el.contains(e.target)) return;
      setIsNotifOpen(false);
    };

    const onKey = (e) => {
      if (e.key === 'Escape') setIsNotifOpen(false);
    };

    document.addEventListener('mousedown', onDown);
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('mousedown', onDown);
      document.removeEventListener('keydown', onKey);
    };
  }, [isNotifOpen]);

  const onToggleNotif = async () => {
    const next = !isNotifOpen;
    setIsNotifOpen(next);
    if (next) {
      await loadUnreadNotifications();
    }
  };

  const onMarkRead = async (userNotificationId) => {
    try {
      await markAsRead(userNotificationId);
      setNotifItems((prev) => prev.filter((it) => it?.id !== userNotificationId));
      setNotifUnreadCount((prev) => Math.max(0, prev - 1));
    } catch (e) {
      alert(e?.message ?? '읽음 처리에 실패했습니다.');
    }
  };

  const onDelete = async (userNotificationId) => {
    const ok = window.confirm('해당 공지를 삭제하겠습니까?');
    if (!ok) return;
    try {
      await deleteNotification(userNotificationId);
      setNotifItems((prev) => prev.filter((it) => it?.id !== userNotificationId));
      setNotifUnreadCount((prev) => Math.max(0, prev - 1));
    } catch (e) {
      alert(e?.message ?? '삭제에 실패했습니다.');
    }
  };

  const onMarkAllRead = async () => {
    try {
      await markAllAsRead();
      setNotifItems([]);
      setNotifUnreadCount(0);
    } catch (e) {
      alert(e?.message ?? '전체 읽음 처리에 실패했습니다.');
    }
  };

  // 사용자 역할 배열 가져오기
  const getUserRoles = () => {
    if (!user) return [];

    // roles 배열에서 역할 추출
    if (user.roles && Array.isArray(user.roles)) {
      // roles가 문자열 배열인 경우
      if (user.roles.length > 0 && typeof user.roles[0] === 'string') {
        return user.roles.filter((role) => ['ADMIN', 'SELLER', 'USER'].includes(role));
      }
      // roles가 객체 배열인 경우
      if (user.roles.length > 0 && user.roles[0]?.roleType) {
        return user.roles
          .map((ur) => ur.roleType)
          .filter((role) => ['ADMIN', 'SELLER', 'USER'].includes(role));
      }
      if (user.roles.length > 0 && user.roles[0]?.role?.roleType) {
        return user.roles
          .map((ur) => ur.role?.roleType)
          .filter((role) => ['ADMIN', 'SELLER', 'USER'].includes(role));
      }
    }

    // role 필드가 직접 있는 경우 (단일 역할)
    if (user.role) {
      return [user.role];
    }

    return [];
  };

  const roles = getUserRoles();
  const isSubscribed =
    subscription?.status === 'ACTIVE' || subscription?.isActive === true;

  // 역할에 따른 버튼 텍스트 및 동작
  // 우선순위: ADMIN > SELLER > USER만
  const getRoleButton = () => {
    // ADMIN이 있으면 관리자 버튼
    if (roles.includes('ADMIN')) {
      return (
        <Link
          href="/admin"
          className="px-4 py-2 rounded-lg bg-gray-100 text-gray-700 hover:bg-gray-200 transition-colors"
        >
          관리자
        </Link>
      );
    }

    // SELLER가 있으면 구독중 버튼 (구독 페이지로 이동은 유지)
    if (roles.includes('SELLER')) {
      return (
        <Link
          href="/subscription"
          className="px-4 py-2 rounded-lg bg-gray-100 text-gray-700 hover:bg-gray-200 transition-colors"
        >
          구독중
        </Link>
      );
    }

    // USER만 있으면 구독하기/구독중 버튼
    if (roles.length === 1 && roles[0] === 'USER') {
      return (
        <Link
          href="/subscription"
          className="px-4 py-2 rounded-lg bg-gray-100 text-gray-700 hover:bg-gray-200 transition-colors"
        >
          {isSubscribed ? '구독중' : '구독하기'}
        </Link>
      );
    }

    // 위 조건에 해당하지 않으면 버튼 안 보임
    return null;
  };

  // 로그아웃 핸들러
  const handleLogout = async () => {
    try {
      // 로그아웃 전 SSE 연결 해제
      disconnectAllSSE();
      
      await logout();
      // 로그아웃 성공 시 상태 초기화
      setUser(null);
      setIsLoggedIn(false);
      // 페이지 새로고침 또는 홈으로 이동
      router.push('/');
      router.refresh();
    } catch (error) {
      console.error('Logout error:', error);
      // 에러가 발생해도 SSE 연결 해제 및 로컬 상태 정리
      disconnectAllSSE();
      setUser(null);
      setIsLoggedIn(false);
      if (typeof window !== 'undefined') {
        localStorage.removeItem('accessToken');
      }
      router.push('/');
      router.refresh();
    }
  };

  return (
    <header className="w-full border-b border-gray-200 bg-white">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* 로고 및 메뉴 */}
          <div className="flex items-center gap-6">
            <Link href="/" className="flex items-center">
              <span className="text-xl font-bold text-gray-900">HomeDataZip</span>
            </Link>

            {/* 항상 표시되는 메뉴 */}
            <Link
              href="/search"
              className="text-gray-700 hover:text-gray-900 transition-colors font-medium"
            >
              지도 검색
            </Link>
            <Link
              href="/search/listing"
              className="text-gray-700 hover:text-gray-900 transition-colors font-medium"
            >
              매물 검색
            </Link>

            {/* 로그인 상태일 때만 표시되는 메뉴 */}
            {isLoggedIn && (
              <>
                <Link
                  href="/favorite"
                  className="text-gray-700 hover:text-gray-900 transition-colors font-medium"
                >
                  내 관심매물
                </Link>
                {roles.includes('SELLER') && (
                  <Link
                    href="/listing"
                    className="text-gray-700 hover:text-gray-900 transition-colors font-medium"
                  >
                    매물 관리
                  </Link>
                )}
              </>
            )}
          </div>

          {/* 우측 메뉴 */}
          <div className="flex items-center gap-4">
            {/* 로그인 상태일 때만 알림/메시지 아이콘 표시 - 종·메시지 붙여서 표시 */}
            {isLoggedIn && (
              <>
                <div className="flex items-center gap-1">
                  {/* 알림(종) 아이콘 + 팝오버 */}
                  <div className="relative" ref={notifRef}>
                    <button
                      onClick={onToggleNotif}
                      className="relative p-2 text-gray-600 hover:text-gray-900 transition-colors"
                      aria-label="공지 알림"
                      aria-haspopup="dialog"
                      aria-expanded={isNotifOpen}
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
                          d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"
                        />
                      </svg>
                      {notifUnreadCount > 0 && (
                        <span className="absolute -top-0.5 -right-0.5 flex items-center justify-center min-w-[18px] h-[18px] px-1 text-xs font-bold text-white bg-red-600 rounded-full">
                          {notifUnreadCount > 99 ? '99+' : notifUnreadCount}
                        </span>
                      )}
                    </button>

                    {isNotifOpen && (
                      <div
                        className="absolute right-0 mt-2 w-[360px] max-w-[90vw] rounded-2xl border border-gray-200 bg-white shadow-lg overflow-hidden z-50"
                        role="dialog"
                        aria-label="공지 알림"
                      >
                        <div className="flex items-center justify-between px-4 py-3 border-b border-gray-100">
                          <div className="text-sm font-bold text-gray-900">{notifTitle}</div>
                          <div className="flex items-center gap-2">
                            <button
                              type="button"
                              onClick={onMarkAllRead}
                              disabled={notifUnreadCount === 0 || notifLoading}
                              className={`text-xs font-semibold px-2 py-1 rounded-lg ${
                                notifUnreadCount === 0 || notifLoading
                                  ? 'bg-gray-100 text-gray-400 cursor-not-allowed'
                                  : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                              }`}
                            >
                              모두 읽음
                            </button>
                            <button
                              type="button"
                              onClick={() => {
                                setIsNotifOpen(false);
                                router.push('/notification');
                              }}
                              className="text-xs font-semibold px-2 py-1 rounded-lg bg-gray-900 text-white hover:bg-gray-800"
                            >
                              상세보기
                            </button>
                          </div>
                        </div>

                        <div className="max-h-[420px] overflow-y-auto">
                          {notifLoading ? (
                            <div className="p-4 space-y-3">
                              {Array.from({ length: 3 }).map((_, i) => (
                                <div
                                  key={i}
                                  className="h-16 rounded-xl bg-gray-50 border border-gray-100 animate-pulse"
                                />
                              ))}
                            </div>
                          ) : notifError ? (
                            <div className="p-4 text-sm text-red-600">{notifError}</div>
                          ) : notifItems.length === 0 ? (
                            <div className="p-6 text-center text-sm text-gray-500">
                              미읽음 공지 알림이 없습니다.
                            </div>
                          ) : (
                            <div className="divide-y divide-gray-100">
                              {notifItems.map((it) => (
                                <div key={it?.id} className="px-4 py-3">
                                  <div className="flex items-start justify-between gap-3">
                                    <div className="min-w-0">
                                      <div className="text-sm font-semibold text-gray-900 truncate">
                                        {it?.title ?? '(제목 없음)'}
                                      </div>
                                      <div className="text-xs text-gray-500 mt-0.5">
                                        {formatDate(it?.createdAt)}
                                      </div>
                                      <div className="text-sm text-gray-700 mt-2 line-clamp-2 break-words">
                                        {it?.message ?? ''}
                                      </div>
                                    </div>
                                    <div className="shrink-0 flex flex-col gap-2">
                                      <button
                                        type="button"
                                        onClick={() => onMarkRead(it?.id)}
                                        className="text-xs font-semibold px-2 py-1 rounded-lg bg-blue-50 text-blue-700 hover:bg-blue-100"
                                      >
                                        읽음
                                      </button>
                                      <button
                                        type="button"
                                        onClick={() => onDelete(it?.id)}
                                        className="text-xs font-semibold px-2 py-1 rounded-lg bg-red-50 text-red-700 hover:bg-red-100"
                                      >
                                        삭제
                                      </button>
                                    </div>
                                  </div>
                                </div>
                              ))}
                            </div>
                          )}
                        </div>
                      </div>
                    )}
                  </div>

                  {/* 메시지 아이콘 (종 바로 오른쪽) */}
                <button
                  onClick={() => router.push('/chat')}
                  className="relative p-2 text-gray-600 hover:text-gray-900 transition-colors"
                  aria-label="메시지"
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
                      d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"
                    />
                  </svg>
                  {unreadCount > 0 && (
                    <span className="absolute top-0 right-0 flex items-center justify-center min-w-[18px] h-[18px] px-1 text-xs font-bold text-white bg-red-600 rounded-full">
                      {unreadCount > 99 ? '99+' : unreadCount}
                    </span>
                  )}
                </button>
                </div>

                {/* 마이페이지 */}
                <Link
                  href="/my_page"
                  className="flex items-center gap-2 text-gray-600 hover:text-gray-900 transition-colors"
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
                      d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"
                    />
                  </svg>
                  <span className="text-sm font-medium">마이페이지</span>
                </Link>
              </>
            )}

            {/* 역할별 버튼 (로그인 상태일 때만, 마이페이지 오른쪽에 표시) */}
            {isLoggedIn && !loading && getRoleButton()}

            {/* 로그아웃 버튼 (로그인 상태일 때만) */}
            {isLoggedIn && (
              <button
                onClick={handleLogout}
                className="px-4 py-2 rounded-lg bg-red-100 text-red-700 hover:bg-red-200 transition-colors text-sm font-medium"
              >
                로그아웃
              </button>
            )}

            {/* 로그인하지 않았을 때 로그인 링크 표시 */}
            {!isLoggedIn && (
              <Link
                href="/auth/login"
                className="flex items-center gap-2 text-gray-600 hover:text-gray-900 transition-colors"
              >
                <span className="text-sm font-medium">로그인</span>
              </Link>
            )}
          </div>
        </div>
      </div>
    </header>
  );
}
