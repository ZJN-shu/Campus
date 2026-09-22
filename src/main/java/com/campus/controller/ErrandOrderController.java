package com.campus.controller;

import com.campus.dto.Result;
import com.campus.service.IErrandOrderService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController//表明这是一个控制器，所有方法返回值直接作为 HTTP 响应体（JSON 格式）
@RequestMapping("/errand/order")
public class ErrandOrderController {

    @Resource
    private IErrandOrderService orderService;

    /**
     * 支付订单
     */
    @PostMapping("/{id}/pay")
    public Result payOrder(@PathVariable Long id) {
        return orderService.payOrder(id);
    }

    /**
     * 订单详情
     */
    @GetMapping("/{id}")
    public Result getOrderDetail(@PathVariable Long id) {
        return orderService.getOrderDetail(id);
    }
}