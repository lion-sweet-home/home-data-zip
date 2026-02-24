"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";

export default function SubscribeRedirectPage() {
  const router = useRouter();

  useEffect(() => {
    router.replace("/subscription");
  }, [router]);

  return <div style={{ padding: 40 }}>이동 중...</div>;
}
