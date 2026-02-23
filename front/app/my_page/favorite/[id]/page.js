'use client';

import { useEffect } from 'react';
import { useParams, useRouter } from 'next/navigation';

export default function FavoriteDetailRedirect() {
  const params = useParams();
  const router = useRouter();

  useEffect(() => {
    router.replace(`/search/listing/${params.id}`);
  }, [params.id, router]);

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center">
      <div className="flex flex-col items-center gap-4">
        <svg className="animate-spin h-8 w-8 text-blue-500" viewBox="0 0 24 24" fill="none">
          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
        </svg>
        <p className="text-gray-500 text-sm">매물 상세 페이지로 이동 중...</p>
      </div>
    </div>
  );
}
