const config = {
  plugins: {
    // Tailwind v4 PostCSS 플러그인
    // (Next/Turbopack에서 `tailwindcss`를 직접 플러그인으로 쓰면 에러가 나므로 분리 패키지를 사용)
    "@tailwindcss/postcss": {},
  },
};

export default config;
