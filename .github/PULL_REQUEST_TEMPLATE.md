## 배경                                      
                                                                 
  <!-- 이 PR이 왜 필요한지 설명해주세요 -->                      
  > 관련 이슈: #이슈번호                                         
  > 작업 브랜치: `feature/#이슈번호-주제`                        
                                                                 
  **문제 상황:**                                                 
  - 문제 상황 작성       
                                                                 
  **목적:**                                                      
  - 구현 목적 작성                                   
                                                                 
<br />                                                          
                                                                 
  ## 변경사항                                 
                                                               
  ### 주요 변경 내용                                             
  | 구분 | 파일 | 변경 내용 |
  |------|------|-----------|                                    
  | 추가 | `AuthController.java` | `/auth/refresh` 엔드포인트 추가 |                                                         
  | 수정 | `JwtFilter.java` | 만료 토큰 예외 처리 분기 추가 |             
                                                                  
                                                                 
  ### 주요 로직 변경 요약                                        
  -                                                           
                                                                 
<br />                                                           
                                                                 
  ## 테스트 결과                           ─
                                                               
  ### 단위 테스트                                                
  - [ ] `TokenServiceTest` — 정상 재발급 / 만료된 Refresh Token /
   존재하지 않는 토큰                                            
  - [ ] `JwtFilterTest` — 만료 토큰 예외 분기 처리
                                                                 
  ### 통합 테스트                                                
  - [ ] `POST /auth/refresh` — 정상 케이스 200 OK                
  - [ ] `POST /auth/refresh` — Refresh Token 없음 → 401          
  - [ ] `POST /auth/refresh` — Refresh Token 만료 → 401          
                                                                 
  ### 수동 테스트                                                
  1. 로그인 → Access Token 발급 확인                             
  2. Access Token 만료 후 /auth/refresh 호출 → 신규 토큰 반환    
  확인                                                       
  3. Refresh Token 만료 후 호출 → 401 반환 확인                  
   
  ### CI                                                         
  - [ ] 빌드 통과 
  - [ ] 전체 테스트 통과                                         
                                                                 
<br />                                                             
                                                                 
  ## 리스크 (Risks)

  | 수준 | 항목 | 설명 |                                         
  |------|------|------|
  | 중 | Redis 의존성 추가 | Redis 장애 시 로그인 불가 |                                                       
  | 하 | 토큰 탈취 시 재발급 가능 | Refresh Token Rotation 미구현 상태 |                                                        
  | 하 | 기존 클라이언트 호환성 | errorCode 필드 추가로 기존 파싱 / 로직 영향 없음 |                                              
                  
  ---                                                                                                                 
                  
  ## 리뷰 포인트 & 궁금한 점                              
   
  > 리뷰어가 집중해서 봐주셨으면 하는 부분을 명시해주세요        
                  
  - **안정성:** Redis 장애 시 fallback 처리가 필요한지 의견 부탁드립니다.
  - **성능:** Refresh Token 조회 시 Redis key 설계가 적절한지 확인해주시면 감사드리겠습니다!                                         
  - **가독성:** `JwtFilter` 예외 분기 흐름이 명확한지 검토해주시면 감사드리겠습니다!                                                   
                  
