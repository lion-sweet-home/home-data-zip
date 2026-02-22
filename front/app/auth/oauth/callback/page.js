'use client';

import { useEffect, useState, Suspense } from 'react';
import { useSearchParams } from 'next/navigation';
import Link from 'next/link';
import { oauthWithCode, getOAuthRedirectUri } from '../../../api/auth';

function OAuthCallbackContent() {
  const searchParams = useSearchParams();
  const [status, setStatus] = useState('loading'); // 'loading' | 'success' | 'error'
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    if (typeof window === 'undefined') return;

    const params = new URLSearchParams(window.location.search);
    const error = searchParams.get('error') ?? params.get('error');
    if (error) {
      const message = decodeURIComponent(error) || '소셜 로그인에 실패했습니다.';
      window.location.replace(`/auth/login?error=${encodeURIComponent(message)}`);
      return;
    }

    const code = searchParams.get('code') ?? params.get('code');
    if (code) {
      const redirectUri = getOAuthRedirectUri();
      let mounted = true;
      oauthWithCode(code, redirectUri)
        .then(() => {
          if (!mounted) return;
          window.history.replaceState({}, '', window.location.pathname);
          window.location.replace('/');
        })
        .catch((err) => {
          if (!mounted) return;
          const message = err?.message || '소셜 로그인에 실패했습니다. 다시 시도해 주세요.';
          window.location.replace(`/auth/login?error=${encodeURIComponent(message)}`);
        });
      return () => { mounted = false; };
    }

    if (localStorage.getItem('accessToken')) {
      window.location.replace('/');
      return;
    }

    // OAuth 콜백인데 code도 없고 토큰도 없으면 reissue 호출 없이 로그인 페이지로
    window.location.replace('/auth/login');
  }, [searchParams]);

  if (status === 'loading') {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
        <div className="text-center">
          <div className="inline-block w-10 h-10 border-4 border-blue-600 border-t-transparent rounded-full animate-spin mb-4" />
          <p className="text-gray-600">로그인 처리 중...</p>
        </div>
      </div>
    );
  }

  if (status === 'error') {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
        <div className="w-full max-w-md bg-white rounded-lg shadow-md p-8 text-center">
          <h1 className="text-xl font-bold text-gray-900 mb-2">소셜 로그인 실패</h1>
          <p className="text-gray-600 mb-6">{errorMessage}</p>
          <Link
            href="/auth/login"
            className="inline-block w-full py-3 bg-blue-600 text-white font-medium rounded-lg hover:bg-blue-700 transition-colors"
          >
            로그인 페이지로 돌아가기
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
      <div className="text-center">
        <p className="text-gray-600">로그인 성공. 이동 중...</p>
      </div>
    </div>
  );
}

export default function OAuthCallbackPage() {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
          <div className="text-center">
            <div className="inline-block w-10 h-10 border-4 border-blue-600 border-t-transparent rounded-full animate-spin mb-4" />
            <p className="text-gray-600">처리 중...</p>
          </div>
        </div>
      }
    >
      <OAuthCallbackContent />
    </Suspense>
  );
}
