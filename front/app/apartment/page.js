'use client';

import { Suspense, useEffect, useMemo, useRef, useState, useCallback } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import {
  getAptAreaTypes,
  getApartmentRegion,
  getAreaTypeAvg,
  getNearbySchools,
  getRecentAreaAvg,
} from '../api/apartment';
import { getAptSaleChart, getAptSaleDetail } from '../api/apartment_sale';
import { getRecentRentTrades, getRentDetailByArea, getRentDots } from '../api/apartment_rent';
import SchoolForm from './components/schoolform';
import ApartmentTopBar from './components/top_bar';
import ApartmentSummaryCard from './components/summary_card';
import AreaSelectorCard from './components/area_selector_card';
import TradeHistoryCard from './components/trade_history_card';
import GraphModal from './components/graph_modal';

const PERIOD_OPTIONS = [6, 12, 24, 36, 48];

function parsePeriod(value, fallback = 6) {
  const parsed = Number(value);
  return PERIOD_OPTIONS.includes(parsed) ? parsed : fallback;
}

function getValueByNumericKey(obj, key) {
  if (!obj || key == null) return undefined;
  const direct = obj[key];
  if (direct != null) return direct;

  const target = Number(key);
  if (!Number.isFinite(target)) return undefined;

  const matchedKey = Object.keys(obj).find((k) => {
    const n = Number(k);
    return Number.isFinite(n) && Math.abs(n - target) < 1e-6;
  });

  return matchedKey != null ? obj[matchedKey] : undefined;
}

function normalizeSaleChartRows(rows) {
  if (!Array.isArray(rows)) return [];
  const normalizeDot = (dot) => ({
    // 백엔드 응답 케이스를 모두 허용
    dealDate: dot?.dealDate ?? dot?.date ?? '',
    dealAmount: toNumber(dot?.dealAmount ?? dot?.price ?? dot?.amount ?? 0),
    floor: dot?.floor ?? null,
  });

  const normalized = rows.map((row) => {
    const dots = Array.isArray(row?.dots) ? row.dots.map(normalizeDot) : [];
    const avgFromDots =
      dots.length > 0
        ? Math.round(dots.reduce((sum, d) => sum + toNumber(d?.dealAmount), 0) / dots.length)
        : 0;

    const avgAmount = toNumber(row?.avgAmount ?? row?.averageAmount ?? row?.avg ?? 0);
    const tradeCount = toNumber(row?.tradeCount ?? row?.count ?? 0);

    return {
      month: row?.month ?? row?.yyyymm ?? '',
      // avgAmount가 0이면 dots 가격 평균으로 보정
      avgAmount: avgAmount > 0 ? avgAmount : avgFromDots,
      tradeCount: tradeCount > 0 ? tradeCount : dots.length,
      dots,
    };
  });

  // 거래량 0인 달은 선이 급락하지 않도록 직전 평균값을 유지한다.
  // 단, period 시작 구간이 0이라면 period 내 첫 거래 발생 달의 평균으로 시작값을 맞춘다.
  const firstTraded = normalized.find(
    (row) => toNumber(row?.tradeCount) > 0 && toNumber(row?.avgAmount) > 0
  );
  let carriedAvg = firstTraded ? toNumber(firstTraded.avgAmount) : 0;

  return normalized.map((row) => {
    const hasTrade = toNumber(row?.tradeCount) > 0;
    const avgAmount = toNumber(row?.avgAmount);

    if (hasTrade && avgAmount > 0) {
      carriedAvg = avgAmount;
      return row;
    }

    return {
      ...row,
      avgAmount: carriedAvg,
    };
  });
}

function normalizeRentGraphRows(rows) {
  if (!Array.isArray(rows)) return [];

  // 월별 중복 응답이 와도 안전하게 집계
  const grouped = new Map();
  rows.forEach((row) => {
    const yyyymm = row?.yyyymm ?? row?.month ?? '';
    if (!yyyymm) return;

    if (!grouped.has(yyyymm)) {
      grouped.set(yyyymm, {
        yyyymm,
        jeonseDepositAvg: 0,
        wolseDepositAvg: 0,
        wolseRentAvg: 0,
        jeonseCount: 0,
        wolseCount: 0,
        _jeonseSum: 0,
        _wolseDepositSum: 0,
        _wolseRentSum: 0,
      });
    }

    const target = grouped.get(yyyymm);
    const jeonseAvg = toNumber(row?.jeonseDepositAvg ?? row?.jeonseAvg ?? row?.deposit ?? 0);
    const wolseDepositAvg = toNumber(row?.wolseDepositAvg ?? row?.wolseAvg ?? 0);
    const wolseRentAvg = toNumber(row?.wolseRentAvg ?? row?.monthlyRent ?? row?.mothlyRent ?? 0);
    const jeonseCount = Math.max(0, toNumber(row?.jeonseCount));
    const wolseCount = Math.max(0, toNumber(row?.wolseCount));

    // 월세 0은 전세로 분류: 월세 평균 연산에서 제외
    // count가 없으면 값 유무로 1건으로 간주(폴백 dots 대응)
    const effectiveJeonseCount = jeonseCount > 0 ? jeonseCount : (jeonseAvg > 0 && wolseRentAvg <= 0 ? 1 : 0);
    const effectiveWolseCount = wolseRentAvg > 0 ? (wolseCount > 0 ? wolseCount : 1) : 0;

    target.jeonseCount += effectiveJeonseCount;
    target.wolseCount += effectiveWolseCount;
    target._jeonseSum += jeonseAvg * effectiveJeonseCount;
    target._wolseDepositSum += wolseDepositAvg * effectiveWolseCount;
    target._wolseRentSum += wolseRentAvg * effectiveWolseCount;
  });

  const normalized = [...grouped.values()]
    .sort((a, b) => String(a.yyyymm).localeCompare(String(b.yyyymm)))
    .map((row) => {
      const jeonseDepositAvg = row.jeonseCount > 0 ? Math.round(row._jeonseSum / row.jeonseCount) : 0;
      const wolseDepositAvg = row.wolseCount > 0 ? Math.round(row._wolseDepositSum / row.wolseCount) : 0;
      const wolseRentAvg = row.wolseCount > 0 ? Math.round(row._wolseRentSum / row.wolseCount) : 0;
      return {
        yyyymm: row.yyyymm,
        jeonseDepositAvg,
        wolseDepositAvg,
        wolseRentAvg,
        jeonseCount: row.jeonseCount,
        wolseCount: row.wolseCount,
      };
    });

  // 매매와 동일: 거래량 0이면 직전 평균 유지, 시작 구간 0이면 첫 거래월 평균으로 시작
  const firstJeonse = normalized.find((row) => row.jeonseCount > 0 && toNumber(row.jeonseDepositAvg) > 0);
  const firstWolseDeposit = normalized.find((row) => row.wolseCount > 0 && toNumber(row.wolseDepositAvg) > 0);
  const firstWolseRent = normalized.find((row) => row.wolseCount > 0 && toNumber(row.wolseRentAvg) > 0);

  let carryJeonse = firstJeonse ? toNumber(firstJeonse.jeonseDepositAvg) : 0;
  let carryWolseDeposit = firstWolseDeposit ? toNumber(firstWolseDeposit.wolseDepositAvg) : 0;
  let carryWolseRent = firstWolseRent ? toNumber(firstWolseRent.wolseRentAvg) : 0;

  return normalized.map((row) => {
    const hasJeonseTrade = row.jeonseCount > 0 && toNumber(row.jeonseDepositAvg) > 0;
    if (hasJeonseTrade) carryJeonse = toNumber(row.jeonseDepositAvg);

    const hasWolseTrade = row.wolseCount > 0;
    if (hasWolseTrade && toNumber(row.wolseDepositAvg) > 0) carryWolseDeposit = toNumber(row.wolseDepositAvg);
    if (hasWolseTrade && toNumber(row.wolseRentAvg) > 0) carryWolseRent = toNumber(row.wolseRentAvg);

    return {
      ...row,
      jeonseDepositAvg: hasJeonseTrade ? toNumber(row.jeonseDepositAvg) : carryJeonse,
      wolseDepositAvg: hasWolseTrade && toNumber(row.wolseDepositAvg) > 0 ? toNumber(row.wolseDepositAvg) : carryWolseDeposit,
      wolseRentAvg: hasWolseTrade && toNumber(row.wolseRentAvg) > 0 ? toNumber(row.wolseRentAvg) : carryWolseRent,
    };
  });
}

function normalizeRentDots(rows) {
  if (!Array.isArray(rows)) return [];
  return rows
    .map((row) => ({
      yyyymm: row?.yyyymm ?? row?.month ?? '',
      deposit: toNumber(row?.deposit),
      monthlyRent: toNumber(row?.monthlyRent ?? row?.mothlyRent),
      floor: row?.floor ?? null, // floor 필드 추가
    }))
    .filter((row) => row.yyyymm);
}

function buildRentAreaOptionsFromTrades(trades) {
  if (!Array.isArray(trades)) return [];
  const map = new Map();
  trades.forEach((trade) => {
    const ex = Number(trade?.exclusiveArea);
    if (!Number.isFinite(ex) || ex <= 0) return;
    const areaKey = Math.round(ex * 100);
    if (!map.has(areaKey)) {
      map.set(areaKey, { value: areaKey, label: ex.toFixed(1) });
    }
  });
  return [...map.values()].sort((a, b) => a.value - b.value);
}

function buildRentSummaryFromMonthlyAverages(rows) {
  if (!Array.isArray(rows) || rows.length === 0) return null;

  let jeonseSum = 0;
  let jeonseCount = 0;
  let wolseDepositSum = 0;
  let wolseCount = 0;
  let wolseRentSum = 0;

  rows.forEach((row) => {
    const jc = toNumber(row?.jeonseCount);
    const wc = toNumber(row?.wolseCount);
    jeonseCount += jc;
    wolseCount += wc;
    jeonseSum += toNumber(row?.jeonseDepositAvg) * jc;
    wolseDepositSum += toNumber(row?.wolseDepositAvg) * wc;
    wolseRentSum += toNumber(row?.wolseRentAvg) * wc;
  });

  return {
    jeonseAvg: jeonseCount > 0 ? Math.round(jeonseSum / jeonseCount) : 0,
    wolseAvg: wolseCount > 0 ? Math.round(wolseDepositSum / wolseCount) : 0,
    wolseRentAvg: wolseCount > 0 ? Math.round(wolseRentSum / wolseCount) : 0,
  };
}

function buildRentSummaryFromTrades(trades) {
  if (!Array.isArray(trades) || trades.length === 0) return null;

  let jeonseSum = 0;
  let jeonseCount = 0;
  let wolseDepositSum = 0;
  let wolseCount = 0;
  let wolseRentSum = 0;

  trades.forEach((trade) => {
    const deposit = toNumber(trade?.deposit);
    const monthlyRent = toNumber(trade?.monthlyRent);

    if (isJeonseByMonthlyRent(monthlyRent)) {
      jeonseSum += deposit;
      jeonseCount += 1;
      return;
    }

    wolseDepositSum += deposit;
    wolseRentSum += monthlyRent;
    wolseCount += 1;
  });

  return {
    jeonseAvg: jeonseCount > 0 ? Math.round(jeonseSum / jeonseCount) : 0,
    wolseAvg: wolseCount > 0 ? Math.round(wolseDepositSum / wolseCount) : 0,
    wolseRentAvg: wolseCount > 0 ? Math.round(wolseRentSum / wolseCount) : 0,
  };
}

function hasMeaningfulRentSummary(summary) {
  if (!summary) return false;
  return (
    toNumber(summary.jeonseAvg) > 0 ||
    toNumber(summary.wolseAvg) > 0 ||
    toNumber(summary.wolseRentAvg) > 0
  );
}

function filterRecentTradesByArea(trades, selectedAreaKey) {
  if (!Array.isArray(trades) || !selectedAreaKey) return [];
  const selectedExclusive = Number(selectedAreaKey) / 100;
  return trades.filter((trade) => {
    const ex = Number(trade?.exclusiveArea);
    return Number.isFinite(ex) && Math.abs(ex - selectedExclusive) < 0.11;
  });
}

function parseSchoolLevels(raw) {
  if (!raw) return [];
  return String(raw)
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean);
}

function toNumber(value) {
  const n = Number(value);
  return Number.isFinite(n) ? n : 0;
}

function isJeonseByMonthlyRent(monthlyRent) {
  return toNumber(monthlyRent) <= 0;
}

function formatDate(value) {
  if (!value) return '-';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) {
    return String(value).replaceAll('-', '.');
  }
  return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, '0')}.${String(d.getDate()).padStart(2, '0')}`;
}

function formatMonth(yyyymm) {
  if (!yyyymm || String(yyyymm).length < 6) return '-';
  const v = String(yyyymm);
  return `${v.slice(0, 4)}.${v.slice(4, 6)}`;
}

function formatPrice(value) {
  if (value == null) return '-';
  const n = Number(value);
  if (!Number.isFinite(n)) return '-';

  // 일부 API는 원 단위, 일부는 만원 단위를 사용하므로 안전하게 보정
  const manwon = n >= 1_000_000 ? Math.round(n / 10000) : Math.round(n);
  const eok = Math.floor(manwon / 10000);
  const rest = manwon % 10000;

  if (eok > 0 && rest > 0) return `${eok}억 ${rest.toLocaleString()}만`;
  if (eok > 0) return `${eok}억`;
  return `${manwon.toLocaleString()}만`;
}

function formatAvgPrice(value) {
  const n = Number(value);
  if (!Number.isFinite(n) || n <= 0) return '-';
  return formatPrice(n);
}

const CHART_WIDTH = 960;
const CHART_HEIGHT = 320;
const CHART_PADDING_X = 36;
const CHART_PADDING_Y = 24;

function createLinePath(points) {
  if (!points || points.length === 0) return '';
  return points
    .map((point, index) => `${index === 0 ? 'M' : 'L'} ${point.x.toFixed(1)} ${point.y.toFixed(1)}`)
    .join(' ');
}

function buildYScaler(maxValue) {
  const safeMax = maxValue > 0 ? maxValue : 1;
  const plotHeight = CHART_HEIGHT - CHART_PADDING_Y * 2;
  return (value) => CHART_HEIGHT - CHART_PADDING_Y - (toNumber(value) / safeMax) * plotHeight;
}

function buildMonthX(index, count) {
  if (count <= 1) return CHART_WIDTH / 2;
  const plotWidth = CHART_WIDTH - CHART_PADDING_X * 2;
  return CHART_PADDING_X + (index * plotWidth) / (count - 1);
}

function SaleGraph({ data }) {
  const [tooltip, setTooltip] = useState(null);
  const tooltipRef = useRef(null);

  if (!data?.length) {
    return <div className="w-full text-center text-gray-500 py-10">차트 데이터가 없습니다.</div>;
  }

  const maxAvg = Math.max(...data.map((d) => toNumber(d?.avgAmount)), 0);
  const maxDot = Math.max(
    ...data.flatMap((monthRow) => (monthRow?.dots || []).map((dot) => toNumber(dot?.dealAmount))),
    0
  );
  const maxValue = Math.max(maxAvg, maxDot, 1);
  const scaleY = buildYScaler(maxValue);

  const avgPoints = data.map((row, index) => ({
    x: buildMonthX(index, data.length),
    y: scaleY(row?.avgAmount),
    value: toNumber(row?.avgAmount),
    month: row?.month,
  }));

  const tradeDots = data.flatMap((row, monthIndex) => {
    const centerX = buildMonthX(monthIndex, data.length);
    return (row?.dots || []).map((dot, dotIndex) => ({
      x: centerX + ((dotIndex % 5) - 2) * 2,
      y: scaleY(dot?.dealAmount),
      value: toNumber(dot?.dealAmount),
      month: row?.month,
      dealDate: dot?.dealDate,
      floor: dot?.floor,
    }));
  });

  // useCallback으로 최적화
  const handleDotMouseEnter = useCallback((dot) => {
    setTooltip({ data: dot, x: dot.x, y: dot.y });
  }, []);

  const handleDotMouseLeave = useCallback(() => {
    setTooltip(null);
  }, []);

  const yGuideValues = [1, 0.75, 0.5, 0.25, 0].map((ratio) => Math.round(maxValue * ratio));

  return (
    <div className="space-y-2">
      <div className="flex items-center gap-4 text-xs text-gray-600">
        <span className="inline-flex items-center gap-1">
          <span className="w-2.5 h-2.5 rounded-full bg-blue-600" />
          월평균가
        </span>
        <span className="inline-flex items-center gap-1">
          <span className="w-2.5 h-2.5 rounded-full bg-gray-500" />
          개별 거래(dots)
        </span>
      </div>
      <svg 
        viewBox={`0 0 ${CHART_WIDTH} ${CHART_HEIGHT}`} 
        className="w-full h-72 bg-white rounded-lg border border-gray-100"
      >
        {yGuideValues.map((value, index) => {
          const y = scaleY(value);
          return (
            <g key={`sale-guide-${index}-${value}`}>
              <line x1={CHART_PADDING_X} y1={y} x2={CHART_WIDTH - CHART_PADDING_X} y2={y} stroke="#E5E7EB" strokeWidth="1" />
              <text x={6} y={y + 4} fontSize="10" fill="#9CA3AF">
                {formatPrice(value)}
              </text>
            </g>
          );
        })}

        <path d={createLinePath(avgPoints)} fill="none" stroke="#2563EB" strokeWidth="2.2" />

        {avgPoints.map((point, idx) => (
          <circle
            key={`${point.month}-${idx}`}
            cx={point.x}
            cy={point.y}
            r="4.2"
            fill="#2563EB"
          >
            <title>{`${formatMonth(point.month)} 평균 ${formatPrice(point.value)}`}</title>
          </circle>
        ))}

        {tradeDots.map((dot, idx) => (
          <g key={`${dot.month}-${dot.dealDate || idx}-${idx}`}>
            {/* 히트박스: 투명한 큰 circle (hover 영역 확대) */}
            <circle
              cx={dot.x}
              cy={dot.y}
              r="10"
              fill="transparent"
              onMouseEnter={() => handleDotMouseEnter(dot)}
              onMouseLeave={handleDotMouseLeave}
              style={{ cursor: 'pointer' }}
            />
            {/* 실제 점: 조금 더 큰 circle */}
            <circle
              cx={dot.x}
              cy={dot.y}
              r="3"
              fill="#6B7280"
              opacity="0.85"
              style={{ pointerEvents: 'none' }}
            />
          </g>
        ))}

        {/* 커스텀 툴팁: 점 위에 고정 (말풍선 스타일) */}
        {tooltip && tooltip.data && (() => {
          const tooltipWidth = 180;
          const tooltipHeight = tooltipRef.current?.offsetHeight || 60;
          const tailHeight = 10;
          const gap = 6;
          const padding = 10;
          
          const dotX = tooltip.x;
          const dotY = tooltip.y;
          
          // X 위치: 점의 x 좌표를 중심으로 배치 (화면 경계 고려)
          const tooltipX = Math.min(Math.max(dotX - tooltipWidth / 2, padding), CHART_WIDTH - tooltipWidth - padding);
          
          // Y 위치: 위/아래 플립 로직
          const topIfAbove = dotY - (tooltipHeight + tailHeight + gap);
          const topIfBelow = dotY + (tailHeight + gap);
          
          // 위쪽 공간이 부족하면 아래에 배치
          const placement = topIfAbove < padding ? 'bottom' : 'top';
          
          let tooltipY;
          if (placement === 'top') {
            tooltipY = Math.max(Math.min(topIfAbove, CHART_HEIGHT - tooltipHeight - tailHeight - padding), padding);
          } else {
            tooltipY = Math.max(Math.min(topIfBelow, CHART_HEIGHT - tooltipHeight - padding), padding);
          }
          
          // 꼬리 위치 계산: 점의 x 좌표에 맞춤
          const dotXInTooltip = dotX - tooltipX;
          const tailLeft = Math.max(8, Math.min(dotXInTooltip, tooltipWidth - 8));
          
          return (
            <g style={{ pointerEvents: 'none' }}>
              <foreignObject
                x={tooltipX}
                y={tooltipY}
                width={tooltipWidth}
                height={tooltipHeight + tailHeight}
              >
                <div className="relative" ref={tooltipRef}>
                  {/* 말풍선 본체 */}
                  <div className="bg-gray-900 text-white text-xs rounded-lg p-2 shadow-xl">
                    <div className="text-[10px] text-gray-300 mb-1">
                      {tooltip.data.dealDate ? formatDate(tooltip.data.dealDate) : (tooltip.data.month ? formatMonth(tooltip.data.month) : '-')}
                    </div>
                    {tooltip.data.floor != null && (
                      <div className="text-[10px] text-gray-300 mb-1">
                        {tooltip.data.floor}층
                      </div>
                    )}
                    <div className="font-medium">
                      매매가: {formatPrice(tooltip.data.value)}
                    </div>
                  </div>
                  {/* 말풍선 꼬리: placement에 따라 방향 변경 */}
                  {placement === 'top' ? (
                    <div 
                      className="absolute top-full"
                      style={{
                        left: `${tailLeft}px`,
                        transform: 'translateX(-50%)',
                        width: 0,
                        height: 0,
                        borderLeft: '6px solid transparent',
                        borderRight: '6px solid transparent',
                        borderTop: `${tailHeight}px solid rgb(17, 24, 39)`,
                      }}
                    />
                  ) : (
                    <div 
                      className="absolute bottom-full"
                      style={{
                        left: `${tailLeft}px`,
                        transform: 'translateX(-50%)',
                        width: 0,
                        height: 0,
                        borderLeft: '6px solid transparent',
                        borderRight: '6px solid transparent',
                        borderBottom: `${tailHeight}px solid rgb(17, 24, 39)`,
                      }}
                    />
                  )}
                </div>
              </foreignObject>
            </g>
          );
        })()}
      </svg>

      <div className="flex justify-between gap-2 text-[11px] text-gray-500">
        {data.map((item, idx) => (
          <span key={`${item?.month || idx}-${idx}`}>{formatMonth(item?.month)}</span>
        ))}
      </div>
    </div>
  );
}

function RentGraph({ avgData, dotData, mode = 'jeonse' }) {
  const [tooltip, setTooltip] = useState(null);
  const [wolseDepositTooltip, setWolseDepositTooltip] = useState(null);
  const [wolseRentTooltip, setWolseRentTooltip] = useState(null);
  const jeonseTooltipRef = useRef(null);
  const wolseDepositTooltipRef = useRef(null);
  const wolseRentTooltipRef = useRef(null);

  const monthSet = new Set([
    ...(avgData || []).map((r) => r?.yyyymm).filter(Boolean),
    ...(dotData || []).map((r) => r?.yyyymm).filter(Boolean),
  ]);
  const months = [...monthSet].sort((a, b) => String(a).localeCompare(String(b)));

  if (months.length === 0) {
    return <div className="w-full text-center text-gray-500 py-10">차트 데이터가 없습니다.</div>;
  }

  const avgMap = new Map((avgData || []).map((row) => [row.yyyymm, row]));
  const dotsByMonth = new Map();
  (dotData || []).forEach((dot) => {
    if (!dotsByMonth.has(dot.yyyymm)) dotsByMonth.set(dot.yyyymm, []);
    dotsByMonth.get(dot.yyyymm).push(dot);
  });

  // useCallback으로 최적화된 핸들러
  const handleJeonseDotEnter = useCallback((dot) => {
    setTooltip({ data: dot, x: dot.x, y: dot.y });
  }, []);

  const handleJeonseDotLeave = useCallback(() => {
    setTooltip(null);
  }, []);

  const handleWolseDepositDotEnter = useCallback((dot) => {
    setWolseDepositTooltip({ data: dot, x: dot.x, y: dot.depositY });
  }, []);

  const handleWolseDepositDotLeave = useCallback(() => {
    setWolseDepositTooltip(null);
  }, []);

  const handleWolseRentDotEnter = useCallback((dot) => {
    setWolseRentTooltip({ data: dot, x: dot.x, y: dot.monthlyRentY });
  }, []);

  const handleWolseRentDotLeave = useCallback(() => {
    setWolseRentTooltip(null);
  }, []);

  const jeonseLine = months.map((month) => ({ month, value: toNumber(avgMap.get(month)?.jeonseDepositAvg) }));
  const wolseDepositLine = months.map((month) => ({ month, value: toNumber(avgMap.get(month)?.wolseDepositAvg) }));
  const wolseRentLine = months.map((month) => ({ month, value: toNumber(avgMap.get(month)?.wolseRentAvg) }));

  const jeonseMax = Math.max(
    ...jeonseLine.map((p) => p.value),
    ...months.flatMap((m) => (dotsByMonth.get(m) || []).filter((d) => isJeonseByMonthlyRent(d?.monthlyRent)).map((d) => toNumber(d?.deposit))),
    1
  );
  const wolseDepositMax = Math.max(
    ...wolseDepositLine.map((p) => p.value),
    ...months.flatMap((m) => (dotsByMonth.get(m) || []).filter((d) => !isJeonseByMonthlyRent(d?.monthlyRent)).map((d) => toNumber(d?.deposit))),
    1
  );
  const wolseRentMax = Math.max(
    ...wolseRentLine.map((p) => p.value),
    ...months.flatMap((m) => (dotsByMonth.get(m) || []).filter((d) => !isJeonseByMonthlyRent(d?.monthlyRent)).map((d) => toNumber(d?.monthlyRent))),
    1
  );

  const jeonseScaleY = buildYScaler(jeonseMax);
  const wolseDepositScaleY = buildYScaler(wolseDepositMax);
  const wolseRentScaleY = buildYScaler(wolseRentMax);
  const jeonseGuide = [1, 0.75, 0.5, 0.25, 0].map((r) => Math.round(jeonseMax * r));
  const wolseDepositGuide = [1, 0.75, 0.5, 0.25, 0].map((r) => Math.round(wolseDepositMax * r));
  const wolseRentGuide = [1, 0.75, 0.5, 0.25, 0].map((r) => Math.round(wolseRentMax * r));

  const jeonseLinePoints = jeonseLine.map((p, idx) => ({ x: buildMonthX(idx, months.length), y: jeonseScaleY(p.value), value: p.value, month: p.month }));
  const wolseDepositLinePoints = wolseDepositLine.map((p, idx) => ({ x: buildMonthX(idx, months.length), y: wolseDepositScaleY(p.value), value: p.value, month: p.month }));
  const wolseRentLinePoints = wolseRentLine.map((p, idx) => ({ x: buildMonthX(idx, months.length), y: wolseRentScaleY(p.value), value: p.value, month: p.month }));

  const jeonseDots = months.flatMap((month, monthIdx) => {
    const centerX = buildMonthX(monthIdx, months.length);
    return (dotsByMonth.get(month) || [])
      .filter((d) => isJeonseByMonthlyRent(d?.monthlyRent))
      .map((dot, dotIdx) => ({
        x: centerX + ((dotIdx % 5) - 2) * 2,
        y: jeonseScaleY(dot?.deposit),
        value: toNumber(dot?.deposit),
        month,
        deposit: toNumber(dot?.deposit),
        monthlyRent: null, // 전세는 월세 없음
        floor: dot?.floor ?? null,
        dealDate: dot?.dealDate || formatMonth(month), // yyyymm을 날짜 형식으로 변환
      }));
  });

  const wolseDots = months.flatMap((month, monthIdx) => {
    const centerX = buildMonthX(monthIdx, months.length);
    return (dotsByMonth.get(month) || [])
      .filter((d) => !isJeonseByMonthlyRent(d?.monthlyRent))
      .map((dot, dotIdx) => ({
        x: centerX + ((dotIdx % 5) - 2) * 2,
        depositY: wolseDepositScaleY(dot?.deposit),
        monthlyRentY: wolseRentScaleY(dot?.monthlyRent),
        depositValue: toNumber(dot?.deposit),
        monthlyRentValue: toNumber(dot?.monthlyRent),
        month,
        deposit: toNumber(dot?.deposit),
        monthlyRent: toNumber(dot?.monthlyRent),
        floor: dot?.floor ?? null,
        dealDate: dot?.dealDate || formatMonth(month), // yyyymm을 날짜 형식으로 변환
      }));
  });

  return (
    <div className="space-y-6">
      {mode === 'jeonse' && (
        <div className="space-y-2">
          <div className="flex items-center gap-4 text-xs text-gray-600">
            <span className="inline-flex items-center gap-1"><span className="w-2.5 h-2.5 rounded-full bg-blue-600" />전세 월 평균가</span>
            <span className="inline-flex items-center gap-1"><span className="w-2.5 h-2.5 rounded-full bg-gray-500" />전세 거래 점(dots)</span>
          </div>
          <svg 
            viewBox={`0 0 ${CHART_WIDTH} ${CHART_HEIGHT}`} 
            className="w-full h-72 bg-white rounded-lg border border-gray-100"
          >
            {jeonseGuide.map((value, index) => {
              const y = jeonseScaleY(value);
              return (
                <g key={`rent-jeonse-guide-${index}-${value}`}>
                  <line x1={CHART_PADDING_X} y1={y} x2={CHART_WIDTH - CHART_PADDING_X} y2={y} stroke="#E5E7EB" strokeWidth="1" />
                  <text x={6} y={y + 4} fontSize="10" fill="#9CA3AF">{formatPrice(value)}</text>
                </g>
              );
            })}
            <path d={createLinePath(jeonseLinePoints)} fill="none" stroke="#2563EB" strokeWidth="2.2" />
            {jeonseLinePoints.map((point, idx) => (
              <circle key={`rent-jeonse-line-${idx}`} cx={point.x} cy={point.y} r="3.4" fill="#2563EB"><title>{`${formatMonth(point.month)} 전세 평균 ${formatPrice(point.value)}`}</title></circle>
            ))}
            {jeonseDots.map((dot, idx) => (
              <g key={`rent-jeonse-dot-${idx}`}>
                {/* 히트박스: 투명한 큰 circle */}
                <circle
                  cx={dot.x}
                  cy={dot.y}
                  r="10"
                  fill="transparent"
                  onMouseEnter={() => handleJeonseDotEnter(dot)}
                  onMouseLeave={handleJeonseDotLeave}
                  style={{ cursor: 'pointer' }}
                />
                {/* 실제 점: 조금 더 큰 circle */}
                <circle
                  cx={dot.x}
                  cy={dot.y}
                  r="3"
                  fill="#6B7280"
                  opacity="0.9"
                  style={{ pointerEvents: 'none' }}
                />
              </g>
            ))}
            {/* 전세 툴팁: 점 위에 고정 */}
            {tooltip && tooltip.data && tooltip.data.monthlyRent === null && (() => {
              const tooltipWidth = 180;
              const tooltipHeight = jeonseTooltipRef.current?.offsetHeight || 60;
              const tailHeight = 10;
              const gap = 6;
              const padding = 10;
              
              const dotX = tooltip.x;
              const dotY = tooltip.y;
              
              const tooltipX = Math.min(Math.max(dotX - tooltipWidth / 2, padding), CHART_WIDTH - tooltipWidth - padding);
              
              const topIfAbove = dotY - (tooltipHeight + tailHeight + gap);
              const topIfBelow = dotY + (tailHeight + gap);
              
              const placement = topIfAbove < padding ? 'bottom' : 'top';
              
              let tooltipY;
              if (placement === 'top') {
                tooltipY = Math.max(Math.min(topIfAbove, CHART_HEIGHT - tooltipHeight - tailHeight - padding), padding);
              } else {
                tooltipY = Math.max(Math.min(topIfBelow, CHART_HEIGHT - tooltipHeight - padding), padding);
              }
              
              const dotXInTooltip = dotX - tooltipX;
              const tailLeft = Math.max(8, Math.min(dotXInTooltip, tooltipWidth - 8));
              
              return (
                <g style={{ pointerEvents: 'none' }}>
                  <foreignObject
                    x={tooltipX}
                    y={tooltipY}
                    width={tooltipWidth}
                    height={tooltipHeight + tailHeight}
                  >
                    <div className="relative" ref={jeonseTooltipRef}>
                      <div className="bg-gray-900 text-white text-xs rounded-lg p-2 shadow-xl">
                        <div className="text-[10px] text-gray-300 mb-1">
                          {tooltip.data.dealDate ? (tooltip.data.dealDate.includes('-') ? formatDate(tooltip.data.dealDate) : formatMonth(tooltip.data.dealDate)) : (tooltip.data.month ? formatMonth(tooltip.data.month) : '-')}
                        </div>
                        {tooltip.data.floor != null && (
                          <div className="text-[10px] text-gray-300 mb-1">
                            {tooltip.data.floor}층
                          </div>
                        )}
                        <div className="font-medium">
                          보증금: {formatPrice(tooltip.data.deposit)}
                        </div>
                      </div>
                      {placement === 'top' ? (
                        <div 
                          className="absolute top-full"
                          style={{
                            left: `${tailLeft}px`,
                            transform: 'translateX(-50%)',
                            width: 0,
                            height: 0,
                            borderLeft: '6px solid transparent',
                            borderRight: '6px solid transparent',
                            borderTop: `${tailHeight}px solid rgb(17, 24, 39)`,
                          }}
                        />
                      ) : (
                        <div 
                          className="absolute bottom-full"
                          style={{
                            left: `${tailLeft}px`,
                            transform: 'translateX(-50%)',
                            width: 0,
                            height: 0,
                            borderLeft: '6px solid transparent',
                            borderRight: '6px solid transparent',
                            borderBottom: `${tailHeight}px solid rgb(17, 24, 39)`,
                          }}
                        />
                      )}
                    </div>
                  </foreignObject>
                </g>
              );
            })()}
          </svg>
        </div>
      )}

      {mode === 'wolse' && (
        <>
          <div className="space-y-2">
            <div className="flex items-center gap-4 text-xs text-gray-600">
              <span className="inline-flex items-center gap-1"><span className="w-2.5 h-2.5 rounded-full bg-emerald-600" />월별 보증금 평균</span>
              <span className="inline-flex items-center gap-1"><span className="w-2.5 h-2.5 rounded-full bg-gray-500" />보증금 거래 점(dots)</span>
            </div>
            <svg 
              viewBox={`0 0 ${CHART_WIDTH} ${CHART_HEIGHT}`} 
              className="w-full h-72 bg-white rounded-lg border border-gray-100"
            >
              {wolseDepositGuide.map((value, index) => {
                const y = wolseDepositScaleY(value);
                return (
                  <g key={`rent-wolse-deposit-guide-${index}-${value}`}>
                    <line x1={CHART_PADDING_X} y1={y} x2={CHART_WIDTH - CHART_PADDING_X} y2={y} stroke="#E5E7EB" strokeWidth="1" />
                    <text x={6} y={y + 4} fontSize="10" fill="#9CA3AF">{formatPrice(value)}</text>
                  </g>
                );
              })}
              <path d={createLinePath(wolseDepositLinePoints)} fill="none" stroke="#059669" strokeWidth="2.2" />
              {wolseDepositLinePoints.map((point, idx) => (
                <circle key={`rent-wolse-deposit-line-${idx}`} cx={point.x} cy={point.y} r="4.2" fill="#059669"><title>{`${formatMonth(point.month)} 월세 보증금 평균 ${formatPrice(point.value)}`}</title></circle>
              ))}
              {wolseDots.map((dot, idx) => (
                <g key={`rent-wolse-deposit-dot-${idx}`}>
                  {/* 히트박스 */}
                  <circle
                    cx={dot.x}
                    cy={dot.depositY}
                    r="10"
                    fill="transparent"
                    onMouseEnter={() => handleWolseDepositDotEnter(dot)}
                    onMouseLeave={handleWolseDepositDotLeave}
                    style={{ cursor: 'pointer' }}
                  />
                  {/* 실제 점: 조금 더 큰 circle */}
                  <circle
                    cx={dot.x}
                    cy={dot.depositY}
                    r="3"
                    fill="#6B7280"
                    opacity="0.9"
                    style={{ pointerEvents: 'none' }}
                  />
                </g>
              ))}
              {/* 월세 보증금 툴팁: 점 위에 고정 */}
              {wolseDepositTooltip && wolseDepositTooltip.data && wolseDepositTooltip.data.monthlyRent != null && (() => {
                const tooltipWidth = 180;
                const tooltipHeight = wolseDepositTooltipRef.current?.offsetHeight || 70;
                const tailHeight = 10;
                const gap = 6;
                const padding = 10;
                
                const dotX = wolseDepositTooltip.x;
                const dotY = wolseDepositTooltip.y;
                
                const tooltipX = Math.min(Math.max(dotX - tooltipWidth / 2, padding), CHART_WIDTH - tooltipWidth - padding);
                
                const topIfAbove = dotY - (tooltipHeight + tailHeight + gap);
                const topIfBelow = dotY + (tailHeight + gap);
                
                const placement = topIfAbove < padding ? 'bottom' : 'top';
                
                let tooltipY;
                if (placement === 'top') {
                  tooltipY = Math.max(Math.min(topIfAbove, CHART_HEIGHT - tooltipHeight - tailHeight - padding), padding);
                } else {
                  tooltipY = Math.max(Math.min(topIfBelow, CHART_HEIGHT - tooltipHeight - padding), padding);
                }
                
                const dotXInTooltip = dotX - tooltipX;
                const tailLeft = Math.max(8, Math.min(dotXInTooltip, tooltipWidth - 8));
                
                return (
                  <g style={{ pointerEvents: 'none' }}>
                    <foreignObject
                      x={tooltipX}
                      y={tooltipY}
                      width={tooltipWidth}
                      height={tooltipHeight + tailHeight}
                    >
                      <div className="relative" ref={wolseDepositTooltipRef}>
                        <div className="bg-gray-900 text-white text-xs rounded-lg p-2 shadow-xl">
                          <div className="text-[10px] text-gray-300 mb-1">
                            {wolseDepositTooltip.data.dealDate ? (wolseDepositTooltip.data.dealDate.includes('-') ? formatDate(wolseDepositTooltip.data.dealDate) : formatMonth(wolseDepositTooltip.data.dealDate)) : (wolseDepositTooltip.data.month ? formatMonth(wolseDepositTooltip.data.month) : '-')}
                          </div>
                          {wolseDepositTooltip.data.floor != null && (
                            <div className="text-[10px] text-gray-300 mb-1">
                              {wolseDepositTooltip.data.floor}층
                            </div>
                          )}
                          <div className="font-medium mb-1">
                            보증금: {formatPrice(wolseDepositTooltip.data.deposit)}
                          </div>
                          <div className="font-medium">
                            월세: {formatPrice(wolseDepositTooltip.data.monthlyRent)}
                          </div>
                        </div>
                        {placement === 'top' ? (
                          <div 
                            className="absolute top-full"
                            style={{
                              left: `${tailLeft}px`,
                              transform: 'translateX(-50%)',
                              width: 0,
                              height: 0,
                              borderLeft: '6px solid transparent',
                              borderRight: '6px solid transparent',
                              borderTop: `${tailHeight}px solid rgb(17, 24, 39)`,
                            }}
                          />
                        ) : (
                          <div 
                            className="absolute bottom-full"
                            style={{
                              left: `${tailLeft}px`,
                              transform: 'translateX(-50%)',
                              width: 0,
                              height: 0,
                              borderLeft: '6px solid transparent',
                              borderRight: '6px solid transparent',
                              borderBottom: `${tailHeight}px solid rgb(17, 24, 39)`,
                            }}
                          />
                        )}
                      </div>
                    </foreignObject>
                  </g>
                );
              })()}
            </svg>
          </div>

          <div className="space-y-2">
            <div className="flex items-center gap-4 text-xs text-gray-600">
              <span className="inline-flex items-center gap-1"><span className="w-2.5 h-2.5 rounded-full bg-orange-500" />월별 월세 평균</span>
              <span className="inline-flex items-center gap-1"><span className="w-2.5 h-2.5 rounded-full bg-gray-500" />월세 거래 점(dots)</span>
            </div>
            <svg 
              viewBox={`0 0 ${CHART_WIDTH} ${CHART_HEIGHT}`} 
              className="w-full h-72 bg-white rounded-lg border border-gray-100"
            >
              {wolseRentGuide.map((value, index) => {
                const y = wolseRentScaleY(value);
                return (
                  <g key={`rent-wolse-rent-guide-${index}-${value}`}>
                    <line x1={CHART_PADDING_X} y1={y} x2={CHART_WIDTH - CHART_PADDING_X} y2={y} stroke="#E5E7EB" strokeWidth="1" />
                    <text x={6} y={y + 4} fontSize="10" fill="#9CA3AF">{formatPrice(value)}</text>
                  </g>
                );
              })}
              <path d={createLinePath(wolseRentLinePoints)} fill="none" stroke="#F97316" strokeWidth="2.2" />
              {wolseRentLinePoints.map((point, idx) => (
                <circle key={`rent-wolse-rent-line-${idx}`} cx={point.x} cy={point.y} r="4.2" fill="#F97316"><title>{`${formatMonth(point.month)} 월세 평균 ${formatPrice(point.value)}`}</title></circle>
              ))}
              {wolseDots.map((dot, idx) => (
                <g key={`rent-wolse-rent-dot-${idx}`}>
                  {/* 히트박스 */}
                  <circle
                    cx={dot.x}
                    cy={dot.monthlyRentY}
                    r="10"
                    fill="transparent"
                    onMouseEnter={() => handleWolseRentDotEnter(dot)}
                    onMouseLeave={handleWolseRentDotLeave}
                    style={{ cursor: 'pointer' }}
                  />
                  {/* 실제 점: 조금 더 큰 circle */}
                  <circle
                    cx={dot.x}
                    cy={dot.monthlyRentY}
                    r="3"
                    fill="#6B7280"
                    opacity="0.9"
                    style={{ pointerEvents: 'none' }}
                  />
                </g>
              ))}
              {/* 월세 월세 툴팁: 점 위에 고정 */}
              {wolseRentTooltip && wolseRentTooltip.data && wolseRentTooltip.data.monthlyRent != null && (() => {
                const tooltipWidth = 180;
                const tooltipHeight = wolseRentTooltipRef.current?.offsetHeight || 70;
                const tailHeight = 10;
                const gap = 6;
                const padding = 10;
                
                const dotX = wolseRentTooltip.x;
                const dotY = wolseRentTooltip.y;
                
                const tooltipX = Math.min(Math.max(dotX - tooltipWidth / 2, padding), CHART_WIDTH - tooltipWidth - padding);
                
                const topIfAbove = dotY - (tooltipHeight + tailHeight + gap);
                const topIfBelow = dotY + (tailHeight + gap);
                
                const placement = topIfAbove < padding ? 'bottom' : 'top';
                
                let tooltipY;
                if (placement === 'top') {
                  tooltipY = Math.max(Math.min(topIfAbove, CHART_HEIGHT - tooltipHeight - tailHeight - padding), padding);
                } else {
                  tooltipY = Math.max(Math.min(topIfBelow, CHART_HEIGHT - tooltipHeight - padding), padding);
                }
                
                const dotXInTooltip = dotX - tooltipX;
                const tailLeft = Math.max(8, Math.min(dotXInTooltip, tooltipWidth - 8));
                
                return (
                  <g style={{ pointerEvents: 'none' }}>
                    <foreignObject
                      x={tooltipX}
                      y={tooltipY}
                      width={tooltipWidth}
                      height={tooltipHeight + tailHeight}
                    >
                      <div className="relative" ref={wolseRentTooltipRef}>
                        <div className="bg-gray-900 text-white text-xs rounded-lg p-2 shadow-xl">
                          <div className="text-[10px] text-gray-300 mb-1">
                            {wolseRentTooltip.data.dealDate ? (wolseRentTooltip.data.dealDate.includes('-') ? formatDate(wolseRentTooltip.data.dealDate) : formatMonth(wolseRentTooltip.data.dealDate)) : (wolseRentTooltip.data.month ? formatMonth(wolseRentTooltip.data.month) : '-')}
                          </div>
                          {wolseRentTooltip.data.floor != null && (
                            <div className="text-[10px] text-gray-300 mb-1">
                              {wolseRentTooltip.data.floor}층
                            </div>
                          )}
                          <div className="font-medium mb-1">
                            보증금: {formatPrice(wolseRentTooltip.data.deposit)}
                          </div>
                          <div className="font-medium">
                            월세: {formatPrice(wolseRentTooltip.data.monthlyRent)}
                          </div>
                        </div>
                        {placement === 'top' ? (
                          <div 
                            className="absolute top-full"
                            style={{
                              left: `${tailLeft}px`,
                              transform: 'translateX(-50%)',
                              width: 0,
                              height: 0,
                              borderLeft: '6px solid transparent',
                              borderRight: '6px solid transparent',
                              borderTop: `${tailHeight}px solid rgb(17, 24, 39)`,
                            }}
                          />
                        ) : (
                          <div 
                            className="absolute bottom-full"
                            style={{
                              left: `${tailLeft}px`,
                              transform: 'translateX(-50%)',
                              width: 0,
                              height: 0,
                              borderLeft: '6px solid transparent',
                              borderRight: '6px solid transparent',
                              borderBottom: `${tailHeight}px solid rgb(17, 24, 39)`,
                            }}
                          />
                        )}
                      </div>
                    </foreignObject>
                  </g>
                );
              })()}
            </svg>
          </div>
        </>
      )}

      <div className="flex justify-between gap-2 text-[11px] text-gray-500">
        {months.map((month, idx) => (
          <span key={`rent-month-${month}-${idx}`}>{formatMonth(month)}</span>
        ))}
      </div>
    </div>
  );
}

function ApartmentPageContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const isInternalUrlSyncRef = useRef(false);
  const searchParamsKey = searchParams.toString();

  const aptId = searchParams.get('aptId');
  const schoolLevels = parseSchoolLevels(searchParams.get('schoolTypes'));

  const [tradeType, setTradeType] = useState(searchParams.get('tradeType') || '매매');
  const [aptName, setAptName] = useState(searchParams.get('aptName') || '아파트');
  const [address, setAddress] = useState(searchParams.get('address') || '');

  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const [saleDetail, setSaleDetail] = useState(null);
  const [saleAreaOptions, setSaleAreaOptions] = useState([]);
  const [selectedSaleAreaKey, setSelectedSaleAreaKey] = useState('');

  const [rentAreaOptions, setRentAreaOptions] = useState([]);
  const [selectedRentAreaKey, setSelectedRentAreaKey] = useState(null);
  const [rentAreaSummary, setRentAreaSummary] = useState(null);
  const [rentTrades, setRentTrades] = useState([]);

  const [nearbySchools, setNearbySchools] = useState([]);
  const [aptRegion, setAptRegion] = useState(null);

  const [showGraphModal, setShowGraphModal] = useState(false);
  const [rentGraphView, setRentGraphView] = useState('jeonse'); // 'jeonse' | 'wolse'
  const [tradePeriod, setTradePeriod] = useState(() => parsePeriod(searchParams.get('tradePeriod'), 6));
  const [graphPeriod, setGraphPeriod] = useState(() => parsePeriod(searchParams.get('graphPeriod'), 6));
  const [graphLoading, setGraphLoading] = useState(false);
  const [saleGraphData, setSaleGraphData] = useState([]);
  const [rentGraphData, setRentGraphData] = useState([]);
  const [rentDotGraphData, setRentDotGraphData] = useState([]);

  const selectedSaleTrades = useMemo(() => {
    const map = saleDetail?.pyeongTrades || {};
    const rows = getValueByNumericKey(map, selectedSaleAreaKey) || [];
    return [...rows].sort((a, b) => String(b?.dealDate || '').localeCompare(String(a?.dealDate || '')));
  }, [saleDetail, selectedSaleAreaKey]);

  const selectedSaleAvgAmount = useMemo(() => {
    const rawRows = getValueByNumericKey(saleDetail?.chartData || {}, selectedSaleAreaKey) || [];
    const chartRows = normalizeSaleChartRows(rawRows);
    if (chartRows.length > 0) {
      const latestMeaningful = [...chartRows]
        .reverse()
        .map((row) => toNumber(row?.avgAmount))
        .find((v) => v > 0);
      if (latestMeaningful) return latestMeaningful;
    }
    if (selectedSaleTrades.length > 0) {
      const sum = selectedSaleTrades.reduce((acc, row) => acc + toNumber(row?.dealAmount), 0);
      const avgByTrades = Math.round(sum / selectedSaleTrades.length);
      if (avgByTrades > 0) return avgByTrades;
    }
    return toNumber(saleDetail?.avgAmount);
  }, [saleDetail, selectedSaleAreaKey, selectedSaleTrades]);

  const selectedAreaLabel = useMemo(() => {
    if (tradeType === '매매') {
      const target = saleAreaOptions.find((opt) => String(opt.value) === String(selectedSaleAreaKey));
      return target ? `${target.label}㎡` : '-';
    }
    const target = rentAreaOptions.find((opt) => Number(opt.value) === Number(selectedRentAreaKey));
    return target ? `${target.label}㎡` : '-';
  }, [tradeType, saleAreaOptions, selectedSaleAreaKey, rentAreaOptions, selectedRentAreaKey]);

  useEffect(() => {
    // 내부 state 동기화로 발생한 URL 변경은 다시 state에 반영하지 않는다.
    if (isInternalUrlSyncRef.current) {
      isInternalUrlSyncRef.current = false;
      return;
    }

    const qTradeType = searchParams.get('tradeType');
    if ((qTradeType === '매매' || qTradeType === '전월세') && qTradeType !== tradeType) {
      setTradeType(qTradeType);
    }

    const qGraphPeriod = parsePeriod(searchParams.get('graphPeriod'), graphPeriod);
    if (qGraphPeriod !== graphPeriod) {
      setGraphPeriod(qGraphPeriod);
    }
    const qTradePeriod = parsePeriod(searchParams.get('tradePeriod'), tradePeriod);
    if (qTradePeriod !== tradePeriod) {
      setTradePeriod(qTradePeriod);
    }

    if (tradeType === '매매' && saleAreaOptions.length > 0) {
      const qSaleAreaKey = searchParams.get('saleAreaKey');
      if (!qSaleAreaKey) return;
      const exists = saleAreaOptions.some((opt) => String(opt.value) === String(qSaleAreaKey));
      if (exists && String(selectedSaleAreaKey) !== String(qSaleAreaKey)) {
        setSelectedSaleAreaKey(String(qSaleAreaKey));
      }
      return;
    }

    if (tradeType === '전월세' && rentAreaOptions.length > 0) {
      const qRentAreaKey = Number(searchParams.get('rentAreaKey'));
      if (!Number.isFinite(qRentAreaKey)) return;
      const exists = rentAreaOptions.some((opt) => Number(opt.value) === qRentAreaKey);
      if (exists && Number(selectedRentAreaKey) !== qRentAreaKey) {
        setSelectedRentAreaKey(qRentAreaKey);
      }
    }
  }, [searchParamsKey, saleAreaOptions, rentAreaOptions]);

  useEffect(() => {
    if (!aptId) return;

    const fetchSchools = async () => {
      try {
        const data = await getNearbySchools(aptId, schoolLevels);
        setNearbySchools(data || []);
      } catch {
        setNearbySchools([]);
      }
    };

    fetchSchools();
  }, [aptId, schoolLevels.join(',')]);

  useEffect(() => {
    if (!aptId) return;
    getApartmentRegion(aptId)
      .then((data) => { if (data) setAptRegion(data); })
      .catch(() => {});
  }, [aptId]);

  useEffect(() => {
    if (!aptId) return;
    if (tradeType !== '매매') return;

    const fetchSaleBase = async () => {
      setLoading(true);
      setErrorMessage('');
      try {
        const detail = await getAptSaleDetail(aptId, tradePeriod);
        setSaleDetail(detail || null);
        if (detail?.aptNm) setAptName(detail.aptNm);
        if (detail?.address) setAddress(detail.address);

        const areaEntries = Object.entries(detail?.pyeongTrades || {});
        const options = areaEntries
          .map(([k, rows]) => ({
            value: String(k),
            label: toNumber(rows?.[0]?.exurArea ?? k).toFixed(1),
          }))
          .sort((a, b) => toNumber(a.label) - toNumber(b.label));
        setSaleAreaOptions(options);
        const preferred = searchParams.get('saleAreaKey');
        const preferredOption = options.find((opt) => String(opt.value) === String(preferred));
        setSelectedSaleAreaKey(preferredOption?.value || options[0]?.value || '');
      } catch (e) {
        console.error(e);
        setErrorMessage('매매 상세 정보를 불러오지 못했습니다.');
      } finally {
        setLoading(false);
      }
    };

    fetchSaleBase();
  }, [aptId, tradeType, tradePeriod]);

  useEffect(() => {
    if (!aptId) return;
    if (tradeType !== '전월세') return;

    const fetchRentAreas = async () => {
      setLoading(true);
      setErrorMessage('');
      try {
        let options = [];
        try {
          const areaType = await getAptAreaTypes(aptId);
          options = (areaType?.options || [])
            .map((opt) => ({
              value: Number(opt?.areaKey),
              label: toNumber(opt?.exclusive).toFixed(1),
            }))
            .filter((opt) => Number.isFinite(opt.value))
            .sort((a, b) => a.value - b.value);
        } catch (areaError) {
          // month-avg 기반 면적 API가 실패하면 최근 거래에서 면적을 유추한다.
          console.error('전월세 면적 API 실패, 최근 거래 기반 폴백 사용:', areaError);
        }

        if (options.length === 0) {
          const fallbackRecent = await getRecentRentTrades(aptId);
          options = buildRentAreaOptionsFromTrades(fallbackRecent);
          if (!aptName && Array.isArray(fallbackRecent) && fallbackRecent[0]?.apartmentName) {
            setAptName(fallbackRecent[0].apartmentName);
          }
        }

        // NOTE: 백엔드 응답에 중복 areaKey가 포함될 수 있어 key 중복 경고 방지
        const seen = new Set();
        const deduped = options.filter((opt) => {
          const key = Number(opt?.value);
          if (!Number.isFinite(key)) return false;
          if (seen.has(key)) return false;
          seen.add(key);
          return true;
        });

        setRentAreaOptions(deduped);
        const preferred = Number(searchParams.get('rentAreaKey'));
        const preferredOption = deduped.find((opt) => Number(opt.value) === preferred);
        setSelectedRentAreaKey(preferredOption?.value ?? deduped[0]?.value ?? null);
      } catch (e) {
        console.error(e);
        setRentAreaOptions([]);
        setSelectedRentAreaKey(null);
        setErrorMessage('전월세 면적 정보를 불러오지 못했습니다.');
      } finally {
        setLoading(false);
      }
    };

    fetchRentAreas();
  }, [aptId, tradeType]);

  useEffect(() => {
    if (!aptId || tradeType !== '전월세' || !selectedRentAreaKey) return;

    const fetchRentDetail = async () => {
      try {
        const [detailRes, recentRes, avgRes, recentAvgRes] = await Promise.allSettled([
          getRentDetailByArea(aptId, selectedRentAreaKey, tradePeriod),
          getRecentRentTrades(aptId),
          getAreaTypeAvg(aptId, selectedRentAreaKey, tradePeriod),
          getRecentAreaAvg(aptId, selectedRentAreaKey),
        ]);

        const detail = detailRes.status === 'fulfilled' ? detailRes.value : null;
        const fallbackRecent = recentRes.status === 'fulfilled' ? recentRes.value : [];
        const avgRows = avgRes.status === 'fulfilled' ? avgRes.value : [];
        const summaryByRows = buildRentSummaryFromMonthlyAverages(avgRows);
        const summaryByRecent = recentAvgRes.status === 'fulfilled' ? recentAvgRes.value : null;

        const areaItems = detail?.items || [];
        const filtered = filterRecentTradesByArea(fallbackRecent || [], selectedRentAreaKey);
        const tradesForSummary = areaItems.length > 0 ? areaItems : (filtered.length > 0 ? filtered : (fallbackRecent || []));
        const summaryByTrades = buildRentSummaryFromTrades(tradesForSummary);
        const summary = hasMeaningfulRentSummary(summaryByRecent)
          ? summaryByRecent
          : hasMeaningfulRentSummary(summaryByRows)
            ? summaryByRows
            : summaryByTrades;

        setRentAreaSummary(summary || null);

        if (areaItems.length > 0) {
          setRentTrades(areaItems);
        } else {
          setRentTrades(filtered.length > 0 ? filtered : (fallbackRecent || []));
        }
      } catch (e) {
        console.error(e);
        setRentAreaSummary(null);
        setRentTrades([]);
      }
    };

    fetchRentDetail();
  }, [aptId, tradeType, selectedRentAreaKey, tradePeriod]);

  useEffect(() => {
    if (!showGraphModal || !aptId) return;

    const fetchGraph = async () => {
      setGraphLoading(true);
      try {
        if (tradeType === '매매') {
          const chart = await getAptSaleChart(aptId, graphPeriod);
          const map = chart?.pyeongChartDataMap || {};
          const selectedRows = getValueByNumericKey(map, selectedSaleAreaKey);
          const fallbackRows = Object.values(map || {})[0] || [];
          setSaleGraphData(normalizeSaleChartRows(selectedRows || fallbackRows || []));
        } else if (selectedRentAreaKey) {
          const [avgRes, dotsRes] = await Promise.allSettled([
            getAreaTypeAvg(aptId, selectedRentAreaKey, graphPeriod),
            getRentDots(aptId, graphPeriod),
          ]);

          const normalizedDots = normalizeRentDots(dotsRes.status === 'fulfilled' ? dotsRes.value : []);
          setRentDotGraphData(normalizedDots);

          if (avgRes.status === 'fulfilled') {
            setRentGraphData(normalizeRentGraphRows(avgRes.value || []));
          } else {
            // month-avg 기반 API가 실패하면 dots 데이터로 평균선을 구성
            const fallbackRows = normalizedDots.map((d) => ({
              yyyymm: d?.yyyymm,
              jeonseDepositAvg: d?.deposit ?? 0,
              wolseDepositAvg: 0,
              wolseRentAvg: d?.monthlyRent ?? 0,
              jeonseCount: isJeonseByMonthlyRent(d?.monthlyRent) && toNumber(d?.deposit) > 0 ? 1 : 0,
              wolseCount: isJeonseByMonthlyRent(d?.monthlyRent) ? 0 : 1,
            }));
            setRentGraphData(normalizeRentGraphRows(fallbackRows));
          }
        }
      } catch (e) {
        console.error(e);
        setSaleGraphData([]);
        setRentGraphData([]);
        setRentDotGraphData([]);
      } finally {
        setGraphLoading(false);
      }
    };

    fetchGraph();
  }, [showGraphModal, aptId, graphPeriod, tradeType, selectedSaleAreaKey, selectedRentAreaKey]);

  useEffect(() => {
    if (!aptId) return;

    const nextParams = new URLSearchParams(searchParamsKey);
    let hasChanged = false;

    if (nextParams.get('tradeType') !== tradeType) {
      nextParams.set('tradeType', tradeType);
      hasChanged = true;
    }

    if (nextParams.get('graphPeriod') !== String(graphPeriod)) {
      nextParams.set('graphPeriod', String(graphPeriod));
      hasChanged = true;
    }
    if (nextParams.get('tradePeriod') !== String(tradePeriod)) {
      nextParams.set('tradePeriod', String(tradePeriod));
      hasChanged = true;
    }

    if (tradeType === '매매') {
      if (nextParams.has('rentAreaKey')) {
        nextParams.delete('rentAreaKey');
        hasChanged = true;
      }
      if (saleAreaOptions.length > 0) {
        if (selectedSaleAreaKey) {
          if (nextParams.get('saleAreaKey') !== String(selectedSaleAreaKey)) {
            nextParams.set('saleAreaKey', String(selectedSaleAreaKey));
            hasChanged = true;
          }
        } else if (nextParams.has('saleAreaKey')) {
          nextParams.delete('saleAreaKey');
          hasChanged = true;
        }
      }
    } else {
      if (nextParams.has('saleAreaKey')) {
        nextParams.delete('saleAreaKey');
        hasChanged = true;
      }
      if (rentAreaOptions.length > 0) {
        if (selectedRentAreaKey != null) {
          if (nextParams.get('rentAreaKey') !== String(selectedRentAreaKey)) {
            nextParams.set('rentAreaKey', String(selectedRentAreaKey));
            hasChanged = true;
          }
        } else if (nextParams.has('rentAreaKey')) {
          nextParams.delete('rentAreaKey');
          hasChanged = true;
        }
      }
    }

    if (hasChanged) {
      isInternalUrlSyncRef.current = true;
      router.replace(`/apartment?${nextParams.toString()}`, { scroll: false });
    }
  }, [
    aptId,
    tradeType,
    tradePeriod,
    graphPeriod,
    selectedSaleAreaKey,
    selectedRentAreaKey,
    saleAreaOptions.length,
    rentAreaOptions.length,
    searchParamsKey,
    router,
  ]);

  const handleBackToMap = () => {
    // 아파트 지역 정보가 있으면 URL 쿼리 파라미터로 전달하여 지도 필터를 해당 지역으로 설정
    if (aptRegion?.sido && aptRegion?.gugun) {
      const params = new URLSearchParams();
      params.set('sido', aptRegion.sido);
      params.set('gugun', aptRegion.gugun);
      if (aptRegion.dong) params.set('dong', aptRegion.dong);
      router.push(`/search/map?${params.toString()}`);
      return;
    }

    // 지역 정보가 없으면 기존 복원 로직
    try {
      const lastParams = sessionStorage.getItem('search_map_lastParams');
      if (lastParams) {
        router.push('/search/map');
        return;
      }
    } catch (e) {
      // ignore
    }

    try {
      const lastUrl = sessionStorage.getItem('search_map_lastUrl');
      if (lastUrl && lastUrl.startsWith('/search/map')) {
        router.push(lastUrl);
        return;
      }
    } catch (e) {
      // ignore
    }

    if (window.history.length > 1) {
      router.back();
      return;
    }

    router.push('/search/map');
  };

  if (!aptId) {
    return (
      <div className="min-h-screen bg-gray-50 p-6">
        <div className="max-w-5xl mx-auto bg-white border border-gray-200 rounded-xl p-8 text-center">
          <div className="text-gray-800 font-semibold mb-2">아파트 정보가 없습니다.</div>
          <div className="text-sm text-gray-500 mb-5">지도에서 아파트를 선택한 뒤 상세보기를 눌러주세요.</div>
          <button
            type="button"
            onClick={handleBackToMap}
            className="px-4 py-2 rounded-lg bg-blue-600 text-white hover:bg-blue-700"
          >
            지도로 돌아가기
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 p-4 md:p-6">
      <div className="max-w-7xl mx-auto space-y-4">
        <ApartmentTopBar
          tradeType={tradeType}
          onBack={handleBackToMap}
          onTradeTypeChange={setTradeType}
        />

        <ApartmentSummaryCard
          aptName={aptName}
          address={address}
          tradeType={tradeType}
          selectedAreaLabel={selectedAreaLabel}
          saleAvgPriceText={formatAvgPrice(selectedSaleAvgAmount)}
          jeonseAvgText={formatAvgPrice(rentAreaSummary?.jeonseAvg)}
          wolseAvgText={`${formatAvgPrice(rentAreaSummary?.wolseAvg)} / ${formatAvgPrice(
            rentAreaSummary?.wolseRentAvg
          )}`}
        />

        <AreaSelectorCard
          tradeType={tradeType}
          saleAreaOptions={saleAreaOptions}
          selectedSaleAreaKey={selectedSaleAreaKey}
          onSelectSaleAreaKey={setSelectedSaleAreaKey}
          rentAreaOptions={rentAreaOptions}
          selectedRentAreaKey={selectedRentAreaKey}
          onSelectRentAreaKey={setSelectedRentAreaKey}
        />

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <TradeHistoryCard
            tradeType={tradeType}
            tradePeriod={tradePeriod}
            periodOptions={PERIOD_OPTIONS}
            onChangeTradePeriod={setTradePeriod}
            onOpenSaleGraph={() => {
              setGraphPeriod(tradePeriod);
              setShowGraphModal(true);
            }}
            onOpenJeonseGraph={() => {
              setRentGraphView('jeonse');
              setGraphPeriod(tradePeriod);
              setShowGraphModal(true);
            }}
            onOpenWolseGraph={() => {
              setRentGraphView('wolse');
              setGraphPeriod(tradePeriod);
              setShowGraphModal(true);
            }}
            selectedSaleTrades={selectedSaleTrades}
            rentTrades={rentTrades}
            formatDate={formatDate}
            formatPrice={formatPrice}
            isJeonseByMonthlyRent={isJeonseByMonthlyRent}
          />

          <SchoolForm schools={nearbySchools} />
        </div>

        {(loading || graphLoading) && (
          <div className="text-sm text-gray-500 text-center py-2">데이터를 불러오는 중입니다...</div>
        )}
        {errorMessage && <div className="text-sm text-red-600 text-center py-2">{errorMessage}</div>}
      </div>

      <GraphModal
        open={showGraphModal}
        onClose={() => setShowGraphModal(false)}
        tradeType={tradeType}
        rentGraphView={rentGraphView}
        graphPeriod={graphPeriod}
        periodOptions={PERIOD_OPTIONS}
        onChangeGraphPeriod={setGraphPeriod}
        graphLoading={graphLoading}
      >
        {tradeType === '매매' ? (
          <SaleGraph data={saleGraphData} />
        ) : (
          <RentGraph avgData={rentGraphData} dotData={rentDotGraphData} mode={rentGraphView} />
        )}
      </GraphModal>
    </div>
  );
}

export default function ApartmentPage() {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen bg-gray-50 p-6">
          <div className="max-w-5xl mx-auto bg-white border border-gray-200 rounded-xl p-8 text-center text-gray-600">
            로딩 중...
          </div>
        </div>
      }
    >
      <ApartmentPageContent />
    </Suspense>
  );
}
