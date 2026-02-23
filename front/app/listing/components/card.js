'use client';

import Link from 'next/link';

/**
 * 가격 포맷 (만원 단위 → 억/만 표기)
 */
function formatPrice(value) {
  if (value == null || !Number.isFinite(Number(value))) return '-';
  const n = Number(value);
  const manwon = Math.floor(n / 10000);
  const eok = Math.floor(manwon / 10000);
  const rest = manwon % 10000;
  if (eok > 0 && rest > 0) return `${eok}억 ${rest.toLocaleString()}만원`;
  if (eok > 0) return `${eok}억원`;
  return `${manwon.toLocaleString()}만원`;
}

/**
 * 매물 카드
 * @param {object} props.listing - 내 매물 또는 관심 매물 한 건 (MyListingResponse 형태)
 * @param {'manage'|'favorite'} props.variant - manage: 수정/삭제 버튼, favorite: 상세보기/연락하기 버튼
 * @param {function} [props.onEdit] - 수정 클릭 (variant=manage)
 * @param {function} [props.onDelete] - 삭제 클릭 (variant=manage)
 */
export default function ListingCard({ listing, variant = 'manage', isDeleted = false, onEdit, onDelete }) {
  if (!listing) return null;

  const {
    listingId,
    apartmentName,
    tradeType,
    exclusiveArea,
    floor,
    salePrice,
    deposit,
    monthlyRent,
    description,
    createdAt,
    mainImageUrl,
    contactPhone,
    regionName,
    nickname,
  } = listing;

  const isSale = tradeType === 'SALE';
  const tradeLabel = isSale ? '매매' : '전/월세';
  const priceText = isSale
    ? formatPrice(salePrice)
    : `${formatPrice(deposit)}${monthlyRent != null && monthlyRent > 0 ? ` / 월세 ${formatPrice(monthlyRent)}` : ''}`;
  const areaText = exclusiveArea != null ? `${Math.round(exclusiveArea)}㎡` : '-';
  const floorText = floor != null ? `${floor}층` : '-';
  const dateText =
    createdAt != null
      ? new Date(createdAt).toLocaleDateString('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit' }).replace(/\. /g, '-').replace('.', '')
      : '-';
  const displayNickname = nickname ?? '등록자';
  const displayRegion = regionName ?? '-';

  return (
    <article className="bg-white border border-gray-200 rounded-2xl overflow-hidden shadow-sm hover:shadow-md transition-shadow">
      <div className="flex flex-col sm:flex-row">
        {/* 썸네일 */}
        <div className="w-full sm:w-40 h-36 sm:h-auto shrink-0 bg-gray-100">
          {mainImageUrl ? (
            <img
              src={mainImageUrl}
              alt={apartmentName}
              className="w-full h-full object-cover"
            />
          ) : (
            <div className="w-full h-full flex items-center justify-center text-gray-400">
              <svg className="w-12 h-12" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
              </svg>
            </div>
          )}
        </div>

        <div className="flex-1 p-5 flex flex-col">
          <div className="flex items-start justify-between gap-2">
            <h3 className="text-lg font-bold text-gray-900 truncate">{apartmentName ?? '-'}</h3>
            <span
              className={`shrink-0 px-2 py-0.5 rounded text-xs font-medium ${
                isSale ? 'bg-blue-100 text-blue-700' : 'bg-amber-100 text-amber-700'
              }`}
            >
              {tradeLabel}
            </span>
          </div>

          <div className="mt-2 space-y-1.5 text-sm text-gray-600">
            <div className="flex items-center gap-1.5">
              <svg className="w-4 h-4 text-gray-400 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
              </svg>
              <span>{displayRegion}</span>
            </div>
            <div className="flex items-center gap-1.5">
              <svg className="w-4 h-4 text-gray-400 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
              </svg>
              <span>{areaText} · {floorText}</span>
            </div>
            <div className="flex items-center gap-1.5">
              <svg className="w-4 h-4 text-gray-400 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <span>{priceText}</span>
            </div>
            <div className="flex items-center gap-1.5">
              <svg className="w-4 h-4 text-gray-400 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
              </svg>
              <span>{displayNickname}</span>
            </div>
            <div className="flex items-center gap-1.5">
              <svg className="w-4 h-4 text-gray-400 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
              </svg>
              <span>등록일: {dateText}</span>
            </div>
          </div>

          {description && (
            <p className="mt-3 text-sm text-gray-500 line-clamp-2">{description}</p>
          )}

          <div className="mt-4 flex flex-wrap gap-2 justify-end">
            {variant === 'manage' && (
              isDeleted ? (
                <button
                  type="button"
                  disabled
                  className="px-4 py-2 rounded-lg bg-gray-400 text-white text-sm font-medium cursor-not-allowed"
                >
                  삭제된 매물
                </button>
              ) : (
                <>
                  <button
                    type="button"
                    onClick={() => onEdit?.(listing)}
                    className="px-4 py-2 rounded-lg bg-blue-600 text-white text-sm font-medium hover:bg-blue-700 transition-colors"
                  >
                    수정
                  </button>
                  <button
                    type="button"
                    onClick={() => onDelete?.(listing)}
                    className="px-4 py-2 rounded-lg bg-red-600 text-white text-sm font-medium hover:bg-red-700 transition-colors"
                  >
                    삭제
                  </button>
                </>
              )
            )}
            {variant === 'favorite' && (
              <>
                <Link
                  href={`/listing/${listingId}`}
                  className="inline-block px-4 py-2 rounded-lg bg-blue-600 text-white text-sm font-medium hover:bg-blue-700 transition-colors"
                >
                  상세보기
                </Link>
                {contactPhone && (
                  <a
                    href={`tel:${contactPhone}`}
                    className="inline-block px-4 py-2 rounded-lg bg-green-600 text-white text-sm font-medium hover:bg-green-700 transition-colors"
                  >
                    연락하기
                  </a>
                )}
              </>
            )}
          </div>
        </div>
      </div>
    </article>
  );
}
