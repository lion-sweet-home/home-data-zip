'use client';

import Link from 'next/link';
import { useMemo } from 'react';
import { useRouter } from 'next/navigation';
import { getLocalJSON } from '../my_page/components/_utils';

const LS_KEY = 'myPage:favoritesMock';

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

export default function FavoritePage() {
  const router = useRouter();

  const items = useMemo(() => {
    const list = getLocalJSON(LS_KEY, []);
    return Array.isArray(list) ? list : [];
  }, []);

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-6xl mx-auto px-6 md:px-10 py-8">
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-3">
            <Link href="/" className="text-sm font-medium text-blue-600 hover:text-blue-700">
              ← 메인으로
            </Link>
            <h1 className="text-2xl font-bold text-gray-900">관심 매물</h1>
          </div>
          <Link href="/my_page" className="text-sm font-semibold text-gray-700 hover:text-gray-900">
            마이페이지
          </Link>
        </div>

        {items.length === 0 ? (
          <div className="bg-white border border-gray-200 rounded-2xl p-8 text-center text-sm text-gray-600">
            관심 매물이 없습니다.
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {items.map((it) => (
              <button
                key={it.id}
                type="button"
                onClick={() => router.push(`/my_page/favorite/${it.id}`)}
                className="text-left bg-white border border-gray-200 rounded-2xl p-5 hover:bg-gray-50 transition-colors"
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <div className="text-base font-bold text-gray-900 truncate">{it.aptName}</div>
                    <div className="text-sm text-gray-600 mt-1 truncate">{it.address}</div>
                    <div className="text-base font-semibold text-blue-600 mt-3">{it.priceText}</div>
                  </div>
                  <div className="text-right shrink-0">
                    <div className="text-xs text-gray-500">전월 대비</div>
                    <Rate value={it.changeRate} />
                  </div>
                </div>
                <div className="mt-4 text-xs text-gray-500">클릭하면 상세로 이동합니다.</div>
              </button>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

