'use client';

import { useState, useEffect, useCallback } from 'react';
import { getMonthlyTradeVolume, getNearbySubways, getNearbyBusStations, getNearbySchools, getApartmentRegion } from '../../../api/apartment';
import { getAptSaleSummary } from '../../../api/apartment_sale';
import { getRecentRentTrades } from '../../../api/apartment_rent';
import { getHospitalCount, getHospitalStats, getHospitalListByDong } from '../../../api/hospital';

export default function SidePanner({ apartmentId, apartmentInfo, schoolLevels, tradeType = '매매', onShowDetail, onToggleBusMarker, onToggleSchoolMarker }) {
  // 월별 거래량
  const [monthlyData, setMonthlyData] = useState([]);
  const [selectedPeriod, setSelectedPeriod] = useState(6);
  const [showGraphModal, setShowGraphModal] = useState(false);
  const [graphModalPeriod, setGraphModalPeriod] = useState(6);
  // Tooltip 상태
  const [tooltip, setTooltip] = useState(null);

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
            setMonthlyData(saleData);
          } else {
            setMonthlyData(rawData || []);
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

  const formatMonth = (yyyymm) => {
    if (!yyyymm || yyyymm.length !== 6) return '';
    const year = yyyymm.substring(2, 4); // YY 형식
    const month = yyyymm.substring(4, 6);
    return `${year}${month}`;
  };

  // Tooltip용 월 포맷 (YYYY-MM 형식)
  const formatMonthForTooltip = (yyyymm) => {
    if (!yyyymm || yyyymm.length !== 6) return '';
    const year = yyyymm.substring(0, 4); // YYYY 형식
    const month = yyyymm.substring(4, 6);
    return `${year}-${month}`;
  };

  if (loading) {
    return (
      <div className="w-full h-full flex items-center justify-center">
        <div className="text-gray-500">로딩 중...</div>
      </div>
    );
  }

  return (
    <div className="w-full h-full overflow-y-auto bg-white p-6 space-y-6">
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
          <div className="h-48 flex items-end gap-1.5">
          {monthlyData.length > 0 ? (() => {
            const isSale = tradeType === '매매';
            const graphHeight = 192; // h-48 = 192px
            
            // maxCount를 한 번만 계산
            const maxCount = Math.max(
              1, // 최소값 1로 설정하여 0으로 나누는 것을 방지
              ...monthlyData.map(d => {
                if (isSale) {
                  return Number(d.saleCount) || 0;
                }
                return Math.max(
                  Number(d.jeonseCount) || 0,
                  Number(d.wolseCount) || 0
                );
              })
            );

            return monthlyData.map((item, index) => {
              if (isSale) {
                const saleCount = Number(item.saleCount) || 0;
                const saleHeightPx = maxCount > 0 ? (saleCount / maxCount) * graphHeight : 0;

                return (
                  <div key={item.yyyymm || index} className="flex-1 flex flex-col items-center min-w-0">
                    <div className="w-full flex justify-center items-end" style={{ height: `${graphHeight}px` }}>
                      {saleCount > 0 ? (
                        <div
                          className="w-full bg-blue-600 rounded-t cursor-pointer hover:bg-blue-700 transition-colors"
                          style={{ height: `${Math.max(saleHeightPx, 4)}px` }}
                          onMouseEnter={(e) => {
                            const rect = e.currentTarget.getBoundingClientRect();
                            const containerRect = e.currentTarget.closest('.relative')?.getBoundingClientRect();
                            if (containerRect) {
                              setTooltip({
                                month: formatMonthForTooltip(item.yyyymm),
                                count: saleCount,
                                type: '매매',
                                x: rect.left - containerRect.left + rect.width / 2,
                                y: rect.top - containerRect.top - 10,
                              });
                            }
                          }}
                          onMouseLeave={() => setTooltip(null)}
                        />
                      ) : (
                        <div className="w-full h-[2px] bg-gray-200" />
                      )}
                    </div>
                    <div className="text-xs text-gray-600 mt-2 text-center whitespace-nowrap">
                      {formatMonth(item.yyyymm)}
                    </div>
                  </div>
                );
              }

              // 전월세: 전세와 월세를 별도 막대로 표시
              const jeonseCount = Number(item.jeonseCount) || 0;
              const wolseCount = Number(item.wolseCount) || 0;
              const jeonseHeightPx = maxCount > 0 ? (jeonseCount / maxCount) * graphHeight : 0;
              const wolseHeightPx = maxCount > 0 ? (wolseCount / maxCount) * graphHeight : 0;

              return (
                <div key={item.yyyymm || index} className="flex-1 flex flex-col items-center min-w-0">
                  <div className="w-full flex gap-1 justify-center items-end" style={{ height: `${graphHeight}px` }}>
                    {jeonseCount > 0 && (
                      <div
                        className="flex-1 bg-blue-600 rounded-t cursor-pointer hover:bg-blue-700 transition-colors"
                        style={{ height: `${Math.max(jeonseHeightPx, 4)}px` }}
                        onMouseEnter={(e) => {
                          const rect = e.currentTarget.getBoundingClientRect();
                          const containerRect = e.currentTarget.closest('.relative')?.getBoundingClientRect();
                          if (containerRect) {
                            setTooltip({
                              month: formatMonthForTooltip(item.yyyymm),
                              count: jeonseCount,
                              type: '전세',
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
                        className="flex-1 bg-green-600 rounded-t cursor-pointer hover:bg-green-700 transition-colors"
                        style={{ height: `${Math.max(wolseHeightPx, 4)}px` }}
                        onMouseEnter={(e) => {
                          const rect = e.currentTarget.getBoundingClientRect();
                          const containerRect = e.currentTarget.closest('.relative')?.getBoundingClientRect();
                          if (containerRect) {
                            setTooltip({
                              month: formatMonthForTooltip(item.yyyymm),
                              count: wolseCount,
                              type: '월세',
                              x: rect.left - containerRect.left + rect.width / 2,
                              y: rect.top - containerRect.top - 10,
                            });
                          }
                        }}
                        onMouseLeave={() => setTooltip(null)}
                      />
                    )}
                    {jeonseCount === 0 && wolseCount === 0 && (
                      <div className="w-full h-[2px] bg-gray-200" />
                    )}
                  </div>
                  <div className="text-xs text-gray-600 mt-2 text-center whitespace-nowrap">
                    {formatMonth(item.yyyymm)}
                  </div>
                </div>
              );
            });
          })() : (
            <div className="w-full text-center text-gray-500 py-8">거래 데이터가 없습니다</div>
          )}
          </div>
          {/* Tooltip */}
          {tooltip && (
            <div
              className="absolute z-50 bg-gray-900 text-white text-xs rounded-lg px-3 py-2 shadow-lg pointer-events-none"
              style={{
                left: `${tooltip.x}px`,
                top: `${tooltip.y}px`,
                transform: 'translate(-50%, -100%)',
              }}
            >
              <div className="font-medium mb-1">{tooltip.month}</div>
              <div className="text-gray-300">
                {tooltip.type}: {tooltip.count}건
              </div>
              {/* 말풍선 꼬리 */}
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
          )}
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
            <div className="mt-[100px]">
              <div className="h-96 flex items-end gap-2">
              {monthlyData.length > 0 ? (() => {
                const isSale = tradeType === '매매';
                const graphHeight = 384; // h-96 = 384px
                
                // maxCount를 한 번만 계산
                const maxCount = Math.max(
                  1, // 최소값 1로 설정하여 0으로 나누는 것을 방지
                  ...monthlyData.map(d => {
                    if (isSale) {
                      return Number(d.saleCount) || 0;
                    }
                    return Math.max(
                      Number(d.jeonseCount) || 0,
                      Number(d.wolseCount) || 0
                    );
                  })
                );

                return monthlyData.map((item, index) => {
                  if (isSale) {
                    const saleCount = Number(item.saleCount) || 0;
                    const saleHeightPx = maxCount > 0 ? (saleCount / maxCount) * graphHeight : 0;

                    return (
                      <div key={item.yyyymm || index} className="flex-1 flex flex-col items-center min-w-0">
                        <div className="w-full flex justify-center items-end" style={{ height: `${graphHeight}px` }}>
                          {saleCount > 0 ? (
                            <div
                              className="w-full bg-blue-600 rounded-t"
                              style={{ height: `${Math.max(saleHeightPx, 4)}px` }}
                              title={`매매: ${saleCount}건`}
                            />
                          ) : (
                            <div className="w-full h-[2px] bg-gray-200" title="거래 없음" />
                          )}
                        </div>
                        <div className="text-xs text-gray-600 mt-2 text-center whitespace-nowrap">
                          {formatMonth(item.yyyymm)}
                        </div>
                        <div className="text-xs text-gray-500 mt-1">
                          매매: {saleCount}건
                        </div>
                      </div>
                    );
                  }

                  // 전월세: 전세와 월세를 별도 막대로 표시
                  const jeonseCount = Number(item.jeonseCount) || 0;
                  const wolseCount = Number(item.wolseCount) || 0;
                  const jeonseHeightPx = maxCount > 0 ? (jeonseCount / maxCount) * graphHeight : 0;
                  const wolseHeightPx = maxCount > 0 ? (wolseCount / maxCount) * graphHeight : 0;

                  return (
                    <div key={item.yyyymm || index} className="flex-1 flex flex-col items-center min-w-0">
                      <div className="w-full flex gap-1.5 justify-center items-end" style={{ height: `${graphHeight}px` }}>
                        {jeonseCount > 0 && (
                          <div
                            className="flex-1 bg-blue-600 rounded-t"
                            style={{ height: `${Math.max(jeonseHeightPx, 4)}px` }}
                            title={`전세: ${jeonseCount}건`}
                          />
                        )}
                        {wolseCount > 0 && (
                          <div
                            className="flex-1 bg-green-600 rounded-t"
                            style={{ height: `${Math.max(wolseHeightPx, 4)}px` }}
                            title={`월세: ${wolseCount}건`}
                          />
                        )}
                        {jeonseCount === 0 && wolseCount === 0 && (
                          <div className="w-full h-[2px] bg-gray-200" title="거래 없음" />
                        )}
                      </div>
                      <div className="text-xs text-gray-600 mt-2 text-center whitespace-nowrap">
                        {formatMonth(item.yyyymm)}
                      </div>
                      <div className="text-xs text-gray-500 mt-1">
                        전세: {jeonseCount} / 월세: {wolseCount}
                      </div>
                    </div>
                  );
                });
              })() : (
                <div className="w-full text-center text-gray-500 py-8">거래 데이터가 없습니다</div>
              )}
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
