package com.campus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.entity.PostComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import java.util.List;
@Mapper
public interface PostCommentMapper extends BaseMapper<PostComment> {
    @Update("UPDATE tb_post_comment SET like_count = like_count + 1 WHERE id = #{id}")
    int incrementLikeCount(@Param("id") Long id);

    @Update("UPDATE tb_post_comment SET like_count = like_count - 1 WHERE id = #{id}")
    int decrementLikeCount(@Param("id") Long id);
    List<PostComment> selectByPostIdWithUser(@Param("postId") Long postId);

    @Update("UPDATE tb_post_comment SET like_count = like_count + #{delta} WHERE id = #{id}")
    int updateLikeCount(@Param("id") Long id, @Param("delta") int delta);
}