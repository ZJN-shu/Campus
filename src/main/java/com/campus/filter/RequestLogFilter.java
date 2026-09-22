package com.campus.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * 请求上下文过滤器：为每个请求注入 MDC 字段，用于结构化日志追踪
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestLogFilter implements Filter {

    private static final String TRACE_ID = "traceId";
    private static final String REQUEST_URI = "requestUri";
    private static final String USER_ID = "userId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        try {
            // 生成链路追踪ID（优先从请求头获取，支持上游透传）
            String traceId = req.getHeader("X-Trace-Id");
            if (traceId == null || traceId.isEmpty()) {
                traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            }
            MDC.put(TRACE_ID, traceId);
            MDC.put(REQUEST_URI, req.getRequestURI());

            // 响应头返回 traceId，方便前端排查
            res.setHeader("X-Trace-Id", traceId);

            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
