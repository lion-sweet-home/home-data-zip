'use client';

import { useEffect, useRef, useState } from 'react';

export default function Map({
  center = { lat: 37.5665, lng: 126.9780 }, // 기본값: 서울시청
  markers = [], // 마커 데이터 배열 [{ lat, lng, title, info }]
  level = 3, // 지도 확대 레벨 (1-14)
  onMarkerClick, // 마커 클릭 핸들러
  onMapClick, // 지도 클릭 핸들러
  schoolMarkers = [], // 학교 마커
  showSchoolMarkers = false, // 학교 마커 표시 여부
  selectedMarkerId = null, // 선택된 아파트 마커 ID (apartmentId 등) – 선택 시 강조 표시
}) {
  const mapRef = useRef(null);
  const [map, setMap] = useState(null);
  const [kakaoLoaded, setKakaoLoaded] = useState(false);
  const markersRef = useRef([]);
  const apartmentLabelOverlaysRef = useRef([]); // 아파트명 상시 표시용 CustomOverlay[]
  // NOTE: 컴포넌트명이 Map이라 전역 Map 생성자와 이름이 충돌할 수 있어 globalThis.Map을 사용한다.
  const numberedMarkerImageCacheRef = useRef(new globalThis.Map()); // key: "num" | "num-selected" -> kakao.maps.MarkerImage
  const schoolMarkerImageCacheRef = useRef(null); // kakao.maps.MarkerImage
  const schoolMarkersRef = useRef([]);
  const schoolLabelOverlaysRef = useRef([]); // kakao.maps.CustomOverlay[]
  const schoolInfoWindowRef = useRef(null); // kakao.maps.InfoWindow

  const getNumberedMarkerImage = (num, isSelected) => {
    const key = `${String(num)}-${isSelected ? 'selected' : 'normal'}`;
    const cached = numberedMarkerImageCacheRef.current.get(key);
    if (cached) return cached;

    const size = isSelected ? 40 : 34;
    const canvas = document.createElement('canvas');
    canvas.width = size;
    canvas.height = size;
    const ctx = canvas.getContext('2d');
    if (!ctx) return null;

    // shadow
    ctx.shadowColor = 'rgba(0,0,0,0.25)';
    ctx.shadowBlur = isSelected ? 8 : 6;
    ctx.shadowOffsetX = 0;
    ctx.shadowOffsetY = 2;

    // circle
    const r = isSelected ? 16 : 13;
    const cx = size / 2;
    const cy = size / 2;
    ctx.beginPath();
    ctx.arc(cx, cy, r, 0, Math.PI * 2);
    ctx.fillStyle = isSelected ? '#ea580c' : '#2563eb'; // 선택: orange-600, 기본: blue-600
    ctx.fill();

    // border (선택 시 더 두껍게)
    ctx.shadowColor = 'transparent';
    ctx.lineWidth = isSelected ? 3 : 2;
    ctx.strokeStyle = '#ffffff';
    ctx.stroke();

    // number
    ctx.fillStyle = '#ffffff';
    ctx.font = isSelected ? 'bold 14px system-ui, -apple-system, Segoe UI, Roboto, sans-serif' : 'bold 12px system-ui, -apple-system, Segoe UI, Roboto, sans-serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText(String(num), cx, cy + 0.5);

    const dataUrl = canvas.toDataURL('image/png');
    const markerImage = new window.kakao.maps.MarkerImage(
      dataUrl,
      new window.kakao.maps.Size(size, size),
      { offset: new window.kakao.maps.Point(size / 2, size / 2) }
    );

    numberedMarkerImageCacheRef.current.set(key, markerImage);
    return markerImage;
  };

  const escapeHtml = (value) => {
    if (value == null) return '';
    return String(value)
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#039;');
  };

  const getSchoolMarkerImage = () => {
    if (schoolMarkerImageCacheRef.current) return schoolMarkerImageCacheRef.current;
    if (!window.kakao?.maps) return null;

    // "학교" 아이콘 SVG (옵션 2: 동그란 배지형)
    // - 아파트(파란색)와 구분되도록 초록 계열 + 굵은 흰 테두리
    // - 원형 마커는 좌표 중심에 오도록 offset을 중앙으로 설정
    const size = 32;
    const main = '#16A34A'; // green-600
    const svg = `
      <svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 64 64">
        <defs>
          <filter id="s" x="-20%" y="-20%" width="140%" height="140%">
            <feDropShadow dx="0" dy="2" stdDeviation="2" flood-color="rgba(0,0,0,0.35)"/>
          </filter>
        </defs>
        <g filter="url(#s)">
          <!-- badge -->
          <circle cx="32" cy="32" r="22" fill="${main}" />
          <circle cx="32" cy="32" r="22" fill="none" stroke="#FFFFFF" stroke-width="5" />

          <!-- school pictogram -->
          <path d="M32 20 22 25v3h20v-3L32 20z" fill="#FFFFFF" opacity="0.95"/>
          <path d="M24.5 29.5h15v15h-15v-15z" fill="none" stroke="#FFFFFF" stroke-width="3" />
          <path d="M29 44v-8h6v8" fill="#FFFFFF" opacity="0.25" stroke="#FFFFFF" stroke-width="3"/>
          <circle cx="32" cy="33" r="2.1" fill="#FFFFFF"/>
        </g>
      </svg>
    `.trim();
    const dataUrl = `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`;
    const markerImage = new window.kakao.maps.MarkerImage(
      dataUrl,
      new window.kakao.maps.Size(size, size),
      { offset: new window.kakao.maps.Point(size / 2, size / 2) } // 원형은 중심이 좌표에 오도록
    );
    schoolMarkerImageCacheRef.current = markerImage;
    return markerImage;
  };

  const buildSchoolLabelText = (schoolsAtSamePoint) => {
    const names = (schoolsAtSamePoint || [])
      .map((s) => s?.schoolName || s?.name)
      .map((n) => (n == null ? '' : String(n).trim()))
      .filter(Boolean);

    if (names.length === 0) return '';
    if (names.length <= 2) return names.join(', ');
    // 너무 길어지는 걸 방지: 처음 2개 + 외 N
    return `${names[0]}, ${names[1]} 외 ${names.length - 2}`;
  };

  const buildSchoolInfoHtml = (schoolsAtSamePoint) => {
    const names = (schoolsAtSamePoint || [])
      .map((s) => s?.schoolName || s?.name)
      .map((n) => (n == null ? '' : String(n).trim()))
      .filter(Boolean);

    if (names.length === 0) return '';
    const items = names.map((n) => `<div style="padding:2px 0;">- ${escapeHtml(n)}</div>`).join('');
    return `
      <div style="padding:8px 10px;font-size:12px;line-height:1.3;max-width:240px;">
        <div style="font-weight:700;margin-bottom:6px;">학교</div>
        ${items}
      </div>
    `.trim();
  };

  // Kakao Maps SDK 로드
  useEffect(() => {
    // 이미 로드되어 있는지 확인
    if (window.kakao && window.kakao.maps) {
      setKakaoLoaded(true);
      return;
    }

    // 스크립트 동적 로드
    const script = document.createElement('script');
    const apiKey = process.env.NEXT_PUBLIC_KAKAO_MAP_API_KEY || '430da516c7ace85abd6ad3bcdc00b4de';
    script.src = `//dapi.kakao.com/v2/maps/sdk.js?appkey=${apiKey}&autoload=false`;
    script.async = true;
    
    script.onload = () => {
      window.kakao.maps.load(() => {
        setKakaoLoaded(true);
      });
    };

    document.head.appendChild(script);

    return () => {
      // 컴포넌트 언마운트 시 스크립트 제거
      if (document.head.contains(script)) {
        document.head.removeChild(script);
      }
    };
  }, []);

  // 지도 초기화
  useEffect(() => {
    if (!kakaoLoaded || !mapRef.current) return;

    const container = mapRef.current;
    const options = {
      center: new window.kakao.maps.LatLng(center.lat, center.lng),
      level: level,
    };

    const kakaoMap = new window.kakao.maps.Map(container, options);
    setMap(kakaoMap);

    // 지도 클릭 이벤트
    if (onMapClick) {
      window.kakao.maps.event.addListener(kakaoMap, 'click', (mouseEvent) => {
        const latlng = mouseEvent.latLng;
        onMapClick({
          lat: latlng.getLat(),
          lng: latlng.getLng(),
        });
      });
    }
  }, [kakaoLoaded, center.lat, center.lng, level, onMapClick]);

  // 마커 표시
  useEffect(() => {
    if (!map || !kakaoLoaded) return;

    // 기존 아파트명 라벨 제거
    apartmentLabelOverlaysRef.current.forEach((overlay) => {
      overlay.setMap(null);
    });
    apartmentLabelOverlaysRef.current = [];

    // 기존 마커 제거
    markersRef.current.forEach((marker) => {
      marker.setMap(null);
    });
    markersRef.current = [];

    // 새 마커 추가
    markers.forEach((markerData, index) => {
      const aptId = markerData.apartmentId ?? markerData.apartmentData?.aptId ?? markerData.apartmentData?.apartmentId;
      const isSelected = selectedMarkerId != null && String(aptId) === String(selectedMarkerId);
      const position = new window.kakao.maps.LatLng(markerData.lat, markerData.lng);
      const markerImage = getNumberedMarkerImage(index + 1, isSelected);

      const marker = new window.kakao.maps.Marker({
        position: position,
        ...(markerImage ? { image: markerImage } : {}),
        map: map,
        zIndex: isSelected ? 20 : 10,
      });

      // 아파트명 상시 표시 (마커 위에 라벨)
      const labelText = markerData.title || '(아파트)';
      if (labelText) {
        const labelOverlay = new window.kakao.maps.CustomOverlay({
          position,
          content: `
            <div style="
              transform: translate(-50%, -155%);
              padding: 3px 8px;
              background: rgba(255,255,255,0.96);
              border: 1px solid rgba(0,0,0,0.18);
              border-radius: 999px;
              box-shadow: 0 1px 4px rgba(0,0,0,0.15);
              font-size: 12px;
              color: #111827;
              white-space: nowrap;
              max-width: 260px;
              overflow: hidden;
              text-overflow: ellipsis;
              pointer-events: none;
            ">${escapeHtml(labelText)}</div>
          `.trim(),
          xAnchor: 0,
          yAnchor: 0,
          zIndex: 11,
        });
        labelOverlay.setMap(map);
        apartmentLabelOverlaysRef.current.push(labelOverlay);
      }

      // 마커 클릭: 선택만 (인포윈도우 없음, 아파트명은 라벨로 상시 표시)
      if (onMarkerClick) {
        window.kakao.maps.event.addListener(marker, 'click', () => {
          onMarkerClick(markerData, index);
        });
      }

      markersRef.current.push(marker);
    });

    // 마커가 있으면 지도 중심 조정
    if (markers.length > 0) {
      const bounds = new window.kakao.maps.LatLngBounds();
      markers.forEach((marker) => {
        bounds.extend(new window.kakao.maps.LatLng(marker.lat, marker.lng));
      });
      map.setBounds(bounds);
    }
  }, [map, markers, kakaoLoaded, onMarkerClick, selectedMarkerId]);

  // 학교 마커 표시
  useEffect(() => {
    if (!map || !kakaoLoaded || !showSchoolMarkers) {
      schoolMarkersRef.current.forEach((marker) => {
        marker.setMap(null);
      });
      schoolMarkersRef.current = [];

      schoolLabelOverlaysRef.current.forEach((overlay) => {
        overlay.setMap(null);
      });
      schoolLabelOverlaysRef.current = [];

      if (schoolInfoWindowRef.current) {
        schoolInfoWindowRef.current.close();
      }
      return;
    }

    schoolMarkersRef.current.forEach((marker) => {
      marker.setMap(null);
    });
    schoolMarkersRef.current = [];

    schoolLabelOverlaysRef.current.forEach((overlay) => {
      overlay.setMap(null);
    });
    schoolLabelOverlaysRef.current = [];

    // 같은 좌표(또는 거의 같은 좌표)를 묶어서 한 번에 표시
    const grouped = new globalThis.Map(); // key -> { lat, lng, items: [] }
    (schoolMarkers || []).forEach((school) => {
      const lat = school?.latitude ?? school?.lat;
      const lng = school?.longitude ?? school?.lng;
      if (lat == null || lng == null) return;
      const latNum = Number(lat);
      const lngNum = Number(lng);
      if (!Number.isFinite(latNum) || !Number.isFinite(lngNum)) return;

      // 동일 좌표 판정을 위해 소수점 6자리까지 반올림
      const key = `${latNum.toFixed(6)},${lngNum.toFixed(6)}`;
      if (!grouped.has(key)) {
        grouped.set(key, { lat: latNum, lng: lngNum, items: [] });
      }
      grouped.get(key).items.push(school);
    });

    const schoolMarkerImage = getSchoolMarkerImage();
    if (!schoolInfoWindowRef.current) {
      schoolInfoWindowRef.current = new window.kakao.maps.InfoWindow();
    }

    [...grouped.values()].forEach((group) => {
      const position = new window.kakao.maps.LatLng(group.lat, group.lng);

      const marker = new window.kakao.maps.Marker({
        position,
        ...(schoolMarkerImage ? { image: schoolMarkerImage } : {}),
        map,
        zIndex: 2,
      });

      // 마커 위에 학교 이름(들) 상시 표시
      const labelText = buildSchoolLabelText(group.items);
      if (labelText) {
        const overlay = new window.kakao.maps.CustomOverlay({
          position,
          content: `
            <div style="
              transform: translate(-50%, -155%);
              padding: 3px 8px;
              background: rgba(255,255,255,0.96);
              border: 1px solid rgba(0,0,0,0.18);
              border-radius: 999px;
              box-shadow: 0 1px 4px rgba(0,0,0,0.15);
              font-size: 12px;
              color: #111827;
              white-space: nowrap;
              max-width: 260px;
              overflow: hidden;
              text-overflow: ellipsis;
              pointer-events: none;
            ">${escapeHtml(labelText)}</div>
          `.trim(),
          // 앵커를 (0,0)로 두고, transform으로 중앙 정렬하면 "비스듬함"이 덜 생긴다
          xAnchor: 0,
          yAnchor: 0,
          zIndex: 3,
        });
        overlay.setMap(map);
        schoolLabelOverlaysRef.current.push(overlay);
      }

      // 클릭 시: 같은 좌표의 학교 목록 표시
      window.kakao.maps.event.addListener(marker, 'click', () => {
        const html = buildSchoolInfoHtml(group.items);
        if (!html) return;
        schoolInfoWindowRef.current.setContent(html);
        schoolInfoWindowRef.current.open(map, marker);
      });

      schoolMarkersRef.current.push(marker);
    });
  }, [map, schoolMarkers, showSchoolMarkers, kakaoLoaded]);

  return (
    <div className="w-full h-full relative">
      <div ref={mapRef} className="w-full h-full" />
      {!kakaoLoaded && (
        <div className="absolute inset-0 flex items-center justify-center bg-gray-100">
          <div className="text-gray-500">지도를 불러오는 중...</div>
        </div>
      )}
    </div>
  );
}
