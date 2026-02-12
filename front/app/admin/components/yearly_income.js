'use client';

import { useEffect, useMemo, useState } from 'react';
import { getYearlyIncome } from '../../api/admin';

function formatMoneyKRWShort(amount) {
  const n = Number(amount ?? 0);
  if (!Number.isFinite(n)) return 0;
  // 차트는 '만원' 단위로 표시
  return Math.round(n / 10000);
}

function clamp(n, min, max) {
  return Math.max(min, Math.min(max, n));
}

function buildSeriesFromSettlement(list) {
  // backend: SettlementResponse { year, month, amount, ... }
  const map = new Map();
  (Array.isArray(list) ? list : []).forEach((it) => {
    const m = Number(it?.month);
    if (Number.isFinite(m)) map.set(m, it);
  });

  const points = Array.from({ length: 12 }, (_, i) => {
    const month = i + 1;
    const row = map.get(month);
    const amount = row?.amount ?? 0;
    return { month, amount: Number(amount ?? 0) };
  });

  return points;
}

function LineChart({ data, year }) {
  // 간단 SVG 라인 차트 (외부 라이브러리 없이)
  const width = 920;
  const height = 260;
  const padding = { top: 18, right: 18, bottom: 38, left: 48 };

  const values = data.map((d) => formatMoneyKRWShort(d.amount));
  const maxV = Math.max(1, ...values);
  const minV = 0;

  const plotW = width - padding.left - padding.right;
  const plotH = height - padding.top - padding.bottom;

  const x = (i) => padding.left + (plotW * i) / (data.length - 1);
  const y = (v) => padding.top + plotH - (plotH * (v - minV)) / (maxV - minV);

  const coords = values.map((v, i) => [x(i), y(v)]);
  const pointsStr = coords.map(([cx, cy]) => `${cx},${cy}`).join(' ');

  const gridLines = 4;
  const yTicks = Array.from({ length: gridLines + 1 }, (_, i) => {
    const ratio = i / gridLines;
    const v = Math.round(maxV - (maxV * ratio));
    const cy = y(v);
    return { v, cy };
  });

  return (
    <div className="w-full overflow-x-auto">
      <svg width={width} height={height} className="min-w-[720px]">
        {/* grid */}
        {yTicks.map((t, idx) => (
          <g key={idx}>
            <line
              x1={padding.left}
              x2={width - padding.right}
              y1={t.cy}
              y2={t.cy}
              stroke="#E5E7EB"
              strokeDasharray="4 4"
            />
            <text
              x={padding.left - 10}
              y={t.cy + 4}
              textAnchor="end"
              fontSize="11"
              fill="#6B7280"
            >
              {t.v}
            </text>
          </g>
        ))}

        {/* x labels */}
        {data.map((d, i) => (
          <text
            key={d.month}
            x={x(i)}
            y={height - 14}
            textAnchor="middle"
            fontSize="11"
            fill="#6B7280"
          >
            {year ? `${year}-${String(d.month).padStart(2, '0')}` : String(d.month).padStart(2, '0')}
          </text>
        ))}

        {/* line */}
        <polyline fill="none" stroke="#2563EB" strokeWidth="3" points={pointsStr} />

        {/* dots */}
        {coords.map(([cx, cy], idx) => (
          <circle key={idx} cx={cx} cy={cy} r="3.5" fill="#2563EB" />
        ))}
      </svg>
    </div>
  );
}

export default function YearlyIncome() {
  const currentYear = new Date().getFullYear();
  const yearOptions = useMemo(() => {
    // 최근 5년만 제공
    return Array.from({ length: 5 }, (_, i) => currentYear - i);
  }, [currentYear]);

  const [year, setYear] = useState(currentYear);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [series, setSeries] = useState(() =>
    Array.from({ length: 12 }, (_, i) => ({ month: i + 1, amount: 0 }))
  );

  useEffect(() => {
    let alive = true;
    async function run() {
      setLoading(true);
      setError(null);
      try {
        const result = await getYearlyIncome(year);
        if (!alive) return;
        setSeries(buildSeriesFromSettlement(result));
      } catch (e) {
        if (!alive) return;
        setError(e?.message ?? '연도별 수입 데이터를 불러오지 못했습니다.');
      } finally {
        if (!alive) return;
        setLoading(false);
      }
    }
    run();
    return () => {
      alive = false;
    };
  }, [year]);

  const label = useMemo(() => {
    const total = series.reduce((acc, cur) => acc + Number(cur.amount ?? 0), 0);
    return `${formatMoneyKRWShort(total).toLocaleString('ko-KR')}만원`;
  }, [series]);

  return (
    <div className="bg-white border border-gray-200 rounded-2xl p-5 shadow-sm">
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-3">
        <div>
          <h2 className="text-base font-bold text-gray-900">월별 플랫폼 수입</h2>
          <p className="text-sm text-gray-600 mt-1">
            {year}년 합계: <span className="font-semibold text-gray-900">{label}</span> (단위: 만원)
          </p>
        </div>
        <div className="flex items-center gap-2">
          <span className="text-sm text-gray-600">연도</span>
          <select
            value={year}
            onChange={(e) => setYear(clamp(Number(e.target.value), 2000, 3000))}
            className="px-3 py-2 rounded-lg border border-gray-200 bg-white text-sm"
          >
            {yearOptions.map((y) => (
              <option key={y} value={y}>
                {y}
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className="mt-4">
        {error ? (
          <div className="bg-red-50 border border-red-100 text-red-700 rounded-xl px-4 py-3 text-sm">
            {error}
          </div>
        ) : null}

        <div className="mt-3">
          {loading ? (
            <div className="h-[260px] rounded-xl bg-gray-50 border border-gray-100 animate-pulse" />
          ) : (
            <LineChart data={series} year={year} />
          )}
        </div>

        <div className="mt-2 text-xs text-gray-500 flex items-center justify-center gap-2">
          <span className="inline-flex items-center gap-1">
            <span className="inline-block w-2 h-2 rounded-full bg-blue-600" />
            수입 (만원)
          </span>
        </div>
      </div>
    </div>
  );
}
