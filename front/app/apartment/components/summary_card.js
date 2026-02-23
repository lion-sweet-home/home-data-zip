'use client';

export default function ApartmentSummaryCard({
  aptName,
  address,
  tradeType,
  selectedAreaLabel,
  saleAvgPriceText,
  jeonseAvgText,
  wolseAvgText,
}) {
  return (
    <div className="bg-white border border-gray-200 rounded-xl p-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <div className="text-2xl font-bold text-gray-900">{aptName}</div>
          <div className="text-sm text-gray-600 mt-1">{address || '-'}</div>
        </div>

        {tradeType === '매매' ? (
          <div className="text-right">
            <div className="text-xs text-gray-500">선택 면적({selectedAreaLabel}) 평균 거래가</div>
            <div className="text-2xl font-bold text-blue-700 mt-1">{saleAvgPriceText}</div>
          </div>
        ) : (
          <div className="text-right">
            <div className="text-xs text-gray-500">선택 면적({selectedAreaLabel}) 평균 거래가</div>
            <div className="text-sm text-gray-700 mt-1">
              전세 평균 <span className="font-semibold text-blue-700">{jeonseAvgText}</span>
            </div>
            <div className="text-sm text-gray-700">
              월세 보증금/월세 평균{' '}
              <span className="font-semibold text-emerald-700">{wolseAvgText}</span>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

