'use client';

export default function PeriodSelector({ options = [], selected, onChange }) {
  return (
    <div className="flex flex-wrap gap-2">
      {options.map((period) => (
        <button
          key={period}
          type="button"
          onClick={() => onChange(period)}
          className={`px-3 py-1.5 rounded-md text-sm border ${
            selected === period
              ? 'bg-blue-600 border-blue-600 text-white'
              : 'bg-white border-gray-300 text-gray-700 hover:bg-gray-50'
          }`}
        >
          {period === 6 ? '6개월' : `${period / 12}년`}
        </button>
      ))}
    </div>
  );
}

