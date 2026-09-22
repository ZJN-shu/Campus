package com.campus.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 自定义 Prometheus 监控指标
 */
@Configuration
public class PrometheusMetricsConfig {

    /**
     * HTTP 请求总数计数器
     */
    @Bean
    public Counter httpRequestsTotal(MeterRegistry registry) {
        return Counter.builder("campus_http_requests_total")
                .description("Total HTTP requests")
                .register(registry);
    }

    /**
     * 帖子发布计数器
     */
    @Bean
    public Counter postPublishTotal(MeterRegistry registry) {
        return Counter.builder("campus_post_publish_total")
                .description("Total posts published")
                .register(registry);
    }

    /**
     * 订单创建计数器
     */
    @Bean
    public Counter orderCreateTotal(MeterRegistry registry) {
        return Counter.builder("campus_order_create_total")
                .description("Total orders created")
                .register(registry);
    }

    /**
     * 跑腿任务发布计数器
     */
    @Bean
    public Counter errandTaskTotal(MeterRegistry registry) {
        return Counter.builder("campus_errand_task_total")
                .description("Total errand tasks published")
                .register(registry);
    }

    /**
     * 文件上传计数器
     */
    @Bean
    public Counter fileUploadTotal(MeterRegistry registry) {
        return Counter.builder("campus_file_upload_total")
                .description("Total file uploads")
                .tag("type", "oss")
                .register(registry);
    }

    /**
     * WebSocket 在线用户数（Gauge）
     */
    @Bean
    public AtomicLong websocketOnlineCount(MeterRegistry registry) {
        AtomicLong count = new AtomicLong(0);
        registry.gauge("campus_websocket_online", count);
        return count;
    }

    /**
     * 消息队列处理耗时
     */
    @Bean
    public Timer mqProcessTimer(MeterRegistry registry) {
        return Timer.builder("campus_mq_process_duration")
                .description("Message queue processing duration")
                .register(registry);
    }
}
