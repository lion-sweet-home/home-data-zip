"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import SubscriptionDetailModal from "../components/SubscriptionDetailModal";
import SubscriptionFlowModal from "./SubscriptionFlowModal";
import {
  cancelAutoPay,
  getMySubscription,
  issueBillingKey,
  reactivateAutoPay,
  revokeBillingKey,
} from "../api/subscription";
import { getMyProfile } from "../api/user";
import { getSubscriptionMeta } from "../utils/subscription";

const PLAN_PRICE = 9900;

function parseApiError(error) {
  if (!error) {
    return { status: null, message: "알 수 없는 오류가 발생했습니다." };
  }
  return {
    status: error.status ?? null,
    message: error.message || "알 수 없는 오류가 발생했습니다.",
    data: error.data,
  };
}

function extractPhoneVerifiedAt(payload) {
  if (!payload) return null;
  return (
    payload.phoneVerifiedAt ||
    payload.phone_verified_at ||
    payload.user?.phoneVerifiedAt ||
    payload.user?.phone_verified_at ||
    null
  );
}

function extractCustomerKey(payload) {
  if (!payload) return null;
  return (
    payload.customerKey ||
    payload.customer_key ||
    payload.user?.customerKey ||
    payload.user?.customer_key ||
    null
  );
}

function normalizeApiData(res) {
  return res?.data ?? res ?? null;
}

function formatSubscriptionStatus(status, isActiveFlag) {
  if (isActiveFlag || status === "ACTIVE") return "구독중";
  if (status === "CANCELED") return "구독취소";
  if (status === "EXPIRED") return "구독만료";
  if (status === "NONE") return "-";
  return status || "-";
}

export default function SubscribePage() {
  const router = useRouter();
  const [subscription, setSubscription] = useState(null);

  // ✅ user 관련 상태
  const [phoneVerifiedAt, setPhoneVerifiedAt] = useState(null);
  const [customerKey, setCustomerKey] = useState(null);
  const [userApiAvailable, setUserApiAvailable] = useState(true);

  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState(null);

  const [detailOpen, setDetailOpen] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState(null);
  const [detailData, setDetailData] = useState(null);

  const [flowOpen, setFlowOpen] = useState(false);

  const [cancelLoading, setCancelLoading] = useState(false);
  const [reactivateLoading, setReactivateLoading] = useState(false);
  const [revokeLoading, setRevokeLoading] = useState(false);
  const [registerLoading, setRegisterLoading] = useState(false);

  const subscriptionMeta = useMemo(
    () => getSubscriptionMeta(subscription),
    [subscription]
  );
  const isActive = subscriptionMeta.isActive;
  const hasBillingKey = subscriptionMeta.hasBillingKey;
  const isPhoneVerified = Boolean(phoneVerifiedAt);

  useEffect(() => {
    refreshState({ showLoading: true });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function refreshState({ showLoading = false } = {}) {
    if (showLoading) setLoading(true);
    setError(null);

    const [subscriptionResult, userResult] = await Promise.allSettled([
      getMySubscription(),
      getMyProfile(),
    ]);

    let nextSubscription = subscription;
    let nextPhoneVerifiedAt = phoneVerifiedAt;
    let nextCustomerKey = customerKey;
    let nextUserApiAvailable = userApiAvailable;

    if (subscriptionResult.status === "fulfilled") {
      nextSubscription = normalizeApiData(subscriptionResult.value);
      setSubscription(nextSubscription);
    } else {
      setError(parseApiError(subscriptionResult.reason));
    }

    if (userResult.status === "fulfilled") {
      const userPayload = normalizeApiData(userResult.value);

      nextUserApiAvailable = true;
      setUserApiAvailable(true);

      nextPhoneVerifiedAt = extractPhoneVerifiedAt(userPayload);
      setPhoneVerifiedAt(nextPhoneVerifiedAt);

      nextCustomerKey = extractCustomerKey(userPayload);
      setCustomerKey(nextCustomerKey);
    } else if (userResult.reason?.status === 404) {
      nextUserApiAvailable = false;
      setUserApiAvailable(false);
    } else {
      setError(parseApiError(userResult.reason));
    }

    if (showLoading) setLoading(false);

    return {
      subscription: nextSubscription,
      phoneVerifiedAt: nextPhoneVerifiedAt,
      customerKey: nextCustomerKey,
      userApiAvailable: nextUserApiAvailable,
    };
  }

  async function refreshDetail() {
    const res = await getMySubscription();
    const data = normalizeApiData(res);
    setDetailData(data);
    return data;
  }

  async function handleOpenDetail() {
    setDetailOpen(true);
    setDetailLoading(true);
    setDetailError(null);

    try {
      await refreshDetail();
    } catch (err) {
      setDetailError(err?.message || "구독 정보를 불러오지 못했습니다.");
    } finally {
      setDetailLoading(false);
    }
  }

  async function handleSubscribeFlow() {
    if (actionLoading || loading) return;
    setActionLoading(true);
    setError(null);

    try {
      const latest = await refreshState();

      if (latest.subscription?.status === "ACTIVE" || latest.subscription?.isActive) {
        return;
      }
      setFlowOpen(true);
    } catch (err) {
      setError(parseApiError(err));
    } finally {
      setActionLoading(false);
    }
  }

  async function startBillingAuth(billingInfo) {
    console.log("[BILLING] startBillingAuth input =", billingInfo);

    const info = normalizeApiData(billingInfo);

    console.log("[BILLING] startBillingAuth normalized =", info);
    console.log("[BILLING] required fields =", {
      customerKey: info?.customerKey,
      successUrl: info?.successUrl,
      failUrl: info?.failUrl,
      orderName: info?.orderName,
      amount: info?.amount,
    });

    if (!info?.customerKey || !info?.successUrl || !info?.failUrl) {
      throw new Error(
        `카드 등록 정보를 가져오지 못했습니다. (customerKey/successUrl/failUrl 누락)`
      );
    }

    const clientKey = process.env.NEXT_PUBLIC_TOSS_CLIENT_KEY;
    if (!clientKey || typeof window === "undefined") {
      throw new Error(
        "Toss 결제 위젯 설정이 필요합니다. NEXT_PUBLIC_TOSS_CLIENT_KEY를 확인해주세요."
      );
    }

    if (typeof window.TossPayments !== "function") {
      throw new Error(
        "TossPayments SDK가 로드되지 않았습니다. 결제 위젯 스크립트를 확인해주세요."
      );
    }

    const tossPayments = window.TossPayments(clientKey);
    if (!tossPayments?.requestBillingAuth) {
      throw new Error("TossPayments SDK 초기화에 실패했습니다.");
    }

    await tossPayments.requestBillingAuth("CARD", {
      customerKey: info.customerKey,
      successUrl: info.successUrl,
      failUrl: info.failUrl,
    });
  }

  async function handleCancel() {
    if (cancelLoading) return;
    const ok = confirm(
      "구독을 취소할까요?\n취소해도 남은 기간까지 권한은 유지됩니다."
    );
    if (!ok) return;

    setCancelLoading(true);
    try {
      await cancelAutoPay();
      await refreshDetail();
      await refreshState();
      alert("구독이 취소되었습니다.");
    } catch (err) {
      alert(err?.message || "구독 취소에 실패했습니다.");
    } finally {
      setCancelLoading(false);
    }
  }

  async function handleReactivate() {
    if (reactivateLoading) return;
    const ok = confirm("재구독(자동결제 재활성화) 할까요?");
    if (!ok) return;

    setReactivateLoading(true);
    try {
      await reactivateAutoPay();
      await refreshDetail();
      await refreshState();
      alert("재구독 처리되었습니다.");
    } catch (err) {
      alert(err?.message || "재구독에 실패했습니다.");
    } finally {
      setReactivateLoading(false);
    }
  }

  async function handleRevoke() {
    if (revokeLoading) return;
    const ok = confirm(
      "등록된 카드를 삭제할까요?\n삭제하면 다시 카드등록이 필요합니다."
    );
    if (!ok) return;

    setRevokeLoading(true);
    try {
      await revokeBillingKey();
      await refreshDetail();
      await refreshState();
      alert("카드가 삭제되었습니다.");
    } catch (err) {
      alert(err?.message || "카드 삭제에 실패했습니다.");
    } finally {
      setRevokeLoading(false);
    }
  }

  async function handleRegisterCard() {
    if (registerLoading) return;

    setRegisterLoading(true);
    try {
      const latest = await refreshState();
      const ck = latest.customerKey || customerKey;
      if (!ck) {
        throw new Error(
          "customerKey를 가져오지 못했습니다. /users/me 응답을 확인해주세요."
        );
      }

      const billingInfoRes = await issueBillingKey({
        orderName: "HomeDataZip 구독",
        amount: PLAN_PRICE,
      });

      const billingInfo = normalizeApiData(billingInfoRes);
      if (!billingInfo) {
        throw new Error("빌링키 발급 응답이 undefined 입니다.");
      }

      if (!billingInfo.customerKey) {
        billingInfo.customerKey = ck;
      }

      await startBillingAuth(billingInfo);
    } catch (err) {
      alert(err?.message || "카드 등록 시작 실패");
    } finally {
      setRegisterLoading(false);
    }
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="w-10 h-10 border-4 border-blue-600 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 px-4 py-12">
      <div className="max-w-3xl mx-auto space-y-6">
        <div className="bg-white rounded-2xl shadow-sm p-6">
          <div className="flex items-start justify-between gap-4">
            <div>
              <h2 className="text-2xl font-bold text-gray-900">구독 관리</h2>
            </div>
            <div className="flex items-center gap-2">
              <button
                onClick={handleOpenDetail}
                className="px-4 py-2 rounded-xl border border-gray-200 text-gray-600 hover:bg-gray-50"
              >
                자세히
              </button>
              <button
                onClick={() => router.push("/my_page")}
                className="px-4 py-2 rounded-xl border border-gray-200 text-gray-600 hover:bg-gray-50"
              >
                마이페이지
              </button>
            </div>
          </div>

          {error && (
            <div className="mt-5 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
              <div className="font-semibold">요청 실패</div>
              <div className="mt-1">
                {error.message}
                {error.status ? ` (status: ${error.status})` : ""}
              </div>
            </div>
          )}

          <div className="mt-6 grid gap-4 md:grid-cols-3">
            <div className="rounded-xl border border-gray-200 p-4">
              <p className="text-sm text-gray-500">휴대폰 인증</p>
              <p className="mt-2 text-lg font-semibold text-gray-900">
                {isPhoneVerified ? "인증 완료" : "미인증"}
              </p>
              {!userApiAvailable && (
                <p className="mt-2 text-xs text-amber-600">
                  사용자 정보 API가 없어 인증 여부를 확인할 수 없습니다.
                </p>
              )}
            </div>
            <div className="rounded-xl border border-gray-200 p-4">
              <p className="text-sm text-gray-500">카드 등록</p>
              <p className="mt-2 text-lg font-semibold text-gray-900">
                {hasBillingKey ? "등록 완료" : "미등록"}
              </p>
            </div>
            <div className="rounded-xl border border-gray-200 p-4">
              <p className="text-sm text-gray-500">구독 상태</p>
              <p className="mt-2 text-lg font-semibold text-gray-900">
                {formatSubscriptionStatus(subscription?.status, isActive)}
              </p>
            </div>
          </div>

          <div className="mt-6">
            <button
              onClick={handleSubscribeFlow}
              disabled={actionLoading || isActive}
              className={`w-full py-3 rounded-xl text-lg font-semibold transition-colors ${
                actionLoading || isActive
                  ? "bg-gray-200 text-gray-400 cursor-not-allowed"
                  : "bg-blue-600 text-white hover:bg-blue-700"
              }`}
            >
              {isActive
                ? "구독중"
                : actionLoading
                ? "처리 중..."
                : !isPhoneVerified || !hasBillingKey
                ? "개인정보등록"
                : "9,900원 구독하기"}
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

        <SubscriptionFlowModal
          open={flowOpen}
          onClose={() => {
            setFlowOpen(false);
          }}
          refreshState={refreshState}
          initialState={{ subscription, phoneVerifiedAt, customerKey }}
        />
      </div>
    </div>
  );
}