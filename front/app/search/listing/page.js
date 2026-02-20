import Link from 'next/link';

export default function SearchListingPage() {
  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="max-w-3xl mx-auto bg-white border border-gray-200 rounded-xl p-8">
        <h1 className="text-xl font-semibold text-gray-900 mb-2">매물 목록 페이지</h1>
        <p className="text-sm text-gray-600 mb-6">
          현재 프로젝트에서는 목록 페이지가 아직 구현되지 않았습니다. 아래 버튼으로 지도 검색으로 이동해 주세요.
        </p>

        <div className="flex flex-col sm:flex-row gap-3">
          <Link
            href="/search"
            className="inline-flex items-center justify-center px-4 py-2 rounded-lg bg-gray-900 text-white hover:bg-gray-800"
          >
            검색 조건 페이지로 이동
          </Link>
          <Link
            href="/search/map"
            className="inline-flex items-center justify-center px-4 py-2 rounded-lg bg-blue-600 text-white hover:bg-blue-700"
          >
            지도 검색으로 이동
          </Link>
        </div>
      </div>
    </div>
  );
}
