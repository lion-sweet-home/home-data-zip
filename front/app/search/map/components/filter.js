'use client';

import { useState, useEffect, useRef } from 'react';
import { getSidoList, getGugunList, getDongList } from '../../../api/region';
import { searchSubwayStations } from '../../../api/subway';
import { searchSchoolsByName, searchSchoolsByRegion } from '../../../api/school';
import PriceFilter from './price_filter';
import AreaFilter from './area_filter';

export default function Filter({ onSearch, onAutoApply, initialParams }) {
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

  // 학교 검색 (학교명)
  const [schoolSearchKeyword, setSchoolSearchKeyword] = useState('');
  const [schoolSearchResults, setSchoolSearchResults] = useState([]);
  const [selectedSchool, setSelectedSchool] = useState(null);
  const [schoolSearchRadius, setSchoolSearchRadius] = useState(1.0);
  const [schoolSearchRadiusActive, setSchoolSearchRadiusActive] = useState(false);

  // 기간 선택 (최근 N개월)
  const [period, setPeriod] = useState(6);

  // 가격 범위
  const [priceMin, setPriceMin] = useState('');
  const [priceMax, setPriceMax] = useState('');
  // 매매용 면적 범위 (m²)
  const [minArea, setMinArea] = useState('');
  const [maxArea, setMaxArea] = useState('');
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
  const autoApplyTimerRef = useRef(null);

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
    if (initialParams.minArea != null) setMinArea(String(initialParams.minArea));
    if (initialParams.maxArea != null) setMaxArea(String(initialParams.maxArea));
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

  // 지하철역 검색
  const handleSubwaySearch = async () => {
    if (!subwaySearchKeyword.trim()) {
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
    } catch (error) {
      console.error('지하철역 검색 실패:', error);
      alert('지하철역 검색에 실패했습니다.');
    }
  };

  // 지하철역 선택
  const handleSelectSubwayStation = (station) => {
    setSelectedSubwayStation(station);
    // 역을 선택하면 기본 반경(1km)으로 바로 조회 가능하게 활성화
    setSubwayRadius(1.0);
    setSubwayRadiusActive(true);
  };

  // 학교명 검색
  const handleSchoolSearch = async () => {
    if (!schoolSearchKeyword.trim()) {
      setSchoolSearchResults([]);
      return;
    }
    try {
      const results = await searchSchoolsByName(schoolSearchKeyword.trim(), 20);
      setSchoolSearchResults(results || []);
    } catch (error) {
      console.error('학교 검색 실패:', error);
      alert('학교 검색에 실패했습니다.');
      setSchoolSearchResults([]);
    }
  };

  // 학교 선택
  const handleSelectSchool = (school) => {
    setSelectedSchool(school);
    setSchoolSearchRadiusActive(true);
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
              ? { priceMin, priceMax, minArea, maxArea }
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

  // 요구사항 변경: 구/군까지만 선택해도 매매 필터(가격/면적)가 보여야 함
  const isRegionSelected = Boolean(selectedSido && selectedGugun);

  // 매매(region)에서만: 필터 변경 시 자동 적용(디바운스 300ms)
  useEffect(() => {
    if (!onAutoApply) return;
    if (tradeType !== '매매') return;
    if (searchConditionType !== 'region') return;
    if (!isRegionSelected) return;

    if (autoApplyTimerRef.current) clearTimeout(autoApplyTimerRef.current);
    autoApplyTimerRef.current = setTimeout(() => {
      const autoParams = {
        tradeType,
        searchConditionType,
        period,
        sido: selectedSido,
        gugun: selectedGugun,
        dong: selectedDong || undefined,
        priceMin,
        priceMax,
        minArea,
        maxArea,
        schoolTypes: Object.entries(schoolTypes)
          .filter(([_, selected]) => selected)
          .map(([type]) => type),
      };
      onAutoApply(autoParams);
    }, 300);

    return () => {
      if (autoApplyTimerRef.current) clearTimeout(autoApplyTimerRef.current);
    };
  }, [
    onAutoApply,
    tradeType,
    searchConditionType,
    period,
    selectedSido,
    selectedGugun,
    selectedDong,
    priceMin,
    priceMax,
    minArea,
    maxArea,
    schoolTypes,
    isRegionSelected,
  ]);

  return (
    <div className="bg-white border-b border-gray-200 py-1 px-3 h-16 flex items-center">
      <div className="flex items-center gap-2 w-full overflow-x-auto">
        {/* 매매/전월세 선택 */}
        <div className="flex gap-1 flex-shrink-0">
          <button
            type="button"
            onClick={() => {
              setTradeType('매매');
              // 전/월세 관련 필드 초기화
              setDepositMin('');
              setDepositMax('');
              setMonthlyRentMin('');
              setMonthlyRentMax('');
              setMinExclusive('');
              setMaxExclusive('');
            }}
            className={`px-2 py-1 rounded text-xs font-medium transition-colors ${
              tradeType === '매매'
                ? 'bg-blue-600 text-white'
                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
            }`}
          >
            매매
          </button>
          <button
            type="button"
            onClick={() => {
              setTradeType('전월세');
              // 매매 관련 필드 초기화
              setPriceMin('');
              setPriceMax('');
              setMinArea('');
              setMaxArea('');
            }}
            className={`px-2 py-1 rounded text-xs font-medium transition-colors ${
              tradeType === '전월세'
                ? 'bg-blue-600 text-white'
                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
            }`}
          >
            전/월세
          </button>
        </div>

        {/* 검색 조건 타입 선택 */}
        <div className="flex gap-1 flex-shrink-0">
          <button
            type="button"
            onClick={() => {
              setSearchConditionType('region');
              // NOTE: 지역↔지하철 전환 시 선택값이 리셋되지 않도록
              // 다른 모드의 상태는 유지한다.
            }}
            className={`px-2 py-1 rounded text-xs font-medium transition-colors ${
              searchConditionType === 'region'
                ? 'bg-blue-600 text-white'
                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
            }`}
          >
            지역
          </button>
          <button
            type="button"
            onClick={() => {
              setSearchConditionType('subway');
              // NOTE: 지역↔지하철 전환 시 선택값이 리셋되지 않도록
              // 다른 모드의 상태는 유지한다.
            }}
            className={`px-2 py-1 rounded text-xs font-medium transition-colors ${
              searchConditionType === 'subway'
                ? 'bg-blue-600 text-white'
                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
            }`}
          >
            지하철
          </button>
          <button
            type="button"
            onClick={() => {
              setSearchConditionType('school');
              // NOTE: 모드 전환 시 다른 모드 상태는 유지한다.
            }}
            className={`px-2 py-1 rounded text-xs font-medium transition-colors ${
              searchConditionType === 'school'
                ? 'bg-blue-600 text-white'
                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
            }`}
          >
            학교
          </button>
        </div>

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
              className="flex-1 px-2 py-1 border border-gray-300 rounded text-xs focus:ring-1 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900 placeholder:text-gray-600 min-w-0"
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
                className="flex-1 px-2 py-1 border border-gray-300 rounded text-xs focus:ring-1 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900 placeholder:text-gray-600 min-w-0"
              />
              <button
                type="button"
                onClick={handleSchoolSearch}
                className="px-3 py-1 bg-gray-100 text-gray-700 rounded text-xs hover:bg-gray-200 transition-colors flex-shrink-0"
              >
                검색
              </button>

              {schoolSearchResults.length > 0 && (
                <select
                  value={selectedSchool?.id ?? ''}
                  onChange={(e) => {
                    const id = Number(e.target.value);
                    const found = schoolSearchResults.find((s) => Number(s?.id) === id);
                    if (found) handleSelectSchool(found);
                  }}
                  className="px-2 py-1 border border-gray-300 rounded text-xs focus:ring-1 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900 flex-shrink-0 max-w-[220px]"
                >
                  <option value="">학교 선택</option>
                  {schoolSearchResults.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.name}
                    </option>
                  ))}
                </select>
              )}

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
              // 요구사항 변경: 구/군까지만 선택하면 가격/면적 필터 표시
              isRegionSelected ? (
                <>
                  <PriceFilter
                    valueMin={priceMin}
                    valueMax={priceMax}
                    onChangeMin={setPriceMin}
                    onChangeMax={setPriceMax}
                    unitLabel="만원"
                  />
                  <AreaFilter
                    valueMin={minArea}
                    valueMax={maxArea}
                    onChangeMin={setMinArea}
                    onChangeMax={setMaxArea}
                    unitLabel="m²"
                    minPlaceholder="면적 최소"
                    maxPlaceholder="면적 최대"
                  />
                </>
              ) : null
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
  );
}
