package com.msc.config;

import com.msc.interceptor.JwtAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtAuthInterceptor jwtAuthInterceptor;

    /**
     *  拦截器配置
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/login",
                        "/auth/register",

                        // 公共接口
                        "/fixtures/**",
                        "/teams/**",
                        "/players/**",
                        "/news/**",

                        //  关键：放行 websocket
                        "/ws/**"
                );
    }

    /**
     *  全局 CORS（最终版）
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {

        registry.addMapping("/**")
                .allowedOrigins(
                        "http://localhost:5173",
                        "https://sicheng55.com",
                        "https://www.sicheng55.com"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}