package com.campus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import java.math.BigDecimal;
import java.util.List;

public interface PostMapper extends BaseMapper<Post> {

    List<Post> queryHotPosts(@Param("category") String category,
                             @Param("limit") Integer limit);

    @Update("UPDATE tb_post SET view_count = view_count + 1 WHERE id = #{id}")
    int incrementViewCount(@Param("id") Long id);

    @Update("UPDATE tb_post SET like_count = like_count + #{delta} WHERE id = #{id}")
    int updateLikeCount(@Param("id") Long id, @Param("delta") int delta);

    @Update("UPDATE tb_post SET comment_count = comment_count + #{delta} WHERE id = #{id}")
    int updateCommentCount(@Param("id") Long id, @Param("delta") int delta);

    @Update("UPDATE tb_post SET hot_score = #{score} WHERE id = #{id}")
    int updateHotScore(@Param("id") Long id, @Param("score") BigDecimal score);
}