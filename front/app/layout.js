import "./globals.css";
import Header from "./components/header";
import Script from "next/script";

export const metadata = {
  title: "HomeDataZip",
  description: "부동산 정보 검색 플랫폼",
};

export default function RootLayout({ children }) {
  return (
    <html lang="ko">
      <body className="antialiased">
        {/* TossPayments SDK 로드 (App Router에서 이 방식이 제일 안정적) */}
        <Script
          src="https://js.tosspayments.com/v1"
          strategy="beforeInteractive"
        />

        <Header />
        <main>{children}</main>
      </body>
    </html>
  );
}