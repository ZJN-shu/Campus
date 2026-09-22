package com.campus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.entity.Follow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FollowMapper extends BaseMapper<Follow> {

    /**
     * 查询共同关注
     */
    @Select("SELECT f2.followee_id FROM tb_follow f1 " +
            "JOIN tb_follow f2 ON f1.followee_id = f2.followee_id " +
            "WHERE f1.user_id = #{userId} AND f2.user_id = #{otherUserId}")
    List<Long> selectCommonFollows(@Param("userId") Long userId, @Param("otherUserId") Long otherUserId);
}
