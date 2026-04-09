package com.msc.config;

import com.msc.interceptor.JwtAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtAuthInterceptor jwtAuthInterceptor;

    /**
     * ==============================
     * JWT 拦截器配置
     * ==============================
     *
     * 拦截所有请求，但放行：
     * - 登录 / 注册
     * - 公共数据接口（fixtures / teams / players / news）
     * - WebSocket
     *
     * ⚠️ 注意：
     * ❌ 这里不再处理 CORS
     * ✅ CORS 已统一交给 CorsFilter（Filter 层）
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/login",
                        "/auth/register",

                        "/fixtures", "/fixtures/**",
                        "/teams", "/teams/**",
                        "/players", "/players/**",
                        "/news", "/news/**",

                        "/ws/**",
                        "/error"
                );
    }
}