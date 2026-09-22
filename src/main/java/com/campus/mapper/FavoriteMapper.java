package com.campus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;
@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {

    /**
     * 查询用户的收藏列表（带目标详情）
     */
    @Select("SELECT f.* FROM tb_favorite f " +
            "WHERE f.user_id = #{userId} " +
            "ORDER BY f.create_time DESC " +
            "LIMIT #{offset}, #{limit}")
    List<Favorite> selectUserFavorites(@Param("userId") Long userId,
                                       @Param("offset") int offset,
                                       @Param("limit") int limit);

    /**
     * 统计用户的收藏数
     */
    @Select("SELECT COUNT(*) FROM tb_favorite WHERE user_id = #{userId}")
    int countUserFavorites(@Param("userId") Long userId);

    /**
     * 统计目标的收藏数
     */
    @Select("SELECT COUNT(*) FROM tb_favorite WHERE target_type = #{targetType} AND target_id = #{targetId}")
    int countTargetFavorites(@Param("targetType") String targetType,
                             @Param("targetId") Long targetId);

    /**
     * 检查是否已收藏
     */
    @Select("SELECT COUNT(*) FROM tb_favorite WHERE user_id = #{userId} AND target_type = #{targetType} AND target_id = #{targetId}")
    int checkFavorite(@Param("userId") Long userId,
                      @Param("targetType") String targetType,
                      @Param("targetId") Long targetId);
}