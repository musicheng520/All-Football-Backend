package com.msc.interceptor;

import com.msc.utils.JwtUtil;
import com.msc.utils.ThreadLocalUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {

    private static final List<String> PUBLIC_PATH_PATTERNS = List.of(
            "/auth/login",
            "/auth/register",
            "/fixtures",
            "/fixtures/**",
            "/teams",
            "/teams/**",
            "/players",
            "/players/**",
            "/news",
            "/news/**",
            "/ws/**",
            "/error"
    );

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {

        // 放行预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String uri = request.getRequestURI();

        // 放行公开接口
        if (isPublicPath(uri)) {
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

    private boolean isPublicPath(String uri) {
        for (String pattern : PUBLIC_PATH_PATTERNS) {
            if (antPathMatcher.match(pattern, uri)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {

        ThreadLocalUtil.remove();
    }
}
