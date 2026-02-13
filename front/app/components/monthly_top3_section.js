'use client';

import { useEffect, useState } from 'react';
import { getJeonseTop3, getSaleTop3, getWolseTop3 } from '../api/apartment';
import SaleCard from './apartment_card/sale';
import RentJeonseCard from './apartment_card/rent_jeose';
import RentWolseCard from './apartment_card/rent_wolse';

function CategoryPanel({ title, subtitle, accentClassName, children }) {
  return (
    <div className="bg-white border border-gray-200 rounded-2xl overflow-hidden">
      <div className={`px-4 py-3 border-b border-gray-200 bg-gray-50 ${accentClassName}`}>
        <div className="flex items-center justify-between gap-3">
          <div className="min-w-0">
            <div className="text-base font-semibold text-gray-900">{title}</div>
            {subtitle ? <div className="text-xs text-gray-600 mt-0.5">{subtitle}</div> : null}
          </div>
        </div>
      </div>
      <div className="p-4 space-y-3">{children}</div>
    </div>
  );
}

export default function MonthlyTop3Section() {
  const [saleTop3, setSaleTop3] = useState([]);
  const [jeonseTop3, setJeonseTop3] = useState([]);
  const [wolseTop3, setWolseTop3] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let alive = true;
    const run = async () => {
      setLoading(true);
      try {
        const [sale, jeonse, wolse] = await Promise.allSettled([
          getSaleTop3(),
          getJeonseTop3(),
          getWolseTop3(),
        ]);

        if (!alive) return;
        setSaleTop3(sale.status === 'fulfilled' ? sale.value || [] : []);
        setJeonseTop3(jeonse.status === 'fulfilled' ? jeonse.value || [] : []);
        setWolseTop3(wolse.status === 'fulfilled' ? wolse.value || [] : []);
      } finally {
        if (alive) setLoading(false);
      }
    };

    run();
    return () => {
      alive = false;
    };
  }, []);

  const handleCardClick = () => {
    // TODO: 클릭 시 상세/지도 이동 연결 (요청대로 현재는 미구현)
  };

  return (
    <section className="px-6 md:px-10 py-8 bg-gray-50">
      <div className="max-w-7xl mx-auto">
        <div className="flex items-end justify-between gap-4 mb-4">
          <div>
            <div className="text-2xl font-bold text-gray-900">전월 거래량 TOP 3</div>
          </div>
        </div>

        {loading ? (
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <div className="bg-white border border-gray-200 rounded-2xl overflow-hidden animate-pulse">
              <div className="h-14 bg-gray-100 border-b border-gray-200" />
              <div className="p-4 space-y-3">
                <div className="h-[170px] bg-gray-100 rounded-2xl" />
                <div className="h-[170px] bg-gray-100 rounded-2xl" />
                <div className="h-[170px] bg-gray-100 rounded-2xl" />
              </div>
            </div>
            <div className="bg-white border border-gray-200 rounded-2xl overflow-hidden animate-pulse">
              <div className="h-14 bg-gray-100 border-b border-gray-200" />
              <div className="p-4 space-y-3">
                <div className="h-[170px] bg-gray-100 rounded-2xl" />
                <div className="h-[170px] bg-gray-100 rounded-2xl" />
                <div className="h-[170px] bg-gray-100 rounded-2xl" />
              </div>
            </div>
            <div className="bg-white border border-gray-200 rounded-2xl overflow-hidden animate-pulse">
              <div className="h-14 bg-gray-100 border-b border-gray-200" />
              <div className="p-4 space-y-3">
                <div className="h-[170px] bg-gray-100 rounded-2xl" />
                <div className="h-[170px] bg-gray-100 rounded-2xl" />
                <div className="h-[170px] bg-gray-100 rounded-2xl" />
              </div>
            </div>
          </div>
        ) : (
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <CategoryPanel
              title="매매"
              accentClassName="border-l-4 border-l-blue-600"
            >
              {(saleTop3 || []).slice(0, 3).map((item, idx) => (
                <SaleCard key={item?.aptId ?? idx} item={item} rank={idx + 1} onClick={handleCardClick} />
              ))}
              {(!saleTop3 || saleTop3.length === 0) && (
                <div className="text-sm text-gray-500 bg-white border border-gray-200 rounded-2xl p-5">
                  데이터가 없습니다.
                </div>
              )}
            </CategoryPanel>

            <CategoryPanel
              title="전세"
              accentClassName="border-l-4 border-l-violet-600"
            >
              {(jeonseTop3 || []).slice(0, 3).map((item, idx) => (
                <RentJeonseCard key={item?.aptId ?? idx} item={item} rank={idx + 1} onClick={handleCardClick} />
              ))}
              {(!jeonseTop3 || jeonseTop3.length === 0) && (
                <div className="text-sm text-gray-500 bg-white border border-gray-200 rounded-2xl p-5">
                  데이터가 없습니다.
                </div>
              )}
            </CategoryPanel>

            <CategoryPanel
              title="월세"
              accentClassName="border-l-4 border-l-emerald-600"
            >
              {(wolseTop3 || []).slice(0, 3).map((item, idx) => (
                <RentWolseCard key={item?.aptId ?? idx} item={item} rank={idx + 1} onClick={handleCardClick} />
              ))}
              {(!wolseTop3 || wolseTop3.length === 0) && (
                <div className="text-sm text-gray-500 bg-white border border-gray-200 rounded-2xl p-5">
                  데이터가 없습니다.
                </div>
              )}
            </CategoryPanel>
          </div>
        )}
      </div>
    </section>
  );
}

