'use client';

export default function ApartmentList({ markers = [], onSelect }) {
  if (!markers || markers.length === 0) {
    return (
      <div className="w-full h-full flex items-center justify-center p-6">
        <div className="text-gray-500 text-sm">검색 결과가 없습니다.</div>
      </div>
    );
  }

  return (
    <div className="w-full h-full overflow-y-auto bg-white">
      <div className="p-4 border-b">
        <div className="text-lg font-semibold text-gray-900">아파트 목록</div>
        <div className="text-sm text-gray-600 mt-1">총 {markers.length}개</div>
        <div className="text-xs text-gray-500 mt-1">
          지도에 찍힌 <span className="font-medium">번호</span>와 동일한 순서입니다.
        </div>
      </div>

      <div className="divide-y">
        {markers.map((m, idx) => {
          const apt = m.apartmentData || m;
          const name = m.title || apt.aptNm || apt.aptName || apt.name || '아파트';
          const address = apt.roadAddress || apt.jibunAddress || apt.address || '';
          const subInfo =
            m.info ||
            (apt.distanceKm != null ? `역까지 ${Number(apt.distanceKm).toFixed(2)}km` : '');

          return (
            <button
              key={m.apartmentId ?? apt.aptId ?? apt.apartmentId ?? idx}
              type="button"
              onClick={() => onSelect?.(m, idx)}
              className="w-full text-left p-4 hover:bg-gray-50 transition-colors"
            >
              <div className="flex items-start gap-3">
                <div className="flex-shrink-0 w-7 h-7 rounded-full bg-blue-600 text-white text-sm font-semibold flex items-center justify-center">
                  {idx + 1}
                </div>
                <div className="min-w-0 flex-1">
                  <div className="font-medium text-gray-900 truncate">{name}</div>
                  {address && (
                    <div className="text-xs text-gray-600 mt-1 line-clamp-2">{address}</div>
                  )}
                  {subInfo && (
                    <div className="text-xs text-blue-600 mt-1">{subInfo}</div>
                  )}
                </div>
              </div>
            </button>
          );
        })}
      </div>
    </div>
  );
}

