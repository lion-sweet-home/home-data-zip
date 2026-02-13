'use client';

import { useEffect, useMemo, useState } from 'react';
import {
  getListingsCount,
  getMonthlyIncome,
  getSubscribersCount,
  getUsersCount,
} from '../../api/admin';

function formatMoneyKRWShort(amount) {
  const n = Number(amount ?? 0);
  if (!Number.isFinite(n)) return '-';

  // 1억(100,000,000) 단위까지는 '만원' 표현이 더 친숙해서 스크린샷과 유사하게 맞춤
  // 예) 2,350,000 => 235만원
  const 만원 = Math.round(n / 10000);
  return `${만원.toLocaleString('ko-KR')}만원`;
}

function formatNumber(n) {
  const v = Number(n ?? 0);
  if (!Number.isFinite(v)) return '-';
  return v.toLocaleString('ko-KR');
}

function StatCard({ icon, title, value, trend, accentClass }) {
  return (
    <div className="bg-white border border-gray-200 rounded-2xl p-5 shadow-sm">
      <div className="flex items-start justify-between">
        <div className="flex items-center gap-3">
          <div className={`w-9 h-9 rounded-xl flex items-center justify-center ${accentClass}`}>
            {icon}
          </div>
          <div>
            <div className="text-sm text-gray-500 font-medium">{title}</div>
            <div className="text-xl font-bold text-gray-900 mt-1">{value}</div>
          </div>
        </div>

        {trend ? (
          <div className="text-xs font-semibold text-green-600 flex items-center gap-1">
            <span>↗</span>
            <span>{trend}</span>
          </div>
        ) : null}
      </div>
    </div>
  );
}

export default function DashboardCards() {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [stats, setStats] = useState({
    monthlyIncome: null,
    usersCount: null,
    subscribersCount: null,
    listingsCount: null,
  });

  useEffect(() => {
    let alive = true;

    async function run() {
      setLoading(true);
      setError(null);
      try {
        const [monthlyIncome, usersCount, subscribersCount, listingsCount] = await Promise.all([
          getMonthlyIncome(),
          getUsersCount(),
          getSubscribersCount(),
          getListingsCount(), // NOTE: 백엔드 엔드포인트가 있으면 바로 연동
        ]);
        if (!alive) return;
        setStats({ monthlyIncome, usersCount, subscribersCount, listingsCount });
      } catch (e) {
        if (!alive) return;
        setError(e?.message ?? '통계 데이터를 불러오지 못했습니다.');
      } finally {
        if (!alive) return;
        setLoading(false);
      }
    }

    run();
    return () => {
      alive = false;
    };
  }, []);

  const cards = useMemo(() => {
    const monthlyIncomeText = loading ? '불러오는 중…' : formatMoneyKRWShort(stats.monthlyIncome);
    const usersText = loading ? '불러오는 중…' : `${formatNumber(stats.usersCount)}명`;
    const subscribersText = loading ? '불러오는 중…' : `${formatNumber(stats.subscribersCount)}명`;
    const listingsText = loading
      ? '불러오는 중…'
      : stats.listingsCount === null || stats.listingsCount === undefined
        ? 'TODO'
        : `${formatNumber(stats.listingsCount)}개`;

    return [
      {
        title: '이번 달 수입',
        value: monthlyIncomeText,
        trend: null,
        accentClass: 'bg-green-50 text-green-600',
        icon: (
          <span className="text-lg font-bold" aria-hidden>
            $
          </span>
        ),
      },
      {
        title: '총 회원 수',
        value: usersText,
        trend: null,
        accentClass: 'bg-blue-50 text-blue-600',
        icon: (
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden>
            <path
              d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
            <path
              d="M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
            <path
              d="M22 21v-2a4 4 0 0 0-3-3.87"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
            <path
              d="M16 3.13a4 4 0 0 1 0 7.75"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        ),
      },
      {
        title: '프리미엄 구독자',
        value: subscribersText,
        trend: null,
        accentClass: 'bg-purple-50 text-purple-600',
        icon: (
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden>
            <path
              d="M12 14c3.866 0 7-3.134 7-7S15.866 0 12 0 5 3.134 5 7s3.134 7 7 7Z"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
            <path
              d="M7 21v-1a5 5 0 0 1 10 0v1"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        ),
      },
      {
        title: '등록된 매물',
        value: listingsText,
        trend: null,
        accentClass: 'bg-orange-50 text-orange-600',
        icon: (
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden>
            <path
              d="M3 9.5 12 3l9 6.5V21a1 1 0 0 1-1 1h-5v-7H9v7H4a1 1 0 0 1-1-1V9.5Z"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        ),
      },
    ];
  }, [loading, stats]);

  return (
    <div className="space-y-3">
      {error ? (
        <div className="bg-red-50 border border-red-100 text-red-700 rounded-xl px-4 py-3 text-sm">
          {error}
        </div>
      ) : null}

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {cards.map((c) => (
          <StatCard key={c.title} {...c} />
        ))}
      </div>
    </div>
  );
}
