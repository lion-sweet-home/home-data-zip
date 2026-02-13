'use client';

export default function ApartmentTopBar({ tradeType, onBack, onTradeTypeChange }) {
  return (
    <div className="flex flex-wrap gap-2 items-center justify-between">
      <button
        type="button"
        onClick={onBack}
        className="px-3 py-1.5 rounded-md border border-gray-300 text-xs text-gray-700 bg-white hover:bg-gray-50"
      >
        지도로 돌아가기
      </button>

      <div className="flex gap-2">
        <button
          type="button"
          onClick={() => onTradeTypeChange('매매')}
          className={`px-3 py-1.5 rounded-md text-xs font-medium ${
            tradeType === '매매'
              ? 'bg-blue-600 text-white'
              : 'bg-white border border-gray-300 text-gray-700 hover:bg-gray-100'
          }`}
        >
          매매
        </button>
        <button
          type="button"
          onClick={() => onTradeTypeChange('전월세')}
          className={`px-3 py-1.5 rounded-md text-xs font-medium ${
            tradeType === '전월세'
              ? 'bg-blue-600 text-white'
              : 'bg-white border border-gray-300 text-gray-700 hover:bg-gray-100'
          }`}
        >
          전월세
        </button>
      </div>
    </div>
  );
}

