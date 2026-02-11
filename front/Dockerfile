# 1. 빌드 스테이지
FROM node:20-alpine AS build
WORKDIR /app

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