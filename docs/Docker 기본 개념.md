## 목차

0. [Docker 용량 관리](#0-docker-용량-관리)
1. [Docker 기초 개념](#1-docker-기초-개념)
2. [Docker 핵심 명령어](#2-docker-핵심-명령어)
3. [Docker 컨테이너 & 이미지 관리](#3-docker-컨테이너--이미지-관리)
4. [Docker 이미지 레이어 구조 및 최적화](#4-docker-이미지-레이어-구조-및-최적화)
5. [Docker Hub 및 Dockerfile](#5-docker-hub-및-dockerfile)
6. [Dockerfile 기반 이미지 생성 및 공유](#6-dockerfile-기반-이미지-생성-및-공유)

---

## 빠른 시작

### 설치 (Ubuntu 24.04 기준)

```bash
# 보안 키 설정
sudo apt-get update
sudo apt-get install -y ca-certificates curl gnupg lsb-release
sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

# Docker 저장소 추가
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# 설치
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# 권한 설정 (sudo 없이 사용)
sudo usermod -aG docker $USER
newgrp docker
```

### 삭제

```bash
# 1. 컨테이너 중지 및 삭제
docker stop $(docker ps -aq)
docker rm $(docker ps -aq)

# 2. 패키지 완전 삭제
sudo apt-get purge -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# 3. 잔여 디렉토리 삭제
sudo rm -rf /var/lib/docker
sudo rm -rf /var/lib/containerd
sudo rm -rf /etc/docker
sudo rm -f /etc/apt/keyrings/docker.gpg
sudo rm -f /etc/apt/sources.list.d/docker.list
```

### 컨테이너 한글 설정
```bash
# 방법 1: 실시간 설정
docker exec -it tainer_id> -e LC_ALL=C.UTF-8 bash

# 방법 2: Dockerfile에서 설정
ENV LC_ALL=C.UTF-8
```

---

## Docker 용량 관리

> 인프라 관점의 중요성
> 로컬 환경에서 Docker를 계속 사용하면 사용하지 않는 컨테이너, 이미지, 볼륨, 네트워크 등이 누적되어 **디스크 용량을 낭비**하기 때문에 **정기적인 정리**가 필수임

### 관리 명령어

```bash
# 중지된 모든 컨테이너 삭제
docker container prune

# 컨테이너와 연결되지 않은 모든 볼륨 삭제
docker volume prune

# 컨테이너와 연결되지 않은 모든 네트워크 삭제
docker network prune

# 사용하지 않는 모든 오브젝트 한 번에 삭제 (가장 자주 사용)
docker system prune -a

# 모든 컨테이너 삭제
docker rm $(docker ps -aq)

# 모든 이미지 삭제
docker rmi $(docker images -q)
```

---

## Docker 기초 개념

### 기존 가상화(VM) 과의 차이

- **VM**:
  - Host OS -> Hypervisor -> 각 VM 마다 **독립적인 OS** 포함하기 때문에 **무겁고, 부팅 시간이 오래 걸리고 자원 낭비가 심함**

- **Docker 컨테이너**:
  - Host OS의 Kernel을 공유하고 애플리케이션과 종속성만 패키징 하기 때문에 **가볍고, 시작이 빠르고, 자원 효율성 및 재현성이 높음**

### Docker 아키텍처

```text
┌─────────────────────────────────────┐
│        Docker Client (CLI)          │  ← 사용자 명령어 입력
└──────────────┬──────────────────────┘
               │ REST API/UNIX Socket
┌──────────────▼──────────────────────┐
│        Docker Daemon                │  ← 실제 작업 수행
│  (image, container, network 관리)     |
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│    Docker Host (Docker Engine)      │
│  - Containers, Images, Volumes      │
└─────────────────────────────────────┘
       ↓
┌──────────────────────────────────────┐
│    Docker Registry (Docker Hub)      │  ← 이미지 저장소
└──────────────────────────────────────┘
```

### 핵심 개념

| 용어         | 설명                      | 인프라 관점                         |
| ---------- | ----------------------- | ------------------------------ |
| Image      | 애플리케이션 실행을 위한 읽기 전용 템플릿 | 배포 단위, 버전 관리 필수                |
| Container  | Image로부터 생성된 실행 중인 인스턴스 | 1회용 또는 지속 실행                   |
| Registry   | 이미지를 저장하는 원격 저장소        | 공개(Docker Hub) 또는 비공개 ECR/Quay |
| Dockerfile | 이미지 구축을 정의하는 스크립트       | IaC (Infrastructure as Code)   |


### Docker 사용 이유

1. **인프라 표준화**: 개발 -> 스테이징 -> 배포 일관성 보장
2. **빠른 배포**: 컨테이너 시작 시간이 매우 빠름
3. **자원 효율성**: VM 대비 자원 효율성이 매우 우수함

---

## Docker 핵심 명령어

### Image 관리

```bash
# 이미지 검색 (Docker Hub)
docker search nginx

# 이미지 다운로드 (버전 명시 권장)
docker pull nginx:1.25.3-alpine

# 이미지 목록 확인
docker images

# 이미지 상세 정보
docker inspect <image_id>

# 이미지 레이어 히스토리
docker history --no-trunc <image_name> | less -S

# 이미지 삭제 (컨테이너 종료 후)
docker rmi <image_id>
```

### 컨테이너 생성 및 실행
```bash
# 컨테이너 생성 및 실행
docker run [OPTIONS] <image> [COMMAND]

# 주요 옵션
-d              # Detach (백그라운드 실행)
-it             # Interactive + TTY (터미널 접속)
-p 80:8080      # 포트 매핑 (host:container)
--name myapp    # 컨테이너 이름
-e VAR=value    # 환경 변수
-v /host:/app   # 볼륨 마운트
--rm            # 종료 후 자동 삭제

# 예시: Nginx 서버 구동
docker run -d -p 80:80 --name webserver nginx

# 이미 존재하는 컨테이너 실행
docker start tainer_id>

# 컨테이너 접속
docker exec -it tainer_id> bash

# 컨테이너 로그 확인
docker logs tainer_id>           # 기록된 로그
docker logs -f tainer_id>        # 실시간 로그 (Ctrl+C로 종료)
```

### 컨테이너 관리
```bash
# 실행 중인 컨테이너 목록
docker ps

# 모든 컨테이너 목록
docker ps -a

# 컨테이너 정보 조회
docker inspect tainer_id>

# 컨테이너 중지
docker stop tainer_id>

# 컨테이너 재시작
docker restart tainer_id>

# 컨테이너 삭제
docker rm tainer_id>

# 여러 컨테이너 한 번에 삭제
docker rm $(docker ps -aq)
```
---

## Docker 컨테이너 & 이미지 관리

### 특정 이미지 기반 컨테이너 관리
```bash
# 특정 이미지로 생성된 컨테이너 ID 검색
docker ps -q -f ancestor=<image_name>

# 특정 이미지 기반 모든 컨테이너 중지
docker stop $(docker ps -q -f ancestor=<image_name>)

# 특정 이미지 기반 모든 컨테이너 조회
docker ps -f ancestor=<image_name>

# 정지된 모든 컨테이너 삭제
docker rm $(docker ps -aq)

# 특정 이미지 기반 컨테이너 삭제
docker rm $(docker ps -aq -f ancestor=<image_name>)
```

### 이미지 삭제
```bash
# 특정 이미지 삭제
docker rmi <image_name>

# 특정 태그의 모든 이미지 삭제
docker rmi $(docker images -q <image_name>)

# 모든 이미지 삭제
docker rmi $(docker images -q)

# 강제 삭제 (컨테이너가 실행 중일 때)
docker rmi -f <image_id>
```

### Tip (UI Manager Tool)

```bash
docker run -d -p 9000:9000 --privileged \
  -v /var/run/docker.sock:/var/run/docker.sock \
  uifd/ui-for-docker

# 접속: http://localhost:9000
```

<img width="1049" height="873" alt="스크린샷 2026-03-26 105612" src="https://github.com/user-attachments/assets/db756c76-abdb-4a66-b0dc-a182d07ecd68" />


#### 활용

- Dashboard: 전체 컨테이너 상태 모니터링
- 컨테이너 관리: 시작/중지/재시작
- 이미지 관리: 이미지 검색 및 다운로드

> 위와 같이 다양한 편의 도구를 제공하는 도구로 개발 단계에서 다양한 활용이 가능

---

## Docker 이미지 레이어 구조 및 최적화

> Image 크기는 배포 속도, 네트워크 비용, 스토리지 비용 등 자원 효율성과 연결되어 있기 때문에 Dockerfile 최적화는 비즈니스 관점에서 필수 요구사항임

### Docker 이미지 레이어의 작동 원리

```text
Dockerfile                          이미지 레이어 구조

FROM ubuntu:22.04                  Layer 0: Base Image (ubuntu:22.04)
RUN apt update                     Layer 1: apt 캐시 추가
RUN apt install curl               Layer 2: curl 설치
RUN apt install git                Layer 3: git 설치  
COPY app.py /app                   Layer 4: 애플리케이션 파일
CMD ["python", "app.py"]           Layer 5: 실행 설정
```

### 핵심 특성

- Layer의 불변성
  - 한 번 생성된 레이어는 **절대 수정**되지 않음
  - 변경 필요 시 새로운 레이어를 위에 덮음

- 캐시 활용 규칙
```bash
# Dockerfile 빌드 시
docker build -t myapp:1.0 .

# 빌드 프로세스
FROM ubuntu           ← 캐시 히트 (기존 이미지 재사용)
RUN apt update        ← 새로 실행 (명령 변경됨)
RUN apt install curl  ← 캐시 무효화! (이전 단계 재실행)
RUN apt install git   ← 캐시 무효화 (연쇄)
COPY app.py /app      ← 캐시 무효화
```

> 하나의 명령이 변경되면 **그 이후의 모든 Layer가 재빌드**

### 이미지 크기 확인

```bash
# 각 레이어 크기 상세 확인
docker history --no-trunc <image_name> | less -S

# 출력 예시
IMAGE               CREATED             CREATED BY                                      SIZE
sha256:abc123...    2 hours ago         /bin/sh -c #(nop) CMD ["python" "app.py"]     0B
sha256:def456...    2 hours ago         /bin/sh -c apt install -y git                  150MB  ← 레이어 3
sha256:ghi789...    2 hours ago         /bin/sh -c apt install -y curl                 25MB   ← 레이어 2
sha256:jkl012...    2 hours ago         /bin/sh -c apt update                          200MB  ← 레이어 1
sha256:mno345...    1 month ago         /bin/sh -c #(nop) ADD file:ubuntu              77MB   ← 레이어 0

# 최종 이미지 크기
docker images | grep myapp
# REPOSITORY    TAG      IMAGE ID       CREATED       SIZE
# myapp        1.0      abc123...      2 hours ago   452MB
```

### Dockerfile 최적화

> **Dockerfile을 최적화**하여 자원 효율성을 높일 수 있음

### 최적화 체크리스트

| 항목          | 설명                          | 영향도   |
| ----------- | --------------------------- | ----- |
| RUN 명령 병합   | && 사용하여 하나로 통합              | 매우 높음 |
| 캐시 제거       | rm -rf /var/lib/apt/lists/* | 높음    |
| 다단계 빌드      | FROM ... AS builder 사용      | 매우 높음 |
| 필요한 패키지만    | --no-install-recommends     | 중간    |
| 불필요한 파일 제거  | 빌드 후 정리                     | 높음    |
| Base 이미지 선택 | alpine(5MB) vs ubuntu(77MB) | 매우 높음 |

### Layer 최적화

#### 비효율적인 Layer 예시

```text
FROM ubuntu:22.04
RUN apt update                              # 레이어 1: ~200MB
RUN apt install -y curl                     # 레이어 2: ~25MB
RUN apt install -y git                      # 레이어 3: ~150MB
# apt 캐시가 각 레이어에 남음 → 낭비

# 최종 이미지 크기: ~452MB
```

#### 효율적인 Layer 예시
```text
FROM ubuntu:22.04
RUN apt update && \
    apt install -y --no-install-recommends curl git && \
    rm -rf /var/lib/apt/lists/*            # 레이어 1: ~175MB (캐시 삭제로 감소)

# apt 캐시 제거 → 용량 절약
# 최종 이미지 크기: ~252MB (44% 감소!)
```

### Stage 최적화 (Multi-stage Build)

> **Multi-stage Build**?: 하나의 Dockerfile 안에 **여러 개의 FROM 구문**을 사용하여, **빌드용 이미지와 실행용 이미지를 분리**하는 전략

#### 장점

- **용량 최적화**: 컴파일러, 소스 코드, 빌드 도구(Maven, Gradle 등)를 최종 이미지에서 제외하여 50% 이상의 용량을 절감 가능

- **효율적인 배포**: 이미지 크기가 작아지면 네트워크 전송 속도가 빨라지고, 클라우드 저장 비용이 절감 가능

#### 단계,역할,주요 명령어,특징
- 1단계: 빌드,소스 컴파일 및 패키징,AS builder,"JDK, 소스 코드, 빌드 라이브러리 포함 (무거움)"
- 2단계: 실행,애플리케이션 실행,COPY --from=builder,"JRE, 빌드 결과물(.class 또는 .jar)만 포함 (가벼움)"

#### 예시

- **단일 단계 빌드**
```text
FROM openjdk:17
COPY . /app
WORKDIR /app
RUN javac Main.java
CMD ["java", "Main"]
# JDK 통째로 포함 → 불필요한 빌드 도구 남음
```

- **다단계 빌드**
```text
# 빌드 단계
FROM openjdk:17 AS builder
COPY . /app
WORKDIR /app
RUN javac Main.java

# 런타임 단계 (JRE만 포함)
FROM openjdk:17-jre
WORKDIR /app
COPY --from=builder /app /app
CMD ["java", "Main"]

# 결과: 빌드 도구 제거 → 거의 절반 크기 감소
```

---

## Docker Hub 및 Dockerfile
 
### Docker Hub?

> Docker Inc.에서 운영하는 공식 Container Registry

```text
┌─────────────────┐
│  Public Image   │  ← 무료, 누구나 다운로드
│  (ubuntu, nginx)│
├─────────────────┤
│ Private Image   │  ← 유료, 조직 내부만 접근
│  (company/app)  │
├─────────────────┤
│   Official      │  ← Docker 공식 이미지 (높은 신뢰도)
│   Verified      │  ← 발행자 인증됨
└─────────────────┘
```

### 레지스트리 vs 레포지토리

- 레지스트리: 이미지를 보관하는 서비스
- 레포지토리: 레지스트리 내의 저장 위치

### Docker Hub 로그인 및 이미지 업로드
```bash
# 1단계: 로그인
docker login
# → 인터넷 브라우저로 인증 코드 입력

# 2단계: 이미지에 태그 적용
docker tag myapp:latest username/myapp:1.0

# 3단계: Docker Hub에 푸시 (업로드)
docker push username/myapp:1.0

# 4단계: 확인
docker images | grep myapp
# username/myapp        1.0        abc123...      5 seconds ago    252MB

# 5단계: 로그아웃
docker logout
```

### Image tag 규칙
```bash
# 저장소/이미지이름:태그
username/app-name:version

# 예시
docker tag myapp:latest john/webapp:1.2.3
docker tag myapp:latest john/webapp:latest
docker tag myapp:latest john/webapp:prod
docker tag myapp:latest john/webapp:staging

# 여러 태그 동시 설정
docker tag myapp:1.2.3 \
  docker.io/john/webapp:1.2.3 \
  john/webapp:latest
```

### Dockerfile

> Docker 이미지를 생성하기 위한 설정 파일

#### 특징 
- 텍스트 파일, 확장자 없음
- 각 줄은 새로운 레이어 생성
- 명령어는 대문자

### 핵심 명령어

| 명령어            | 목적         | 인프라 활용              |
| -------------- | ---------- | ------------------- |
| FROM           | 베이스 이미지 지정 | OS/런타임 선택 (용량 영향 큼) |
| RUN            | 빌드 중 명령 실행 | 패키지 설치, 환경 설정       |
| COPY/ADD       | 파일 복사      | 애플리케이션 코드, 설정 파일    |
| WORKDIR        | 작업 디렉토리 설정 | 정리된 구조 유지           |
| CMD/ENTRYPOINT | 컨테이너 시작 명령 | 애플리케이션 실행           |


### 빌드 및 푸쉬

```bash
# 빌드
docker build -t mycompany/spring-app:1.0 .

# 실행
docker run -d -p 8080:8080 mycompany/spring-app:1.0

# 로그 확인
docker logs -f tainer_id>

# 업로드
docker push mycompany/spring-app:1.0
```

### Tip (Dockerfile CLI 검증: Hadolint)

> **CLI**로 Dockerfile 오류 및 최적화 제안을 받을 수 있음 

```bash
# 설치
sudo apt install hadolint

# 검증
hadolint Dockerfile

# 출력 예시
Dockerfile:1 warning: Always tag the version explicitly  [DL3006]
Dockerfile:2 warning: Use LABEL instead of MAINTAINER  [DL4000]
Dockerfile:5 warning: Pin a specific version  [DL3006]
```

---

## Dockerfile 기반 이미지 생성 및 공유 (Spring Boot 실습)
> 간단한 Spring Boot 애플리케이션을 작성하고, 이를 Docker 이미지로 빌드하여 Docker Hub에 공유

### Step 1: Spring Boot 프로젝트 준비

간단한 Controller가 포함된 Spring Boot 프로젝트를 생성 (Maven/Gradle 빌드 후 target/ 또는 build/libs/에 JAR 파일이 생성된 상태)

기능: 포트 8080에서 실행되며, 환경 변수 APP_ENV를 출력

### Step 2: Dockerfile 작성

```bash
# 1단계: 빌드 스테이지 (JDK 포함)
FROM openjdk:17-jdk-slim AS builder
WORKDIR /app
COPY . .
# 권한 부여 및 빌드 (생략 가능, 이미 JAR가 있다면 COPY만 수행)
RUN ./gradlew bootJar 

# 2단계: 실행 스테이지 (JRE만 포함하여 용량 최적화)
FROM openjdk:17-jdk-slim
WORKDIR /app

# 빌드 스테이지에서 생성된 JAR 파일만 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 환경 변수 설정
ENV APP_ENV="production"
EXPOSE 8080

# 실행 명령
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Step 3: 이미지 빌드 및 확인
```bash
# 1. 이미지 빌드 (-t: 태그 설정, .: 현재 디렉토리의 Dockerfile 사용)
docker build -t spring-app:1.0 .

# 2. 빌드 과정 출력 예시
# [internal] load build definition from Dockerfile
# => => transferring dockerfile: 450B
# => => Step 1/7 : FROM openjdk:17-jdk-slim AS builder
# ...
# => => Successfully built abc123def
# => => Successfully tagged spring-app:1.0

# 3. 생성된 이미지 용량 확인
docker images | grep spring-app
# spring-app    1.0    abc123def    20 seconds ago    230MB (JRE 기반 최적화 결과)
```

### Step 4: 컨테이너 실행 및 테스트
```bash
# 1. 컨테이너 실행 (포트 포워딩 8080:8080)
docker run -d -p 8080:8080 --name my-spring-container spring-app:1.0

# 2. 실행 로그 확인
docker logs -f my-spring-container
#  .   ____          _            __ _ _
# /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
# ( ( )___ | '_ | '_| | '_ \/ _` | \ \ \ \
# ... Spring Boot app started ...

# 3. 환경 변수 적용 확인 (컨테이너 내부 접속)
docker exec -it my-spring-container env | grep APP_ENV
# APP_ENV=production
```

### Step 5: Docker Hub 공유 (Push & Pull)
```bash
# 1. Docker Hub 로그인
docker login

# 2. 배포용 태그 생성 (username/repository:tag 형식)
# 내 로컬의 spring-app:1.0을 내 계정의 원격 저장소 이름으로 매핑
docker tag spring-app:1.0 nyongwan/spring-app:1.0

# 3. 원격 저장소에 업로드
docker push nyongwan/spring-app:1.0

# 4. (다른 환경에서) 이미지 내려받기 및 실행
docker pull nyongwan/spring-app:1.0
docker run -d -p 8080:8080 nyongwan/spring-app:1.0
```

---