'use client';

function toNumber(value) {
  const n = Number(value);
  return Number.isFinite(n) ? n : 0;
}

export default function SchoolForm({ schools = [] }) {
  // 거리 기준 오름차순 정렬 (distanceKm이 null이거나 undefined인 경우는 맨 뒤로)
  const sortedSchools = [...schools].sort((a, b) => {
    const distA = a?.distanceKm != null ? toNumber(a.distanceKm) : Infinity;
    const distB = b?.distanceKm != null ? toNumber(b.distanceKm) : Infinity;
    return distA - distB;
  });

  return (
    <div className="bg-white border border-gray-200 rounded-xl p-5">
      <div className="text-sm font-semibold text-gray-900 mb-4">학교 정보</div>
      <div className="space-y-2">
        {sortedSchools.length > 0 ? (
          sortedSchools.map((school) => (
            <div key={school.schoolId} className="p-3 rounded-lg bg-gray-50 border border-gray-100">
              <div className="flex items-center justify-between">
                <div className="flex-1">
                  <div className="font-semibold text-gray-900">{school.schoolName}</div>
                  <div className="text-sm text-gray-600 mt-1">{school.schoolLevel || '-'}</div>
                </div>
                <div className="text-2xl font-semibold text-blue-600 text-right ml-4">
                  {school.distanceKm != null ? `${Math.round(toNumber(school.distanceKm) * 1000)}m` : '-'}
                </div>
              </div>
            </div>
          ))
        ) : (
          <div className="text-sm text-gray-500 py-8 text-center">학교 정보가 없습니다.</div>
        )}
      </div>
    </div>
  );
}
