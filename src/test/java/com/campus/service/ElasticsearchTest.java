package com.campus.service;

import com.campus.document.ProductDocument;
import com.campus.dto.StudentDTO;
import com.campus.entity.Product;
import com.campus.mapper.ProductMapper;
import com.campus.utils.StudentHolder;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.IndicesClient;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;

@Slf4j
@SpringBootTest
public class ElasticsearchTest {

    @Autowired
    private RestHighLevelClient client;

    @Autowired
    private ElasticsearchService esService;

    @Autowired
    private IProductService productService;

    @Autowired
    private ProductMapper productMapper;

    /**
     * 测试0：模拟登录
     */
    @Test
    void testLogin() {
        StudentDTO studentDTO = new StudentDTO();
        studentDTO.setId(1L);
        studentDTO.setNickName("测试用户");
        StudentHolder.saveStudent(studentDTO);
        log.info("模拟登录成功: userId=1");
    }

    /**
     * 测试1：ES 连接是否正常
     */
    @Test
    void testConnection() throws IOException {
        var info = client.info(RequestOptions.DEFAULT);
        log.info("ES 连接成功！版本：{}", info.getVersion().getNumber());
        log.info("集群名称：{}", info.getClusterName());
    }

    /**
     * 测试2：发布商品并同步到 ES（需要先运行 testLogin）
     */
    @Test
    void testPublishAndSyncToEs() {
        // 先模拟登录
        testLogin();

        log.info("========== 测试：发布商品并同步到 ES ==========");

        Product product = new Product();
        product.setTitle("测试商品-ES搜索");
        product.setDescription("这是一个用于测试 Elasticsearch 搜索功能的商品");
        product.setPrice(new java.math.BigDecimal("99.00"));
        product.setCategory("测试");
        product.setStock(100);
        product.setReservedStock(0);
        product.setVersion(0);

        var result = productService.publishProduct(product);
        log.info("发布结果：{}", result);

        if (result.getSuccess()) {
            log.info("商品发布成功，ID：{}", result.getData());
        }
    }
    /**
     * 测试3：搜索商品（ES 搜索）
     */
    @Test
    void testSearchFromEs() {
        log.info("========== 测试：ES 搜索商品 ==========");

        String keyword = "测试";
        List<ProductDocument> results = esService.searchProducts(keyword, null, 1, 10);

        log.info("搜索关键词：{}", keyword);
        log.info("搜索结果数：{}", results.size());

        for (ProductDocument doc : results) {
            log.info("  - ID: {}, 标题: {}, 价格: {}",
                    doc.getId(), doc.getTitle(), doc.getPrice());
        }
    }

    /**
     * 测试4：分类筛选搜索
     */
    @Test
    void testSearchWithCategory() {
        log.info("========== 测试：带分类的 ES 搜索 ==========");

        String keyword = "测试";
        String category = "测试";
        List<ProductDocument> results = esService.searchProducts(keyword, category, 1, 10);

        log.info("搜索关键词：{}，分类：{}", keyword, category);
        log.info("搜索结果数：{}", results.size());

        for (ProductDocument doc : results) {
            log.info("  - ID: {}, 标题: {}, 分类: {}",
                    doc.getId(), doc.getTitle(), doc.getCategory());
        }
    }

    /**
     * 测试5：更新商品并同步 ES
     */
    @Test
    void testUpdateAndSyncToEs() {
        log.info("========== 测试：更新商品并同步到 ES ==========");

        // 先查询一个商品
        List<Product> products = productMapper.selectList(null);
        if (products.isEmpty()) {
            log.warn("没有商品数据，请先运行 testPublishAndSyncToEs");
            return;
        }

        Product product = products.get(0);
        product.setTitle(product.getTitle() + "【已更新】");

        var result = productService.updateProduct(product);
        log.info("更新结果：{}", result);

        if (result.getSuccess()) {
            log.info("商品更新成功，ID：{}", product.getId());
        }
    }

    /**
     * 测试6：删除商品并从 ES 移除
     */
    @Test
    void testDeleteAndRemoveFromEs() {
        log.info("========== 测试：删除商品并从 ES 移除 ==========");

        // 查询测试商品
        List<Product> products = productMapper.selectList(null);
        if (products.isEmpty()) {
            log.warn("没有商品数据，请先运行 testPublishAndSyncToEs");
            return;
        }

        Long productId = products.get(0).getId();
        var result = productService.deleteProduct(productId);
        log.info("删除结果：{}", result);
    }

    /**
     * 测试7：清理 ES 索引（慎用）
     */
    @Test
    void testDeleteIndex() throws IOException {
        log.info("========== 测试：删除 ES 索引 ==========");

        IndicesClient indicesClient = client.indices();
        DeleteIndexRequest request = new DeleteIndexRequest("campus_product");
        AcknowledgedResponse response = indicesClient.delete(request, RequestOptions.DEFAULT);

        log.info("索引删除结果：{}", response.isAcknowledged());
    }
}