'use client';

import PeriodSelector from './period_selector';

export default function TradeHistoryCard({
  tradeType,
  tradePeriod,
  periodOptions = [],
  onChangeTradePeriod,
  onOpenSaleGraph,
  onOpenJeonseGraph,
  onOpenWolseGraph,
  selectedSaleTrades = [],
  rentTrades = [],
  formatDate,
  formatPrice,
  isJeonseByMonthlyRent,
}) {
  return (
    <div className="bg-white border border-gray-200 rounded-xl p-5">
      <div className="flex items-center justify-between mb-4">
        <div className="text-sm font-semibold text-gray-900">거래내역</div>
        {tradeType === '매매' ? (
          <button
            type="button"
            onClick={onOpenSaleGraph}
            className="text-xs text-blue-600 hover:text-blue-700"
          >
            거래 그래프 보기
          </button>
        ) : (
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={onOpenJeonseGraph}
              className="text-xs text-blue-600 hover:text-blue-700"
            >
              전세 그래프 보기
            </button>
            <button
              type="button"
              onClick={onOpenWolseGraph}
              className="text-xs text-emerald-700 hover:text-emerald-800"
            >
              월세 그래프 보기
            </button>
          </div>
        )}
      </div>

      <div className="mb-4">
        <PeriodSelector options={periodOptions} selected={tradePeriod} onChange={onChangeTradePeriod} />
      </div>

      <div className="space-y-2 max-h-[600px] overflow-y-auto">
        {tradeType === '매매' ? (
          selectedSaleTrades.length > 0 ? (
            selectedSaleTrades.map((item, idx) => (
              <div key={`${item?.dealDate || idx}-${idx}`} className="p-3 rounded-lg bg-gray-50 border border-gray-100">
                <div className="flex items-center justify-between">
                  <div className="flex-1">
                    <div className="text-sm text-gray-700">{formatDate(item?.dealDate)}</div>
                    <div className="text-sm text-gray-600 mt-1">{item?.floor != null ? `${item.floor}층` : '-'}</div>
                  </div>
                  <div className="text-2xl font-semibold text-gray-900 text-right ml-4">
                    {formatPrice(item?.dealAmount)}
                  </div>
                </div>
              </div>
            ))
          ) : (
            <div className="text-sm text-gray-500 py-8 text-center">거래 내역이 없습니다.</div>
          )
        ) : rentTrades.length > 0 ? (
          rentTrades.map((item, idx) => (
            <div key={`${item?.dealDate || idx}-${idx}`} className="p-3 rounded-lg bg-gray-50 border border-gray-100">
              <div className="flex items-center justify-between">
                <div className="flex-1">
                  <div className="text-sm text-gray-700">{formatDate(item?.dealDate)}</div>
                  <div className="text-sm text-gray-600 mt-1">{item?.floor != null ? `${item.floor}층` : '-'}</div>
                </div>
                <div className="text-2xl font-semibold text-gray-900 text-right ml-4">
                  {isJeonseByMonthlyRent(item?.monthlyRent) ? (
                    <div>전세 {formatPrice(item?.deposit)}</div>
                  ) : (
                    <div>
                      <div>보증금 {formatPrice(item?.deposit)}</div>
                      <div className="text-xl">월세 {formatPrice(item?.monthlyRent)}</div>
                    </div>
                  )}
                </div>
              </div>
            </div>
          ))
        ) : (
          <div className="text-sm text-gray-500 py-8 text-center">거래 내역이 없습니다.</div>
        )}
      </div>
    </div>
  );
}

