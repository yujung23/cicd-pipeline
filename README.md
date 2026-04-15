# Blue-Green Deployment Pipeline

Spring Boot 애플리케이션을 대상으로  
**Jenkins CI 파이프라인 / SonarQube 품질 게이트 / Docker 기반 Blue-Green 무중단 배포**를 구성한 프로젝트입니다.

GitHub에 코드가 push되면 Jenkins가 자동으로 빌드와 테스트를 수행하고,  
**SonarQube Quality Gate를 통과한 경우에만** Docker 이미지를 생성하여  
새로운 버전을 Green 환경에 배포한 뒤, Nginx를 통해 트래픽을 무중단으로 전환합니다.

---

## 목차

- 프로젝트 개요
- 아키텍처
- CI/CD 파이프라인
- Blue/Green 배포 전략
- 주요 컴포넌트
- Quality Gate 기준
- 기술 스택

---

## 1. 프로젝트 개요

- GitHub Push → Jenkins CI 자동 실행
- Gradle 기반 빌드 및 테스트 수행
- SonarQube 코드 품질 분석 및 Quality Gate 적용
- Docker 이미지 자동 빌드
- Blue-Green 컨테이너 배포
- Nginx Reload 기반 무중단 트래픽 전환
- 실패 시 배포 중단 및 알림(Webhook)

---

## 2. 아키텍처

<img width="1345" height="442" alt="image" src="https://github.com/user-attachments/assets/3407a041-26ea-40a6-b1d7-96366b9a7eb9" />

---

## 3. CI/CD 파이프라인

```
GitHub Push
    ↓
Jenkins Pipeline 실행
    ↓
Gradle Build
    ↓
Unit Test
    ↓
SonarQube 코드 분석
    ↓
Quality Gate 확인
    ↓
(통과)
Docker Image Build
    ↓
Green 컨테이너 실행
    ↓
Health Check
    ↓
Nginx Reload
    ↓
트래픽 Green 전환
    ↓
Blue 컨테이너 종료

### 실패 시 흐름

Quality Gate 실패
또는
Build 실패
또는
Health Check 실패
    ↓
Jenkins Pipeline 실패
    ↓
배포 중단
    ↓
GitHub Commit Status 실패 표시
```

---

## 인프라 구성

| 서비스 | 포트 | 역할 |
|--------|------|------|
| Jenkins | 8500 | CI 파이프라인 실행 |
| SonarQube | 8501 | 코드 품질 분석 |
| Spring Blue | 8502 | 기존 운영 버전 |
| Spring Green | 8503 | 새 버전 |
| Nginx | 8504 | 트래픽 라우팅 |

---

## 4. Blue/Green 배포 전략

### Blue-Green 배포 전략을 적용한 이유

본 시스템은 다음과 같은 특성을 가지고 있습니다.

- WebSocket을 통해 외부(Binance)와 지속적으로 연결된 상태 유지
- Reactor Flux와 Sinks 기반의 Hot Stream 구조로 다수의 클라이언트가 동일한 스트림을 구독
- SSE를 통해 실시간 데이터를 지속적으로 전송하는 장기 연결(Long-lived connection) 환경

이러한 환경에서 기존 방식의 배포(컨테이너 재시작)는 다음과 같은 문제를 유발할 수 있습니다.

- 기존 WebSocket 연결 강제 종료
- 클라이언트 SSE 연결 중단
- 스트림 재구독 과정에서 데이터 누락 가능성
- 서비스 응답 지연 및 사용자 경험 저하

따라서 새로운 버전을 별도의 환경(Green)에 먼저 배포하고,
Health Check를 통해 정상 동작을 확인한 뒤 트래픽을 전환하는 Blue-Green 배포 전략을 적용했습니다.

이를 통해 다음과 같은 효과를 얻었습니다.

- 실시간 스트림 연결을 유지한 상태에서 서비스 업데이트 가능
- 사용자 연결(Session / Stream) 중단 없이 버전 전환
- 장애 발생 시 즉시 이전 버전으로 롤백 가능
- 실시간 데이터 처리 서비스의 안정성 향상

### 배포 전

Nginx → Blue

현재 운영 중인 서비스.

---

### 배포 중

Green 컨테이너 실행
Health Check 수행

새 버전을 사용자 트래픽과 분리된 상태에서 검증.

---

### 전환

Nginx Reload

트래픽을 Green으로 전환.

---

### 롤백

문제 발생 시:

Nginx → Blue

즉시 이전 버전으로 복구.

---

## 4-1. 무중단 처리 전략 (Graceful Shutdown)

Blue-Green 배포 시 기존 컨테이너가 즉시 종료되면,
처리 중이던 요청이 중단되어 사용자 오류가 발생할 수 있습니다.

이를 방지하기 위해 Graceful Shutdown 전략을 적용하여
기존 요청을 정상적으로 처리한 후 안전하게 컨테이너를 종료하도록 구성했습니다.

### 전략

1. Nginx가 새로운 요청을 Green 환경으로 전환
2. 기존 Blue 컨테이너는 새로운 요청을 더 이상 받지 않음
3. 처리 중인 요청이 모두 완료될 때까지 대기
4. 일정 시간 이후 컨테이너 종료

---

## 5. 주요 컴포넌트

### Jenkins

CI/CD 파이프라인을 실행

- GitHub Webhook 수신
- Build / Test 수행
- SonarQube 분석 요청
- Docker 이미지 생성
- 배포 실행
- Health Check 수행
- Nginx Reload 수행

---

### SonarQube

- 코드 품질 분석
- 버그 / 취약점 탐지
- 코드 커버리지 검사
- Quality Gate 판정

---

### Nginx

Reverse Proxy 및 Load Balancer 역할

- 사용자 요청 수신
- Blue / Green 트래픽 라우팅
- 무중단 배포 지원

---

## 6. Quality Gate 기준

다음 조건 중 하나라도 실패하면 배포가 중단됩니다.

- Code Coverage < 80%
- Duplicated Lines > 3%
- Maintainability Rating > B
- Reliability Rating > A
- Security Issues > 0
- Issues > 5

---

## 7. 기술 스택

### Backend

- Java 17
- Spring Boot
- Gradle

### DevOps

- Jenkins
- SonarQube
- Docker
- Nginx
- GitHub Webhook

### Deployment

- Blue-Green Deployment
- Health Check
- Reverse Proxy
- CI/CD Pipeline
