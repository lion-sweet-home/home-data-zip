'use client';

import { useState, useEffect, useCallback } from 'react';
import { getSidoList, getGugunList, getDongList } from '../../api/region';
import { searchApartmentsByName, getAptAreaTypes } from '../../api/apartment';
import { createListing, uploadListingImageTemp } from '../../api/listing';

const TRADE_SALE = 'SALE';
const TRADE_RENT = 'RENT';

/**
 * 매물 등록 모달
 * - 등록자 유형 제거
 * - 지역 선택 후 아파트명 검색 (선택한 구/군과 일치하는 결과만 표시)
 * - 전/월세 시 가격: 보증금(좌), 월세(우)
 */
export default function ListingFormModal({ isOpen, onClose, onSuccess }) {
  const [sidoList, setSidoList] = useState([]);
  const [gugunList, setGugunList] = useState([]);
  const [dongList, setDongList] = useState([]);
  const [selectedSido, setSelectedSido] = useState('');
  const [selectedGugun, setSelectedGugun] = useState('');
  const [selectedDong, setSelectedDong] = useState('');
  const [aptKeyword, setAptKeyword] = useState('');
  const [aptSearchResults, setAptSearchResults] = useState([]);
  const [aptSearching, setAptSearching] = useState(false);
  const [selectedApt, setSelectedApt] = useState(null);
  const [areaOptions, setAreaOptions] = useState([]);
  const [areaOptionsLoading, setAreaOptionsLoading] = useState(false);

  const [tradeType, setTradeType] = useState(TRADE_SALE);
  const [exclusiveArea, setExclusiveArea] = useState('');
  const [floor, setFloor] = useState('');
  const [salePriceMan, setSalePriceMan] = useState('');
  const [depositMan, setDepositMan] = useState('');
  const [monthlyRentMan, setMonthlyRentMan] = useState('');
  const [contactPhone, setContactPhone] = useState('');
  const [description, setDescription] = useState('');

  const [uploadedImages, setUploadedImages] = useState([]);
  const [mainImageIndex, setMainImageIndex] = useState(0);
  const [imageUploading, setImageUploading] = useState(false);

  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const loadSido = useCallback(async () => {
    try {
      const list = await getSidoList();
      setSidoList(Array.isArray(list) ? list : []);
    } catch (e) {
      setSidoList([]);
    }
  }, []);

  useEffect(() => {
    if (isOpen) loadSido();
  }, [isOpen, loadSido]);

  useEffect(() => {
    if (!selectedSido) {
      setGugunList([]);
      setSelectedGugun('');
      setSelectedDong('');
      setDongList([]);
      return;
    }
    let cancelled = false;
    getGugunList(selectedSido).then((list) => {
      if (!cancelled) {
        setGugunList(Array.isArray(list) ? list : []);
        setSelectedGugun('');
        setSelectedDong('');
        setDongList([]);
      }
    }).catch(() => {
      if (!cancelled) setGugunList([]);
    });
    return () => { cancelled = true; };
  }, [selectedSido]);

  useEffect(() => {
    if (!selectedSido || !selectedGugun) {
      setDongList([]);
      setSelectedDong('');
      return;
    }
    let cancelled = false;
    getDongList(selectedSido, selectedGugun).then((list) => {
      if (!cancelled) {
        setDongList(Array.isArray(list) ? list : []);
        setSelectedDong('');
      }
    }).catch(() => {
      if (!cancelled) setDongList([]);
    });
    return () => { cancelled = true; };
  }, [selectedSido, selectedGugun]);

  const handleAptSearch = useCallback(async () => {
    const keyword = aptKeyword.trim();
    if (keyword.length < 2) {
      setError('아파트명을 2글자 이상 입력해주세요.');
      return;
    }
    setError('');
    setAptSearching(true);
    setAptSearchResults([]);
    try {
      const res = await searchApartmentsByName(keyword);
      const list = Array.isArray(res) ? res : [];
      const filtered = selectedGugun
        ? list.filter((item) => item.gu === selectedGugun)
        : list;
      // 동일 aptId 중복 제거 (API가 평형별로 여러 건 반환할 수 있음)
      const seen = new Set();
      const unique = filtered.filter((item) => {
        const id = item.aptId;
        if (seen.has(id)) return false;
        seen.add(id);
        return true;
      });
      setAptSearchResults(unique);
    } catch (e) {
      setAptSearchResults([]);
      setError('아파트 검색에 실패했습니다.');
    } finally {
      setAptSearching(false);
    }
  }, [aptKeyword, selectedGugun]);

  const resetForm = useCallback(() => {
    setTradeType(TRADE_SALE);
    setSelectedSido('');
    setSelectedGugun('');
    setSelectedDong('');
    setAptKeyword('');
    setAptSearchResults([]);
    setSelectedApt(null);
    setAreaOptions([]);
    setAreaOptionsLoading(false);
    setExclusiveArea('');
    setFloor('');
    setSalePriceMan('');
    setDepositMan('');
    setMonthlyRentMan('');
    setContactPhone('');
    setDescription('');
    setUploadedImages([]);
    setMainImageIndex(0);
    setError('');
  }, []);

  const handleImageSelect = async (e) => {
    const files = e.target.files ? Array.from(e.target.files) : [];
    if (files.length === 0) return;
    setImageUploading(true);
    setError('');
    try {
      const newItems = [];
      for (const file of files) {
        if (!file.type.startsWith('image/')) continue;
        const res = await uploadListingImageTemp(file);
        if (res?.key) {
          newItems.push({ key: res.key, url: res.url || '', originalName: file.name });
        }
      }
      setUploadedImages((prev) => {
        const next = [...prev, ...newItems];
        return next;
      });
    } catch (err) {
      setError('사진 업로드에 실패했습니다.');
    } finally {
      setImageUploading(false);
      e.target.value = '';
    }
  };

  const removeImage = (index) => {
    setUploadedImages((prev) => {
      const next = prev.filter((_, i) => i !== index);
      return next;
    });
    setMainImageIndex((prev) => {
      if (index === prev) return 0;
      if (index < prev) return prev - 1;
      return prev;
    });
  };

  const setAsMainImage = (index) => {
    setMainImageIndex(index);
  };

  const handleClose = useCallback(() => {
    resetForm();
    onClose?.();
  }, [onClose, resetForm]);

  const manToWon = (man) => {
    const n = Number(man);
    if (!Number.isFinite(n) || n < 0) return null;
    return Math.round(n * 10000);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!selectedApt?.aptId) {
      setError('아파트를 선택해주세요.');
      return;
    }
    const area = Number(exclusiveArea);
    if (!Number.isFinite(area) || area <= 0) {
      setError('전용면적을 입력해주세요.');
      return;
    }
    const floorNum = Number(floor);
    if (!Number.isFinite(floorNum) || floorNum < 0) {
      setError('층수를 입력해주세요.');
      return;
    }

    if (tradeType === TRADE_SALE) {
      const price = manToWon(salePriceMan);
      if (price == null || price <= 0) {
        setError('매매가를 입력해주세요.');
        return;
      }
    } else {
      const dep = manToWon(depositMan);
      if (dep == null || dep < 0) {
        setError('보증금을 입력해주세요.');
        return;
      }
      const monthly = manToWon(monthlyRentMan);
      if (monthly == null || monthly < 0) {
        setError('월세를 입력해주세요. (전세인 경우 0)');
        return;
      }
    }

    setSubmitting(true);
    try {
      const imageTempKeys = uploadedImages.map((img) => img.key);
      const imageOriginalNames = uploadedImages.map((img) => img.originalName);
      let mainIndex = Math.min(mainImageIndex, imageTempKeys.length - 1);
      if (mainIndex < 0 && imageTempKeys.length > 0) mainIndex = 0;

      const body = {
        apartmentId: selectedApt.aptId,
        tradeType,
        exclusiveArea: area,
        floor: floorNum,
        contactPhone: contactPhone.trim() || null,
        description: description.trim() || null,
        imageTempKeys,
        imageOriginalNames,
        mainIndex,
      };
      if (tradeType === TRADE_SALE) {
        body.salePrice = manToWon(salePriceMan);
        body.deposit = null;
        body.monthlyRent = null;
      } else {
        body.salePrice = null;
        body.deposit = manToWon(depositMan);
        body.monthlyRent = Math.round(Number(monthlyRentMan) * 10000) || 0;
      }
      await createListing(body);
      onSuccess?.();
      handleClose();
    } catch (err) {
      const msg = err?.message || err?.response?.data?.message || '등록에 실패했습니다.';
      setError(msg);
    } finally {
      setSubmitting(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50" onClick={handleClose}>
      <div
        className="bg-white rounded-2xl shadow-xl max-w-lg w-full max-h-[90vh] overflow-y-auto"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="sticky top-0 bg-white border-b border-gray-200 px-6 py-4 flex items-center justify-between">
          <h2 className="text-xl font-bold text-gray-900">매물 등록</h2>
          <button
            type="button"
            onClick={handleClose}
            className="p-2 text-gray-500 hover:text-gray-700 rounded-lg hover:bg-gray-100"
            aria-label="닫기"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-5">
          {error && (
            <div className="p-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>
          )}

          {/* 거래 유형 */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">거래 유형</label>
            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => setTradeType(TRADE_SALE)}
                className={`flex-1 py-2.5 rounded-lg border text-sm font-medium transition-colors ${
                  tradeType === TRADE_SALE
                    ? 'bg-blue-600 text-white border-blue-600'
                    : 'bg-white text-gray-700 border-gray-300 hover:bg-gray-50'
                }`}
              >
                매매
              </button>
              <button
                type="button"
                onClick={() => setTradeType(TRADE_RENT)}
                className={`flex-1 py-2.5 rounded-lg border text-sm font-medium transition-colors ${
                  tradeType === TRADE_RENT
                    ? 'bg-blue-600 text-white border-blue-600'
                    : 'bg-white text-gray-700 border-gray-300 hover:bg-gray-50'
                }`}
              >
                전/월세
              </button>
            </div>
          </div>

          {/* 지역 선택 */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">지역 선택</label>
            <div className="grid grid-cols-3 gap-2">
              <select
                value={selectedSido}
                onChange={(e) => setSelectedSido(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-gray-900 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              >
                <option value="">시/도</option>
                {sidoList.filter(Boolean).map((s) => (
                  <option key={s} value={s}>{s}</option>
                ))}
              </select>
              <select
                value={selectedGugun}
                onChange={(e) => setSelectedGugun(e.target.value)}
                disabled={!selectedSido}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-gray-900 text-sm focus:ring-2 focus:ring-blue-500 disabled:bg-gray-100 disabled:text-gray-500"
              >
                <option value="">구/군</option>
                {gugunList.filter(Boolean).map((g) => (
                  <option key={g} value={g}>{g}</option>
                ))}
              </select>
              <select
                value={selectedDong}
                onChange={(e) => setSelectedDong(e.target.value)}
                disabled={!selectedGugun}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-gray-900 text-sm focus:ring-2 focus:ring-blue-500 disabled:bg-gray-100 disabled:text-gray-500"
              >
                <option value="">동</option>
                {dongList.filter(Boolean).map((d) => (
                  <option key={d} value={d}>{d}</option>
                ))}
              </select>
            </div>
          </div>

          {/* 아파트명 검색 (지역 선택 후 권장) */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">아파트명</label>
            <div className="flex gap-2">
              <input
                type="text"
                value={aptKeyword}
                onChange={(e) => setAptKeyword(e.target.value)}
                placeholder="아파트명 입력 후 검색"
                className="flex-1 px-3 py-2 border border-gray-300 rounded-lg text-gray-900 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              />
              <button
                type="button"
                onClick={handleAptSearch}
                disabled={aptSearching || aptKeyword.trim().length < 2}
                className="px-4 py-2 rounded-lg bg-gray-800 text-white text-sm font-medium hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {aptSearching ? '검색 중...' : '검색'}
              </button>
            </div>
            <p className="mt-1 text-xs text-gray-500">지역(구/군) 선택 후 검색하면 해당 지역 아파트만 표시됩니다.</p>
            {aptSearchResults.length > 0 && (
              <ul className="mt-2 border border-gray-200 rounded-lg divide-y max-h-40 overflow-y-auto">
                {aptSearchResults.map((apt) => (
                  <li key={apt.aptId}>
                    <button
                      type="button"
                      onClick={async () => {
                        setSelectedApt(apt);
                        setAptSearchResults([]);
                        setAptKeyword(apt.aptName ?? '');
                        setExclusiveArea('');
                        setAreaOptions([]);
                        setAreaOptionsLoading(true);
                        try {
                          const data = await getAptAreaTypes(apt.aptId);
                          setAreaOptions(Array.isArray(data?.options) ? data.options : []);
                        } catch {
                          setAreaOptions([]);
                        } finally {
                          setAreaOptionsLoading(false);
                        }
                      }}
                      className={`w-full text-left px-3 py-2 text-sm hover:bg-gray-50 ${
                        selectedApt?.aptId === apt.aptId ? 'bg-blue-50 text-blue-700 font-medium' : 'text-gray-900'
                      }`}
                    >
                      {apt.aptName} {apt.gu && `(${apt.gu})`}
                    </button>
                  </li>
                ))}
              </ul>
            )}
            {selectedApt && (
              <p className="mt-2 text-sm text-green-600 font-medium">선택: {selectedApt.aptName}</p>
            )}
          </div>

          {/* 전용면적, 층수 */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">전용면적(㎡)</label>
              <select
                value={exclusiveArea}
                onChange={(e) => setExclusiveArea(e.target.value)}
                disabled={!selectedApt || areaOptionsLoading}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-gray-900 text-sm focus:ring-2 focus:ring-blue-500 disabled:bg-gray-100 disabled:text-gray-500"
              >
                <option value="">
                  {!selectedApt
                    ? '아파트를 먼저 선택해주세요'
                    : areaOptionsLoading
                      ? '로딩 중...'
                      : areaOptions.length === 0
                        ? '해당 아파트의 전용면적 정보가 없습니다'
                        : '전용면적 선택'}
                </option>
                {areaOptions.map((opt) => (
                  <option key={opt.areaKey} value={opt.exclusive}>
                    {opt.exclusive}㎡
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">층수</label>
              <input
                type="number"
                min="0"
                value={floor}
                onChange={(e) => setFloor(e.target.value)}
                placeholder="10"
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-gray-900 text-sm focus:ring-2 focus:ring-blue-500"
              />
            </div>
          </div>

          {/* 가격: 매매 1개 / 전월세 보증금(좌) 월세(우) */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">가격 (만원)</label>
            {tradeType === TRADE_SALE ? (
              <input
                type="number"
                min="0"
                value={salePriceMan}
                onChange={(e) => setSalePriceMan(e.target.value)}
                placeholder="매매가 (만원)"
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-gray-900 text-sm focus:ring-2 focus:ring-blue-500"
              />
            ) : (
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs text-gray-500 mb-1">보증금 (만원)</label>
                  <input
                    type="number"
                    min="0"
                    value={depositMan}
                    onChange={(e) => setDepositMan(e.target.value)}
                    placeholder="0"
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg text-gray-900 text-sm focus:ring-2 focus:ring-blue-500"
                  />
                </div>
                <div>
                  <label className="block text-xs text-gray-500 mb-1">월세 (만원)</label>
                  <input
                    type="number"
                    min="0"
                    value={monthlyRentMan}
                    onChange={(e) => setMonthlyRentMan(e.target.value)}
                    placeholder="0 (전세 시 0)"
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg text-gray-900 text-sm focus:ring-2 focus:ring-blue-500"
                  />
                </div>
              </div>
            )}
          </div>

          {/* 연락처 */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">연락처</label>
            <input
              type="tel"
              value={contactPhone}
              onChange={(e) => setContactPhone(e.target.value)}
              placeholder="010-1234-5678"
              className="w-full px-3 py-2 border border-gray-300 rounded-lg text-gray-900 text-sm focus:ring-2 focus:ring-blue-500"
            />
          </div>

          {/* 사진 등록 (상세 설명 위) */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">사진 등록</label>
            <div className="flex flex-wrap gap-3">
              <label className="flex flex-col items-center justify-center w-24 h-24 rounded-lg border-2 border-dashed border-gray-300 text-gray-500 hover:border-blue-500 hover:bg-gray-50 cursor-pointer transition-colors">
                <input
                  type="file"
                  accept="image/*"
                  multiple
                  onChange={handleImageSelect}
                  disabled={imageUploading}
                  className="hidden"
                />
                {imageUploading ? (
                  <span className="text-xs">업로드 중...</span>
                ) : (
                  <>
                    <svg className="w-8 h-8 mb-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                    </svg>
                    <span className="text-xs">추가</span>
                  </>
                )}
              </label>
              {uploadedImages.map((img, index) => (
                <div key={`${img.key}-${index}`} className="relative w-24 h-24 rounded-lg overflow-hidden border border-gray-200 bg-gray-100 shrink-0">
                  {img.url ? (
                    <img src={img.url} alt="" className="w-full h-full object-cover" />
                  ) : (
                    <div className="w-full h-full flex items-center justify-center text-gray-400 text-xs">미리보기</div>
                  )}
                  <div className="absolute inset-x-0 bottom-0 flex gap-1 p-1 bg-black/50">
                    <button
                      type="button"
                      onClick={() => setAsMainImage(index)}
                      className={`flex-1 py-0.5 rounded text-xs text-white ${mainImageIndex === index ? 'bg-blue-600' : 'bg-gray-600 hover:bg-gray-500'}`}
                    >
                      대표
                    </button>
                    <button
                      type="button"
                      onClick={() => removeImage(index)}
                      className="p-0.5 rounded bg-red-600 text-white hover:bg-red-500"
                      aria-label="삭제"
                    >
                      <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                      </svg>
                    </button>
                  </div>
                </div>
              ))}
            </div>
            <p className="mt-1 text-xs text-gray-500">첫 번째 사진 또는 &#39;대표&#39;로 지정한 사진이 대표 이미지로 사용됩니다.</p>
          </div>

          {/* 상세 설명 */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">상세 설명</label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="매물에 대한 상세 설명을 입력하세요"
              rows={3}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg text-gray-900 text-sm focus:ring-2 focus:ring-blue-500 resize-none"
            />
          </div>

          <div className="flex gap-3 pt-2">
            <button
              type="button"
              onClick={handleClose}
              className="flex-1 py-2.5 rounded-lg border border-gray-300 text-gray-700 text-sm font-medium hover:bg-gray-50"
            >
              취소
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="flex-1 py-2.5 rounded-lg bg-blue-600 text-white text-sm font-medium hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {submitting ? '등록 중...' : '등록하기'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
