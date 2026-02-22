'use client';

import { useEffect, useRef, useState } from 'react';

export default function Map({ 
  center = { lat: 37.5665, lng: 126.9780 }, // 기본값: 서울시청
  markers = [], // 마커 데이터 배열 [{ lat, lng, title, info }]
  level = 3, // 지도 확대 레벨 (1-14)
  onMarkerClick, // 마커 클릭 핸들러
  onMapClick, // 지도 클릭 핸들러
  onMapReady, // map 객체 생성 시 콜백
  onIdle, // 지도 idle 시 bounds/level 전달 콜백
  useCluster = true, // MarkerClusterer 사용 여부
  autoFitBounds = false, // markers 변경 시 지도 bounds 자동 맞춤(기본 off: bounds 기반 마커 갱신 시 루프 방지)
  schoolMarkers = [], // 학교 마커
  showSchoolMarkers = false, // 학교 마커 표시 여부
}) {
  const mapRef = useRef(null);
  const [map, setMap] = useState(null);
  const [kakaoLoaded, setKakaoLoaded] = useState(() => {
    // eslint(rule react-hooks/set-state-in-effect): 이미 로드된 케이스는 초기값으로 처리
    return Boolean(window?.kakao?.maps);
  });
  const markersRef = useRef([]);
  const clustererRef = useRef(null);
  // NOTE: 컴포넌트명이 Map이라 전역 Map 생성자와 이름이 충돌할 수 있어 globalThis.Map을 사용한다.
  const numberedMarkerImageCacheRef = useRef(new globalThis.Map()); // key: number(string) -> kakao.maps.MarkerImage
  const schoolMarkerImageCacheRef = useRef(null); // kakao.maps.MarkerImage
  const schoolMarkersRef = useRef([]);
  const schoolLabelOverlaysRef = useRef([]); // kakao.maps.CustomOverlay[]
  const schoolInfoWindowRef = useRef(null); // kakao.maps.InfoWindow

  const getNumberedMarkerImage = (num) => {
    const key = String(num);
    const cached = numberedMarkerImageCacheRef.current.get(key);
    if (cached) return cached;

    const size = 34;
    const canvas = document.createElement('canvas');
    canvas.width = size;
    canvas.height = size;
    const ctx = canvas.getContext('2d');
    if (!ctx) return null;

    // shadow
    ctx.shadowColor = 'rgba(0,0,0,0.25)';
    ctx.shadowBlur = 6;
    ctx.shadowOffsetX = 0;
    ctx.shadowOffsetY = 2;

    // circle
    const r = 13;
    const cx = size / 2;
    const cy = size / 2;
    ctx.beginPath();
    ctx.arc(cx, cy, r, 0, Math.PI * 2);
    ctx.fillStyle = '#2563eb';
    ctx.fill();

    // border
    ctx.shadowColor = 'transparent';
    ctx.lineWidth = 2;
    ctx.strokeStyle = '#ffffff';
    ctx.stroke();

    // number
    ctx.fillStyle = '#ffffff';
    ctx.font = 'bold 12px system-ui, -apple-system, Segoe UI, Roboto, sans-serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText(key, cx, cy + 0.5);

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
    // 이미 로드되어 있으면 effect에서는 아무것도 하지 않는다.
    if (kakaoLoaded) return;

    // 스크립트 동적 로드
    const script = document.createElement('script');
    const apiKey = process.env.NEXT_PUBLIC_KAKAO_MAP_API_KEY || '430da516c7ace85abd6ad3bcdc00b4de';
    // clusterer 사용을 위해 libraries=clusterer 추가
    script.src = `//dapi.kakao.com/v2/maps/sdk.js?appkey=${apiKey}&autoload=false&libraries=clusterer`;
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
  }, [kakaoLoaded]);

  // 지도 초기화
  useEffect(() => {
    if (!kakaoLoaded || !mapRef.current) return;
    if (map) return; // 이미 생성됨

    const container = mapRef.current;
    const options = {
      center: new window.kakao.maps.LatLng(center.lat, center.lng),
      level: level,
    };

    const kakaoMap = new window.kakao.maps.Map(container, options);
    setMap(kakaoMap);
    onMapReady?.(kakaoMap);

  }, [kakaoLoaded, map, center.lat, center.lng, level, onMapReady]);

  // center/level 변경 시 map에 반영 (map 재생성 금지)
  useEffect(() => {
    if (!map) return;
    const nextCenter = new window.kakao.maps.LatLng(center.lat, center.lng);
    map.setCenter(nextCenter);
  }, [map, center.lat, center.lng]);

  useEffect(() => {
    if (!map) return;
    if (typeof level !== 'number') return;
    if (map.getLevel?.() === level) return;
    map.setLevel(level);
  }, [map, level]);

  // 지도 클릭 이벤트
  useEffect(() => {
    if (!map || !kakaoLoaded || !onMapClick) return;

    const handler = (mouseEvent) => {
      const latlng = mouseEvent.latLng;
      onMapClick({
        lat: latlng.getLat(),
        lng: latlng.getLng(),
      });
    };

    window.kakao.maps.event.addListener(map, 'click', handler);
    return () => {
      window.kakao.maps.event.removeListener(map, 'click', handler);
    };
  }, [map, kakaoLoaded, onMapClick]);

  // idle 이벤트: bounds + level을 부모로 전달
  useEffect(() => {
    if (!map || !kakaoLoaded || !onIdle) return;

    const emit = () => {
      try {
        const b = map.getBounds();
        const sw = b.getSouthWest();
        const ne = b.getNorthEast();
        const c = map.getCenter();

        onIdle({
          bounds: {
            south: sw.getLat(),
            west: sw.getLng(),
            north: ne.getLat(),
            east: ne.getLng(),
          },
          level: map.getLevel?.(),
          center: { lat: c.getLat(), lng: c.getLng() },
        });
      } catch (e) {
        // ignore
      }
    };

    window.kakao.maps.event.addListener(map, 'idle', emit);
    // 최초 1회도 호출(초기 마커 로딩 트리거용)
    emit();

    return () => {
      window.kakao.maps.event.removeListener(map, 'idle', emit);
    };
  }, [map, kakaoLoaded, onIdle]);

  // 마커 표시
  useEffect(() => {
    if (!map || !kakaoLoaded) return;

    // 기존 마커/클러스터 제거
    try {
      if (clustererRef.current?.clear) {
        clustererRef.current.clear();
      }
    } catch (e) {
      // ignore
    }

    markersRef.current.forEach((marker) => marker.setMap(null));
    markersRef.current = [];

    // clusterer 생성/유지
    if (useCluster && window.kakao?.maps?.MarkerClusterer && !clustererRef.current) {
      clustererRef.current = new window.kakao.maps.MarkerClusterer({
        map,
        averageCenter: true,
        // level이 클수록(더 축소) 클러스터링이 필요해짐
        // 요구사항: level >= 6부터 클러스터 사용
        minLevel: 6,
      });
    }

    const newMarkers = [];

    // 새 마커 추가(클러스터에 넣거나 직접 map에 올림)
    (markers || []).forEach((markerData, index) => {
      const position = new window.kakao.maps.LatLng(markerData.lat, markerData.lng);
      const markerImage = getNumberedMarkerImage(index + 1);

      const marker = new window.kakao.maps.Marker({
        position,
        ...(markerImage ? { image: markerImage } : {}),
        ...(useCluster ? {} : { map }),
        zIndex: 10,
      });

      if (onMarkerClick) {
        window.kakao.maps.event.addListener(marker, 'click', () => {
          onMarkerClick(markerData, index);
        });
      }

      newMarkers.push(marker);
      markersRef.current.push(marker);
    });

    if (useCluster && clustererRef.current?.addMarkers) {
      clustererRef.current.addMarkers(newMarkers);
    }

    // 필요 시 1회만 bounds 맞춤(기본 off)
    if (autoFitBounds && markers.length > 0) {
      const b = new window.kakao.maps.LatLngBounds();
      (markers || []).forEach((m) => {
        b.extend(new window.kakao.maps.LatLng(m.lat, m.lng));
      });
      map.setBounds(b);
    }
  }, [map, markers, kakaoLoaded, onMarkerClick, useCluster, autoFitBounds]);

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
