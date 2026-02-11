'use client';

import { useState, useEffect } from 'react';
import { getMonthlyTradeVolume, getRecentTrades, getNearbySubways, getNearbyBusStations } from '../../../api/apartment';
import { getHospitalCount, getHospitalStats, getHospitalListByDong } from '../../../api/hospital';

export default function SidePanner({ apartmentId, apartmentInfo, onShowDetail, onToggleBusMarker }) {
  // 월별 거래량
  const [monthlyData, setMonthlyData] = useState([]);
  const [selectedPeriod, setSelectedPeriod] = useState(6);
  const [showGraphModal, setShowGraphModal] = useState(false);
  const [graphModalPeriod, setGraphModalPeriod] = useState(6);

  // 최근 거래내역
  const [recentTrades, setRecentTrades] = useState([]);

  // 인근 지하철역
  const [nearbySubways, setNearbySubways] = useState([]);

  // 인근 버스
  const [busStations, setBusStations] = useState([]);
  const [busMarkerVisible, setBusMarkerVisible] = useState(false);

  // 인근 병원
  const [hospitalCount, setHospitalCount] = useState(0);
  const [hospitalStats, setHospitalStats] = useState(null);
  const [showHospitalModal, setShowHospitalModal] = useState(false);
  const [selectedHospitalType, setSelectedHospitalType] = useState('전체');

  // 로딩 상태
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!apartmentId) return;

    const fetchData = async () => {
      try {
        setLoading(true);
        
        // 월별 거래량
        const monthly = await getMonthlyTradeVolume(apartmentId, selectedPeriod);
        setMonthlyData(monthly || []);

        // 최근 거래내역
        const trades = await getRecentTrades(apartmentId);
        setRecentTrades(trades || []);

        // 인근 지하철역
        const subways = await getNearbySubways(apartmentId);
        setNearbySubways(subways || []);

        // 인근 버스 정류장
        const buses = await getNearbyBusStations(apartmentId, 500, 50);
        setBusStations(buses?.items || []);

        // 인근 병원 (동 정보 필요)
        if (apartmentInfo?.dong) {
          const count = await getHospitalCount({
            sido: apartmentInfo.sido || '',
            gugun: apartmentInfo.gugun || '',
            dong: apartmentInfo.dong || ''
          });
          setHospitalCount(count || 0);

          const stats = await getHospitalStats({
            sido: apartmentInfo.sido || '',
            gugun: apartmentInfo.gugun || '',
            dong: apartmentInfo.dong || ''
          });
          setHospitalStats(stats);
        }
      } catch (error) {
        console.error('데이터 로딩 실패:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [apartmentId, selectedPeriod, apartmentInfo]);

  // 그래프 모달 데이터 로드
  useEffect(() => {
    if (showGraphModal && apartmentId) {
      const fetchGraphData = async () => {
        try {
          const data = await getMonthlyTradeVolume(apartmentId, graphModalPeriod);
          setMonthlyData(data || []);
        } catch (error) {
          console.error('그래프 데이터 로딩 실패:', error);
        }
      };
      fetchGraphData();
    }
  }, [showGraphModal, graphModalPeriod, apartmentId]);

  const handleBusMarkerToggle = (checked) => {
    setBusMarkerVisible(checked);
    if (onToggleBusMarker) {
      onToggleBusMarker(busStations, checked);
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
    const year = yyyymm.substring(0, 4);
    const month = yyyymm.substring(4, 6);
    return `${year}.${month}`;
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
        <div className="h-48 flex items-end gap-2">
          {monthlyData.length > 0 ? (
            monthlyData.map((item, index) => {
              const maxCount = Math.max(
                ...monthlyData.map(d => Math.max(d.jeonseCount || 0, d.wolseCount || 0))
              );
              const jeonseHeight = maxCount > 0 ? ((item.jeonseCount || 0) / maxCount) * 100 : 0;
              const wolseHeight = maxCount > 0 ? ((item.wolseCount || 0) / maxCount) * 100 : 0;

              return (
                <div key={index} className="flex-1 flex flex-col items-center">
                  <div className="w-full flex gap-0.5 justify-center items-end h-full">
                    <div
                      className="w-full bg-blue-500 rounded-t"
                      style={{ height: `${jeonseHeight}%` }}
                      title={`전세: ${item.jeonseCount || 0}건`}
                    />
                    <div
                      className="w-full bg-green-500 rounded-t"
                      style={{ height: `${wolseHeight}%` }}
                      title={`월세: ${item.wolseCount || 0}건`}
                    />
                  </div>
                  <div className="text-xs text-gray-600 mt-2 transform -rotate-45 origin-top-left whitespace-nowrap">
                    {formatMonth(item.yyyymm)}
                  </div>
                </div>
              );
            })
          ) : (
            <div className="w-full text-center text-gray-500 py-8">거래 데이터가 없습니다</div>
          )}
        </div>
      </div>

      {/* 최근 거래내역 */}
      <div className="border-b pb-4">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">최근 거래내역</h3>
        <div className="space-y-3">
          {recentTrades.length > 0 ? (
            recentTrades.slice(0, 5).map((trade) => (
              <div key={trade.id} className="p-3 bg-gray-50 rounded-lg">
                <div className="flex justify-between items-start mb-2">
                  <div className="text-sm text-gray-600">
                    {trade.floor}층 · {trade.exclusiveArea ? `${trade.exclusiveArea}㎡` : '-'}
                  </div>
                  <div className="text-sm text-gray-500">{formatDate(trade.dealDate)}</div>
                </div>
                <div className="text-sm text-gray-900">
                  보증금: {formatPrice(trade.deposit)}
                  {trade.monthlyRent && ` / 월세: ${formatPrice(trade.monthlyRent)}`}
                </div>
              </div>
            ))
          ) : (
            <div className="text-center text-gray-500 py-4">거래내역이 없습니다</div>
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
          <label className="flex items-center gap-2 cursor-pointer">
            <input
              type="checkbox"
              checked={busMarkerVisible}
              onChange={(e) => handleBusMarkerToggle(e.target.checked)}
              className="w-4 h-4 text-blue-600 rounded focus:ring-blue-500"
            />
            <span className="text-sm text-gray-700">마커 표시</span>
          </label>
        </div>
        <div className="text-sm text-gray-600">
          반경 500m 내 <span className="font-semibold text-gray-900">{busStations.length}개</span> 정류장
        </div>
      </div>

      {/* 인근 병원 */}
      <div className="border-b pb-4">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold text-gray-900">인근 병원</h3>
          <button
            onClick={() => setShowHospitalModal(true)}
            className="text-sm text-blue-600 hover:text-blue-700"
          >
            상세
          </button>
        </div>
        <div className="text-sm text-gray-600">
          {apartmentInfo?.dong ? (
            <>
              <span className="font-semibold text-gray-900">{apartmentInfo.dong}</span> 동 내{' '}
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
            <div className="h-96 flex items-end gap-2">
              {monthlyData.length > 0 ? (
                monthlyData.map((item, index) => {
                  const maxCount = Math.max(
                    ...monthlyData.map(d => Math.max(d.jeonseCount || 0, d.wolseCount || 0))
                  );
                  const jeonseHeight = maxCount > 0 ? ((item.jeonseCount || 0) / maxCount) * 100 : 0;
                  const wolseHeight = maxCount > 0 ? ((item.wolseCount || 0) / maxCount) * 100 : 0;

                  return (
                    <div key={index} className="flex-1 flex flex-col items-center">
                      <div className="w-full flex gap-1 justify-center items-end h-full">
                        <div
                          className="w-full bg-blue-500 rounded-t"
                          style={{ height: `${jeonseHeight}%` }}
                          title={`전세: ${item.jeonseCount || 0}건`}
                        />
                        <div
                          className="w-full bg-green-500 rounded-t"
                          style={{ height: `${wolseHeight}%` }}
                          title={`월세: ${item.wolseCount || 0}건`}
                        />
                      </div>
                      <div className="text-xs text-gray-600 mt-2 transform -rotate-45 origin-top-left whitespace-nowrap">
                        {formatMonth(item.yyyymm)}
                      </div>
                      <div className="text-xs text-gray-500 mt-1">
                        전세: {item.jeonseCount || 0} / 월세: {item.wolseCount || 0}
                      </div>
                    </div>
                  );
                })
              ) : (
                <div className="w-full text-center text-gray-500 py-8">거래 데이터가 없습니다</div>
              )}
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
