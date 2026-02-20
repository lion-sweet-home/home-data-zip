"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import {
  getMySubscription,
  issueBillingKey,
  sendPhoneAuth,
  startSubscription,
  verifyPhoneAuth,
} from "../api/subscription";
import { getMyProfile } from "../api/user";

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

export default function SubscribePage() {
  const router = useRouter();
  const [subscription, setSubscription] = useState(null);
  const [phoneVerifiedAt, setPhoneVerifiedAt] = useState(null);
  const [userApiAvailable, setUserApiAvailable] = useState(true);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState(null);

  const [phoneAuthOpen, setPhoneAuthOpen] = useState(false);
  const [phoneNumber, setPhoneNumber] = useState("");
  const [requestId, setRequestId] = useState("");
  const [ttlSeconds, setTtlSeconds] = useState(null);
  const [code, setCode] = useState("");
  const [verificationToken, setVerificationToken] = useState(null);
  const [phoneAuthStatus, setPhoneAuthStatus] = useState("idle");

  const isActive = useMemo(() => {
    return subscription?.status === "ACTIVE" || subscription?.isActive === true;
  }, [subscription]);

  const hasBillingKey = subscription?.hasBillingKey === true;
  const isPhoneVerified =
    Boolean(phoneVerifiedAt) || phoneAuthStatus === "verified";

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
    let nextUserApiAvailable = userApiAvailable;

    if (subscriptionResult.status === "fulfilled") {
      nextSubscription = subscriptionResult.value;
      setSubscription(subscriptionResult.value);
    } else {
      setError(parseApiError(subscriptionResult.reason));
    }

    if (userResult.status === "fulfilled") {
      nextUserApiAvailable = true;
      nextPhoneVerifiedAt = extractPhoneVerifiedAt(userResult.value);
      setUserApiAvailable(true);
      setPhoneVerifiedAt(nextPhoneVerifiedAt);
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
      userApiAvailable: nextUserApiAvailable,
    };
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

      // ✅ 폰 인증이 안 되어 있으면 여기서 막고 모달 오픈
      if (!latest.phoneVerifiedAt && phoneAuthStatus !== "verified") {
        setPhoneAuthOpen(true);
        return;
      }

      if (!latest.subscription?.hasBillingKey) {
        const billingInfoRes = await issueBillingKey({
          orderName: "HomeDataZip 구독",
          amount: PLAN_PRICE,
        });

        // =======================
        // ✅✅ 1분 디버깅 (여기!)
        // =======================
        console.log("[BILLING] issueBillingKey raw response =", billingInfoRes);

        // 응답이 { data: {...} } 형태일 수도 있어서 정규화
        const billingInfo = billingInfoRes?.data ?? billingInfoRes;

        console.log("[BILLING] normalized billingInfo =", billingInfo);
        console.log("[BILLING] keys check =", {
          hasCustomerKey: !!billingInfo?.customerKey,
          hasSuccessUrl: !!billingInfo?.successUrl,
          hasFailUrl: !!billingInfo?.failUrl,
        });

        if (!billingInfo) {
          throw new Error("빌링키 발급 응답이 undefined 입니다. (issueBillingKey 결과 확인)");
        }

        await startBillingAuth(billingInfo);
        return;
      }

      await startSubscription();
      await refreshState();
    } catch (err) {
      setError(parseApiError(err));
    } finally {
      setActionLoading(false);
    }
  }

  async function startBillingAuth(billingInfo) {
    // =======================
    // ✅✅ 1분 디버깅 (여기!)
    // =======================
    console.log("[BILLING] startBillingAuth input =", billingInfo);

    // 혹시 여기에서도 {data:{...}} 넘어오는 케이스 방어
    const info = billingInfo?.data ?? billingInfo;

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

    await tossPayments.requestBillingAuth({
      method: "CARD",
      customerKey: info.customerKey,
      orderName: info.orderName || "HomeDataZip 구독",
      amount: info.amount && info.amount > 0 ? info.amount : PLAN_PRICE,
      successUrl: info.successUrl,
      failUrl: info.failUrl,
    });
  }

  async function handleSendPhoneAuth() {
    if (!phoneNumber) {
      setError({ status: null, message: "휴대폰 번호를 입력해주세요." });
      return;
    }

    setActionLoading(true);
    setError(null);
    try {
      const response = await sendPhoneAuth(phoneNumber);

      // ✅ 백엔드 응답키에 맞춤: expiresInSeconds
      setRequestId(response.requestId);
      setTtlSeconds(response.expiresInSeconds ?? null);

      setPhoneAuthStatus("sent");
    } catch (err) {
      setError(parseApiError(err));
    } finally {
      setActionLoading(false);
    }
  }

  async function handleVerifyPhoneAuth() {
    if (!phoneNumber || !requestId || !code) {
      setError({ status: null, message: "인증 정보를 모두 입력해주세요." });
      return;
    }

    setActionLoading(true);
    setError(null);
    try {
      const response = await verifyPhoneAuth({ phoneNumber, requestId, code });

      // ✅ 백엔드 응답키에 맞춤: verified
      if (!response?.verified) {
        throw new Error("인증에 실패했습니다. 인증번호를 확인해주세요.");
      }

      setVerificationToken(response.verificationToken ?? null);
      localStorage.setItem(
        "phoneVerificationToken",
        response.verificationToken ?? ""
      );

      setPhoneAuthStatus("verified");
      setPhoneVerifiedAt(new Date().toISOString());
      setPhoneAuthOpen(false);

      await refreshState();
    } catch (err) {
      setError(parseApiError(err));
    } finally {
      setActionLoading(false);
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
              <p className="text-gray-500 mt-1">
                휴대폰 인증 → 카드등록 → 구독 시작 순서로 진행됩니다.
              </p>
            </div>
            <button
              onClick={() => router.push("/my_page")}
              className="px-4 py-2 rounded-xl border border-gray-200 text-gray-600 hover:bg-gray-50"
            >
              마이페이지
            </button>
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
                {isActive ? "구독중" : subscription?.status || "대기"}
              </p>
              {subscription?.startDate && (
                <p className="mt-2 text-xs text-gray-400">
                  시작일: {subscription.startDate}
                </p>
              )}
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
                : "9,900원 구독하기"}
            </button>
          </div>
        </div>

        {phoneAuthOpen && (
          <div className="bg-white rounded-2xl shadow-sm p-6">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-semibold text-gray-900">휴대폰 인증</h3>
              <button
                onClick={() => setPhoneAuthOpen(false)}
                className="text-sm text-gray-400 hover:text-gray-600"
              >
                닫기
              </button>
            </div>

            <div className="mt-4 space-y-4">
              <div>
                <label className="block text-sm text-gray-500 mb-1">
                  휴대폰 번호
                </label>
                <input
                  value={phoneNumber}
                  onChange={(event) => setPhoneNumber(event.target.value)}
                  placeholder="01012345678"
                  className="w-full rounded-xl border border-gray-200 px-4 py-2"
                />
              </div>

              <button
                onClick={handleSendPhoneAuth}
                disabled={actionLoading}
                className="w-full py-2.5 rounded-xl bg-gray-900 text-white hover:bg-gray-800 disabled:opacity-50"
              >
                인증번호 발송
              </button>

              {phoneAuthStatus !== "idle" && (
                <div className="rounded-xl border border-gray-200 p-4 space-y-3">
                  <div className="text-sm text-gray-500">
                    요청 ID: <span className="font-mono">{requestId}</span>
                  </div>

                  {ttlSeconds != null && (
                    <div className="text-sm text-gray-500">
                      유효 시간: {ttlSeconds}초
                    </div>
                  )}

                  <div>
                    <label className="block text-sm text-gray-500 mb-1">
                      인증번호
                    </label>
                    <input
                      value={code}
                      onChange={(event) => setCode(event.target.value)}
                      placeholder="인증번호 입력"
                      className="w-full rounded-xl border border-gray-200 px-4 py-2"
                    />
                  </div>

                  <button
                    onClick={handleVerifyPhoneAuth}
                    disabled={actionLoading}
                    className="w-full py-2.5 rounded-xl bg-blue-600 text-white hover:bg-blue-700 disabled:opacity-50"
                  >
                    인증 완료
                  </button>

                  {verificationToken && (
                    <p className="text-xs text-gray-400">
                      인증 토큰이 발급되었습니다.
                    </p>
                  )}
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}