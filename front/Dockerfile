# 1. 빌드 스테이지
FROM node:20-alpine AS build
WORKDIR /app

# Docker build args 선언
ARG NEXT_PUBLIC_BACKEND_URL
ARG NEXT_PUBLIC_KAKAO_MAP_API_KEY
ARG NEXT_PUBLIC_GOOGLE_CLIENT_ID
ARG NEXT_PUBLIC_OAUTH_REDIRECT_URI
ARG NEXT_PUBLIC_TOSS_SECRET_KEY
ARG NEXT_PUBLIC_TOSS_CLIENT_KEY

# 환경 변수로 설정 (빌드 타임에 Next.js가 읽을 수 있도록)
ENV NEXT_PUBLIC_BACKEND_URL=$NEXT_PUBLIC_BACKEND_URL
ENV NEXT_PUBLIC_KAKAO_MAP_API_KEY=$NEXT_PUBLIC_KAKAO_MAP_API_KEY
ENV NEXT_PUBLIC_GOOGLE_CLIENT_ID=$NEXT_PUBLIC_GOOGLE_CLIENT_ID
ENV NEXT_PUBLIC_OAUTH_REDIRECT_URI=$NEXT_PUBLIC_OAUTH_REDIRECT_URI
ENV NEXT_PUBLIC_TOSS_SECRET_KEY=$NEXT_PUBLIC_TOSS_SECRET_KEY
ENV NEXT_PUBLIC_TOSS_CLIENT_KEY=$NEXT_PUBLIC_TOSS_CLIENT_KEY

# 의존성 설치
COPY package*.json ./
RUN npm ci

# 소스 코드 복사 및 빌드
COPY . .
RUN npm run build

# 2. 실행 스테이지
FROM node:20-alpine
WORKDIR /app

# 빌드 스테이지에서 생성된 결과물만 복사
# standanlone 모드 : 실행에 필요한 최소한의 파일들만 모아서 별도의 실행 가능한 폴더를 만듬.
COPY --from=build /app/.next/standalone ./
COPY --from=build /app/.next/static ./.next/static
COPY --from=build /app/public ./public

# 애플리케이션 실행
EXPOSE 3000
CMD ["node", "server.js"]