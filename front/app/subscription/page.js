'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { issueBillingKey } from '../api/subscription';

const CLIENT_KEY = process.env.NEXT_PUBLIC_TOSS_CLIENT_KEY;

const PLAN_FEATURES = [
  '매물 등록 및 관리',
  '프리미엄 데이터 분석 리포트 (지원 예정)',
  '실시간 시세 알림 (지원 예정)',
  '우선 고객 지원 (지원 예정)',
];

export default function SubscriptionPage() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [amount] = useState(9900);
  const router = useRouter();

  const handleSubscribe = async () => {
    const accessToken =
      typeof window !== 'undefined' ? localStorage.getItem('accessToken') : null;

    if (!accessToken) {
      router.push('/auth/login');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const billingInfo = await issueBillingKey();
      const { customerKey, successUrl, failUrl } = billingInfo;

      const { loadTossPayments } = await import('@tosspayments/tosspayments-sdk');
      const tossPayments = await loadTossPayments(CLIENT_KEY);

      const payment = tossPayments.payment({ customerKey });

      await payment.requestBillingAuth({
        method: 'CARD',
        successUrl,
        failUrl,
      });
    } catch (err) {
      if (err.code !== 'USER_CANCEL') {
        setError(err.message || '결제 요청 중 오류가 발생했습니다.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 py-12 px-4">
      <div className="max-w-lg mx-auto">
        {/* 구독 플랜 카드 */}
        <div className="bg-white rounded-2xl shadow-lg overflow-hidden mb-8">
          <div className="bg-gradient-to-r from-blue-600 to-indigo-600 p-8 text-center">
            <span className="inline-block px-3 py-1 bg-white/20 text-white text-xs font-semibold rounded-full mb-3">
              PREMIUM
            </span>
            <h1 className="text-2xl font-bold text-white mb-1">프리미엄 구독</h1>
            <p className="text-blue-100 text-sm">매물 등록과 프리미엄 기능을 이용해보세요</p>
          </div>

          <div className="p-8">
            <div className="text-center mb-8">
              <div className="flex items-baseline justify-center gap-1">
                <span className="text-4xl font-extrabold text-gray-900">
                  {amount.toLocaleString()}
                </span>
                <span className="text-gray-500 text-lg">원</span>
              </div>
              <p className="text-gray-400 text-sm mt-1">매월 자동 결제</p>
            </div>

            <div className="border-t border-gray-100 pt-6">
              <ul className="space-y-4">
                {PLAN_FEATURES.map((feature) => (
                  <li key={feature} className="flex items-center gap-3">
                    <div className="w-5 h-5 rounded-full bg-blue-100 flex items-center justify-center shrink-0">
                      <svg
                        className="w-3 h-3 text-blue-600"
                        fill="currentColor"
                        viewBox="0 0 20 20"
                      >
                        <path
                          fillRule="evenodd"
                          d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
                          clipRule="evenodd"
                        />
                      </svg>
                    </div>
                    <span className="text-gray-700">{feature}</span>
                  </li>
                ))}
              </ul>
            </div>
          </div>
        </div>

        {/* 에러 메시지 */}
        {error && (
          <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-xl mb-6 text-sm">
            {error}
          </div>
        )}

        {/* 구독 버튼 */}
        <button
          onClick={handleSubscribe}
          disabled={loading}
          className="w-full py-4 bg-blue-600 hover:bg-blue-700 disabled:bg-gray-300 text-white font-semibold text-lg rounded-xl transition-colors disabled:cursor-not-allowed"
        >
          {loading ? (
            <span className="flex items-center justify-center gap-2">
              <span className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
              처리 중...
            </span>
          ) : (
            `${amount.toLocaleString()}원 구독하기`
          )}
        </button>

        {/* 안내 문구 */}
        <div className="mt-6 space-y-2">
          <p className="text-center text-gray-400 text-xs">
            카드 등록 후 첫 결제가 진행되며, 이후 매월 자동 결제됩니다.
            <br />
            구독은 마이페이지에서 언제든지 해지할 수 있습니다.
          </p>
          <div className="flex items-center justify-center gap-2 pt-2">
            <svg className="w-4 h-4 text-gray-300" fill="currentColor" viewBox="0 0 20 20">
              <path
                fillRule="evenodd"
                d="M5 9V7a5 5 0 0110 0v2a2 2 0 012 2v5a2 2 0 01-2 2H5a2 2 0 01-2-2v-5a2 2 0 012-2zm8-2v2H7V7a3 3 0 016 0z"
                clipRule="evenodd"
              />
            </svg>
            <span className="text-gray-400 text-xs">토스페이먼츠 안전결제</span>
          </div>
        </div>
      </div>
    </div>
  );
}
