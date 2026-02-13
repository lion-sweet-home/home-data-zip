import "./globals.css";
import Header from "./components/header";

export const metadata = {
  title: "HomeDataZip",
  description: "부동산 정보 검색 플랫폼",
};

export default function RootLayout({ children }) {
  return (
    <html lang="ko">
      <body className="antialiased">
        <Header />
        <main>{children}</main>
      </body>
    </html>
  );
}
