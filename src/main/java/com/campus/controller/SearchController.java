package com.campus.controller;

import com.campus.document.ProductDocument;
import com.campus.dto.Result;
import com.campus.service.ElasticsearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/search")
public class SearchController {

    @Autowired
    private ElasticsearchService esService;

    /**
     * 搜索商品
     */
    @GetMapping("/products")
    public Result searchProducts(
            @RequestParam String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {

        List<ProductDocument> results = esService.searchProducts(keyword, category, page, size);
        return Result.ok(results);
    }
}