"use client";

import { Suspense, useEffect, useRef } from "react";
import { useRouter, useSearchParams } from "next/navigation";

function BillingSuccessContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const calledRef = useRef(false);

  useEffect(() => {
    // ✅ dev StrictMode에서 useEffect 2번 도는 거 방지
    if (calledRef.current) return;
    calledRef.current = true;

    const authKey = searchParams.get("authKey");
    const customerKey = searchParams.get("customerKey");

    if (!authKey || !customerKey) {
      alert("잘못된 접근입니다. (authKey/customerKey 없음)");
      router.replace("/subscription");
      return;
    }

    // ✅ accessToken 저장 위치가 localStorage 기준 (너네 프로젝트가 다르면 여기만 바꿔)
    const accessToken =
      localStorage.getItem("accessToken") ||
      localStorage.getItem("ACCESS_TOKEN") ||
      "";

    if (!accessToken) {
      alert("로그인이 필요합니다. (accessToken 없음)");
      router.replace("/subscription");
      return;
    }

    fetch("http://localhost:8080/api/payments/billing-key/confirm", {
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