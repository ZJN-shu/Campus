package com.campus.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
// 删除这行：@EnableElasticsearchRepositories
public class ElasticsearchConfig {

    @Value("${spring.elasticsearch.uris:http://localhost:9200}")
    private String esUris;

    @Bean
    public RestHighLevelClient restHighLevelClient() {
        String[] parts = esUris.replace("http://", "").split(":");
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);

        return new RestHighLevelClient(
                RestClient.builder(new HttpHost(host, port, "http"))
        );
    }
}