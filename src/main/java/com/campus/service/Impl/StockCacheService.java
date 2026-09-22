package com.campus.service.Impl;

import com.campus.cache.MultiLevelCache;
import com.campus.entity.Product;
import com.campus.mapper.ProductMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class StockCacheService {

    @Resource
    private ProductMapper productMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private MultiLevelCache multiLevelCache;

    private static final String CACHE_STOCK_KEY = "cache:stock:";
    private static final String CACHE_PRODUCT_KEY = "cache:product:";

    /**
     * 获取商品库存（优先从缓存）
     */
    public Integer getAvailableStock(Long productId) {
        String cacheKey = CACHE_STOCK_KEY + productId;

        log.info("getAvailableStock called: productId={}, cacheKey={}", productId, cacheKey);

        // 1. 查Redis（使用hasKey判断是否存在，而不是通过值判断）
        Boolean hasKey = stringRedisTemplate.hasKey(cacheKey);
        log.info("Redis key是否存在: {}", hasKey);

        if (Boolean.TRUE.equals(hasKey)) {
            String stockStr = stringRedisTemplate.opsForValue().get(cacheKey);
            log.info("Redis查询结果: {}", stockStr);
            if (stockStr != null) {
                try {
                    return Integer.parseInt(stockStr);
                } catch (NumberFormatException e) {
                    log.warn("缓存值格式错误: {}", stockStr);
                }
            }
        }

        // 2. 查数据库
        Product product = productMapper.selectById(productId);
        log.info("数据库查询结果: product={}, stock={}, reservedStock={}",
                product != null ? product.getId() : null,
                product != null ? product.getStock() : null,
                product != null ? product.getReservedStock() : null);

        if (product == null) {
            log.warn("商品不存在: productId={}", productId);
            return 0;
        }

        // 直接计算可用库存
        int stock = product.getStock() != null ? product.getStock() : 0;
        int reservedStock = product.getReservedStock() != null ? product.getReservedStock() : 0;
        int availableStock = stock - reservedStock;

        log.info("计算可用库存: stock={}, reservedStock={}, availableStock={}", stock, reservedStock, availableStock);

        // 3. 写入缓存（10秒过期）
        stringRedisTemplate.opsForValue().set(cacheKey, String.valueOf(availableStock), 10, TimeUnit.SECONDS);
        log.info("缓存写入完成: key={}, value={}", cacheKey, availableStock);

        return availableStock;
    }
    /**
     * 预扣库存时更新缓存
     */
    /**
     * 预扣库存时更新缓存
     */
    public void onReserveStock(Long productId, Integer quantity) {
        log.info("onReserveStock: productId={}, quantity={}", productId, quantity);
        String stockKey = CACHE_STOCK_KEY + productId;

        // 删除缓存，让下次查询重新加载
        stringRedisTemplate.delete(stockKey);
        log.info("预扣库存删除缓存: key={}", stockKey);

        multiLevelCache.evict(CACHE_PRODUCT_KEY + productId);
    }

    /**
     * 确认扣减时更新缓存
     */
    public void onConfirmStock(Long productId, Integer quantity) {
        log.info("onConfirmStock: productId={}, quantity={}", productId, quantity);
        String stockKey = CACHE_STOCK_KEY + productId;

        // 删除缓存
        stringRedisTemplate.delete(stockKey);
        log.info("确认扣减删除缓存: key={}", stockKey);

        multiLevelCache.evict(CACHE_PRODUCT_KEY + productId);
    }

    /**
     * 释放库存时更新缓存
     */
    /**
     * 释放库存时更新缓存
     */
    /**
     * 释放库存时更新缓存
     */
    public void onReleaseStock(Long productId, Integer quantity) {
        log.info("onReleaseStock: productId={}, quantity={}", productId, quantity);
        String stockKey = CACHE_STOCK_KEY + productId;

        // 直接删除缓存，让下次查询重新加载（最安全可靠）
        Boolean deleted = stringRedisTemplate.delete(stockKey);
        log.info("释放库存删除缓存: key={}, 删除成功={}", stockKey, deleted);

        // 清除商品详情缓存
        multiLevelCache.evict(CACHE_PRODUCT_KEY + productId);
    }
}