package com.msc.interceptor;

import com.msc.utils.JwtUtil;
import com.msc.utils.ThreadLocalUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String uri = request.getRequestURI();

        // Public APIs
        if (uri.equals("/auth/login")
                || uri.equals("/auth/register")
                || uri.startsWith("/fixtures")
                || uri.startsWith("/teams")
                || uri.startsWith("/players")
                || uri.startsWith("/news")) {
            return true;
        }

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            response.setStatus(401);
            return false;
        }

        String token = header.substring(7);

        try {

            Claims claims = jwtUtil.parseToken(token);

            Long userId = Long.valueOf(claims.getSubject());
            String role = (String) claims.get("role");

            String redisToken = redisTemplate.opsForValue()
                    .get("login:" + userId);

            if (redisToken == null || !redisToken.equals(token)) {
                response.setStatus(401);
                return false;
            }

            if (uri.startsWith("/admin") && !"ADMIN".equals(role)) {
                response.setStatus(403);
                return false;
            }

            ThreadLocalUtil.set(userId);

            return true;

        } catch (Exception e) {
            response.setStatus(401);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        ThreadLocalUtil.remove();
    }

}