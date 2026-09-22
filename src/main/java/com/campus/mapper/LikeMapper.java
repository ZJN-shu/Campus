package com.campus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.entity.Like;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;

import java.util.List;
@Mapper
public interface LikeMapper extends BaseMapper<Like> {

    /**
     * 统计目标的点赞数
     */
    @Select("SELECT COUNT(*) FROM tb_like WHERE target_type = #{targetType} AND target_id = #{targetId}")
    int countTargetLikes(@Param("targetType") String targetType,
                         @Param("targetId") Long targetId);

    /**
     * 检查用户是否已点赞
     */
    @Select("SELECT COUNT(*) FROM tb_like WHERE user_id = #{userId} AND target_type = #{targetType} AND target_id = #{targetId}")
    int checkUserLike(@Param("userId") Long userId,
                      @Param("targetType") String targetType,
                      @Param("targetId") Long targetId);

    /**
     * 删除点赞记录
     */
    @Delete("DELETE FROM tb_like WHERE user_id = #{userId} AND target_type = #{targetType} AND target_id = #{targetId}")
    int deleteUserLike(@Param("userId") Long userId,
                       @Param("targetType") String targetType,
                       @Param("targetId") Long targetId);

    /**
     * 获取目标的所有点赞用户ID
     */
    @Select("SELECT user_id FROM tb_like WHERE target_type = #{targetType} AND target_id = #{targetId}")
    List<Long> getLikeUserIds(@Param("targetType") String targetType,
                              @Param("targetId") Long targetId);
}