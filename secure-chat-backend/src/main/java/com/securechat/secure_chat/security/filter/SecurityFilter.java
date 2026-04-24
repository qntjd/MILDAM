package com.securechat.secure_chat.security.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

@Slf4j
@Component
public class SecurityFilter implements Filter {

    private static final Pattern SQL_INJECTION = Pattern.compile(
        "(?i)('\\s*(or|and)\\s*'?\\d)" +
        "|((?i)union\\s+(all\\s+)?select)" +
        "|((?i)drop\\s+(table|database))" +
        "|((?i)insert\\s+into)" +
        "|((?i)delete\\s+from)" +
        "|((?i)exec(ute)?\\s*\\()" +
        "|(--\\s*$)" +
        "|((?i)/\\*.*\\*/)",
        Pattern.DOTALL
    );

    private static final Pattern XSS = Pattern.compile(
        "(?i)(<\\s*script[^>]*>)" +
        "|((?i)</\\s*script\\s*>)" +
        "|((?i)javascript\\s*:)" +
        "|((?i)on(load|error|click|mouseover|focus)\\s*=)" +
        "|((?i)<\\s*iframe)" +
        "|((?i)eval\\s*\\()" +
        "|((?i)document\\s*\\.\\s*cookie)",
        Pattern.DOTALL
    );

    private static final Pattern PATH_TRAVERSAL = Pattern.compile(
        "(\\.{2}[/\\\\])|(%2e{2}%2f)|(%2e%2e/)",
        Pattern.CASE_INSENSITIVE
    );

    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  req  = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String path  = req.getRequestURI();
        String query = req.getQueryString() != null ? req.getQueryString() : "";

        // URL 디코딩 후 검사 (인코딩 우회 방지)
        String decodedPath  = decode(path);
        String decodedQuery = decode(query);
        String target = decodedPath + decodedQuery;

        if (isMalicious(target)) {
            log.warn("악성 요청 차단 - IP: {}, URI: {}, Query: {}",
                    req.getRemoteAddr(), path, query);
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("잘못된 요청입니다");
            return;
        }

        resp.setHeader("X-Content-Type-Options",   "nosniff");
        resp.setHeader("X-Frame-Options",           "DENY");
        resp.setHeader("X-XSS-Protection",          "1; mode=block");
        resp.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        resp.setHeader("Referrer-Policy",           "strict-origin-when-cross-origin");

        chain.doFilter(request, response);
    }

    private boolean isMalicious(String input) {
        return SQL_INJECTION.matcher(input).find()
            || XSS.matcher(input).find()
            || PATH_TRAVERSAL.matcher(input).find();
    }

    private String decode(String value) {
        try {
            // 이중 인코딩 우회 방지를 위해 2번 디코딩
            String decoded = URLDecoder.decode(value, StandardCharsets.UTF_8);
            return URLDecoder.decode(decoded, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }
}