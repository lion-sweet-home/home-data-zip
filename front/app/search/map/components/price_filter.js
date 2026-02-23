'use client';

export default function PriceFilter({
  valueMin = '',
  valueMax = '',
  onChangeMin,
  onChangeMax,
  unitLabel = '만원',
  minPlaceholder = '최소',
  maxPlaceholder = '최대',
  disabled = false,
}) {
  return (
    <>
      <input
        type="number"
        value={valueMin}
        onChange={(e) => onChangeMin?.(e.target.value)}
        placeholder={minPlaceholder}
        disabled={disabled}
        className="px-2 py-1 border border-gray-300 rounded text-xs focus:ring-1 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900 w-20 flex-shrink-0 disabled:bg-gray-100 disabled:cursor-not-allowed disabled:text-gray-600"
      />
      <span className="text-gray-500 text-xs">~</span>
      <input
        type="number"
        value={valueMax}
        onChange={(e) => onChangeMax?.(e.target.value)}
        placeholder={maxPlaceholder}
        disabled={disabled}
        className="px-2 py-1 border border-gray-300 rounded text-xs focus:ring-1 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900 w-20 flex-shrink-0 disabled:bg-gray-100 disabled:cursor-not-allowed disabled:text-gray-600"
      />
      <span className="text-xs text-gray-500 flex-shrink-0">{unitLabel}</span>
    </>
  );
}

