Blue-Green Deployment Pipeline

Spring Boot 애플리케이션을 대상으로
Jenkins CI 파이프라인 / SonarQube 품질 게이트 / Docker 기반 Blue-Green 무중단 배포를 구성한 프로젝트입니다.

GitHub에 코드가 push되면 Jenkins가 자동으로 빌드와 테스트를 수행하고,
SonarQube Quality Gate를 통과한 경우에만 Docker 이미지를 생성하여
새로운 버전을 Green 환경에 배포한 뒤, Nginx를 통해 트래픽을 무중단으로 전환합니다.

⸻

목차
	•	프로젝트 개요￼
	•	아키텍처￼
	•	CI/CD 파이프라인￼
	•	Blue/Green 배포 전략￼
	•	주요 컴포넌트￼
	•	Quality Gate 기준￼
	•	실행￼
	•	기술 스택￼

⸻

1. 프로젝트 개요
	•	GitHub Push → Jenkins CI 자동 실행
	•	Gradle 기반 빌드 및 테스트 수행
	•	SonarQube 코드 품질 분석 및 Quality Gate 적용
	•	Docker 이미지 자동 빌드
	•	Blue-Green 컨테이너 배포
	•	Nginx Reload 기반 무중단 트래픽 전환
	•	실패 시 배포 중단 및 알림(Webhook)

⸻

2. 아키텍처
<img width="1345" height="442" alt="image" src="https://github.com/user-attachments/assets/3407a041-26ea-40a6-b1d7-96366b9a7eb9" />

⸻

3. CI/CD 파이프라인

전체 흐름

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

실패 시 흐름

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


⸻

인프라 구성

서비스	포트	역할
Jenkins	8500	CI 파이프라인 실행
SonarQube	8501	코드 품질 분석
Spring Blue	8502	기존 운영 버전
Spring Green	8503	새 버전
Nginx	8504	트래픽 라우팅


⸻

4. Blue/Green 배포 전략

배포 전

Nginx → Blue

현재 운영 중인 서비스.

⸻

배포 중

Green 컨테이너 실행
Health Check 수행

새 버전을 사용자 트래픽과 분리된 상태에서 검증.

⸻

전환

Nginx Reload

트래픽을 Green으로 전환.

⸻

롤백

문제 발생 시:

Nginx → Blue

즉시 이전 버전으로 복구.

⸻

Blue-Green 배포 흐름

[배포 전]

Nginx → Blue

[배포 중]

Green 실행
Health Check 수행

[전환]

Nginx Reload
→ Green 트래픽 전환

[정리]

Blue 종료


⸻

5. 주요 컴포넌트

Jenkins

CI/CD 파이프라인을 실행하는 핵심 컴포넌트.

역할:
	•	GitHub Webhook 수신
	•	Build / Test 수행
	•	SonarQube 분석 요청
	•	Docker 이미지 생성
	•	배포 실행
	•	Health Check 수행
	•	Nginx Reload 수행

⸻

SonarQube

코드 품질을 자동으로 검사하는 정적 분석 도구.

역할:
	•	코드 품질 분석
	•	버그 / 취약점 탐지
	•	코드 커버리지 검사
	•	Quality Gate 판정

⸻

Nginx

Reverse Proxy 및 Load Balancer 역할.

역할:
	•	사용자 요청 수신
	•	Blue / Green 트래픽 라우팅
	•	무중단 배포 지원
⸻

6. Quality Gate 기준

다음 조건 중 하나라도 실패하면 배포가 중단됩니다.
	•	Code Coverage < 80%
	•	Duplicated Lines > 3%
	•	Maintainability Rating > A
	•	Reliability Rating > A
	•	Security Issues > 0
	•	Issues > 0

⸻

7. 기술 스택

Backend
	•	Java 17
	•	Spring Boot
	•	Gradle

DevOps
	•	Jenkins
	•	SonarQube
	•	Docker
	•	Nginx
	•	GitHub Webhook

Deployment
	•	Blue-Green Deployment
	•	Health Check
	•	Reverse Proxy
	•	CI/CD Pipeline
