package com.campus.service.Impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.entity.Post;
import com.campus.entity.Product;
import com.campus.mapper.PostMapper;
import com.campus.mapper.ProductMapper;
import com.campus.service.IStatsService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class StatsServiceImpl implements IStatsService {

    @Resource
    private PostMapper postMapper;

    @Resource
    private ProductMapper productMapper;

    @Override
    public void incrementPostFavoriteCount(Long postId) {
        postMapper.update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, postId)
                .setSql("favorite_count = favorite_count + 1"));
    }

    @Override
    public void decrementPostFavoriteCount(Long postId) {
        postMapper.update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, postId)
                .setSql("favorite_count = favorite_count - 1"));
    }

    @Override
    public void incrementProductFavoriteCount(Long productId) {
        productMapper.update(null, new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, productId)
                .setSql("favorite_count = favorite_count + 1"));
    }

    @Override
    public void decrementProductFavoriteCount(Long productId) {
        productMapper.update(null, new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, productId)
                .setSql("favorite_count = favorite_count - 1"));
    }
}