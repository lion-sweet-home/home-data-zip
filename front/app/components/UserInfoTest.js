'use client';

import { useEffect, useState } from 'react';

/**
 * 현재 로그인 유저 정보 테스트 컴포넌트
 * 나중에 삭제 예정
 */
export default function UserInfoTest() {
  const [userInfo, setUserInfo] = useState(null);

  useEffect(() => {
    if (typeof window === 'undefined') return;

    const token = localStorage.getItem('accessToken');
    if (!token) return;

    try {
      const parts = token.split('.');
      if (parts.length !== 3) return;

      const payload = JSON.parse(
        atob(parts[1].replace(/-/g, '+').replace(/_/g, '/')),
      );

      setUserInfo({
        nickname: '(토큰에 없음)', // JWT에는 닉네임이 없음
        email: payload.email || '',
        password: '(보안상 표시 안 함)', // 실제 비밀번호는 알 수 없음
        roles: payload.roles || [],
      });
    } catch (e) {
      console.error('JWT decode error:', e);
    }
  }, []);

  return (
    <div className="min-h-screen px-8 py-10 bg-gray-50">
      <h1 className="text-2xl font-bold mb-6">현재 로그인 유저 정보 (테스트)</h1>

      {!userInfo ? (
        <p className="text-gray-600">로그인한 유저 정보가 없습니다.</p>
      ) : (
        <table className="min-w-full bg-white shadow rounded-lg overflow-hidden">
          <thead className="bg-gray-100">
            <tr>
              <th className="px-4 py-2 text-left text-sm font-semibold text-gray-700">
                닉네임
              </th>
              <th className="px-4 py-2 text-left text-sm font-semibold text-gray-700">
                이메일
              </th>
              <th className="px-4 py-2 text-left text-sm font-semibold text-gray-700">
                비밀번호
              </th>
              <th className="px-4 py-2 text-left text-sm font-semibold text-gray-700">
                Roles
              </th>
            </tr>
          </thead>
          <tbody>
            <tr className="border-t">
              <td className="px-4 py-2 text-sm text-gray-800">
                {userInfo.nickname}
              </td>
              <td className="px-4 py-2 text-sm text-gray-800">
                {userInfo.email}
              </td>
              <td className="px-4 py-2 text-sm text-gray-800">
                {userInfo.password}
              </td>
              <td className="px-4 py-2 text-sm text-gray-800">
                {Array.isArray(userInfo.roles)
                  ? userInfo.roles.join(', ')
                  : String(userInfo.roles)}
              </td>
            </tr>
          </tbody>
        </table>
      )}
    </div>
  );
}
