"use client";

import {
  DEFAULT_SUBSCRIPTION_PLAN_NAME,
  DEFAULT_SUBSCRIPTION_PRICE,
  getSubscriptionMeta,
} from "../utils/subscription";

function formatDate(value) {
  if (!value) return "-";
  try {
    const d = new Date(value);
    if (Number.isNaN(d.getTime())) return String(value);
    return d.toLocaleString();
  } catch {
    return String(value);
  }
}

function formatSubscriptionStatus(status, isActive) {
  if (isActive || status === "ACTIVE") return "구독중";
  if (status === "CANCELED") return "구독취소";
  if (status === "EXPIRED") return "구독만료";
  if (status === "NONE") return "-";
  return status || "-";
}

export default function SubscriptionDetailModal({
  open,
  onClose,
  data,
  loading,
  error,
  onCancel,
  cancelLoading,
  onReactivate,
  reactivateLoading,
  onRevoke,
  revokeLoading,
  onRegisterCard,
  registerLoading,
}) {
  if (!open) return null;

  const meta = getSubscriptionMeta(data);
  const status = meta.status;
  const hasBillingKey = meta.hasBillingKey;
  const isActive = meta.isActive;
  const planName = meta.planName || (isActive ? DEFAULT_SUBSCRIPTION_PLAN_NAME : "-");
  const priceValue =
    typeof meta.price === "number"
      ? meta.price
      : isActive
      ? DEFAULT_SUBSCRIPTION_PRICE
      : null;

  const canCancel = !loading && !error && (status === "ACTIVE" || isActive);
  const canReactivate =
    !loading && !error && (status === "CANCELED" || status === "EXPIRED");

  const canRevoke = !loading && !error && hasBillingKey;
  const canRegister = !loading && !error && !hasBillingKey;

  const grayBtn =
    "rounded-xl border border-gray-200 bg-gray-100 px-4 py-2 text-sm font-semibold text-gray-700 hover:bg-gray-200 disabled:opacity-60";

  const blueBtn =
    "rounded-xl bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700 disabled:opacity-60";

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
              <div className="grid grid-cols-2 gap-3">
                <div className="rounded-xl border border-gray-200 p-4">
                  <div className="text-xs text-gray-500">상태</div>
                  <div className="mt-1 text-base font-semibold text-gray-900">
                    {formatSubscriptionStatus(status, isActive)}
                  </div>
                </div>

                <div className="rounded-xl border border-gray-200 p-4">
                  <div className="text-xs text-gray-500">자동결제 활성</div>
                  <div className="mt-1 text-base font-semibold text-gray-900">
                    {isActive ? "ON" : "OFF"}
                  </div>
                </div>

                <div className="rounded-xl border border-gray-200 p-4">
                  <div className="text-xs text-gray-500">요금제</div>
                  <div className="mt-1 text-base font-semibold text-gray-900">
                    {planName}
                  </div>
                </div>

                <div className="rounded-xl border border-gray-200 p-4">
                  <div className="text-xs text-gray-500">가격</div>
                  <div className="mt-1 text-base font-semibold text-gray-900">
                    {typeof priceValue === "number"
                      ? `${priceValue.toLocaleString()}원`
                      : "-"}
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
                <div className="flex items-center justify-between gap-3">
                  <div>
                    <div className="text-xs text-gray-500">카드 등록</div>
                    <div className="mt-1 text-base font-semibold text-gray-900">
                      {hasBillingKey ? "등록 완료" : "미등록"}
                    </div>
                  </div>

                  {canRevoke && (
                    <button
                      type="button"
                      onClick={onRevoke}
                      disabled={revokeLoading || anyActionLoading}
                      className={grayBtn}
                    >
                      {revokeLoading ? "삭제 중..." : "카드삭제"}
                    </button>
                  )}

                  {canRegister && (
                    <button
                      type="button"
                      onClick={onRegisterCard}
                      disabled={registerLoading || anyActionLoading}
                      className={blueBtn}
                    >
                      {registerLoading ? "이동 중..." : "카드등록"}
                    </button>
                  )}
                </div>
              </div>

              <div className="mt-5 flex items-center justify-between gap-2">
                <div className="flex items-center gap-2">
                  {canCancel && (
                    <button
                      type="button"
                      onClick={onCancel}
                      disabled={cancelLoading || anyActionLoading}
                      className={grayBtn}
                    >
                      {cancelLoading ? "취소 중..." : "구독취소"}
                    </button>
                  )}

                  {canReactivate && (
                    <button
                      type="button"
                      onClick={onReactivate}
                      disabled={reactivateLoading || anyActionLoading}
                      className={blueBtn}
                    >
                      {reactivateLoading ? "진행 중..." : "재구독"}
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
