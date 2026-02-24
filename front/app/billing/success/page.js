"use client";

import { Suspense, useEffect, useRef } from "react";
import { useRouter, useSearchParams } from "next/navigation";

function BillingSuccessContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const calledRef = useRef(false);

  useEffect(() => {
    if (calledRef.current) return;
    calledRef.current = true;

    const authKey = searchParams.get("authKey");
    const customerKey = searchParams.get("customerKey");

    if (!authKey || !customerKey) {
      alert("잘못된 접근입니다. (authKey/customerKey 없음)");
      router.replace("/subscription");
      return;
    }

    const accessToken =
      localStorage.getItem("accessToken") ||
      localStorage.getItem("ACCESS_TOKEN") ||
      "";

    if (!accessToken) {
      alert("로그인이 필요합니다. (accessToken 없음)");
      router.replace("/subscription");
      return;
    }

    // ✅ 배포/로컬 모두 대응: env 있으면 env 쓰고, 없으면 기본값(EC2) 사용
    const API_BASE =
      process.env.NEXT_PUBLIC_API_BASE_URL || "http://homedatazip.duckdns.org:8080";

    fetch(`${API_BASE}/api/payments/billing-key/confirm`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${accessToken}`,
      },
      body: JSON.stringify({ authKey, customerKey }),
      credentials: "include",
    })
      .then(async (res) => {
        const text = await res.text().catch(() => "");
        if (!res.ok) throw new Error(`${res.status} ${text}`);
        alert("카드 등록 완료");
        router.replace("/subscription");
      })
      .catch((err) => {
        console.error(err);
        alert("카드 등록 처리 실패");
        router.replace("/subscription");
      });
  }, [router, searchParams]);

  return <div style={{ padding: 40 }}>카드 등록 처리 중...</div>;
}

export default function BillingSuccessPage() {
  return (
    <Suspense fallback={<div style={{ padding: 40 }}>카드 등록 처리 중...</div>}>
      <BillingSuccessContent />
    </Suspense>
  );
}