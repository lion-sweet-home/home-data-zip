'use client';

import { useState, useEffect, useCallback, useRef } from 'react';
import { useRouter } from 'next/navigation';
import { searchListings, getSaleListings, getRentListings } from '../../api/listing';
import { searchApartmentsByName } from '../../api/apartment';
import { getSidoList, getGugunList, getDongList } from '../../api/region';
import { addFavorite, removeFavorite, getMyFavorites } from '../../api/favorite';

function formatPrice(won) {
  const n = Number(won);
  if (!Number.isFinite(n) || n <= 0) return '-';
  const manwon = Math.round(n / 10000);
  if (manwon <= 0) return '-';
  const eok = Math.floor(manwon / 10000);
  const rest = manwon % 10000;
  if (eok > 0 && rest > 0) return `${eok}억 ${rest.toLocaleString()}만`;
  if (eok > 0) return `${eok}억`;
  return `${manwon.toLocaleString()}만`;
}

function formatDate(dateStr) {
  if (!dateStr) return '-';
  const d = new Date(dateStr);
  if (isNaN(d.getTime())) return '-';
  return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, '0')}.${String(d.getDate()).padStart(2, '0')}`;
}

const TRADE_BADGE = {
  SALE: { label: '매매', bg: 'bg-blue-100', text: 'text-blue-700' },
  RENT_CHARTER: { label: '전세', bg: 'bg-emerald-100', text: 'text-emerald-700' },
  RENT_MONTHLY: { label: '월세', bg: 'bg-orange-100', text: 'text-orange-700' },
};

function getBadge(tradeType, rentType) {
  if (tradeType === 'SALE') return TRADE_BADGE.SALE;
  if (rentType === 'MONTHLY') return TRADE_BADGE.RENT_MONTHLY;
  return TRADE_BADGE.RENT_CHARTER;
}

function getPriceText(item) {
  if (item.tradeType === 'SALE') {
    return formatPrice(item.salePrice);
  }
  const dep = formatPrice(item.deposit);
  if (item.monthlyRent && item.monthlyRent > 0) {
    return `${dep} / 월 ${formatPrice(item.monthlyRent)}`;
  }
  return dep;
}

const TABS = [
  { key: 'all', label: '전체' },
  { key: 'sale', label: '매매' },
  { key: 'rent', label: '전/월세' },
];

const RENT_TYPES = [
  { key: '', label: '전체' },
  { key: 'CHARTER', label: '전세' },
  { key: 'MONTHLY', label: '월세' },
];

export default function ListingSearchPage() {
  const router = useRouter();

  const [activeTab, setActiveTab] = useState('all');
  const [rentType, setRentType] = useState('');
  const [limit, setLimit] = useState(50);

  // 아파트 검색 필터
  const [aptKeyword, setAptKeyword] = useState('');
  const [aptResults, setAptResults] = useState([]);
  const [aptSearching, setAptSearching] = useState(false);
  const [selectedApt, setSelectedApt] = useState(null);
  const [showAptDropdown, setShowAptDropdown] = useState(false);
  const aptDropdownRef = useRef(null);

  // 지역 필터
  const [sidoList, setSidoList] = useState([]);
  const [gugunList, setGugunList] = useState([]);
  const [dongList, setDongList] = useState([]);
  const [selectedSido, setSelectedSido] = useState('');
  const [selectedGugun, setSelectedGugun] = useState('');
  const [selectedDong, setSelectedDong] = useState('');

  // 결과
  const [listings, setListings] = useState([]);
  const [loading, setLoading] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);

  // 관심매물
  const [favoriteIds, setFavoriteIds] = useState(new Set());
  const [favoriteLoadingId, setFavoriteLoadingId] = useState(null);

  // 관심매물 목록 로드
  useEffect(() => {
    let cancelled = false;
    getMyFavorites()
      .then((list) => {
        if (cancelled) return;
        if (Array.isArray(list)) {
          setFavoriteIds(new Set(list.map((f) => String(f.listingId))));
        }
      })
      .catch(() => {});
    return () => { cancelled = true; };
  }, []);

  const handleToggleFavorite = async (e, listingId) => {
    e.stopPropagation();
    if (favoriteLoadingId === listingId) return;
    setFavoriteLoadingId(listingId);
    const id = String(listingId);
    try {
      if (favoriteIds.has(id)) {
        await removeFavorite(listingId);
        setFavoriteIds((prev) => {
          const next = new Set(prev);
          next.delete(id);
          return next;
        });
      } else {
        await addFavorite(listingId);
        setFavoriteIds((prev) => new Set(prev).add(id));
      }
    } catch (err) {
      if (err.status === 401) {
        alert('로그인이 필요합니다.');
        router.push('/auth/login');
        return;
      }
      console.error('관심 매물 처리 실패:', err);
    } finally {
      setFavoriteLoadingId(null);
    }
  };

  // 시도 목록 로드
  useEffect(() => {
    getSidoList()
      .then((data) => setSidoList(Array.isArray(data) ? data : []))
      .catch(() => setSidoList([]));
  }, []);

  // 구군 목록 로드
  useEffect(() => {
    setSelectedGugun('');
    setGugunList([]);
    setSelectedDong('');
    setDongList([]);
    if (!selectedSido) return;
    getGugunList(selectedSido)
      .then((data) => setGugunList(Array.isArray(data) ? data : []))
      .catch(() => setGugunList([]));
  }, [selectedSido]);

  // 동 목록 로드
  useEffect(() => {
    setSelectedDong('');
    setDongList([]);
    if (!selectedSido || !selectedGugun) return;
    getDongList(selectedSido, selectedGugun)
      .then((data) => setDongList(Array.isArray(data) ? data : []))
      .catch(() => setDongList([]));
  }, [selectedSido, selectedGugun]);

  // 아파트 드롭다운 외부 클릭 닫기
  useEffect(() => {
    const handleClickOutside = (e) => {
      if (aptDropdownRef.current && !aptDropdownRef.current.contains(e.target)) {
        setShowAptDropdown(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleAptSearch = async () => {
    const kw = aptKeyword.trim();
    if (!kw) {
      setAptResults([]);
      setShowAptDropdown(false);
      return;
    }
    setAptSearching(true);
    try {
      const res = await searchApartmentsByName(kw, selectedSido || undefined, selectedGugun || undefined);
      const list = Array.isArray(res) ? res : [];
      setAptResults(list);
      setShowAptDropdown(list.length > 0);
    } catch {
      setAptResults([]);
      setShowAptDropdown(false);
    } finally {
      setAptSearching(false);
    }
  };

  const handleSelectApt = (apt) => {
    setSelectedApt(apt);
    setAptKeyword(apt.aptName || '');
    setShowAptDropdown(false);
    setAptResults([]);
  };

  const handleClearApt = () => {
    setSelectedApt(null);
    setAptKeyword('');
    setAptResults([]);
    setShowAptDropdown(false);
  };

  const handleSearch = useCallback(async () => {
    setLoading(true);
    setHasSearched(true);
    try {
      const params = { limit };
      if (selectedSido) params.sido = selectedSido;
      if (selectedGugun) params.gugun = selectedGugun;
      if (selectedDong) params.dong = selectedDong;
      if (selectedApt?.aptName) {
        params.apartmentName = selectedApt.aptName;
      }

      let results;
      if (activeTab === 'sale') {
        results = await getSaleListings(params);
      } else if (activeTab === 'rent') {
        if (rentType) params.rentType = rentType;
        results = await getRentListings(params);
      } else {
        results = await searchListings(params);
      }
      setListings(Array.isArray(results) ? results : []);
    } catch (err) {
      console.error('매물 검색 실패:', err);
      setListings([]);
    } finally {
      setLoading(false);
    }
  }, [activeTab, selectedApt, rentType, limit, selectedSido, selectedGugun, selectedDong]);

  // 초기 로드
  useEffect(() => {
    handleSearch();
  }, []);

  const handleTabChange = (tab) => {
    setActiveTab(tab);
    if (tab !== 'rent') setRentType('');
  };

  const selectBase =
    'px-3 py-2.5 border border-gray-300 rounded-xl text-sm outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-gray-900 bg-white appearance-none cursor-pointer';

  return (
    <div className="min-h-screen bg-gray-50 py-8 px-4">
      <div className="max-w-7xl mx-auto">
        {/* 헤더 */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900">매물 검색</h1>
          <p className="mt-2 text-gray-500">등록된 매물을 검색하고 비교해보세요.</p>
        </div>

        {/* 탭 */}
        <div className="flex gap-1 mb-6 bg-gray-100 rounded-xl p-1 w-fit">
          {TABS.map((tab) => (
            <button
              key={tab.key}
              onClick={() => handleTabChange(tab.key)}
              className={`px-5 py-2.5 rounded-lg text-sm font-semibold transition-all ${
                activeTab === tab.key
                  ? 'bg-white text-blue-600 shadow-sm'
                  : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {/* 필터 영역 */}
        <div className="bg-white rounded-2xl shadow-sm border border-gray-200 p-6 mb-6">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
            {/* 지역 필터: 시/도 */}
            <div>
              <label className="block text-xs font-medium text-gray-500 mb-1.5">시/도</label>
              <select
                value={selectedSido}
                onChange={(e) => setSelectedSido(e.target.value)}
                className={`w-full ${selectBase}`}
              >
                <option value="">전체</option>
                {sidoList.filter(Boolean).map((sido) => (
                  <option key={sido} value={sido}>{sido}</option>
                ))}
              </select>
            </div>

            {/* 지역 필터: 구/군 */}
            <div>
              <label className="block text-xs font-medium text-gray-500 mb-1.5">구/군</label>
              <select
                value={selectedGugun}
                onChange={(e) => setSelectedGugun(e.target.value)}
                disabled={!selectedSido}
                className={`w-full ${selectBase} ${!selectedSido ? 'bg-gray-50 cursor-not-allowed text-gray-400' : ''}`}
              >
                <option value="">전체</option>
                {gugunList.filter(Boolean).map((gugun) => (
                  <option key={gugun} value={gugun}>{gugun}</option>
                ))}
              </select>
            </div>

            {/* 지역 필터: 동 */}
            <div>
              <label className="block text-xs font-medium text-gray-500 mb-1.5">동</label>
              <select
                value={selectedDong}
                onChange={(e) => setSelectedDong(e.target.value)}
                disabled={!selectedGugun}
                className={`w-full ${selectBase} ${!selectedGugun ? 'bg-gray-50 cursor-not-allowed text-gray-400' : ''}`}
              >
                <option value="">전체</option>
                {dongList.filter(Boolean).map((dong) => (
                  <option key={dong} value={dong}>{dong}</option>
                ))}
              </select>
            </div>

            {/* 전/월세 타입 (전/월세 탭에서만) */}
            {activeTab === 'rent' && (
              <div>
                <label className="block text-xs font-medium text-gray-500 mb-1.5">전/월세 구분</label>
                <div className="flex gap-1.5">
                  {RENT_TYPES.map((rt) => (
                    <button
                      key={rt.key}
                      onClick={() => setRentType(rt.key)}
                      className={`flex-1 px-3 py-2.5 rounded-xl text-sm font-medium transition-all ${
                        rentType === rt.key
                          ? 'bg-blue-600 text-white'
                          : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                      }`}
                    >
                      {rt.label}
                    </button>
                  ))}
                </div>
              </div>
            )}

            {/* 조회 수 */}
            <div>
              <label className="block text-xs font-medium text-gray-500 mb-1.5">조회 수</label>
              <select
                value={limit}
                onChange={(e) => setLimit(Number(e.target.value))}
                className={`w-full ${selectBase}`}
              >
                <option value={20}>20개</option>
                <option value={50}>50개</option>
                <option value={100}>100개</option>
              </select>
            </div>
          </div>

          {/* 아파트 검색 */}
          <div className="mt-4 pt-4 border-t border-gray-100">
            <label className="block text-xs font-medium text-gray-500 mb-1.5">아파트 필터</label>
            <div className="flex gap-2 items-start" ref={aptDropdownRef}>
              <div className="flex-1 relative">
                <div className="relative">
                  <div className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                      <path d="M10 18a8 8 0 1 1 0-16 8 8 0 0 1 0 16Zm11 3-6-6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                    </svg>
                  </div>
                  <input
                    value={aptKeyword}
                    onChange={(e) => {
                      setAptKeyword(e.target.value);
                      if (selectedApt) setSelectedApt(null);
                    }}
                    onKeyDown={(e) => e.key === 'Enter' && handleAptSearch()}
                    placeholder="아파트명 입력 (선택)"
                    className="w-full pl-9 pr-9 py-2.5 border border-gray-300 rounded-xl text-sm outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-gray-900 placeholder:text-gray-400"
                  />
                  {(aptKeyword || selectedApt) && (
                    <button
                      onClick={handleClearApt}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                    >
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                        <path d="M18 6L6 18M6 6l12 12" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                      </svg>
                    </button>
                  )}
                </div>

                {/* 아파트 검색 드롭다운 */}
                {showAptDropdown && aptResults.length > 0 && (
                  <div className="absolute z-20 mt-1 w-full bg-white border border-gray-200 rounded-xl shadow-lg max-h-60 overflow-y-auto">
                    {aptResults.slice(0, 20).map((apt, idx) => (
                      <button
                        key={`${apt.aptId}-${idx}`}
                        onClick={() => handleSelectApt(apt)}
                        className="w-full text-left px-4 py-3 hover:bg-gray-50 border-b border-gray-50 last:border-b-0 transition-colors"
                      >
                        <div className="font-medium text-sm text-gray-900">{apt.aptName}</div>
                        <div className="text-xs text-gray-500 mt-0.5">
                          {apt.gu && `${apt.gu} `}{apt.dong && apt.dong}
                        </div>
                      </button>
                    ))}
                  </div>
                )}
              </div>

              <button
                onClick={handleAptSearch}
                disabled={aptSearching || !aptKeyword.trim()}
                className={`px-4 py-2.5 rounded-xl text-sm font-medium transition-colors shrink-0 ${
                  aptKeyword.trim() && !aptSearching
                    ? 'bg-gray-900 text-white hover:bg-gray-800'
                    : 'bg-gray-100 text-gray-400 cursor-not-allowed'
                }`}
              >
                {aptSearching ? '검색중...' : '아파트 검색'}
              </button>
            </div>

            {selectedApt && (
              <div className="mt-2 flex items-center gap-2">
                <span className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-blue-50 text-blue-700 rounded-lg text-sm font-medium">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                    <path d="M3 21V7l9-4 9 4v14" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                    <path d="M9 21V11h6v10" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                  </svg>
                  {selectedApt.aptName}
                  <button onClick={handleClearApt} className="ml-1 hover:text-blue-900">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none">
                      <path d="M18 6L6 18M6 6l12 12" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                    </svg>
                  </button>
                </span>
              </div>
            )}
          </div>

          {/* 검색 버튼 */}
          <div className="mt-5">
            <button
              onClick={handleSearch}
              disabled={loading}
              className={`w-full py-3 rounded-xl font-semibold text-sm transition-all ${
                loading
                  ? 'bg-gray-200 text-gray-400 cursor-not-allowed'
                  : 'bg-blue-600 text-white hover:bg-blue-700 active:scale-[0.99]'
              }`}
            >
              {loading ? (
                <span className="flex items-center justify-center gap-2">
                  <svg className="animate-spin h-4 w-4" viewBox="0 0 24 24" fill="none">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                  </svg>
                  검색 중...
                </span>
              ) : '매물 검색'}
            </button>
          </div>
        </div>

        {/* 결과 영역 */}
        {hasSearched && (
          <div>
            {/* 결과 헤더 */}
            <div className="flex items-center justify-between mb-4">
              <div className="text-sm text-gray-600">
                총 <span className="font-semibold text-gray-900">{listings.length}</span>건의 매물
              </div>
            </div>

            {loading ? (
              <div className="flex flex-col items-center justify-center py-20">
                <svg className="animate-spin h-8 w-8 text-blue-500 mb-4" viewBox="0 0 24 24" fill="none">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                </svg>
                <p className="text-gray-500">매물을 검색하고 있습니다...</p>
              </div>
            ) : listings.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-20 bg-white rounded-2xl border border-gray-200">
                <svg width="48" height="48" viewBox="0 0 24 24" fill="none" className="text-gray-300 mb-4">
                  <path d="M3 21V7l9-4 9 4v14" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
                  <path d="M9 21V11h6v10" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
                <p className="text-gray-500 font-medium">검색 결과가 없습니다.</p>
                <p className="text-sm text-gray-400 mt-1">필터 조건을 변경해보세요.</p>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {listings.map((item) => {
                  const badge = getBadge(item.tradeType, item.rentType);
                  return (
                    <div
                      key={item.listingId}
                      className="bg-white rounded-2xl border border-gray-200 overflow-hidden hover:shadow-md hover:border-gray-300 transition-all group cursor-pointer"
                      onClick={() => router.push(`/search/listing/${item.listingId}`)}
                    >
                      {/* 이미지 */}
                      <div className="relative aspect-[16/10] bg-gray-100 overflow-hidden">
                        {item.mainImageUrl ? (
                          <img
                            src={item.mainImageUrl}
                            alt={item.apartmentName || '매물 이미지'}
                            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                          />
                        ) : (
                          <div className="w-full h-full flex items-center justify-center text-gray-300">
                            <svg width="48" height="48" viewBox="0 0 24 24" fill="none">
                              <path d="M3 21V7l9-4 9 4v14" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
                              <path d="M9 21V11h6v10" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
                            </svg>
                          </div>
                        )}
                        <span className={`absolute top-3 left-3 px-2.5 py-1 rounded-lg text-xs font-semibold ${badge.bg} ${badge.text}`}>
                          {badge.label}
                        </span>
                        <button
                          onClick={(e) => handleToggleFavorite(e, item.listingId)}
                          disabled={favoriteLoadingId === item.listingId}
                          className={`absolute top-3 right-3 w-9 h-9 rounded-full flex items-center justify-center transition-all ${
                            favoriteIds.has(String(item.listingId))
                              ? 'bg-red-50 text-red-500 hover:bg-red-100'
                              : 'bg-white/80 text-gray-400 hover:bg-white hover:text-gray-500'
                          } ${favoriteLoadingId === item.listingId ? 'opacity-50 cursor-not-allowed' : ''}`}
                          title={favoriteIds.has(String(item.listingId)) ? '관심 매물 해제' : '관심 매물 등록'}
                        >
                          <svg width="18" height="18" viewBox="0 0 24 24" fill={favoriteIds.has(String(item.listingId)) ? 'currentColor' : 'none'}>
                            <path
                              d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z"
                              stroke="currentColor"
                              strokeWidth="2"
                              strokeLinecap="round"
                              strokeLinejoin="round"
                            />
                          </svg>
                        </button>
                      </div>

                      {/* 정보 */}
                      <div className="p-4">
                        {/* 아파트명 + 가격 (같은 줄) */}
                        <div className="flex items-baseline justify-between gap-3 mb-1">
                          <div className="text-lg font-bold text-gray-900 truncate min-w-0">
                            {item.apartmentName || '아파트'}
                            {item.buildYear && (
                              <span className="text-gray-400 font-normal text-sm ml-1">({item.buildYear}년)</span>
                            )}
                          </div>
                          <div className="text-sm font-semibold text-blue-700 whitespace-nowrap flex-shrink-0">
                            {getPriceText(item)}
                          </div>
                        </div>

                        {/* 주소 */}
                        {item.jibunAddress && (
                          <div className="text-xs text-gray-500 mb-2 truncate">{item.jibunAddress}</div>
                        )}

                        {/* 상세 정보 */}
                        <div className="flex items-center gap-3 text-xs text-gray-500 mb-3">
                          {item.exclusiveArea && (
                            <span className="flex items-center gap-1">
                              <svg width="12" height="12" viewBox="0 0 24 24" fill="none">
                                <rect x="3" y="3" width="18" height="18" rx="2" stroke="currentColor" strokeWidth="2" />
                              </svg>
                              {Number(item.exclusiveArea).toFixed(1)}m²
                            </span>
                          )}
                          {item.floor != null && (
                            <span className="flex items-center gap-1">
                              <svg width="12" height="12" viewBox="0 0 24 24" fill="none">
                                <path d="M3 21h18M5 21V7l7-4 7 4v14" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                              </svg>
                              {item.floor}층
                            </span>
                          )}
                          <span className="flex items-center gap-1">
                            <svg width="12" height="12" viewBox="0 0 24 24" fill="none">
                              <rect x="3" y="4" width="18" height="18" rx="2" stroke="currentColor" strokeWidth="2" />
                              <path d="M16 2v4M8 2v4M3 10h18" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                            </svg>
                            {formatDate(item.createdAt)}
                          </span>
                        </div>

                        {/* 설명 */}
                        {item.description && (
                          <p className="text-xs text-gray-500 line-clamp-2 leading-relaxed">
                            {item.description}
                          </p>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
