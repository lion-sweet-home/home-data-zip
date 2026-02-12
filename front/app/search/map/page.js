'use client';

import { useState, useEffect, useCallback } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import Filter from './components/filter';
import Map from './components/map';
import SidePanner from './components/side_panner';
import ApartmentList from './components/sp_apartment_list';
import { get } from '../../api/api';
import { getSaleMarkers } from '../../api/apartment_sale';
import { getRentMarkers } from '../../api/apartment_rent';

export default function MapSearchPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  
  // 선택된 아파트 정보
  const [selectedApartment, setSelectedApartment] = useState(null);
  const [showSidePanner, setShowSidePanner] = useState(false);

  // 지도 마커 데이터
  const [apartments, setApartments] = useState([]);
  const [mapCenter, setMapCenter] = useState({ lat: 37.5665, lng: 126.9780 });
  const [loading, setLoading] = useState(false);

  // 버스 마커 관련
  const [busMarkers, setBusMarkers] = useState([]);
  const [showBusMarkers, setShowBusMarkers] = useState(false);

  // 학교 마커 관련
  const [schoolMarkers, setSchoolMarkers] = useState([]);
  const [showSchoolMarkers, setShowSchoolMarkers] = useState(false);

  // 현재 검색 파라미터 저장 (마커 클릭 시 사용)
  const [currentSearchParams, setCurrentSearchParams] = useState(null);

  // Filter 초기값(= /search에서 넘어온 값) 주입용
  const [initialFilterParams, setInitialFilterParams] = useState(null);

  // URL 파라미터에서 초기 검색 조건 로드
  useEffect(() => {
    const sido = searchParams.get('sido');
    const gugun = searchParams.get('gugun');
    const dong = searchParams.get('dong');

    const subwayStationId = searchParams.get('subwayStationId');
    const subwayStationName = searchParams.get('subwayStationName');
    const subwayRadius = searchParams.get('subwayRadius');

    const priceMin = searchParams.get('priceMin');
    const priceMax = searchParams.get('priceMax');
    const depositMin = searchParams.get('depositMin');
    const depositMax = searchParams.get('depositMax');
    const monthlyRentMin = searchParams.get('monthlyRentMin');
    const monthlyRentMax = searchParams.get('monthlyRentMax');

    const schoolTypesStr = searchParams.get('schoolTypes'); // "초등학교,중학교"
    const schoolRadius = searchParams.get('schoolRadius');

    // tradeType은 쿼리스트링에 넣지 않기로 했으니, /search에서 sessionStorage로 넘긴 값 우선 사용
    let tradeType = '매매';
    try {
      const stored = typeof window !== 'undefined' ? sessionStorage.getItem('search_tradeType') : null;
      if (stored === '매매' || stored === '전월세') tradeType = stored;
    } catch (e) {
      // ignore
    }
    // URL에 전월세 필드가 있으면 전월세로 추론 (sessionStorage가 없을 때 대비)
    if (
      depositMin != null ||
      depositMax != null ||
      monthlyRentMin != null ||
      monthlyRentMax != null
    ) {
      tradeType = '전월세';
    }

    const searchConditionType = subwayStationId ? 'subway' : 'region';

    const initial = {
      tradeType,
      searchConditionType,
      period: 6,
      // region
      ...(sido ? { sido } : {}),
      ...(gugun ? { gugun } : {}),
      ...(dong ? { dong } : {}),
      ...(priceMin ? { priceMin } : {}),
      ...(priceMax ? { priceMax } : {}),
      ...(depositMin ? { depositMin } : {}),
      ...(depositMax ? { depositMax } : {}),
      ...(monthlyRentMin ? { monthlyRentMin } : {}),
      ...(monthlyRentMax ? { monthlyRentMax } : {}),
      ...(schoolTypesStr ? { schoolTypes: schoolTypesStr } : {}),
      ...(schoolRadius ? { schoolRadius } : {}),
      // subway
      ...(subwayStationId ? { subwayStationId } : {}),
      ...(subwayStationName ? { subwayStationName } : {}),
      ...(subwayRadius ? { subwayRadius } : {}),
    };

    setInitialFilterParams(initial);

    // URL 파라미터가 있으면 검색 실행 (region만 확실히 동작)
    if (searchConditionType === 'region' && sido && gugun) {
      handleSearch(initial);
    } else if (searchConditionType === 'subway' && subwayStationId) {
      // TODO: subway 검색도 마커 조회 로직 완성되면 자동 검색 활성화
      // handleSearch(initial);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchParams]);

  // 검색 실행
  const handleSearch = async (searchParams) => {
    setLoading(true);
    setSelectedApartment(null);
    setShowSidePanner(false);
    setBusMarkers([]);
    setShowBusMarkers(false);
    setSchoolMarkers([]);
    setShowSchoolMarkers(false);

    try {
      const tradeType = searchParams.tradeType || '매매';
      
      // 현재 검색 파라미터 저장 (마커 클릭 시 사용)
      setCurrentSearchParams(searchParams);

      // 마커 조회
      let markers = [];

      if (searchParams.searchConditionType === 'region') {
        // 지역 검색 - 마커 API 호출
        if (tradeType === '매매') {
          const markerRequest = {
            sido: searchParams.sido,
            gugun: searchParams.gugun,
            dong: searchParams.dong,
            minAmount: searchParams.priceMin ? Number(searchParams.priceMin) * 10000 : undefined,
            maxAmount: searchParams.priceMax ? Number(searchParams.priceMax) * 10000 : undefined,
            periodMonths: searchParams.period || 6,
          };
          const response = await getSaleMarkers(markerRequest);
          markers = (response || []).map(m => ({
            lat: m.latitude,
            lng: m.longitude,
            title: m.aptNm,
            info: '',
            apartmentId: m.aptId,
            apartmentData: m,
          }));
        } else {
          const markerRequest = {
            sido: searchParams.sido,
            gugun: searchParams.gugun,
            dong: searchParams.dong,
            minDeposit: searchParams.depositMin ? Number(searchParams.depositMin) * 10000 : undefined,
            maxDeposit: searchParams.depositMax ? Number(searchParams.depositMax) * 10000 : undefined,
            minMonthlyRent: searchParams.monthlyRentMin ? Number(searchParams.monthlyRentMin) * 10000 : undefined,
            maxMonthlyRent: searchParams.monthlyRentMax ? Number(searchParams.monthlyRentMax) * 10000 : undefined,
          };
          const response = await getRentMarkers(markerRequest);
          markers = (response || []).map(m => ({
            lat: m.latitude,
            lng: m.longitude,
            title: m.aptNm,
            info: '',
            apartmentId: m.aptId,
            apartmentData: m,
          }));
        }
      } else if (searchParams.searchConditionType === 'subway') {
        // 지하철역 검색: 선택한 역 + 반경 내 아파트 마커 표시
        const distanceKm = Number(searchParams.subwayRadius || 1.0);
        const subwayResponse = await get(
          `/subway/stations/${searchParams.subwayStationId}/apartments?distanceKm=${distanceKm}`
        );

        markers = (subwayResponse || []).map((apt) => ({
          lat: apt.latitude,
          lng: apt.longitude,
          title: apt.aptName,
          info: apt.distanceKm != null ? `역까지 ${Number(apt.distanceKm).toFixed(2)}km` : '',
          apartmentId: apt.apartmentId,
          apartmentData: apt,
        }));
      }

      setApartments(markers);

      // 지도 중심 조정 (모든 마커를 포함하도록)
      if (markers.length > 0) {
        // 첫 번째 마커를 중심으로 설정
        setMapCenter({
          lat: markers[0].lat,
          lng: markers[0].lng,
        });
      } else {
        // 마커가 없으면 기본 위치로
        setMapCenter({ lat: 37.5665, lng: 126.9780 });
      }
    } catch (error) {
      console.error('검색 실패:', error);
      alert('검색에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  // 마커 클릭 핸들러
  const handleMarkerClick = async (markerData, index) => {
    const apartment = markerData.apartmentData || markerData;
    const apartmentId = apartment.aptId || apartment.apartmentId || apartment.id;
    
    // 아파트 정보 설정 (사이드 패널에 필요한 모든 정보 포함)
    setSelectedApartment({
      id: apartmentId,
      name: apartment.aptNm || apartment.aptName || apartment.aptName || apartment.name || '아파트',
      address: apartment.roadAddress || apartment.address || apartment.jibunAddress || '',
      sido: currentSearchParams?.sido || apartment.sido || '',
      gugun: currentSearchParams?.gugun || apartment.gugun || '',
      dong: currentSearchParams?.dong || apartment.dong || '',
      latitude: apartment.latitude || apartment.lat,
      longitude: apartment.longitude || apartment.lng,
    });
    setShowSidePanner(true);
  };

  // 지도 클릭 핸들러 (사이드 패널 닫기)
  const handleMapClick = () => {
    setShowSidePanner(false);
    setSelectedApartment(null);
  };

  // 버스 마커 토글
  const handleToggleBusMarker = useCallback((stations, visible) => {
    if (visible && stations) {
      const markers = stations.map((station) => ({
        latitude: station.latitude,
        longitude: station.longitude,
        name: station.name || station.stationNumber,
      }));
      setBusMarkers(markers);
    } else {
      setBusMarkers([]);
    }
    setShowBusMarkers(visible);
  }, []);

  // 학교 마커 토글
  const handleToggleSchoolMarker = useCallback((schools, visible) => {
    if (visible && schools) {
      const markers = schools.map((s) => ({
        latitude: s.latitude ?? s.lat,
        longitude: s.longitude ?? s.lng,
        schoolName: s.schoolName,
        schoolLevel: s.schoolLevel,
      }));
      setSchoolMarkers(markers);
    } else {
      setSchoolMarkers([]);
    }
    setShowSchoolMarkers(visible);
  }, []);

  // 매물 상세정보보기
  const handleShowDetail = (apartmentId) => {
    if (!apartmentId) return;

    const params = new URLSearchParams();
    params.append('aptId', String(apartmentId));
    params.append('tradeType', currentSearchParams?.tradeType || '매매');
    if (selectedApartment?.name) params.append('aptName', selectedApartment.name);
    if (selectedApartment?.address) params.append('address', selectedApartment.address);
    if (selectedApartment?.sido) params.append('sido', selectedApartment.sido);
    if (selectedApartment?.gugun) params.append('gugun', selectedApartment.gugun);
    if (selectedApartment?.dong) params.append('dong', selectedApartment.dong);
    if (currentSearchParams?.schoolTypes) {
      const schoolTypes = Array.isArray(currentSearchParams.schoolTypes)
        ? currentSearchParams.schoolTypes.join(',')
        : String(currentSearchParams.schoolTypes);
      if (schoolTypes) params.append('schoolTypes', schoolTypes);
    }

    router.push(`/apartment?${params.toString()}`);
  };

  return (
    <div className="flex flex-col h-screen">
      {/* 상단 필터 */}
      <div className="flex-shrink-0 p-4 bg-gray-50 border-b">
        <Filter onSearch={handleSearch} initialParams={initialFilterParams} />
      </div>

      {/* 하단: 지도와 사이드 패널 */}
      <div className="flex-1 flex overflow-hidden">
        {/* 좌측 패널: 기본은 목록, 마커 클릭 시 SidePanner */}
        {apartments.length > 0 && (
          <div className="w-[30%] border-r border-gray-200 overflow-hidden">
            {showSidePanner && selectedApartment ? (
              <SidePanner
                apartmentId={selectedApartment.id}
                apartmentInfo={selectedApartment}
                schoolLevels={currentSearchParams?.schoolTypes}
                onShowDetail={handleShowDetail}
                onToggleBusMarker={handleToggleBusMarker}
                onToggleSchoolMarker={handleToggleSchoolMarker}
              />
            ) : (
              <ApartmentList
                markers={apartments}
                onSelect={(markerData, index) => handleMarkerClick(markerData, index)}
              />
            )}
          </div>
        )}

        {/* 우측 지도 (7:3 비율 또는 전체) */}
        <div className={`${apartments.length > 0 ? 'w-[70%]' : 'w-full'} relative`}>
          {loading && (
            <div className="absolute inset-0 bg-white bg-opacity-75 flex items-center justify-center z-10">
              <div className="text-gray-600">검색 중...</div>
            </div>
          )}
          <Map
            center={mapCenter}
            markers={apartments}
            onMarkerClick={handleMarkerClick}
            onMapClick={handleMapClick}
            busMarkers={busMarkers}
            showBusMarkers={showBusMarkers}
            schoolMarkers={schoolMarkers}
            showSchoolMarkers={showSchoolMarkers}
          />
        </div>
      </div>
    </div>
  );
}
