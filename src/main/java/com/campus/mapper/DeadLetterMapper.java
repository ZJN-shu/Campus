package com.campus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.entity.DeadLetter;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface DeadLetterMapper extends BaseMapper<DeadLetter> {

    @Update("UPDATE tb_dead_letter SET status = #{status}, retry_count = retry_count + 1 WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}