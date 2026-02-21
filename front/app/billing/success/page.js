"use client";

import { useSearchParams } from "next/navigation";
import { useEffect } from "react";

export default function BillingSuccessPage() {
  const searchParams = useSearchParams();

  useEffect(() => {
    const authKey = searchParams.get("authKey");
    const customerKey = searchParams.get("customerKey");

    fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}/api/payments/billing-key/confirm`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ authKey, customerKey }),
      credentials: "include",
    })
      .then(() => {
        alert("카드 등록 완료");
        window.location.href = "/subscribe";
      })
      .catch(console.error);
  }, []);

  return <div>카드 등록 처리 중...</div>;
}
