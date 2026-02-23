'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { getMyFavorites } from '../../api/favorite';

function formatPrice(won) {
  const n = Number(won);
  if (!Number.isFinite(n) || n <= 0) return '-';
  const manwon = Math.round(n / 10000);
  if (manwon <= 0) return '-';
  const eok = Math.floor(manwon / 10000);
  const rest = manwon % 10000;
  if (eok > 0 && rest > 0) return `${eok}억 ${rest.toLocaleString()}만`;
  if (eok > 0) return `${eok}억`;
  return `${manwon.toLocaleString()}만`;
}

function getPriceText(item) {
  if (item.tradeType === 'SALE') {
    return formatPrice(item.salePrice);
  }
  const dep = formatPrice(item.deposit);
  if (item.monthlyRent && item.monthlyRent > 0) {
    return `${dep} / 월 ${formatPrice(item.monthlyRent)}`;
  }
  return dep;
}

const TRADE_BADGE = {
  SALE: { label: '매매', bg: 'bg-blue-100', text: 'text-blue-700' },
  RENT_CHARTER: { label: '전세', bg: 'bg-emerald-100', text: 'text-emerald-700' },
  RENT_MONTHLY: { label: '월세', bg: 'bg-orange-100', text: 'text-orange-700' },
};

function getBadge(tradeType, monthlyRent) {
  if (tradeType === 'SALE') return TRADE_BADGE.SALE;
  if (monthlyRent && monthlyRent > 0) return TRADE_BADGE.RENT_MONTHLY;
  return TRADE_BADGE.RENT_CHARTER;
}

export default function FavoriteCard() {
  const [items, setItems] = useState([]);
  const [loaded, setLoaded] = useState(false);
  const [error, setError] = useState(false);
  const router = useRouter();

  useEffect(() => {
    let cancelled = false;
    getMyFavorites()
      .then((data) => {
        if (cancelled) return;
        setItems(Array.isArray(data) ? data : []);
      })
      .catch(() => {
        if (!cancelled) setError(true);
      })
      .finally(() => {
        if (!cancelled) setLoaded(true);
      });
    return () => { cancelled = true; };
  }, []);

  return (
    <div className="bg-white border border-gray-200 rounded-2xl p-5 shadow-sm">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <div className="w-9 h-9 rounded-xl bg-gray-50 text-gray-700 flex items-center justify-center">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden>
              <path
                d="M12 21s-7-4.35-7-10a4 4 0 0 1 7-2 4 4 0 0 1 7 2c0 5.65-7 10-7 10Z"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
            </svg>
          </div>
          <h2 className="text-base font-bold text-gray-900">관심 매물</h2>
        </div>
        <Link
          href="/favorite"
          className="text-sm font-semibold text-gray-600 hover:text-gray-900"
        >
          상세보기 &rarr;
        </Link>
      </div>

      {!loaded ? (
        <div className="h-56 rounded-xl bg-gray-50 border border-gray-100 animate-pulse" />
      ) : error ? (
        <div className="py-10 text-center text-sm text-gray-500">관심 매물을 불러오지 못했습니다.</div>
      ) : items.length === 0 ? (
        <div className="py-10 text-center text-sm text-gray-500">관심 매물이 없습니다.</div>
      ) : (
        <div className="space-y-3 max-h-[420px] overflow-y-auto pr-1">
          {items.slice(0, 4).map((it) => {
            const badge = getBadge(it.tradeType, it.monthlyRent);
            return (
              <button
                key={it.listingId}
                type="button"
                onClick={() => router.push(`/search/listing/${it.listingId}`)}
                className="w-full text-left border border-gray-200 rounded-xl p-4 hover:bg-gray-50 transition-colors"
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <div className="flex items-center gap-2 mb-1">
                      <span className={`inline-block px-2 py-0.5 rounded-md text-xs font-semibold ${badge.bg} ${badge.text}`}>
                        {badge.label}
                      </span>
                    </div>
                    <div className="text-sm font-bold text-gray-900 truncate">{it.aptName}</div>
                    <div className="text-xs text-gray-500 mt-1 truncate">
                      {it.jibunAddress || it.roadAddress || '-'}
                    </div>
                    <div className="text-sm font-semibold text-blue-600 mt-2">{getPriceText(it)}</div>
                  </div>
                  <span className="text-red-400 shrink-0" aria-hidden>
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
                      <path
                        d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z"
                      />
                    </svg>
                  </span>
                </div>
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
}
