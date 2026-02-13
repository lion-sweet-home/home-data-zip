'use client';

export default function AreaSelectorCard({
  tradeType,
  saleAreaOptions = [],
  selectedSaleAreaKey,
  onSelectSaleAreaKey,
  rentAreaOptions = [],
  selectedRentAreaKey,
  onSelectRentAreaKey,
}) {
  return (
    <div className="bg-white border border-gray-200 rounded-xl p-5">
      <div className="text-sm font-semibold text-gray-900 mb-3">면적 선택</div>
      <div className="flex flex-wrap gap-2">
        {tradeType === '매매'
          ? saleAreaOptions.map((area) => (
              <button
                key={area.value}
                type="button"
                onClick={() => onSelectSaleAreaKey(String(area.value))}
                className={`px-4 py-2 rounded-lg border text-sm ${
                  String(selectedSaleAreaKey) === String(area.value)
                    ? 'bg-blue-600 text-white border-blue-600'
                    : 'bg-white text-gray-700 border-gray-300 hover:bg-gray-50'
                }`}
              >
                {area.label}㎡
              </button>
            ))
          : rentAreaOptions.map((area) => (
              <button
                key={area.value}
                type="button"
                onClick={() => onSelectRentAreaKey(Number(area.value))}
                className={`px-4 py-2 rounded-lg border text-sm ${
                  Number(selectedRentAreaKey) === Number(area.value)
                    ? 'bg-blue-600 text-white border-blue-600'
                    : 'bg-white text-gray-700 border-gray-300 hover:bg-gray-50'
                }`}
              >
                {area.label}㎡
              </button>
            ))}
      </div>
    </div>
  );
}

