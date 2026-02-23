'use client';

import Link from 'next/link';
import { useMemo, useState } from 'react';
import DashboardCards from './components/card';
import YearlyIncome from './components/yearly_income';
import ManageUsers from './components/manage';
import AdminNotifications from './components/notification';

function decodeRolesFromAccessToken() {
  if (typeof window === 'undefined') return [];
  const token = localStorage.getItem('accessToken');
  if (!token) return [];

  try {
    const parts = token.split('.');
    if (parts.length !== 3) return [];
    const payload = JSON.parse(atob(parts[1].replace(/-/g, '+').replace(/_/g, '/')));
    const roles = payload?.roles ?? [];

    if (Array.isArray(roles)) {
      if (roles.length > 0 && typeof roles[0] === 'string') return roles;
      if (roles.length > 0 && roles[0]?.roleType) return roles.map((r) => r.roleType);
      if (roles.length > 0 && roles[0]?.role?.roleType) return roles.map((r) => r.role.roleType);
    }
    return [];
  } catch {
    return [];
  }
}

export default function AdminPage() {
  // eslint(rule): effect body에서 동기 setState를 금지 → 초기값 계산으로 대체
  const [roles] = useState(() => decodeRolesFromAccessToken());

  const isAdmin = useMemo(() => roles.includes('ADMIN'), [roles]);

  if (!isAdmin) {
    return (
      <div className="min-h-screen bg-gray-50">
        <div className="max-w-4xl mx-auto px-6 md:px-10 py-10">
          <div className="bg-white border border-gray-200 rounded-2xl p-6">
            <div className="flex items-start justify-between gap-4">
              <div>
                <h1 className="text-xl font-bold text-gray-900">관리자 페이지</h1>
                <p className="text-sm text-gray-600 mt-1">
                  접근 권한이 없습니다. 관리자 계정으로 로그인해주세요.
                </p>
              </div>
              <Link
                href="/"
                className="text-sm font-medium text-gray-700 hover:text-gray-900"
              >
                메인으로
              </Link>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-7xl mx-auto px-6 md:px-10 py-8">
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-4">
            <Link href="/" className="text-sm font-medium text-blue-600 hover:text-blue-700">
              ← 메인으로
            </Link>
            <h1 className="text-xl md:text-2xl font-bold text-gray-900">관리자 페이지</h1>
          </div>
          <span className="px-3 py-1 rounded-full text-xs font-semibold bg-red-50 text-red-600 border border-red-100">
            Admin
          </span>
        </div>

        <DashboardCards />

        <div className="mt-6">
          <YearlyIncome />
        </div>

        <div className="mt-6 grid grid-cols-1 lg:grid-cols-2 gap-6">
          <ManageUsers />
          <div className="space-y-6">
            <div className="bg-white border border-gray-200 rounded-2xl p-5">
              <div className="flex items-center justify-between">
                <div>
                  <h2 className="text-base font-bold text-gray-900">카테고리 관리</h2>
                  <p className="text-sm text-gray-600 mt-1">미구현 (TODO)</p>
                </div>
                <button
                  className="px-3 py-2 rounded-lg bg-gray-100 text-gray-500 text-sm font-medium cursor-not-allowed"
                  disabled
                >
                  추가
                </button>
              </div>
              <div className="mt-4 text-sm text-gray-600">
                백엔드에 카테고리 관리 API가 없어 현재는 화면만 스텁으로 두었습니다.
              </div>
            </div>

            <AdminNotifications />
          </div>
        </div>
      </div>
    </div>
  );
}
