"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";

export default function SubscriptionSuccessPage() {
  const router = useRouter();

  useEffect(() => {
    const timer = setTimeout(() => {
      alert("카드 등록이 완료되었습니다.");
      router.replace("/subscription");
    }, 800);

    return () => clearTimeout(timer);
  }, [router]);

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center px-4">
      <div className="max-w-md w-full bg-white rounded-2xl shadow-lg p-8 text-center">
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
        <h2 className="text-xl font-bold text-gray-900 mb-2">카드 등록 완료</h2>
        <p className="text-gray-500 mb-6">
          잠시 후 구독 페이지로 이동합니다.
        </p>
        <button
          onClick={() => router.replace("/subscription")}
          className="px-5 py-2.5 rounded-xl bg-blue-600 text-white hover:bg-blue-700 transition-colors font-medium"
        >
          바로 이동
        </button>
      </div>
    </div>
  );
}
