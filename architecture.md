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
│  │   Controller Layer  (진입점 / 라우팅)                   │   │
│  ├──────────────────────────────────────────────────────┤   │
│  │   Service Layer  (비즈니스 로직)                        │   │
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

## 2. 현재 구현 상태 (As-Is) vs 목표 구조 (To-Be)

아래 표는 **현재 코드베이스 기준(as-is)** 과 **본 문서의 목표 아키텍처(to-be)** 를 구분해, 구현/문서 간 혼선을 줄이기 위한 체크포인트다.

| 영역 | As-Is (현재 구현) | To-Be (목표 구조) | 정합성/다음 액션 |
|------|---|---|---|
| 모듈/범위 | `backend` 단일 모듈 중심 | 기능 확장 가능한 SNS 전체 도메인 구조 | 단일 모듈 유지, 도메인 확장은 점진 적용 |
| 도메인 구현 범위 | `user`, `post`, `global` 중심 구현 | `like/comment/follow/bookmark/search` 포함 | P0 우선순위부터 순차 구현 (`requirements.md`) |
| 인증/보안 | JWT + Spring Security, Stateless 인증 | 동일 | 유지 (회귀 테스트 강화) |
| Refresh Token 저장 | Redis 기반 저장/검증 흐름 사용 | 동일 | 유지 |
| 이미지 저장 | Local 저장 기본 + S3 구현체 자리(placeholder) | S3(+Cloudflare), Presigned URL 전환 | S3 실제 연동 후 Presigned URL 단계적 전환 |
| 피드 페이지네이션 | Spring `Pageable` 기반(Offset) | Cursor 기반 페이지네이션 | 피드 API를 Cursor 전략으로 전환 설계 필요 |
| 데이터/인프라 | MySQL + Redis 사용, Kafka 미도입 | MySQL + Redis + Kafka + Consumer | 이벤트 처리 필요 시 Kafka 도입 |
| API 응답/예외 | `ApiResponse`, `CustomException` + `ErrorCode` 표준화 | 동일 | 유지 |
| 관측성 | Actuator/Metrics 설정 미구성(또는 미노출) | Actuator + Micrometer + 대시보드 | 의존성/엔드포인트 정책 확정 후 도입 |
| 배포/운영 | Docker Compose/CI-CD 파이프라인 문서 대비 미완성 | Compose + GitHub Actions + 배포 자동화 | 로컬 Compose 및 CI 워크플로우 순차 추가 |

> 원칙: 구현 변경 시 이 표와 본문을 함께 갱신해, 문서가 항상 현재 상태를 반영하도록 유지한다.

<br />

## 3. 패키지 구조 (Package Structure)

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

## 4. 레이어 역할 정의

| 레이어 | 역할 | 규칙 |
|--------|------|------|
| **Controller** | HTTP 요청/응답 처리, DTO 변환 | 비즈니스 로직 없음 |
| **Service** | 비즈니스 로직, 트랜잭션 관리 | `@Transactional` 적용 단위 |
| **Repository** | DB 접근 (Spring Data JPA) | 쿼리 최적화는 여기서 관리 |
| **Entity** | 도메인 모델, DB 테이블 매핑 | 비즈니스 메서드 포함 가능 |
| **DTO** | 계층 간 요청/응답 데이터 전달 전용 객체 | entity를 controller까지 직접 노출하지 않음 |

<br />

## 5. 인증 흐름

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

## 6. 이미지 업로드 흐름 (S3)

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

## 7. 캐싱 전략 (Redis Cache)

| 대상 | 캐시 키 | TTL | 전략 |
|------|---------|-----|------|
| 피드 목록 | `feed:{userId}` | 5분 | Cache-Aside |
| 게시물 좋아요 수 | `like:count:{postId}` | - | Write-Through |
| Refresh Token | `refresh:{userId}` | 7일 | Write-Through |

<br />

## 8. 페이지네이션 전략

Cursor 기반 페이지네이션 을 채택합니다.

Offset 방식과 비교
| 항목 | Offset (`?page=3`) | Cursor (`?cursor=lastId`) |
| --- | --- | --- |
| 구현 난이도 | 쉬움 | 보통 |
| 데이터 많을 때 성능 | 느림 (앞 데이터 전부 읽고 버림) | 빠름 (인덱스 직접 탐색) |
| 실시간 데이터 변경 시 | 중복/누락 발생 가능 | 안정적 |
| SNS 피드 적합성 | 낮음 | 높음 |

<br />

## 9. Docker Compose 구성

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

## 10. CI/CD — GitHub Actions

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

## 11. Observability

| 항목 | 도구 | 엔드포인트 |
|------|------|-----------|
| 헬스체크 | Spring Actuator | `GET /actuator/health` |
| 메트릭 | Micrometer + Actuator | `GET /actuator/metrics` |
| 로그 | Logback (JSON 포맷) | stdout → 파일 |

> **고도화**: Prometheus + Grafana 연동, 구조화 로그 → ELK 스택
