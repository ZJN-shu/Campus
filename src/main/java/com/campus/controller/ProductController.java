package com.campus.controller;

import com.campus.dto.Result;
import com.campus.entity.Product;
import com.campus.service.IFavoriteService;
import com.campus.service.IProductService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/product")
public class ProductController {

    @Resource
    private IProductService productService;

    @Resource
    private IFavoriteService favoriteService;

    /**
     * 获取商品详情
     */
    @GetMapping("/{id}")
    public Result getById(@PathVariable Long id) {
        return productService.queryProductById(id);
    }

    /**
     * 按分类查询商品
     */
    @GetMapping("/category")
    public Result getByCategory(
            @RequestParam(defaultValue = "all") String category,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "time") String sort) {
        return productService.queryProductByCategory(category, current, minPrice, maxPrice, sort);
    }

    /**
     * 发布商品
     */
    @PostMapping
    public Result publish(@Valid @RequestBody Product product) {
        return productService.publishProduct(product);
    }

    /**
     * 更新商品
     */
    @PutMapping
    public Result update(@Valid @RequestBody Product product) {
        return productService.updateProduct(product);
    }

    /**
     * 删除商品
     */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        return productService.deleteProduct(id);
    }

    /**
     * 搜索商品
     */
    @GetMapping("/search")
    public Result search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Integer current) {
        return productService.searchProducts(keyword, current);
    }

    /**
     * 我发布的商品
     */
    @GetMapping("/my")
    public Result getMyProducts(@RequestParam(defaultValue = "1") Integer current) {
        return productService.getMyProducts(current);
    }

    /**
     * 收藏商品
     */
    @PostMapping("/favorite/{id}")
    public Result favorite(@PathVariable Long id) {
        return favoriteService.favorite("product", id);
    }

    /**
     * 取消收藏商品
     */
    @DeleteMapping("/favorite/{id}")
    public Result unfavorite(@PathVariable Long id) {
        return favoriteService.unfavorite("product", id);
    }

    /**
     * 检查是否已收藏
     */
    @GetMapping("/favorite/check/{id}")
    public Result isFavorited(@PathVariable Long id) {
        return Result.ok(favoriteService.isFavorited("product", id));
    }
}
