'use client';

import Link from 'next/link';
import { useEffect, useMemo, useState } from 'react';
import { decodeAccessTokenPayload, normalizeRoles } from './_utils';
import {
  cancelAutoPay,
  getMySubscription,
  revokeBillingKey,
  reactivateAutoPay,
  issueBillingKey,
} from '../../api/subscription';

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

  // 구독 관련
  onCancel,
  cancelLoading,
  onReactivate,
  reactivateLoading,

  // 카드 관련
  onRevoke,
  revokeLoading,
  onRegisterCard,
  registerLoading,
}) {
  if (!open) return null;

  const status = data?.status;
  const hasBillingKey = data?.hasBillingKey === true;

  // 버튼 노출 조건
  const canCancel = !loading && !error && status === 'ACTIVE';
  const canReactivate =
    !loading && !error && (status === 'CANCELED' || status === 'EXPIRED');

  const canRevoke = !loading && !error && hasBillingKey;
  const canRegister = !loading && !error && !hasBillingKey;

  // 버튼 공통 스타일
  const grayBtn =
    'rounded-xl border border-gray-200 bg-gray-100 px-4 py-2 text-sm font-semibold text-gray-700 hover:bg-gray-200 disabled:opacity-60';

  const blueBtn =
    'rounded-xl bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700 disabled:opacity-60';

  // 다른 액션 로딩 중이면 전체 버튼 잠그기(실수 방지)
  const anyActionLoading =
    !!cancelLoading || !!reactivateLoading || !!revokeLoading || !!registerLoading;

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
              {/* 상단 2x3 카드 */}
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

              {/* ✅ 카드등록 박스 + (오른쪽) 카드삭제/등록 버튼 */}
              <div className="mt-4 rounded-xl border border-gray-200 p-4">
                <div className="flex items-center justify-between gap-3">
                  <div>
                    <div className="text-xs text-gray-500">카드 등록</div>
                    <div className="mt-1 text-base font-semibold text-gray-900">
                      {hasBillingKey ? '등록 완료' : '미등록'}
                    </div>
                  </div>

                  {canRevoke && (
                    <button
                      type="button"
                      onClick={onRevoke}
                      disabled={revokeLoading || anyActionLoading}
                      className={grayBtn}
                    >
                      {revokeLoading ? '삭제 중...' : '카드삭제'}
                    </button>
                  )}

                  {canRegister && (
                    <button
                      type="button"
                      onClick={onRegisterCard}
                      disabled={registerLoading || anyActionLoading}
                      className={blueBtn}
                    >
                      {registerLoading ? '이동 중...' : '카드등록'}
                    </button>
                  )}
                </div>
              </div>

              {/* ✅ 모달 하단: (왼쪽) 구독취소/재구독  (오른쪽) 닫기 */}
              <div className="mt-5 flex items-center justify-between gap-2">
                <div className="flex items-center gap-2">
                  {canCancel && (
                    <button
                      type="button"
                      onClick={onCancel}
                      disabled={cancelLoading || anyActionLoading}
                      className={grayBtn}
                    >
                      {cancelLoading ? '취소 중...' : '구독취소'}
                    </button>
                  )}

                  {canReactivate && (
                    <button
                      type="button"
                      onClick={onReactivate}
                      disabled={reactivateLoading || anyActionLoading}
                      className={blueBtn}
                    >
                      {reactivateLoading ? '진행 중...' : '재구독'}
                    </button>
                  )}
                </div>

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

  // 상세 모달 상태
  const [detailOpen, setDetailOpen] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState(null);
  const [detailData, setDetailData] = useState(null);

  // 액션 로딩
  const [cancelLoading, setCancelLoading] = useState(false);
  const [reactivateLoading, setReactivateLoading] = useState(false);
  const [revokeLoading, setRevokeLoading] = useState(false);
  const [registerLoading, setRegisterLoading] = useState(false);

  useEffect(() => {
    const payload = decodeAccessTokenPayload();
    setRoles(normalizeRoles(payload?.roles));
  }, []);

  const isPremium = useMemo(
    () => roles.includes('SELLER') || roles.includes('PREMIUM'),
    [roles]
  );

  async function refreshDetail() {
    const res = await getMySubscription();
    const data = res?.data ?? res;
    setDetailData(data);
    return data;
  }

  // 자세히 열기
  async function handleOpenDetail() {
    setDetailOpen(true);
    setDetailLoading(true);
    setDetailError(null);

    try {
      await refreshDetail();
    } catch (e) {
      console.error(e);
      setDetailError(e?.message || '구독 정보를 불러오지 못했습니다.');
    } finally {
      setDetailLoading(false);
    }
  }

  // 구독취소
  async function handleCancel() {
    if (cancelLoading) return;
    const ok = confirm('구독을 취소할까요?\n취소해도 남은 기간까지 권한은 유지됩니다.');
    if (!ok) return;

    setCancelLoading(true);
    try {
      await cancelAutoPay();
      await refreshDetail();
      alert('구독이 취소되었습니다.');
    } catch (e) {
      console.error(e);
      alert(e?.message || '구독 취소에 실패했습니다.');
    } finally {
      setCancelLoading(false);
    }
  }

  // 재구독(자동결제 재활성화)
  async function handleReactivate() {
    if (reactivateLoading) return;
    const ok = confirm('재구독(자동결제 재활성화) 할까요?');
    if (!ok) return;

    setReactivateLoading(true);
    try {
      await reactivateAutoPay();
      await refreshDetail();
      alert('재구독 처리되었습니다.');
    } catch (e) {
      console.error(e);
      alert(e?.message || '재구독에 실패했습니다.');
    } finally {
      setReactivateLoading(false);
    }
  }

  // 카드삭제(빌링키 해지)
  async function handleRevoke() {
    if (revokeLoading) return;
    const ok = confirm('등록된 카드를 삭제할까요?\n삭제하면 다시 카드등록이 필요합니다.');
    if (!ok) return;

    setRevokeLoading(true);
    try {
      await revokeBillingKey();
      await refreshDetail();
      alert('카드가 삭제되었습니다.');
    } catch (e) {
      console.error(e);
      alert(e?.message || '카드 삭제에 실패했습니다.');
    } finally {
      setRevokeLoading(false);
    }
  }

  // 카드등록(= billing/issue 호출해서 Toss로 넘기는 흐름)
  // ✅ 여기서는 "마이페이지 모달"에서 subscription 페이지로 보내는 방식이 가장 안전함
  // (Toss SDK 로딩/스크립트 등 subscription 페이지에 이미 있을 확률이 높아서)
  async function handleRegisterCard() {
    if (registerLoading) return;
  
    setRegisterLoading(true);
    try {
      // 1) billing/issue 호출해서 successUrl/failUrl/customerKey 받기
      const info = await issueBillingKey({
        orderName: 'HomeDataZip 구독',
        amount: 9900,
      });
  
      if (!info?.successUrl || !info?.failUrl) {
        throw new Error('successUrl/failUrl 누락 (백엔드 응답 확인)');
      }
  
      const clientKey = process.env.NEXT_PUBLIC_TOSS_CLIENT_KEY;
      if (!clientKey) throw new Error('NEXT_PUBLIC_TOSS_CLIENT_KEY 없음 (.env 확인)');
  
      // 2) 토스 SDK로 카드등록 창 띄우기
      const tossPayments = window.TossPayments(clientKey);
  
      await tossPayments.requestBillingAuth('CARD', {
        customerKey: info.customerKey, // billing/issue에서 내려주는 customerKey
        successUrl: info.successUrl,
        failUrl: info.failUrl,
      });
  
      // 보통 여기 아래는 redirect 때문에 실행 안 됨
    } catch (e) {
      console.error(e);
      alert(e?.message || '카드 등록 시작 실패');
    } finally {
      setRegisterLoading(false);
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
              <span className="font-semibold">{isPremium ? '프리미엄' : '무료 체험'}</span>
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
        onCancel={handleCancel}
        cancelLoading={cancelLoading}
        onReactivate={handleReactivate}
        reactivateLoading={reactivateLoading}
        onRevoke={handleRevoke}
        revokeLoading={revokeLoading}
        onRegisterCard={handleRegisterCard}
        registerLoading={registerLoading}
      />
    </>
  );
}