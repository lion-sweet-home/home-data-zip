'use client';

import Link from 'next/link';
import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { getMyFavorites, removeFavorite } from '../api/favorite';

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

export default function FavoritePage() {
  const router = useRouter();
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [removingId, setRemovingId] = useState(null);

  const fetchFavorites = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getMyFavorites();
      setItems(Array.isArray(data) ? data : []);
    } catch (err) {
      if (err.status === 401) {
        setError('login');
      } else {
        setError('fail');
      }
      setItems([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchFavorites();
  }, []);

  const handleRemove = async (e, listingId) => {
    e.stopPropagation();
    if (removingId) return;
    setRemovingId(listingId);
    try {
      await removeFavorite(listingId);
      setItems((prev) => prev.filter((it) => it.listingId !== listingId));
    } catch (err) {
      console.error('관심 매물 해제 실패:', err);
    } finally {
      setRemovingId(null);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-6xl mx-auto px-6 md:px-10 py-8">
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-3">
            <Link href="/" className="text-sm font-medium text-blue-600 hover:text-blue-700">
              &larr; 메인으로
            </Link>
            <h1 className="text-2xl font-bold text-gray-900">관심 매물</h1>
          </div>
          <Link href="/my_page" className="text-sm font-semibold text-gray-700 hover:text-gray-900">
            마이페이지
          </Link>
        </div>

        {loading ? (
          <div className="flex flex-col items-center justify-center py-20">
            <svg className="animate-spin h-8 w-8 text-blue-500 mb-4" viewBox="0 0 24 24" fill="none">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
            </svg>
            <p className="text-gray-500">관심 매물을 불러오는 중...</p>
          </div>
        ) : error === 'login' ? (
          <div className="bg-white border border-gray-200 rounded-2xl p-8 text-center">
            <p className="text-sm text-gray-600 mb-4">로그인이 필요합니다.</p>
            <button
              onClick={() => router.push('/auth/login')}
              className="px-5 py-2.5 bg-blue-600 text-white rounded-xl text-sm font-semibold hover:bg-blue-700 transition-colors"
            >
              로그인하기
            </button>
          </div>
        ) : error === 'fail' ? (
          <div className="bg-white border border-gray-200 rounded-2xl p-8 text-center">
            <p className="text-sm text-gray-600 mb-4">관심 매물을 불러오지 못했습니다.</p>
            <button
              onClick={fetchFavorites}
              className="px-5 py-2.5 bg-gray-900 text-white rounded-xl text-sm font-semibold hover:bg-black transition-colors"
            >
              다시 시도
            </button>
          </div>
        ) : items.length === 0 ? (
          <div className="bg-white border border-gray-200 rounded-2xl p-8 text-center">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" className="text-gray-300 mx-auto mb-3">
              <path
                d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z"
                stroke="currentColor"
                strokeWidth="1.5"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
            </svg>
            <p className="text-sm text-gray-600 mb-1">관심 매물이 없습니다.</p>
            <p className="text-xs text-gray-400">매물 상세에서 하트를 눌러 관심 매물로 등록해보세요.</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {items.map((it) => {
              const badge = getBadge(it.tradeType, it.monthlyRent);
              return (
                <div
                  key={it.listingId}
                  className="relative bg-white border border-gray-200 rounded-2xl p-5 hover:bg-gray-50 transition-colors cursor-pointer"
                  onClick={() => router.push(`/search/listing/${it.listingId}`)}
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <div className="flex items-center gap-2 mb-1">
                        <span className={`inline-block px-2 py-0.5 rounded-md text-xs font-semibold ${badge.bg} ${badge.text}`}>
                          {badge.label}
                        </span>
                      </div>
                      <div className="text-base font-bold text-gray-900 truncate">{it.aptName}</div>
                      <div className="text-sm text-gray-500 mt-1 truncate">
                        {it.jibunAddress || it.roadAddress || '-'}
                      </div>
                      <div className="flex items-center gap-3 mt-2 text-xs text-gray-400">
                        {it.exclusiveArea && <span>{Number(it.exclusiveArea).toFixed(1)}m²</span>}
                        {it.floor != null && <span>{it.floor}층</span>}
                      </div>
                      <div className="text-base font-semibold text-blue-600 mt-2">
                        {getPriceText(it)}
                      </div>
                    </div>
                    <button
                      onClick={(e) => handleRemove(e, it.listingId)}
                      disabled={removingId === it.listingId}
                      className={`shrink-0 w-9 h-9 rounded-full flex items-center justify-center bg-red-50 text-red-500 hover:bg-red-100 transition-colors ${
                        removingId === it.listingId ? 'opacity-50 cursor-not-allowed' : ''
                      }`}
                      title="관심 매물 해제"
                    >
                      <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
                        <path
                          d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z"
                          stroke="currentColor"
                          strokeWidth="2"
                          strokeLinecap="round"
                          strokeLinejoin="round"
                        />
                      </svg>
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
