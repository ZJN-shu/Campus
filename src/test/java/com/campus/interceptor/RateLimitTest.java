package com.campus.interceptor;

import com.campus.dto.LoginFormDTO;
import com.campus.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RateLimitTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static String authToken;
    private static final String TEST_PHONE = "13800138001";

    @BeforeAll
    static void setup() {
        log.info("========== 开始接口限流测试 ==========");
    }

    @AfterAll
    static void tearDown() {
        log.info("========== 接口限流测试完成 ==========");
    }

    // ==================== 辅助方法 ====================

    /**
     * 发送验证码并获取真实的验证码（从Redis读取）
     */
    private String sendCodeAndGetRealCode() {
        // 清除旧的验证码和限流计数
        String codeKey = "login:code:" + TEST_PHONE;
        String rateKey = "rate:limit:anonymous:/student/code";
        stringRedisTemplate.delete(codeKey);
        stringRedisTemplate.delete(rateKey);

        // 发送验证码
        String url = "/student/code?phone=" + TEST_PHONE;
        restTemplate.postForEntity(url, null, Result.class);

        // 等待Redis写入
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 从Redis获取验证码
        String code = stringRedisTemplate.opsForValue().get(codeKey);
        log.info("获取到验证码: {}", code);
        return code;
    }

    /**
     * 登录获取token
     */
    private String loginWithCode(String code) {
        String url = "/student/login";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        LoginFormDTO loginForm = new LoginFormDTO();
        loginForm.setPhone(TEST_PHONE);
        loginForm.setCode(code);

        HttpEntity<LoginFormDTO> request = new HttpEntity<>(loginForm, headers);
        ResponseEntity<Result> response = restTemplate.postForEntity(url, request, Result.class);

        if (response.getBody() != null && response.getBody().getSuccess()) {
            return response.getBody().getData().toString();
        }
        return null;
    }

    // ==================== 测试1：发送验证码限流 ====================

    @Test
    @Order(1)
    void testSendCodeRateLimit() throws InterruptedException {
        log.info("测试1：验证码发送限流（每小时最多5次）");

        String url = "/student/code?phone=" + TEST_PHONE;

        // 清除限流计数
        String rateKey = "rate:limit:anonymous:/student/code";
        stringRedisTemplate.delete(rateKey);

        int successCount = 0;
        int failCount = 0;

        for (int i = 1; i <= 10; i++) {
            ResponseEntity<Result> response = restTemplate.postForEntity(url, null, Result.class);

            if (response.getBody() != null && response.getBody().getSuccess()) {
                successCount++;
                log.info("第{}次发送成功", i);
            } else {
                failCount++;
                log.info("第{}次发送被限流: {}", i, response.getBody() != null ? response.getBody().getErrorMsg() : "unknown");
            }

            Thread.sleep(100);
        }

        log.info("验证码发送 - 成功: {}, 被限流: {}", successCount, failCount);
        assertTrue(successCount <= 5, "成功次数不应超过5次");
        assertTrue(failCount >= 4, "限流次数应至少4次");
    }

    // ==================== 测试2：登录接口限流 ====================

    @Test
    @Order(2)
    void testLoginRateLimit() throws InterruptedException {
        log.info("测试2：登录接口限流（每分钟最多10次）");

        // 先发送验证码获取真实验证码
        String realCode = sendCodeAndGetRealCode();
        assertNotNull(realCode, "验证码不能为空");

        String url = "/student/login";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 清除登录限流计数
        String rateKey = "rate:limit:anonymous:/student/login";
        stringRedisTemplate.delete(rateKey);

        int successCount = 0;
        int failCount = 0;
        String token = null;

        for (int i = 1; i <= 15; i++) {
            LoginFormDTO loginForm = new LoginFormDTO();
            loginForm.setPhone(TEST_PHONE);
            loginForm.setCode(realCode);  // 使用同一个验证码

            HttpEntity<LoginFormDTO> request = new HttpEntity<>(loginForm, headers);
            ResponseEntity<Result> response = restTemplate.postForEntity(url, request, Result.class);

            if (response.getBody() != null && response.getBody().getSuccess()) {
                successCount++;
                if (token == null && response.getBody().getData() != null) {
                    token = response.getBody().getData().toString();
                    authToken = token;
                }
                log.info("第{}次登录成功", i);
            } else {
                failCount++;
                String errorMsg = response.getBody() != null ? response.getBody().getErrorMsg() : "unknown";
                log.info("第{}次登录失败: {}", i, errorMsg);
            }

            Thread.sleep(100);
        }

        log.info("登录 - 成功: {}, 失败: {}", successCount, failCount);
        assertTrue(successCount >= 1, "应至少有1次登录成功");
        assertNotNull(authToken, "应获取到token");
    }

    // ==================== 测试3：发布帖子限流 ====================

    @Test
    @Order(3)
    void testCreatePostRateLimit() throws InterruptedException {
        log.info("测试3：发布帖子限流（每分钟最多3次）");

        assertNotNull(authToken, "需要先登录获取token");

        String url = "/post";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("authorization", authToken);

        int successCount = 0;
        int failCount = 0;

        for (int i = 1; i <= 10; i++) {
            String postBody = String.format(
                    "{\"title\":\"限流测试%d\",\"content\":\"这是第%d个测试帖子\",\"category\":\"学习交流\"}",
                    i, i);

            HttpEntity<String> request = new HttpEntity<>(postBody, headers);
            ResponseEntity<Result> response = restTemplate.postForEntity(url, request, Result.class);

            if (response.getBody() != null && response.getBody().getSuccess()) {
                successCount++;
                log.info("第{}次发布成功", i);
            } else {
                failCount++;
                log.info("第{}次发布被限流: {}", i, response.getBody() != null ? response.getBody().getErrorMsg() : "unknown");
            }

            Thread.sleep(100);
        }

        log.info("发布帖子 - 成功: {}, 被限流: {}", successCount, failCount);
        assertTrue(successCount <= 3, "成功次数不应超过3次");
        assertTrue(failCount >= 6, "限流次数应至少6次");
    }

    // ==================== 测试4：点赞接口限流 ====================

    @Test
    @Order(4)
    void testLikeRateLimit() throws InterruptedException {
        log.info("测试4：点赞接口限流（每秒最多5次）");

        assertNotNull(authToken, "需要先登录获取token");

        String url = "/post/like/1";
        HttpHeaders headers = new HttpHeaders();
        headers.set("authorization", authToken);

        int successCount = 0;
        int failCount = 0;

        for (int i = 1; i <= 10; i++) {
            HttpEntity<String> request = new HttpEntity<>(null, headers);
            ResponseEntity<Result> response = restTemplate.postForEntity(url, request, Result.class);

            if (response.getBody() != null && response.getBody().getSuccess()) {
                successCount++;
                log.info("第{}次点赞成功", i);
            } else {
                failCount++;
                log.info("第{}次点赞被限流: {}", i, response.getBody() != null ? response.getBody().getErrorMsg() : "unknown");
            }
        }

        log.info("点赞 - 成功: {}, 被限流: {}", successCount, failCount);
        assertTrue(successCount <= 6, "成功次数不应超过6次");
        assertTrue(failCount >= 4, "限流次数应至少4次");
    }

    // ==================== 测试5：评论接口限流 ====================

    @Test
    @Order(5)
    void testCommentRateLimit() throws InterruptedException {
        log.info("测试5：评论接口限流（每秒最多5次）");

        assertNotNull(authToken, "需要先登录获取token");

        String url = "/post/comment";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("authorization", authToken);

        int successCount = 0;
        int failCount = 0;

        for (int i = 1; i <= 10; i++) {
            String commentBody = String.format(
                    "{\"postId\":1,\"content\":\"测试评论%d\"}", i);

            HttpEntity<String> request = new HttpEntity<>(commentBody, headers);
            ResponseEntity<Result> response = restTemplate.postForEntity(url, request, Result.class);

            if (response.getBody() != null && response.getBody().getSuccess()) {
                successCount++;
                log.info("第{}次评论成功", i);
            } else {
                failCount++;
                log.info("第{}次评论被限流: {}", i, response.getBody() != null ? response.getBody().getErrorMsg() : "unknown");
            }
        }

        log.info("评论 - 成功: {}, 被限流: {}", successCount, failCount);
        assertTrue(successCount <= 6, "成功次数不应超过6次");
        assertTrue(failCount >= 4, "限流次数应至少4次");
    }
}