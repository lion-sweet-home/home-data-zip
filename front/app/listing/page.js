'use client';

import { useState, useEffect, useCallback } from 'react';
import Link from 'next/link';
import { getMyListings, deleteListing } from '../api/listing';
import ListingCard from './components/card';
import ListingFormModal from './components/listing_form_modal';

const TAB_ALL = 'ALL';
const TAB_SALE = 'SALE';
const TAB_RENT = 'RENT';

const TAB_OPTIONS = [
  { value: TAB_ALL, label: '전체' },
  { value: TAB_SALE, label: '매매 매물' },
  { value: TAB_RENT, label: '전월세 매물' },
];

export default function ListingPage() {
  const [listings, setListings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState(TAB_ALL);
  const [modalOpen, setModalOpen] = useState(false);

  const fetchListings = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getMyListings();
      setListings(Array.isArray(data) ? data : []);
    } catch (e) {
      setListings([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchListings();
  }, [fetchListings]);

  const filteredListings =
    activeTab === TAB_SALE
      ? listings.filter((l) => l.tradeType === 'SALE')
      : activeTab === TAB_RENT
        ? listings.filter((l) => l.tradeType === 'RENT')
        : listings;

  const handleDelete = async (listing) => {
    if (!listing?.listingId) return;
    if (!confirm('이 매물을 삭제하시겠습니까?')) return;
    try {
      await deleteListing(listing.listingId);
      await fetchListings();
    } catch (e) {
      alert('삭제에 실패했습니다.');
    }
  };

  const handleEdit = (listing) => {
    // 백엔드 수정 API 미구현 시 안내
    alert('수정 기능은 준비 중입니다.');
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-4xl mx-auto px-4 sm:px-6 py-8">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
          <div className="flex items-center gap-3">
            <Link
              href="/"
              className="text-sm font-medium text-blue-600 hover:text-blue-700"
            >
              ← 메인으로
            </Link>
            <h1 className="text-2xl font-bold text-gray-900">매물 관리</h1>
          </div>
          <button
            type="button"
            onClick={() => setModalOpen(true)}
            className="inline-flex items-center justify-center gap-2 px-4 py-2.5 rounded-lg bg-blue-600 text-white text-sm font-medium hover:bg-blue-700 transition-colors shrink-0"
          >
            <span className="text-lg leading-none">+</span>
            매물 등록
          </button>
        </div>

        {/* 탭: 전체 / 매매 / 전월세 */}
        <div className="flex gap-2 mb-6">
          {TAB_OPTIONS.map((tab) => (
            <button
              key={tab.value}
              type="button"
              onClick={() => setActiveTab(tab.value)}
              className={`px-4 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                activeTab === tab.value
                  ? 'bg-blue-600 text-white'
                  : 'bg-white text-gray-700 border border-gray-200 hover:bg-gray-50'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {loading ? (
          <div className="flex justify-center py-16">
            <div className="w-8 h-8 border-2 border-blue-600 border-t-transparent rounded-full animate-spin" />
          </div>
        ) : filteredListings.length === 0 ? (
          <div className="bg-white border border-gray-200 rounded-2xl p-12 text-center text-gray-500">
            {listings.length === 0
              ? '등록된 매물이 없습니다. 매물 등록 버튼으로 등록해보세요.'
              : '해당 유형의 매물이 없습니다.'}
          </div>
        ) : (
          <ul className="space-y-4">
            {filteredListings.map((listing) => (
              <li key={listing.listingId}>
                <ListingCard
                  listing={listing}
                  variant="manage"
                  isDeleted={listing.status === 'DELETED'}
                  onEdit={handleEdit}
                  onDelete={handleDelete}
                />
              </li>
            ))}
          </ul>
        )}
      </div>

      <ListingFormModal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        onSuccess={fetchListings}
      />
    </div>
  );
}
