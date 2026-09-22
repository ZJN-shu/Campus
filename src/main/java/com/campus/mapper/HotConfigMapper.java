package com.campus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.entity.HotConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
@Mapper
public interface HotConfigMapper extends BaseMapper<HotConfig> {

    /**
     * 根据目标类型获取热度配置
     */
    @Select("SELECT * FROM tb_hot_config WHERE target_type = #{targetType}")
    HotConfig selectByTargetType(@Param("targetType") String targetType);

    /**
     * 更新配置
     */
    @Update("UPDATE tb_hot_config SET " +
            "view_weight = #{viewWeight}, " +
            "like_weight = #{likeWeight}, " +
            "comment_weight = #{commentWeight}, " +
            "share_weight = #{shareWeight}, " +
            "favorite_weight = #{favoriteWeight}, " +
            "time_decay = #{timeDecay} " +
            "WHERE target_type = #{targetType}")
    int updateConfig(HotConfig config);

    /**
     * 获取所有配置
     */
    @Select("SELECT * FROM tb_hot_config ORDER BY target_type")
    List<HotConfig> selectAllConfigs();
}