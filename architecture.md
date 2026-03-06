# SNS Service Architecture


## 1. 시스템 아키텍처 개요

```
┌─────────────────────────────────────────────────────────────┐
│                        Client (HTTP)                        │
└─────────────────────────┬───────────────────────────────────┘
                          │ REST API (JSON)
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                   Spring Boot Application                   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  Presentation Layer  (Controller / Filter / Advice)  │   │
│  ├──────────────────────────────────────────────────────┤   │
│  │  Application Layer   (Service / UseCase)             │   │
│  ├──────────────────────────────────────────────────────┤   │
│  │  Domain Layer        (Entity / Repository Interface) │   │
│  ├──────────────────────────────────────────────────────┤   │
│  │  Infrastructure Layer (JPA / Redis / MinIO / Kafka)  │   │
│  └──────────────────────────────────────────────────────┘   │
└────────┬──────────────┬───────────────┬─────────────────────┘
         │              │               │
         ▼              ▼               ▼
      MySQL           Redis           Kafka
      (Primary DB)    (Cache/Lock)    (Message Broker)
                                          │
                                          ▼
                                    Consumer (App)
                                          │
                                          ▼
                                  MinIO / Cloudflare
                                   (Image Storage)
```

<br />

## 2. 패키지 구조 (Package Structure)

레이어드 아키텍처(Layered Architecture)를 기반으로 도메인별 패키지를 구성합니다.

```
src/
└── main/
    └── java/
        └── com.example.instagram/
            │
            ├── common/                        # 공통 유틸
            │   ├── config/                    # Spring 설정 (Security, Redis, Kafka, S3)
            │   ├── exception/                 # GlobalExceptionHandler, CustomException
            │   ├── response/                  # ApiResponse<T>, ErrorResponse
            │   ├── util/                      # JwtUtil, CursorUtil, SnowflakeIdGenerator
            │   └── interceptor/               # AuthInterceptor
            │
            ├── domain/
            │   ├── user/
            │   │   ├── controller/            # UserController, AuthController
            │   │   ├── service/               # UserService, AuthService
            │   │   ├── repository/            # UserRepository (JPA Interface)
            │   │   ├── entity/                # User.java
            │   │   └── dto/                   # request/response DTO
            │   │
            │   ├── post/
            │   │   ├── controller/
            │   │   ├── service/
            │   │   ├── repository/
            │   │   ├── entity/                # Post.java, PostImage.java
            │   │   └── dto/
            │   │
            │   ├── like/
            │   ├── comment/
            │   ├── follow/
            │   ├── bookmark/
            │   └── search/                    # (고도화)
            │
            └── infrastructure/
                ├── s3/                        # S3Uploader
                ├── redis/                     # RedisService
                └── kafka/                     # KafkaProducer, KafkaConsumer
```

> **설계 원칙**: Controller → Service → Repository 단방향 의존성  
> Service는 다른 도메인의 Service를 직접 참조하지 않고, Repository 또는 이벤트를 통해 통신합니다.

<br />

## 3. 레이어 역할 정의

| 레이어 | 역할 | 규칙 |
|--------|------|------|
| **Controller** | HTTP 요청/응답 처리, DTO 변환 | 비즈니스 로직 없음 |
| **Service** | 비즈니스 로직, 트랜잭션 관리 | `@Transactional` 적용 단위 |
| **Repository** | DB 접근 (Spring Data JPA) | 쿼리 최적화는 여기서 관리 |
| **Entity** | 도메인 모델, DB 테이블 매핑 | 비즈니스 메서드 포함 가능 |
| **DTO** | 요청/응답 데이터 전달 객체 | `record` 또는 불변 객체 권장 |

<br />

## 4. 인증 흐름

```
[로그인 요청]
  Client → POST /api/v1/auth/login
         → AuthService.login()
         → 비밀번호 검증 (bcrypt)
         → Access Token (JWT, 15분) 발급
         → Refresh Token 생성 → Redis 저장 (key: refresh:{userId})
         → 응답 반환

[인증이 필요한 요청]
  Client → Authorization: Bearer {accessToken}
         → JwtAuthFilter → 토큰 검증
         → SecurityContextHolder에 Authentication 저장
         → Controller 진입

[토큰 재발급]
  Client → POST /api/v1/auth/refresh (body: refreshToken)
         → Redis에서 유효한 Refresh Token 확인
         → 새 Access Token 발급
```

<br />

## 5. 이미지 업로드 흐름 (S3)

```
[Presigned URL 방식 — 고도화 시 전환]
  현재(1단계): Client → POST /api/v1/posts (multipart)
                     → Server → S3 업로드
                     → S3 URL → DB 저장

  고도화(2단계): Client → GET /api/v1/posts/upload-url
                      → Server → S3 Presigned URL 반환
                      → Client → S3 직접 PUT 업로드
                      → Client → POST /api/v1/posts (S3 key 전달)
```

<br />

## 6. 캐싱 전략 (Redis Cache)

| 대상 | 캐시 키 | TTL | 전략 |
|------|---------|-----|------|
| 피드 목록 | `feed:{userId}` | 5분 | Cache-Aside |
| 게시물 좋아요 수 | `like:count:{postId}` | - | Write-Through |
| Refresh Token | `refresh:{userId}` | 7일 | Write-Through |

<br />

## 7. 페이지네이션 전략

적합한 방식 채택 이후 작성 예정

<br />

## 8. Docker Compose 구성

```yaml
# docker-compose.yml 구성 컴포넌트
services:
  app:          # Spring Boot (포트: 8080)
  mysql:        # MySQL 8.x (포트: 3306)
  redis:        # Redis 7.x (포트: 6379)
  zookeeper:    # Kafka 의존성
  kafka:        # Kafka (포트: 9092)
```

<br />

## 9. CI/CD — GitHub Actions

```
[main 브랜치 Push / PR]
  1. Checkout
  2. JDK 25 설정
  3. Gradle 빌드 + 테스트
  4. Docker 이미지 빌드
  5. (고도화) Docker Hub / ECR Push
  6. (고도화) EC2 또는 ECS 배포
```

<br />

## 10. Observability

| 항목 | 도구 | 엔드포인트 |
|------|------|-----------|
| 헬스체크 | Spring Actuator | `GET /actuator/health` |
| 메트릭 | Micrometer + Actuator | `GET /actuator/metrics` |
| 로그 | Logback (JSON 포맷) | stdout → 파일 |

> **고도화**: Prometheus + Grafana 연동, 구조화 로그 → ELK 스택
