package com.campus.controller;

import com.campus.annotation.RateLimit;
import com.campus.dto.LoginFormDTO;
import com.campus.dto.Result;
import com.campus.dto.StudentDTO;
import com.campus.service.IStudentService;
import com.campus.utils.StudentHolder;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/student")
public class StudentController {

    @Resource
    private IStudentService studentService;

    /**
     * 发送验证码
     */
    @PostMapping("/code")
    @RateLimit(value = 5, timeout = 1, timeUnit = TimeUnit.HOURS,
            message = "验证码发送太频繁，请1小时后再试")
    public Result sendCode(@RequestParam("phone") String phone) {
        return studentService.sendCode(phone);
    }

    /**
     * 登录
     */
    @PostMapping("/login")
    @RateLimit(value = 10, timeout = 1, timeUnit = TimeUnit.MINUTES,
            message = "登录尝试太频繁，请稍后再试")
    public Result login(@Valid @RequestBody LoginFormDTO loginForm) {
        return studentService.login(loginForm);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public Result me() {
        StudentDTO student = StudentHolder.getStudent();
        if (student == null) {
            return Result.fail("未登录");
        }
        return Result.ok(student);
    }

    /**
     * 获取当前用户统计数据
     */
    @GetMapping("/stats")
    public Result stats() {
        return studentService.getMyStats();
    }

    /**
     * 获取用户信息
     */
    @GetMapping("/{id}")
    public Result getInfo(@PathVariable Long id) {
        return studentService.getInfo(id);
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/info")
    public Result updateInfo(@Valid @RequestBody StudentDTO studentDTO) {
        return studentService.updateInfo(studentDTO);
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public Result logout() {
        // 前端删除token即可
        return Result.ok();
    }
}