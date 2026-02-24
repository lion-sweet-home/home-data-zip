"use client";

import { Suspense } from "react";
import { useSearchParams, useRouter } from "next/navigation";

function BillingFailContent() {
  const searchParams = useSearchParams();
  const router = useRouter();

  const code = searchParams.get("code");
  const message = searchParams.get("message");

  return (
    <div style={{ padding: 40 }}>
      <h2>카드 등록 실패</h2>
      <p>code: {code}</p>
      <p>message: {message}</p>

      <button onClick={() => router.replace("/subscription")}>다시 시도</button>
    </div>
  );
}

export default function BillingFailPage() {
  return (
    <Suspense fallback={<div style={{ padding: 40 }}>로딩 중...</div>}>
      <BillingFailContent />
    </Suspense>
  );
}