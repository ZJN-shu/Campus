package com.campus;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@MapperScan("com.campus.mapper")  // 扫描 MyBatis 接口
@EnableTransactionManagement      // 开启事务管理
@EnableAspectJAutoProxy(exposeProxy = true)  // 开启 AOP 代理，暴露代理对象
@EnableAsync                      // 开启异步任务
@EnableScheduling                 // 开启定时任务
public class CampusApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusApplication.class, args);
    }
}