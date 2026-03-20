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
│  │   Controller Layer  (진입점 / 라우팅)                  │   │
│  ├──────────────────────────────────────────────────────┤   │
│  │   Service Layer  (비즈니스 로직)                       │   │
│  ├──────────────────────────────────────────────────────┤   │
│  │   Repository Layer  (DB 접근)                         │   │
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
                                   S3 / Cloudflare
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
            ├── global/                          # 프로젝트 전역 공통 코드
            │   ├── config/
            │   │   ├── SecurityConfig.java      # Spring Security 설정
            │   │   ├── RedisConfig.java         # Redis 연결 설정
            │   │   ├── S3Config.java            # AWS S3 설정
            │   │   └── SwaggerConfig.java       # API 문서 설정
            │   │
            │   ├── exception/
            │   │   ├── GlobalExceptionHandler.java   # @RestControllerAdvice
            │   │   ├── CustomException.java           # 커스텀 예외 베이스
            │   │   └── ErrorCode.java                 # 에러 코드 enum
            │   │
            │   ├── response/
            │   │   └── ApiResponse.java         # 공통 응답 포맷 { success, data, message }
            │   │
            │   ├── security/
            │   │   ├── JwtTokenProvider.java    # JWT 생성 / 검증
            │   │   └── JwtAuthFilter.java       # 요청마다 토큰 확인하는 필터
            │   │
            │   └── util/
            │       └── S3Uploader.java          # S3 파일 업로드 유틸
            │
            ├── user/                            # 사용자 / 인증
            │   ├── controller/
            │   │   ├── AuthController.java      # POST /auth/signup, /auth/login
            │   │   └── UserController.java      # GET /users/me, PATCH /users/me
            │   ├── service/
            │   │   ├── AuthService.java
            │   │   └── UserService.java
            │   ├── repository/
            │   │   └── UserRepository.java      # JpaRepository 상속
            │   ├── entity/
            │   │   └── User.java                # @Entity — users 테이블
            │   └── dto/
            │       ├── SignupRequest.java
            │       ├── LoginRequest.java
            │       ├── LoginResponse.java
            │       └── UserProfileResponse.java
            │
            ├── post/                            # 게시물
            │   ├── controller/
            │   │   └── PostController.java
            │   ├── service/
            │   │   └── PostService.java
            │   ├── repository/
            │   │   ├── PostRepository.java
            │   │   └── PostImageRepository.java
            │   ├── entity/
            │   │   ├── Post.java
            │   │   └── PostImage.java
            │   └── dto/
            │       ├── CreatePostRequest.java
            │       ├── UpdatePostRequest.java
            │       └── PostResponse.java
            │
            ├── like/                            # 좋아요
            │   ├── controller/
            │   │   └── LikeController.java
            │   ├── service/
            │   │   └── LikeService.java
            │   ├── repository/
            │   │   └── LikeRepository.java
            │   └── entity/
            │       └── Like.java
            │
            ├── comment/                         # 댓글
            │   ├── controller/
            │   ├── service/
            │   ├── repository/
            │   ├── entity/
            │   └── dto/
            │
            ├── follow/                          # 팔로우
            │   ├── controller/
            │   ├── service/
            │   ├── repository/
            │   ├── entity/
            │   └── dto/
            │
            └── bookmark/                        # 북마크
                ├── controller/
                ├── service/
                ├── repository/
                ├── entity/
                └── dto/
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
| **DTO** | 계층 간 요청/응답 데이터 전달 전용 객체 | entity를 controller까지 직접 노출하지 않음 |

<br />

## 4. 인증 흐름

```
[회원가입 / 로그인]
  Client → POST /api/v1/auth/login
         → AuthController
         → AuthService
          → [회원가입] 비밀번호 bcrypt 해싱 → User 저장
          → [로그인] 비밀번호 검증 (bcrypt)
          → Access Token (JWT, 15분) 발급
          → Refresh Token 생성 → Redis 저장 (key: refresh:{userId})
         → 응답 반환

[인증이 필요한 요청]
  Client → Authorization: Bearer {accessToken}
         → JwtAuthFilter → 토큰 유효성 검증
         → SecurityContextHolder에 Authentication 저장

[토큰 재발급]
  Client → POST /api/v1/auth/refresh (body: refreshToken)
         → Redis에서 유효한 Refresh Token 확인
         → 새 Access Token 발급
```

<br />

## 5. 이미지 업로드 흐름 (S3)

```
[Presigned URL 방식 — 고도화 시 전환]
  현재(1단계): Client → POST /api/v1/posts (multipart/form-data)
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

Cursor 기반 페이지네이션 을 채택합니다.

Offset 방식과 비교
| 항목 | Offset (`?page=3`) | Cursor (`?cursor=lastId`) |
| --- | --- | --- |
| 구현 난이도 | 쉬움 | 보통 |
| 데이터 많을 때 성능 | 느림 (앞 데이터 전부 읽고 버림) | 빠름 (인덱스 직접 탐색) |
| 실시간 데이터 변경 시 | 중복/누락 발생 가능 | 안정적 |
| SNS 피드 적합성 | 낮음 | 높음 |

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
