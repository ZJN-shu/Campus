package com.campus.service.Impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.cache.MultiLevelCache;
import com.campus.document.ProductDocument;
import com.campus.dto.ProductDTO;
import com.campus.dto.Result;
import com.campus.entity.Product;
import com.campus.entity.Student;
import com.campus.mapper.ProductMapper;
import com.campus.service.*;
import com.campus.utils.StudentHolder;
import com.campus.utils.SystemConstants;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.campus.utils.RedisConstants.*;

@Slf4j
@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product>
        implements IProductService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private MultiLevelCache multiLevelCache;

    @Resource
    private IStudentService studentService;

    @Resource
    private IFavoriteService favoriteService;

    @Resource
    private IHotRankService hotRankService;

    @Resource
    private StockCacheService stockCacheService;

    @Resource
    private ElasticsearchService esService;  // ✅ 添加 ES 服务注入

    private static final String CACHE_STOCK_KEY = "cache:stock:";
    private static final String CACHE_PRODUCT_KEY = "cache:product:";

    // ==================== 查询方法 ====================

    @Override
    public Result queryProductById(Long id) {
        String cacheKey = CACHE_PRODUCT_KEY + id;

        String productJson = multiLevelCache.get(cacheKey, key -> {
            Product product = getById(id);
            if (product == null) {
                return null;
            }
            return JSON.toJSONString(product);
        }, CACHE_PRODUCT_TTL);

        if (productJson == null) {
            return Result.fail("商品不存在");
        }

        Product product = JSON.parseObject(productJson, Product.class);

        Integer stock = getAvailableStock(id);
        product.setStock(stock);

        ProductDTO dto = convertToDTO(product);

        incrementViewCount(id);

        return Result.ok(dto);
    }

    /**
     * 获取可用库存（优先从缓存读取）
     */
    private Integer getAvailableStock(Long productId) {
        String stockKey = CACHE_STOCK_KEY + productId;

        String stockStr = stringRedisTemplate.opsForValue().get(stockKey);
        if (StrUtil.isNotBlank(stockStr)) {
            return Integer.parseInt(stockStr);
        }

        Product product = getById(productId);
        if (product == null) {
            return 0;
        }

        int availableStock = product.getAvailableStock();

        stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(availableStock),
                10, TimeUnit.SECONDS);

        return availableStock;
    }

    // ==================== 更新/删除方法 ====================

    @Override
    @Transactional
    public Result updateProduct(Product product) {
        Long currentUserId = StudentHolder.getStudentId();
        Product old = getById(product.getId());

        if (old == null) {
            return Result.fail("商品不存在");
        }

        if (!old.getSellerId().equals(currentUserId)) {
            return Result.fail("无权修改");
        }

        boolean success = updateById(product);
        if (success) {
            // 清除多级缓存
            multiLevelCache.evict(CACHE_PRODUCT_KEY + product.getId());
            // 清除库存缓存
            stringRedisTemplate.delete(CACHE_STOCK_KEY + product.getId());
            // ✅ 同步更新 ES
            esService.indexProduct(product);
        }

        return success ? Result.ok() : Result.fail("修改失败");
    }

    @Override
    @Transactional
    public Result deleteProduct(Long id) {
        Long currentUserId = StudentHolder.getStudentId();
        Product product = getById(id);

        if (product == null) {
            return Result.fail("商品不存在");
        }

        if (!product.getSellerId().equals(currentUserId)) {
            return Result.fail("无权删除");
        }

        boolean success = removeById(id);
        if (success) {
            // 清除多级缓存
            multiLevelCache.evict(CACHE_PRODUCT_KEY + id);
            // 清除库存缓存
            stringRedisTemplate.delete(CACHE_STOCK_KEY + id);
            // ✅ 从 ES 删除
            esService.deleteProduct(id);
        }

        return success ? Result.ok() : Result.fail("删除失败");
    }

    // ==================== 库存相关方法 ====================

    @Transactional
    public boolean reserveStock(Long productId, Integer quantity, Integer version) {
        int rows = baseMapper.reserveStock(productId, quantity, version);
        if (rows > 0) {
            stockCacheService.onReserveStock(productId, quantity);
            multiLevelCache.evict(CACHE_PRODUCT_KEY + productId);
            return true;
        }
        return false;
    }

    @Transactional
    public boolean confirmStock(Long productId, Integer quantity) {
        int rows = baseMapper.confirmStock(productId, quantity);
        if (rows > 0) {
            stockCacheService.onConfirmStock(productId, quantity);
            multiLevelCache.evict(CACHE_PRODUCT_KEY + productId);
            return true;
        }
        return false;
    }

    @Transactional
    public boolean releaseReservedStock(Long productId, Integer quantity) {
        int rows = baseMapper.releaseReservedStock(productId, quantity);
        if (rows > 0) {
            stockCacheService.onReleaseStock(productId, quantity);
            multiLevelCache.evict(CACHE_PRODUCT_KEY + productId);
            return true;
        }
        return false;
    }

    public Product getProductWithVersion(Long productId) {
        return baseMapper.selectForUpdate(productId);
    }

    // ==================== 其他方法 ====================

    @Override
    public Result queryProductByCategory(String category, Integer current,
                                         BigDecimal minPrice, BigDecimal maxPrice, String sort) {
        Page<Product> page = lambdaQuery()
                .eq(Product::getStatus, 1)
                .eq(StrUtil.isNotBlank(category) && !"all".equals(category),
                        Product::getCategory, category)
                .ge(minPrice != null, Product::getPrice, minPrice)
                .le(maxPrice != null, Product::getPrice, maxPrice)
                .orderByDesc("time".equals(sort), Product::getCreateTime)
                .orderByAsc("price_asc".equals(sort), Product::getPrice)
                .orderByDesc("price_desc".equals(sort), Product::getPrice)
                .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));

        List<ProductDTO> dtoList = page.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return Result.ok(dtoList, page.getTotal());
    }

    @Override
    @Transactional
    public Result publishProduct(Product product) {
        Long userId = StudentHolder.getStudentId();
        if (userId == null) {
            return Result.fail("请先登录");
        }

        product.setSellerId(userId);
        product.setStatus(1);
        product.setViewCount(0);
        product.setFavoriteCount(0);
        product.setCommentCount(0);
        product.setStock(100);
        product.setReservedStock(0);
        product.setVersion(0);

        boolean success = save(product);
        if (!success) {
            return Result.fail("发布失败");
        }

        // ✅ 同步到 ES
        esService.indexProduct(product);

        return Result.ok(product.getId());
    }

    @Override
    public Result searchProducts(String keyword, Integer current) {
        // ✅ 优先使用 ES 搜索
        if (StrUtil.isNotBlank(keyword)) {
            try {
                List<ProductDocument> esResults = esService.searchProducts(keyword, null, current, SystemConstants.DEFAULT_PAGE_SIZE);
                if (!esResults.isEmpty()) {
                    // 转换为 DTO
                    List<ProductDTO> dtoList = esResults.stream()
                            .map(this::convertDocumentToDTO)
                            .collect(Collectors.toList());
                    return Result.ok(dtoList);
                }
            } catch (Exception e) {
                log.error("ES搜索失败，降级到MySQL", e);
                // 降级到 MySQL 模糊查询
            }
        }

        // 降级：MySQL 模糊查询
        if (StrUtil.isBlank(keyword)) {
            return queryProductByCategory("all", current, null, null, "time");
        }

        Page<Product> page = lambdaQuery()
                .eq(Product::getStatus, 1)
                .and(wrapper -> wrapper
                        .like(Product::getTitle, keyword)
                        .or()
                        .like(Product::getDescription, keyword))
                .orderByDesc(Product::getCreateTime)
                .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));

        List<ProductDTO> dtoList = page.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return Result.ok(dtoList, page.getTotal());
    }

    @Override
    public void incrementViewCount(Long id) {
        String key = PRODUCT_STATS_KEY + id;
        stringRedisTemplate.opsForHash().increment(key, "viewCount", 1);

        CompletableFuture.runAsync(() -> {
            baseMapper.incrementViewCount(id);
        });
    }

    @Override
    public Result getMyProducts(Integer current) {
        Long userId = StudentHolder.getStudentId();
        if (userId == null) {
            return Result.fail("请先登录");
        }
        Page<Product> page = lambdaQuery()
                .eq(Product::getSellerId, userId)
                .orderByDesc(Product::getCreateTime)
                .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));

        List<ProductDTO> dtoList = page.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return Result.ok(dtoList, page.getTotal());
    }

    /**
     * 将 ES 文档转换为 DTO
     */
    private ProductDTO convertDocumentToDTO(ProductDocument doc) {
        ProductDTO dto = new ProductDTO();
        dto.setId(doc.getId());
        dto.setTitle(doc.getTitle());
        dto.setDescription(doc.getDescription());
        dto.setPrice(doc.getPrice());
        dto.setCategory(doc.getCategory());
        dto.setQuality(doc.getQuality());
        dto.setSellerId(doc.getSellerId());
        dto.setSellerName(doc.getSellerName());
        dto.setViewCount(doc.getViewCount());
        dto.setFavoriteCount(doc.getFavoriteCount());
        dto.setCreateTime(doc.getCreateTime());
        return dto;
    }

    /**
     * 转换为DTO
     */
    private ProductDTO convertToDTO(Product product) {
        ProductDTO dto = BeanUtil.copyProperties(product, ProductDTO.class);
        dto.setImages(product.getImageList());

        Student seller = studentService.getById(product.getSellerId());
        if (seller != null) {
            dto.setSellerName(seller.getNickName());
            dto.setSellerAvatar(seller.getAvatar());
            dto.setSellerCredit(seller.getCredit());
        }

        Long currentUserId = StudentHolder.getStudentId();
        if (currentUserId != null) {
            dto.setIsFavorited(favoriteService.isFavorited("product", product.getId()));
        }

        return dto;
    }
}