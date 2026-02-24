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
import { logPyeongClick } from '../api/log';
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
    const yyyymm = normalizeYyyymmKey(row?.yyyymm ?? row?.month ?? '');
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
    // 그래프 스케일 통일: 만원 단위로 변환
    const jeonseAvg = toManwon(row?.jeonseDepositAvg ?? row?.jeonseAvg ?? row?.deposit ?? 0);
    const wolseDepositAvg = toManwon(row?.wolseDepositAvg ?? row?.wolseAvg ?? 0);
    const wolseRentAvg = toManwon(row?.wolseRentAvg ?? row?.monthlyRent ?? row?.mothlyRent ?? 0);
    const jeonseCount = Math.max(0, toNumber(row?.jeonseCount));
    const wolseCount = Math.max(0, toNumber(row?.wolseCount));

    // 월세 0은 전세로 분류: 월세 평균 연산에서 제외
    // count가 없으면 값 유무로 1건으로 간주(폴백 dots 대응)
    const hasJeonseValue = jeonseAvg > 0;
    const hasWolseValue = wolseDepositAvg > 0 || wolseRentAvg > 0;

    // (중요) 한 달에 전세+월세가 함께 존재할 수 있으므로,
    // count가 없더라도 전세 값이 있으면 전세로 인정해야 전세 평균선이 0으로 떨어지지 않는다.
    const effectiveJeonseCount =
      jeonseCount > 0 ? jeonseCount : (hasJeonseValue ? 1 : 0);

    // (중요) wolseRentAvg가 0으로 내려오는 달이 있어도 wolseDepositAvg/wolseCount로 월세를 인정해야
    // 월세 보증금 그래프가 "0으로 고정"되는 문제를 막을 수 있다.
    const effectiveWolseCount =
      wolseCount > 0 ? wolseCount : (hasWolseValue ? 1 : 0);

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

    const hasWolseTrade =
      row.wolseCount > 0 &&
      (toNumber(row.wolseDepositAvg) > 0 || toNumber(row.wolseRentAvg) > 0);
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
      yyyymm: normalizeYyyymmKey(row?.yyyymm ?? row?.month ?? ''),
      // 면적(㎡) — DotResponse.exclusive 기반
      // - 배포/로컬/과거 응답 키 차이를 흡수
      exclusive: (() => {
        const raw =
          row?.exclusive ??
          row?.exclusiveArea ??
          row?.exclusive_area ??
          row?.areaExclusive ??
          row?.area ??
          null;
        if (raw == null || raw === '') return null;
        const n = Number(raw);
        return Number.isFinite(n) && n > 0 ? n : null;
      })(),
      // 그래프 스케일 통일: 만원 단위로 변환
      deposit: toManwon(row?.deposit),
      monthlyRent: toManwon(row?.monthlyRent ?? row?.mothlyRent),
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

function filterRentDotsByArea(dots, selectedAreaKey) {
  if (!Array.isArray(dots) || !selectedAreaKey) return [];
  const selectedExclusive = Number(selectedAreaKey) / 100;
  return dots.filter((dot) => {
    const ex = Number(dot?.exclusive);
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

/**
 * 다양한 yyyymm 표현을 YYYYMM(6자리)로 정규화한다.
 * - "202501" -> "202501"
 * - "2025.1" / "2025-1" / "20251" -> "202501"
 * - "2025.10" / "2025-10" -> "202510"
 */
function normalizeYyyymmKey(raw) {
  if (raw == null) return '';
  const digits = String(raw).replaceAll(/[^0-9]/g, '');
  if (!digits) return '';
  let key = '';
  if (digits.length === 6) key = digits;
  else if (digits.length === 5) key = `${digits.slice(0, 4)}0${digits.slice(4)}`; // YYYY M
  else if (digits.length >= 6) key = digits.slice(0, 6); // YYYYMMDD... -> YYYYMM
  else return digits;

  // month validation: 01~12만 허용
  const mm = Number(key.slice(4, 6));
  if (!Number.isFinite(mm) || mm < 1 || mm > 12) return '';
  return key;
}

function yyyymmToYearMonth(key) {
  const k = normalizeYyyymmKey(key);
  if (!k) return null;
  const y = Number(k.slice(0, 4));
  const m = Number(k.slice(4, 6)); // 1~12
  if (!Number.isFinite(y) || !Number.isFinite(m)) return null;
  return { year: y, month: m };
}

function yearMonthToYyyymm(year, month) {
  if (!Number.isFinite(year) || !Number.isFinite(month)) return '';
  const y = Math.trunc(year);
  const m = Math.trunc(month);
  if (m < 1 || m > 12) return '';
  return `${String(y).padStart(4, '0')}${String(m).padStart(2, '0')}`;
}

function addMonthsToYyyymm(key, deltaMonths) {
  const ym = yyyymmToYearMonth(key);
  if (!ym) return '';
  const delta = Math.trunc(toNumber(deltaMonths));
  const idx = (ym.year * 12 + (ym.month - 1)) + delta;
  const y = Math.floor(idx / 12);
  const m = (idx % 12) + 1;
  return yearMonthToYyyymm(y, m);
}

function currentYyyymm() {
  const d = new Date();
  if (Number.isNaN(d.getTime())) return '';
  return yearMonthToYyyymm(d.getFullYear(), d.getMonth() + 1);
}

function maxYyyymm(keys) {
  if (!Array.isArray(keys) || keys.length === 0) return '';
  const normalized = keys.map(normalizeYyyymmKey).filter(Boolean);
  if (normalized.length === 0) return '';
  return normalized.reduce((max, k) => (String(k) > String(max) ? k : max), normalized[0]);
}

function buildMonthsRange(endKey, monthsCount) {
  const end = normalizeYyyymmKey(endKey) || currentYyyymm();
  const count = Math.max(1, Math.trunc(toNumber(monthsCount) || 6));
  const start = addMonthsToYyyymm(end, -(count - 1));
  if (!start) return [end].filter(Boolean);
  const out = [];
  for (let i = 0; i < count; i++) {
    const k = addMonthsToYyyymm(start, i);
    if (k) out.push(k);
  }
  return out;
}

function toNumber(value) {
  if (value == null) return 0;
  // "1,234" 같은 문자열이 오면 Number()가 NaN이므로 보정
  if (typeof value === 'string') {
    const cleaned = value.replaceAll(',', '').replaceAll('_', '').trim();
    const n = Number(cleaned);
    return Number.isFinite(n) ? n : 0;
  }
  const n = Number(value);
  return Number.isFinite(n) ? n : 0;
}

/**
 * API가 원 단위/만원 단위를 섞어서 내려주는 케이스를 흡수하기 위한 통일 함수.
 * - n이 충분히 크면(>= 1,000,000) 원 단위로 보고 만원으로 변환한다.
 * - 그 외는 이미 만원 단위라고 가정한다.
 *
 * 그래프 스케일링은 반드시 같은 단위로 맞춰야 (평균선이 dots보다 한없이 아래/위로 튀는 문제) 방지 가능.
 */
function toManwon(value) {
  const n = toNumber(value);
  if (n <= 0) return 0;
  return n >= 1_000_000 ? Math.round(n / 10000) : Math.round(n);
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
  const key = normalizeYyyymmKey(yyyymm);
  if (!key || String(key).length < 6) return '-';
  const v = String(key);
  return `${v.slice(0, 4)}.${v.slice(4, 6)}`; // YYYY.MM (01~12)
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

function buildMonthX(index, count, chartWidth = CHART_WIDTH) {
  const safeCount = Math.max(1, toNumber(count));
  const plotWidth = chartWidth - CHART_PADDING_X * 2;
  if (safeCount <= 1) return CHART_PADDING_X + plotWidth / 2;
  return CHART_PADDING_X + (index * plotWidth) / (safeCount - 1);
}

function SaleGraph({ data, periodMonths }) {
  const [tooltip, setTooltip] = useState(null);
  const tooltipRef = useRef(null);

  // hooks는 early return보다 먼저 선언되어야 한다 (rules-of-hooks)
  const handleDotMouseEnter = useCallback((dot) => {
    setTooltip({ data: dot, x: dot.x, y: dot.y });
  }, []);

  const handleDotMouseLeave = useCallback(() => {
    setTooltip(null);
  }, []);

  const normalizedRows = useMemo(() => {
    const rows = Array.isArray(data) ? data : [];
    return rows
      .map((r) => ({
        ...r,
        month: normalizeYyyymmKey(r?.month ?? r?.yyyymm ?? ''),
        avgAmount: toNumber(r?.avgAmount),
        tradeCount: toNumber(r?.tradeCount),
        dots: Array.isArray(r?.dots) ? r.dots : [],
      }))
      .filter((r) => r?.month);
  }, [data]);

  const months = useMemo(() => {
    const endKey = maxYyyymm(normalizedRows.map((r) => r.month)) || currentYyyymm();
    return buildMonthsRange(endKey, periodMonths);
  }, [normalizedRows, periodMonths]);

  const rowsByMonth = useMemo(() => new Map(normalizedRows.map((r) => [r.month, r])), [normalizedRows]);

  const chartRows = useMemo(() => {
    // period 범위 내 모든 월을 생성하고, 데이터 없는 달은 빈 row로 둔다.
    const raw = months.map((m) => {
      const r = rowsByMonth.get(m);
      const dots = Array.isArray(r?.dots) ? r.dots : [];
      const normalizedDots = dots.map((d) => ({
        dealDate: d?.dealDate ?? d?.date ?? '',
        dealAmount: toNumber(d?.dealAmount ?? d?.price ?? d?.amount ?? 0),
        floor: d?.floor ?? null,
      }));
      const avgFromDots =
        normalizedDots.length > 0
          ? Math.round(normalizedDots.reduce((sum, d) => sum + toNumber(d?.dealAmount), 0) / normalizedDots.length)
          : 0;

      const avgAmount = toNumber(r?.avgAmount);
      const tradeCount = toNumber(r?.tradeCount);

      return {
        month: m,
        avgAmount: avgAmount > 0 ? avgAmount : avgFromDots,
        tradeCount: tradeCount > 0 ? tradeCount : normalizedDots.length,
        dots: normalizedDots,
      };
    });

    // 매매 평균선은 거래 없는 달이 급락하지 않도록 carry-forward (기존 UI 유지)
    const firstTraded = raw.find((row) => toNumber(row?.tradeCount) > 0 && toNumber(row?.avgAmount) > 0);
    let carriedAvg = firstTraded ? toNumber(firstTraded.avgAmount) : 0;

    return raw.map((row) => {
      const hasTrade = toNumber(row?.tradeCount) > 0;
      const avgAmount = toNumber(row?.avgAmount);
      if (hasTrade && avgAmount > 0) {
        carriedAvg = avgAmount;
        return row;
      }
      return { ...row, avgAmount: carriedAvg };
    });
  }, [months, rowsByMonth]);

  // 디버깅(필수): period 적용 여부 확인
  useEffect(() => {
    if (typeof window === 'undefined') return;
    if (process.env.NODE_ENV === 'production') return;
    const start = months[0];
    const end = months[months.length - 1];
    // eslint-disable-next-line no-console
    console.log('[SaleGraph period]', { periodMonths, monthsLen: months.length, start, end });
  }, [periodMonths, months]);

  if (months.length === 0) {
    return <div className="w-full text-center text-gray-500 py-10">차트 데이터가 없습니다.</div>;
  }

  const maxAvg = Math.max(...chartRows.map((d) => toNumber(d?.avgAmount)), 0);
  const maxDot = Math.max(
    ...chartRows.flatMap((monthRow) => (monthRow?.dots || []).map((dot) => toNumber(dot?.dealAmount))),
    0
  );
  const maxValue = Math.max(maxAvg, maxDot, 1);
  const scaleY = buildYScaler(maxValue);

  // 기간이 길어질수록 x축이 빽빽해지므로, 월 개수에 비례해 차트 폭을 늘리고 스크롤로 감싼다.
  const pxPerMonth = 72;
  const plotWidth = Math.max(CHART_WIDTH - CHART_PADDING_X * 2, (months.length - 1) * pxPerMonth);
  const chartWidth = Math.round(plotWidth + CHART_PADDING_X * 2);
  const xRight = chartWidth - CHART_PADDING_X;
  const tickStep = Math.max(1, Math.ceil(months.length / 10));

  const avgPoints = chartRows.map((row, index) => ({
    x: buildMonthX(index, months.length, chartWidth),
    y: scaleY(row?.avgAmount),
    value: toNumber(row?.avgAmount),
    month: row?.month,
  }));

  const tradeDots = chartRows.flatMap((row, monthIndex) => {
    const centerX = buildMonthX(monthIndex, months.length, chartWidth);
    return (row?.dots || []).map((dot, dotIndex) => ({
      x: centerX + ((dotIndex % 5) - 2) * 2,
      y: scaleY(dot?.dealAmount),
      value: toNumber(dot?.dealAmount),
      month: row?.month,
      dealDate: dot?.dealDate,
      floor: dot?.floor,
    }));
  });

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
      <div className="overflow-x-auto">
        <div style={{ minWidth: `${chartWidth}px` }}>
          <svg 
            viewBox={`0 0 ${chartWidth} ${CHART_HEIGHT}`} 
            className="w-full h-72 bg-white rounded-lg border border-gray-100"
          >
            {yGuideValues.map((value, index) => {
              const y = scaleY(value);
              return (
                <g key={`sale-guide-${index}-${value}`}>
                  <line x1={CHART_PADDING_X} y1={y} x2={xRight} y2={y} stroke="#E5E7EB" strokeWidth="1" />
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

            {/* x축 라벨 */}
            {months.map((m, idx) => {
              const show = idx % tickStep === 0 || idx === months.length - 1;
              if (!show) return null;
              const x = buildMonthX(idx, months.length, chartWidth);
              return (
                <text
                  key={`sale-x-${m}-${idx}`}
                  x={x}
                  y={CHART_HEIGHT - 6}
                  fontSize="10"
                  fill="#9CA3AF"
                  textAnchor="middle"
                >
                  {formatMonth(m)}
                </text>
              );
            })}

            {/* 커스텀 툴팁: 점 위에 고정 (말풍선 스타일) */}
            {tooltip && tooltip.data && (() => {
              const tooltipWidth = 180;
              const tooltipHeight = 60;
              const tailHeight = 10;
              const gap = 6;
              const padding = 10;
              
              const dotX = tooltip.x;
              const dotY = tooltip.y;
              
              const tooltipX = Math.min(Math.max(dotX - tooltipWidth / 2, padding), chartWidth - tooltipWidth - padding);
              
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
                    <div className="relative" ref={tooltipRef}>
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
      </div>
    </div>
  );
}

function RentGraph({ avgData, dotData, mode = 'jeonse', periodMonths }) {
  const [tooltip, setTooltip] = useState(null);
  const [wolseDepositTooltip, setWolseDepositTooltip] = useState(null);
  const [wolseRentTooltip, setWolseRentTooltip] = useState(null);
  const jeonseTooltipRef = useRef(null);
  const wolseDepositTooltipRef = useRef(null);
  const wolseRentTooltipRef = useRef(null);

  // hooks는 early return보다 먼저 선언되어야 한다 (rules-of-hooks)
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

  // (중요) x축 키를 하나로 통일: YYYYMM(6자리)
  const normalizedAvgRows = useMemo(() => {
    const normKey = (v) => normalizeYyyymmKey(v);
    return (avgData || [])
      .map((r) => ({ ...r, yyyymm: normKey(r?.yyyymm) }))
      .filter((r) => r?.yyyymm);
  }, [avgData]);

  const normalizedDotRows = useMemo(() => {
    const normKey = (v) => normalizeYyyymmKey(v);
    return (dotData || [])
      .map((r) => ({ ...r, yyyymm: normKey(r?.yyyymm) }))
      .filter((r) => r?.yyyymm);
  }, [dotData]);

  const months = useMemo(() => {
    const keys = [
      ...normalizedAvgRows.map((r) => r?.yyyymm).filter(Boolean),
      ...normalizedDotRows.map((r) => r?.yyyymm).filter(Boolean),
    ];
    const endKey = maxYyyymm(keys) || currentYyyymm();
    return buildMonthsRange(endKey, periodMonths);
  }, [normalizedAvgRows, normalizedDotRows, periodMonths]);

  // period가 길어질수록 x축이 빽빽해지므로, 월 개수에 비례해 차트 폭을 늘리고 스크롤로 감싼다.
  const { chartWidth, xRight, tickStep } = useMemo(() => {
    const pxPerMonth = 72; // 보기 좋은 밀도(월 간격)
    const plotWidth = Math.max(CHART_WIDTH - CHART_PADDING_X * 2, (months.length - 1) * pxPerMonth);
    const chartWidth = Math.round(plotWidth + CHART_PADDING_X * 2);
    const xRight = chartWidth - CHART_PADDING_X;
    const tickStep = Math.max(1, Math.ceil(months.length / 10)); // 약 10개 정도만 라벨 표시
    return { chartWidth, xRight, tickStep };
  }, [months.length]);

  const avgMap = useMemo(() => {
    return new Map(normalizedAvgRows.map((row) => [row.yyyymm, row]));
  }, [normalizedAvgRows]);

  const dotsByMonth = useMemo(() => {
    const m = new Map();
    normalizedDotRows.forEach((dot) => {
      if (!m.has(dot.yyyymm)) m.set(dot.yyyymm, []);
      m.get(dot.yyyymm).push(dot);
    });
    return m;
  }, [normalizedDotRows]);

  const dotAvgByMonth = useMemo(() => {
    // dots 기반 월평균(프론트에서 직접 계산): avg API가 0/누락/키 불일치여도 평균선이 0에 붙지 않도록 보강한다.
    const m = new Map();
    months.forEach((month) => {
      const dots = dotsByMonth.get(month) || [];
      const jeonseDots = dots.filter((d) => isJeonseByMonthlyRent(d?.monthlyRent) && toNumber(d?.deposit) > 0);
      const wolseDots = dots.filter(
        (d) => !isJeonseByMonthlyRent(d?.monthlyRent) && (toNumber(d?.deposit) > 0 || toNumber(d?.monthlyRent) > 0)
      );

      const jeonseDepositAvg =
        jeonseDots.length > 0 ? Math.round(jeonseDots.reduce((s, d) => s + toNumber(d?.deposit), 0) / jeonseDots.length) : 0;

      const wolseDepositAvg =
        wolseDots.length > 0 ? Math.round(wolseDots.reduce((s, d) => s + toNumber(d?.deposit), 0) / wolseDots.length) : 0;

      const wolseRentAvg =
        wolseDots.length > 0 ? Math.round(wolseDots.reduce((s, d) => s + toNumber(d?.monthlyRent), 0) / wolseDots.length) : 0;

      m.set(month, { jeonseDepositAvg, wolseDepositAvg, wolseRentAvg });
    });
    return m;
  }, [months, dotsByMonth]);

  const { jeonseLine, wolseDepositLine, wolseRentLine } = useMemo(() => {
    // (중요) dots가 있는 달은 dots 기반 mean(sum/count)을 "항상" 우선한다.
    // 백엔드 월평균(avgData)은 필터/면적/집계 기준이 다를 수 있어 line을 왜곡할 수 있으므로,
    // dots와 라인이 항상 일치해야 하는 그래프에서는 dots를 source of truth로 둔다.
    const pickAvgPreferDots = (dotAvg, backendAvg) => {
      const d = toNumber(dotAvg);
      if (d > 0) return d;
      return toNumber(backendAvg);
    };

    const jeonseLine = months.map((month) => ({
      month,
      value: pickAvgPreferDots(dotAvgByMonth.get(month)?.jeonseDepositAvg, avgMap.get(month)?.jeonseDepositAvg),
    }));
    const wolseDepositLine = months.map((month) => ({
      month,
      value: pickAvgPreferDots(dotAvgByMonth.get(month)?.wolseDepositAvg, avgMap.get(month)?.wolseDepositAvg),
    }));
    const wolseRentLine = months.map((month) => ({
      month,
      value: pickAvgPreferDots(dotAvgByMonth.get(month)?.wolseRentAvg, avgMap.get(month)?.wolseRentAvg),
    }));

    return { jeonseLine, wolseDepositLine, wolseRentLine };
  }, [months, avgMap, dotAvgByMonth]);

  // 디버깅: 차트에 최종 주입되는 데이터/타입/key 매칭 확인
  useEffect(() => {
    if (typeof window === 'undefined') return;
    if (process.env.NODE_ENV === 'production') return;

    try {
      console.groupCollapsed(`[RentGraph debug] mode=${mode} months=${months.length}`);
      console.log('[RentGraph period]', { periodMonths, monthsLen: months.length, start: months[0], end: months[months.length - 1] });
      console.log('months:', months.slice(0, 12), months.length > 12 ? `...(+${months.length - 12})` : '');
      console.log('avgData(sample):', normalizedAvgRows.slice(0, 3));
      console.log('dotData(sample):', normalizedDotRows.slice(0, 3));
      if (months[0]) {
        const m0 = months[0];
        console.log('month[0] key:', m0, 'avgMap.has?', avgMap.has(m0), 'avgRow:', avgMap.get(m0));
      }
      console.log('jeonseLine(sample):', jeonseLine.slice(0, 6));
      console.log('wolseDepositLine(sample):', wolseDepositLine.slice(0, 6));
      console.log('wolseRentLine(sample):', wolseRentLine.slice(0, 6));

      // 강제 검증: 2025-12 (YYYYMM=202512) 평균 검증 로그
      const targetMonth = '202512';
      if (months.includes(targetMonth)) {
        const dots = dotsByMonth.get(targetMonth) || [];
        const jeonseDeposits = dots
          .filter((d) => isJeonseByMonthlyRent(d?.monthlyRent))
          .map((d) => toNumber(d?.deposit))
          .filter((v) => v > 0);
        const wolseDeposits = dots
          .filter((d) => !isJeonseByMonthlyRent(d?.monthlyRent))
          .map((d) => toNumber(d?.deposit))
          .filter((v) => v > 0);
        const wolseRents = dots
          .filter((d) => !isJeonseByMonthlyRent(d?.monthlyRent))
          .map((d) => toNumber(d?.monthlyRent))
          .filter((v) => v > 0);

        const mean = (arr) => {
          const sum = arr.reduce((s, v) => s + toNumber(v), 0);
          const count = arr.length;
          const avg = count > 0 ? Math.round(sum / count) : 0;
          return { sum, count, avg };
        };

        const j = mean(jeonseDeposits);
        const wd = mean(wolseDeposits);
        const wr = mean(wolseRents);

        console.groupCollapsed(`[RentGraph validate] ${targetMonth}`);
        console.log('jeonse deposits:', jeonseDeposits, j);
        console.log('wolse deposits:', wolseDeposits, wd);
        console.log('wolse rents:', wolseRents, wr);
        console.log('line values:', {
          jeonse: jeonseLine.find((p) => p.month === targetMonth)?.value,
          wolseDeposit: wolseDepositLine.find((p) => p.month === targetMonth)?.value,
          wolseRent: wolseRentLine.find((p) => p.month === targetMonth)?.value,
        });
        console.groupEnd();
      }
      console.groupEnd();
    } catch {
      // ignore
    }
  }, [mode, periodMonths, months, normalizedAvgRows, normalizedDotRows, avgMap, jeonseLine, wolseDepositLine, wolseRentLine]);

  if (months.length === 0) {
    return <div className="w-full text-center text-gray-500 py-10">차트 데이터가 없습니다.</div>;
  }

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

  const jeonseLinePoints = jeonseLine.map((p, idx) => ({ x: buildMonthX(idx, months.length, chartWidth), y: jeonseScaleY(p.value), value: p.value, month: p.month }));
  const wolseDepositLinePoints = wolseDepositLine.map((p, idx) => ({ x: buildMonthX(idx, months.length, chartWidth), y: wolseDepositScaleY(p.value), value: p.value, month: p.month }));
  const wolseRentLinePoints = wolseRentLine.map((p, idx) => ({ x: buildMonthX(idx, months.length, chartWidth), y: wolseRentScaleY(p.value), value: p.value, month: p.month }));

  const jeonseDots = months.flatMap((month, monthIdx) => {
    const centerX = buildMonthX(monthIdx, months.length, chartWidth);
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
    const centerX = buildMonthX(monthIdx, months.length, chartWidth);
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
          <div className="overflow-x-auto">
            <div style={{ minWidth: `${chartWidth}px` }}>
              <svg 
                viewBox={`0 0 ${chartWidth} ${CHART_HEIGHT}`} 
                className="w-full h-72 bg-white rounded-lg border border-gray-100"
              >
                {jeonseGuide.map((value, index) => {
                  const y = jeonseScaleY(value);
                  return (
                    <g key={`rent-jeonse-guide-${index}-${value}`}>
                      <line x1={CHART_PADDING_X} y1={y} x2={xRight} y2={y} stroke="#E5E7EB" strokeWidth="1" />
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

                {/* x축 라벨: dot/라인과 동일한 buildMonthX 스케일 사용 */}
                {months.map((m, idx) => {
                  const show = idx % tickStep === 0 || idx === months.length - 1;
                  if (!show) return null;
                  const x = buildMonthX(idx, months.length, chartWidth);
                  return (
                    <text
                      key={`rent-jeonse-x-${m}-${idx}`}
                      x={x}
                      y={CHART_HEIGHT - 6}
                      fontSize="10"
                      fill="#9CA3AF"
                      textAnchor="middle"
                    >
                      {formatMonth(m)}
                    </text>
                  );
                })}
            {/* 전세 툴팁: 점 위에 고정 */}
            {tooltip && tooltip.data && tooltip.data.monthlyRent === null && (() => {
              const tooltipWidth = 180;
              // eslint(rule): render 중 ref.current 접근 금지 → 고정 높이 사용
              const tooltipHeight = 60;
              const tailHeight = 10;
              const gap = 6;
              const padding = 10;
              
              const dotX = tooltip.x;
              const dotY = tooltip.y;
              
              const tooltipX = Math.min(Math.max(dotX - tooltipWidth / 2, padding), chartWidth - tooltipWidth - padding);
              
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
          </div>
        </div>
      )}

      {mode === 'wolse' && (
        <>
          <div className="space-y-2">
            <div className="flex items-center gap-4 text-xs text-gray-600">
              <span className="inline-flex items-center gap-1"><span className="w-2.5 h-2.5 rounded-full bg-emerald-600" />월별 보증금 평균</span>
              <span className="inline-flex items-center gap-1"><span className="w-2.5 h-2.5 rounded-full bg-gray-500" />보증금 거래 점(dots)</span>
            </div>
            <div className="overflow-x-auto">
              <div style={{ minWidth: `${chartWidth}px` }}>
                <svg 
                  viewBox={`0 0 ${chartWidth} ${CHART_HEIGHT}`} 
                  className="w-full h-72 bg-white rounded-lg border border-gray-100"
                >
              {wolseDepositGuide.map((value, index) => {
                const y = wolseDepositScaleY(value);
                return (
                  <g key={`rent-wolse-deposit-guide-${index}-${value}`}>
                    <line x1={CHART_PADDING_X} y1={y} x2={xRight} y2={y} stroke="#E5E7EB" strokeWidth="1" />
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
                // eslint(rule): render 중 ref.current 접근 금지 → 고정 높이 사용
                const tooltipHeight = 70;
                const tailHeight = 10;
                const gap = 6;
                const padding = 10;
                
                const dotX = wolseDepositTooltip.x;
                const dotY = wolseDepositTooltip.y;
                
                const tooltipX = Math.min(Math.max(dotX - tooltipWidth / 2, padding), chartWidth - tooltipWidth - padding);
                
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

              {/* x축 라벨 */}
              {months.map((m, idx) => {
                const show = idx % tickStep === 0 || idx === months.length - 1;
                if (!show) return null;
                const x = buildMonthX(idx, months.length, chartWidth);
                return (
                  <text
                    key={`rent-wolse-deposit-x-${m}-${idx}`}
                    x={x}
                    y={CHART_HEIGHT - 6}
                    fontSize="10"
                    fill="#9CA3AF"
                    textAnchor="middle"
                  >
                    {formatMonth(m)}
                  </text>
                );
              })}
                </svg>
              </div>
            </div>
          </div>

          <div className="space-y-2">
            <div className="flex items-center gap-4 text-xs text-gray-600">
              <span className="inline-flex items-center gap-1"><span className="w-2.5 h-2.5 rounded-full bg-orange-500" />월별 월세 평균</span>
              <span className="inline-flex items-center gap-1"><span className="w-2.5 h-2.5 rounded-full bg-gray-500" />월세 거래 점(dots)</span>
            </div>
            <div className="overflow-x-auto">
              <div style={{ minWidth: `${chartWidth}px` }}>
                <svg 
                  viewBox={`0 0 ${chartWidth} ${CHART_HEIGHT}`} 
                  className="w-full h-72 bg-white rounded-lg border border-gray-100"
                >
              {wolseRentGuide.map((value, index) => {
                const y = wolseRentScaleY(value);
                return (
                  <g key={`rent-wolse-rent-guide-${index}-${value}`}>
                    <line x1={CHART_PADDING_X} y1={y} x2={xRight} y2={y} stroke="#E5E7EB" strokeWidth="1" />
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
                // eslint(rule): render 중 ref.current 접근 금지 → 고정 높이 사용
                const tooltipHeight = 70;
                const tailHeight = 10;
                const gap = 6;
                const padding = 10;
                
                const dotX = wolseRentTooltip.x;
                const dotY = wolseRentTooltip.y;
                
                const tooltipX = Math.min(Math.max(dotX - tooltipWidth / 2, padding), chartWidth - tooltipWidth - padding);
                
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

              {/* x축 라벨 */}
              {months.map((m, idx) => {
                const show = idx % tickStep === 0 || idx === months.length - 1;
                if (!show) return null;
                const x = buildMonthX(idx, months.length, chartWidth);
                return (
                  <text
                    key={`rent-wolse-rent-x-${m}-${idx}`}
                    x={x}
                    y={CHART_HEIGHT - 6}
                    fontSize="10"
                    fill="#9CA3AF"
                    textAnchor="middle"
                  >
                    {formatMonth(m)}
                  </text>
                );
              })}
                </svg>
              </div>
            </div>
          </div>
        </>
      )}
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

  const handleSelectSaleAreaKey = useCallback(
    (key) => {
      const opt = saleAreaOptions.find((o) => String(o.value) === String(key));
      if (aptId && opt) {
        const area = Number(opt.label);
        const rawRows = getValueByNumericKey(saleDetail?.chartData || {}, key) || [];
        const chartRows = normalizeSaleChartRows(rawRows);
        let price = 0;
        if (chartRows.length > 0) {
          const latestMeaningful = [...chartRows]
            .reverse()
            .map((row) => toNumber(row?.avgAmount))
            .find((v) => v > 0);
          if (latestMeaningful) price = latestMeaningful;
        }
        if (price === 0 && saleDetail?.pyeongTrades) {
          const rows = getValueByNumericKey(saleDetail.pyeongTrades, key) || [];
          if (rows.length > 0) {
            price = Math.round(
              rows.reduce((acc, row) => acc + toNumber(row?.dealAmount), 0) / rows.length
            );
          }
        }
        if (price === 0) price = toNumber(saleDetail?.avgAmount) || 0;
        logPyeongClick({
          aptId,
          area,
          price: price || null,
          monthlyRent: null,
          isRent: false,
        }).catch(() => {});
      }
      setSelectedSaleAreaKey(String(key));
    },
    [aptId, saleDetail, saleAreaOptions]
  );

  const handleSelectRentAreaKey = useCallback(
    (key) => {
      const opt = rentAreaOptions.find((o) => Number(o.value) === Number(key));
      if (aptId && opt) {
        const area = Number(opt.label);
        // 전세/월세 구분: 현재 보고 있는 그래프 뷰(rentGraphView) 기준으로 로그
        // - 전세(jeonse): monthlyRent = 0 → 백엔드 TradeType.RENT
        // - 월세(wolse): monthlyRent = 월세료(wolseRentAvg) → 백엔드 TradeType.WOLSE
        // (jeonseAvg는 보증금이므로 monthlyRent에 넣지 않음)
        const monthlyRent =
          rentGraphView === 'wolse' && rentAreaSummary != null
            ? Number(rentAreaSummary.wolseRentAvg) || 0
            : 0;
        logPyeongClick({
          aptId,
          area,
          price: null,
          monthlyRent: monthlyRent > 0 ? monthlyRent : null,
          isRent: true,
        }).catch(() => {});
      }
      setSelectedRentAreaKey(Number(key));
    },
    [aptId, rentAreaOptions, rentAreaSummary, rentGraphView]
  );

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

          // dots는 "아파트 전체" 데이터이므로, 현재 선택한 면적(areaKey)에 맞춰 필터링해야
          // 면적별 그래프가 섞이지 않는다.
          const allDots = normalizeRentDots(dotsRes.status === 'fulfilled' ? dotsRes.value : []);
          const normalizedDots = filterRentDotsByArea(allDots, selectedRentAreaKey);
          setRentDotGraphData(normalizedDots);

          if (avgRes.status === 'fulfilled') {
            const normalizedAvg = normalizeRentGraphRows(avgRes.value || []);
            const hasAnyAvgValue = normalizedAvg.some(
              (r) =>
                toNumber(r?.jeonseDepositAvg) > 0 ||
                toNumber(r?.wolseDepositAvg) > 0 ||
                toNumber(r?.wolseRentAvg) > 0
            );

            // avg API가 "성공"해도 값이 전부 0으로 내려오는 케이스가 있어,
            // dots가 충분히 있는데 avg가 무의미하면 dots로 평균선을 재구성한다.
            if (!hasAnyAvgValue && normalizedDots.length > 0) {
              const fallbackRows = normalizedDots.map((d) => ({
                yyyymm: d?.yyyymm,
                // d.deposit / d.monthlyRent 는 이미 normalizeRentDots에서 만원 단위로 정규화되어 있음
                jeonseDepositAvg: isJeonseByMonthlyRent(d?.monthlyRent) ? (d?.deposit ?? 0) : 0,
                wolseDepositAvg: isJeonseByMonthlyRent(d?.monthlyRent) ? 0 : (d?.deposit ?? 0),
                wolseRentAvg: isJeonseByMonthlyRent(d?.monthlyRent) ? 0 : (d?.monthlyRent ?? 0),
                jeonseCount: isJeonseByMonthlyRent(d?.monthlyRent) && toNumber(d?.deposit) > 0 ? 1 : 0,
                wolseCount: isJeonseByMonthlyRent(d?.monthlyRent) ? 0 : 1,
              }));
              setRentGraphData(normalizeRentGraphRows(fallbackRows));
            } else {
              setRentGraphData(normalizedAvg);
            }
          } else {
            // month-avg 기반 API가 실패하면 dots 데이터로 평균선을 구성
            const fallbackRows = normalizedDots.map((d) => ({
              yyyymm: d?.yyyymm,
              // d.deposit / d.monthlyRent 는 이미 normalizeRentDots에서 만원 단위로 정규화되어 있음
              jeonseDepositAvg: isJeonseByMonthlyRent(d?.monthlyRent) ? (d?.deposit ?? 0) : 0,
              wolseDepositAvg: isJeonseByMonthlyRent(d?.monthlyRent) ? 0 : (d?.deposit ?? 0),
              wolseRentAvg: isJeonseByMonthlyRent(d?.monthlyRent) ? 0 : (d?.monthlyRent ?? 0),
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
          onSelectSaleAreaKey={handleSelectSaleAreaKey}
          rentAreaOptions={rentAreaOptions}
          selectedRentAreaKey={selectedRentAreaKey}
          onSelectRentAreaKey={handleSelectRentAreaKey}
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
          <SaleGraph data={saleGraphData} periodMonths={graphPeriod} />
        ) : (
          <RentGraph avgData={rentGraphData} dotData={rentDotGraphData} mode={rentGraphView} periodMonths={graphPeriod} />
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
