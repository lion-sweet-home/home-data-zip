'use client';

import { useMemo, useRef, useState, useEffect, useCallback } from 'react';
import { getMonthlyTradeVolume, getNearbySubways, getNearbyBusStations, getNearbySchools, getApartmentRegion } from '../../../api/apartment';
import { getAptSaleSummary } from '../../../api/apartment_sale';
import { getRecentRentTrades } from '../../../api/apartment_rent';
import { getHospitalCount, getHospitalStats, getHospitalListByDong } from '../../../api/hospital';

// 막대 폭/간격 고정 규칙 (필수)
const BAR_WIDTH_PX = 50; // 18~24 권장
const BAR_GAP_PX = 12; // 10~14 권장 (그룹 간 간격)
const GROUP_INNER_GAP_PX = 6; // 전월세(전세/월세) 2개 막대 사이 간격
const CHART_PAD_X_PX = 16;

function computeDomainMax(maxValue) {
  const max = Math.max(1, Math.trunc(toNumber(maxValue)));
  // 상단 여유(headroom) 15% + 최소 1
  const padded = Math.ceil(max * 1.15);
  return Math.max(max + 1, padded);
}

function quarterTooltipHeaderFromKey(qKey) {
  // "2024-Q1" -> "24-1분기"
  const m = String(qKey).match(/^(\d{4})-Q([1-4])$/);
  if (!m) return '';
  return `${m[1].slice(2)}-${m[2]}분기`;
}

function quarterMonthRangeLineFromKey(qKey) {
  // "2024-Q1" -> "1~ 3월"
  const m = String(qKey).match(/^(\d{4})-Q([1-4])$/);
  if (!m) return '';
  const q = Number(m[2]);
  const start = (q - 1) * 3 + 1;
  const end = start + 2;
  return `${start}~ ${end}월`;
}

function FloatingTooltip({ tooltip }) {
  if (!tooltip) return null;
  return (
    <div
      className="absolute z-50 bg-gray-900 text-white text-xs rounded-lg px-3 py-2 shadow-lg pointer-events-none"
      style={{
        left: `${tooltip.x}px`,
        top: `${tooltip.y}px`,
        transform: 'translate(-50%, -100%)',
        maxWidth: 220,
      }}
    >
      {Array.isArray(tooltip.lines) && tooltip.lines.filter(Boolean).length > 0 ? (
        tooltip.lines.filter(Boolean).map((line, idx) => (
          <div key={`tt-${idx}`} className={idx === 0 ? 'font-medium mb-1' : 'text-gray-200'}>
            {line}
          </div>
        ))
      ) : (
        <div className="font-medium">{tooltip.title || ''}</div>
      )}
      <div
        className="absolute top-full left-1/2 transform -translate-x-1/2"
        style={{
          width: 0,
          height: 0,
          borderLeft: '6px solid transparent',
          borderRight: '6px solid transparent',
          borderTop: '6px solid rgb(17, 24, 39)',
        }}
      />
    </div>
  );
}

function toNumber(value) {
  if (value == null) return 0;
  if (typeof value === 'string') {
    const cleaned = value.replaceAll(',', '').replaceAll('_', '').trim();
    const n = Number(cleaned);
    return Number.isFinite(n) ? n : 0;
  }
  const n = Number(value);
  return Number.isFinite(n) ? n : 0;
}

function normalizeYyyymmKey(raw) {
  if (raw == null) return '';
  const digits = String(raw).replaceAll(/[^0-9]/g, '');
  if (!digits) return '';
  let key = '';
  if (digits.length === 6) key = digits;
  else if (digits.length === 5) key = `${digits.slice(0, 4)}0${digits.slice(4)}`;
  else if (digits.length >= 6) key = digits.slice(0, 6);
  else return digits;
  const mm = Number(key.slice(4, 6));
  if (!Number.isFinite(mm) || mm < 1 || mm > 12) return '';
  return key;
}

function yearMonthToYyyymm(year, month) {
  const y = Math.trunc(toNumber(year));
  const m = Math.trunc(toNumber(month));
  if (!Number.isFinite(y) || !Number.isFinite(m) || m < 1 || m > 12) return '';
  return `${String(y).padStart(4, '0')}${String(m).padStart(2, '0')}`;
}

function yyyymmToYearMonth(key) {
  const k = normalizeYyyymmKey(key);
  if (!k) return null;
  return { year: Number(k.slice(0, 4)), month: Number(k.slice(4, 6)) };
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

function monthKeyFromYyyymm(yyyymm) {
  const k = normalizeYyyymmKey(yyyymm);
  if (!k) return '';
  const yyyy = k.slice(0, 4);
  const mm = k.slice(4, 6);
  return `${yyyy}-${mm}`; // 내부 키: YYYY-MM
}

function quarterKeyFromYyyymm(yyyymm) {
  const ym = yyyymmToYearMonth(yyyymm);
  if (!ym) return '';
  const q = Math.floor((ym.month - 1) / 3) + 1; // 1~4
  return `${ym.year}-Q${q}`; // 내부 키: YYYY-Qn
}

function quarterLabelFromKey(qKey) {
  // qKey: "2025-Q4" -> "25 4분기" (표시: YYn분기)
  const m = String(qKey).match(/^(\d{4})-Q([1-4])$/);
  if (!m) return '';
  const yy = m[1].slice(2);
  const q = m[2]; // 1~4
  return `${yy}${q}분기`;
}

function quarterLabelLinesFromKey(qKey) {
  // 두 줄 라벨: 1줄=YY, 2줄=N분기
  const m = String(qKey).match(/^(\d{4})-Q([1-4])$/);
  if (!m) return null;
  return { top: m[1].slice(2), bottom: `${m[2]}분기` };
}

function monthLabelYYMM(yyyymm) {
  const k = normalizeYyyymmKey(yyyymm);
  if (!k) return '';
  const yy = k.slice(2, 4);
  const mm = k.slice(4, 6);
  return `${yy}.${mm}`; // 요구사항: YY.MM 한 줄
}

function fallbackMonthLabelFromKey(monthKey) {
  // monthKey: "2024-01" -> "24.01"
  const m = String(monthKey || '').match(/^(\d{4})-(\d{2})$/);
  if (!m) return '';
  return `${m[1].slice(2)}.${m[2]}`;
}

function monthLabelYYYYMMForTooltip(yyyymm) {
  const k = normalizeYyyymmKey(yyyymm);
  if (!k) return '';
  const yyyy = k.slice(0, 4);
  const mm = k.slice(4, 6);
  return `${yyyy}-${mm}`;
}

function transformMonthlyVolume(rawMonthlyData, tradeType, periodMonths) {
  const isSale = tradeType === '매매';
  const rows = Array.isArray(rawMonthlyData) ? rawMonthlyData : [];
  const period = Math.max(1, Math.trunc(toNumber(periodMonths) || 6));

  // raw -> map by yyyymm
  const map = new Map();
  rows.forEach((r) => {
    const key = normalizeYyyymmKey(r?.yyyymm);
    if (!key) return;
    if (isSale) {
      map.set(key, { yyyymm: key, saleCount: toNumber(r?.saleCount) });
    } else {
      map.set(key, { yyyymm: key, jeonseCount: toNumber(r?.jeonseCount), wolseCount: toNumber(r?.wolseCount) });
    }
  });

  const endKey = maxYyyymm([...map.keys()]) || currentYyyymm();
  const monthsRange = buildMonthsRange(endKey, period);

  const unit = period >= 24 ? 'quarter' : 'month';

  if (unit === 'month') {
    const items = monthsRange.map((m) => {
      const monthKey = monthKeyFromYyyymm(m);
      const row = map.get(m);
      if (isSale) {
        return {
          key: monthKey,
          label: monthLabelYYMM(m),
          tooltipTitle: monthLabelYYYYMMForTooltip(m),
          tooltipSub: '',
          saleCount: toNumber(row?.saleCount),
        };
      }
      return {
        key: monthKey,
        label: monthLabelYYMM(m),
        tooltipTitle: monthLabelYYYYMMForTooltip(m),
        tooltipSub: '',
        jeonseCount: toNumber(row?.jeonseCount),
        wolseCount: toNumber(row?.wolseCount),
      };
    });

    const maxCount = Math.max(
      1,
      ...items.map((it) => (isSale ? toNumber(it.saleCount) : Math.max(toNumber(it.jeonseCount), toNumber(it.wolseCount))))
    );

    return { unit, items, maxCount, startKey: monthsRange[0], endKey: monthsRange[monthsRange.length - 1] };
  }

  // quarter aggregation (2~4년)
  const quarterMap = new Map();
  monthsRange.forEach((m) => {
    const qKey = quarterKeyFromYyyymm(m);
    if (!qKey) return;
    const ym = yyyymmToYearMonth(m);
    const row = map.get(m);

    if (!quarterMap.has(qKey)) {
      quarterMap.set(qKey, {
        qKey,
        year: ym?.year ?? 0,
        quarter: Number(String(qKey).split('Q')[1]) || 0,
        months: [],
        saleCount: 0,
        jeonseCount: 0,
        wolseCount: 0,
      });
    }
    const target = quarterMap.get(qKey);
    if (ym?.month) target.months.push(ym.month);

    if (isSale) target.saleCount += toNumber(row?.saleCount);
    else {
      target.jeonseCount += toNumber(row?.jeonseCount);
      target.wolseCount += toNumber(row?.wolseCount);
    }
  });

  const quarters = [...quarterMap.values()].sort((a, b) => (a.year * 10 + a.quarter) - (b.year * 10 + b.quarter));
  const items = quarters.map((q) => {
    const months = (q.months || []).slice().sort((a, b) => a - b);
    const minM = months.length > 0 ? months[0] : null;
    const maxM = months.length > 0 ? months[months.length - 1] : null;
    const tooltipSub = minM && maxM ? `포함 월: ${String(minM).padStart(2, '0')}~${String(maxM).padStart(2, '0')}월` : '';
    const label = quarterLabelFromKey(q.qKey);
    return isSale
      ? {
          key: q.qKey,
          label,
          tooltipTitle: label,
          tooltipSub,
          saleCount: toNumber(q.saleCount),
        }
      : {
          key: q.qKey,
          label,
          tooltipTitle: label,
          tooltipSub,
          jeonseCount: toNumber(q.jeonseCount),
          wolseCount: toNumber(q.wolseCount),
        };
  });

  const maxCount = Math.max(
    1,
    ...items.map((it) => (isSale ? toNumber(it.saleCount) : Math.max(toNumber(it.jeonseCount), toNumber(it.wolseCount))))
  );

  return { unit, items, maxCount, startKey: monthsRange[0], endKey: monthsRange[monthsRange.length - 1] };
}

export default function SidePanner({ apartmentId, apartmentInfo, schoolLevels, tradeType = '매매', onShowDetail, onBackToList, onToggleBusMarker, onToggleSchoolMarker }) {
  // 월별 거래량
  const [monthlyData, setMonthlyData] = useState([]);
  const [selectedPeriod, setSelectedPeriod] = useState(6);
  const [showGraphModal, setShowGraphModal] = useState(false);
  const [graphModalPeriod, setGraphModalPeriod] = useState(6);
  const [modalMonthlyData, setModalMonthlyData] = useState([]);
  const modalChartScrollRef = useRef(null);
  const smallChartScrollRef = useRef(null);
  const modalChartBoxRef = useRef(null);
  // Tooltip 상태
  const [tooltip, setTooltip] = useState(null);
  const [modalTooltip, setModalTooltip] = useState(null);

  // 최근 거래내역
  const [recentTrades, setRecentTrades] = useState([]);

  // 인근 지하철역
  const [nearbySubways, setNearbySubways] = useState([]);

  // 인근 학교
  const [nearbySchools, setNearbySchools] = useState([]);
  const [schoolMarkerVisible, setSchoolMarkerVisible] = useState(false);

  // 인근 버스
  const [busStations, setBusStations] = useState([]);
  const [busMarkerVisible, setBusMarkerVisible] = useState(false);

  // 해당 동 병원
  const [hospitalCount, setHospitalCount] = useState(0);
  const [hospitalStats, setHospitalStats] = useState(null);
  const [showHospitalModal, setShowHospitalModal] = useState(false);
  const [selectedHospitalType, setSelectedHospitalType] = useState('전체');
  const [resolvedRegion, setResolvedRegion] = useState(null);

  // 로딩 상태
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!apartmentId) return;

    const fetchData = async () => {
      try {
        setLoading(true);

        const normalizedSchoolLevels = Array.isArray(schoolLevels)
          ? schoolLevels
          : typeof schoolLevels === 'string'
            ? schoolLevels.split(',').map((s) => s.trim()).filter(Boolean)
            : [];

        // 병원 조회를 위한 지역정보(sido/gugun/dong) 보강
        let region = {
          sido: apartmentInfo?.sido || null,
          gugun: apartmentInfo?.gugun || null,
          dong: apartmentInfo?.dong || null,
        };
        if (!region.sido || !region.gugun || !region.dong) {
          try {
            const r = await getApartmentRegion(apartmentId);
            region = {
              sido: r?.sido || region.sido,
              gugun: r?.gugun || region.gugun,
              dong: r?.dong || region.dong,
            };
          } catch (e) {
            // region 조회 실패 시 병원은 스킵(다른 데이터는 계속 보여줌)
          }
        }
        setResolvedRegion(region);
        
        // 매매/전월세에 따라 다른 API 호출
        const isSale = tradeType === '매매';
        
        // tradeType이 없으면 경고하고 조기 종료
        if (!tradeType || (tradeType !== '매매' && tradeType !== '전월세')) {
          console.warn('[SidePanner] tradeType이 유효하지 않습니다:', tradeType);
          setLoading(false);
          return;
        }
        
        const monthlyVolumePromise = isSale
          ? getAptSaleSummary(apartmentId, selectedPeriod)
          : getMonthlyTradeVolume(apartmentId, selectedPeriod);
        
        // 최근 거래내역 API: tradeType에 따라 분기
        const recentTradesPromise = isSale
          ? Promise.resolve(null) // 매매는 getAptSaleSummary 응답에서 추출
          : getRecentRentTrades(apartmentId);
        
        // 모든 필요한 API를 병렬로 호출
        // Promise.allSettled를 사용하여 일부 실패해도 다른 API는 계속 처리
        const promises = [
          monthlyVolumePromise,
          recentTradesPromise,
          getNearbySchools(apartmentId, normalizedSchoolLevels),
          getNearbySubways(apartmentId),
          getNearbyBusStations(apartmentId, 500, 50)
        ];

        // 병원 정보도 병렬로 호출 (지역 정보가 있는 경우)
        let hospitalCountPromise = null;
        let hospitalStatsPromise = null;
        if (region?.sido && region?.gugun && region?.dong) {
          hospitalCountPromise = getHospitalCount({
            sido: region.sido,
            gugun: region.gugun,
            dong: region.dong
          });
          hospitalStatsPromise = getHospitalStats({
            sido: region.sido,
            gugun: region.gugun,
            dong: region.dong
          });
          promises.push(hospitalCountPromise, hospitalStatsPromise);
        }

        // Promise.allSettled를 사용하여 일부 API 실패해도 다른 데이터는 계속 처리
        const results = await Promise.allSettled(promises);
        
        // 각 결과를 안전하게 추출 (실패한 경우 기본값 사용)
        const extractResult = (index, defaultValue = null, apiName = '') => {
          const result = results[index];
          if (result.status === 'fulfilled') {
            return result.value;
          } else {
            const error = result.reason;
            console.error(`[SidePanner] API 호출 실패 (index ${index}${apiName ? `, ${apiName}` : ''}):`, error);
            // 에러 상세 정보 로깅
            if (error?.message) {
              console.error(`[SidePanner] 에러 메시지:`, error.message);
            }
            if (error?.stack) {
              console.error(`[SidePanner] 에러 스택:`, error.stack);
            }
            if (error?.response) {
              console.error(`[SidePanner] 응답 데이터:`, error.response);
            }
            return defaultValue;
          }
        };
        
        // 매매/전월세에 따라 데이터 형식 변환
        const rawMonthlyData = extractResult(0);
        let recentTradesData = extractResult(1);
        
        if (isSale && rawMonthlyData) {
          // 매매: {monthlyVolumes: [1,2,3], monthLabels: ['202401','202402',...], recentTradeSales: [...]} 형식
          const saleData = rawMonthlyData.monthlyVolumes?.map((count, index) => ({
            yyyymm: rawMonthlyData.monthLabels?.[index] || '',
            saleCount: count || 0,
            jeonseCount: 0,
            wolseCount: 0,
          })) || [];
          setMonthlyData(saleData);
          
          // 매매 최근 거래내역: getAptSaleSummary 응답에서 추출
          // RecentTradeSale 형식을 RentFromAptResponse 형식으로 변환
          // Note: RecentTradeSale에는 PK가 없으므로 index를 포함하여 유니크한 ID 생성
          const saleRecentTrades = (rawMonthlyData.recentTradeSales || []).map((trade, index) => ({
            id: `sale-${trade.dealDate}-${trade.floor}-${trade.exurArea}-${index}`, // 유니크 ID 생성 (index 포함)
            dealDate: trade.dealDate,
            floor: trade.floor,
            exclusiveArea: trade.exurArea,
            deposit: trade.dealAmount, // 매매는 dealAmount가 가격
            monthlyRent: null, // 매매는 월세 없음
          }));
          // 중복 제거: 같은 날짜/층/면적의 거래가 여러 건 있어도 첫 번째만 유지
          const uniqueSaleTrades = saleRecentTrades.filter((trade, index, self) => {
            const baseKey = `${trade.dealDate}-${trade.floor}-${trade.exclusiveArea}`;
            return index === self.findIndex(t => 
              `${t.dealDate}-${t.floor}-${t.exclusiveArea}` === baseKey
            );
          });
          recentTradesData = uniqueSaleTrades;
        } else {
          // 전월세: [{yyyymm, jeonseCount, wolseCount}] 형식
          setMonthlyData(rawMonthlyData || []);
          // 전월세 최근 거래내역: getRecentRentTrades 응답 그대로 사용
          // RentFromAptResponse에는 id 필드가 있지만, 혹시 중복이 있을 수 있으므로 안전하게 처리
          if (Array.isArray(recentTradesData)) {
            // id가 없는 항목에 대해 index 기반 id 생성
            recentTradesData = recentTradesData.map((trade, index) => ({
              ...trade,
              id: trade.id || `rent-${trade.dealDate}-${trade.floor}-${trade.exclusiveArea}-${index}`,
            }));
            // id 기준 중복 제거 (같은 id가 여러 개 있으면 첫 번째만 유지)
            const seenIds = new Set();
            recentTradesData = recentTradesData.filter((trade) => {
              if (seenIds.has(trade.id)) {
                return false;
              }
              seenIds.add(trade.id);
              return true;
            });
          } else {
            recentTradesData = [];
          }
        }
        
        setRecentTrades(recentTradesData);
        
        // 학교 데이터 처리 (배열 직접 반환)
        const nearbySchoolsData = extractResult(2, []);
        setNearbySchools(Array.isArray(nearbySchoolsData) ? nearbySchoolsData : []);
        
        // 지하철역 데이터 처리 (배열 직접 반환)
        const nearbySubwaysData = extractResult(3, []);
        setNearbySubways(Array.isArray(nearbySubwaysData) ? nearbySubwaysData : []);
        
        // 버스정류장 데이터 처리 ({count, items} 형식)
        const busStationsResponse = extractResult(4, null, '버스정류장');
        if (busStationsResponse && typeof busStationsResponse === 'object') {
          // 응답 구조 확인 및 로깅 (개발 환경)
          if (process.env.NODE_ENV === 'development') {
            console.log('[SidePanner] 버스정류장 API 응답:', JSON.stringify(busStationsResponse, null, 2));
          }
          
          // {count, items} 형식인지 확인
          if ('items' in busStationsResponse && Array.isArray(busStationsResponse.items)) {
            const items = busStationsResponse.items;
            // 데이터 검증 및 정규화
            const normalizedItems = items.map((item, idx) => {
              if (!item || typeof item !== 'object') {
                console.warn(`[SidePanner] 버스정류장 아이템 ${idx}가 유효하지 않습니다:`, item);
                return null;
              }
              return {
                id: item.id || item.nodeId || `bus-${idx}`,
                nodeId: item.nodeId || '',
                stationNumber: item.stationNumber || '',
                name: item.name || '',
                longitude: typeof item.longitude === 'number' ? item.longitude : parseFloat(item.longitude) || 0,
                latitude: typeof item.latitude === 'number' ? item.latitude : parseFloat(item.latitude) || 0,
                distanceMeters: typeof item.distanceMeters === 'number' ? item.distanceMeters : parseFloat(item.distanceMeters) || 0,
              };
            }).filter(Boolean); // null 제거
            
            setBusStations(normalizedItems);
            if (process.env.NODE_ENV === 'development') {
              console.log(`[SidePanner] 버스정류장 ${normalizedItems.length}개 로드 완료`);
            }
          } else if (Array.isArray(busStationsResponse)) {
            // 배열로 직접 반환된 경우 (예외 처리)
            console.warn('[SidePanner] 버스정류장 응답이 예상과 다른 형식입니다 (배열):', busStationsResponse);
            setBusStations(busStationsResponse);
          } else {
            console.warn('[SidePanner] 버스정류장 응답 형식이 올바르지 않습니다. 예상: {count, items}, 실제:', busStationsResponse);
            setBusStations([]);
          }
        } else if (busStationsResponse === null) {
          // API 호출 실패 (extractResult가 null 반환)
          console.warn('[SidePanner] 버스정류장 API 호출 실패 또는 응답 없음');
          setBusStations([]);
        } else {
          setBusStations([]);
        }

        // 병원 정보 처리
        if (region?.sido && region?.gugun && region?.dong && hospitalCountPromise && hospitalStatsPromise) {
          const hospitalCountResult = extractResult(5, 0);
          const hospitalStatsResult = extractResult(6, null);
          setHospitalCount(typeof hospitalCountResult === 'number' ? hospitalCountResult : 0);
          setHospitalStats(hospitalStatsResult);
        } else {
          setHospitalCount(0);
          setHospitalStats(null);
        }
      } catch (error) {
        console.error('[SidePanner] 데이터 로딩 실패:', error);
        // 에러 상세 정보 로깅
        if (error.response) {
          console.error('[SidePanner] 응답 데이터:', error.response);
        }
        if (error.message) {
          console.error('[SidePanner] 에러 메시지:', error.message);
        }
        // 일부 데이터라도 표시할 수 있도록 기본값 설정
        setMonthlyData([]);
        setRecentTrades([]);
        setNearbySchools([]);
        setNearbySubways([]);
        setBusStations([]);
        setHospitalCount(0);
        setHospitalStats(null);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [apartmentId, selectedPeriod, apartmentInfo, schoolLevels, tradeType]);

  // 학교 데이터가 늦게 로딩되어도, 토글이 켜져 있으면 마커를 다시 올려준다
  useEffect(() => {
    if (!onToggleSchoolMarker) return;
    if (!schoolMarkerVisible) return;
    onToggleSchoolMarker(nearbySchools, true);
  }, [nearbySchools, schoolMarkerVisible]); // 부모 콜백은 useCallback으로 고정되어야 무한루프가 안 남

  // 그래프 모달 데이터 로드
  useEffect(() => {
    if (showGraphModal && apartmentId) {
      const fetchGraphData = async () => {
        try {
          const isSale = tradeType === '매매';
          const rawData = isSale
            ? await getAptSaleSummary(apartmentId, graphModalPeriod)
            : await getMonthlyTradeVolume(apartmentId, graphModalPeriod);
          
          if (isSale && rawData) {
            const saleData = rawData.monthlyVolumes?.map((count, index) => ({
              yyyymm: rawData.monthLabels?.[index] || '',
              saleCount: count || 0,
              jeonseCount: 0,
              wolseCount: 0,
            })) || [];
            setModalMonthlyData(saleData);
          } else {
            setModalMonthlyData(rawData || []);
          }
        } catch (error) {
          console.error('그래프 데이터 로딩 실패:', error);
        }
      };
      fetchGraphData();
    }
  }, [showGraphModal, graphModalPeriod, apartmentId, tradeType]);

  const handleBusMarkerToggle = (checked) => {
    setBusMarkerVisible(checked);
    if (onToggleBusMarker) {
      onToggleBusMarker(busStations, checked);
    }
  };

  const handleSchoolMarkerToggle = (checked) => {
    setSchoolMarkerVisible(checked);
    if (onToggleSchoolMarker) {
      onToggleSchoolMarker(nearbySchools, checked);
    }
  };

  const formatPrice = (price) => {
    if (!price) return '-';
    if (price >= 10000) {
      return `${(price / 10000).toFixed(1)}억`;
    }
    return `${price}만`;
  };

  const formatDate = (dateString) => {
    if (!dateString) return '-';
    const date = new Date(dateString);
    return `${date.getFullYear()}.${String(date.getMonth() + 1).padStart(2, '0')}.${String(date.getDate()).padStart(2, '0')}`;
  };

  const formatMonth = (yyyymm) => monthLabelYYMM(yyyymm);
  const formatMonthForTooltip = (yyyymm) => monthLabelYYYYMMForTooltip(yyyymm);

  const smallGraph = useMemo(() => {
    return transformMonthlyVolume(monthlyData, tradeType, selectedPeriod);
  }, [monthlyData, tradeType, selectedPeriod]);

  const modalGraph = useMemo(() => {
    return transformMonthlyVolume(modalMonthlyData, tradeType, graphModalPeriod);
  }, [modalMonthlyData, tradeType, graphModalPeriod]);

  // UX: 2~4년(분기)에서 최근 데이터 바로 보이도록 오른쪽 끝으로 자동 스크롤
  useEffect(() => {
    if (!showGraphModal) return;
    const el = modalChartScrollRef.current;
    if (!el) return;
    if (modalGraph?.unit !== 'quarter') return;

    // DOM 업데이트 이후에 스크롤 조정
    const t = setTimeout(() => {
      try {
        el.scrollLeft = el.scrollWidth;
      } catch (e) {
        // ignore
      }
    }, 0);

    return () => clearTimeout(t);
  }, [showGraphModal, graphModalPeriod, modalGraph.unit, modalGraph.items.length]);

  // 디버깅(필수): 기간 변경 시 unit 전환/범위 확인
  useEffect(() => {
    if (process.env.NODE_ENV !== 'development') return;
    console.log('[SidePanner period][small]', {
      tradeType,
      period: selectedPeriod,
      unit: smallGraph.unit,
      len: smallGraph.items.length,
      start: smallGraph.startKey,
      end: smallGraph.endKey,
    });
  }, [tradeType, selectedPeriod, smallGraph]);

  useEffect(() => {
    if (process.env.NODE_ENV !== 'development') return;
    const el = smallChartScrollRef.current;
    if (!el) return;
    const containerWidth = el.clientWidth;
    const chartWidth = el.scrollWidth;
    const hasOverflowX = chartWidth > containerWidth + 1;
    // eslint-disable-next-line no-console
    console.log('[SidePanner layout][small]', {
      period: selectedPeriod,
      len: smallGraph.items.length,
      chartWidth,
      containerWidth,
      hasOverflowX,
    });
  }, [selectedPeriod, smallGraph.items.length, smallGraph.unit]);

  useEffect(() => {
    if (process.env.NODE_ENV !== 'development') return;
    if (!showGraphModal) return;
    console.log('[SidePanner period][modal]', {
      tradeType,
      period: graphModalPeriod,
      unit: modalGraph.unit,
      len: modalGraph.items.length,
      start: modalGraph.startKey,
      end: modalGraph.endKey,
    });
  }, [tradeType, graphModalPeriod, showGraphModal, modalGraph]);

  useEffect(() => {
    if (process.env.NODE_ENV !== 'development') return;
    if (!showGraphModal) return;
    const el = modalChartScrollRef.current;
    if (!el) return;
    const containerWidth = el.clientWidth;
    const chartWidth = el.scrollWidth;
    const hasOverflowX = chartWidth > containerWidth + 1;
    // eslint-disable-next-line no-console
    console.log('[SidePanner layout][modal]', {
      period: graphModalPeriod,
      len: modalGraph.items.length,
      chartWidth,
      containerWidth,
      hasOverflowX,
    });
  }, [showGraphModal, graphModalPeriod, modalGraph.items.length, modalGraph.unit]);

  if (loading) {
    return (
      <div className="w-full h-full flex items-center justify-center">
        <div className="text-gray-500">로딩 중...</div>
      </div>
    );
  }

  return (
    <div className="w-full h-full overflow-y-auto bg-white p-6 space-y-6">
      {/* 아파트 목록으로 돌아가기 */}
      {onBackToList && (
        <div className="flex-shrink-0 pb-2 border-b border-gray-200">
          <button
            type="button"
            onClick={onBackToList}
            className="flex items-center gap-2 px-3 py-2 text-sm font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
            </svg>
            목록 보기
          </button>
        </div>
      )}

      {/* 선택된 아파트 기본 정보 */}
      <div className="pb-4 border-b">
        <div className="text-xl font-bold text-gray-900">
          {apartmentInfo?.name || '아파트'}
        </div>
        {apartmentInfo?.address ? (
          <div className="text-sm text-gray-700 mt-1">{apartmentInfo.address}</div>
        ) : (
          <div className="text-sm text-gray-500 mt-1">
            {apartmentInfo?.sido || ''} {apartmentInfo?.gugun || ''} {apartmentInfo?.dong || ''}
          </div>
        )}
      </div>

      {/* 월별 거래량 */}
      <div className="border-b pb-4">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold text-gray-900">월별 거래량</h3>
          <button
            onClick={() => setShowGraphModal(true)}
            className="text-sm text-blue-600 hover:text-blue-700"
          >
            크게보기
          </button>
        </div>
        
        {/* 기간 선택 버튼 */}
        <div className="flex gap-2 mb-4">
          {[6, 12, 24, 36, 48].map((period) => (
            <button
              key={period}
              onClick={() => setSelectedPeriod(period)}
              className={`px-3 py-1 text-sm rounded ${
                selectedPeriod === period
                  ? 'bg-blue-600 text-white'
                  : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
              }`}
            >
              {period === 6 ? '6개월' : period === 12 ? '1년' : `${period / 12}년`}
            </button>
          ))}
        </div>

        {/* 간단한 그래프 (막대 그래프) */}
        <div className="mt-12 relative">
          <div ref={smallChartScrollRef} className="h-48 overflow-x-auto overflow-y-hidden w-full">
            {smallGraph.items.length > 0 ? (() => {
              const isSale = tradeType === '매매';
              const chartHeight = 192; // h-48 = 192px
              // 라벨 영역을 고정 높이로 분리해서(overflow-y-hidden에서도) 잘리지 않도록 한다.
              const labelAreaHeight = smallGraph.unit === 'quarter' ? 36 : 24;
              const plotHeight = Math.max(80, chartHeight - labelAreaHeight);
              const yMax = smallGraph.maxCount;
              const yDomainMax = computeDomainMax(yMax);
              const n = smallGraph.items.length;
              const groupWidth = isSale ? BAR_WIDTH_PX : (BAR_WIDTH_PX * 2 + GROUP_INNER_GAP_PX);
              const pointPx = groupWidth + BAR_GAP_PX; // POINT_PX
              const chartContentWidthPx = Math.max(0, n * pointPx - (n > 0 ? BAR_GAP_PX : 0));

              if (process.env.NODE_ENV === 'development') {
                // eslint-disable-next-line no-console
                console.log('[SidePanner yScale][small]', { period: selectedPeriod, len: n, yMax, yDomain: [0, yDomainMax] });
              }

              return (
                <div
                  className={`flex items-end ${smallGraph.unit === 'month' ? 'justify-center' : 'justify-start'}`}
                  style={{
                    height: `${chartHeight}px`,
                    gap: `${BAR_GAP_PX}px`,
                    paddingLeft: `${CHART_PAD_X_PX}px`,
                    paddingRight: `${CHART_PAD_X_PX}px`,
                    // chartWidth = max(가시폭, points * POINT_PX + padding)
                    width: `max(100%, ${chartContentWidthPx}px)`,
                  }}
                >
                  {smallGraph.items.map((item, index) => {
                    if (isSale) {
                      const saleCount = toNumber(item.saleCount);
                      const saleHeightPx = yDomainMax > 0 ? (saleCount / yDomainMax) * plotHeight : 0;
                      return (
                        <div
                          key={item.key || index}
                          className="flex flex-col items-center"
                          style={{ width: `${groupWidth}px` }}
                        >
                          <div className="w-full flex justify-center items-end" style={{ height: `${plotHeight}px` }}>
                            {saleCount > 0 ? (
                              <div
                                className="bg-blue-600 rounded-t cursor-pointer hover:bg-blue-700 transition-colors"
                                style={{ width: `${BAR_WIDTH_PX}px`, height: `${Math.max(saleHeightPx, 4)}px` }}
                                onMouseEnter={(e) => {
                                  const rect = e.currentTarget.getBoundingClientRect();
                                  const containerRect = smallChartScrollRef.current?.getBoundingClientRect();
                                  if (containerRect) {
                                    const lines =
                                      smallGraph.unit === 'quarter'
                                        ? [
                                            quarterTooltipHeaderFromKey(item.key),
                                            quarterMonthRangeLineFromKey(item.key),
                                            `거래량 ${saleCount}건`,
                                          ]
                                        : [item.tooltipTitle, `거래량 ${saleCount}건`];
                                    setTooltip({
                                      lines,
                                      x: rect.left - containerRect.left + rect.width / 2,
                                      y: rect.top - containerRect.top - 10,
                                    });
                                  }
                                }}
                                onMouseLeave={() => setTooltip(null)}
                              />
                            ) : (
                              <div className="h-[2px] bg-gray-200" style={{ width: `${BAR_WIDTH_PX}px` }} />
                            )}
                          </div>
                          <div style={{ height: `${labelAreaHeight}px` }} className="flex items-center justify-center">
                            {smallGraph.unit === 'quarter' ? (() => {
                              const lines = quarterLabelLinesFromKey(item.key);
                              return (
                                <div className="text-[11px] text-gray-600 text-center leading-tight">
                                  <span className="block">{lines?.top || ''}</span>
                                  <span className="block">{lines?.bottom || ''}</span>
                                </div>
                              );
                            })() : (
                              <div className="text-xs text-gray-600 text-center whitespace-nowrap leading-none">
                                {item.label || fallbackMonthLabelFromKey(item.key)}
                              </div>
                            )}
                          </div>
                        </div>
                      );
                    }

                    // 전월세: 전세와 월세를 별도 막대로 표시
                    const jeonseCount = toNumber(item.jeonseCount);
                    const wolseCount = toNumber(item.wolseCount);
                    const jeonseHeightPx = yDomainMax > 0 ? (jeonseCount / yDomainMax) * plotHeight : 0;
                    const wolseHeightPx = yDomainMax > 0 ? (wolseCount / yDomainMax) * plotHeight : 0;

                    return (
                      <div
                        key={item.key || index}
                        className="flex flex-col items-center"
                        style={{ width: `${groupWidth}px` }}
                      >
                        <div className="w-full flex justify-center items-end" style={{ height: `${plotHeight}px`, gap: `${GROUP_INNER_GAP_PX}px` }}>
                          {jeonseCount > 0 && (
                            <div
                              className="bg-blue-600 rounded-t cursor-pointer hover:bg-blue-700 transition-colors"
                              style={{ width: `${BAR_WIDTH_PX}px`, height: `${Math.max(jeonseHeightPx, 4)}px` }}
                              onMouseEnter={(e) => {
                                const rect = e.currentTarget.getBoundingClientRect();
                                const containerRect = smallChartScrollRef.current?.getBoundingClientRect();
                                if (containerRect) {
                                  const lines =
                                    smallGraph.unit === 'quarter'
                                      ? [
                                          quarterTooltipHeaderFromKey(item.key),
                                          quarterMonthRangeLineFromKey(item.key),
                                          `거래량 ${jeonseCount}건`,
                                        ]
                                      : [item.tooltipTitle, `거래량 ${jeonseCount}건`];
                                  setTooltip({
                                    lines,
                                    x: rect.left - containerRect.left + rect.width / 2,
                                    y: rect.top - containerRect.top - 10,
                                  });
                                }
                              }}
                              onMouseLeave={() => setTooltip(null)}
                            />
                          )}
                          {wolseCount > 0 && (
                            <div
                              className="bg-green-600 rounded-t cursor-pointer hover:bg-green-700 transition-colors"
                              style={{ width: `${BAR_WIDTH_PX}px`, height: `${Math.max(wolseHeightPx, 4)}px` }}
                              onMouseEnter={(e) => {
                                const rect = e.currentTarget.getBoundingClientRect();
                                const containerRect = smallChartScrollRef.current?.getBoundingClientRect();
                                if (containerRect) {
                                  const lines =
                                    smallGraph.unit === 'quarter'
                                      ? [
                                          quarterTooltipHeaderFromKey(item.key),
                                          quarterMonthRangeLineFromKey(item.key),
                                          `거래량 ${wolseCount}건`,
                                        ]
                                      : [item.tooltipTitle, `거래량 ${wolseCount}건`];
                                  setTooltip({
                                    lines,
                                    x: rect.left - containerRect.left + rect.width / 2,
                                    y: rect.top - containerRect.top - 10,
                                  });
                                }
                              }}
                              onMouseLeave={() => setTooltip(null)}
                            />
                          )}
                          {jeonseCount === 0 && wolseCount === 0 && (
                            <div className="h-[2px] bg-gray-200" style={{ width: `${groupWidth}px` }} />
                          )}
                        </div>
                        <div style={{ height: `${labelAreaHeight}px` }} className="flex items-center justify-center">
                          {smallGraph.unit === 'quarter' ? (() => {
                            const lines = quarterLabelLinesFromKey(item.key);
                            return (
                              <div className="text-[11px] text-gray-600 text-center leading-tight">
                                <span className="block">{lines?.top || ''}</span>
                                <span className="block">{lines?.bottom || ''}</span>
                              </div>
                            );
                          })() : (
                            <div className="text-xs text-gray-600 text-center whitespace-nowrap leading-none">
                              {item.label || fallbackMonthLabelFromKey(item.key)}
                            </div>
                          )}
                        </div>
                      </div>
                    );
                  })}
                </div>
              );
            })() : (
              <div className="w-full text-center text-gray-500 py-8">거래 데이터가 없습니다</div>
            )}
          </div>
          {/* Tooltip */}
          <FloatingTooltip tooltip={tooltip} />
        </div>
      </div>

      {/* 최근 거래내역 */}
      <div className="border-b pb-4">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">최근 거래내역</h3>
        <div className="space-y-3">
          {recentTrades.length > 0 ? (
            recentTrades.slice(0, 5).map((trade, index) => (
              <div key={trade.id || `trade-${index}`} className="p-3 bg-gray-50 rounded-lg">
                <div className="flex justify-between items-start mb-2">
                  <div className="text-sm text-gray-600">
                    {trade.floor}층 · {trade.exclusiveArea ? `${trade.exclusiveArea}㎡` : '-'}
                  </div>
                  <div className="text-sm text-gray-500">{formatDate(trade.dealDate)}</div>
                </div>
                <div className="text-sm text-gray-900">
                  {tradeType === '매매' ? (
                    <>매매가: {formatPrice(trade.deposit)}</>
                  ) : (
                    <>
                      보증금: {formatPrice(trade.deposit)}
                      {trade.monthlyRent && ` / 월세: ${formatPrice(trade.monthlyRent)}`}
                    </>
                  )}
                </div>
              </div>
            ))
          ) : (
            <div className="text-center text-gray-500 py-4">거래내역이 없습니다</div>
          )}
        </div>
      </div>

      {/* 인근 학교 */}
      <div className="border-b pb-4">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold text-gray-900">인근 학교</h3>
          <label className="flex items-center gap-2 cursor-pointer">
            <input
              type="checkbox"
              checked={schoolMarkerVisible}
              onChange={(e) => handleSchoolMarkerToggle(e.target.checked)}
              className="w-4 h-4 text-blue-600 rounded focus:ring-blue-500"
            />
            <span className="text-sm text-gray-700">마커 표시</span>
          </label>
        </div>
        <div className="space-y-3">
          {nearbySchools.length > 0 ? (
            nearbySchools.slice(0, 3).map((school, index) => (
              <div key={school.schoolId ?? index} className="p-3 bg-gray-50 rounded-lg">
                <div className="font-medium text-gray-900 mb-1">{school.schoolName}</div>
                <div className="text-sm text-gray-600 mb-1">{school.schoolLevel || '-'}</div>
                <div className="text-sm text-blue-600">
                  {school.distanceKm != null ? `${Number(school.distanceKm).toFixed(2)}km` : '-'}
                </div>
              </div>
            ))
          ) : (
            <div className="text-center text-gray-500 py-4">학교 정보가 없습니다</div>
          )}
        </div>
      </div>

      {/* 인근 지하철역 */}
      <div className="border-b pb-4">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">인근 지하철역</h3>
        <div className="space-y-3">
          {nearbySubways.length > 0 ? (
            nearbySubways.slice(0, 3).map((subway, index) => (
              <div key={index} className="p-3 bg-gray-50 rounded-lg">
                <div className="font-medium text-gray-900 mb-1">{subway.stationName}</div>
                <div className="text-sm text-gray-600 mb-1">
                  {subway.lineNames?.join(', ') || '-'}
                </div>
                <div className="text-sm text-blue-600">
                  {subway.distanceKm ? `${subway.distanceKm.toFixed(2)}km` : '-'}
                </div>
              </div>
            ))
          ) : (
            <div className="text-center text-gray-500 py-4">지하철역 정보가 없습니다</div>
          )}
        </div>
      </div>

      {/* 인근 버스 */}
      <div className="border-b pb-4">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold text-gray-900">인근 버스</h3>
        </div>
        <div className="text-sm text-gray-600">
          반경 500m 내 <span className="font-semibold text-gray-900">{busStations.length}개</span> 정류장
        </div>
      </div>

      {/* 해당 동 병원 */}
      <div className="border-b pb-4">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold text-gray-900">해당 동 병원</h3>
          <button
            onClick={() => setShowHospitalModal(true)}
            className="text-sm text-blue-600 hover:text-blue-700"
          >
            상세
          </button>
        </div>
        <div className="text-sm text-gray-600">
          {resolvedRegion?.dong ? (
            <>
              <span className="font-semibold text-gray-900">{resolvedRegion.dong}</span> 동 내{' '}
              <span className="font-semibold text-gray-900">{hospitalCount}개</span> 병원
            </>
          ) : (
            <span className="text-gray-500">동 정보가 없습니다</span>
          )}
        </div>
      </div>

      {/* 매물 상세정보보기 버튼 */}
      <div>
        <button
          onClick={() => onShowDetail && onShowDetail(apartmentId)}
          className="w-full py-3 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 transition-colors"
        >
          매물 상세정보보기
        </button>
      </div>

      {/* 그래프 크게보기 모달 */}
      {showGraphModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-full max-w-4xl max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-2xl font-bold text-gray-900">월별 거래량</h2>
              <button
                onClick={() => setShowGraphModal(false)}
                className="text-gray-500 hover:text-gray-700 text-2xl"
              >
                ×
              </button>
            </div>

            {/* 기간 선택 버튼 */}
            <div className="flex gap-2 mb-6">
              {[6, 12, 24, 36, 48].map((period) => (
                <button
                  key={period}
                  onClick={() => setGraphModalPeriod(period)}
                  className={`px-4 py-2 rounded ${
                    graphModalPeriod === period
                      ? 'bg-blue-600 text-white'
                      : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                  }`}
                >
                  {period === 6 ? '6개월' : period === 12 ? '1년' : `${period / 12}년`}
                </button>
              ))}
            </div>

            {/* 큰 그래프 */}
            <div className="mt-6">
              <div ref={modalChartBoxRef} className="relative w-full flex justify-center">
                <div
                  ref={modalChartScrollRef}
                  className="h-[320px] overflow-x-auto overflow-y-hidden w-full flex justify-center"
                >
                {modalGraph.items.length > 0 ? (() => {
                  const isSale = tradeType === '매매';
                  const chartHeight = 320;
                  const labelAreaHeight = modalGraph.unit === 'quarter' ? 52 : 24;
                  const plotHeight = Math.max(140, chartHeight - labelAreaHeight);
                  const yMax = modalGraph.maxCount;
                  const yDomainMax = computeDomainMax(yMax);
                  const n = modalGraph.items.length;
                  const groupWidth = isSale ? BAR_WIDTH_PX : (BAR_WIDTH_PX * 2 + GROUP_INNER_GAP_PX);
                  const pointPx = groupWidth + BAR_GAP_PX;
                  const chartContentWidthPx = Math.max(0, n * pointPx - (n > 0 ? BAR_GAP_PX : 0));
                  const chartOuterWidthPx = Math.max(0, chartContentWidthPx + CHART_PAD_X_PX * 2);

                  if (process.env.NODE_ENV === 'development') {
                    // eslint-disable-next-line no-console
                    console.log('[SidePanner yScale][modal]', { period: graphModalPeriod, len: n, yMax, yDomain: [0, yDomainMax] });
                  }

                  return (
                    <div
                      className="flex items-end justify-start"
                      style={{
                        height: `${chartHeight}px`,
                        gap: `${BAR_GAP_PX}px`,
                        paddingLeft: `${CHART_PAD_X_PX}px`,
                        paddingRight: `${CHART_PAD_X_PX}px`,
                        width: `${chartOuterWidthPx}px`,
                      }}
                    >
                      {modalGraph.items.map((item, index) => {
                        if (isSale) {
                          const saleCount = toNumber(item.saleCount);
                          const saleHeightPx = yDomainMax > 0 ? (saleCount / yDomainMax) * plotHeight : 0;
                          return (
                            <div
                              key={item.key || index}
                              className="flex flex-col items-center"
                              style={{ width: `${groupWidth}px` }}
                            >
                              <div className="w-full flex justify-center items-end" style={{ height: `${plotHeight}px` }}>
                                {saleCount > 0 ? (
                                  <div
                                    className="bg-blue-600 rounded-t"
                                    style={{ width: `${BAR_WIDTH_PX}px`, height: `${Math.max(saleHeightPx, 4)}px` }}
                                    onMouseEnter={(e) => {
                                      const rect = e.currentTarget.getBoundingClientRect();
                                      const box = modalChartBoxRef.current?.getBoundingClientRect();
                                      if (box) {
                                        const lines =
                                          modalGraph.unit === 'quarter'
                                            ? [
                                                quarterTooltipHeaderFromKey(item.key),
                                                quarterMonthRangeLineFromKey(item.key),
                                                `거래량 ${saleCount}건`,
                                              ]
                                            : [item.tooltipTitle, `거래량 ${saleCount}건`];
                                        setModalTooltip({
                                          lines,
                                          x: rect.left - box.left + rect.width / 2,
                                          y: rect.top - box.top - 10,
                                        });
                                      }
                                    }}
                                    onMouseLeave={() => setModalTooltip(null)}
                                  />
                                ) : (
                                  <div className="h-[2px] bg-gray-200" style={{ width: `${BAR_WIDTH_PX}px` }} title="거래 없음" />
                                )}
                              </div>
                              {/* x축 라벨: 모든 포인트 표시 (분기=2줄) */}
                              <div style={{ height: `${labelAreaHeight}px` }} className="flex items-center justify-center">
                                {modalGraph.unit === 'quarter' ? (() => {
                                  const lines = quarterLabelLinesFromKey(item.key);
                                  return (
                                    <div className="text-[11px] text-gray-600 text-center leading-tight">
                                      <span className="block">{lines?.top || ''}</span>
                                      <span className="block">{lines?.bottom || ''}</span>
                                    </div>
                                  );
                                })() : (
                                  <div className="text-xs text-gray-600 text-center whitespace-nowrap leading-none">
                                    {item.label || fallbackMonthLabelFromKey(item.key)}
                                  </div>
                                )}
                              </div>
                            </div>
                          );
                        }

                        const jeonseCount = toNumber(item.jeonseCount);
                        const wolseCount = toNumber(item.wolseCount);
                        const jeonseHeightPx = yDomainMax > 0 ? (jeonseCount / yDomainMax) * plotHeight : 0;
                        const wolseHeightPx = yDomainMax > 0 ? (wolseCount / yDomainMax) * plotHeight : 0;

                        return (
                          <div
                            key={item.key || index}
                            className="flex flex-col items-center"
                            style={{ width: `${groupWidth}px` }}
                          >
                            <div className="w-full flex justify-center items-end" style={{ height: `${plotHeight}px`, gap: `${GROUP_INNER_GAP_PX}px` }}>
                              {jeonseCount > 0 && (
                                <div
                                  className="bg-blue-600 rounded-t"
                                  style={{ width: `${BAR_WIDTH_PX}px`, height: `${Math.max(jeonseHeightPx, 4)}px` }}
                                  onMouseEnter={(e) => {
                                    const rect = e.currentTarget.getBoundingClientRect();
                                    const box = modalChartBoxRef.current?.getBoundingClientRect();
                                    if (box) {
                                      const lines =
                                        modalGraph.unit === 'quarter'
                                          ? [
                                              quarterTooltipHeaderFromKey(item.key),
                                              quarterMonthRangeLineFromKey(item.key),
                                              `거래량 ${jeonseCount}건`,
                                            ]
                                          : [item.tooltipTitle, `거래량 ${jeonseCount}건`];
                                      setModalTooltip({
                                        lines,
                                        x: rect.left - box.left + rect.width / 2,
                                        y: rect.top - box.top - 10,
                                      });
                                    }
                                  }}
                                  onMouseLeave={() => setModalTooltip(null)}
                                />
                              )}
                              {wolseCount > 0 && (
                                <div
                                  className="bg-green-600 rounded-t"
                                  style={{ width: `${BAR_WIDTH_PX}px`, height: `${Math.max(wolseHeightPx, 4)}px` }}
                                  onMouseEnter={(e) => {
                                    const rect = e.currentTarget.getBoundingClientRect();
                                    const box = modalChartBoxRef.current?.getBoundingClientRect();
                                    if (box) {
                                      const lines =
                                        modalGraph.unit === 'quarter'
                                          ? [
                                              quarterTooltipHeaderFromKey(item.key),
                                              quarterMonthRangeLineFromKey(item.key),
                                              `거래량 ${wolseCount}건`,
                                            ]
                                          : [item.tooltipTitle, `거래량 ${wolseCount}건`];
                                      setModalTooltip({
                                        lines,
                                        x: rect.left - box.left + rect.width / 2,
                                        y: rect.top - box.top - 10,
                                      });
                                    }
                                  }}
                                  onMouseLeave={() => setModalTooltip(null)}
                                />
                              )}
                              {jeonseCount === 0 && wolseCount === 0 && (
                                <div className="h-[2px] bg-gray-200" style={{ width: `${groupWidth}px` }} title="거래 없음" />
                              )}
                            </div>
                            {/* x축 라벨: 모든 포인트 표시 (분기=2줄) */}
                            <div style={{ height: `${labelAreaHeight}px` }} className="flex items-center justify-center">
                              {modalGraph.unit === 'quarter' ? (() => {
                                const lines = quarterLabelLinesFromKey(item.key);
                                return (
                                  <div className="text-[11px] text-gray-600 text-center leading-tight">
                                    <span className="block">{lines?.top || ''}</span>
                                    <span className="block">{lines?.bottom || ''}</span>
                                  </div>
                                );
                              })() : (
                                <div className="text-xs text-gray-600 text-center whitespace-nowrap leading-none">
                                  {item.label || fallbackMonthLabelFromKey(item.key)}
                                </div>
                              )}
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  );
                })() : (
                  <div className="w-full text-center text-gray-500 py-8">거래 데이터가 없습니다</div>
                )}
              </div>
              <FloatingTooltip tooltip={modalTooltip} />
            </div>
          </div>
        </div>
        </div>
      )}

      {/* 병원 상세 모달 */}
      {showHospitalModal && hospitalStats && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-full max-w-2xl max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-2xl font-bold text-gray-900">병원 상세 정보</h2>
              <button
                onClick={() => setShowHospitalModal(false)}
                className="text-gray-500 hover:text-gray-700 text-2xl"
              >
                ×
              </button>
            </div>

            <div className="mb-4">
              <div className="text-sm text-gray-600 mb-2">
                {hospitalStats.sido} {hospitalStats.gugun} {hospitalStats.dong}
              </div>
              <div className="text-lg font-semibold text-gray-900">
                총 {hospitalStats.totalCount}개 병원
              </div>
            </div>

            {/* 종류별 드롭다운 */}
            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-2">병원 종류</label>
              <select
                value={selectedHospitalType}
                onChange={(e) => setSelectedHospitalType(e.target.value)}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900"
              >
                <option value="전체">전체</option>
                {hospitalStats.countByTypeName &&
                  Object.keys(hospitalStats.countByTypeName).map((type) => (
                    <option key={type} value={type}>
                      {type}
                    </option>
                  ))}
              </select>
            </div>

            {/* 종류별 개수 표시 */}
            <div className="space-y-2">
              {hospitalStats.countByTypeName &&
                Object.entries(hospitalStats.countByTypeName)
                  .filter(([type]) => selectedHospitalType === '전체' || type === selectedHospitalType)
                  .map(([type, count]) => (
                    <div
                      key={type}
                      className="flex justify-between items-center p-3 bg-gray-50 rounded-lg"
                    >
                      <span className="text-gray-900">{type}</span>
                      <span className="font-semibold text-blue-600">{count}개</span>
                    </div>
                  ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
