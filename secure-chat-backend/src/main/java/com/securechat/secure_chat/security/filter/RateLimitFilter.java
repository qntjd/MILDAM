package com.securechat.secure_chat.security.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class RateLimitFilter implements Filter {

    // IP별 버킷 저장
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    // Rate Limit 적용할 경로
    private static final Map<String, int[]> LIMITS = Map.of(
        "/api/auth/login",   new int[]{5,  60},  // 1분에 5회
        "/api/auth/signup",  new int[]{3,  60},  // 1분에 3회
        "/api/auth/refresh", new int[]{10, 60}   // 1분에 10회
    );

    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  req  = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String path = req.getRequestURI();

        // Rate Limit 적용 경로인지 확인
        int[] limit = LIMITS.get(path);
        if (limit == null) {
            chain.doFilter(request, response);
            return;
        }

        String ip     = getClientIp(req);
        String key    = ip + ":" + path;
        Bucket bucket = buckets.computeIfAbsent(key,
                k -> createBucket(limit[0], limit[1]));

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            log.warn("Rate Limit 초과 — IP: {}, Path: {}", ip, path);
            resp.setStatus(429); // Too Many Requests
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write(
                "{\"status\":429,\"message\":\"요청이 너무 많습니다. 잠시 후 다시 시도해주세요\"}"
            );
        }
    }

    private Bucket createBucket(int capacity, int refillSeconds) {
        Bandwidth limit = Bandwidth.classic(
                capacity,
                Refill.intervally(capacity, Duration.ofSeconds(refillSeconds))
        );
        return Bucket.builder().addLimit(limit).build();
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}