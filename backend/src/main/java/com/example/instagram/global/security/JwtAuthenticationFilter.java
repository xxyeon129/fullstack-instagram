package com.example.instagram.global.security;

import com.example.instagram.global.exception.ErrorCode;
import com.example.instagram.global.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(AUTHORIZATION_HEADER);

        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length());

        try {
            Long userId = jwtTokenProvider.getUserId(token);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JwtException e) {
            writeJsonUnauthorized(response, ErrorCode.INVALID_ACCESS_TOKEN);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeJsonUnauthorized(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        // NOTE: HTTP 응답 상태 코드를 401 Unauthorized로 설정
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        // NOTE: 응답 본문이 JSON이라는 뜻 Content-Type 헤더를 application/json 형식으로 맞춤
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        // NOTE: 한글 등 유니코드 문자가 깨지지 않도록 응답 문자 인코딩을 UTF-8로 지정
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(
                response.getOutputStream(),
                // NOTE: JSON 직접 출력: Content-Type: application/json, UTF-8, HTTP 401과 함께 본문을 씀
                // 응답 예시: {"success":false,"message":"유효하지 않거나 만료된 액세스 토큰입니다.","data":null}
                ApiResponse.fail(errorCode.getMessage()));
    }
}
