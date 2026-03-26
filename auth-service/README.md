# Auth Service

인증 및 권한 부여를 담당하는 마이크로서비스입니다.  
**JWT(Json Web Token)** 기반의 인증 방식을 사용하며, **Redis**를 활용한 토큰 블랙리스트(로그아웃) 기능을 제공합니다.

## 기술 스택
- **Language**: Java 17
- **Framework**: Spring Boot 3.5.10
- **Database**: PostgreSQL (JPA/Hibernate)
- **Security**: 
  - Spring Security (BCrypt Password Encoder)
  - JWT (RS256) - RSA Asymmetric Key Pair
- **Cache**: Redis (Token Blacklist)
- **Monitoring**: Zipkin (Tracing)

## 실행 방법

### 사전 요구사항 (Infrastructure)
PostgreSQL(5432)과 Redis(6379)가 실행 중이어야 합니다.
```bash
docker-compose up -d
```

### 로컬 실행 (권장)
루트에서 생성된 `.env.local`을 사용하여 실행합니다.

```bash
source ../.env.local && ./gradlew bootRun
```
실행 후 `http://localhost:8082`에서 서비스가 동작합니다.

> **Note**: 초기 실행 시 `DataInitializer`가 테스트용 계정(`user` / `password`)을 자동으로 생성합니다.
> **Key Management (RS256)**: 
> - 로컬: `local_setup.sh`가 생성한 키가 환경변수(`JWT_PRIVATE_KEY`, `JWT_PUBLIC_KEY`)로 주입됩니다.
> - 프로덕션(K8s): Kubernetes Secret(`jwt-secrets`)에서 파드에 주입됩니다.

## API 명세

### 1. 로그인 (Login)
사용자 자격 증명을 확인하고 JWT 토큰을 발급합니다.

- **URL**: `POST /auth/login`
- **Request Body**:
  ```json
  {
    "username": "user",
    "password": "password"
  }
  ```
- **Response (200 OK)**:
  ```json
  {
    "accessToken": "eyJhbGciOiJSUzI1NiJ9...", // Signed with RS256
    "tokenType": "Bearer",
    "expiresIn": 3600
  }
  ```
- **Response (401 Unauthorized)**: `Invalid credentials`

### 2. 로그아웃 (Logout)
토큰을 블랙리스트에 등록하여 만료시킵니다 (남은 유효시간 동안).

- **URL**: `POST /auth/logout`
- **Header**: `Authorization: Bearer <accessToken>`
- **Response (200 OK)**: `Logged out successfully`

### 3. 토큰 검증 (Validate)
발급된 JWT 토큰의 유효성을 검증하고 사용자 ID를 반환합니다. 블랙리스트에 등록된 토큰은 거부됩니다.

- **URL**: `GET /auth/validate`
- **Query Parameter**: `token` (JWT 토큰)
- **Response (200 OK)**: `Valid Token for user: {userId}`
- **Response (401 Unauthorized)**: `Logged out token` or `Invalid Token`

### 4. 공개키 조회 (JWKS)
Istio 등 외부 서비스가 토큰 서명을 검증할 수 있도록 RSA 공개키를 제공합니다.

- **URL**: `GET /.well-known/jwks.json`
- **Response (200 OK)**:
  ```json
  {
    "keys": [
      {
        "kty": "RSA",
        "kid": "uuid-key-id",
        "use": "sig",
        "alg": "RS256",
        "n": "...",
        "e": "AQAB"
      }
    ]
  }
  ```
