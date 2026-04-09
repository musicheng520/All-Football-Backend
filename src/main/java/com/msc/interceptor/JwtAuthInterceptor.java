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
     * 公共接口（无需登录）
     * ==============================
     *
     * 使用 AntPathMatcher：
     * ✔ 支持 /xxx 和 /xxx/**
     * ✔ 比 contains 更安全
     */
    private static final List<String> PUBLIC_PATH_PATTERNS = List.of(
            "/auth/login",
            "/auth/register",

            "/fixtures", "/fixtures/**",
            "/teams", "/teams/**",
            "/players", "/players/**",
            "/news", "/news/**",

            "/ws/**",
            "/error"
    );

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    // 路径匹配器（支持通配符）
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    /**
     * ==============================
     * 核心拦截逻辑
     * ==============================
     */
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {

        // ✅ 1. 放行预检请求（CORS 必须）
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String uri = request.getRequestURI();

        // ✅ 2. 放行公开接口
        if (isPublicPath(uri)) {
            return true;
        }

        // ✅ 3. 获取 Authorization
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

            // ✅ 5. 校验 Redis 中的 token（防伪造 / 单端登录）
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

            // ✅ 7. 存入 ThreadLocal（后续业务使用）
            ThreadLocalUtil.set(userId);

            return true;

        } catch (Exception e) {
            // token 解析失败 / 过期
            response.setStatus(401);
            return false;
        }
    }

    /**
     * ==============================
     * 判断是否为公开路径
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
     * 请求结束清理 ThreadLocal
     * ==============================
     *
     * ❗ 非常重要（防止线程复用污染）
     */
    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {

        ThreadLocalUtil.remove();
    }
}