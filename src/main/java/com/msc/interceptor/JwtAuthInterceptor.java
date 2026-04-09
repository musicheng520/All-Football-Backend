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

    /**
     * ==============================
     * 1️⃣ 公共接口路径（无需登录）
     * ==============================
     *
     * 使用 AntPathMatcher 支持：
     * - /xxx
     * - /xxx/**
     *
     * 比 contains() 更安全、可维护
     */
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

    // 路径匹配器（支持通配符）
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    /**
     * ==============================
     * 2️⃣ 请求前拦截（核心逻辑）
     * ==============================
     */
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {

        // ✅ 1. 放行 OPTIONS（预检请求）
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String uri = request.getRequestURI();

        // ✅ 2. 放行公共接口
        if (isPublicPath(uri)) {
            return true;
        }

        // ✅ 3. 获取 Authorization 头
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            response.setStatus(401);
            return false;
        }

        String token = header.substring(7);

        try {
            // ✅ 4. 解析 JWT
            Claims claims = jwtUtil.parseToken(token);

            Long userId = Long.valueOf(claims.getSubject());
            String role = (String) claims.get("role");

            // ✅ 5. 校验 Redis 中 token（防止伪造 / 多端冲突）
            String redisToken = redisTemplate.opsForValue()
                    .get("login:" + userId);

            if (redisToken == null || !redisToken.equals(token)) {
                response.setStatus(401);
                return false;
            }

            // ✅ 6. 权限控制（管理员接口）
            if (uri.startsWith("/admin") && !"ADMIN".equals(role)) {
                response.setStatus(403);
                return false;
            }

            // ✅ 7. 存入 ThreadLocal（供 Controller 使用）
            ThreadLocalUtil.set(userId);

            return true;

        } catch (Exception e) {
            // JWT 解析失败 / 过期
            response.setStatus(401);
            return false;
        }
    }

    /**
     * ==============================
     * 3️⃣ 判断是否为公开路径
     * ==============================
     */
    private boolean isPublicPath(String uri) {
        for (String pattern : PUBLIC_PATH_PATTERNS) {
            if (antPathMatcher.match(pattern, uri)) {
                return true;
            }
        }
        return false;
    }

    /**
     * ==============================
     * 4️⃣ 请求完成后清理 ThreadLocal
     * ==============================
     *
     * 防止线程复用导致数据污染（非常关键）
     */
    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {

        ThreadLocalUtil.remove();
    }
}