'use client';

import { useEffect, useState, Suspense } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import Link from 'next/link';
import { reissue } from '../../../api/auth';

function OAuthCallbackContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [status, setStatus] = useState('loading'); // 'loading' | 'success' | 'error'
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    const error = searchParams.get('error');
    if (error) {
      setErrorMessage(decodeURIComponent(error));
      setStatus('error');
      return;
    }

    const token = searchParams.get('token');
    if (token) {
      if (typeof window !== 'undefined') {
        localStorage.setItem('accessToken', token);
        window.dispatchEvent(new Event('auth:changed'));
      }
      setStatus('success');
      router.push('/');
      router.refresh();
      return;
    }

    let mounted = true;
    reissue()
      .then((response) => {
        if (!mounted) return;
        if (response?.AccessToken ?? response?.accessToken) {
          setStatus('success');
          router.push('/');
          router.refresh();
        } else {
          setStatus('error');
          setErrorMessage('로그인에 실패했습니다. 다시 시도해 주세요.');
        }
      })
      .catch((err) => {
        if (!mounted) return;
        setStatus('error');
        setErrorMessage(err?.message || '로그인에 실패했습니다. 다시 시도해 주세요.');
      });

    return () => { mounted = false; };
  }, [searchParams, router]);

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
