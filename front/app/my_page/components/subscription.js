'use client';

import Link from 'next/link';
import { useEffect, useMemo, useState } from 'react';
import { decodeAccessTokenPayload, normalizeRoles } from './_utils';
import { cancelAutoPay, getMySubscription } from '../../api/subscription';

function formatDate(value) {
  if (!value) return '-';
  try {
    const d = new Date(value);
    if (Number.isNaN(d.getTime())) return String(value);
    return d.toLocaleString();
  } catch {
    return String(value);
  }
}

function SubscriptionDetailModal({
  open,
  onClose,
  data,
  loading,
  error,
  onCancel,
  cancelLoading,
}) {
  if (!open) return null;

  const canCancel = !loading && !error && data?.status === 'ACTIVE';

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div className="w-full max-w-lg rounded-2xl bg-white shadow-xl">
        <div className="flex items-center justify-between border-b px-6 py-4">
          <div className="font-bold text-lg text-gray-900">구독 상세</div>
          <button
            onClick={onClose}
            className="rounded-lg px-3 py-1 text-sm text-gray-500 hover:bg-gray-100"
          >
            닫기
          </button>
        </div>

        <div className="px-6 py-5">
          {loading && <div className="text-sm text-gray-600">불러오는 중...</div>}

          {!loading && error && (
            <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
              {error}
            </div>
          )}

          {!loading && !error && (
            <>
              <div className="grid grid-cols-2 gap-3">
                <div className="rounded-xl border border-gray-200 p-4">
                  <div className="text-xs text-gray-500">상태</div>
                  <div className="mt-1 text-base font-semibold text-gray-900">
                    {data?.status ?? '-'}
                  </div>
                </div>

                <div className="rounded-xl border border-gray-200 p-4">
                  <div className="text-xs text-gray-500">자동결제 활성</div>
                  <div className="mt-1 text-base font-semibold text-gray-900">
                    {data?.isActive ? 'ON' : 'OFF'}
                  </div>
                </div>

                <div className="rounded-xl border border-gray-200 p-4">
                  <div className="text-xs text-gray-500">요금제</div>
                  <div className="mt-1 text-base font-semibold text-gray-900">
                    {data?.planName ?? '-'}
                  </div>
                </div>

                <div className="rounded-xl border border-gray-200 p-4">
                  <div className="text-xs text-gray-500">가격</div>
                  <div className="mt-1 text-base font-semibold text-gray-900">
                    {typeof data?.price === 'number'
                      ? `${data.price.toLocaleString()}원`
                      : '-'}
                  </div>
                </div>

                <div className="rounded-xl border border-gray-200 p-4">
                  <div className="text-xs text-gray-500">시작일</div>
                  <div className="mt-1 text-base font-semibold text-gray-900">
                    {formatDate(data?.startDate)}
                  </div>
                </div>

                <div className="rounded-xl border border-gray-200 p-4">
                  <div className="text-xs text-gray-500">종료일</div>
                  <div className="mt-1 text-base font-semibold text-gray-900">
                    {formatDate(data?.endDate)}
                  </div>
                </div>
              </div>

              <div className="mt-4 rounded-xl border border-gray-200 p-4">
                <div className="text-xs text-gray-500">카드 등록</div>
                <div className="mt-1 text-base font-semibold text-gray-900">
                  {data?.hasBillingKey ? '등록 완료' : '미등록'}
                </div>
              </div>

              {/*  모달 하단 버튼 영역 (회색 구독취소 + 닫기) */}
              <div className="mt-5 flex items-center justify-between gap-2">
              {/*  ACTIVE일 때만 "구독취소" 버튼 표시 */}
              {canCancel ? (
                <button
                  type="button"
                  onClick={onCancel}
                  disabled={cancelLoading}
                  className="rounded-xl border border-gray-200 bg-gray-100 px-4 py-2 text-sm font-semibold text-gray-700 hover:bg-gray-200 disabled:opacity-60"
                >
                  {cancelLoading ? '취소 중...' : '구독취소'}
                </button>
              ) : (
                // 왼쪽 공간 비우면 오른쪽 버튼이 붙어서 보기 싫으면 이거 둬
                <div />
              )}

              <button
                onClick={onClose}
                className="rounded-xl border border-gray-200 px-4 py-2 text-sm font-semibold text-gray-700 hover:bg-gray-50"
              >
                닫기
              </button>
            </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

export default function SubscriptionCard() {
  const [roles, setRoles] = useState([]);

  // ✅ 상세 모달 상태
  const [detailOpen, setDetailOpen] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState(null);
  const [detailData, setDetailData] = useState(null);

  // ✅ 모달 취소 로딩
  const [cancelLoading, setCancelLoading] = useState(false);

  useEffect(() => {
    const payload = decodeAccessTokenPayload();
    setRoles(normalizeRoles(payload?.roles));
  }, []);

  const isPremium = useMemo(
    () => roles.includes('SELLER') || roles.includes('PREMIUM'),
    [roles]
  );

  // 자세히 버튼 클릭 -> /subscriptions/me 호출 -> 모달 오픈
  async function handleOpenDetail() {
    setDetailOpen(true);
    setDetailLoading(true);
    setDetailError(null);

    try {
      const res = await getMySubscription();
      const data = res?.data ?? res;
      setDetailData(data);
    } catch (e) {
      console.error(e);
      setDetailError(e?.message || '구독 정보를 불러오지 못했습니다.');
    } finally {
      setDetailLoading(false);
    }
  }

  // ✅ 모달에서 구독취소
  async function handleCancelInModal() {
    if (cancelLoading) return;

    const ok = confirm(
      '구독을 취소할까요?\n취소해도 남은 기간(endDate)까지는 프리미엄 권한이 유지됩니다.'
    );
    if (!ok) return;

    setCancelLoading(true);
    try {
      await cancelAutoPay();
      alert('구독이 취소되었습니다. (남은 기간까지 권한 유지)');

      // 모달 내용 즉시 갱신
      const res = await getMySubscription();
      setDetailData(res?.data ?? res);
    } catch (e) {
      console.error(e);
      alert(e?.message || '구독 취소에 실패했습니다.');
    } finally {
      setCancelLoading(false);
    }
  }

  return (
    <>
      <div className="rounded-2xl p-6 shadow-sm border border-blue-100 bg-gradient-to-r from-blue-700 to-blue-500 text-white">
        <div className="flex items-start justify-between gap-4">
          <div>
            <h2 className="text-lg font-bold">구독 정보</h2>
            <div className="text-sm mt-2">
              현재 플랜:{' '}
              <span className="font-semibold">
                {isPremium ? '프리미엄' : '무료 체험'}
              </span>
            </div>
            <p className="text-sm text-blue-50 mt-2">
              프리미엄 구독으로 업그레이드하고 더 많은 기능을 사용해보세요!
            </p>
          </div>

          <span className="px-3 py-1 rounded-full bg-white/15 text-xs font-semibold">
            {isPremium ? 'Premium' : 'Free'}
          </span>
        </div>

        <div className="mt-5 flex items-center gap-3">
          <Link
            href="/subscription"
            className={`inline-flex items-center justify-center px-4 py-2.5 rounded-lg text-sm font-semibold ${
              isPremium
                ? 'bg-white/20 text-white cursor-not-allowed'
                : 'bg-white text-blue-700 hover:bg-blue-50'
            }`}
            onClick={(e) => {
              if (isPremium) e.preventDefault();
            }}
          >
            {isPremium ? '구독중' : '구독하기'}
          </Link>

          {/* 카드에서는 취소 버튼 제거하고 "자세히"에서만 처리 */}
          <button
            type="button"
            onClick={handleOpenDetail}
            className="px-4 py-2.5 rounded-lg bg-white/15 text-white text-sm font-semibold hover:bg-white/20"
          >
            자세히
          </button>
        </div>
      </div>

      <SubscriptionDetailModal
        open={detailOpen}
        onClose={() => setDetailOpen(false)}
        data={detailData}
        loading={detailLoading}
        error={detailError}
        onCancel={handleCancelInModal}
        cancelLoading={cancelLoading}
      />
    </>
  );
}