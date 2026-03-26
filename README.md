# Container Infra Build Optimization

## 프로젝트 개요
Docker 기반으로 애플리케이션을 컨테이너화하고,
이미지 최적화 및 모니터링 시스템을 구축하여 운영 관점의 인프라 환경을 구성

---

## 목차

0. [Docker 기본 개념](https://github.com/YongwanJoo/docker_study/blob/main/docs/Docker%20기본%20개념.md)
1. [아키텍처](#-아키텍처)
2. [기술 스택](#-기술-스택)
3. [📌 주요 내용](#-주요-내용)
4. [성능 개선 (이미지 최적화)](#-성능-개선-이미지-최적화)
5. [모니터링](#-모니터링)
6. [CI/CD (Jenkins)](#-cicd-jenkins)

---

## 아키텍처

<img width="3610" height="1822" alt="User Authentication-2026-03-26-071833" src="https://github.com/user-attachments/assets/b5db29c8-ef3b-40b9-ba7e-4b7384a53366" />


---

## 기술 스택

- Container: Docker, docker-compose
- Monitoring: Prometheus, cAdvisor
- Backend: Spring Boot 3.x(Java 17)
- Infra: Linux (Ubuntu)

---

## 📌 주요 내용

- Docker 기반 멀티 컨테이너 환경 구성
- Dockerfile 최적화를 통한 이미지 경량화
- Prometheus + Grafana 기반 모니터링 시스템 구축
- 컨테이너 리소스 사용량 시각화 

---

## 성능 개선 (이미지 최적화)

### 문제 상황
- 초기 Docker 이미지 크기가 과도하게 커서 배포 시간이 오래 소요됨

### 해결
- **Multi-stage Build**: 빌드 시에만 필요한 JDK와 Gradle 도구를 최종 이미지에서 제거하여 보안성을 높이고 용량을 절감
- **불필요한 패키지 제거**: alpine 기반의 경량 이미지를 베이스 이미지로 사용하여 OS 레벨의 취약점과 크기를 최소화
- **Layer Cache 최적화**: 종속성 설정 파일(gradlew, build.gradle 등)을 소스 코드보다 먼저 복사하여, 코드 수정 시에도 라이브러리 다운로드 단계를 건너뛰도록 구성

### Worst vs Best Case 비교

#### Worst Case (Dockerfile)
```Dockerfile
# 최적화 없는 단일 스테이지 방식
FROM openjdk:17-jdk-slim 
WORKDIR /app

# 프로젝트 전체를 한꺼번에 복사 (레이어 캐싱 불가)
COPY . .

# 빌드 실행 (최종 이미지에 빌드 도구가 남음)
RUN ./gradlew :auth-service:build -x test

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "auth-service/build/libs/app.jar"]
```

#### Best Case (Dockerfile)
```Dockerfile
# Stage 1: Builder (빌드 환경)
FROM gradle:8.5-jdk17-alpine AS builder
WORKDIR /app

# 종속성 파일을 먼저 복사하여 레이어 캐싱 활용
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle

# 소스 코드 복사 및 빌드
COPY auth-service ./auth-service
RUN chmod +x gradlew
RUN ./gradlew :auth-service:build -x test --no-daemon

# Stage 2: Runner (경량 실행 환경)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# 빌드 결과물(jar)만 복사하여 용량 최소화
COPY --from=builder /app/auth-service/build/libs/*.jar app.jar

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
```

| 구분 | Worst Case (비최적화) | Best Case (최적화 완료) | 개선 결과 |
| --- | --- | --- | --- |
| Dockerfile 전략 | 단일 스테이지, 전체 복사 | Multi-stage, Layer Caching | 빌드 구조 최적화 |
| 이미지 크기 | 약 1.62GB | 약 1.25GB | **약 23% 감소** |
| 최초 빌드 시간 | 약 62s | 약 55s | 빌드 속도 개선 |
| **재빌드 시간 (캐시)** | 약 62s (매번 전체 빌드) | **약 2s** | **약 96% 시간 단축** |

---

## 📈 모니터링

> Prometheus를 통해 컨테이너 메트릭을 수집하고, Grafana를 활용하여 시각화 환경을 구축함

### 지표 수집 및 시각화
- **Spring Boot Actuator**: 어플리케이션 내부 지표(Heap, CPU, Thread)를 Prometheus 포맷으로 노출
- **cAdvisor**: 컨테이너 리소스 사용량(CPU, Memory, Network, Disk)을 수집
- **Prometheus**: 노출된 엔드포인트에서 메트릭 주기적으로 수집
- **Grafana**: 수집된 메트릭을 시각화하여 대시보드 구성

### 실시간 성능 비교 (Worst vs Best)
두 서비스를 동시에 기동하여 리소스 점유율을 실시간으로 비교 가능합니다.
- **CPU 점유율 (`process_cpu_usage`)**: 초기 기동 및 부하 발생 시 JVM 최적화(JIT) 과정에서의 차이 관찰
- **메모리 사용량 (`jvm_memory_used_bytes`)**: 힙 메모리 및 메타스페이스 점유 양상 비교
- **요청 처리량 (`http_server_requests_seconds_count`)**: 동일 부하 상황에서의 처리 안정성 확인

### 캐시 활용 모니터링 (Redis + Lettuce)
Spring Boot의 Lettuce 드라이버가 제공하는 메트릭을 통해 Redis 캐시 성능을 추적합니다.
- **주요 메트릭**:
  - `lettuce_command_firstresponse_seconds_count`: Redis 명령어 실행 횟수 (Get/Set 비율 확인 가능)
  - `lettuce_command_firstresponse_seconds_sum`: 명령어 응답 시간 합계 (지연 시간 분석)
- **분석 내용**: 애플리케이션에서 발생하는 캐시 요청의 빈도와 지연 시간을 모니터링하여 데이터베이스 부하 분산 효과를 검증

### 예시 화면

#### Prometheus

<img width="1405" height="476" alt="스크린샷 2026-03-26 오후 3 26 40" src="https://github.com/user-attachments/assets/309f46e3-b5bf-4ca2-98c2-533ff8682947" />
<img width="1405" height="695" alt="스크린샷 2026-03-26 오후 3 27 07" src="https://github.com/user-attachments/assets/7372e7a4-4c67-4800-8f5f-7a044b45bd9f" />

#### cAdvisor

<img width="559" height="595" alt="스크린샷 2026-03-26 오후 3 27 38" src="https://github.com/user-attachments/assets/8a093ff4-f92f-434e-91a7-14645742929c" />


---

## 문제 해결

- Layer Caching을 통해 빌드 속도 개선
  - **문제**: 소스 코드 한 줄만 수정해도 매 번 라이브러리 다운로드 시간 소요
  - **해결**: 종속성 파일(gradlew, build.gradle 등)을 소스 코드보다 먼저 복사하여, 코드 수정 시에도 라이브러리 다운로드 단계를 건너뛰도록 구성

- Multi-stage Build를 통한 이미지 경량화
  - **문제**: 빌드 도구(JDK, Gradle)가 포함된 이미지를 사용
  - **해결**: 빌드 시에만 필요한 JDK와 Gradle 도구를 최종 이미지에서 제거하여 보안성을 높이고 용량을 절감

- 실행 이미지 경량화
  - **문제**: 실행 이미지가 너무 큼
  - **해결**: 실행 이미지를 경량화하여 용량을 절감

---

## 🛠 CI/CD (Jenkins)

Jenkins를 활용하여 빌드 및 최적화 실험을 자동화하고 Docker Hub로 배포하는 파이프라인을 구축

### 주요 프로세스
1. **Source Code Checkout**: Git Repository로부터 최신 코드를 가져옴
2. **Build & Optimization Experiment**: `experiment.sh`를 실행하여 'Worst Case'와 'Best Case'의 빌드 속도 및 용량을 비교 측정
3. **Analyze & Log**: 측정된 성능 지표를 Jenkins Console Output에 기록
4. **Push to Docker Hub**: 최적화가 완료된 'Best Case' 이미지를 Docker Hub 레지스트리에 자동으로 푸시

### Jenkins Pipeline 구성 (Jenkinsfile)
- **파일**: [Jenkinsfile](file:///Users/nyongwan/docker_study/Jenkinsfile)
- **필수 설정 (Jenkins 관리)**:
  - **Credentials**: `DOCKER_HUB_CREDENTIALS` (Docker Hub 로그인 정보)
  - **Docker Integration**: Jenkins 컨테이너 내부에서 Docker 명령어를 실행할 수 있도록 설정 필수 (예: `/var/run/docker.sock` 마운트)

### 모니터링 연동
- **Jenkins Prometheus Metrics**: Jenkins에 `Prometheus Metrics` 플러그인을 설치하면, 빌드 성공/실패율 및 소요 시간을 Prometheus로 수집하여 Grafana에서 시각화 가능


### Docker Hub Credentials

<img width="1375" height="754" alt="스크린샷 2026-03-26 오후 4 14 40" src="https://github.com/user-attachments/assets/ad9ee849-17f5-4ea1-b69d-5479bcce6832" />
<img width="729" height="500" alt="스크린샷 2026-03-26 오후 4 14 58" src="https://github.com/user-attachments/assets/b0005964-b4a1-4829-9682-48c5a5007867" />

### Jenkins Credentials

<img width="1395" height="347" alt="스크린샷 2026-03-26 오후 4 15 47" src="https://github.com/user-attachments/assets/8d33e58c-0bbc-4d91-8ee3-63cee2ada1f6" />

<img width="1190" height="787" alt="스크린샷 2026-03-26 오후 4 16 42" src="https://github.com/user-attachments/assets/70cb2f27-2ecc-4ecf-b89f-9555b62dfd0d" />
<img width="705" height="231" alt="스크린샷 2026-03-26 오후 4 17 23" src="https://github.com/user-attachments/assets/471e9598-f338-43f8-a2a8-c1b9aa578bc1" />

---
