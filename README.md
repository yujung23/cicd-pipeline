# Reactive Stream Trade

Binance WebSocket에서 실시간 암호화폐 체결 데이터를 수신하고,
Spring WebFlux + SSE로 브라우저에 스트리밍하는 프로젝트입니다.

Jenkins CI 파이프라인 / SonarQube 품질 게이트 / Blue-Green 컨테이너 배포를 적용합니다.


---

## 목차

- [프로젝트 개요](#1-프로젝트-개요)
- [아키텍처](#2-아키텍처)
- [CI/CD 파이프라인](#3-cicd-파이프라인)
- [주요 컴포넌트](#4-주요-컴포넌트)
- [API](#5-api)
- [설정](#6-설정)
- [실행](#7-실행)
- [기술 스택](#8-기술-스택)

---

## 1. 프로젝트 개요

- Binance WebSocket → `Sinks.Many` Hot Stream → SSE(브라우저) 실시간 스트리밍
- `onBackpressureBuffer`로 버퍼 기반 백프레셔 처리
- React 프론트엔드를 Gradle 빌드 시 `static/`으로 통합, 단일 JAR 서빙
- Jenkins + SonarQube + Docker 기반 CI/CD 파이프라인 구성

---

## 2. 아키텍처

```
Binance WebSocket
  └→ BinanceWebSocketClient  (Sinks.Many — onBackpressureBuffer)
       └→ TradeStreamService  (onBackpressureBuffer 적용)
            └→ TradeController (SSE 스트림)
                    └→ React Frontend (CoinCard / StatsPanel / TradeLog)
```

<img width="526" height="708" alt="image" src="https://github.com/user-attachments/assets/9dd582d9-34d3-426b-9432-1f21e14c3ccf" />

### 데이터 흐름

```
Binance → WebSocket → Sink.emit()
→ Flux (Hot Stream)
→ onBackpressureBuffer (버퍼 500, 초과 시 DROP 로그)
→ SSE → Browser
```

- **Hot Stream** : `Sinks.Many`는 구독 여부와 무관하게 데이터가 계속 흐름
- **Backpressure** : 소비자가 느릴 경우 버퍼에 쌓고, 초과 시 오래된 데이터부터 DROP
- **SSE** : HTTP 커넥션을 유지한 채 서버 → 클라이언트 방향으로 이벤트 전송

---

## 3. CI/CD 파이프라인

### 전체 흐름

```
GitHub Push
    ↓
Jenkins (CI)
    ├─ Gradle Build (프론트 통합 포함)
    ├─ 단위 테스트
    ├─ SonarQube 코드 품질 분석
    ├─ Quality Gate 통과 여부 확인
    │     └─ 실패 시 → 파이프라인 중단
    └─ Docker 이미지 빌드 & Push
         ↓
    배포 서버 (Blue/Green)
    └─ Nginx가 트래픽을 새 버전으로 전환
```

### 인프라 구성

| 서비스 | 포트 | 역할 |
|---|---|---|
| Jenkins | 8090 | CI 파이프라인 실행 |
| SonarQube | 9000 | 코드 품질 분석 / 품질 게이트 |
| App Blue | 8080 | 운영 중인 현재 버전 |
| App Green | 8081 | 새 버전 배포 대상 |
| Nginx | 80 | 트래픽 라우팅 (Blue ↔ Green 전환) |

### Blue/Green 배포 전략

```
[배포 전]  Nginx → Blue (8080) ← 운영 트래픽
[배포 중]  Green (8081)에 새 버전 배포 및 헬스체크
[전환]     Nginx → Green (8081) (무중단 전환)
[롤백]     문제 발생 시 Nginx → Blue (8080) 즉시 복구
```

### SonarQube 품질 게이트

- 커버리지, 코드 중복, 버그/취약점 기준 적용
- 기준 미달 시 Jenkins 파이프라인 자동 실패 → 배포 차단

---

## 4. 주요 컴포넌트

### BinanceWebSocketClient

Binance WebSocket에 연결해 실시간 체결 데이터를 수신하는 핵심 컴포넌트.

- `Sinks.many().multicast().onBackpressureBuffer()` — 내부 버퍼로 backpressure 자동 처리
- `@PostConstruct connect()` — 애플리케이션 시작 시 자동 WebSocket 연결
- `retryWhen(Retry.backoff(...))` — 연결 끊김 시 3초 간격 자동 재연결 (최대 30초)
- `parse()` — Binance JSON → TradeLog 변환 (`m` 필드: `true` = 매도, `false` = 매수)

### TradeStreamService

버퍼 기반 백프레셔를 적용해 스트림을 구성하는 서비스.

- `onBackpressureBuffer(500)` — 버퍼 500개 유지, 초과 시 DROP 로그 기록

### TradeController

- `GET /api/trades/stream` — SSE로 실시간 체결 스트림 전송

### BinanceTradeMessage / TradeLog

- `BinanceTradeMessage` — Binance API 응답 JSON 매핑 DTO
- `TradeLog` — 내부 도메인 객체 (price/quantity String → double 변환)

---

## 5. API

### `GET /api/trades/stream`

```bash
curl -N "http://localhost:8080/api/trades/stream"
```

**응답 (SSE)**
```
event: trade
data: {"symbol":"BTCUSDT","price":96450.0,"quantity":0.082,"buy":true,"tradeTime":1710000000000,"tradeId":123456}
```

---

## 6. 설정

`src/main/resources/application.yaml`

```yaml
binance:
  ws-url: wss://stream.binance.com/ws/btcusdt@trade/ethusdt@trade/xrpusdt@trade

stream:
  buffer-size: 500

server:
  port: 8080
```

구독 심볼 변경: `ws-url` 경로 수정 (형식: `심볼@trade`)

---

## 7. 실행

### 로컬 실행

```bash
./gradlew bootRun
```

프론트엔드 포함 빌드:

```bash
./gradlew build
java -jar build/libs/trade-0.0.1-SNAPSHOT.jar
```

### Docker 실행

```bash
docker build -t reactive-stream-trade .
docker run -p 8080:8080 reactive-stream-trade
```

CORS: `http://localhost:3000` 허용

---

## 8. 기술 스택

**Backend**
- Java 17
- Spring Boot 4.0.5 / Spring WebFlux
- Project Reactor (`Flux`, `Sinks`)
- Reactor Netty WebSocket Client
- Lombok / Jackson

**Frontend**
- React 18
- SSE (`EventSource`)

**CI/CD**
- Jenkins (Declarative Pipeline)
- SonarQube (Quality Gate)
- Docker / Docker Compose
- Nginx (Blue/Green 트래픽 라우팅)
