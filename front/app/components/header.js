'use client';

import { useCallback, useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { logout } from '../api/auth';
import { connectSSE, disconnectSSE, onUnreadCount, offUnreadCount } from '../utils/sseManager';

export default function Header() {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);
  const router = useRouter();

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
    if (isLoggedIn) {
      // 로그인 시 SSE 연결
      connectSSE();
    } else {
      // 로그아웃 시 SSE 연결 해제
      disconnectSSE();
      setUnreadCount(0); // 로그아웃 시 읽지 않은 메시지 개수 초기화
    }

    // 컴포넌트 언마운트 시 SSE 연결 해제
    return () => {
      disconnectSSE();
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

    // SELLER가 있으면 구독중 버튼
    if (roles.includes('SELLER')) {
      return (
        <button className="px-4 py-2 rounded-lg bg-gray-100 text-gray-700 cursor-default" disabled>
          구독중
        </button>
      );
    }

    // USER만 있으면 구독하기 버튼
    if (roles.length === 1 && roles[0] === 'USER') {
      return (
        <Link
          href="/subscription"
          className="px-4 py-2 rounded-lg bg-gray-100 text-gray-700 hover:bg-gray-200 transition-colors"
        >
          구독하기
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
      disconnectSSE();
      
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
      disconnectSSE();
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
                  href="/favorites"
                  className="text-gray-700 hover:text-gray-900 transition-colors font-medium"
                >
                  내 관심매물
                </Link>
                {roles.includes('SELLER') && (
                  <Link
                    href="/listing"
                    className="text-gray-700 hover:text-gray-900 transition-colors font-medium"
                  >
                    매물 등록
                  </Link>
                )}
              </>
            )}
          </div>

          {/* 우측 메뉴 */}
          <div className="flex items-center gap-4">
            {/* 로그인 상태일 때만 알림/메시지 아이콘 표시 */}
            {isLoggedIn && (
              <>
                {/* 알림 아이콘 */}
                <button
                  onClick={() => router.push('/notification')}
                  className="p-2 text-gray-600 hover:text-gray-900 transition-colors"
                  aria-label="알림"
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
                </button>

                {/* 메시지 아이콘 */}
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
