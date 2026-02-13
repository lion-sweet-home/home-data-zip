'use client';

import { useMemo, useRef, useState } from 'react';
import { searchApartmentsByName } from '../api/apartment';

function formatManwon(manwon) {
  const n = Number(manwon);
  if (!Number.isFinite(n) || n <= 0) return '-';

  const eok = Math.floor(n / 10000);
  const rest = n % 10000;
  if (eok > 0 && rest > 0) return `${eok}억 ${rest.toLocaleString()}만`;
  if (eok > 0) return `${eok}억`;
  return `${n.toLocaleString()}만`;
}

function formatRate(rate) {
  const n = Number(rate);
  if (!Number.isFinite(n)) return { text: '-', className: 'text-gray-500' };
  const text = `${n > 0 ? '+' : ''}${n.toFixed(1)}%`;
  if (n > 0) return { text, className: 'text-emerald-600' };
  if (n < 0) return { text, className: 'text-red-600' };
  return { text, className: 'text-gray-600' };
}

export default function ApartmentSearch() {
  const [aptKeyword, setAptKeyword] = useState('');
  const [aptSearching, setAptSearching] = useState(false);
  const [aptSearchResults, setAptSearchResults] = useState([]);
  const [aptHasSearched, setAptHasSearched] = useState(false);
  const aptSearchSeqRef = useRef(0);

  const canSearch = useMemo(() => aptKeyword.trim().length > 0, [aptKeyword]);

  const handleAptResultClick = () => {
    // TODO: 클릭 시 상세/지도 이동 연결 (요청대로 현재는 미구현)
  };

  const handleAptSearch = async () => {
    const keyword = aptKeyword.trim();
    if (!keyword) return;

    // 검색을 연속으로 수행할 때, 이전 요청 응답이 늦게 도착해
    // 최신 결과를 덮어쓰는 문제(race condition) 방지용 시퀀스
    const seq = ++aptSearchSeqRef.current;

    setAptHasSearched(true);
    setAptSearching(true);
    // 새 검색 시작 시 기존 결과를 먼저 비워서 "이전 결과가 잠깐 보이는" 현상 방지
    setAptSearchResults([]);
    try {
      const res = await searchApartmentsByName(keyword);
      const list = Array.isArray(res) ? res : [];
      // 최신 검색이 아니면 무시
      if (aptSearchSeqRef.current !== seq) return;
      setAptSearchResults(list);
    } catch (e) {
      if (aptSearchSeqRef.current !== seq) return;
      setAptSearchResults([]);
    } finally {
      if (aptSearchSeqRef.current === seq) setAptSearching(false);
    }
  };

  return (
    <div className="mb-6 bg-white border border-gray-200 rounded-2xl p-5">
      <div className="text-lg font-semibold text-gray-900 mb-3">아파트 검색</div>
      <div className="flex gap-3">
        <div className="flex-1">
          <div className="relative">
            <div className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                <path
                  d="M10 18a8 8 0 1 1 0-16 8 8 0 0 1 0 16Zm11 3-6-6"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeLinecap="round"
                />
              </svg>
            </div>
            <input
              value={aptKeyword}
              onChange={(e) => setAptKeyword(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') handleAptSearch();
              }}
              placeholder="아파트명 입력"
              className="w-full pl-10 pr-3 py-3 border border-gray-300 rounded-xl text-sm outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-gray-900 placeholder:text-gray-500"
            />
          </div>
        </div>
        <button
          type="button"
          onClick={handleAptSearch}
          disabled={!canSearch || aptSearching}
          className={`px-6 py-3 rounded-xl text-sm font-semibold transition-colors ${
            canSearch && !aptSearching
              ? 'bg-blue-600 text-white hover:bg-blue-700'
              : 'bg-gray-200 text-gray-500 cursor-not-allowed'
          }`}
        >
          {aptSearching ? '검색중...' : '검색'}
        </button>
      </div>

      {/* 검색 결과 */}
      {aptHasSearched && (
        <div className="mt-4 border border-gray-200 rounded-xl overflow-hidden">
          <div className="px-4 py-2 bg-gray-50 border-b border-gray-200 text-xs text-gray-600">
            검색 결과 {aptSearchResults.length}건
          </div>
          {aptSearchResults.length === 0 ? (
            <div className="px-4 py-4 text-sm text-gray-500">검색 결과가 없습니다.</div>
          ) : (
            <div className="max-h-60 overflow-y-auto divide-y">
              {aptSearchResults.slice(0, 20).map((item, idx) => {
                const avg = item?.avgDealAmount ?? item?.AvgDealAmount;
                const rate = formatRate(item?.priceChangeRate);
                const count = item?.tradeCount ?? 0;
                return (
                  <button
                    key={item?.aptId ?? idx}
                    type="button"
                    onClick={() => handleAptResultClick(item)}
                    className="w-full text-left px-4 py-3 hover:bg-gray-50"
                  >
                    <div className="flex items-start justify-between gap-3">
                      <div className="min-w-0">
                        <div className="font-medium text-gray-900 truncate">{item?.aptName || '아파트'}</div>
                        <div className="text-xs text-gray-600 mt-1 truncate">{item?.gu || '-'}</div>
                      </div>
                      <div className="text-right flex-shrink-0">
                        <div className="text-xs text-gray-600">
                          평균 {formatManwon(avg)}원 · {count}건
                        </div>
                        <div className={`text-xs font-semibold ${rate.className}`}>전월 대비 {rate.text}</div>
                      </div>
                    </div>
                  </button>
                );
              })}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

