# EasyTrade Backend

EasyTrade의 Spring Boot 백엔드입니다.

## 기술 스택

- Java 21
- Spring Boot 4.0.6
- Gradle
- MySQL
- Spring Security
- JWT

## 실행 전 준비

로컬에 Java 21과 Gradle 또는 Gradle Wrapper가 필요합니다.

MySQL에는 다음 DB를 생성합니다.

```sql
CREATE DATABASE easytrade CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

기본 접속 정보는 `src/main/resources/application.yml`에 있습니다.

```text
DB_URL=jdbc:mysql://localhost:3306/easytrade?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
DB_USERNAME=easytrade
DB_PASSWORD=easytrade
JWT_SECRET=change-this-secret-key-for-local-development-only
```

## 주요 API

### 인증

```text
POST /api/auth/register
POST /api/auth/login
GET  /api/auth/me
```

### 주식

```text
GET /api/stocks/search?query=005930
GET /api/stocks/{code}
GET /api/stocks/popular
```

현재 주식 시세는 한국투자증권 Open API 연동 전 단계라서
`MockStockPriceProvider`의 임시 데이터를 사용합니다.

한국투자증권 Open API를 사용할 경우 앱키와 앱시크릿은 코드에 저장하지 않고
환경변수로 설정합니다.

```text
KIS_ENABLED=true
KIS_BASE_URL=https://openapi.koreainvestment.com:9443
KIS_APP_KEY=발급받은_앱키
KIS_APP_SECRET=발급받은_앱시크릿
```

API 호출에 실패하거나 `KIS_ENABLED=false`이면 기존 임시 시세 데이터로
fallback 됩니다.

### 거래

```text
POST /api/trades/buy
POST /api/trades/sell
GET  /api/trades
```

매수/매도 요청 예시는 다음과 같습니다.

```json
{
  "stockCode": "005930",
  "quantity": 10
}
```

### 포트폴리오

```text
GET /api/portfolio
```

로그인 이후 API는 다음 헤더가 필요합니다.

```text
Authorization: Bearer {JWT_TOKEN}
```
