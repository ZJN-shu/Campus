package com.campus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.entity.ErrandTask;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import java.util.List;

public interface ErrandTaskMapper extends BaseMapper<ErrandTask> {

    @Update("UPDATE tb_errand_task SET status = #{status}, acceptor_id = #{acceptorId} WHERE id = #{id} AND status = 1")
    int acceptTask(@Param("id") Long id, @Param("acceptorId") Long acceptorId, @Param("status") Integer status);

    List<ErrandTask> selectNearbyTasks(@Param("latitude") Double latitude,
                                       @Param("longitude") Double longitude,
                                       @Param("radius") Double radius);
    @Update("UPDATE tb_errand_task SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}