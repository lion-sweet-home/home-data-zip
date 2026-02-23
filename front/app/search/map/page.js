'use client';

import { Suspense, useState, useEffect, useCallback, useRef } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import Filter from './components/filter';
import Map from './components/map';
import SidePanner from './components/side_panner';
import ApartmentList from './components/sp_apartment_list';
import { get } from '../../api/api';
import { getSaleMarkers } from '../../api/apartment_sale';
import { getRentMarkers } from '../../api/apartment_rent';

function MapSearchPageContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const hasRestoredFromSessionRef = useRef(false);
  const pendingSelectedApartmentRestoreRef = useRef(null);
  const pendingRegionMoveRef = useRef(null);
  const mapObjRef = useRef(null);
  const regionMoveInProgressRef = useRef(false);

  // idle 기반 마커 갱신: debounce + abort + 최신성 보장
  const lastIdlePayloadRef = useRef(null);
  const idleTimerRef = useRef(null);
  const markerAbortRef = useRef(null);
  const markerReqSeqRef = useRef(0);
  const centerReqSeqRef = useRef(0);

  const currentSearchParamsRef = useRef(null);
  
  // 선택된 아파트 정보
  const [selectedApartment, setSelectedApartment] = useState(null);
  const [showSidePanner, setShowSidePanner] = useState(false);

  // 지도 마커 데이터
  const [apartments, setApartments] = useState([]);
  const [mapCenter, setMapCenter] = useState({ lat: 37.5665, lng: 126.9780 });
  const [mapLevel, setMapLevel] = useState(3);
  const [loading, setLoading] = useState(false);

  // 학교 마커 관련
  const [schoolMarkers, setSchoolMarkers] = useState([]);
  const [showSchoolMarkers, setShowSchoolMarkers] = useState(false);

  // 현재 검색 파라미터 저장 (마커 클릭 시 사용)
  const [currentSearchParams, setCurrentSearchParams] = useState(null);

  // Filter 초기값(= /search에서 넘어온 값) 주입용
  const [initialFilterParams, setInitialFilterParams] = useState(null);

  // URL 파라미터에서 초기 검색 조건 로드
  useEffect(() => {
    // URL에 쿼리가 없으면(session 진입 / 새 탭 / 상세에서 복귀 등),
    // 마지막 검색조건을 sessionStorage에서 복원한다.
    const queryString = searchParams.toString();
    if (!queryString && !hasRestoredFromSessionRef.current) {
      hasRestoredFromSessionRef.current = true;
      try {
        // "상세에서 복귀" 케이스면 마지막 선택 아파트도 복원 후보로 저장한다.
        const shouldRestoreSelected = sessionStorage.getItem('search_map_restoreSelected') === '1';
        if (shouldRestoreSelected) {
          const storedSelected = sessionStorage.getItem('search_map_selectedApartment');
          if (storedSelected) {
            try {
              const parsedSelected = JSON.parse(storedSelected);
              if (parsedSelected && typeof parsedSelected === 'object') {
                pendingSelectedApartmentRestoreRef.current = parsedSelected;
              }
            } catch (e) {
              // ignore
            }
          }
          sessionStorage.removeItem('search_map_restoreSelected');
        }

        const stored = sessionStorage.getItem('search_map_lastParams');
        if (stored) {
          const parsed = JSON.parse(stored);
          if (parsed && typeof parsed === 'object') {
            setInitialFilterParams(parsed);
            // 복원 후 자동 검색
            if (parsed.searchConditionType === 'region' && parsed.sido && parsed.gugun) {
              handleSearch(parsed);
            } else if (parsed.searchConditionType === 'subway' && parsed.subwayStationId) {
              handleSearch(parsed);
            } else if (parsed.searchConditionType === 'school' && parsed.schoolId) {
              handleSearch(parsed);
            }
            return;
          }
        }
      } catch (e) {
        // ignore
      }
    }

    const sido = searchParams.get('sido');
    const gugun = searchParams.get('gugun');
    const dong = searchParams.get('dong');

    const subwayStationId = searchParams.get('subwayStationId');
    const subwayStationName = searchParams.get('subwayStationName');
    const subwayRadius = searchParams.get('subwayRadius');

    const schoolId = searchParams.get('schoolId');
    const schoolName = searchParams.get('schoolName');

    const priceMin = searchParams.get('priceMin');
    const priceMax = searchParams.get('priceMax');
    const minArea = searchParams.get('minArea');
    const maxArea = searchParams.get('maxArea');
    const depositMin = searchParams.get('depositMin');
    const depositMax = searchParams.get('depositMax');
    const monthlyRentMin = searchParams.get('monthlyRentMin');
    const monthlyRentMax = searchParams.get('monthlyRentMax');
    const minExclusive = searchParams.get('minExclusive');
    const maxExclusive = searchParams.get('maxExclusive');

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

    const searchConditionType = schoolId ? 'school' : (subwayStationId ? 'subway' : 'region');

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
      ...(minArea ? { minArea } : {}),
      ...(maxArea ? { maxArea } : {}),
      ...(depositMin ? { depositMin } : {}),
      ...(depositMax ? { depositMax } : {}),
      ...(monthlyRentMin ? { monthlyRentMin } : {}),
      ...(monthlyRentMax ? { monthlyRentMax } : {}),
      ...(minExclusive ? { minExclusive } : {}),
      ...(maxExclusive ? { maxExclusive } : {}),
      ...(schoolTypesStr ? { schoolTypes: schoolTypesStr } : {}),
      ...(schoolRadius ? { schoolRadius } : {}),
      // school
      ...(schoolId ? { schoolId } : {}),
      ...(schoolName ? { schoolName } : {}),
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
      handleSearch(initial);
    } else if (searchConditionType === 'school' && schoolId) {
      handleSearch(initial);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchParams]);

  useEffect(() => {
    currentSearchParamsRef.current = currentSearchParams;
  }, [currentSearchParams]);

  useEffect(() => {
    return () => {
      try {
        if (idleTimerRef.current) clearTimeout(idleTimerRef.current);
        markerAbortRef.current?.abort?.();
      } catch (e) {
        // ignore
      }
    };
  }, []);

  const moveMapToRegionByOneMarker = useCallback(async (paramsWithTradeType) => {
    const map = mapObjRef.current;
    if (!map) {
      pendingRegionMoveRef.current = paramsWithTradeType;
      return;
    }

    // 연속 검색 시 최신 검색만 반영
    const seq = ++centerReqSeqRef.current;
    regionMoveInProgressRef.current = true;

    const tradeType = paramsWithTradeType?.tradeType || '매매';
    const requestCommon = {
      sido: paramsWithTradeType?.sido,
      gugun: paramsWithTradeType?.gugun,
      dong: paramsWithTradeType?.dong,
      limit: 1,
    };

    try {
      let response = [];
      if (tradeType === '매매') {
        const parseArea = (val) => {
          if (val == null || val === '') return undefined;
          const num = parseFloat(val);
          return Number.isFinite(num) ? num : undefined;
        };
        const markerRequest = {
          ...requestCommon,
          minAmount: paramsWithTradeType?.priceMin ? Number(paramsWithTradeType.priceMin) * 10000 : undefined,
          maxAmount: paramsWithTradeType?.priceMax ? Number(paramsWithTradeType.priceMax) * 10000 : undefined,
          minArea: parseArea(paramsWithTradeType?.minArea),
          maxArea: parseArea(paramsWithTradeType?.maxArea),
          periodMonths: paramsWithTradeType?.period || 6,
        };
        response = await getSaleMarkers(markerRequest);
      } else {
        const parseExclusive = (val) => {
          if (!val || val === '') return undefined;
          const num = parseFloat(val);
          return isNaN(num) ? undefined : num;
        };

        const markerRequest = {
          ...requestCommon,
          minDeposit: paramsWithTradeType?.depositMin ? Number(paramsWithTradeType.depositMin) * 10000 : undefined,
          maxDeposit: paramsWithTradeType?.depositMax ? Number(paramsWithTradeType.depositMax) * 10000 : undefined,
          minMonthlyRent: paramsWithTradeType?.monthlyRentMin ? Number(paramsWithTradeType.monthlyRentMin) * 10000 : undefined,
          maxMonthlyRent: paramsWithTradeType?.monthlyRentMax ? Number(paramsWithTradeType.monthlyRentMax) * 10000 : undefined,
          minExclusive: parseExclusive(paramsWithTradeType?.minExclusive),
          maxExclusive: parseExclusive(paramsWithTradeType?.maxExclusive),
        };
        response = await getRentMarkers(markerRequest);
      }

      if (seq !== centerReqSeqRef.current) return;

      const first = Array.isArray(response) ? response[0] : null;
      const lat = first?.latitude;
      const lng = first?.longitude;
      if (lat == null || lng == null) return;

      setMapCenter({ lat, lng });
      // 지역 이동 시 너무 확대된 상태면 적당히 보여주기
      setMapLevel((prev) => (prev < 5 ? 5 : prev));
    } catch (e) {
      // ignore (해당 조건 데이터가 없으면 이동하지 않음)
    } finally {
      // 이동이 실패했더라도 idle fetch를 막아두면 영원히 갱신이 멈출 수 있으므로 반드시 해제
      regionMoveInProgressRef.current = false;
    }
  }, []);

  const fetchMarkersByIdlePayload = useCallback(async (payload) => {
    const paramsWithTradeType = currentSearchParamsRef.current;
    if (!paramsWithTradeType) return;
    if (paramsWithTradeType.searchConditionType !== 'region') return;
    if (regionMoveInProgressRef.current) return;

    const tradeType = paramsWithTradeType.tradeType || '매매';
    const bounds = payload?.bounds;
    const level = payload?.level;
    if (!bounds) return;

    // 이전 요청 취소 + 최신 응답만 반영
    markerAbortRef.current?.abort?.();
    const controller = new AbortController();
    markerAbortRef.current = controller;
    const seq = ++markerReqSeqRef.current;

    const requestCommon = {
      // 요구사항: 패닝/줌 시에는 지역(sido/gugun/dong) 필터를 제거하고 bounds 기준으로만 갱신
      level,
      south: bounds.south,
      west: bounds.west,
      north: bounds.north,
      east: bounds.east,
    };

    try {
      let response = [];
      if (tradeType === '매매') {
        const parseArea = (val) => {
          if (val == null || val === '') return undefined;
          const num = parseFloat(val);
          return Number.isFinite(num) ? num : undefined;
        };
        const markerRequest = {
          ...requestCommon,
          minAmount: paramsWithTradeType?.priceMin ? Number(paramsWithTradeType.priceMin) * 10000 : undefined,
          maxAmount: paramsWithTradeType?.priceMax ? Number(paramsWithTradeType.priceMax) * 10000 : undefined,
          minArea: parseArea(paramsWithTradeType?.minArea),
          maxArea: parseArea(paramsWithTradeType?.maxArea),
          periodMonths: paramsWithTradeType?.period || 6,
        };
        response = await getSaleMarkers(markerRequest, { signal: controller.signal });
      } else {
        const parseExclusive = (val) => {
          if (!val || val === '') return undefined;
          const num = parseFloat(val);
          return isNaN(num) ? undefined : num;
        };

        const markerRequest = {
          ...requestCommon,
          minDeposit: paramsWithTradeType?.depositMin ? Number(paramsWithTradeType.depositMin) * 10000 : undefined,
          maxDeposit: paramsWithTradeType?.depositMax ? Number(paramsWithTradeType.depositMax) * 10000 : undefined,
          minMonthlyRent: paramsWithTradeType?.monthlyRentMin ? Number(paramsWithTradeType.monthlyRentMin) * 10000 : undefined,
          maxMonthlyRent: paramsWithTradeType?.monthlyRentMax ? Number(paramsWithTradeType.monthlyRentMax) * 10000 : undefined,
          minExclusive: parseExclusive(paramsWithTradeType?.minExclusive),
          maxExclusive: parseExclusive(paramsWithTradeType?.maxExclusive),
        };
        response = await getRentMarkers(markerRequest, { signal: controller.signal });
      }

      if (seq !== markerReqSeqRef.current) return;

      const markers = (response || []).map((m) => ({
        lat: m.latitude,
        lng: m.longitude,
        title: m.aptNm,
        info: '',
        apartmentId: m.aptId,
        apartmentData: m,
      }));

      setApartments(markers);
    } catch (error) {
      // Abort는 정상 흐름
      if (error?.name === 'AbortError') return;
      if (String(error?.message || '').toLowerCase().includes('aborted')) return;
      console.error('마커 갱신 실패:', error);
    }
  }, []);

  // 매매(region) 필터 변경 시: 현재 bounds 기준으로 마커/리스트를 즉시 갱신한다.
  const handleAutoApply = useCallback((nextParams) => {
    if (!nextParams) return;
    const tradeType = nextParams.tradeType || '매매';
    const paramsWithTradeType = { ...nextParams, tradeType };

    setCurrentSearchParams(paramsWithTradeType);
    currentSearchParamsRef.current = paramsWithTradeType;

    try {
      sessionStorage.setItem('search_map_lastParams', JSON.stringify(nextParams));
      sessionStorage.setItem('search_tradeType', tradeType);
    } catch (e) {
      // ignore
    }

    // map bounds가 준비된 상태라면 즉시 재조회
    const last = lastIdlePayloadRef.current;
    if (last?.bounds) {
      fetchMarkersByIdlePayload(last);
    }
  }, [fetchMarkersByIdlePayload]);

  const handleMapReady = useCallback((kakaoMap) => {
    mapObjRef.current = kakaoMap;
    if (pendingRegionMoveRef.current) {
      moveMapToRegionByOneMarker(pendingRegionMoveRef.current);
      pendingRegionMoveRef.current = null;
    }
  }, [moveMapToRegionByOneMarker]);

  const handleMapIdle = useCallback((payload) => {
    lastIdlePayloadRef.current = payload;
    if (idleTimerRef.current) clearTimeout(idleTimerRef.current);
    idleTimerRef.current = setTimeout(() => {
      fetchMarkersByIdlePayload(lastIdlePayloadRef.current);
    }, 1000);
  }, [fetchMarkersByIdlePayload]);

  // 검색 실행
  const handleSearch = async (searchParams) => {
    setLoading(true);
    setSelectedApartment(null);
    setShowSidePanner(false);
    setSchoolMarkers([]);
    setShowSchoolMarkers(false);

    try {
      const tradeType = searchParams.tradeType || '매매';
      
      // 현재 검색 파라미터 저장 (마커 클릭 시 사용)
      // tradeType을 명시적으로 포함하여 전달
      const paramsWithTradeType = {
        ...searchParams,
        tradeType: tradeType, // 명시적으로 tradeType 포함
      };
      setCurrentSearchParams(paramsWithTradeType);
      // 검색조건 보존(상세페이지 갔다가 돌아와도 유지)
      try {
        sessionStorage.setItem('search_map_lastParams', JSON.stringify(searchParams));
        sessionStorage.setItem('search_tradeType', tradeType);
        // 새 검색이면 "마지막 선택 아파트"는 일단 해제(검색결과가 바뀌므로)
        sessionStorage.removeItem('search_map_selectedApartment');
      } catch (e) {
        // ignore
      }

      // 마커 조회
      let markers = [];

      if (searchParams.searchConditionType === 'region') {
        // region 검색은 "bounds 기반 갱신"이 기본.
        // 1) 마커/리스트를 비우고 2) 조건에 맞는 아파트 1개로 지도 중심을 옮긴 뒤 3) idle에서 bounds+필터로 마커를 가져온다.
        setApartments([]);
        await moveMapToRegionByOneMarker(paramsWithTradeType);
        // 실제 마커 조회는 handleMapIdle → fetchMarkersByIdlePayload에서 수행
        markers = [];
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
      } else if (searchParams.searchConditionType === 'school') {
        // 학교 검색: 선택한 학교 + 반경 내 아파트 마커 표시
        const distanceKm = Number(searchParams.schoolRadius || 1.0);
        const schoolResponse = await get(
          `/schools/${searchParams.schoolId}/apartments?distanceKm=${distanceKm}`
        );

        markers = (schoolResponse || []).map((apt) => ({
          lat: apt.latitude,
          lng: apt.longitude,
          title: apt.aptName,
          info: apt.distanceKm != null ? `학교까지 ${Number(apt.distanceKm).toFixed(2)}km` : '',
          apartmentId: apt.apartmentId,
          apartmentData: apt,
        }));
      }

      // region은 idle에서 setApartments 하므로, subway/school만 여기서 반영
      if (searchParams.searchConditionType !== 'region') {
        setApartments(markers);
        if (markers.length > 0) {
          setMapCenter({ lat: markers[0].lat, lng: markers[0].lng });
        } else {
          setMapCenter({ lat: 37.5665, lng: 126.9780 });
        }
      }
    } catch (error) {
      console.error('검색 실패:', error);
      alert('검색에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  // "상세에서 복귀" 시 마지막 선택 아파트(사이드패널)를 다시 열어준다.
  useEffect(() => {
    const pending = pendingSelectedApartmentRestoreRef.current;
    if (!pending) return;
    if (loading) return;
    if (!apartments || apartments.length === 0) return;

    const targetId = pending?.id;
    if (!targetId) {
      pendingSelectedApartmentRestoreRef.current = null;
      return;
    }

    const idx = apartments.findIndex((m) => String(m?.apartmentId) === String(targetId));
    if (idx < 0) {
      // 검색 결과에 없으면 복원하지 않는다.
      pendingSelectedApartmentRestoreRef.current = null;
      return;
    }

    const markerData = apartments[idx];
    const apt = markerData?.apartmentData || markerData || {};
    const restored = {
      id: targetId,
      name: pending?.name || markerData?.title || apt.aptNm || apt.aptName || apt.name || '아파트',
      address: pending?.address || apt.roadAddress || apt.address || apt.jibunAddress || '',
      sido: pending?.sido || currentSearchParams?.sido || apt.sido || '',
      gugun: pending?.gugun || currentSearchParams?.gugun || apt.gugun || '',
      dong: pending?.dong || currentSearchParams?.dong || apt.dong || '',
      latitude: pending?.latitude || apt.latitude || apt.lat,
      longitude: pending?.longitude || apt.longitude || apt.lng,
    };

    setSelectedApartment(restored);
    setShowSidePanner(true);
    if (restored.latitude != null && restored.longitude != null) {
      setMapCenter({ lat: restored.latitude, lng: restored.longitude });
    }

    // 복원 완료
    pendingSelectedApartmentRestoreRef.current = null;
  }, [apartments, loading, currentSearchParams]);

  // 마커 클릭 핸들러
  const handleMarkerClick = async (markerData, index) => {
    const apartment = markerData.apartmentData || markerData;
    const apartmentId = apartment.aptId || apartment.apartmentId || apartment.id;
    
    // 아파트 정보 설정 (사이드 패널에 필요한 모든 정보 포함)
    const nextSelected = {
      id: apartmentId,
      name: apartment.aptNm || apartment.aptName || apartment.aptName || apartment.name || '아파트',
      address: apartment.roadAddress || apartment.address || apartment.jibunAddress || '',
      sido: currentSearchParams?.sido || apartment.sido || '',
      gugun: currentSearchParams?.gugun || apartment.gugun || '',
      dong: currentSearchParams?.dong || apartment.dong || '',
      latitude: apartment.latitude || apartment.lat,
      longitude: apartment.longitude || apartment.lng,
    };
    setSelectedApartment(nextSelected);
    setShowSidePanner(true);

    // 마지막 선택 아파트 저장(상세페이지 복귀 시 복원용)
    try {
      sessionStorage.setItem('search_map_selectedApartment', JSON.stringify(nextSelected));
    } catch (e) {
      // ignore
    }
  };

  // 지도 클릭 핸들러 (사이드 패널 닫기)
  const handleMapClick = () => {
    setShowSidePanner(false);
    setSelectedApartment(null);
    try {
      sessionStorage.removeItem('search_map_selectedApartment');
    } catch (e) {
      // ignore
    }
  };

  // 버스 마커 토글
  const handleToggleBusMarker = useCallback((stations, visible) => {
    // TODO: 버스 마커 기능 구현 필요
    // 현재는 SidePanner에서 호출되지만 Map 컴포넌트에 버스 마커 지원이 필요
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

    // 상세에서 돌아올 때 검색상태를 복원할 수 있도록 현재 URL을 저장
    try {
      const currentUrl = `${window.location.pathname}${window.location.search || ''}`;
      sessionStorage.setItem('search_map_lastUrl', currentUrl);
      // 상세에서 복귀 시 마지막 선택 아파트까지 복원하도록 플래그 설정
      sessionStorage.setItem('search_map_restoreSelected', '1');
    } catch (e) {
      // ignore
    }

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
      <div className="flex-shrink-0 bg-gray-50 border-b">
        <Filter onSearch={handleSearch} onAutoApply={handleAutoApply} initialParams={initialFilterParams} />
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
                tradeType={currentSearchParams?.tradeType || '매매'}
                onShowDetail={handleShowDetail}
                onBackToList={() => {
                  setShowSidePanner(false);
                  setSelectedApartment(null);
                }}
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
            level={mapLevel}
            markers={apartments}
            selectedMarkerId={selectedApartment?.id ?? null}
            onMarkerClick={handleMarkerClick}
            onMapClick={handleMapClick}
            schoolMarkers={schoolMarkers}
            showSchoolMarkers={showSchoolMarkers}
            onMapReady={handleMapReady}
            onIdle={handleMapIdle}
            useCluster={true}
            autoFitBounds={false}
          />
        </div>
      </div>
    </div>
  );
}

export default function MapSearchPage() {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
          <div className="text-center text-gray-500">로딩 중...</div>
        </div>
      }
    >
      <MapSearchPageContent />
    </Suspense>
  );
}
