'use client';

import { useState, useEffect, useCallback } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { getListingDetail } from '../../../api/listing';
import { addFavorite, removeFavorite, getMyFavorites } from '../../../api/favorite';

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

export default function ListingDetailPage() {
  const params = useParams();
  const router = useRouter();
  const listingId = params.id;

  const [detail, setDetail] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [currentImageIndex, setCurrentImageIndex] = useState(0);
  const [showFullscreen, setShowFullscreen] = useState(false);
  const [isFavorited, setIsFavorited] = useState(false);
  const [favoriteLoading, setFavoriteLoading] = useState(false);

  const fetchDetail = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getListingDetail(listingId);
      setDetail(data);
    } catch (err) {
      console.error('매물 상세 조회 실패:', err);
      setError(err.message || '매물 정보를 불러올 수 없습니다.');
    } finally {
      setLoading(false);
    }
  }, [listingId]);

  useEffect(() => {
    if (listingId) fetchDetail();
  }, [listingId, fetchDetail]);

  useEffect(() => {
    let cancelled = false;
    getMyFavorites()
      .then((list) => {
        if (cancelled) return;
        const found = Array.isArray(list) && list.some((f) => String(f.listingId) === String(listingId));
        setIsFavorited(found);
      })
      .catch(() => {});
    return () => { cancelled = true; };
  }, [listingId]);

  const handleToggleFavorite = async () => {
    const token = typeof window !== 'undefined' ? localStorage.getItem('accessToken') : null;
    if (!token) {
      alert('로그인이 필요합니다.');
      router.push('/auth/login');
      return;
    }
    if (favoriteLoading) return;
    setFavoriteLoading(true);
    try {
      if (isFavorited) {
        await removeFavorite(listingId);
        setIsFavorited(false);
      } else {
        await addFavorite(listingId);
        setIsFavorited(true);
      }
    } catch (err) {
      console.error('관심 매물 처리 실패:', err);
    } finally {
      setFavoriteLoading(false);
    }
  };

  const images = detail?.images || [];

  const handlePrevImage = () => {
    setCurrentImageIndex((prev) => (prev === 0 ? images.length - 1 : prev - 1));
  };

  const handleNextImage = () => {
    setCurrentImageIndex((prev) => (prev === images.length - 1 ? 0 : prev + 1));
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="flex flex-col items-center gap-4">
          <svg className="animate-spin h-10 w-10 text-blue-500" viewBox="0 0 24 24" fill="none">
            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
          </svg>
          <p className="text-gray-500 font-medium">매물 정보를 불러오는 중...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="bg-white rounded-2xl shadow-sm border border-gray-200 p-10 max-w-md text-center">
          <div className="w-16 h-16 bg-red-50 rounded-full flex items-center justify-center mx-auto mb-4">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" className="text-red-500">
              <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="2" />
              <path d="M12 8v4m0 4h.01" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
            </svg>
          </div>
          <h2 className="text-xl font-bold text-gray-900 mb-2">매물을 찾을 수 없습니다</h2>
          <p className="text-gray-500 mb-6">{error}</p>
          <button
            onClick={() => router.push('/search/listing')}
            className="px-6 py-3 bg-blue-600 text-white rounded-xl font-semibold hover:bg-blue-700 transition-colors"
          >
            매물 목록으로 돌아가기
          </button>
        </div>
      </div>
    );
  }

  const badge = detail ? getBadge(detail.tradeType, detail.rentType) : null;

  return (
    <div className="min-h-screen bg-gray-50">
      {/* 풀스크린 이미지 뷰어 */}
      {showFullscreen && images.length > 0 && (
        <div className="fixed inset-0 z-50 bg-black/90 flex items-center justify-center">
          <button
            onClick={() => setShowFullscreen(false)}
            className="absolute top-4 right-4 z-10 w-10 h-10 bg-white/20 hover:bg-white/30 rounded-full flex items-center justify-center text-white transition-colors"
          >
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
              <path d="M18 6L6 18M6 6l12 12" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
            </svg>
          </button>

          {images.length > 1 && (
            <>
              <button
                onClick={handlePrevImage}
                className="absolute left-4 z-10 w-12 h-12 bg-white/20 hover:bg-white/30 rounded-full flex items-center justify-center text-white transition-colors"
              >
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
                  <path d="M15 18l-6-6 6-6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
              </button>
              <button
                onClick={handleNextImage}
                className="absolute right-4 z-10 w-12 h-12 bg-white/20 hover:bg-white/30 rounded-full flex items-center justify-center text-white transition-colors"
              >
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
                  <path d="M9 18l6-6-6-6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
              </button>
            </>
          )}

          <img
            src={images[currentImageIndex]?.url}
            alt={`이미지 ${currentImageIndex + 1}`}
            className="max-w-[90vw] max-h-[85vh] object-contain"
          />

          <div className="absolute bottom-6 left-1/2 -translate-x-1/2 px-4 py-2 bg-black/60 rounded-full text-white text-sm">
            {currentImageIndex + 1} / {images.length}
          </div>
        </div>
      )}

      {/* 상단 네비게이션 */}
      <div className="bg-white border-b border-gray-200 sticky top-0 z-40">
        <div className="max-w-5xl mx-auto px-4 h-14 flex items-center gap-3">
          <button
            onClick={() => router.back()}
            className="flex items-center gap-1.5 text-gray-600 hover:text-gray-900 transition-colors"
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
              <path d="M15 18l-6-6 6-6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
            <span className="text-sm font-medium">뒤로가기</span>
          </button>
          <div className="h-4 w-px bg-gray-300" />
          <span className="text-sm text-gray-500 truncate">{detail?.title || '매물 상세'}</span>
        </div>
      </div>

      <div className="max-w-5xl mx-auto px-4 py-8">
        <div className="grid grid-cols-1 lg:grid-cols-5 gap-8">
          {/* 좌측: 이미지 영역 */}
          <div className="lg:col-span-3">
            {/* 메인 이미지 */}
            <div className="relative bg-gray-100 rounded-2xl overflow-hidden aspect-[4/3]">
              {images.length > 0 ? (
                <>
                  <img
                    src={images[currentImageIndex]?.url}
                    alt={`${detail?.title} 이미지 ${currentImageIndex + 1}`}
                    className="w-full h-full object-cover cursor-pointer"
                    onClick={() => setShowFullscreen(true)}
                  />

                  {images.length > 1 && (
                    <>
                      <button
                        onClick={handlePrevImage}
                        className="absolute left-3 top-1/2 -translate-y-1/2 w-10 h-10 bg-black/40 hover:bg-black/60 rounded-full flex items-center justify-center text-white transition-colors"
                      >
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                          <path d="M15 18l-6-6 6-6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                        </svg>
                      </button>
                      <button
                        onClick={handleNextImage}
                        className="absolute right-3 top-1/2 -translate-y-1/2 w-10 h-10 bg-black/40 hover:bg-black/60 rounded-full flex items-center justify-center text-white transition-colors"
                      >
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                          <path d="M9 18l6-6-6-6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                        </svg>
                      </button>
                    </>
                  )}

                  <div className="absolute bottom-3 right-3 px-3 py-1.5 bg-black/60 rounded-lg text-white text-xs font-medium">
                    {currentImageIndex + 1} / {images.length}
                  </div>

                  <button
                    onClick={() => setShowFullscreen(true)}
                    className="absolute top-3 right-3 w-9 h-9 bg-black/40 hover:bg-black/60 rounded-lg flex items-center justify-center text-white transition-colors"
                  >
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                      <path d="M15 3h6v6M9 21H3v-6M21 3l-7 7M3 21l7-7" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                    </svg>
                  </button>
                </>
              ) : (
                <div className="w-full h-full flex flex-col items-center justify-center text-gray-300">
                  <svg width="64" height="64" viewBox="0 0 24 24" fill="none">
                    <path d="M3 21V7l9-4 9 4v14" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
                    <path d="M9 21V11h6v10" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
                  </svg>
                  <p className="mt-2 text-sm">등록된 이미지가 없습니다</p>
                </div>
              )}
            </div>

            {/* 썸네일 목록 */}
            {images.length > 1 && (
              <div className="mt-3 flex gap-2 overflow-x-auto pb-2">
                {images.map((img, idx) => (
                  <button
                    key={img.id}
                    onClick={() => setCurrentImageIndex(idx)}
                    className={`shrink-0 w-20 h-20 rounded-xl overflow-hidden border-2 transition-all ${
                      idx === currentImageIndex
                        ? 'border-blue-500 ring-2 ring-blue-200'
                        : 'border-transparent hover:border-gray-300'
                    }`}
                  >
                    <img
                      src={img.url}
                      alt={`썸네일 ${idx + 1}`}
                      className="w-full h-full object-cover"
                    />
                  </button>
                ))}
              </div>
            )}
          </div>

          {/* 우측: 매물 정보 */}
          <div className="lg:col-span-2 space-y-6">
            {/* 아파트명 + 거래유형 뱃지 + 관심 매물 버튼 */}
            <div className="flex items-start justify-between gap-3">
              <div className="min-w-0">
                {badge && (
                  <span className={`inline-block px-3 py-1 rounded-lg text-xs font-semibold ${badge.bg} ${badge.text} mb-3`}>
                    {badge.label}
                  </span>
                )}
                <h1 className="text-2xl font-bold text-gray-900">
                  {detail?.title || '매물 상세'}
                </h1>
                {detail?.jibunAddress && (
                  <p className="text-sm text-gray-500 mt-1.5 flex items-center gap-1.5">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" className="text-gray-400 shrink-0">
                      <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 1118 0z" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                      <circle cx="12" cy="10" r="3" stroke="currentColor" strokeWidth="2" />
                    </svg>
                    {detail.jibunAddress}
                  </p>
                )}
                {detail?.buildYear && (
                  <p className="text-sm text-gray-500 mt-1">{detail.buildYear}년 건축</p>
                )}
              </div>
              <button
                onClick={handleToggleFavorite}
                disabled={favoriteLoading}
                className={`shrink-0 w-11 h-11 rounded-full flex items-center justify-center transition-all ${
                  isFavorited
                    ? 'bg-red-50 text-red-500 hover:bg-red-100'
                    : 'bg-gray-100 text-gray-400 hover:bg-gray-200 hover:text-gray-500'
                } ${favoriteLoading ? 'opacity-50 cursor-not-allowed' : ''}`}
                title={isFavorited ? '관심 매물 해제' : '관심 매물 등록'}
              >
                <svg width="22" height="22" viewBox="0 0 24 24" fill={isFavorited ? 'currentColor' : 'none'}>
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

            {/* 가격 정보 */}
            {detail && (
              <div className="bg-blue-50 rounded-2xl p-5">
                <p className="text-xs font-medium text-blue-600 mb-1">
                  {detail.tradeType === 'SALE' ? '매매가' : detail.rentType === 'MONTHLY' ? '보증금 / 월세' : '전세가'}
                </p>
                <p className="text-2xl font-bold text-gray-900">
                  {getPriceText(detail)}
                </p>
              </div>
            )}

            {/* 상세 스펙 */}
            {detail && (
              <div className="bg-white rounded-2xl border border-gray-200 divide-y divide-gray-100">
                {detail.exclusiveArea && (
                  <div className="flex items-center justify-between px-5 py-4">
                    <span className="text-sm text-gray-500 flex items-center gap-2">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" className="text-gray-400">
                        <rect x="3" y="3" width="18" height="18" rx="2" stroke="currentColor" strokeWidth="2" />
                      </svg>
                      전용면적
                    </span>
                    <span className="text-sm font-semibold text-gray-900">
                      {Number(detail.exclusiveArea).toFixed(1)}m²
                      <span className="text-gray-400 font-normal ml-1">
                        ({(Number(detail.exclusiveArea) * 0.3025).toFixed(1)}평)
                      </span>
                    </span>
                  </div>
                )}
                {detail.floor != null && (
                  <div className="flex items-center justify-between px-5 py-4">
                    <span className="text-sm text-gray-500 flex items-center gap-2">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" className="text-gray-400">
                        <path d="M3 21h18M5 21V7l7-4 7 4v14" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                      </svg>
                      층수
                    </span>
                    <span className="text-sm font-semibold text-gray-900">{detail.floor}층</span>
                  </div>
                )}
                <div className="flex items-center justify-between px-5 py-4">
                  <span className="text-sm text-gray-500 flex items-center gap-2">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" className="text-gray-400">
                      <rect x="3" y="4" width="18" height="18" rx="2" stroke="currentColor" strokeWidth="2" />
                      <path d="M16 2v4M8 2v4M3 10h18" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                    </svg>
                    등록일
                  </span>
                  <span className="text-sm font-semibold text-gray-900">{formatDate(detail.createdAt)}</span>
                </div>
                {detail.tradeType === 'SALE' && detail.salePrice && (
                  <div className="flex items-center justify-between px-5 py-4">
                    <span className="text-sm text-gray-500 flex items-center gap-2">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" className="text-gray-400">
                        <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="2" />
                        <path d="M12 6v12M8 10h8M8 14h8" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                      </svg>
                      매매가
                    </span>
                    <span className="text-sm font-semibold text-gray-900">{formatPrice(detail.salePrice)}</span>
                  </div>
                )}
                {detail.tradeType === 'RENT' && detail.deposit && (
                  <div className="flex items-center justify-between px-5 py-4">
                    <span className="text-sm text-gray-500 flex items-center gap-2">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" className="text-gray-400">
                        <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="2" />
                        <path d="M12 6v12M8 10h8M8 14h8" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                      </svg>
                      보증금
                    </span>
                    <span className="text-sm font-semibold text-gray-900">{formatPrice(detail.deposit)}</span>
                  </div>
                )}
                {detail.tradeType === 'RENT' && detail.monthlyRent > 0 && (
                  <div className="flex items-center justify-between px-5 py-4">
                    <span className="text-sm text-gray-500 flex items-center gap-2">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" className="text-gray-400">
                        <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="2" />
                        <path d="M12 6v12M8 10h8M8 14h8" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                      </svg>
                      월세
                    </span>
                    <span className="text-sm font-semibold text-gray-900">{formatPrice(detail.monthlyRent)}원</span>
                  </div>
                )}
              </div>
            )}

            {/* 연락처 */}
            {detail?.contactPhone && (
              <div className="bg-white rounded-2xl border border-gray-200 p-5">
                <h3 className="text-sm font-semibold text-gray-900 mb-3 flex items-center gap-2">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" className="text-gray-400">
                    <path d="M22 16.92v3a2 2 0 01-2.18 2 19.79 19.79 0 01-8.63-3.07 19.5 19.5 0 01-6-6 19.79 19.79 0 01-3.07-8.67A2 2 0 014.11 2h3a2 2 0 012 1.72c.127.96.361 1.903.7 2.81a2 2 0 01-.45 2.11L8.09 9.91a16 16 0 006 6l1.27-1.27a2 2 0 012.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0122 16.92z" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                  </svg>
                  연락처
                </h3>
                <a
                  href={`tel:${detail.contactPhone}`}
                  className="inline-flex items-center gap-2 px-4 py-2.5 bg-blue-600 text-white rounded-xl text-sm font-medium hover:bg-blue-700 transition-colors"
                >
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                    <path d="M22 16.92v3a2 2 0 01-2.18 2 19.79 19.79 0 01-8.63-3.07 19.5 19.5 0 01-6-6 19.79 19.79 0 01-3.07-8.67A2 2 0 014.11 2h3a2 2 0 012 1.72c.127.96.361 1.903.7 2.81a2 2 0 01-.45 2.11L8.09 9.91a16 16 0 006 6l1.27-1.27a2 2 0 012.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0122 16.92z" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                  </svg>
                  {detail.contactPhone}
                </a>
              </div>
            )}

            {/* 매물 설명 */}
            {detail?.description && (
              <div className="bg-white rounded-2xl border border-gray-200 p-5">
                <h3 className="text-sm font-semibold text-gray-900 mb-3 flex items-center gap-2">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" className="text-gray-400">
                    <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8l-6-6z" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                    <path d="M14 2v6h6M16 13H8M16 17H8M10 9H8" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                  </svg>
                  매물 설명
                </h3>
                <p className="text-sm text-gray-600 leading-relaxed whitespace-pre-wrap">{detail.description}</p>
              </div>
            )}

            {/* 목록으로 돌아가기 */}
            <button
              onClick={() => router.push('/search/listing')}
              className="w-full py-3.5 border border-gray-300 rounded-xl text-sm font-semibold text-gray-700 hover:bg-gray-50 transition-colors"
            >
              매물 목록으로 돌아가기
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
