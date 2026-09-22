package com.campus.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;
import org.springframework.data.redis.stream.Subscription;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.time.Duration;
import java.util.concurrent.Executor;

@Slf4j
@Configuration
public class StreamConfig {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final String STREAM_KEY_LIKE = "stream:like";
    private static final String STREAM_KEY_COMMENT = "stream:comment";
    private static final String STREAM_KEY_VIEW = "stream:view";

    private static final String GROUP_NAME = "campus-group";

    /**
     * 初始化Stream和消费者组
     */
    @PostConstruct
    public void initStreams() {
        // 创建Stream和消费者组（如果不存在）
        createStreamAndGroup(STREAM_KEY_LIKE);
        createStreamAndGroup(STREAM_KEY_COMMENT);
        createStreamAndGroup(STREAM_KEY_VIEW);
    }

    private void createStreamAndGroup(String streamKey) {
        try {
            // 尝试创建消费者组（Stream会自动创建）
            stringRedisTemplate.opsForStream().createGroup(streamKey, GROUP_NAME);
            log.info("创建消费者组成功: {}", streamKey);
        } catch (Exception e) {
            // 消费者组已存在，忽略异常
            log.debug("消费者组已存在: {}", streamKey);
        }
    }

    /**
     * 配置消息监听容器
     */
    @Bean
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer(
            RedisConnectionFactory connectionFactory) {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("stream-consumer-");
        executor.initialize();

        StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainerOptions.builder()
                        .executor(executor)
                        .pollTimeout(Duration.ofSeconds(2))
                        .build();

        return StreamMessageListenerContainer.create(connectionFactory, options);
    }
}