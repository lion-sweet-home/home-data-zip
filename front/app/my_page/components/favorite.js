'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { getLocalJSON, setLocalJSON } from './_utils';

const LS_KEY = 'myPage:favoritesMock';

const DEFAULT_ITEMS = [
  {
    id: 1,
    aptName: '강남 래미안',
    address: '서울특별시 강남구 역삼동',
    priceText: '27억 원',
    changeRate: 5.2,
  },
  {
    id: 2,
    aptName: '서초 아크로리버파크',
    address: '서울특별시 서초구 반포동',
    priceText: '23억 2천만원',
    changeRate: 3.8,
  },
  {
    id: 3,
    aptName: '송파 헬리오시티',
    address: '서울특별시 송파구 가락동',
    priceText: '19억 8천만원',
    changeRate: 1.4,
  },
  {
    id: 4,
    aptName: '마포 래미안푸르지오',
    address: '서울특별시 마포구 아현동',
    priceText: '15억 6천만원',
    changeRate: -0.7,
  },
  {
    id: 5,
    aptName: '용산 센트럴파크',
    address: '서울특별시 용산구 한강로동',
    priceText: '28억 5천만원',
    changeRate: 0.3,
  },
  {
    id: 6,
    aptName: '성동 트리마제',
    address: '서울특별시 성동구 성수동',
    priceText: '32억 원',
    changeRate: 2.1,
  },
];

function Rate({ value }) {
  const n = Number(value);
  if (!Number.isFinite(n)) return null;
  return (
    <span className={`text-sm font-semibold ${n >= 0 ? 'text-emerald-600' : 'text-red-600'}`}>
      {n >= 0 ? '+' : ''}
      {n.toFixed(1)}%
    </span>
  );
}

export default function FavoriteCard() {
  const [items, setItems] = useState([]);
  const [loaded, setLoaded] = useState(false);
  const router = useRouter();

  useEffect(() => {
    const saved = getLocalJSON(LS_KEY, null);
    if (Array.isArray(saved) && saved.length > 0) {
      setItems(saved);
    } else {
      setItems(DEFAULT_ITEMS);
      setLocalJSON(LS_KEY, DEFAULT_ITEMS);
    }
    setLoaded(true);
  }, []);

  const onDetail = (id) => {
    router.push(`/my_page/favorite/${id}`);
  };

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
          상세보기 →
        </Link>
      </div>

      {!loaded ? (
        <div className="h-56 rounded-xl bg-gray-50 border border-gray-100 animate-pulse" />
      ) : items.length === 0 ? (
        <div className="py-10 text-center text-sm text-gray-500">관심 매물이 없습니다.</div>
      ) : (
        <div className="space-y-3 max-h-[420px] overflow-y-auto pr-1">
          {items.slice(0, 4).map((it) => (
            <button
              key={it.id}
              type="button"
              onClick={() => onDetail(it.id)}
              className="w-full text-left border border-gray-200 rounded-xl p-4 hover:bg-gray-50 transition-colors"
            >
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0">
                  <div className="text-sm font-bold text-gray-900 truncate">{it.aptName}</div>
                  <div className="text-xs text-gray-500 mt-1 truncate">{it.address}</div>
                  <div className="text-sm font-semibold text-blue-600 mt-2">{it.priceText}</div>
                </div>
                <div className="flex items-center gap-2 shrink-0">
                  <Rate value={it.changeRate} />
                  <span className="text-amber-500" aria-hidden>
                    🔖
                  </span>
                </div>
              </div>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

