'use client';

import { useState, useEffect, useRef } from 'react';
import { getSidoList, getGugunList, getDongList } from '../../../api/region';
import { searchSubwayStations } from '../../../api/subway';
import { searchSchoolsByRegion } from '../../../api/school';

export default function Filter({ onSearch, initialParams }) {
  // 매매/전월세 선택
  const [tradeType, setTradeType] = useState('매매');

  // 검색 조건 타입 선택
  const [searchConditionType, setSearchConditionType] = useState('region'); // 'region' 또는 'subway'

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

  // 학교 필터
  const [schoolTypes, setSchoolTypes] = useState({
    초등학교: false,
    중학교: false,
    고등학교: false,
  });
  const [schoolRadius, setSchoolRadius] = useState(1.0);
  const [schoolRadiusActive, setSchoolRadiusActive] = useState(false);
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
    if (initialParams.searchConditionType === 'region' || initialParams.searchConditionType === 'subway') {
      setSearchConditionType(initialParams.searchConditionType);
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
    if (initialParams.schoolRadius != null) {
      const r = Number(initialParams.schoolRadius);
      if (!Number.isNaN(r)) {
        setSchoolRadius(r);
        setSchoolRadiusActive(true);
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
              : { depositMin, depositMax, monthlyRentMin, monthlyRentMax }),
            schoolTypes: Object.entries(schoolTypes)
              .filter(([_, selected]) => selected)
              .map(([type]) => type),
            schoolRadius: schoolRadiusActive ? schoolRadius : undefined,
          }
        : {
            subwayStationId: selectedSubwayStation?.stationId,
            subwayStationName: selectedSubwayStation?.stationName,
            subwayRadius: subwayRadiusActive ? subwayRadius : undefined,
          }),
    };

    if (onSearch) {
      onSearch(searchParams);
    }
  };

  return (
    <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-4">
      {/* 매매/전월세 선택 */}
      <div className="mb-4">
        <div className="flex gap-2">
          <button
            type="button"
            onClick={() => {
              setTradeType('매매');
              // 전/월세 관련 필드 초기화
              setDepositMin('');
              setDepositMax('');
              setMonthlyRentMin('');
              setMonthlyRentMax('');
            }}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
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
            }}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
              tradeType === '전월세'
                ? 'bg-blue-600 text-white'
                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
            }`}
          >
            전/월세
          </button>
        </div>
      </div>

      {/* 검색 조건 타입 선택 */}
      <div className="mb-4">
        <div className="flex gap-2">
          <button
            type="button"
            onClick={() => {
              setSearchConditionType('region');
              setSubwaySearchKeyword('');
              setSubwayResults([]);
              setSelectedSubwayStation(null);
              setSubwayRadiusActive(false);
            }}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
              searchConditionType === 'region'
                ? 'bg-blue-600 text-white'
                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
            }`}
          >
            지역 검색
          </button>
          <button
            type="button"
            onClick={() => {
              setSearchConditionType('subway');
              setSelectedSido('');
              setSelectedGugun('');
              setSelectedDong('');
              setSchoolList([]);
            }}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
              searchConditionType === 'subway'
                ? 'bg-blue-600 text-white'
                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
            }`}
          >
            지하철역 검색
          </button>
        </div>
      </div>

      {/* 필터 바 */}
      <div className="flex flex-wrap items-center gap-3">
        {/* 지역 검색일 때 */}
        {searchConditionType === 'region' && (
          <>
            {/* 시/도 선택 */}
            <div className="flex flex-col">
              <label className="text-xs text-gray-600 mb-1">시/도</label>
              <select
                value={selectedSido}
                onChange={(e) => setSelectedSido(e.target.value)}
                className="px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900 min-w-[120px]"
              >
                <option value="">시/도 선택</option>
                {sidoList.filter((sido) => sido != null).map((sido) => (
                  <option key={sido} value={sido}>
                    {sido}
                  </option>
                ))}
              </select>
            </div>

            {/* 구/군 선택 */}
            <div className="flex flex-col">
              <label className="text-xs text-gray-600 mb-1">구/군</label>
              <select
                value={selectedGugun}
                onChange={(e) => setSelectedGugun(e.target.value)}
                disabled={!selectedSido}
                className="px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900 disabled:bg-gray-100 disabled:cursor-not-allowed disabled:text-gray-600 min-w-[120px]"
              >
                <option value="">구/군 선택</option>
                {gugunList.filter((gugun) => gugun != null).map((gugun) => (
                  <option key={gugun} value={gugun}>
                    {gugun}
                  </option>
                ))}
              </select>
            </div>

            {/* 동 선택 */}
            <div className="flex flex-col">
              <label className="text-xs text-gray-600 mb-1">동</label>
              <select
                value={selectedDong}
                onChange={(e) => setSelectedDong(e.target.value)}
                disabled={!selectedGugun}
                className="px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900 disabled:bg-gray-100 disabled:cursor-not-allowed disabled:text-gray-600 min-w-[120px]"
              >
                <option value="">동 선택</option>
                {dongList.filter((dong) => dong != null).map((dong) => (
                  <option key={dong} value={dong}>
                    {dong}
                  </option>
                ))}
              </select>
            </div>
          </>
        )}

        {/* 지하철역 검색일 때 */}
        {searchConditionType === 'subway' && (
          <div className="flex-1 min-w-[300px]">
            <div className="flex gap-2">
              <select
                value={subwaySearchType}
                onChange={(e) => setSubwaySearchType(e.target.value)}
                className="px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900"
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
                className="flex-1 px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900 placeholder:text-gray-600"
              />
              <button
                type="button"
                onClick={handleSubwaySearch}
                className="px-4 py-2 bg-gray-100 text-gray-700 rounded-lg text-sm hover:bg-gray-200 transition-colors"
              >
                검색
              </button>
            </div>
            {/* 지하철역 검색 결과 */}
            {subwayResults.length > 0 && (
              <div className="mt-2 border border-gray-200 rounded-lg max-h-40 overflow-y-auto">
                {subwayResults.slice(0, 5).map((station) => (
                  <button
                    key={station.stationId}
                    type="button"
                    onClick={() => handleSelectSubwayStation(station)}
                    className={`w-full px-3 py-2 text-left text-sm hover:bg-gray-50 border-b border-gray-100 last:border-b-0 ${
                      selectedSubwayStation?.stationId === station.stationId
                        ? 'bg-blue-50 border-blue-200'
                        : ''
                    }`}
                  >
                    <div className="font-medium text-gray-900">{station.stationName}</div>
                    <div className="text-xs text-gray-600">
                      {station.lineNames?.join(', ') || '-'}
                    </div>
                  </button>
                ))}
              </div>
            )}
            {selectedSubwayStation && (
              <div className="mt-2 flex flex-wrap items-center gap-3">
                <div className="text-sm text-gray-700">
                  선택: <span className="font-medium">{selectedSubwayStation.stationName}</span>
                </div>
                <div className="flex items-center gap-2">
                  <span className="text-sm text-gray-700 font-medium">반경</span>
                  <select
                    value={subwayRadius}
                    onChange={(e) => {
                      setSubwayRadius(parseFloat(e.target.value));
                      setSubwayRadiusActive(true);
                    }}
                    className="px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900"
                  >
                    <option value={0.5}>0.5km</option>
                    <option value={1.0}>1km</option>
                    <option value={2.0}>2km</option>
                    <option value={3.0}>3km</option>
                    <option value={5.0}>5km</option>
                    <option value={10.0}>10km</option>
                  </select>
                </div>
              </div>
            )}
          </div>
        )}

        {/* 기간 선택 */}
        <div className="flex flex-col">
          <label className="text-xs text-gray-600 mb-1">기간</label>
          <select
            value={period}
            onChange={(e) => setPeriod(Number(e.target.value))}
            className="px-3 py-2 border-2 border-blue-500 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900 bg-white font-medium min-w-[120px]"
          >
            <option value={6}>최근 6개월</option>
            <option value={12}>최근 1년</option>
            <option value={24}>최근 2년</option>
            <option value={36}>최근 3년</option>
            <option value={48}>최근 4년</option>
          </select>
        </div>

        {/* 가격 범위 */}
        {searchConditionType === 'region' && (
          <>
            {tradeType === '매매' ? (
              <>
                <div className="flex flex-col">
                  <label className="text-xs text-gray-600 mb-1">최소(만원)</label>
                  <div className="relative">
                    <input
                      type="number"
                      value={priceMin}
                      onChange={(e) => setPriceMin(e.target.value)}
                      className="px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900 w-32 pr-8"
                    />
                    <div className="absolute right-2 top-1/2 -translate-y-1/2 flex flex-col">
                      <button
                        type="button"
                        onClick={() => setPriceMin(String(Number(priceMin || 0) + 1000))}
                        className="text-xs text-gray-500 hover:text-gray-700"
                      >
                        ▲
                      </button>
                      <button
                        type="button"
                        onClick={() => setPriceMin(String(Math.max(0, Number(priceMin || 0) - 1000)))}
                        className="text-xs text-gray-500 hover:text-gray-700"
                      >
                        ▼
                      </button>
                    </div>
                  </div>
                </div>
                <span className="text-gray-500 mt-6">~</span>
                <div className="flex flex-col">
                  <label className="text-xs text-gray-600 mb-1">최대(만원)</label>
                  <input
                    type="number"
                    value={priceMax}
                    onChange={(e) => setPriceMax(e.target.value)}
                    className="px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900 w-32"
                  />
                </div>
              </>
            ) : (
              <>
                <div className="flex flex-col">
                  <label className="text-xs text-gray-600 mb-1">보증금 최소(만원)</label>
                  <input
                    type="number"
                    value={depositMin}
                    onChange={(e) => setDepositMin(e.target.value)}
                    className="px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900 w-32"
                  />
                </div>
                <span className="text-gray-500 mt-6">~</span>
                <div className="flex flex-col">
                  <label className="text-xs text-gray-600 mb-1">보증금 최대(만원)</label>
                  <input
                    type="number"
                    value={depositMax}
                    onChange={(e) => setDepositMax(e.target.value)}
                    className="px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900 w-32"
                  />
                </div>
                <div className="flex flex-col">
                  <label className="text-xs text-gray-600 mb-1">월세 최소(만원)</label>
                  <input
                    type="number"
                    value={monthlyRentMin}
                    onChange={(e) => setMonthlyRentMin(e.target.value)}
                    className="px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900 w-32"
                  />
                </div>
                <span className="text-gray-500 mt-6">~</span>
                <div className="flex flex-col">
                  <label className="text-xs text-gray-600 mb-1">월세 최대(만원)</label>
                  <input
                    type="number"
                    value={monthlyRentMax}
                    onChange={(e) => setMonthlyRentMax(e.target.value)}
                    className="px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900 w-32"
                  />
                </div>
              </>
            )}
          </>
        )}

        {/* 조회하기 버튼 */}
        <div className="flex items-end">
          <button
            onClick={handleSearch}
            disabled={
              searchConditionType === 'region'
                ? !selectedSido || !selectedGugun
                : !selectedSubwayStation
            }
            className={`px-6 py-2 rounded-lg text-sm font-medium transition-colors ${
              (searchConditionType === 'region' && selectedSido && selectedGugun) ||
              (searchConditionType === 'subway' && selectedSubwayStation)
                ? 'bg-blue-600 text-white hover:bg-blue-700'
                : 'bg-gray-300 text-gray-500 cursor-not-allowed'
            }`}
          >
            조회하기
          </button>
        </div>
      </div>

      {/* 학교 필터 (지역 검색일 때만) */}
      {searchConditionType === 'region' && selectedSido && selectedGugun && (
        <div className="mt-4 pt-4 border-t border-gray-200">
          <label className="block text-sm font-medium text-gray-700 mb-2">학교 필터</label>
          <div className="flex gap-4 mb-3">
            {['초등학교', '중학교', '고등학교'].map((type) => (
              <label key={type} className="flex items-center gap-2 cursor-pointer">
                <input
                  type="checkbox"
                  checked={schoolTypes[type]}
                  onChange={() => handleSchoolTypeChange(type)}
                  className="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
                />
                <span className="text-sm text-gray-700">{type}</span>
              </label>
            ))}
          </div>

          {/* 학교 반경 선택 */}
          {Object.values(schoolTypes).some((selected) => selected) && (
            <div className="flex items-center gap-4">
              <label className="text-sm font-medium text-gray-700">반경:</label>
              <select
                value={schoolRadius}
                onChange={(e) => {
                  setSchoolRadius(parseFloat(e.target.value));
                  setSchoolRadiusActive(true);
                }}
                className="px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900"
              >
                <option value={0.5}>0.5km</option>
                <option value={1.0}>1.0km</option>
                <option value={2.0}>2.0km</option>
                <option value={3.0}>3.0km</option>
                <option value={5.0}>5.0km</option>
                <option value={10.0}>10.0km</option>
              </select>
            </div>
          )}

          {/* 학교 목록 표시 */}
          {schoolList.length > 0 && (
            <div className="mt-3 border border-gray-200 rounded-lg max-h-40 overflow-y-auto">
              <div className="px-3 py-2 bg-gray-50 text-xs font-medium text-gray-700 border-b">
                검색된 학교 ({schoolList.length}개)
              </div>
              {schoolList.map((school) => (
                <div key={school.id} className="px-3 py-2 border-b border-gray-100 last:border-b-0">
                  <div className="text-sm font-medium text-gray-900">{school.name}</div>
                  <div className="text-xs text-gray-600">
                    {school.schoolLevel} · {school.roadAddress}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
