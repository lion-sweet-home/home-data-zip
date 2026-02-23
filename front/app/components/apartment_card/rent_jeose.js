'use client';

function formatManwon(manwon) {
  const n = Number(manwon);
  if (!Number.isFinite(n) || n <= 0) return '-';

  const eok = Math.floor(n / 10000);
  const rest = n % 10000;

  if (eok > 0 && rest > 0) return `${eok}억 ${rest.toLocaleString()}만`;
  if (eok > 0) return `${eok}억`;
  return `${n.toLocaleString()}만`;
}

function formatRate(rate) {
  const n = Number(rate);
  if (!Number.isFinite(n)) return { text: '-', className: 'text-gray-500' };
  const text = `${n > 0 ? '+' : ''}${n.toFixed(1)}%`;
  if (n > 0) return { text, className: 'text-emerald-600' };
  if (n < 0) return { text, className: 'text-red-600' };
  return { text, className: 'text-gray-600' };
}

export default function RentJeonseCard({ item, rank = 1, onClick }) {
  const name = item?.aptName || '아파트';
  const dong = item?.dong || '';
  const exclusive = item?.exclusive != null ? Number(item.exclusive) : null;
  const avg = item?.avgDeposit;
  const count = item?.jeonseCount ?? 0;
  const rate = formatRate(item?.depositChangeRate);

  return (
    <button
      type="button"
      onClick={() => onClick?.(item)}
      className="w-full text-left bg-white border border-gray-200 rounded-2xl shadow-sm hover:shadow transition-shadow p-5"
    >
      <div className="flex items-start justify-between gap-4">
        <div className="flex items-start gap-3 min-w-0">
          <div className="w-11 h-11 rounded-full bg-blue-600 text-white flex items-center justify-center font-bold flex-shrink-0">
            {rank}
          </div>
          <div className="min-w-0">
            <div className="font-semibold text-gray-900 truncate">{name}</div>
            <div className="text-sm text-gray-600 mt-1 truncate">
              {dong || item?.gugun || '-'}
              {exclusive != null ? ` · ${exclusive.toFixed(1)}㎡` : ''}
            </div>
          </div>
        </div>
      </div>

      <div className="mt-4 grid grid-cols-2 gap-y-2 text-sm">
        <div className="text-gray-700">평균 거래가</div>
        <div className="text-right font-semibold text-blue-700">
          {formatManwon(avg)} <span className="text-gray-800 font-medium">원</span>
        </div>

        <div className="text-gray-700">전월 대비</div>
        <div className={`text-right font-semibold ${rate.className}`}>{rate.text}</div>

        <div className="text-gray-700">거래량</div>
        <div className="text-right text-gray-900 font-semibold">{count}건</div>
      </div>
    </button>
  );
}

