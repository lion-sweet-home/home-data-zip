'use client';

import Link from 'next/link';
import { useEffect, useMemo, useState } from 'react';
import { decodeAccessTokenPayload, normalizeRoles } from './_utils';

export default function SubscriptionCard() {
  const [roles, setRoles] = useState([]);

  useEffect(() => {
    const payload = decodeAccessTokenPayload();
    setRoles(normalizeRoles(payload?.roles));
  }, []);

  const isPremium = useMemo(() => roles.includes('SELLER') || roles.includes('PREMIUM'), [roles]);

  return (
    <div className="rounded-2xl p-6 shadow-sm border border-blue-100 bg-gradient-to-r from-blue-700 to-blue-500 text-white">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h2 className="text-lg font-bold">구독 정보</h2>
          <div className="text-sm mt-2">
            현재 플랜: <span className="font-semibold">{isPremium ? '프리미엄' : '무료 체험'}</span>
          </div>
          <p className="text-sm text-blue-50 mt-2">
            프리미엄 구독으로 업그레이드하고 더 많은 기능을 사용해보세요!
          </p>
        </div>

        <span className="px-3 py-1 rounded-full bg-white/15 text-xs font-semibold">
          {isPremium ? 'Premium' : 'Free'}
        </span>
      </div>

      <div className="mt-5 flex items-center gap-3">
        <Link
          href="/subscription"
          className={`inline-flex items-center justify-center px-4 py-2.5 rounded-lg text-sm font-semibold ${
            isPremium ? 'bg-white/20 text-white cursor-not-allowed' : 'bg-white text-blue-700 hover:bg-blue-50'
          }`}
          onClick={(e) => {
            if (isPremium) e.preventDefault();
          }}
        >
          {isPremium ? '구독중' : '구독하기'}
        </Link>

        <button
          type="button"
          onClick={() => alert('구독 상세/결제내역 (TODO)')}
          className="px-4 py-2.5 rounded-lg bg-white/15 text-white text-sm font-semibold hover:bg-white/20"
        >
          자세히
        </button>
      </div>

      <div className="mt-3 text-xs text-blue-100">
        구독/결제 플로우는 추후 연결 예정입니다. (TODO)
      </div>
    </div>
  );
}

