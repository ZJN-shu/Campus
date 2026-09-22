package com.campus.config;

import com.campus.interceptor.LoginInterceptor;
import com.campus.interceptor.RateLimitInterceptor;
import com.campus.interceptor.RefreshTokenInterceptor;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * CORS 跨域配置
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // 生产环境应限定为具体域名，如 "https://campus.example.com"
                .allowedOriginPatterns("http://localhost:*", "https://*.aliyuncs.com")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RateLimitInterceptor(stringRedisTemplate))
                .addPathPatterns("/**")
                .order(0);
        // 1. token刷新的拦截器（优先级高，先执行）
        registry.addInterceptor(new RefreshTokenInterceptor(stringRedisTemplate))
                .addPathPatterns("/**")  // 拦截所有路径
                .order(0);

        // 2. 登录拦截器（优先级低，后执行）
        registry.addInterceptor(new LoginInterceptor(stringRedisTemplate))
                .excludePathPatterns(
                        // 静态资源
                        "/**/*.html",
                        "/**/*.css",
                        "/**/*.js",
                        "/**/*.png",
                        "/**/*.jpg",
                        "/**/*.jpeg",
                        "/**/*.gif",
                        "/**/*.ico",

                        // 用户相关公开接口
                        "/student/code",        // 发送验证码
                        "/student/login",       // 登录
                        "/student/register",    // 注册（如果有）

                        // 帖子相关公开接口
                        "/post/hot",             // 热门帖子
                        "/post/category",        // 帖子列表
                        "/post/*",                // 单个帖子详情

                        // 商品相关公开接口
                        "/product/category",     // 商品列表
                        "/product/search",       // 商品搜索
                        "/product/*",             // 单个商品详情

                        // 跑腿任务公开浏览接口（接单/完成/取消在 Service 层校验登录）
                        "/errand/tasks/nearby",   // 任务列表
                        "/errand/task/*",         // 单个任务详情

                        // 热榜公开接口
                        "/hot/**",               // 热榜相关

                        // WebSocket 连接
                        "/ws/**",                // WebSocket 端点

                        // 监控端点
                        "/actuator/**",           // Spring Boot Actuator

                        // SEO
                        "/sitemap.xml"            // Sitemap
                )
                .order(1);
    }
}