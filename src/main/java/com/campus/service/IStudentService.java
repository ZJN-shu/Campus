package com.campus.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.dto.LoginFormDTO;
import com.campus.dto.Result;
import com.campus.dto.StudentDTO;
import com.campus.entity.Student;

public interface IStudentService extends IService<Student> {

    /**
     * 发送验证码
     */
    Result sendCode(String phone);

    /**
     * 登录
     */
    Result login(LoginFormDTO loginForm);

    /**
     * 获取用户信息
     */
    Result getInfo(Long id);

    /**
     * 更新用户信息
     */
    Result updateInfo(StudentDTO studentDTO);

    /**
     * 根据手机号创建用户
     */
    Student createUserWithPhone(String phone);

    /**
     * 获取当前用户的统计数据（帖子/商品/收藏/粉丝/关注数）
     */
    Result getMyStats();
}