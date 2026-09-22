package com.campus.service;

import com.alibaba.fastjson.JSON;
import com.campus.document.ProductDocument;
import com.campus.entity.Product;
import com.campus.entity.Student;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ElasticsearchService {

    @Autowired
    private RestHighLevelClient client;

    @Autowired
    private IStudentService studentService;

    @Value("${elasticsearch.index.product:campus_product}")
    private String productIndex;

    /**
     * 启动时自动创建索引
     */
    @PostConstruct
    public void createIndexIfNotExists() {
        try {
            // 确保索引名称不为空
            if (productIndex == null || productIndex.isEmpty()) {
                log.error("索引名称未配置，请检查 application.yml 中的 elasticsearch.index.product");
                return;
            }
            GetIndexRequest getRequest = new GetIndexRequest(productIndex);
            boolean exists = client.indices().exists(getRequest, RequestOptions.DEFAULT);

            if (!exists) {
                CreateIndexRequest createRequest = new CreateIndexRequest(productIndex);
                // 设置映射
                String mapping = "{\n" +
                        "  \"properties\": {\n" +
                        "    \"id\": { \"type\": \"long\" },\n" +
                        "    \"title\": { \"type\": \"text\", \"analyzer\": \"ik_max_word\" },\n" +
                        "    \"description\": { \"type\": \"text\", \"analyzer\": \"ik_max_word\" },\n" +
                        "    \"price\": { \"type\": \"double\" },\n" +
                        "    \"category\": { \"type\": \"keyword\" },\n" +
                        "    \"quality\": { \"type\": \"keyword\" },\n" +
                        "    \"sellerId\": { \"type\": \"long\" },\n" +
                        "    \"sellerName\": { \"type\": \"keyword\" },\n" +
                        "    \"viewCount\": { \"type\": \"integer\" },\n" +
                        "    \"favoriteCount\": { \"type\": \"integer\" },\n" +
                        "    \"status\": { \"type\": \"integer\" },\n" +
                        "    \"createTime\": { \"type\": \"date\" }\n" +
                        "  }\n" +
                        "}";

                createRequest.mapping(mapping, XContentType.JSON);
                client.indices().create(createRequest, RequestOptions.DEFAULT);
                log.info("ES索引创建成功: {}", productIndex);
            } else {
                log.info("ES索引已存在: {}", productIndex);
            }
        } catch (Exception e) {
            log.error("创建ES索引失败", e);
        }
    }
    /**
     * 商品文档转换
     */
    private ProductDocument convertToDocument(Product product) {
        ProductDocument doc = new ProductDocument();
        doc.setId(product.getId());
        doc.setTitle(product.getTitle());
        doc.setDescription(product.getDescription());
        doc.setPrice(product.getPrice());
        doc.setCategory(product.getCategory());
        doc.setQuality(product.getQuality());
        doc.setSellerId(product.getSellerId());
        doc.setViewCount(product.getViewCount());
        doc.setFavoriteCount(product.getFavoriteCount());
        doc.setStatus(product.getStatus());
        doc.setCreateTime(product.getCreateTime());

        // 查询卖家名称
        Student seller = studentService.getById(product.getSellerId());
        if (seller != null) {
            doc.setSellerName(seller.getNickName());
        }

        return doc;
    }

    /**
     * 添加/更新商品到ES
     */
    public void indexProduct(Product product) {
        try {
            ProductDocument doc = convertToDocument(product);
            IndexRequest request = new IndexRequest(productIndex)
                    .id(String.valueOf(product.getId()))
                    .source(JSON.toJSONString(doc), XContentType.JSON);

            client.index(request, RequestOptions.DEFAULT);
            log.info("ES索引成功: productId={}", product.getId());
        } catch (IOException e) {
            log.error("ES索引失败: productId={}", product.getId(), e);
        }
    }

    /**
     * 从ES删除商品
     */
    public void deleteProduct(Long productId) {
        try {
            DeleteRequest request = new DeleteRequest(productIndex, String.valueOf(productId));
            client.delete(request, RequestOptions.DEFAULT);
            log.info("ES删除成功: productId={}", productId);
        } catch (IOException e) {
            log.error("ES删除失败: productId={}", productId, e);
        }
    }

    /**
     * 搜索商品
     */
    public List<ProductDocument> searchProducts(String keyword, String category,
                                                Integer page, Integer size) {
        try {
            SearchRequest request = new SearchRequest(productIndex);
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

            // 构建查询条件
            if (keyword != null && !keyword.isEmpty()) {
                // 关键词搜索（标题或描述）
                sourceBuilder.query(QueryBuilders.multiMatchQuery(keyword, "title", "description"));
            } else {
                sourceBuilder.query(QueryBuilders.matchAllQuery());
            }

            // 分类筛选
            if (category != null && !category.isEmpty() && !"all".equals(category)) {
                sourceBuilder.query(QueryBuilders.boolQuery()
                        .must(sourceBuilder.query())
                        .filter(QueryBuilders.termQuery("category", category)));
            }

            // 分页
            int from = (page - 1) * size;
            sourceBuilder.from(from).size(size);

            request.source(sourceBuilder);
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);

            // 解析结果
            List<ProductDocument> results = new ArrayList<>();
            var hits = response.getHits().getHits();
            for (var hit : hits) {
                ProductDocument doc = JSON.parseObject(hit.getSourceAsString(), ProductDocument.class);
                doc.setId(Long.valueOf(hit.getId()));
                results.add(doc);
            }

            log.info("ES搜索: keyword={}, 结果数={}", keyword, results.size());
            return results;

        } catch (IOException e) {
            log.error("ES搜索失败: keyword={}", keyword, e);
            return new ArrayList<>();
        }
    }
}