---
name: workthrough
description: Automatically document all development work and code modifications in a structured workthrough format. Use this skill after completing any development task, bug fix, feature implementation, or code refactoring to create comprehensive documentation.
license: MIT
---

This skill automatically generates detailed workthrough documentation for all development work, capturing the context, changes made, and verification results in a clear, structured format.

## Language Policy

- 기본 출력 언어는 **한국어**다.
- 문서의 본문(제목, 개요, 변경 내역, 검증 결과, 다음 단계)은 한국어로 작성한다.
- 코드, 파일 경로, 명령어, 에러 메시지, 라이브러리/클래스명 등 기술 식별자는 원문(영문)을 유지한다.

## When to Use This Skill

Use this skill automatically after:
- Implementing new features or functionality
- Fixing bugs or errors
- Refactoring code
- Making configuration changes
- Updating dependencies
- Resolving build/compilation issues
- Any significant code modifications

## Documentation Structure

The workthrough documentation follows this structure:

1. **제목**: 수행한 작업을 명확히 설명하는 제목
2. **개요**: 무엇을 왜 수행했는지 요약
3. **변경 사항**: 수정 내역 상세
4. **코드 예시**: 핵심 변경 코드
5. **검증 결과**: 빌드/테스트 결과

## Implementation Guidelines

When generating workthrough documentation:

### 1. Capture Complete Context
- What problem was being solved?
- What errors or issues existed before?
- What approach was taken?
- Why were specific decisions made?

### 2. Document All Changes Systematically
- List each file modified with full paths
- Describe what changed in each file
- Include before/after code snippets for significant changes
- Note any dependencies added or removed
- Document configuration updates

### 3. Show Code Examples
Use clear, well-formatted code blocks:
```language
// file: src/path/to/file.tsx
<div className="example">
  {/* Show relevant code changes */}
</div>
```

### 4. Include Verification
- Build output showing success
- Test results
- Error messages (if any remain)
- Exit codes
- Screenshots (if relevant)

### 5. Use Clear Formatting
- Use markdown headers (##, ###)
- Use bullet points and numbered lists
- Use code blocks with syntax highlighting
- Use blockquotes for output/logs
- Keep paragraphs concise

## Document Organization

Save workthrough documents with this naming convention:
```
workthrough/YYYY-MM-DD-brief-description.md
```

Or organize by feature/project:
```
workthrough/feature-name/implementation.md
workthrough/bugfix/issue-123.md
```

## Example Workthrough Structure

```markdown
# [작업 내용을 명확히 드러내는 제목]

## 개요
무엇을 왜 변경했는지 2-3문장으로 요약한다.

## 배경
- 왜 이 작업이 필요했는가?
- 초기 문제/요구사항은 무엇이었는가?
- 관련된 맥락은 무엇인가?

## 변경 사항

### 1. [첫 번째 주요 변경]
- 구체적 변경 1
- 구체적 변경 2
- 파일: `path/to/file.tsx`

### 2. [두 번째 주요 변경]
- 구체적 변경 1
- 파일: `path/to/another-file.ts`

### 3. [추가 변경]
- 의존성 추가: `package-name@version`
- 설정 변경: `config-file.json`

## 코드 예시

### [기능/수정 이름]
```typescript
// src/path/to/file.tsx
const example = () => {
  // 핵심 코드 변경
}
```

## 검증 결과

### 빌드 검증
```bash
> build command output
✓ Compiled successfully
Exit code: 0
```

### 테스트 결과
```bash
> test command output
All tests passed
```

## 다음 단계
- 후속 작업
- 현재 한계 또는 개선 포인트
```

## Automation Instructions

After completing ANY development work:

1. **Gather Information**
   - Review all files modified during the session
   - Collect build/test output
   - Identify the main objective that was accomplished

2. **Create Document**
   - Generate workthrough document in `workthrough/` directory
   - Use timestamp or descriptive filename
   - Follow the structure guidelines above

3. **Be Comprehensive**
   - Include all relevant details
   - Don't assume future readers have context
   - Document decisions and reasoning
   - Show concrete examples

4. **Verify Completeness**
   - Confirm all changes are documented
   - Include verification results
   - Add any relevant warnings or notes

## Quality Standards

Good workthrough documentation should:
- Be readable by other developers
- Provide enough detail to understand changes
- Include verification that changes work
- Serve as a reference for similar future work
- Capture important decisions and context

Avoid:
- Overly verbose descriptions
- Unnecessary technical jargon
- Missing verification steps
- Vague or unclear explanations
- Incomplete code examples

## Output Location

Unless specified otherwise, save workthrough documents to:
```
workthrough/YYYY-MM-DD-brief-description.md
```

Create the `workthrough/` directory if it doesn't exist.

## Integration with Workflow

This skill should be triggered automatically at the end of development sessions. The documentation serves as:
- A development log/journal
- Knowledge base for the project
- Onboarding material for new developers
- Reference for debugging similar issues
- Record of architectural decisions

Remember: Good documentation is a gift to your future self and your team.