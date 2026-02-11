'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { getSidoList, getGugunList, getDongList } from '../api/region';
import { searchSubwayStations } from '../api/subway';
import { searchSchoolsByRegion } from '../api/school';

export default function SearchPage() {
  const router = useRouter();

  // 매매/전월세 선택
  const [tradeType, setTradeType] = useState('매매'); // '매매' 또는 '전월세'

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
  const [subwaySearchType, setSubwaySearchType] = useState('stationName'); // 'stationName' 또는 'lineName'
  const [subwayResults, setSubwayResults] = useState([]);
  const [selectedSubwayStation, setSelectedSubwayStation] = useState(null);
  const [subwayRadius, setSubwayRadius] = useState(1.0);
  const [subwayRadiusActive, setSubwayRadiusActive] = useState(false);

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
        setSelectedGugun('');
        setDongList([]);
        setSelectedDong('');
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
        setSelectedDong('');
      } catch (error) {
        console.error('동 목록 로드 실패:', error);
      }
    };
    loadDongList();
  }, [selectedSido, selectedGugun]);

  // 지역 선택 시 학교 목록 로드
  useEffect(() => {
    if (!selectedSido || !selectedGugun) {
      setSchoolList([]);
      return;
    }

    const loadSchools = async () => {
      try {
        const selectedTypes = Object.entries(schoolTypes)
          .filter(([_, selected]) => selected)
          .map(([type, _]) => type);

        const schools = await searchSchoolsByRegion({
          sido: selectedSido,
          gugun: selectedGugun,
          dong: selectedDong || undefined,
          schoolLevel: selectedTypes.length > 0 ? selectedTypes : undefined,
        });
        setSchoolList(schools);
      } catch (error) {
        console.error('학교 목록 로드 실패:', error);
      }
    };

    loadSchools();
  }, [selectedSido, selectedGugun, selectedDong, schoolTypes]);

  // 지하철역 검색
  const handleSubwaySearch = async () => {
    if (!subwaySearchKeyword.trim()) {
      setSubwayResults([]);
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
      setSubwayResults(results);
    } catch (error) {
      console.error('지하철역 검색 실패:', error);
      setSubwayResults([]);
    }
  };

  // 지하철역 선택
  const handleSelectSubwayStation = (station) => {
    setSelectedSubwayStation(station);
    setSubwayRadiusActive(true);
  };

  // 학교 타입 체크박스 변경
  const handleSchoolTypeChange = (type) => {
    setSchoolTypes((prev) => ({
      ...prev,
      [type]: !prev[type],
    }));
  };

  // 검색 버튼 클릭 핸들러
  const handleSearch = () => {
    if (searchConditionType === 'region') {
      // 지역 검색: 시/도와 구/군은 필수
      if (!selectedSido || !selectedGugun) {
        alert('시/도와 구/군을 선택해주세요.');
        return;
      }

      // 검색 파라미터를 쿼리 스트링으로 전달
      const params = new URLSearchParams();
      params.append('tradeType', tradeType);
      params.append('sido', selectedSido);
      params.append('gugun', selectedGugun);
      if (selectedDong) params.append('dong', selectedDong);
      if (tradeType === '매매') {
        if (priceMin) params.append('priceMin', priceMin);
        if (priceMax) params.append('priceMax', priceMax);
      } else {
        if (depositMin) params.append('depositMin', depositMin);
        if (depositMax) params.append('depositMax', depositMax);
        if (monthlyRentMin) params.append('monthlyRentMin', monthlyRentMin);
        if (monthlyRentMax) params.append('monthlyRentMax', monthlyRentMax);
      }
      
      // 학교 필터
      const selectedSchoolTypes = Object.entries(schoolTypes)
        .filter(([_, selected]) => selected)
        .map(([type]) => type);
      if (selectedSchoolTypes.length > 0) {
        params.append('schoolTypes', selectedSchoolTypes.join(','));
        if (schoolRadiusActive) params.append('schoolRadius', schoolRadius);
      }

      router.push(`/search/map?${params.toString()}`);
    } else if (searchConditionType === 'subway') {
      // 지하철역 검색: 역 선택 필수
      if (!selectedSubwayStation) {
        alert('지하철역을 선택해주세요.');
        return;
      }

      const params = new URLSearchParams();
      params.append('tradeType', tradeType);
      params.append('subwayStationId', selectedSubwayStation.stationId);
      params.append('subwayStationName', selectedSubwayStation.stationName);
      if (subwayRadiusActive) params.append('subwayRadius', subwayRadius);

      router.push(`/search/map?${params.toString()}`);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 py-8 px-4">
      <div className="max-w-7xl mx-auto">
        <h1 className="text-3xl font-bold text-gray-900 mb-8">지도 검색</h1>

        <div className="bg-white rounded-lg shadow-md p-6 space-y-6">
          {/* 매매/전월세 선택 */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-3">
              거래 유형
            </label>
            <div className="flex gap-4">
              <button
                type="button"
                onClick={() => setTradeType('매매')}
                className={`px-6 py-3 rounded-lg font-medium transition-colors ${
                  tradeType === '매매'
                    ? 'bg-blue-600 text-white'
                    : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                }`}
              >
                매매
              </button>
              <button
                type="button"
                onClick={() => setTradeType('전월세')}
                className={`px-6 py-3 rounded-lg font-medium transition-colors ${
                  tradeType === '전월세'
                    ? 'bg-blue-600 text-white'
                    : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                }`}
              >
                전/월세
              </button>
            </div>
          </div>

          {/* 검색 조건 */}
          <div className="space-y-6">
            <h2 className="text-xl font-semibold text-gray-900">검색 조건</h2>

            {/* 검색 조건 타입 선택 (라디오 버튼) */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-3">
                검색 조건 선택
              </label>
              <div className="flex gap-4">
                <button
                  type="button"
                  onClick={() => {
                    setSearchConditionType('region');
                    // 지하철역 검색 상태 초기화
                    setSubwaySearchKeyword('');
                    setSubwayResults([]);
                    setSelectedSubwayStation(null);
                    setSubwayRadiusActive(false);
                  }}
                  className={`px-6 py-3 rounded-lg font-medium transition-colors ${
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
                    // 지역 선택 상태 초기화
                    setSelectedSido('');
                    setSelectedGugun('');
                    setSelectedDong('');
                    setSchoolList([]);
                  }}
                  className={`px-6 py-3 rounded-lg font-medium transition-colors ${
                    searchConditionType === 'subway'
                      ? 'bg-blue-600 text-white'
                      : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                  }`}
                >
                  지하철역 검색
                </button>
              </div>
            </div>

            {/* 지역 검색 */}
            {searchConditionType === 'region' && (
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  지역 선택
                </label>
                <div className="grid grid-cols-3 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      시/도 <span className="text-red-500">*</span>
                    </label>
                    <select
                      value={selectedSido}
                      onChange={(e) => setSelectedSido(e.target.value)}
                      required
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900"
                    >
                      <option value="">시/도 선택</option>
                      {sidoList.filter((sido) => sido != null).map((sido) => (
                        <option key={sido} value={sido}>
                          {sido}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      구/군 <span className="text-red-500">*</span>
                    </label>
                    <select
                      value={selectedGugun}
                      onChange={(e) => setSelectedGugun(e.target.value)}
                      disabled={!selectedSido}
                      required
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900 disabled:bg-gray-100 disabled:cursor-not-allowed disabled:text-gray-600"
                    >
                      <option value="">구/군 선택</option>
                      {gugunList.filter((gugun) => gugun != null).map((gugun) => (
                        <option key={gugun} value={gugun}>
                          {gugun}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      동
                    </label>
                    <select
                      value={selectedDong}
                      onChange={(e) => setSelectedDong(e.target.value)}
                      disabled={!selectedGugun}
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900 disabled:bg-gray-100 disabled:cursor-not-allowed disabled:text-gray-600"
                    >
                      <option value="">동 선택</option>
                      {dongList.filter((dong) => dong != null).map((dong) => (
                        <option key={dong} value={dong}>
                          {dong}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>
              </div>
            )}

            {/* 지하철역 검색 */}
            {searchConditionType === 'subway' && (
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  지하철역 검색
                </label>
                <div className="flex gap-2 mb-2">
                  <select
                    value={subwaySearchType}
                    onChange={(e) => setSubwaySearchType(e.target.value)}
                    className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900"
                  >
                    <option value="stationName">역명</option>
                    <option value="lineName">호선</option>
                  </select>
                  <input
                    type="text"
                    value={subwaySearchKeyword}
                    onChange={(e) => setSubwaySearchKeyword(e.target.value)}
                    onKeyPress={(e) => e.key === 'Enter' && handleSubwaySearch()}
                    placeholder={subwaySearchType === 'stationName' ? '역명 입력' : '호선 입력 (예: 2호선)'}
                    className="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900 placeholder:text-gray-600"
                  />
                  <button
                    type="button"
                    onClick={handleSubwaySearch}
                    className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
                  >
                    검색
                  </button>
                </div>

                {/* 지하철역 검색 결과 */}
                {subwayResults.length > 0 && (
                  <div className="mt-2 border border-gray-200 rounded-lg max-h-60 overflow-y-auto">
                    {subwayResults.map((station) => (
                      <button
                        key={station.stationId}
                        type="button"
                        onClick={() => handleSelectSubwayStation(station)}
                        className={`w-full px-4 py-3 text-left hover:bg-gray-50 border-b border-gray-100 last:border-b-0 ${
                          selectedSubwayStation?.stationId === station.stationId
                            ? 'bg-blue-50 border-blue-200'
                            : ''
                        }`}
                      >
                        <div className="font-medium text-gray-900">
                          {station.stationName}
                        </div>
                        <div className="text-sm text-gray-700">
                          {station.lineNames.join(', ')}
                        </div>
                      </button>
                    ))}
                  </div>
                )}

                {/* 반경 선택 (지하철역 선택 시 활성화) */}
                {subwayRadiusActive && selectedSubwayStation && (
                  <div className="mt-3 flex items-center gap-4">
                    <label className="text-sm font-medium text-gray-700">반경:</label>
                    <select
                      value={subwayRadius}
                      onChange={(e) => setSubwayRadius(parseFloat(e.target.value))}
                      className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900"
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
              </div>
            )}
          </div>

          {/* 지역 검색 선택 시 표시되는 필터 */}
          {searchConditionType === 'region' && selectedSido && selectedGugun && (
            <div className="space-y-6 border-t pt-6">
              {/* 가격 범위 */}
              <div className="space-y-4">
                {tradeType === '매매' ? (
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      매매가 범위
                    </label>
                    <div className="flex items-center gap-4">
                      <input
                        type="number"
                        value={priceMin}
                        onChange={(e) => setPriceMin(e.target.value)}
                        placeholder="최소 매매가 (만원)"
                        className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900 placeholder:text-gray-600"
                      />
                      <span className="text-gray-700 font-medium">~</span>
                      <input
                        type="number"
                        value={priceMax}
                        onChange={(e) => setPriceMax(e.target.value)}
                        placeholder="최대 매매가 (만원)"
                        className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900 placeholder:text-gray-600"
                      />
                    </div>
                  </div>
                ) : (
                  <>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-2">
                        보증금 범위
                      </label>
                      <div className="flex items-center gap-4">
                        <input
                          type="number"
                          value={depositMin}
                          onChange={(e) => setDepositMin(e.target.value)}
                          placeholder="최소 보증금 (만원)"
                          className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900 placeholder:text-gray-600"
                        />
                        <span className="text-gray-700 font-medium">~</span>
                        <input
                          type="number"
                          value={depositMax}
                          onChange={(e) => setDepositMax(e.target.value)}
                          placeholder="최대 보증금 (만원)"
                          className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900 placeholder:text-gray-600"
                        />
                      </div>
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-2">
                        월세 범위
                      </label>
                      <div className="flex items-center gap-4">
                        <input
                          type="number"
                          value={monthlyRentMin}
                          onChange={(e) => setMonthlyRentMin(e.target.value)}
                          placeholder="최소 월세 (만원)"
                          className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900 placeholder:text-gray-600"
                        />
                        <span className="text-gray-700 font-medium">~</span>
                        <input
                          type="number"
                          value={monthlyRentMax}
                          onChange={(e) => setMonthlyRentMax(e.target.value)}
                          placeholder="최대 월세 (만원)"
                          className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900 placeholder:text-gray-600"
                        />
                      </div>
                    </div>
                  </>
                )}
              </div>

              {/* 학교 필터 */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  학교 필터
                </label>
                <div className="flex gap-6 mb-3">
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

                {/* 학교 반경 선택 (학교 타입 선택 시 활성화) */}
                {Object.values(schoolTypes).some((selected) => selected) && (
                  <div className="flex items-center gap-4">
                    <label className="text-sm font-medium text-gray-700">반경:</label>
                    <select
                      value={schoolRadius}
                      onChange={(e) => {
                        setSchoolRadius(parseFloat(e.target.value));
                        setSchoolRadiusActive(true);
                      }}
                      className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none text-gray-900"
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
                  <div className="mt-3 border border-gray-200 rounded-lg max-h-60 overflow-y-auto">
                    <div className="px-4 py-2 bg-gray-50 text-sm font-medium text-gray-700 border-b">
                      검색된 학교 ({schoolList.length}개)
                    </div>
                    {schoolList.map((school) => (
                      <div
                        key={school.id}
                        className="px-4 py-3 border-b border-gray-100 last:border-b-0"
                      >
                        <div className="font-medium text-gray-900">{school.name}</div>
                        <div className="text-sm text-gray-700">
                          {school.schoolLevel} · {school.roadAddress}
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          )}

          {/* 검색 버튼 */}
          <div className="mt-6 pt-6 border-t">
            <button
              onClick={handleSearch}
              disabled={
                searchConditionType === 'region' 
                  ? !selectedSido || !selectedGugun
                  : !selectedSubwayStation
              }
              className={`w-full py-3 px-6 rounded-lg font-medium transition-colors ${
                (searchConditionType === 'region' && selectedSido && selectedGugun) ||
                (searchConditionType === 'subway' && selectedSubwayStation)
                  ? 'bg-blue-600 text-white hover:bg-blue-700'
                  : 'bg-gray-300 text-gray-500 cursor-not-allowed'
              }`}
            >
              검색하기
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
