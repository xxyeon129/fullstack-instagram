이 프로젝트는 Harness 스타일의 단계적 실행 방식을 사용한다. 아래 워크플로우를 따른다.

---

## 워크플로우

### A. 탐색

구현 전에 아래 문서를 우선 읽고 현재 상태(as-is)와 목표(to-be)를 파악한다.

- `/CLAUDE.md`
- `/architecture.md`
- `/requirements.md`
- `/docs/backend-post-module-flow.md` (존재 시)

문서와 코드가 다르면 **현재 코드베이스를 우선** 기준으로 삼고, 필요한 경우 문서 정합성을 함께 맞춘다.

### B. 논의

구현에 필요한 결정 사항(트레이드오프, 범위, API 계약 변경)이 있으면 사용자와 먼저 합의한다.

### C. Step 설계

사용자가 계획 작성을 요청하면 step 초안을 제시하고 피드백을 받는다.

설계 원칙:

1. **Scope 최소화** — 한 step은 한 레이어/한 모듈 중심으로 제한한다.
2. **자기완결성** — step 파일만 읽어도 수행 가능해야 한다.
3. **사전 준비 강제** — 읽어야 할 문서/파일 경로를 반드시 명시한다.
4. **시그니처 수준 지시** — 인터페이스 중심으로 지시하고 구현은 에이전트 재량에 맡긴다.
5. **AC는 실행 가능한 커맨드** — 이 저장소 기준으로 `./gradlew` 커맨드를 사용한다.
6. **주의사항은 구체적으로** — "X를 하지 마라. 이유: Y" 형식을 따른다.
7. **네이밍** — step name은 kebab-case (`auth-flow`, `post-api` 등).
8. **가독성 우선** — 불필요한 추상화/복잡도 도입을 피한다.

### D. 파일 생성

사용자가 승인하면 아래 파일을 생성/갱신한다.

#### D-1. `phases/index.json` (전체 현황)

여러 task를 관리하는 top-level 인덱스다. 이미 있으면 `phases` 배열에 항목을 추가한다.

```json
{
  "phases": [
    {
      "dir": "post-mvp",
      "status": "pending"
    }
  ]
}
```

- `dir`: task 디렉토리명
- `status`: `"pending"` | `"completed"` | `"error"` | `"blocked"`

#### D-2. `phases/{task-name}/index.json` (task 상세)

```json
{
  "project": "fullstack-instagram",
  "phase": "post-mvp",
  "steps": [
    { "step": 0, "name": "post-entity-repository", "status": "pending" },
    { "step": 1, "name": "post-service", "status": "pending" },
    { "step": 2, "name": "post-controller", "status": "pending" }
  ]
}
```

필드 규칙:

- `project`: 프로젝트명 (`fullstack-instagram`)
- `phase`: task 이름(디렉토리명과 일치)
- `steps[].step`: 0부터 시작
- `steps[].name`: kebab-case
- `steps[].status`: 초기값 `"pending"`

상태 전이 시 추가 필드:

- 완료: `summary`
- 실패: `error_message`
- 차단: `blocked_reason`

#### D-3. `phases/{task-name}/step{N}.md`

```markdown
# Step {N}: {name}

## 읽어야 할 파일

- /CLAUDE.md
- /architecture.md
- /requirements.md
- /backend/src/main/java/... (관련 코드)
- /backend/src/test/java/... (관련 테스트)
- {이전 step 산출물 경로}

## 작업

{구체적 작업 지시}

핵심 규칙:
- 기존 API 계약을 깨지 마라. 이유: 프론트/테스트 회귀 방지.
- 보안 규칙을 우회하지 마라. 이유: 인증/권한 회귀 위험.
- 불필요한 추상화를 추가하지 마라. 이유: 유지보수성 저하.

## Acceptance Criteria

```bash
cd backend && ./gradlew test
cd backend && ./gradlew clean build
```

## 검증 절차

1. AC 커맨드를 실행한다.
2. 아래 체크리스트를 검증한다.
   - `architecture.md`의 as-is/to-be 정렬 원칙을 위반하지 않는가?
   - `CLAUDE.md`의 TDD/아키텍처/가독성 규칙을 지켰는가?
   - 변경 범위에 맞는 테스트가 추가/수정되었는가?
3. 결과에 따라 `phases/{task-name}/index.json`의 step 상태를 갱신한다.

## 금지사항

- unrelated 리팩터링을 하지 마라. 이유: 리뷰/회귀 범위가 불필요하게 커진다.
- 실패하는 테스트를 남긴 채 완료 처리하지 마라. 이유: 파이프라인 신뢰도 하락.
```