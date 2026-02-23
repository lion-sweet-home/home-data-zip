"use client";

import { Suspense, useEffect } from "react";
import { useSearchParams } from "next/navigation";
function BillingSuccessContent() {
  const router = useRouter();
  const searchParams = useSearchParams();

  useEffect(() => {
    const authKey = searchParams.get("authKey");
    const customerKey = searchParams.get("customerKey");

    // 파라미터 없으면 바로 튕겨 (잘못 들어온 케이스)
    if (!authKey || !customerKey) {
      alert("잘못된 접근입니다. (authKey/customerKey 없음)");
      router.replace("/subscription");
      return;
    }

    fetch(
      `${process.env.NEXT_PUBLIC_API_BASE_URL}/api/payments/billing-key/confirm`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ authKey, customerKey }),
        credentials: "include",
      }
    )
      .then(async (res) => {
        if (!res.ok) {
          const text = await res.text().catch(() => "");
          throw new Error(`billing confirm failed: ${res.status} ${text}`);
        }
        alert("카드 등록 완료");
        router.replace("/subscription"); 
      })
      .catch((err) => {
        console.error(err);
        alert("카드 등록 처리 실패");
        router.replace("/subscription");
      });
  }, [searchParams, router]);

  return <div>카드 등록 처리 중...</div>;
}

export default function BillingSuccessPage() {
  return (
    <Suspense fallback={<div style={{ padding: 40 }}>카드 등록 처리 중...</div>}>
      <BillingSuccessContent />
    </Suspense>
  );
}