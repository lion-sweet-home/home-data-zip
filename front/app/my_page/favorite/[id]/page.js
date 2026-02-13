'use client';

import Link from 'next/link';
import { useMemo } from 'react';
import { useParams } from 'next/navigation';
import { getLocalJSON } from '../../components/_utils';

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

export default function FavoriteDetailPage() {
  const params = useParams();
  const id = Number(params?.id);

  const item = useMemo(() => {
    const list = getLocalJSON(LS_KEY, []);
    if (!Array.isArray(list)) return null;
    return list.find((x) => Number(x?.id) === id) ?? null;
  }, [id]);

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-4xl mx-auto px-6 md:px-10 py-8">
        <div className="flex items-center justify-between mb-5">
          <div className="flex items-center gap-3">
            <Link href="/my_page" className="text-sm font-medium text-blue-600 hover:text-blue-700">
              ← 마이페이지
            </Link>
            <h1 className="text-xl font-bold text-gray-900">관심 아파트 상세</h1>
          </div>
        </div>

        {!item ? (
          <div className="bg-white border border-gray-200 rounded-2xl p-6">
            <div className="text-sm text-gray-600">해당 관심 아파트를 찾을 수 없습니다.</div>
          </div>
        ) : (
          <div className="bg-white border border-gray-200 rounded-2xl p-6 shadow-sm">
            <div className="flex items-start justify-between gap-4">
              <div className="min-w-0">
                <div className="text-lg font-bold text-gray-900">{item.aptName}</div>
                <div className="text-sm text-gray-600 mt-1">{item.address}</div>
                <div className="text-base font-semibold text-blue-600 mt-3">{item.priceText}</div>
              </div>
              <div className="text-right shrink-0">
                <div className="text-xs text-gray-500">전월 대비</div>
                <Rate value={item.changeRate} />
              </div>
            </div>

            <div className="mt-6 grid grid-cols-1 md:grid-cols-2 gap-3">
              <button
                type="button"
                onClick={() => alert('아파트 상세 페이지 연결 (TODO)')}
                className="px-4 py-3 rounded-xl bg-gray-900 text-white text-sm font-semibold hover:bg-black"
              >
                아파트 상세 보기 (TODO)
              </button>
              <button
                type="button"
                onClick={() => alert('관심 해제 (TODO)')}
                className="px-4 py-3 rounded-xl bg-gray-100 text-gray-700 text-sm font-semibold hover:bg-gray-200"
              >
                관심 해제 (TODO)
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

