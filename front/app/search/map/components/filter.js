'use client';

import { useState, useEffect, useRef } from 'react';
import { getSidoList, getGugunList, getDongList } from '../../../api/region';
import { searchSubwayStations } from '../../../api/subway';
import { searchSchoolsByName, searchSchoolsByRegion } from '../../../api/school';

export default function Filter({ onSearch, initialParams }) {
  // 매매/전월세 선택
  const [tradeType, setTradeType] = useState('매매');

  // 검색 조건 타입 선택
  const [searchConditionType, setSearchConditionType] = useState('region'); // 'region' | 'subway' | 'school'

  // 지역 선택
  const [sidoList, setSidoList] = useState([]);
  const [gugunList, setGugunList] = useState([]);
  const [dongList, setDongList] = useState([]);
  const [selectedSido, setSelectedSido] = useState('');
  const [selectedGugun, setSelectedGugun] = useState('');
  const [selectedDong, setSelectedDong] = useState('');

  // 지하철역 검색
  const [subwaySearchKeyword, setSubwaySearchKeyword] = useState('');
  const [subwaySearchType, setSubwaySearchType] = useState('stationName');
  const [subwayResults, setSubwayResults] = useState([]);
  const [selectedSubwayStation, setSelectedSubwayStation] = useState(null);
  const [subwayRadius, setSubwayRadius] = useState(1.0);
  const [subwayRadiusActive, setSubwayRadiusActive] = useState(false);
  const [subwayModalOpen, setSubwayModalOpen] = useState(false);

  // 학교 검색 (학교명)
  const [schoolSearchKeyword, setSchoolSearchKeyword] = useState('');
  const [schoolSearchResults, setSchoolSearchResults] = useState([]);
  const [selectedSchool, setSelectedSchool] = useState(null);
  const [schoolSearchRadius, setSchoolSearchRadius] = useState(1.0);
  const [schoolSearchRadiusActive, setSchoolSearchRadiusActive] = useState(false);
  const [schoolModalOpen, setSchoolModalOpen] = useState(false);

  // 기간 선택 (최근 N개월)
  const [period, setPeriod] = useState(6);

  // 가격 범위
  const [priceMin, setPriceMin] = useState('');
  const [priceMax, setPriceMax] = useState('');
  // 전/월세용 보증금 범위
  const [depositMin, setDepositMin] = useState('');
  const [depositMax, setDepositMax] = useState('');
  // 전/월세용 월세 범위
  const [monthlyRentMin, setMonthlyRentMin] = useState('');
  const [monthlyRentMax, setMonthlyRentMax] = useState('');
  // 전/월세용 면적 범위 (m²)
  const [minExclusive, setMinExclusive] = useState('');
  const [maxExclusive, setMaxExclusive] = useState('');

  // 학교 필터
  const [schoolTypes, setSchoolTypes] = useState({
    초등학교: false,
    중학교: false,
    고등학교: false,
  });
  const [schoolList, setSchoolList] = useState([]);

  // URL/부모에서 넘어온 초기값을 안전하게 주입하기 위한 ref
  const pendingRegionInitRef = useRef(null); // {sido, gugun, dong}

  // 시도 목록 로드
  useEffect(() => {
    const loadSidoList = async () => {
      try {
        const list = await getSidoList();
        setSidoList(list);
      } catch (error) {
        console.error('시도 목록 로드 실패:', error);
      }
    };
    loadSidoList();
  }, []);

  // 초기값(쿼리스트링 등) 주입
  useEffect(() => {
    if (!initialParams) return;

    // tradeType 복원
    if (initialParams.tradeType === '매매' || initialParams.tradeType === '전월세') {
      setTradeType(initialParams.tradeType);
    }

    // searchConditionType 복원
    if (
      initialParams.searchConditionType === 'region' ||
      initialParams.searchConditionType === 'subway' ||
      initialParams.searchConditionType === 'school'
    ) {
      setSearchConditionType(initialParams.searchConditionType);
    } else if (initialParams.schoolId) {
      setSearchConditionType('school');
    } else if (initialParams.subwayStationId) {
      setSearchConditionType('subway');
    } else {
      setSearchConditionType('region');
    }

    // 기간
    if (initialParams.period != null) {
      const n = Number(initialParams.period);
      if (!Number.isNaN(n)) setPeriod(n);
    }

    // 가격 범위
    if (initialParams.priceMin != null) setPriceMin(String(initialParams.priceMin));
    if (initialParams.priceMax != null) setPriceMax(String(initialParams.priceMax));
    if (initialParams.depositMin != null) setDepositMin(String(initialParams.depositMin));
    if (initialParams.depositMax != null) setDepositMax(String(initialParams.depositMax));
    if (initialParams.monthlyRentMin != null) setMonthlyRentMin(String(initialParams.monthlyRentMin));
    if (initialParams.monthlyRentMax != null) setMonthlyRentMax(String(initialParams.monthlyRentMax));
    if (initialParams.minExclusive != null) setMinExclusive(String(initialParams.minExclusive));
    if (initialParams.maxExclusive != null) setMaxExclusive(String(initialParams.maxExclusive));

    // 학교 필터
    if (initialParams.schoolTypes) {
      const types = Array.isArray(initialParams.schoolTypes)
        ? initialParams.schoolTypes
        : String(initialParams.schoolTypes).split(',').map(s => s.trim()).filter(Boolean);

      setSchoolTypes({
        초등학교: types.includes('초등학교'),
        중학교: types.includes('중학교'),
        고등학교: types.includes('고등학교'),
      });
    }
    // schoolRadius는 '학교 검색' 모드에서만 사용 (지역의 학교필터에는 반경 없음)
    if (initialParams.schoolId) {
      setSelectedSchool({
        id: Number(initialParams.schoolId),
        name: initialParams.schoolName || '',
      });
      const r = initialParams.schoolRadius != null ? Number(initialParams.schoolRadius) : NaN;
      if (!Number.isNaN(r)) {
        setSchoolSearchRadius(r);
        setSchoolSearchRadiusActive(true);
      }
    }

    // 지하철역 초기값
    if (initialParams.subwayStationId) {
      setSelectedSubwayStation({
        stationId: Number(initialParams.subwayStationId),
        stationName: initialParams.subwayStationName || '',
        lineNames: initialParams.subwayLineNames || undefined,
      });
      if (initialParams.subwayRadius != null) {
        const r = Number(initialParams.subwayRadius);
        if (!Number.isNaN(r)) {
          setSubwayRadius(r);
          setSubwayRadiusActive(true);
        }
      }
    }

    // 지역 초기값: 시/도만 먼저 넣고, 구/군/동은 리스트 로드 후 자동 세팅
    const initSido = initialParams.sido || '';
    const initGugun = initialParams.gugun || '';
    const initDong = initialParams.dong || '';

    if (initSido) {
      pendingRegionInitRef.current = { sido: initSido, gugun: initGugun, dong: initDong };
      setSelectedSido(initSido);
      // selectedGugun/selectedDong은 effect에서 목록 로드 후 세팅
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [initialParams]);

  // 구/군 목록 로드
  useEffect(() => {
    if (!selectedSido) {
      setGugunList([]);
      setSelectedGugun('');
      setDongList([]);
      setSelectedDong('');
      return;
    }

    const loadGugunList = async () => {
      try {
        const list = await getGugunList(selectedSido);
        setGugunList(list);
        const pending = pendingRegionInitRef.current;
        if (pending && pending.sido === selectedSido && pending.gugun) {
          setSelectedGugun(pending.gugun);
        } else {
          setSelectedGugun('');
          setDongList([]);
          setSelectedDong('');
        }
      } catch (error) {
        console.error('구/군 목록 로드 실패:', error);
      }
    };
    loadGugunList();
  }, [selectedSido]);

  // 동 목록 로드
  useEffect(() => {
    if (!selectedSido || !selectedGugun) {
      setDongList([]);
      setSelectedDong('');
      return;
    }

    const loadDongList = async () => {
      try {
        const list = await getDongList(selectedSido, selectedGugun);
        setDongList(list);
        const pending = pendingRegionInitRef.current;
        if (pending && pending.sido === selectedSido && pending.gugun === selectedGugun && pending.dong) {
          setSelectedDong(pending.dong);
        } else {
          setSelectedDong('');
        }
        // 동까지 세팅했으면 초기 주입 완료로 간주하고 ref 제거
        if (pending && pending.sido === selectedSido && pending.gugun === selectedGugun) {
          pendingRegionInitRef.current = null;
        }
      } catch (error) {
        console.error('동 목록 로드 실패:', error);
      }
    };
    loadDongList();
  }, [selectedSido, selectedGugun]);

  // 학교 목록 로드
  useEffect(() => {
    if (!selectedSido || !selectedGugun) {
      setSchoolList([]);
      return;
    }

    const loadSchoolList = async () => {
      try {
        const selectedTypes = Object.entries(schoolTypes)
          .filter(([_, selected]) => selected)
          .map(([type]) => type);

        if (selectedTypes.length === 0) {
          setSchoolList([]);
          return;
        }

        const params = {
          sido: selectedSido,
          gugun: selectedGugun,
          dong: selectedDong || undefined,
          schoolLevel: selectedTypes.length > 0 ? selectedTypes : undefined,
        };

        const schools = await searchSchoolsByRegion(params);
        setSchoolList(schools || []);
      } catch (error) {
        console.error('학교 목록 로드 실패:', error);
      }
    };

    loadSchoolList();
  }, [selectedSido, selectedGugun, selectedDong, schoolTypes]);

  // 지하철역 검색 (결과는 모달로 표시)
  const handleSubwaySearch = async () => {
    if (!subwaySearchKeyword.trim()) {
      setSubwayResults([]);
      setSubwayModalOpen(false);
      alert('역명 또는 호선을 입력해주세요.');
      return;
    }

    try {
      const params = {};
      if (subwaySearchType === 'stationName') {
        params.stationName = subwaySearchKeyword;
      } else {
        params.lineName = subwaySearchKeyword;
      }

      const results = await searchSubwayStations(params);
      setSubwayResults(results || []);
      setSubwayModalOpen(true);
    } catch (error) {
      console.error('지하철역 검색 실패:', error);
      alert('지하철역 검색에 실패했습니다.');
      setSubwayResults([]);
      setSubwayModalOpen(false);
    }
  };

  // 지하철역 선택 (모달에서 선택 시)
  const handleSelectSubwayStation = (station) => {
    setSelectedSubwayStation(station);
    setSubwayRadius(1.0);
    setSubwayRadiusActive(true);
    setSubwayModalOpen(false);
  };

  // 학교명 검색 (결과는 모달로 표시)
  const handleSchoolSearch = async () => {
    if (!schoolSearchKeyword.trim()) {
      setSchoolSearchResults([]);
      setSchoolModalOpen(false);
      return;
    }
    try {
      const results = await searchSchoolsByName(schoolSearchKeyword.trim(), 20);
      setSchoolSearchResults(results || []);
      setSchoolModalOpen(true);
    } catch (error) {
      console.error('학교 검색 실패:', error);
      alert('학교 검색에 실패했습니다.');
      setSchoolSearchResults([]);
      setSchoolModalOpen(false);
    }
  };

  // 학교 선택 (모달에서 선택 시)
  const handleSelectSchool = (school) => {
    setSelectedSchool(school);
    setSchoolSearchRadiusActive(true);
    setSchoolModalOpen(false);
  };

  // 학교 타입 체크박스 변경
  const handleSchoolTypeChange = (type) => {
    setSchoolTypes((prev) => ({
      ...prev,
      [type]: !prev[type],
    }));
  };

  // 검색 실행
  const handleSearch = () => {
    if (searchConditionType === 'region') {
      if (!selectedSido || !selectedGugun) {
        alert('시/도와 구/군을 선택해주세요.');
        return;
      }
    } else if (searchConditionType === 'subway') {
      if (!selectedSubwayStation) {
        alert('지하철역을 선택해주세요.');
        return;
      }
      if (!subwayRadiusActive) {
        alert('반경을 선택해주세요.');
        return;
      }
    } else if (searchConditionType === 'school') {
      if (!selectedSchool?.id) {
        alert('학교를 선택해주세요.');
        return;
      }
      if (!schoolSearchRadiusActive) {
        alert('반경을 선택해주세요.');
        return;
      }
    }

    // 면적 범위 검증 및 스왑 (검색 실행 시에만)
    let finalMinExclusive = minExclusive;
    let finalMaxExclusive = maxExclusive;
    if (finalMinExclusive && finalMaxExclusive) {
      const minVal = parseFloat(finalMinExclusive);
      const maxVal = parseFloat(finalMaxExclusive);
      if (!isNaN(minVal) && !isNaN(maxVal) && minVal > maxVal) {
        // min > max면 스왑
        finalMinExclusive = maxExclusive;
        finalMaxExclusive = minExclusive;
        // state도 업데이트 (UI에 반영)
        setMinExclusive(finalMinExclusive);
        setMaxExclusive(finalMaxExclusive);
      }
    }

    const searchParams = {
      tradeType,
      searchConditionType,
      period,
      ...(searchConditionType === 'region'
        ? {
            sido: selectedSido,
            gugun: selectedGugun,
            dong: selectedDong || undefined,
            ...(tradeType === '매매'
              ? { priceMin, priceMax }
              : { depositMin, depositMax, monthlyRentMin, monthlyRentMax, minExclusive: finalMinExclusive, maxExclusive: finalMaxExclusive }),
            schoolTypes: Object.entries(schoolTypes)
              .filter(([_, selected]) => selected)
              .map(([type]) => type),
          }
        : searchConditionType === 'subway'
        ? {
            subwayStationId: selectedSubwayStation?.stationId,
            subwayStationName: selectedSubwayStation?.stationName,
            subwayRadius: subwayRadiusActive ? subwayRadius : undefined,
          }
        : {
            schoolId: selectedSchool?.id,
            schoolName: selectedSchool?.name,
            schoolRadius: schoolSearchRadiusActive ? schoolSearchRadius : undefined,
          }),
    };

    if (onSearch) {
      onSearch(searchParams);
    }
  };

  return (
    <>
    <div className="bg-white border-b border-gray-200 py-1 px-3 h-16 flex items-center">
      <div className="flex items-center gap-2 w-full overflow-x-auto">
        {/* 매매/전월세 선택 */}
        <select
          value={tradeType}
          onChange={(e) => {
            const next = e.target.value;
            setTradeType(next);
            if (next === '매매') {
              setDepositMin('');
              setDepositMax('');
              setMonthlyRentMin('');
              setMonthlyRentMax('');
              setMinExclusive('');
              setMaxExclusive('');
            } else {
              setPriceMin('');
              setPriceMax('');
            }
          }}
          className="px-2 py-1 border border-gray-300 rounded text-xs focus:ring-1 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900 min-w-[90px] flex-shrink-0"
        >
          <option value="매매">매매</option>
          <option value="전월세">전/월세</option>
        </select>

        {/* 검색 조건 타입 선택 */}
        <select
          value={searchConditionType}
          onChange={(e) => setSearchConditionType(e.target.value)}
          className="px-2 py-1 border border-gray-300 rounded text-xs focus:ring-1 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900 min-w-[90px] flex-shrink-0"
        >
          <option value="region">지역</option>
          <option value="subway">지하철</option>
          <option value="school">학교</option>
        </select>

        {/* 필터 바 */}
        <div className="flex items-center gap-1.5 flex-1 min-w-0">
          {/* 지역 검색일 때 */}
          {searchConditionType === 'region' && (
          <>
            {/* 시/도 선택 */}
            <select
              value={selectedSido}
              onChange={(e) => setSelectedSido(e.target.value)}
              className="px-2 py-1 border border-gray-300 rounded text-xs focus:ring-1 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900 min-w-[100px] flex-shrink-0"
            >
              <option value="">시/도</option>
              {sidoList.filter((sido) => sido != null).map((sido) => (
                <option key={sido} value={sido}>
                  {sido}
                </option>
              ))}
            </select>

            {/* 구/군 선택 */}
            <select
              value={selectedGugun}
              onChange={(e) => setSelectedGugun(e.target.value)}
              disabled={!selectedSido}
              className="px-2 py-1 border border-gray-300 rounded text-xs focus:ring-1 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900 disabled:bg-gray-100 disabled:cursor-not-allowed disabled:text-gray-600 min-w-[100px] flex-shrink-0"
            >
              <option value="">구/군</option>
              {gugunList.filter((gugun) => gugun != null).map((gugun) => (
                <option key={gugun} value={gugun}>
                  {gugun}
                </option>
              ))}
            </select>

            {/* 동 선택 */}
            <select
              value={selectedDong}
              onChange={(e) => setSelectedDong(e.target.value)}
              disabled={!selectedGugun}
              className="px-2 py-1 border border-gray-300 rounded text-xs focus:ring-1 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900 disabled:bg-gray-100 disabled:cursor-not-allowed disabled:text-gray-600 min-w-[100px] flex-shrink-0"
            >
              <option value="">동</option>
              {dongList.filter((dong) => dong != null).map((dong) => (
                <option key={dong} value={dong}>
                  {dong}
                </option>
              ))}
            </select>
          </>
          )}

          {/* 지하철역 검색일 때 */}
          {searchConditionType === 'subway' && (
          <div className="flex items-center gap-2 flex-1 min-w-0">
            <select
              value={subwaySearchType}
              onChange={(e) => setSubwaySearchType(e.target.value)}
              className="px-2 py-1 border border-gray-300 rounded text-xs focus:ring-1 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900 flex-shrink-0"
            >
              <option value="stationName">역명</option>
              <option value="lineName">호선</option>
            </select>
            <input
              type="text"
              value={subwaySearchKeyword}
              onChange={(e) => setSubwaySearchKeyword(e.target.value)}
              onKeyPress={(e) => e.key === 'Enter' && handleSubwaySearch()}
              placeholder={subwaySearchType === 'stationName' ? '역명 입력' : '호선 입력'}
              className="w-32 px-2 py-1 border border-gray-300 rounded text-xs focus:ring-1 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900 placeholder:text-gray-600 flex-shrink-0"
            />
            <button
              type="button"
              onClick={handleSubwaySearch}
              className="px-3 py-1 bg-gray-100 text-gray-700 rounded text-xs hover:bg-gray-200 transition-colors flex-shrink-0"
            >
              검색
            </button>
            {selectedSubwayStation && (
              <div className="flex items-center gap-2 flex-shrink-0">
                <span className="text-xs text-gray-700">{selectedSubwayStation.stationName}</span>
                <select
                  value={subwayRadius}
                  onChange={(e) => {
                    setSubwayRadius(parseFloat(e.target.value));
                    setSubwayRadiusActive(true);
                  }}
                  className="px-2 py-1 border border-gray-300 rounded text-xs focus:ring-1 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900"
                >
                  <option value={0.5}>0.5km</option>
                  <option value={1.0}>1km</option>
                  <option value={2.0}>2km</option>
                  <option value={3.0}>3km</option>
                  <option value={5.0}>5km</option>
                  <option value={10.0}>10km</option>
                </select>
              </div>
            )}
          </div>
          )}

          {/* 학교 검색일 때 */}
          {searchConditionType === 'school' && (
            <div className="flex items-center gap-2 flex-1 min-w-0">
              <input
                type="text"
                value={schoolSearchKeyword}
                onChange={(e) => setSchoolSearchKeyword(e.target.value)}
                onKeyPress={(e) => e.key === 'Enter' && handleSchoolSearch()}
                placeholder="학교명"
                className="w-32 px-2 py-1 border border-gray-300 rounded text-xs focus:ring-1 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900 placeholder:text-gray-600 flex-shrink-0"
              />
              <button
                type="button"
                onClick={handleSchoolSearch}
                className="px-3 py-1 bg-gray-100 text-gray-700 rounded text-xs hover:bg-gray-200 transition-colors flex-shrink-0"
              >
                검색
              </button>

              {selectedSchool && (
                <div className="flex items-center gap-2 flex-shrink-0">
                  <span className="text-xs text-gray-700">{selectedSchool.name}</span>
                  <select
                    value={schoolSearchRadius}
                    onChange={(e) => {
                      setSchoolSearchRadius(parseFloat(e.target.value));
                      setSchoolSearchRadiusActive(true);
                    }}
                    className="px-2 py-1 border border-gray-300 rounded text-xs focus:ring-1 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900"
                  >
                    <option value={0.5}>0.5km</option>
                    <option value={1.0}>1km</option>
                    <option value={2.0}>2km</option>
                    <option value={3.0}>3km</option>
                    <option value={5.0}>5km</option>
                    <option value={10.0}>10km</option>
                  </select>
                </div>
              )}
            </div>
          )}

          {/* 기간 선택 */}
          <select
          value={period}
          onChange={(e) => setPeriod(Number(e.target.value))}
          className="px-2 py-1 border-2 border-blue-500 rounded text-xs focus:ring-1 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900 bg-white font-medium min-w-[90px] flex-shrink-0"
        >
          <option value={6}>6개월</option>
          <option value={12}>1년</option>
          <option value={24}>2년</option>
          <option value={36}>3년</option>
          <option value={48}>4년</option>
          </select>

          {/* 가격 범위 */}
          {searchConditionType === 'region' && (
          <>
            {tradeType === '매매' ? (
              <>
                <input
                  type="number"
                  value={priceMin}
                  onChange={(e) => setPriceMin(e.target.value)}
                  placeholder="최소"
                  className="px-2 py-1 border border-gray-300 rounded text-xs focus:ring-1 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900 w-20 flex-shrink-0"
                />
                <span className="text-gray-500 text-xs">~</span>
                <input
                  type="number"
                  value={priceMax}
                  onChange={(e) => setPriceMax(e.target.value)}
                  placeholder="최대"
                  className="px-2 py-1 border border-gray-300 rounded text-xs focus:ring-1 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900 w-20 flex-shrink-0"
                />
                <span className="text-xs text-gray-500 flex-shrink-0">만원</span>
              </>
            ) : (
              <>
                <input
                  type="number"
                  value={depositMin}
                  onChange={(e) => setDepositMin(e.target.value)}
                  placeholder="보증금 최소"
                  className="px-2 py-1 border border-gray-300 rounded text-xs focus:ring-1 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900 w-20 flex-shrink-0"
                />
                <span className="text-gray-500 text-xs">~</span>
                <input
                  type="number"
                  value={depositMax}
                  onChange={(e) => setDepositMax(e.target.value)}
                  placeholder="보증금 최대"
                  className="px-2 py-1 border border-gray-300 rounded text-xs focus:ring-1 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900 w-20 flex-shrink-0"
                />
                <input
                  type="number"
                  value={monthlyRentMin}
                  onChange={(e) => setMonthlyRentMin(e.target.value)}
                  placeholder="월세 최소"
                  className="px-2 py-1 border border-gray-300 rounded text-xs focus:ring-1 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900 w-20 flex-shrink-0"
                />
                <span className="text-gray-500 text-xs">~</span>
                <input
                  type="number"
                  value={monthlyRentMax}
                  onChange={(e) => setMonthlyRentMax(e.target.value)}
                  placeholder="월세 최대"
                  className="px-2 py-1 border border-gray-300 rounded text-xs focus:ring-1 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900 w-20 flex-shrink-0"
                />
                <span className="text-xs text-gray-500 flex-shrink-0">만원</span>
              </>
            )}
            {/* 면적 범위 (전월세만) */}
            {tradeType === '전월세' && (
              <>
                <input
                  type="number"
                  step="0.1"
                  value={minExclusive}
                  onChange={(e) => {
                    // 입력 중에는 단순히 값만 업데이트 (스왑 없음)
                    setMinExclusive(e.target.value);
                  }}
                  placeholder="최소 m²"
                  className="px-2 py-1 border border-gray-300 rounded text-xs focus:ring-1 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900 w-20 flex-shrink-0"
                />
                <span className="text-gray-500 text-xs">~</span>
                <input
                  type="number"
                  step="0.1"
                  value={maxExclusive}
                  onChange={(e) => {
                    // 입력 중에는 단순히 값만 업데이트 (스왑 없음)
                    setMaxExclusive(e.target.value);
                  }}
                  placeholder="최대 m²"
                  className="px-2 py-1 border border-gray-300 rounded text-xs focus:ring-1 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900 w-20 flex-shrink-0"
                />
                <span className="text-xs text-gray-500 flex-shrink-0">m²</span>
              </>
            )}
          </>
          )}

          {/* 학교 필터 (지역 검색일 때만) */}
          {searchConditionType === 'region' && selectedSido && selectedGugun && (
          <>
            <div className="flex items-center gap-1 flex-shrink-0">
              {['초등학교', '중학교', '고등학교'].map((type) => (
                <label key={type} className="flex items-center gap-1 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={schoolTypes[type]}
                    onChange={() => handleSchoolTypeChange(type)}
                    className="w-3 h-3 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
                  />
                  <span className="text-xs text-gray-700">{type.replace('학교', '')}</span>
                </label>
              ))}
            </div>
          </>
          )}

          {/* 조회하기 버튼 */}
          <button
          onClick={handleSearch}
          disabled={
            searchConditionType === 'region'
              ? !selectedSido || !selectedGugun
              : searchConditionType === 'subway'
                ? !selectedSubwayStation
                : !selectedSchool
          }
          className={`px-4 py-1 rounded text-xs font-medium transition-colors flex-shrink-0 ${
            (searchConditionType === 'region' && selectedSido && selectedGugun) ||
            (searchConditionType === 'subway' && selectedSubwayStation) ||
            (searchConditionType === 'school' && selectedSchool)
              ? 'bg-blue-600 text-white hover:bg-blue-700'
              : 'bg-gray-300 text-gray-500 cursor-not-allowed'
          }`}
          >
            조회
          </button>
        </div>
      </div>
    </div>

    {/* 지하철역 검색 결과 모달 */}
    {subwayModalOpen && (
      <div
        className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
        onClick={() => setSubwayModalOpen(false)}
        role="dialog"
        aria-modal="true"
        aria-labelledby="subway-modal-title"
      >
        <div
          className="bg-white rounded-lg shadow-xl max-w-md w-full max-h-[70vh] flex flex-col"
          onClick={(e) => e.stopPropagation()}
        >
          <div className="flex items-center justify-between px-4 py-3 border-b border-gray-200">
            <h2 id="subway-modal-title" className="text-sm font-semibold text-gray-900">
              지하철역 선택
            </h2>
            <button
              type="button"
              onClick={() => setSubwayModalOpen(false)}
              className="p-1 rounded text-gray-500 hover:bg-gray-100 hover:text-gray-700"
              aria-label="닫기"
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
          <div className="overflow-y-auto flex-1 p-2">
            {subwayResults.length === 0 ? (
              <p className="py-6 text-center text-sm text-gray-500">검색 결과가 없습니다.</p>
            ) : (
              <ul className="divide-y divide-gray-100">
                {subwayResults.map((s) => {
                  const stationId = s.stationId ?? s.id;
                  const stationName = s.stationName ?? s.name ?? '(이름 없음)';
                  const rawLines = s.lineNames ?? s.lineName;
                  const lineNames = Array.isArray(rawLines)
                    ? rawLines.map((l) => (typeof l === 'string' ? l.trim() : String(l))).filter(Boolean).join(', ')
                    : typeof rawLines === 'string'
                      ? rawLines.split(/,\s*/).map((l) => l.trim()).filter(Boolean).join(', ')
                      : '';
                  return (
                    <li key={stationId ?? stationName}>
                      <button
                        type="button"
                        onClick={() => handleSelectSubwayStation({ stationId, stationName, lineNames: lineNames || undefined })}
                        className="w-full flex items-center justify-between gap-3 px-3 py-2.5 text-sm text-gray-900 hover:bg-blue-50 rounded-lg transition-colors text-left"
                      >
                        <span className="min-w-0 truncate">{stationName}</span>
                        {lineNames && (
                          <span className="text-gray-500 text-xs flex-shrink-0">{lineNames}</span>
                        )}
                      </button>
                    </li>
                  );
                })}
              </ul>
            )}
          </div>
        </div>
      </div>
    )}

    {/* 학교 검색 결과 모달 */}
    {schoolModalOpen && (
      <div
        className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
        onClick={() => setSchoolModalOpen(false)}
        role="dialog"
        aria-modal="true"
        aria-labelledby="school-modal-title"
      >
        <div
          className="bg-white rounded-lg shadow-xl max-w-md w-full max-h-[70vh] flex flex-col"
          onClick={(e) => e.stopPropagation()}
        >
          <div className="flex items-center justify-between px-4 py-3 border-b border-gray-200">
            <h2 id="school-modal-title" className="text-sm font-semibold text-gray-900">
              학교 선택
            </h2>
            <button
              type="button"
              onClick={() => setSchoolModalOpen(false)}
              className="p-1 rounded text-gray-500 hover:bg-gray-100 hover:text-gray-700"
              aria-label="닫기"
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
          <div className="overflow-y-auto flex-1 p-2">
            {schoolSearchResults.length === 0 ? (
              <p className="py-6 text-center text-sm text-gray-500">검색 결과가 없습니다.</p>
            ) : (
              <ul className="divide-y divide-gray-100">
                {schoolSearchResults.map((s) => (
                  <li key={s.id}>
                    <button
                      type="button"
                      onClick={() => handleSelectSchool(s)}
                      className="w-full text-left px-3 py-2.5 text-sm text-gray-900 hover:bg-blue-50 rounded-lg transition-colors"
                    >
                      {s.name ?? s.schoolName ?? '(이름 없음)'}
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      </div>
    )}
    </>
  );
}
