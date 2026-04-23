# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

이 저장소는 **인스타그램 백엔드 API**를 구현하는 Spring Boot 프로젝트다.  
현재는 `backend` 단일 모듈 중심이며, 인증/게시물 기능을 우선 구현하고 있다.

- 현재 구현 범위: 사용자 인증(회원가입/로그인/토큰 재발급), 게시물 CRUD, 이미지 업로드/조회
- 인증 방식: JWT 기반 Stateless 인증 (`JwtAuthenticationFilter`)
- 저장소 구조: 백엔드 단일 모듈(`backend`) 중심
- 공통 응답 포맷: `ApiResponse<T>` 래핑 사용
- 게시물 이미지 저장: `ImageStorage` 추상화 기반(Local 기본, S3 확장 자리 마련)

## 기술 스택

- 프레임워크: Spring Boot v3.5.x (Web, Validation, Security, Data JPA, Data Redis)
- 언어: Java v21
- 빌드 도구: Gradle Wrapper (`backend/gradlew`)
- 데이터베이스: MySQL (운영/로컬), H2 (테스트)
- 인증/보안: Spring Security + JJWT
- API 문서화: springdoc-openapi (Swagger UI)
- 테스트: JUnit 5, Spring Boot Test, Spring Security Test

## 작업 우선순위 (중요)

충돌 시 아래 우선순위로 판단한다.

1. 사용자의 현재 요청
2. 현재 코드베이스의 실제 동작/구조
3. `CLAUDE.md`
4. `architecture.md` (목표 아키텍처)
5. `requirements.md` (기능 요구사항/우선순위)

## 아키텍처 규칙

- 아키텍처 관련 의사결정 전, 반드시 루트의 `architecture.md`를 먼저 확인하고 해당 문서의 레이어/패키지/흐름 원칙을 기본 기준으로 따를 것
- `architecture.md`는 목표 아키텍처(지향점)를 설명하므로, 실제 구현과 차이가 있으면 **현재 코드베이스를 우선 존중**하고 필요한 경우 문서/코드 정합성을 함께 맞출 것
- 기능 범위/우선순위 판단이 필요하면 `requirements.md`를 함께 참고할 것
- 패키지는 도메인 중심으로 분리(`user`, `post`, `global`)하고, 각 도메인은 `controller/service/repository/entity/dto` 레이어를 유지할 것
- 컨트롤러는 HTTP 입출력과 검증에 집중하고, 비즈니스 로직은 서비스 계층에 둘 것
- 인증이 필요한 API는 `@AuthenticationPrincipal Long userId`를 사용하고, JWT 기반 인증 컨텍스트를 전제로 구현할 것
- 예외는 `CustomException` + `ErrorCode` + `GlobalExceptionHandler` 조합으로 처리할 것
- API 응답은 `ApiResponse.ok/fail` 형태의 공통 응답 규약을 유지할 것
- 파일 저장은 `ImageStorage` 인터페이스를 통해 접근하고, 구현체(Local/S3)를 직접 서비스 로직에 하드코딩하지 말 것
- JPA 조회 시 연관 엔티티 접근이 필요한 경우 `@EntityGraph` 등으로 N+1을 방지할 것
- 가독성을 최우선으로 하며, 불필요한 추상화/복잡한 패턴 도입을 피할 것

## 코드 변경 원칙

- 최소 변경으로 목적을 달성하고, 요청 범위를 벗어난 리팩터링은 하지 말 것
- 새 구조/추상화 도입 전, 현재 코드에서 중복 또는 확장 필요성이 명확한지 먼저 검증할 것
- Public API(엔드포인트/응답 필드/에러 코드) 변경 시 영향 범위를 확인하고 테스트를 함께 수정할 것
- 보안 관련 코드(`SecurityConfig`, JWT, 인증 필터)는 회귀 위험이 높으므로 변경 시 관련 테스트를 반드시 수행할 것
- 설정값/비밀정보는 환경변수로 주입하고, 하드코딩하지 말 것

## 개발 프로세스

- CRITICAL: 새 기능 구현 시 반드시 테스트를 먼저 작성하고, 테스트가 통과하는 구현을 작성할 것 (TDD)
- 테스트는 작은 범위(Unit/슬라이스)부터 작성하고, 필요한 경우 통합 테스트로 확장할 것
- 커밋 메시지는 conventional commits 형식을 따를 것 (`Feat:`, `Fix:`, `Docs:`, `Refactor:`)
- 구현 완료 시 변경된 동작/규칙이 있으면 관련 문서(`architecture.md`, `requirements.md`, `README.md`) 업데이트 여부를 확인할 것

## 완료 기준 (Definition of Done)

- 관련 테스트가 추가/수정되고 모두 통과한다
- 기존 동작 회귀가 없다 (특히 인증/권한/예외 처리)
- 코드가 읽기 쉽고 책임 분리가 명확하다****
- 불필요한 복잡도(과한 추상화, 사용되지 않는 코드)가 없다
- 필요한 문서/설정 변경이 반영되어 있다

## 명령어

- 애플리케이션 실행(backend): `cd backend && ./gradlew bootRun`
- 전체 테스트 실행: `cd backend && ./gradlew test`
- 빌드(테스트 포함): `cd backend && ./gradlew clean build`
- 특정 테스트만 실행 예시: `cd backend && ./gradlew test --tests "*PostControllerTest"`
- 특정 패키지 테스트 실행 예시: `cd backend && ./gradlew test --tests "com.example.instagram.post.*"`
- Swagger UI 확인: `http://localhost:8080/swagger-ui.html`

## 환경 변수 (로컬 실행)

- `DB_USERNAME`, `DB_PASSWORD`
- `JWT_SECRET`
- 선택: `LOCAL_STORAGE_DIR` (기본값 `./uploads`)
