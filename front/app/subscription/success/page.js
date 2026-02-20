'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { startSubscription } from '../../api/subscription';

export default function SubscriptionSuccessPage() {
  const router = useRouter();
  const [status, setStatus] = useState('activating');
  const [error, setError] = useState(null);

  useEffect(() => {
    async function activate() {
      try {
        await startSubscription();
        setStatus('success');
      } catch (err) {
        console.error('구독 활성화 실패:', err);
        setStatus('error');
        setError(err.message || '구독 활성화에 실패했습니다.');
      }
    }

    activate();
  }, []);

  if (status === 'activating') {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center px-4">
        <div className="max-w-md w-full bg-white rounded-2xl shadow-lg p-8 text-center">
          <div className="w-16 h-16 mx-auto mb-6 border-4 border-blue-600 border-t-transparent rounded-full animate-spin" />
          <h2 className="text-xl font-bold text-gray-900 mb-2">구독 활성화 중</h2>
          <p className="text-gray-500">잠시만 기다려주세요...</p>
        </div>
      </div>
    );
  }

  if (status === 'error') {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center px-4">
        <div className="max-w-md w-full bg-white rounded-2xl shadow-lg p-8 text-center">
          <div className="w-16 h-16 mx-auto mb-4 rounded-full bg-amber-100 flex items-center justify-center">
            <svg
              className="w-8 h-8 text-amber-500"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z"
              />
            </svg>
          </div>
          <h2 className="text-xl font-bold text-gray-900 mb-2">구독 활성화 실패</h2>
          <p className="text-gray-500 mb-6">{error}</p>
          <div className="flex gap-3 justify-center">
            <button
              onClick={() => router.push('/')}
              className="px-5 py-2.5 rounded-xl border border-gray-300 text-gray-700 hover:bg-gray-50 transition-colors font-medium"
            >
              홈으로
            </button>
            <button
              onClick={() => router.push('/subscription')}
              className="px-5 py-2.5 rounded-xl bg-blue-600 text-white hover:bg-blue-700 transition-colors font-medium"
            >
              다시 시도
            </button>
          </div>
        </div>
      </div>
    );
  }

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
        <h2 className="text-xl font-bold text-gray-900 mb-2">구독이 완료되었습니다!</h2>
        <p className="text-gray-500 mb-2">프리미엄 기능이 활성화되었습니다.</p>
        <p className="text-gray-400 text-sm mb-8">
          이제 매물 등록과 프리미엄 데이터 분석을 이용할 수 있습니다.
        </p>

        <div className="flex gap-3 justify-center">
          <button
            onClick={() => router.push('/')}
            className="px-5 py-2.5 rounded-xl border border-gray-300 text-gray-700 hover:bg-gray-50 transition-colors font-medium"
          >
            홈으로
          </button>
          <button
            onClick={() => router.push('/my_page')}
            className="px-5 py-2.5 rounded-xl bg-blue-600 text-white hover:bg-blue-700 transition-colors font-medium"
          >
            마이페이지
          </button>
        </div>
      </div>
    </div>
  );
}
