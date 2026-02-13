'use client';

import PeriodSelector from './period_selector';

export default function GraphModal({
  open,
  onClose,
  tradeType,
  rentGraphView,
  graphPeriod,
  periodOptions = [],
  onChangeGraphPeriod,
  graphLoading,
  children,
}) {
  if (!open) return null;

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-xl w-full max-w-5xl max-h-[90vh] overflow-y-auto p-5">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl font-bold text-gray-900">
            {tradeType === '매매'
              ? '거래 그래프'
              : rentGraphView === 'jeonse'
                ? '전세 그래프'
                : '월세 그래프'}
          </h2>
          <button
            type="button"
            onClick={onClose}
            className="text-2xl text-gray-500 hover:text-gray-700"
            aria-label="닫기"
          >
            ×
          </button>
        </div>

        <div className="mb-4">
          <PeriodSelector
            options={periodOptions}
            selected={graphPeriod}
            onChange={onChangeGraphPeriod}
          />
        </div>

        {graphLoading ? (
          <div className="text-center py-12 text-gray-500">그래프 로딩 중...</div>
        ) : (
          children
        )}
      </div>
    </div>
  );
}

