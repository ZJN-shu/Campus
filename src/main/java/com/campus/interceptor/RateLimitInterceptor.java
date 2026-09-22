package com.campus.interceptor;

import com.campus.annotation.RateLimit;
import com.campus.utils.StudentHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisScript<List<Long>> redisScript;

    public RateLimitInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;

        // 创建并配置 DefaultRedisScript
        DefaultRedisScript<List<Long>> script = new DefaultRedisScript<>();
        script.setScriptText(LUA_SCRIPT);
        script.setResultType((Class<List<Long>>) (Class<?>) List.class);
        this.redisScript = script;
    }
    // Lua脚本：原子性获取令牌
    private static final String LUA_SCRIPT = """
        local key = KEYS[1]
        local capacity = tonumber(ARGV[1])
        local refillRate = tonumber(ARGV[2])
        local now = tonumber(ARGV[3])
        local requested = tonumber(ARGV[4])
        
        -- 获取上次补充时间和当前令牌数
        local lastRefillTime = redis.call('HGET', key, 'lastRefillTime')
        local tokens = redis.call('HGET', key, 'tokens')
        
        if lastRefillTime == false or tokens == false then
            -- 初始状态：满桶
            lastRefillTime = now
            tokens = capacity
        else
            lastRefillTime = tonumber(lastRefillTime)
            tokens = tonumber(tokens)
        end
        
        -- 计算应该补充的令牌数
        local timePassed = now - lastRefillTime
        local refillTokens = math.floor(timePassed * refillRate)
        
        if refillTokens > 0 then
            tokens = math.min(capacity, tokens + refillTokens)
            lastRefillTime = now
        end
        
        -- 判断是否允许请求通过
        local allowed = 0
        if tokens >= requested then
            allowed = 1
            tokens = tokens - requested
        end
        
        -- 保存状态
        redis.call('HMSET', key, 'tokens', tokens, 'lastRefillTime', lastRefillTime)
        redis.call('EXPIRE', key, math.ceil(capacity / refillRate) * 2 + 60)
        
        return {allowed, tokens, capacity}
        """;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);

        if (rateLimit == null) {
            return true;
        }

        // 兼容旧注解的value参数
        long capacity;
        long refillRate;

        if (rateLimit.value() != -1) {
            // 旧配置：value=2, timeout=60 转换为令牌桶参数
            capacity = rateLimit.value();
            refillRate = (long) Math.ceil(rateLimit.value() * 1.0 / rateLimit.timeout());
            log.warn("使用旧限流注解，已自动转换为令牌桶: capacity={}, refillRate={}/秒",
                    capacity, refillRate);
        } else {
            capacity = rateLimit.capacity();
            refillRate = rateLimit.refillRate();
        }

        // 获取用户标识
        Long userId = StudentHolder.getStudentId();
        String requestUri = request.getRequestURI();
        String key = "rate:token:" + (userId != null ? userId : "anonymous") + ":" + requestUri;

        // 当前时间（秒）
        long now = System.currentTimeMillis() / 1000;

        // 【修复点2】：执行Lua脚本时确保返回类型正确
        List<Long> result = stringRedisTemplate.execute(
                redisScript,
                Arrays.asList(key),  // KEYS列表
                String.valueOf(capacity),   // ARGV[1]
                String.valueOf(refillRate), // ARGV[2]
                String.valueOf(now),        // ARGV[3]
                "1"                         // ARGV[4]
        );

        // 防止空指针
        if (result == null || result.size() < 3) {
            log.error("令牌桶执行失败，result={}", result);
            renderErrorResponse(response, "系统繁忙，请稍后再试");
            return false;
        }

        Long allowed = result.get(0);
        Long currentTokens = result.get(1);
        Long maxCapacity = result.get(2);

        if (allowed == 0) {
            log.warn("令牌桶限流: userId={}, uri={}, 当前令牌={}/{}, 补充速率={}/秒",
                    userId, requestUri, currentTokens, maxCapacity, refillRate);
            renderErrorResponse(response, rateLimit.message());
            return false;
        }

        log.debug("令牌桶限流通过: userId={}, uri={}, 剩余令牌={}/{}",
                userId, requestUri, currentTokens, maxCapacity);
        return true;
    }

    private void renderErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"success\":false,\"errorMsg\":\"" + message + "\"}");
    }
}