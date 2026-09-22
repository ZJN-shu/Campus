package com.campus.task;

import com.campus.entity.Order;
import com.campus.mapper.OrderMapper;
import com.campus.mapper.ProductMapper;
import com.campus.service.Impl.StockCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.List;

@Slf4j
@Component
public class OrderTimeoutScheduler {

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private ProductMapper productMapper;

    @Resource
    private StockCacheService stockCacheService;

    /**
     * 每分钟扫描一次超时订单
     */
    @Scheduled(cron = "0 * * * * ?")
    @Transactional
    public void processTimeoutOrders() {
        List<Order> timeoutOrders = orderMapper.selectTimeoutOrders();

        if (timeoutOrders.isEmpty()) {
            return;
        }

        log.info("扫描到{}个超时订单", timeoutOrders.size());

        for (Order order : timeoutOrders) {
            try {
                // 释放预扣库存
                int rows = productMapper.releaseReservedStock(
                        order.getProductId(), order.getQuantity());

                if (rows > 0) {
                    // 更新库存缓存
                    stockCacheService.onReleaseStock(order.getProductId(), order.getQuantity());

                    // 更新订单状态
                    orderMapper.updateStatus(order.getId(), 2);

                    log.info("订单超时自动取消: orderNo={}, productId={}",
                            order.getOrderNo(), order.getProductId());
                }
            } catch (Exception e) {
                log.error("处理超时订单失败: orderId={}", order.getId(), e);
            }
        }
    }
}