package com.campus.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class WebUtils {

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * 返回未登录错误（401）
     */
    public static void renderUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("errorMsg", "请先登录");
        result.put("code", 401);

        response.getWriter().write(mapper.writeValueAsString(result));
    }

    /**
     * 返回无权限错误（403）
     */
    public static void renderForbidden(HttpServletResponse response) throws IOException {
        response.setStatus(403);
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("errorMsg", "没有权限");
        result.put("code", 403);

        response.getWriter().write(mapper.writeValueAsString(result));
    }

    /**
     * 返回服务器错误（500）
     */
    public static void renderServerError(HttpServletResponse response) throws IOException {
        response.setStatus(500);
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("errorMsg", "服务器内部错误");
        result.put("code", 500);

        response.getWriter().write(mapper.writeValueAsString(result));
    }

    /**
     * 返回自定义错误
     */
    public static void renderError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("errorMsg", message);
        result.put("code", status);

        response.getWriter().write(mapper.writeValueAsString(result));
    }

    /**
     * 返回成功响应（用于其他场景）
     */
    public static void renderSuccess(HttpServletResponse response, Object data) throws IOException {
        response.setStatus(200);
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", data);
        result.put("code", 200);

        response.getWriter().write(mapper.writeValueAsString(result));
    }
}