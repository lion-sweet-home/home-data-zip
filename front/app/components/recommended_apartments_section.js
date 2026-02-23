'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { getRecommendations } from '../api/recommendation';

function formatPrice(value) {
  const n = Number(value);
  if (!Number.isFinite(n) || n <= 0) return '-';
  const eok = Math.floor(n / 10000);
  const man = Math.floor(n % 10000);
  if (eok > 0 && man > 0) return `${eok}억 ${man.toLocaleString()}만`;
  if (eok > 0) return `${eok}억`;
  return `${man.toLocaleString()}만`;
}

export default function RecommendedApartmentsSection() {
  const [list, setList] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let alive = true;
    const run = async () => {
      setLoading(true);
      try {
        const data = await getRecommendations();
        if (!alive) return;
        setList(Array.isArray(data) ? data : []);
      } catch {
        if (!alive) return;
        setList([]);
      } finally {
        if (alive) setLoading(false);
      }
    };
    run();
    return () => {
      alive = false;
    };
  }, []);

  if (loading) return null;
  if (!list || list.length === 0) return null;

  return (
    <section className="px-6 md:px-10 py-8 bg-gray-50">
      <div className="max-w-7xl mx-auto">
        <div className="flex items-end justify-between gap-4 mb-4">
          <div>
            <div className="text-2xl font-bold text-gray-900">맞춤 추천 아파트</div>
            <div className="text-sm text-gray-500 mt-1">최근 관심 평수·지역 기반 추천</div>
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {list.map((item, idx) => {
            const params = new URLSearchParams();
            params.set('aptId', String(item?.id ?? ''));
            if (item?.aptName) params.set('aptName', item.aptName);
            if (item?.roadAddress || item?.jibunAddress) params.set('address', item.roadAddress || item.jibunAddress || '');
            const href = `/apartment?${params.toString()}`;

            return (
              <Link
                key={`rec-${item?.id ?? idx}-${idx}`}
                href={href}
                className="block bg-white border border-gray-200 rounded-xl p-5 shadow-sm hover:shadow-md hover:border-gray-300 transition-all text-left"
              >
                <div className="font-semibold text-gray-900 truncate">{item?.aptName || '아파트'}</div>
                <div className="text-sm text-gray-600 mt-1 truncate">{item?.regionName || '-'}</div>
                <div className="mt-3 flex items-baseline gap-2 text-sm">
                  {item?.recommendArea != null && (
                    <span className="text-gray-700">{Number(item.recommendArea).toFixed(1)}㎡</span>
                  )}
                  {item?.recommendPrice != null && item?.recommendPrice > 0 && (
                    <span className="font-medium text-blue-700">{formatPrice(item.recommendPrice)}</span>
                  )}
                </div>
              </Link>
            );
          })}
        </div>
      </div>
    </section>
  );
}
