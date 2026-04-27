# fullstack-instagram

## 실행 방법
- 수동 E2E(회원가입 → 로그인 → 게시글 업로드) 자동 실행:
  - `cd backend && ./scripts/e2e-auth-login-post.sh`
  - Docker Desktop(또는 Docker daemon)가 실행 중이어야 합니다.

<br />

## ERD 설계
<img width="1760" height="772" alt="instagram" src="https://github.com/user-attachments/assets/f14bf3f6-d3d3-4a2c-98e8-a0618a29a4eb" />

<br />

## 주차별 계획
### Week 1
- [x]  사용자 시나리오 작성(가입, 로그인, 게시물 업로드, 피드 조회)
- [x]  도메인 모델링 및 ERD 작성
- [x]  API 시그니처 설계(OpenAPI 초안)
- [ ]  프로젝트 구조(패키지/레이어) 합의
