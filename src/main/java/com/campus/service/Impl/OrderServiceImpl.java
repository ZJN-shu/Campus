package com.campus.service.Impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.dto.Result;
import com.campus.entity.Order;
import com.campus.entity.Product;
import com.campus.entity.Student;
import com.campus.mapper.OrderMapper;
import com.campus.mapper.ProductMapper;
import com.campus.service.IOrderService;
import com.campus.service.IProductService;
import com.campus.service.IStudentService;
import com.campus.utils.StudentHolder;
import com.campus.utils.SystemConstants;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order>
        implements IOrderService {

    @Resource
    private ProductMapper productMapper;

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private StockCacheService stockCacheService;

    @Resource
    private IProductService productService;

    @Resource
    private IStudentService studentService;

    private static final int PAYMENT_TIMEOUT_MINUTES = 30;

    /**
     * 创建订单（预扣库存）
     */
    @Override
    @Transactional
    public Result createOrder(Long productId, Integer quantity) {
        Long userId = StudentHolder.getStudentId();
        if (userId == null) {
            return Result.fail("请先登录");
        }

        String lockKey = "product:lock:" + productId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean isLock = lock.tryLock(1, 5, TimeUnit.SECONDS);
            if (!isLock) {
                return Result.fail("系统繁忙，请稍后再试");
            }

            // 1. 查询商品（带乐观锁）
            Product product = productMapper.selectForUpdate(productId);
            if (product == null) {
                return Result.fail("商品不存在");
            }

            // 2. 检查库存
            int availableStock = product.getAvailableStock();
            if (availableStock < quantity) {
                return Result.fail("库存不足，剩余" + availableStock + "件");
            }

            // 3. 预扣库存（乐观锁）
            int rows = productMapper.reserveStock(productId, quantity, product.getVersion());
            if (rows == 0) {
                return Result.fail("下单失败，请重试");
            }

            // 4. 更新库存缓存
            stockCacheService.onReserveStock(productId, quantity);

            // 5. 创建订单
            Order order = new Order();
            order.setOrderNo(generateOrderNo());
            order.setProductId(productId);
            order.setUserId(userId);
            order.setQuantity(quantity);
            order.setAmount(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
            order.setStatus(0);  // 待支付
            order.setExpireTime(LocalDateTime.now().plusMinutes(PAYMENT_TIMEOUT_MINUTES));
            orderMapper.insert(order);

            log.info("订单创建成功: orderNo={}, productId={}, quantity={}",
                    order.getOrderNo(), productId, quantity);

            return Result.ok(order);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.fail("系统繁忙");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 支付订单（确认扣减库存）
     */
    @Override
    @Transactional
    public Result payOrder(Long orderId) {
        Long userId = StudentHolder.getStudentId();
        if (userId == null) {
            return Result.fail("请先登录");
        }

        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            return Result.fail("订单不存在");
        }

        if (!order.getUserId().equals(userId)) {
            return Result.fail("无权操作");
        }

        if (order.getStatus() != 0) {
            return Result.fail("订单状态异常");
        }

        // 检查是否超时
        if (order.getExpireTime().isBefore(LocalDateTime.now())) {
            // 超时订单，释放库存
            productMapper.releaseReservedStock(order.getProductId(), order.getQuantity());
            stockCacheService.onReleaseStock(order.getProductId(), order.getQuantity());
            orderMapper.updateStatus(orderId, 2);
            return Result.fail("订单已超时，请重新下单");
        }

        // 确认扣减库存
        int rows = productMapper.confirmStock(order.getProductId(), order.getQuantity());
        if (rows == 0) {
            return Result.fail("支付失败，请重试");
        }

        // 更新库存缓存
        stockCacheService.onConfirmStock(order.getProductId(), order.getQuantity());

        // 更新订单状态
        orderMapper.paySuccess(orderId);

        log.info("支付成功: orderNo={}", order.getOrderNo());
        return Result.ok();
    }

    /**
     * 取消订单（释放预扣库存）
     */
    @Override
    @Transactional
    public Result cancelOrder(Long orderId) {
        Long userId = StudentHolder.getStudentId();
        if (userId == null) {
            return Result.fail("请先登录");
        }

        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            return Result.fail("订单不存在");
        }

        if (!order.getUserId().equals(userId)) {
            return Result.fail("无权操作");
        }

        if (order.getStatus() != 0) {
            return Result.fail("订单已处理，无法取消");
        }

        // 释放预扣库存
        // 释放预扣库存
        int rows = productMapper.releaseReservedStock(order.getProductId(), order.getQuantity());
        log.info("释放预扣库存: productId={}, quantity={}, rows={}",
                order.getProductId(), order.getQuantity(), rows);

        // 更新库存缓存
        log.info("准备调用 onReleaseStock: productId={}, quantity={}",
                order.getProductId(), order.getQuantity());
        stockCacheService.onReleaseStock(order.getProductId(), order.getQuantity());
        log.info("onReleaseStock 调用完成");

        // 更新订单状态
        orderMapper.updateStatus(orderId, 2);

        log.info("订单已取消: orderNo={}", order.getOrderNo());
        return Result.ok();
    }

    /**
     * 获取订单详情
     */
    @Override
    public Result getOrderDetail(Long orderId) {
        Long userId = StudentHolder.getStudentId();
        if (userId == null) {
            return Result.fail("请先登录");
        }

        Order order = getById(orderId);
        if (order == null) {
            return Result.fail("订单不存在");
        }

        // 只有订单所有者可以查看
        if (!order.getUserId().equals(userId)) {
            return Result.fail("无权查看");
        }

        // 查询商品信息
        Product product = productService.getById(order.getProductId());

        // 查询卖家信息
        Student seller = null;
        if (product != null) {
            seller = studentService.getById(product.getSellerId());
        }

        // 组装返回数据
        OrderDetailVO detailVO = new OrderDetailVO();
        detailVO.setOrder(order);
        detailVO.setProduct(product);
        detailVO.setSeller(seller);

        return Result.ok(detailVO);
    }

    /**
     * 获取我的订单列表
     */
    @Override
    public Result getMyOrders(Integer status, Integer current) {
        Long userId = StudentHolder.getStudentId();
        if (userId == null) {
            return Result.fail("请先登录");
        }

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId);

        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }

        wrapper.orderByDesc(Order::getCreateTime);

        Page<Order> page = page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE), wrapper);

        // 填充商品信息
        for (Order order : page.getRecords()) {
            Product product = productService.getById(order.getProductId());
            if (product != null) {
                order.setProductTitle(product.getTitle());
                order.setProductImage(product.getFirstImage());
            }
        }

        return Result.ok(page.getRecords(), page.getTotal());
    }

    /**
     * 删除订单（软删除，只有已取消的订单可删除）
     */
    @Override
    @Transactional
    public Result deleteOrder(Long orderId) {
        Long userId = StudentHolder.getStudentId();
        if (userId == null) {
            return Result.fail("请先登录");
        }

        Order order = getById(orderId);
        if (order == null) {
            return Result.fail("订单不存在");
        }

        if (!order.getUserId().equals(userId)) {
            return Result.fail("无权操作");
        }

        // 只有已取消的订单可以删除
        if (order.getStatus() != 2) {
            return Result.fail("只能删除已取消的订单");
        }

        boolean success = removeById(orderId);
        if (success) {
            log.info("订单已删除: orderNo={}", order.getOrderNo());
        }

        return success ? Result.ok() : Result.fail("删除失败");
    }

    /**
     * 生成订单号
     */
    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = RandomUtil.randomNumbers(6);
        return timestamp + random;
    }

    // ==================== 内部类 ====================

    /**
     * 订单详情VO
     */
    public static class OrderDetailVO {
        private Order order;
        private Product product;
        private Student seller;

        public Order getOrder() { return order; }
        public void setOrder(Order order) { this.order = order; }

        public Product getProduct() { return product; }
        public void setProduct(Product product) { this.product = product; }

        public Student getSeller() { return seller; }
        public void setSeller(Student seller) { this.seller = seller; }
    }
}