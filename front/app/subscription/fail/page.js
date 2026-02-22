"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { getMySubscription } from "../../api/subscription";

function hasBillingKey(subscription) {
  return (
    subscription?.hasBillingKey === true ||
    Boolean(subscription?.billingKey || subscription?.billing_key)
  );
}

export default function SubscriptionFailPage() {
  const router = useRouter();
  const [loading, setLoading] = useState(true);
  const [registered, setRegistered] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    let mounted = true;

    async function checkStatus() {
      setLoading(true);
      setError(null);
      try {
        const res = await getMySubscription();
        if (!mounted) return;
        setRegistered(hasBillingKey(res?.data ?? res));
      } catch (err) {
        if (!mounted) return;
        setError(err?.message || "구독 정보를 확인하지 못했습니다.");
      } finally {
        if (mounted) setLoading(false);
      }
    }

    checkStatus();
    return () => {
      mounted = false;
    };
  }, []);

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center px-4">
      <div className="max-w-md w-full bg-white rounded-2xl shadow-lg p-8 text-center">
        {loading ? (
          <>
            <div className="w-12 h-12 mx-auto mb-4 rounded-full border-4 border-blue-600 border-t-transparent animate-spin" />
            <h2 className="text-xl font-bold text-gray-900 mb-2">
              카드 등록 확인 중
            </h2>
            <p className="text-gray-500 mb-6">
              서버 상태를 확인하고 있습니다.
            </p>
          </>
        ) : registered ? (
          <>
            <div className="w-16 h-16 mx-auto mb-4 rounded-full bg-green-100 flex items-center justify-center">
              <svg
                className="w-8 h-8 text-green-500"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M5 13l4 4L19 7"
                />
              </svg>
            </div>
            <h2 className="text-xl font-bold text-gray-900 mb-2">
              카드 등록 완료
            </h2>
            <p className="text-gray-500 mb-6">
              서버에 카드 정보가 정상 등록되었습니다.
            </p>
            <button
              onClick={() => router.replace("/subscription")}
              className="px-5 py-2.5 rounded-xl bg-blue-600 text-white hover:bg-blue-700 transition-colors font-medium"
            >
              구독 페이지로 이동
            </button>
          </>
        ) : (
          <>
            <div className="w-16 h-16 mx-auto mb-4 rounded-full bg-red-100 flex items-center justify-center">
              <svg
                className="w-8 h-8 text-red-500"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M6 18L18 6M6 6l12 12"
                />
              </svg>
            </div>
            <h2 className="text-xl font-bold text-gray-900 mb-2">
              카드 등록 실패
            </h2>
            <p className="text-gray-500 mb-6">
              {error || "등록 상태를 확인할 수 없습니다."}
            </p>
            <button
              onClick={() => router.replace("/subscription")}
              className="px-5 py-2.5 rounded-xl bg-gray-900 text-white hover:bg-gray-800 transition-colors font-medium"
            >
              다시 시도
            </button>
          </>
        )}
      </div>
    </div>
  );
}
"use client";

import { Suspense, useEffect } from "react";
import { useSearchParams, useRouter } from "next/navigation";

function FailContent() {
  const searchParams = useSearchParams();
  const router = useRouter();

  const code = searchParams.get("code");
  const message = searchParams.get("message");

  useEffect(() => {
    const timer = setTimeout(() => {
      router.replace("/subscription");
    }, 2500);

    return () => clearTimeout(timer);
  }, [router]);

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center px-4">
      <div className="max-w-md w-full bg-white rounded-2xl shadow-lg p-8 text-center">
        <div className="w-16 h-16 mx-auto mb-4 rounded-full bg-red-100 flex items-center justify-center">
          <svg
            className="w-8 h-8 text-red-500"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M6 18L18 6M6 6l12 12"
            />
          </svg>
        </div>
        <h2 className="text-xl font-bold text-gray-900 mb-2">카드 등록 실패</h2>
        <p className="text-gray-500 mb-6">
          {message || "카드 등록 중 문제가 발생했습니다. 다시 시도해주세요."}
        </p>

        {code && (
          <div className="bg-gray-50 rounded-xl p-4 mb-6">
            <p className="text-xs text-gray-400">
              오류 코드: <span className="font-mono">{code}</span>
            </p>
          </div>
        )}

        <div className="flex gap-3 justify-center">
          <button
            onClick={() => router.push("/")}
            className="px-5 py-2.5 rounded-xl border border-gray-300 text-gray-700 hover:bg-gray-50 transition-colors font-medium"
          >
            홈으로
          </button>
          <button
            onClick={() => router.push("/subscription")}
            className="px-5 py-2.5 rounded-xl bg-blue-600 text-white hover:bg-blue-700 transition-colors font-medium"
          >
            다시 시도
          </button>
        </div>
      </div>
    </div>
  );
}

export default function SubscriptionFailPage() {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen bg-gray-50 flex items-center justify-center">
          <div className="w-10 h-10 border-4 border-blue-600 border-t-transparent rounded-full animate-spin" />
        </div>
      }
    >
      <FailContent />
    </Suspense>
  );
}
