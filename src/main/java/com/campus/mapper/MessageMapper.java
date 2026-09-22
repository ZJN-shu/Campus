package com.campus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import java.util.List;
@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    /**
     * 批量标记为已读
     */
    @Update("UPDATE tb_message SET is_read = 1 WHERE to_user_id = #{userId} AND is_read = 0")
    int markAllAsRead(@Param("userId") Long userId);

    /**
     * 获取未读消息数
     */
    @Update("SELECT COUNT(*) FROM tb_message WHERE to_user_id = #{userId} AND is_read = 0")
    int getUnreadCount(@Param("userId") Long userId);

    /**
     * 查询消息列表（带发送者信息）
     */
    List<Message> selectMessagesWithUser(@Param("userId") Long userId,
                                         @Param("type") Integer type,
                                         @Param("offset") int offset,
                                         @Param("limit") int limit);
}