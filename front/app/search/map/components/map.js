'use client';

import { useEffect, useRef, useState } from 'react';

export default function Map({ 
  center = { lat: 37.5665, lng: 126.9780 }, // 기본값: 서울시청
  markers = [], // 마커 데이터 배열 [{ lat, lng, title, info }]
  level = 3, // 지도 확대 레벨 (1-14)
  onMarkerClick, // 마커 클릭 핸들러
  onMapClick, // 지도 클릭 핸들러
  busMarkers = [], // 버스 정류장 마커
  showBusMarkers = false, // 버스 마커 표시 여부
  schoolMarkers = [], // 학교 마커
  showSchoolMarkers = false, // 학교 마커 표시 여부
}) {
  const mapRef = useRef(null);
  const [map, setMap] = useState(null);
  const [kakaoLoaded, setKakaoLoaded] = useState(false);
  const markersRef = useRef([]);
  // NOTE: 컴포넌트명이 Map이라 전역 Map 생성자와 이름이 충돌할 수 있어 globalThis.Map을 사용한다.
  const numberedMarkerImageCacheRef = useRef(new globalThis.Map()); // key: number(string) -> kakao.maps.MarkerImage
  const busMarkersRef = useRef([]);
  const schoolMarkersRef = useRef([]);

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

    // 기존 마커 제거
    markersRef.current.forEach((marker) => {
      marker.setMap(null);
    });
    markersRef.current = [];

    // 새 마커 추가
    markers.forEach((markerData, index) => {
      const position = new window.kakao.maps.LatLng(markerData.lat, markerData.lng);
      const markerImage = getNumberedMarkerImage(index + 1);

      const marker = new window.kakao.maps.Marker({
        position: position,
        ...(markerImage ? { image: markerImage } : {}),
        map: map,
        zIndex: 10,
      });

      // 인포윈도우 (선택사항)
      if (markerData.title || markerData.info) {
        const infowindow = new window.kakao.maps.InfoWindow({
          content: `<div style="padding:5px;font-size:12px;white-space:nowrap;">${markerData.title || ''}${markerData.info ? `<br/>${markerData.info}` : ''}</div>`,
        });

        // 마커 클릭 이벤트
        window.kakao.maps.event.addListener(marker, 'click', () => {
          if (onMarkerClick) {
            onMarkerClick(markerData, index);
          }
          infowindow.open(map, marker);
        });
      } else if (onMarkerClick) {
        // 인포윈도우가 없어도 클릭 이벤트는 등록
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
  }, [map, markers, kakaoLoaded, onMarkerClick]);

  // 버스 정류장 마커 표시
  useEffect(() => {
    if (!map || !kakaoLoaded || !showBusMarkers) {
      // 버스 마커 숨기기
      busMarkersRef.current.forEach((marker) => {
        marker.setMap(null);
      });
      busMarkersRef.current = [];
      return;
    }

    // 기존 버스 마커 제거
    busMarkersRef.current.forEach((marker) => {
      marker.setMap(null);
    });
    busMarkersRef.current = [];

    // 버스 정류장 마커 추가
    busMarkers.forEach((busData) => {
      const position = new window.kakao.maps.LatLng(busData.latitude, busData.longitude);
      
      // 버스 정류장 마커 이미지 (빨간색)
      const busMarkerImage = new window.kakao.maps.MarkerImage(
        'https://t1.daumcdn.net/localimg/localimages/07/mapapidoc/marker_red.png',
        new window.kakao.maps.Size(24, 35),
        {
          offset: new window.kakao.maps.Point(12, 35),
        }
      );

      const marker = new window.kakao.maps.Marker({
        position: position,
        image: busMarkerImage,
        map: map,
        zIndex: 1, // 아파트 마커보다 아래에 표시
      });

      // 버스 정류장 인포윈도우
      if (busData.name) {
        const infowindow = new window.kakao.maps.InfoWindow({
          content: `<div style="padding:5px;font-size:12px;white-space:nowrap;">${busData.name}</div>`,
        });

        window.kakao.maps.event.addListener(marker, 'click', () => {
          infowindow.open(map, marker);
        });
      }

      busMarkersRef.current.push(marker);
    });
  }, [map, busMarkers, showBusMarkers, kakaoLoaded]);

  // 학교 마커 표시
  useEffect(() => {
    if (!map || !kakaoLoaded || !showSchoolMarkers) {
      schoolMarkersRef.current.forEach((marker) => {
        marker.setMap(null);
      });
      schoolMarkersRef.current = [];
      return;
    }

    schoolMarkersRef.current.forEach((marker) => {
      marker.setMap(null);
    });
    schoolMarkersRef.current = [];

    schoolMarkers.forEach((school) => {
      const lat = school.latitude ?? school.lat;
      const lng = school.longitude ?? school.lng;
      if (lat == null || lng == null) return;

      const position = new window.kakao.maps.LatLng(lat, lng);
      const schoolMarkerImage = new window.kakao.maps.MarkerImage(
        'https://t1.daumcdn.net/localimg/localimages/07/mapapidoc/markerStar.png',
        new window.kakao.maps.Size(24, 35),
        { offset: new window.kakao.maps.Point(12, 35) }
      );

      const marker = new window.kakao.maps.Marker({
        position,
        image: schoolMarkerImage,
        map,
        zIndex: 2,
      });

      const name = school.schoolName || school.name;
      if (name) {
        const infowindow = new window.kakao.maps.InfoWindow({
          content: `<div style="padding:5px;font-size:12px;white-space:nowrap;">${name}</div>`,
        });
        window.kakao.maps.event.addListener(marker, 'click', () => {
          infowindow.open(map, marker);
        });
      }

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
