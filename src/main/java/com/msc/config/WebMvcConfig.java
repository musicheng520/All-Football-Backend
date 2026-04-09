package com.msc.config;

import com.msc.interceptor.JwtAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtAuthInterceptor jwtAuthInterceptor;

    /**
     * ==============================
     * 1️ JWT 拦截器配置
     * ==============================
     *
     * 拦截所有请求，校验 token
     * 但放行：
     * - 登录 / 注册
     * - 公共数据接口（比赛 / 球队 / 球员 / 新闻）
     * - WebSocket 连接
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/**/auth/login",
                        "/**/auth/register",
                        "/**/fixtures/**",
                        "/**/teams/**",
                        "/**/players/**",
                        "/**/news/**",
                        "/**/ws/**"
                );
    }

    /**
     * ==============================
     * 2️ 全局 CORS（MVC 层）
     * ==============================
     *
     * 作用：
     * - 处理正常 Controller 请求
     *
     * 为什么用 allowedOriginPatterns：
     * - 支持子域名
     * - 支持 credentials（cookie/token）
     * - Spring 官方推荐（比 allowedOrigins 更灵活）
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {

        registry.addMapping("/**")
                .allowedOriginPatterns(
                        "http://localhost:5173",
                        "https://sicheng55.com",
                        "https://www.sicheng55.com"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .exposedHeaders("Authorization") // 前端可以读取 token
                .allowCredentials(true)
                .maxAge(3600); // 预检请求缓存 1 小时
    }

    /**
     * ==============================
     * 3️⃣ 全局 CORS（Filter 层 - 关键）
     * ==============================
     *
     * 为什么必须加这一层：
     *
     *  MVC CORS 只能处理 Controller 返回
     *
     *  拦截器报错（401 / 403）
     *  Spring Security（如果有）
     *  未进入 Controller 的请求
     *
     * 都不会带 CORS 头 → 浏览器报：
     * "CORS error"（其实是后端报错）
     *
     *  Filter 层可以保证：
     * 所有响应（包括异常）都带 CORS
     *
     *  这是线上环境必须的写法
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE) // 保证最先执行
    public CorsFilter corsFilter() {

        CorsConfiguration config = new CorsConfiguration();

        // 允许的前端来源（支持多个域名）
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:5173",
                "https://sicheng55.com",
                "https://www.sicheng55.com"
        ));

        // 允许的请求方法
        config.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));

        // 允许的请求头
        config.setAllowedHeaders(List.of("*"));

        // 允许前端访问的响应头（比如 token）
        config.setExposedHeaders(List.of("Authorization"));

        // 是否允许携带 cookie / token
        config.setAllowCredentials(true);

        // 预检请求缓存时间
        config.setMaxAge(3600L);

        // 应用到所有路径
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}