"use client";

import { useEffect, useMemo, useState } from "react";
import {
  issueBillingKey,
  sendPhoneAuth,
  startSubscription,
  verifyPhoneAuth,
} from "../api/subscription";

const PLAN_PRICE = 9900;

const STEPS = {
  PHONE: "PHONE",
  BILLING: "BILLING",
  SUBSCRIBE: "SUBSCRIBE",
  DONE: "DONE",
};

export default function SubscriptionFlowModal({ onClose, refreshState, initialState }) {
  const [step, setStep] = useState(STEPS.PHONE);
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState(null);

  // state snapshot
  const [subscription, setSubscription] = useState(initialState.subscription ?? null);
  const [phoneVerifiedAt, setPhoneVerifiedAt] = useState(initialState.phoneVerifiedAt ?? null);
  const [customerKey, setCustomerKey] = useState(initialState.customerKey ?? null);

  // phone auth inputs
  const [phoneNumber, setPhoneNumber] = useState("");
  const [requestId, setRequestId] = useState("");
  const [code, setCode] = useState("");
  const [ttlSeconds, setTtlSeconds] = useState(null);

  const isActive = useMemo(
    () => subscription?.status === "ACTIVE" || subscription?.isActive === true,
    [subscription]
  );
  const hasBillingKey = subscription?.hasBillingKey === true;
  const isPhoneVerified = !!phoneVerifiedAt;

  // 단계 자동 결정
  useEffect(() => {
    const next = decideStep({ isActive, hasBillingKey, isPhoneVerified });
    setStep(next);
  }, [isActive, hasBillingKey, isPhoneVerified]);

  function decideStep({ isActive, hasBillingKey, isPhoneVerified }) {
    if (isActive) return STEPS.DONE;
    if (!isPhoneVerified) return STEPS.PHONE;
    if (!hasBillingKey) return STEPS.BILLING;
    return STEPS.SUBSCRIBE;
  }

  async function syncLatest() {
    const latest = await refreshState();
    setSubscription(latest.subscription ?? null);
    setPhoneVerifiedAt(latest.phoneVerifiedAt ?? null);
    setCustomerKey(latest.customerKey ?? null);
    return latest;
  }

  // 1) 휴대폰 인증 - 발송
  async function handleSend() {
    if (!phoneNumber) return setErr("휴대폰 번호 입력해.");
    setBusy(true); setErr(null);
    try {
      const res = await sendPhoneAuth(phoneNumber);
      setRequestId(res.requestId);
      setTtlSeconds(res.expiresInSeconds ?? null);
    } catch (e) {
      setErr(e?.message ?? "발송 실패");
    } finally {
      setBusy(false);
    }
  }

  // 1) 휴대폰 인증 - 검증
  async function handleVerify() {
    if (!phoneNumber || !requestId || !code) return setErr("요청ID/인증번호 다 입력해.");
    setBusy(true); setErr(null);
    try {
      const res = await verifyPhoneAuth({ phoneNumber, requestId, code });
      if (!res?.verified) throw new Error("인증 실패");
      await syncLatest(); // ✅ 여기서 phoneVerifiedAt 갱신됨
    } catch (e) {
      setErr(e?.message ?? "인증 실패");
    } finally {
      setBusy(false);
    }
  }

  // 2) 카드 등록(토스 BillingAuth)
  async function handleBilling() {
    setBusy(true); setErr(null);
    try {
      const latest = await syncLatest();
      const ck = latest.customerKey || customerKey;
      if (!ck) throw new Error("customerKey 없음. /users/me 응답 확인.");

      const billingInfoRes = await issueBillingKey({
        orderName: "HomeDataZip 구독",
        amount: PLAN_PRICE,
      });
      const info = billingInfoRes?.data ?? billingInfoRes;

      if (!info?.successUrl || !info?.failUrl) {
        throw new Error("successUrl/failUrl 누락");
      }

      // ✅ SDK 호출 (아까 해결한 방식!)
      const clientKey = process.env.NEXT_PUBLIC_TOSS_CLIENT_KEY;
      const tossPayments = window.TossPayments(clientKey);

      await tossPayments.requestBillingAuth("CARD", {
        customerKey: info.customerKey || ck,
        successUrl: info.successUrl,
        failUrl: info.failUrl,
      });

      // 여기 이후는 redirect라서 보통 아래 코드는 실행 안 됨
    } catch (e) {
      setErr(e?.message ?? "카드 등록 실패");
    } finally {
      setBusy(false);
    }
  }

  // 3) 구독 시작
  async function handleSubscribe() {
    setBusy(true); setErr(null);
    try {
      await startSubscription();
      await syncLatest();
      setStep(STEPS.DONE);
    } catch (e) {
      setErr(e?.message ?? "구독 시작 실패");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div className="w-full max-w-lg rounded-2xl bg-white shadow-xl">
        <div className="flex items-center justify-between border-b px-6 py-4">
          <div className="font-bold text-lg">구독 진행</div>
          <button onClick={onClose} className="text-gray-500 hover:text-gray-800">
            닫기
          </button>
        </div>

        <div className="px-6 py-5 space-y-4">
          {/* Step 표시 */}
          <div className="text-sm text-gray-500">
            단계:{" "}
            <span className="font-semibold text-gray-900">
              {step === STEPS.PHONE && "휴대폰 인증"}
              {step === STEPS.BILLING && "카드 등록"}
              {step === STEPS.SUBSCRIBE && "구독 시작"}
              {step === STEPS.DONE && "완료"}
            </span>
          </div>

          {err && (
            <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
              {err}
            </div>
          )}

          {/* PHONE */}
          {step === STEPS.PHONE && (
            <div className="space-y-3">
              <input
                value={phoneNumber}
                onChange={(e) => setPhoneNumber(e.target.value)}
                placeholder="01012345678"
                className="w-full rounded-xl border px-4 py-2"
              />
              <button
                onClick={handleSend}
                disabled={busy}
                className="w-full rounded-xl bg-gray-900 py-2.5 text-white disabled:opacity-50"
              >
                인증번호 발송
              </button>

              {requestId && (
                <div className="space-y-2">
                  <div className="text-xs text-gray-500">
                    요청 ID: <span className="font-mono">{requestId}</span>
                    {ttlSeconds != null && ` (유효 ${ttlSeconds}초)`}
                  </div>
                  <input
                    value={code}
                    onChange={(e) => setCode(e.target.value)}
                    placeholder="인증번호"
                    className="w-full rounded-xl border px-4 py-2"
                  />
                  <button
                    onClick={handleVerify}
                    disabled={busy}
                    className="w-full rounded-xl bg-blue-600 py-2.5 text-white disabled:opacity-50"
                  >
                    인증 완료
                  </button>
                </div>
              )}
            </div>
          )}

          {/* BILLING */}
          {step === STEPS.BILLING && (
            <div className="space-y-3">
              <div className="text-sm text-gray-600">
                카드 등록을 진행합니다. (토스 결제창으로 이동)
              </div>
              <button
                onClick={handleBilling}
                disabled={busy}
                className="w-full rounded-xl bg-blue-600 py-3 text-white font-semibold disabled:opacity-50"
              >
                카드 등록하기
              </button>
            </div>
          )}

          {/* SUBSCRIBE */}
          {step === STEPS.SUBSCRIBE && (
            <div className="space-y-3">
              <div className="text-sm text-gray-600">
                카드 등록이 완료되었습니다. 구독을 시작할까요?
              </div>
              <button
                onClick={handleSubscribe}
                disabled={busy}
                className="w-full rounded-xl bg-blue-600 py-3 text-white font-semibold disabled:opacity-50"
              >
                구독 시작
              </button>
            </div>
          )}

          {/* DONE */}
          {step === STEPS.DONE && (
            <div className="space-y-3">
              <div className="text-sm text-gray-700 font-semibold">
                구독이 완료되었습니다 🎉
              </div>
              <button
                onClick={() => {
                  onClose();
                }}
                className="w-full rounded-xl bg-gray-900 py-2.5 text-white"
              >
                닫기
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}